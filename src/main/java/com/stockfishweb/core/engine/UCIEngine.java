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
import com.stockfishweb.core.engine.enums.Variant;
import com.stockfishweb.core.engine.exception.StockfishEngineException;
import com.stockfishweb.core.engine.exception.StockfishInitException;
import com.stockfishweb.core.engine.util.FileEngineUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import java.io.*;
import java.util.*;

abstract class UCIEngine {

    private static final Log logger = LogFactory.getLog(UCIEngine.class);
    protected BufferedReader input;

    protected BufferedWriter output;

    protected Process process;


    protected boolean busy;

    public UCIEngine(String path, Variant variant, Integer engineVersion, Option... options) throws StockfishInitException {
        try {

            process = Runtime.getRuntime().exec(FileEngineUtil.getPath(variant, path, engineVersion));
            input = new BufferedReader(new InputStreamReader(process.getInputStream()));
            output = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

            for (Option option : options)
                passOption(option);
        } catch (IOException e) {
            throw new StockfishInitException("Unable to start and bind Stockfish process: ", e);
        }
    }

    UCIEngine(String path, Variant variant, Option... options) throws StockfishInitException {
        this(path, variant, null, options);
    }

    void waitForReady() {
        sendCommand("isready");
//        readResponse("readyok");
        readLine("readyok");
    }

    void sendCommand(String command) {
        try {
            output.write(command + "\n");
            output.flush();
        } catch (IOException e) {
            throw new StockfishEngineException(e);
        }
    }

//    String readLine(String expected) {
//        try {
//            return input.lines().sequential().filter(l -> l.startsWith(expected)).findFirst()
//                    .orElseThrow(() -> new StockfishEngineException("Can not find expected line: " + expected));
//        } catch (UncheckedIOException e) {
//            throw new StockfishEngineException(e);
//        }
//    }

    String readLine(String expected) {
        busy = true;
        try {
            String line;
            while ((line = input.readLine()) != null) {
                logger.debug(line);
                if (line.startsWith(expected)) {
                    return line;
                }
            }
            throw new StockfishEngineException("Can not find expected line: " + expected);
        } catch (IOException e) {
            throw new StockfishEngineException(e);
        } finally {
            busy = false;
        }
    }


    String readLine(String[] expected) {
        busy = true;
        try {
            String line = input.readLine();
             while (input.ready() && line!= null) {
                logger.debug(line);
                if (line.contains("Total evaluation") || line.contains("Final evaluation")) {
                    logger.debug("Evaluation line found");
                }
                 for (String s : expected) {
                     if (line.contains(s)) {
                         return line;
                     }
                 }

                line = input.readLine();

            }
            return line;
        } catch (IOException e) {
            throw new StockfishEngineException(e);
        } finally {
            busy = false;
        }
    }

    String[] getBestMoveAndContinuation() {
        busy = true;
        try {
            String pastLine = null;
            String line = input.readLine();
            boolean mateFound = false;

            logger.debug(line);
            logger.debug(input.ready());

            while (/*input.ready() && */line!= null) {
                pastLine = line;
                line = input.readLine();

                logger.debug(line);

//                if (line.contains("mate")) {
//                    return new String[] {line, line};
//                }

                if (/*mateFound || */line.startsWith("bestmove")) {
                    logger.debug("bestmove found");
                    return new String[] {line, pastLine};
                }
            }
            logger.debug("bestmove not found");
            return null;
        } catch (IOException e) {
            throw new StockfishEngineException(e);
        } finally {
            busy = false;
        }
    }

    public String[] readAllAtOnce() {
        List<String> list = input.lines().toList();
        return new String[]{list.get(list.size()-1), list.get(list.size()-2)};
    }

    List<String> readResponse(String expected) {
        busy = true;
        try {
            List<String> lines = new ArrayList<>();
            String line;
            boolean isPresent = false;
            while ((line = input.readLine()) != null) {
                logger.debug(line);
                lines.add(line);

                if (line.startsWith(expected)) {
                    isPresent = true;
                    break;
                }
            }
            if (isPresent) {
                return lines;
            } else {
                throw new StockfishEngineException("Can not find expected line: " + expected);
            }
        } catch (IOException e) {
            throw new StockfishEngineException(e);
        } finally {
            busy = false;
        }
    }

    private void passOption(Option option) {
        logger.info(option.toString());
        sendCommand(option.toString());
    }

    public Process getProcess() {
        return process;
    }

    public boolean isBusy() {
        if (busy) {
            logger.debug(this + " is busy");
        }
        return busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }


    public boolean isDead() {
        return !process.isAlive();
    }
}
