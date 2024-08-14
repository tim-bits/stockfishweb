package com.stockfishweb.core.engine;

import com.stockfishweb.common.Util;
import com.stockfishweb.core.engine.enums.Option;
import com.stockfishweb.core.engine.enums.Query;
import com.stockfishweb.core.engine.enums.QueryType;
import com.stockfishweb.core.engine.enums.Variant;
import com.stockfishweb.core.engine.exception.StockfishEngineException;
import com.stockfishweb.core.engine.exception.StockfishInitException;
import org.junit.jupiter.api.*;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.stockfishweb.common.Util.BEST_MOVE;
import static com.stockfishweb.core.engine.enums.Query.Builder.*;
import static com.stockfishweb.core.engine.util.FileEngineUtil.ENGINE_FILE_NAME_PREFIX;
import static org.junit.jupiter.api.Assertions.*;

public class StockfishTest {

    public static final String ERROR_STOCKFISH = "Unknown command: ";

    private static final Pattern fenPattern = Pattern.compile(START_REGEX + FEN_REGEX + END_REGEX);
    private static final Logger log = Logger.getLogger(StockfishTest.class.getSimpleName());
    private Stockfish stockfish;

    @BeforeAll
    static void beforeAllTests() {
        getEnginePidsSpawnedByTest().forEach(ProcessHandle::destroy);
    }

    @BeforeEach
    void setUp() {
//        log.severe("------------------------------------------");
//        getEnginePidsSpawnedByTest().forEach(System.err::println);
//        log.severe("------------------------------------------");

        try {
            if (stockfish != null) {
                log.severe("stockfish is not null");
            }
            stockfish = new Stockfish(null, Variant.DEFAULT);
        } catch (StockfishInitException e) {
            log.severe( "error while creating Stockfish client: " + e.getCause());
            fail(e);
        }
    }

//    void evalSetup () {
//        try {
//            if (stockfish != null) {
//                log.severe("stockfish is not null");
//            }
//            stockfish = new Stockfish(null, Variant.DEFAULT, 10);
//        } catch (StockfishInitException e) {
//            log.severe( "error while creating Stockfish client: " + e.getCause());
//            fail(e);
//        }
//    }

    @AfterEach
    void tearDown() {
        try {
            stockfish.close();
//            stockfish = null;
        } catch (IOException | StockfishEngineException e) {
            log.severe("error while closing Stockfish engine: " + e.getClass().getSimpleName() + " " + e.getMessage());
        }
    }

    @RepeatedTest(10)
    void createCorrectly() {
            assertEquals(1, getEnginePidsSpawnedByTest().size());
    }

    @Test
    void incorrectCommand() {
        try {
            String incorrectCommand = "incorrect command";
            stockfish.sendCommand(incorrectCommand);
            List<String> response = stockfish.readResponse(ERROR_STOCKFISH);
            // to match e.g. "Stockfish 16.1 by the Stockfish developers (see AUTHORS file)",
            // "Stockfish 10 64 by T. Romstad, M. Costalba, J. Kiiski, G. Linscott" etc
            assertTrue(Pattern.compile("^Stockfish [1-9][0-9]").matcher(response.get(0)).find());
            assertTrue(response.get(1).startsWith(ERROR_STOCKFISH + "'" + incorrectCommand + "'"));

//            incorrectCommand = "one more incorrect command";
//            stockfish.sendCommand(incorrectCommand);
//            assertArrayEquals(new String[]{ERROR_STOCKFISH + incorrectCommand},
//                    stockfish.readResponse(ERROR_STOCKFISH).toArray());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void waitForReadyOnKilledInstance() {
        try {
            assertDoesNotThrow(() -> stockfish.waitForReady());
            stockfish.process.destroy();
            assertThrows(StockfishEngineException.class, () -> stockfish.waitForReady());
        } catch (Exception e) {
            fail(e);
        }
    }

    @RepeatedTest(50)
    void waitForReady() {
        assertDoesNotThrow(() -> stockfish.waitForReady());
    }


    @Test
    void sendCommand() {
        try {
            File tempFile = creteTempFile();
            setOutput(tempFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(tempFile)));
            stockfish.sendCommand("hello world");
            assertEquals("hello world", reader.readLine());
            stockfish.sendCommand("hello world1");
            assertEquals("hello world1", reader.readLine());
            stockfish.sendCommand("hello world2");
            assertEquals("hello world2", reader.readLine());
        } catch (IOException | NoSuchFieldException | IllegalAccessException e) {
            fail(e);
        }
    }

