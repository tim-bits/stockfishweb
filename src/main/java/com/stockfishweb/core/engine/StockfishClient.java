/* Copyright 2018 David Cai Wang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stockfishweb.core.engine;

import com.stockfishweb.core.engine.enums.Option;
import com.stockfishweb.core.engine.enums.Query;
import com.stockfishweb.core.engine.enums.QueryType;
import com.stockfishweb.core.engine.enums.Variant;
import com.stockfishweb.core.engine.exception.StockfishEngineException;
import com.stockfishweb.core.engine.exception.StockfishInitException;
import com.stockfishweb.core.engine.exception.StockfishPoolException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The StockfishClient for managing Stockfish processes,
 * as well as for interacting with the Stockfish API using {@link Query}.
 * <p>
 *
 * @author Niflheim
 * @see <a href="https://stockfishchess.org/">Stockfish website</a>
 * @see <a href="https://github.com/official-stockfish/Stockfish">Official Stochfish <b>Github</b> repository</a>
 * @since 1.0
 */

@Component
public class StockfishClient {

    private static final Log log = LogFactory.getLog(StockfishClient.class);

    private List<Stockfish> engines;

    @Value("${evaluator.version}")
    private Integer evaluatorVersion = 10;
    private Stockfish evaluator;

    @Value("${default.instances.number}")
    private int instances;

    @Value("${engine.path}")
    private String path;
    @Value("${engine.variant}")
    private Variant variant;

//    @Value("#{'${default.options}'.split(',')}")
    private Set<Option> options;

    @Value("#{${options.map}}")
    private Map<String, Integer> optionsMap;

//    @Value("#{${valuesMap}}")
//    private Map<String, Integer> valuesMap;

//    @Value("#{new Boolean('${add.engines.on.demand}')}")
//    private boolean addEnginesOnDemand = true;

    @Value("${max.number.of.open.instances}")
    private int maxNumberOfOpenInstances;

    public StockfishClient()  {

    }

    @PostConstruct
    private void init() throws StockfishInitException{
        this.options = optionsMap.entrySet().stream().map(e ->
        {Option o = Option.valueOf(e.getKey());
            o.setValue(e.getValue());
            return o;
        }).collect(Collectors.toSet());

        engines = new LinkedList<>();

        for (int i = 0; i < instances; i++) {
            engines.add(new Stockfish(path, variant, options.toArray(new Option[0])));
        }
        evaluator = new Stockfish(path, variant, evaluatorVersion, options.toArray(new Option[0]));
        engines.add(evaluator);
    }

    /**
     * Private constructor for {@code StockfishClient} which is used by Builder to create a new instance
     *
     * @param path      path to folder with Stockfish core (default assets/engine)
     * @param instances number of Stockfish core that will be launched to process requests asynchronously
     * @param variant   variant of Stockfish core, see {@link com.stockfishweb.core.engine.enums.Variant} enum
     * @param options   Stockfish launch options, see {@link com.stockfishweb.core.engine.enums.Option} enum
     * @throws StockfishInitException throws if Stockfish process can not be initialized, starter or bind
     */

    private StockfishClient(String path, int instances, Variant variant, Set<Option> options) throws StockfishInitException {
        this.path = path;
        this.variant = variant;
        this.options = options;

        engines = new LinkedList<>();
        for (int i = 0; i < instances; i++) {
            engines.add(new Stockfish(path, variant, options.toArray(new Option[0])));
        }

        evaluator = new Stockfish(path, variant, evaluatorVersion, options.toArray(new Option[0]));
        engines.add(evaluator);
    }

