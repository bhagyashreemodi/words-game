package test;

import gameServer.*;
import test.util.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Runs all Checkpoint tests on lab 0 gameServer components.

    <p>
    Tests performed are:
    <ul>
    <li>{@link test.gameServer.TestCheckpoint_Hello}</li>
    <li>{@link test.gameServer.TestCheckpoint_NewGame}</li>
    <li>{@link test.gameServer.TestCheckpoint_JoinGame}</li>
    <li>{@link test.gameServer.TestCheckpoint_StartGame}</li>
    </ul>
 */
public class Lab0CheckpointTests {

    /** game server port number - global variable shared across test suite */
    public static String gPortStr = "14736";
    
    /** number of times to run each test */
    private static int runsOfEachTest = 1;
    
    /** Runs the tests.

        @param args - pass server port number to test main
     */
    public static void main(String[] args) {
        Process gp = null;
        try {
            ProcessBuilder b = new ProcessBuilder("java", "gameServer.GameServer", gPortStr);
            b.inheritIO();
            gp = b.start();
        } catch (IOException e) {
            System.out.println("failed to start game server...aborting tests");
            System.exit(-1);
        }
        try { 
            Thread.sleep(2000);
        } catch (InterruptedException e) { }

        // Create the test list, the series object, and run the test series.
        @SuppressWarnings("unchecked")
        Class<? extends Test>[] tests = new Class[] {
            test.gameServer.TestCheckpoint_Hello.class,
            test.gameServer.TestCheckpoint_NewGame.class,
            test.gameServer.TestCheckpoint_JoinGame.class,
            test.gameServer.TestCheckpoint_StartGame.class
        };
        
        Map<String, Integer> points = new HashMap<>();
        
        points.put("test.gameServer.TestCheckpoint_Hello", 6);
        points.put("test.gameServer.TestCheckpoint_NewGame", 8);
        points.put("test.gameServer.TestCheckpoint_JoinGame", 8);
        points.put("test.gameServer.TestCheckpoint_StartGame", 10);
        
        Series series = new Series(tests, runsOfEachTest);
        SeriesReport report = series.run(10, System.out);

        gp.destroy();

        // Print the report and exit with an appropriate exit status.
        report.print(System.out, points, runsOfEachTest);
        System.exit(report.successful() ? 0 : 2);        
    }
}