    @Test
    void readLine() {
        try {
            File tempFile = creteTempFile();
            BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempFile));
            outputStream.write("31\n".getBytes());
            for (int i = 0; i < 33; i++) {
                outputStream.write((i + "\n").getBytes());
            }
            outputStream.flush();
            setInput(tempFile);

            assertEquals("31", stockfish.readLine("3"));
            assertEquals("1", stockfish.readLine("1"));
            assertEquals("3", stockfish.readLine("3"));
            assertThrows(StockfishEngineException.class, () -> stockfish.readLine("40"));
            assertThrows(StockfishEngineException.class, () -> stockfish.readLine("21"));
            outputStream.write("21\n".getBytes());
            outputStream.flush();
            assertEquals("21", stockfish.readLine("21"));

        } catch (IOException | NoSuchFieldException | IllegalAccessException e) {
            fail(e);
        }
    }

    @Test
    void readResponse() {
        try {
            File tempFile = creteTempFile();
            BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempFile, true));
            List<String> response = new ArrayList<>();
            outputStream.write("31\n".getBytes());
            for (int i = 0; i < 33; i++) {
                response.add(Integer.toString(i));
                outputStream.write((i + "\n").getBytes());
            }
            outputStream.flush();
            setInput(tempFile);

            assertArrayEquals(new String[]{"31"}, stockfish.readResponse("31").toArray());
            assertArrayEquals(response.toArray(), stockfish.readResponse("32").toArray());
            assertThrows(StockfishEngineException.class, () -> stockfish.readResponse("36").toArray());
            assertThrows(StockfishEngineException.class, () -> stockfish.readResponse("31").toArray());

            outputStream.write("31\n".getBytes());
            outputStream.flush();

            assertArrayEquals(new String[]{"31"}, stockfish.readResponse("31").toArray());
        } catch (IOException | NoSuchFieldException | IllegalAccessException e) {

            fail(e);
        }
    }

    @Test
    void makeMove() {
        try {
            Query makeMove = new Query.Builder(QueryType.Make_Move, Util.START_FEN)
                    .setMove("a2a4").setDepth(10)
//                    .setMovetime(1000)
                    .build();
            log.info("Move: " + stockfish.makeMove(makeMove));
            final Matcher matcher = fenPattern.matcher(stockfish.makeMove(makeMove));
            assertTrue(matcher.matches());
            makeMove = new Query.Builder(QueryType.Make_Move, Util.START_FEN)
                    .setMove("a2h6")
                    .build();
            assertEquals(Util.START_FEN, stockfish.makeMove(makeMove));
            makeMove = new Query.Builder(QueryType.Make_Move, Util.START_FEN)
                    .build();
            assertEquals(Util.START_FEN, stockfish.makeMove(makeMove));
            assertEquals(Util.START_FEN, stockfish.makeMove(makeMove));

        } catch (Exception e) {
            fail(e);
        }

    }

    @Test
    void makeMoveWithWrongFen() {
        try{
            //invalid fen kills sf!
            Query makeErrorMove = new Query.Builder(QueryType.Make_Move, "8/8/8/8/8/8/8/8 b KQkq - 0 1")
                    .build();

            assertThrows(StockfishEngineException.class, () -> stockfish.makeMove(makeErrorMove));
            assertThrows(StockfishEngineException.class, () -> stockfish.readLine(""));

            while (stockfish.getProcess().isAlive()) {
                System.err.println(stockfish.getProcess().pid() + " still alive");
                Thread.sleep(200);
            }
            assertFalse(stockfish.process.isAlive());
//            assertEquals(139, stockfish.process.exitValue());
            assertNotNull(stockfish.process.exitValue());
        } catch (Exception e) {
            fail(e);
        }

    }

    @Test
    void getCheckers() {
        try {
//            Query checkersQuery = new Query.Builder(QueryType.Checkers, START_FEN).build();
//            assertEquals("", stockfish.getCheckers(checkersQuery));

            Query makeErrorMove = new Query.Builder(QueryType.Make_Move, "8/8/8/8/8/8/8/8 b KQkq - 0 1")
                    .build();

            assertThrows(StockfishEngineException.class, () -> stockfish.makeMove(makeErrorMove));
            assertThrows(StockfishEngineException.class, () -> stockfish.readLine(""));

            while (stockfish.process.isAlive()) {
                System.err.println(stockfish.process.pid() + " still alive");
                Thread.sleep(200);
            }

            assertFalse(stockfish.process.isAlive());
//            assertEquals(139, stockfish.process.exitValue());
            assertNotNull(stockfish.process.exitValue());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getEval() {
        try {
            stockfish.close();
            stockfish = null;

            stockfish = new Stockfish(null, Variant.DEFAULT, 10);
            Query evalQuery = new Query.Builder(QueryType.Eval, Util.START_FEN)
                    .build();
            assertTrue(stockfish.process.isAlive());
            String evalString = stockfish.getEval(evalQuery);
            System.out.println(evalString);
            assertNotNull(evalString);
            assertNotEquals(evalString.length(), 0);
            assertTrue(stockfish.process.isAlive());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getBestMoveEval() {
        try {
            stockfish.close();
            stockfish = null;

            stockfish = new Stockfish(null, Variant.DEFAULT, 10);
            Query query = new Query.Builder(QueryType.Best_Move, Util.START_FEN)
                    .build();
            assertTrue(stockfish.process.isAlive());
            String evalString = stockfish.getEval(query);
            System.out.println(evalString);
            assertNotNull(evalString);
            assertNotEquals(evalString.length(), 0);
            assertTrue(stockfish.process.isAlive());

            query.setType(QueryType.Eval);

        } catch (Exception e) {
            fail(e);
        }
    }


    @Test
    void getBestMoveAndContinuation() {
        try {
//            String[] s = stockfish.readAllAtOnce();
            stockfish.sendCommand("position fen r1bqk1nr/p2p1ppp/2p5/1pb5/2BpP3/2P5/PP1P1PPP/RNBQ1RK1 w - - 0 1");

            stockfish.waitForReady();
            stockfish.sendCommand("go depth 15");
            String[] s = stockfish.getBestMoveAndContinuation();
            assertNotNull(s);
            assertNotNull(s[0]);
            assertNotNull(s[1]);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getBestMoveAndContinuationMateDetected() {
        try {
//            String[] s = stockfish.readAllAtOnce();
            stockfish.sendCommand("position fen r1bqk1nr/p2p2Pp/2p5/1pb5/2BpP3/2P5/PP1P1PPP/RNBQ1RK1 w - - 0 1");

            stockfish.waitForReady();
            stockfish.sendCommand("go depth 15");
            String[] s = stockfish.getBestMoveAndContinuation();
            assertNotNull(s);
            assertNotNull(s[0]);
            assertNotNull(s[1]);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getBestMoveFromContinuationNoMate() {
        try {
            Query query = new Query.Builder(QueryType.Best_Move, Util.START_FEN)
                    .build();

            String s = stockfish.getBestMoveFromContinuation(query);//.get(BEST_MOVE);
            assertNotNull(s);
//            assertTrue(s.startsWith(" "));
//            assertTrue(s.trim().startsWith("c4f7"));
            assertTrue(s.startsWith("|"));
        } catch (Exception e) {
            fail(e);
        }
    }


    @Test
    void getBestMoveFromContinuationForcedCheckmateDetected() {
        try {
            Query query = new Query.Builder(QueryType.Best_Move,
                    "r1bqk1nr/p2p2Pp/2p5/1pb5/2BpP3/2P5/PP1P1PPP/RNBQ1RK1 w - - 0 1")
                    .build();

            String s = stockfish.getBestMoveFromContinuation(query);
            assertNotNull(s);
            assertTrue(s.startsWith("2|"));
            assertTrue(s.substring(2).startsWith("d1h5 e8e7 h5e5"));
        } catch (Exception e) {
            fail(e);
        }
    }

//    mate note detecting on windows
    @Test
    void getBestMoveFromContinuationForcedDoubleDigitCheckmateDetected() {
        try {
//            Query query = new Query.Builder(QueryType.Best_Move,
//                    "8/8/8/8/8/K7/R7/6k1 w - - 0 1")
//                    .build();


            Query query = new Query() {
                @Override
                //strnagely enough "mate" segment only appears after 15 moves
                public int getDepth() {
                    return 16;
                }
            };
            query.setFen("8/8/8/8/8/K7/R7/1k6 w - - 0 1");

            String s = stockfish.getBestMoveFromContinuation(query);
            assertNotNull(s);
            assertTrue(s.matches("^[1-9][0-9]\\|.+"));
//            assertTrue(s.substring(2).startsWith("d1h5 e8e7 h5e5"));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getBestMoveFromContinuationStalemate() {
        try {
            Query query = new Query.Builder(QueryType.Best_Move,
                    "k7/1R6/1K6/8/8/8/8/8 b - - 0 1")
                    .build();

            String s = stockfish.getBestMoveFromContinuation(query);
            assertNotNull(s);
            assertEquals(s,"0|none");
//            assertTrue(s.startsWith("0|"));
//            assertTrue(s.substring(2).startsWith("d1h5 e8e7 h5e5"));
        } catch (Exception e) {
            fail(e);
        }
    }

            @Test
    void getBestMove() {
        try {
            final String move = "^([a-h][1-9]){2}$";
            final Pattern movePattern = Pattern.compile(move);
            Query bestMoveQuery = new Query.Builder(QueryType.Best_Move, Util.START_FEN).build();
            bestMoveQuery.setDepth(15);
            String bestMove = stockfish.getBestMove(bestMoveQuery);
            log.info(bestMove);
            assertTrue(movePattern.matcher(bestMove).matches());

            bestMoveQuery = new Query.Builder(QueryType.Best_Move, Util.START_FEN)
                    .setDepth(10)
                    .setMovetime(10)
                    .setDifficulty(10)
                    .build();
            bestMove = stockfish.getBestMove(bestMoveQuery);
            log.info(bestMove);
           assertTrue(movePattern.matcher(bestMove).matches());

            bestMoveQuery = new Query.Builder(QueryType.Best_Move, Util.START_FEN)
                    .setDepth(-10)
                    .setMovetime(-10)
                    .setDifficulty(-10)
                    .build();
            bestMove = stockfish.getBestMove(bestMoveQuery);
            log.info(bestMove);
            assertTrue(movePattern.matcher(bestMove).matches());

            Query makeErrorMove = new Query.Builder(QueryType.Make_Move, "8/8/8/8/8/8/8/8 b KQkq - 0 1")
                    .build();

            assertThrows(StockfishEngineException.class, () -> stockfish.makeMove(makeErrorMove));
            assertThrows(StockfishEngineException.class, () -> stockfish.readLine(""));
//            assertFalse(stockfish.process.isAlive());
            while (stockfish.process.isAlive()) {
                System.err.println(stockfish.getProcess().pid() + " still alive");
                Thread.sleep(200);
            }
//            assertEquals(139, stockfish.process.exitValue());
            assertNotNull(stockfish.process.exitValue());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getLegalMoves() {
        try {
            final String legalMovesRegex = "^(([a-h][1-9]){2}\\s)+$";
            final Pattern movePattern = Pattern.compile(legalMovesRegex);
            Query legalMoveQuery = new Query.Builder(QueryType.Legal_Moves, Util.START_FEN).build();
            String legalMoves = stockfish.getLegalMoves(legalMoveQuery);
            log.info(legalMoves);
            assertTrue(movePattern.matcher(legalMoves).matches());
            legalMoveQuery = new Query.Builder(QueryType.Legal_Moves, Util.START_FEN)
                    .setDepth(20)
                    .setMovetime(20)
                    .setDifficulty(20)
                    .build();
            legalMoves = stockfish.getLegalMoves(legalMoveQuery);
            log.info(legalMoves);
            assertTrue(movePattern.matcher(legalMoves).matches());

            legalMoveQuery = new Query.Builder(QueryType.Legal_Moves, Util.START_FEN)
                    .setDepth(-10)
                    .setMovetime(-10)
                    .setDifficulty(-10)
                    .build();
            legalMoves = stockfish.getLegalMoves(legalMoveQuery);
            log.info(legalMoves);
            assertTrue(movePattern.matcher(legalMoves).matches());

            Query makeErrorMove = new Query.Builder(QueryType.Make_Move, "8/8/8/8/8/8/8/8 b KQkq - 0 1")
                    .build();

            assertThrows(StockfishEngineException.class, () -> stockfish.makeMove(makeErrorMove));
            assertThrows(StockfishEngineException.class, () -> stockfish.readLine(""));

            while (stockfish.getProcess().isAlive()) {
                System.err.println(stockfish.getProcess().pid() + " still alive");
                Thread.sleep(200);
            }

            assertFalse(stockfish.process.isAlive());
//            assertEquals(139, stockfish.process.exitValue());
            assertNotNull(stockfish.process.exitValue());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void checkExceptionAfterClose() {
        try {
            stockfish.getProcess().destroy();
            assertThrows(StockfishEngineException.class, () -> stockfish.waitForReady());
            assertThrows(StockfishEngineException.class, () -> stockfish.sendCommand(""));
            assertThrows(StockfishEngineException.class, () -> stockfish.sendCommand("quit"));
            assertThrows(StockfishEngineException.class,
                    () -> stockfish.getBestMove(new Query.Builder(QueryType.Best_Move, Util.START_FEN).build()));
            assertThrows(StockfishEngineException.class,
                    () -> stockfish.getCheckers(new Query.Builder(QueryType.Checkers, Util.START_FEN).build()));
            assertThrows(StockfishEngineException.class,
                    () -> stockfish.getLegalMoves(new Query.Builder(QueryType.Legal_Moves, Util.START_FEN).build()));
            assertThrows(StockfishEngineException.class,
                    () -> stockfish.makeMove(new Query.Builder(QueryType.Make_Move, Util.START_FEN).build()));
            assertThrows(StockfishEngineException.class, () -> stockfish.readLine(""));
            assertThrows(StockfishEngineException.class, () -> stockfish.readResponse(""));
//            assertThrows(StockfishEngineException.class, () -> stockfish.close());

            while (stockfish.getProcess().isAlive()) {
                System.err.println(stockfish.getProcess().pid() + " still alive");
                Thread.sleep(200);
            }

            assertDoesNotThrow(stockfish::close);
        } catch (Exception e) {
            fail(e);
        }

    }

    @RepeatedTest(10)
    void closeTest() {
        try {
            assertEquals(1, getEnginePidsSpawnedByTest().size());
            stockfish.close();
            assertEquals(0, getEnginePidsSpawnedByTest().size());
            assertDoesNotThrow(() -> stockfish.close());
        } catch (Exception e) {
            fail(e);
        }
    }

    public static List<ProcessHandle> getEnginePidsSpawnedByTest(String enginePrefix) {
        List<ProcessHandle> ph = ProcessHandle.allProcesses().filter(p ->
                p.parent() != null  && p.parent().isPresent() &&
                p.parent().get().pid() == ProcessHandle.current().pid() &&
                p.isAlive() &&
                p.info().command().isPresent()
                        && p.info().command().get().contains(enginePrefix)


        ).collect(Collectors.toList());
        return ph;
    }

    public static List<ProcessHandle> getEnginePidsSpawnedByTest() {
        return getEnginePidsSpawnedByTest(ENGINE_FILE_NAME_PREFIX);
    }

    private void setOutput(File tempFile) throws NoSuchFieldException, IOException, IllegalAccessException {
        Field output = stockfish.getClass().getSuperclass().getDeclaredField("output");
        output.setAccessible(true);
        output.set(stockfish, new BufferedWriter(new FileWriter(tempFile, false)));
        output.setAccessible(false);
    }

    private void setInput(File tempFile) throws NoSuchFieldException, FileNotFoundException, IllegalAccessException {
        Field input = stockfish.getClass().getSuperclass().getDeclaredField("input");
        input.setAccessible(true);
        input.set(stockfish, new BufferedReader(new InputStreamReader(new FileInputStream(tempFile))));
        input.setAccessible(false);
    }

    private File creteTempFile() throws IOException {
        return File.createTempFile("stockfish-", ".tmp");
    }
}
