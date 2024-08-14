package com.stockfishweb.core;

import com.stockfishweb.core.engine.StockfishClient;
import com.stockfishweb.core.engine.enums.Query;
import com.stockfishweb.core.engine.enums.QueryType;
import com.stockfishweb.core.engine.exception.StockfishEngineException;
import com.stockfishweb.core.engine.exception.StockfishInitException;
import com.stockfishweb.model.BestMoveEval;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static com.stockfishweb.common.Util.START_FEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class SfServiceTest {


    private static final Logger log = Logger.getLogger(SfServiceTest.class.getSimpleName());

    private SfService sfService = new SfService();

    private StockfishClient client;

    @BeforeEach
    void setUp() {
        try {
            client = new StockfishClient.Builder()
//                    .setInstances(1)
//                    .setOption(Option.Threads, 4)
//                    .setVariant(Variant.DEFAULT)
                    .build();
            sfService.setClient(client);
        } catch (StockfishInitException e) {
            fail(e);
        }
    }

    @AfterEach
    void tearDown() {
        try {
            client.close();
        } catch (StockfishEngineException e) {
            log.severe("error while closing Stockfish engine: " + e.getClass().getSimpleName() + " " + e.getMessage());
        }
    }



    @Test
    void getBestMoveAndEval() {
        try {
            Query query = new Query(QueryType.Best_Move, START_FEN, 10);
            BestMoveEval b = sfService.getBestMoveEvalAsync(query);

            assertThat(b).isNotNull();
            assertThat(b.getEval()).isNotNull();
            assertThat(b.getBestMove()).isNotNull();
            assertThat(b.getContinuation()).isNotNull();

        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getBestMoveAsync() {
        try {
            Query query = new Query(QueryType.Best_Move, START_FEN, 10);
            String b = sfService.getBestMoveAsync(query);

            assertThat(b).isNotNull();
            assertThat(b.equals("c4f7"));

        } catch (Exception e) {
            fail(e);
        }
    }
    //getBestMoveSync
    @Test
    void getBestMoveSync() {
        try {
            Query query = new Query(QueryType.Best_Move, START_FEN, 10);
            String b = sfService.getBestMoveSync(query);

            assertThat(b).isNotNull();
            assertThat(b.equals("c4f7"));

        } catch (Exception e) {
            fail(e);
        }
    }
}
