package test.gameServer;

import test.util.*;
import static test.Lab0CheckpointTests.gPortStr;

/** Tests leader starting game

    <p>
    This test starts and sets up a game server and players, creates a game, 
    has players join the game, then starts it in various ways.
 */
public class TestCheckpoint_StartGame extends Test {

    /** Test notice. */
    public static final String notice =
        "checking game starting logic using START_GAME";
    /** Prerequisites. */
    public static final Class[] prerequisites =
        new Class[] {TestCheckpoint_JoinGame.class};

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
        // Create a game, have players join, start the game
        wrongArgPlayer.send("START_GAME tag invalid invalid\n");
        if(!wrongArgPlayer.ReadResponse().equals("Invalid arguments for command START_GAME."))
            throw new TestFailed("Incorrect response to START_GAME with invalid arguments");

        TestGame testGame = new TestGame(8);
        try {
            testGame.GameSetup();
            testGame.NewGame();
        }
        catch(TestFailed e) { throw e; }

        TestGameClient leader = testGame.players.get(0);
        TestGameClient badPlayer = testGame.players.get(1);

        badPlayer.SendStartGame("BAD");
        if(!badPlayer.ReadResponse().equals("Game BAD doesn't exist! Please enter correct tag or create a new game."))
            throw new TestFailed("Incorrect response to START_GAME with invalid tag");

        badPlayer.SendStartGame(testGame.tag);
        if(!badPlayer.ReadResponse().equals("Only the leader can start the game. Please contact " + leader.p.uname + "."))
            throw new TestFailed("Incorrect response to START_GAME from non-leader");

        leader.SendStartGame(testGame.tag);
        if(!leader.ReadResponse().equals("Can't start the game " + testGame.tag + ", waiting for 3 more players."))
            throw new TestFailed("Incorrect response to START_GAME with not enough players");

        try {
            testGame.JoinGame();
            testGame.StartGame();
        }
        catch(TestFailed e) { throw e; }

        leader.SendStartGame(testGame.tag);
        if(!leader.ReadResponse().equals("Game " + testGame.tag + " has already started! Please create a new game."))
            throw new TestFailed("Incorrect response to START_GAME from leader when game already started");

        wrongArgPlayer.stop();
        testGame.cleanUp();
    }
}
