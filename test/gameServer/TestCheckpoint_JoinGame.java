package test.gameServer;

import test.util.*;
import static test.Lab0CheckpointTests.gPortStr;

/** Tests players joining game and game state

    <p>
    This test starts and sets up a game server and players, creates a game, 
    then has multiple players join in various scenarios.
 */
public class TestCheckpoint_JoinGame extends Test {

    /** Test notice. */
    public static final String notice =
        "checking game joining logic and game state using JOIN_GAME";
    /** Prerequisites. */
    public static final Class[] prerequisites =
        new Class[] {TestCheckpoint_NewGame.class};

    /** Address of the test game server. */
    private static String address;
    /** Game players with incorrect behaviors. */
    private static TestGameClient wrongArgPlayer;

    /** Initialize the test. */
    @Override
    protected void initialize() throws TestFailed {
        address = "localhost:" + gPortStr;
        wrongArgPlayer = new TestGameClient(address, "bad_player");
    }

    /** Performs the test. */
    @Override
    protected void perform() throws TestFailed {
        // Create a new game and have players join
        wrongArgPlayer.send("JOIN_GAME\n");
        if(!wrongArgPlayer.ReadResponse().equals("Invalid arguments for command JOIN_GAME."))
            throw new TestFailed("Incorrect response to JOIN_GAME with invalid arguments");

        TestGame testGame = new TestGame(10);
        try {
            testGame.GameSetup();
            testGame.NewGame();
            testGame.JoinGame();
        }
        catch(TestFailed e) { throw e; }

        TestGameClient flakyClient = testGame.players.get(1);
        testGame.DisconnectPlayer(flakyClient);

        TestGame.PlayerThread t = new TestGame.PlayerThread(address, "1");
        t.player.SendJoinGame(testGame.tag);

        if(!t.player.ReadResponse().equals("New player must always start with HELLO!"))
            throw new TestFailed("Incorrect response to JOIN_GAME before HELLO");

        wrongArgPlayer.stop();
        testGame.cleanUp();
    }        
}