    public String getResponse(Query query) {
        try {
            return getResponseAsync(query).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public CompletableFuture<String> getResponseAsync(Query query) {
        log.debug("getResponseAsync(query)");
        Stockfish engine = getEngine(query.getType());
        engine.setBusy(true);
        log.debug("engine reference: " + engine);
        CompletableFuture<String> future = CompletableFuture.supplyAsync(
                getMethod(query, engine)
        );
        future
                .thenAccept(msg -> notify(msg, future, query))
                .exceptionally(
                        e -> {
                            log.error(e.getMessage() != null ? e.getMessage() : e.getClass().getName());
                            return null;
                        }
                )
                .whenComplete((success, error) -> engine.setBusy(false))
        ;
        return future;
    }

    /**
     * Migrating away from CompletableFuture in favor of Spring @Async
     * @param query
     * @return
     */
    public String getResponseSync(Query query) {
        log.debug("getResponseSync(query)");
        Stockfish engine = getEngine(query.getType());
        engine.setBusy(true);
        log.debug("engine reference: " + engine);

        Supplier supplier = getMethod(query, engine);
        return (String) supplier.get();
    }


    void notify(String msg, Future<String> future, Query query) {
        log.info("notify() thread: " + Thread.currentThread().getName());
        log.info("Received message: " + msg);
        log.info("Query movetime: " + query.getMovetime());
    }

    protected List<Stockfish> deadEnginePruning() {
        List<Stockfish> deadEngines = engines.stream().filter(e -> e.isDead()).collect(Collectors.toList());
        deadEngines.forEach(sf -> {
            log.warn(sf.getProcess().pid() + " died with " + sf.getProcess().exitValue());
            sf.cleanup();
        });
        engines.removeAll(deadEngines);
        return deadEngines;
    }

    public Stockfish getEngine(QueryType queryType) {
        deadEnginePruning();

        Stockfish engine;

        if (QueryType.Eval.equals(queryType)) {
            if (evaluator.isDead()) {
                try {
                    evaluator = new Stockfish(path, variant, evaluatorVersion, options.toArray(new Option[0]));
                } catch (StockfishInitException e) {
                    log.error("couldn't create evaluator", e);
                    throw new RuntimeException(e);
                }
            } else {

                while (evaluator.isBusy()) {
                    try {
                        log.debug("waiting for evaluator: " + new Date());
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            engine = evaluator;
            log.debug("got evaluator");
        } else {
            Optional<Stockfish> engineOptional = engines.stream().filter(e -> !e.isBusy() && !e.isDead() && e != evaluator).findFirst();
            if (engineOptional.isPresent()) {
                engine = engineOptional.get();
            } else {
//                if (engines.isEmpty() || addEnginesOnDemand) {
                if (engines.size() < maxNumberOfOpenInstances) {
                    try {
                        log.debug("Creating a new engine...");
                        engine = new Stockfish(path, variant, options.toArray(new Option[0]));
                        engines.add(engine);
                    } catch (StockfishInitException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    throw new StockfishPoolException("Number Of Maximum Open Instances Exceeded: " + engines.size()
                            + " actual vs " + maxNumberOfOpenInstances + " threshold");
                }
            }
        }
        return engine;
    }

    private Supplier getMethod(Query query, Stockfish engine) {
        switch (query.getType()) {
            case Best_Move:
//                return () -> engine.getBestMove(query);
                return () -> engine.getBestMoveFromContinuation(query);
            case Make_Move:
                return () ->  engine.makeMove(query);
            case Legal_Moves:
                return () -> engine.getLegalMoves(query);
            case Checkers:
                return () -> engine.getCheckers(query);
            case Eval:
                return () -> evaluator.getEval(query);
        }
        log.error("Illegal command: " + query.getType());
        throw new StockfishEngineException("Illegal command: " + query.getType());
    }

    /**
     * This method close all Stockfish instances that were created, as well as close all
     * threads for processing responses. You must call this method when you close
     * your program to avoid uncontrolled memory leaks.
     * <p>
     * Exceptions are thrown only after trying to close all remaining threads
     *
     * @throws StockfishEngineException when at least one of the processes could not be closed.
     */
    public void close() throws StockfishEngineException {
        System.out.println(new Date() + "   Client is being closed. Please hold tight...");

        engines.stream().forEach(engine -> {
            try {
                System.out.println(new Date() + "   Closing engine with pid = " + engine.getProcess().pid());
                engine.close();
            } catch (IOException | StockfishEngineException e) {
                System.err.println(new Date() +"    Can not stop Stockfish. Please, close it manually.");
                e.printStackTrace(System.err);
            }
        });
    }

    public List<Stockfish> getEngines() {
        return engines;
    }

    public void setMaxNumberOfOpenInstances(int maxNumberOfOpenInstances) {
        this.maxNumberOfOpenInstances = maxNumberOfOpenInstances;
    }

    public static class Builder {
        private Set<Option> options = new HashSet<>();
        private Variant variant = Variant.DEFAULT;
        private String path = null;
        private int instances = 1;

        /**
         * @param num number of Stockfish core that will be launched to process requests asynchronously
         * @return Builder to continue creating StockfishClient
         */
        public final Builder setInstances(int num) {
            instances = num;
            return this;
        }

        /**
         * @param v variant of Stockfish core, see {@link com.stockfishweb.core.engine.enums.Variant} enum
         * @return Builder to continue creating StockfishClient
         */
        public final Builder setVariant(Variant v) {
            variant = v;
            return this;
        }

        /**
         * @param o     Stockfish launch options, see {@link com.stockfishweb.core.engine.enums.Option} enum
         * @param value value of option
         * @return Builder to continue creating StockfishClient
         */
        public final Builder setOption(Option o, long value) {
            options.add(o.setValue(value));
            return this;
        }

        /**
         * @param path path to folder with Stockfish core (default assets/engine/)
         * @return Builder to continue creating StockfishClient
         */
        public final Builder setPath(String path) {
            this.path = path;
            return this;
        }

        /**
         * @return ready StockfishClient with fields set
         * @throws StockfishInitException throws if Stockfish process can not be initialized, starter or bind
         */
        public final StockfishClient build() throws StockfishInitException {
            return new StockfishClient(path, instances, variant, options);
        }
    }
}
