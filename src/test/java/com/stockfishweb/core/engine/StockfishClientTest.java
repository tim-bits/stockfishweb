package com.stockfishweb.core.engine;

import com.stockfishweb.core.engine.enums.Option;
import com.stockfishweb.core.engine.enums.Query;
import com.stockfishweb.core.engine.enums.QueryType;
import com.stockfishweb.core.engine.enums.Variant;
import com.stockfishweb.core.engine.exception.StockfishEngineException;
import com.stockfishweb.core.engine.exception.StockfishInitException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import static com.stockfishweb.common.Util.START_FEN;
import static com.stockfishweb.core.engine.StockfishTest.getEnginePidsSpawnedByTest;
import static com.stockfishweb.core.engine.util.FileEngineUtil.ASSETS_LOCATION;
import static com.stockfishweb.core.engine.util.FileEngineUtil.ENGINE_FILE_NAME_PREFIX;
import static org.junit.jupiter.api.Assertions.*;



class StockfishClientTest {

    private static final Log log = LogFactory.getLog(StockfishClientTest.class);

    StockfishClient client;


    @BeforeAll
    static void beforeAllTests() {
        getEnginePidsSpawnedByTest().forEach(ProcessHandle::destroy);
    }
    /**
     * Standard open/close tests.
     */
    @Test
    void simpleTests() {
            try {
                int instanceNumber = 4;
                StockfishClient client = new StockfishClient.Builder()
                        .setInstances(instanceNumber)
                        .setOption(Option.Threads, 4)
                        .setVariant(Variant.DEFAULT)
                        .build();
                assertEquals(instanceNumber + 1, getEnginePidsSpawnedByTest(ENGINE_FILE_NAME_PREFIX).size());
                client.close();
                assertEquals(0, getEnginePidsSpawnedByTest(ENGINE_FILE_NAME_PREFIX).size());// + ENGINE_FILE_NAME_SUFFIX + "_bmi2"));

            } catch (Exception e) {
                fail(e);
            }
    }

    /**
     * Kill one of Stockfish process and close.
     */
    @Test
    void killOneStockfishTest() {
        try {
            int instanceNumber = 4;
            StockfishClient client = new StockfishClient.Builder()
                    .setInstances(instanceNumber)
                    .setPath(ASSETS_LOCATION)
                    .setOption(Option.Threads, 2)
                    .setVariant(Variant.DEFAULT)
                    .build();
            //plus evaluator
            assertEquals(instanceNumber + 1, StockfishTest.getEnginePidsSpawnedByTest().size());
            getEnginePidsSpawnedByTest().get(ThreadLocalRandom.current().nextInt(0, instanceNumber-1)).destroy();
            assertEquals(instanceNumber, StockfishTest.getEnginePidsSpawnedByTest().size());
//            assertThrows(StockfishEngineException.class, client::close);
            assertDoesNotThrow(client::close);
            assertEquals(0, StockfishTest.getEnginePidsSpawnedByTest().size());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void multipleLongBestMovesAsync() {

        try {
            client = new StockfishClient.Builder().build();
            client.setMaxNumberOfOpenInstances(4);
            Query query = new Query.Builder(QueryType.Best_Move, START_FEN).build();
            CompletableFuture<String> future = client.getResponseAsync(query);
            Thread.sleep(500);
            CompletableFuture<String> future1 = client.getResponseAsync(query);
            Thread.sleep(500);
            CompletableFuture<String> future2 = client.getResponseAsync(query);
            Thread.sleep(500);
            CompletableFuture<String> future3 = client.getResponseAsync(query);
            Thread.sleep(500);
            while (!future.isDone() || !future1.isDone() || !future2.isDone() || !future3.isDone()) {
                Thread.sleep(500);
                System.out.println(new Date() + " " + future.isDone() + future1.isDone()  + future2.isDone()+ future3.isDone());
            }
            log.info("Best move 1: " +  future.get());
            log.info("Best move 2: " +  future1.get());
            log.info("Best move 3: " +  future2.get());
            log.info("Best move 4: " +  future3.get());

        } catch (Throwable e) {
            e.printStackTrace();
            fail(e);
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    @Test
    void multipleLongBestMoves() {
        try {
            client = new StockfishClient.Builder().build();

            Query query = new Query.Builder(QueryType.Best_Move, START_FEN).build();

            String move = client.getResponse(query);
            String move1 = client.getResponse(query);
            String move2 = client.getResponse(query);
            String move3 = client.getResponse(query);
            log.info("Best move 1: " +  move);
            log.info("Best move 2: " +  move1);
            log.info("Best move 3: " +  move2);
            log.info("Best move 4: " +  move3);

        } catch (Throwable e) {
            fail(e);
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    @Test
    void multipleLongBestMovesSync() {

        try {
            client = new StockfishClient.Builder().build();

            Query query = new Query.Builder(QueryType.Best_Move, START_FEN).build();

            String move = client.getResponseSync(query);
            String move1 = client.getResponseSync(query);
            String move2 = client.getResponseSync(query);
            String move3 = client.getResponseSync(query);
            log.info("Best move 1: " +  move);
            log.info("Best move 2: " +  move1);
            log.info("Best move 3: " +  move2);
            log.info("Best move 4: " +  move3);

        } catch (Throwable e) {
            fail(e);
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }



    @Test
    void deadEnginePruningTest() throws StockfishInitException, InterruptedException {
        int localInstances = 5;
        client = new StockfishClient.Builder()
            .setInstances(localInstances)
                        .setPath(ASSETS_LOCATION)
                        .setOption(Option.Threads, 2)
                        .setVariant(Variant.DEFAULT)
                        .build();
        //plus evaluator
        assertEquals(localInstances + 1,  client.getEngines().size());
        client.getEngines().get(0).getProcess().destroy();
        while (client.getEngines().get(0).getProcess().isAlive()) {
            System.err.println(client.getEngines().get(0).getProcess().pid() + " still alive");
            Thread.sleep(200);
        }

        //after destruction, the engine is dead, but is still in the list
        assertEquals(localInstances+1,  client.getEngines().size());

        List<Stockfish> deadEngines = client.deadEnginePruning();
        assertEquals(1, deadEngines.size());
        assertEquals(localInstances,  client.getEngines().size());

        client.getEngines().get(0).sendCommand("quit");
        assertEquals(localInstances,  client.getEngines().size());

        while (client.getEngines().get(0).getProcess().isAlive()) {
            System.err.println(client.getEngines().get(0).getProcess().pid() + " still alive");
            Thread.sleep(200);
        }

        deadEngines = client.deadEnginePruning();
        assertEquals(1, deadEngines.size());
        assertEquals(localInstances - 1,  client.getEngines().size());

    }

    @AfterEach
    void tearDown() {
        try {
            if (client != null) {
                client.close();
                client = null;
            }
        } catch (StockfishEngineException e) {
            log.error("error while closing Stockfish engine: " + e.getClass().getSimpleName() + " " + e.getMessage());
        }
    }
}
