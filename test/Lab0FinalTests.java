package test;

import gameServer.*;
import test.util.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import static test.Lab0CheckpointTests.gPortStr;

/** Runs all Final tests on lab 0 gameServer components.

    <p>
    Tests performed are:
    <ul>
    <li>{@link test.gameServer.TestFinal_FileUpload}</li>
    <li>{@link test.gameServer.TestFinal_RandomWord}</li>
    <li>{@link test.gameServer.TestFinal_GuessCount}</li>
    <li>{@link test.gameServer.TestFinal_Restart}</li>
    <li>{@link test.gameServer.TestFinal_Close}</li>
    <li>{@link test.gameServer.TestFinal_Goodbye}</li>
    </ul>
 */
public class Lab0FinalTests {

    /** number of times to run each test */
    private static int runsOfEachTest = 1;

    /** Runs the tests.

        @param arguments Ignored.
     */
    public static void main(String[] arguments) {
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
            test.gameServer.TestFinal_FileUpload.class,
            test.gameServer.TestFinal_RandomWord.class,
            test.gameServer.TestFinal_GuessCount.class,
            test.gameServer.TestFinal_Restart.class,
            test.gameServer.TestFinal_Close.class,
            test.gameServer.TestFinal_Goodbye.class
        };
        
        Map<String, Integer> points = new HashMap<>();
        
        points.put("test.gameServer.TestFinal_FileUpload", 10);
        points.put("test.gameServer.TestFinal_RandomWord", 8);
        points.put("test.gameServer.TestFinal_GuessCount", 10);
        points.put("test.gameServer.TestFinal_Restart", 8);
        points.put("test.gameServer.TestFinal_Close", 6);
        points.put("test.gameServer.TestFinal_Goodbye", 6);
        
        Series series = new Series(tests, runsOfEachTest);
        SeriesReport report = series.run(10, System.out);

        gp.destroy();

        // Print the report and exit with an appropriate exit status.
        report.print(System.out, points, runsOfEachTest);
        System.exit(report.successful() ? 0 : 2);
    }
}
