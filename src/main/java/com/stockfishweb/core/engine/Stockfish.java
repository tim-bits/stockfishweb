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
import com.stockfishweb.core.engine.enums.Variant;
import com.stockfishweb.core.engine.exception.StockfishInitException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;

import static com.stockfishweb.core.engine.enums.Query.Builder.MOVE_REGEX_PATTERN;

public class Stockfish extends UCIEngine {

    private static final Log log = LogFactory.getLog(Stockfish.class);

    public Stockfish(String path, Variant variant, Option... options) throws StockfishInitException {
        super(path, variant, options);
    }

    public Stockfish(String path, Variant variant, Integer engineVersion, Option... options) throws StockfishInitException {
        super(path, variant, engineVersion, options);
    }

    String makeMove(Query query) {
        waitForReady();
        sendCommand("position fen " + query.getFen() + " moves " + query.getMove());
        return getFen();
    }

    String getCheckers(Query query) {
        waitForReady();
        sendCommand("position fen " + query.getFen());

        waitForReady();
        sendCommand("d");

        return readLine("Checkers: ").substring(10);
    }


    void sendGoCommand(Query query) {
        query.normalize();

        if (query.getDifficulty() >= 0) {
            waitForReady();
            sendCommand("setoption name Skill Level value " + query.getDifficulty());
        }

        waitForReady();
        if (query.getFen() != null) {
            sendCommand("position fen " + query.getFen());
        }
        StringBuilder command = new StringBuilder("go ");

        if (query.getDepth() > 0)
            command.append("depth ").append(query.getDepth()).append(" ");

        if (query.getMovetime() > 0)
            command.append("movetime ").append(query.getMovetime());

        waitForReady();
        System.out.println(command);
        sendCommand(command.toString());
    }

    public String getBestMove(Query query) {
        sendGoCommand(query);
        return readLine("bestmove").substring(9).split("\\s+")[0];
    }

    public String getBestMoveFromContinuation(Query query) {
        sendGoCommand(query);
        String[] bestMoveAndContinuation = getBestMoveAndContinuation();
        System.out.println(bestMoveAndContinuation[0] + ", " + bestMoveAndContinuation[1]);
        Matcher bestMoveMatcher = MOVE_REGEX_PATTERN.matcher(bestMoveAndContinuation[0]);
        Matcher continuationMatcher = MOVE_REGEX_PATTERN.matcher(bestMoveAndContinuation[1]);
        if (bestMoveMatcher.find() && continuationMatcher.find()) {
            String bestmove = continuationMatcher.group(0);
            int mateIndex = bestMoveAndContinuation[1].indexOf("mate ");
            String mate = "";
            if (mateIndex > -1) {
                int endOfmate = bestMoveAndContinuation[1].indexOf("mate ") + "mate ".length();
                mate = bestMoveAndContinuation[1].substring( endOfmate, endOfmate + 2).trim();// e.g. "mate 2" or "mate 10"
            }
            return  mate + "|" + bestMoveAndContinuation[1].substring(bestMoveAndContinuation[1].indexOf(bestmove));

        } else {
            //stalemate?
            if("bestmove (none)".equals(bestMoveAndContinuation[0]) || bestMoveAndContinuation[1].contains("mate 0")){
                return "0|none";
            }
        }
        return bestMoveAndContinuation[0].substring(9).split("\\s+")[0];
    }

    String getEval(Query query) {
        waitForReady();
        sendCommand("position fen " + query.getFen());
        waitForReady();
        sendCommand("eval");

        String line = readLine(new String[] {"Total evaluation", "Final evaluation"});
        return line.split("\\s+")[2];
    }


    String getLegalMoves(Query query) {
        waitForReady();
        sendCommand("position fen " + query.getFen());

        waitForReady();
        sendCommand("go perft 1");

        StringBuilder legal = new StringBuilder();
        List<String> response = readResponse("Nodes");

        for (String line : response)
            if (!line.isEmpty() && !line.contains("Nodes") && line.contains(":"))
                legal.append(line.split(":")[0]).append(" ");

        return legal.toString();
    }

    void close() throws IOException {
        if (process.isAlive()) {
            try {
                sendCommand("quit");
            } finally {
                input.close();
                output.close();
                process.destroy();
            }
        }
    }

    public void cleanup() {
        try {
            close();
        } catch (IOException e) {
            log.error(e);
        } finally {
            input = null;
            output = null;
            process = null;
        }

    }

    private String getFen() {
        waitForReady();
        sendCommand("d");

        return readLine("Fen: ").substring(5);
    }
}
