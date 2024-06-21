package test.gameServer;

import test.util.*;
import static test.Lab0CheckpointTests.gPortStr;

/** Tests server setup and player connection.

    <p>
    This test starts and sets up a game server then attempts connections by
    invalid players.  The test covers server initialization, player connection,
    requirement checking on game server, and reception of errors by players.
 */
public class TestCheckpoint_Hello extends Test {

    /** Test notice. */
    public static final String notice =
        "checking player-server connection and HELLO use";

    /** Address of the test game server. */
    private static String address;
    /** Game players with incorrect behaviors. */
    private static TestGameClient invalidPlayer, wrongArgPlayer;

    /** Initialize the test. */
    @Override
    protected void initialize() throws TestFailed {
        address = "localhost:" + gPortStr;
        invalidPlayer = new TestGameClient(address, "");
        wrongArgPlayer = new TestGameClient(address, "bad_player");
    }

    /** Performs the test. */
    @Override
    protected void perform() throws TestFailed {
        // Create a two-player game and try invalid connections.        
        TestGame testGame = new TestGame(2);
        try {
            testGame.GameSetup();
        }
        catch(TestFailed e) { throw e; }
        
        wrongArgPlayer.send("HELLO bad_player invalid\n");
        if(!wrongArgPlayer.ReadResponse().equals("Invalid arguments for command HELLO."))
            throw new TestFailed("Incorrect response to HELLO with invalid arguments");
        
        invalidPlayer.SendHello();
        if(!invalidPlayer.ReadResponse().equals("Invalid user name. Try again."))
            throw new TestFailed("Incorrect response to HELLO with invalid user name");
            
        invalidPlayer.stop();
        wrongArgPlayer.stop();
        testGame.cleanUp();
    }
}
