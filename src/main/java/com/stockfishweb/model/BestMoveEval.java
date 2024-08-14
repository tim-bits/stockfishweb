package com.stockfishweb.model;

public class BestMoveEval {
    private String bestMove;
    private String eval;

    public String getContinuation() {
        return continuation;
    }

    public void setContinuation(String continuation) {
        this.continuation = continuation;
    }

    public String getMate() {
        return mate;
    }

    public void setMate(String mate) {
        this.mate = mate;
    }

    private String continuation;
    private String mate;

    public BestMoveEval() {
    }

    public BestMoveEval(String bestMoveEval, String eval, String continuation) {
        this.bestMove = bestMoveEval;
        this.eval = eval;
        this.continuation = continuation;
    }

    public BestMoveEval(String bestMoveEval, String eval, String continuation, String mate) {
        this.bestMove = bestMoveEval;
        this.eval = eval;
        this.continuation = continuation;
        this.mate = mate;
    }


    public String getBestMove() {
        return bestMove;
    }

    public void setBestMove(String bestMove) {
        this.bestMove = bestMove;
    }

    public String getEval() {
        return eval;
    }

    public void setEval(String eval) {
        this.eval = eval;
    }

    @Override
    public String toString() {
        return bestMove + ", " + eval;
    }
}
