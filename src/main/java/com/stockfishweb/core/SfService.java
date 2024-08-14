package com.stockfishweb.core;


import com.stockfishweb.core.engine.StockfishClient;
import com.stockfishweb.core.engine.enums.Query;
import com.stockfishweb.core.engine.enums.QueryType;
import com.stockfishweb.core.engine.exception.StockfishPoolException;
import com.stockfishweb.model.BestMoveEval;
import org.bughouse.fen.FenValidator;
import org.bughouse.fen.ReturnCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static com.stockfishweb.common.Util.START_FEN;

@Service
public class SfService {

    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());

    @Autowired
    protected StockfishClient client;

    protected void setClient(final StockfishClient client){
        this.client = client;
    }

    public String getBestMoveAsync() {
        Query query = new Query(QueryType.Best_Move, START_FEN);
        return getBestMoveAsync(query);
    }


    public String getBestMoveSync(Query query) {
        return client.getEngine(query.getType()).getBestMove(query);
    }

    public String getBestMoveAsync(Query query) {
        query.setType(QueryType.Best_Move);
        CompletableFuture<String> f = client.getResponseAsync(query);
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Best move and cont combined
     *
     * @param query
     * @return
     */
    public BestMoveEval getBestMoveEvalAsync(Query query) {

        ReturnCode returnCode = FenValidator.getInstance().validate(query.getFen());
        if (!returnCode.isValid()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, returnCode.getDescription());
        }

        query.setType(QueryType.Best_Move);
        CompletableFuture<String> bestMoveFuture = client.getResponseAsync(query);
        query.setType(QueryType.Eval);
        CompletableFuture<String> evalFuture = client.getResponseAsync(query);
        List<String> bestMoveEvalList = Stream.of(bestMoveFuture, evalFuture)
                .map(CompletableFuture::join)
                .toList();

        String bestmove = bestMoveEvalList.get(0).substring(bestMoveEvalList.get(0).indexOf("|") + 1).split("\\s+")[0];
        String mate = bestMoveEvalList.get(0).substring(0, bestMoveEvalList.get(0).indexOf("|")).trim();

        //todo: need something more elegant than this
        return new BestMoveEval(bestmove, bestMoveEvalList.get(1), bestMoveEvalList.get(0).substring(bestMoveEvalList.get(0).indexOf("|") + 1), mate);
    }

    /**
     * Migrating away from CompletableFuture in favor of Spring @Async
     * @param query
     * @return
     */
    public BestMoveEval getBestMoveEvalSync(Query query) {

        ReturnCode returnCode = FenValidator.getInstance().validate(query.getFen());
        if (!returnCode.isValid()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, returnCode.getDescription());
        }

        String bestMoveResponse = null;
        String evalResponse = null;
        try {
            query.setType(QueryType.Best_Move);
            bestMoveResponse = client.getResponseSync(query);
            query.setType(QueryType.Eval);
            evalResponse = client.getResponseSync(query);

            String bestmove = bestMoveResponse.substring(bestMoveResponse.indexOf("|") + 1).split("\\s+")[0];
            String mate = bestMoveResponse.substring(0, bestMoveResponse.indexOf("|")).trim();

            //todo: need something more elegant than this
            return new BestMoveEval(bestmove, evalResponse, bestMoveResponse.substring(bestMoveResponse.indexOf("|") + 1), mate);
        } catch (StockfishPoolException e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "All Stockfish Engines are Busy. Please try again later");
        }
    }
}