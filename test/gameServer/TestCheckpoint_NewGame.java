package test.gameServer;

import test.util.*;
import static test.Lab0CheckpointTests.gPortStr;

/** Tests game creation by leader

    <p>
    This test starts and sets up a game server and players, then has multiple
    valid and invalid players attempt to start a new game.
 */
public class TestCheckpoint_NewGame extends Test {

    /** Test notice. */
    public static final String notice =
        "checking game creation using NEW_GAME";
    /** Prerequisites. */
    public static final Class[] prerequisites =
        new Class[] {TestCheckpoint_Hello.class};

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
        // Create a game and try invalid commands.        
        wrongArgPlayer.send("NEW_GAME tag invalid\n");
        if(!wrongArgPlayer.ReadResponse().equals("Invalid arguments for command NEW_GAME."))
            throw new TestFailed("Incorrect response to NEW_GAME with invalid arguments");


        TestGame testGame = new TestGame(8);
        try {
            testGame.GameSetup();
            testGame.NewGame();
        }
        catch(TestFailed e) { throw e; }

        invalidPlayer.SendNewGame(testGame.tag);
        if(!invalidPlayer.ReadResponse().equals("New player must always start with HELLO!"))
            throw new TestFailed("Incorrect response to NEW_GAME before HELLO");

        wrongArgPlayer.stop();
        invalidPlayer.stop();
        testGame.cleanUp();        
    }
}
