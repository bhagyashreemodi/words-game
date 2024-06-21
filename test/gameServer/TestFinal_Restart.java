package test.gameServer;

import test.util.*;
import java.util.Random;
import static test.Lab0CheckpointTests.gPortStr;

/** Tests leader restarting game

    <p>
    This test sets up a game server and players, creates a game, gets players,
    starts the game, then restarts the game in various ways.
 */
public class TestFinal_Restart extends Test {

    /** Test notice. */
    public static final String notice =
        "checking game restarting logic using RESTART";
    /** Prerequisites. */
//    public static final Class[] prerequisites = new Class[] {TestCheckpoint_StartGame.class};

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
        // restart game in a variety of settings
        wrongArgPlayer.send("RESTART\n");
        if(!wrongArgPlayer.ReadResponse().equals("Invalid arguments for command RESTART."))
            throw new TestFailed("Incorrect response to RESTART with invalid arguments");

        TestGame testGame = new TestGame(8);
        try {
            testGame.GameSetup();
            testGame.NewGame();
            testGame.JoinGame();
        }
        catch(TestFailed e) { throw e; }

        TestGameClient player = testGame.players.get(new Random().nextInt(testGame.playerCount - 1) + 1);
        testGame.DisconnectPlayer(player);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        player = new TestGameClient(address, player.p.uname);
        player.SendRestart(testGame.tag);
        if(!player.ReadResponse().equals("New player must always start with HELLO!"))
            throw new TestFailed("Incorrect response to RESTART before HELLO");
        
        player.SendHello();
        testGame.ReconnectPlayer(player);
        if(!player.ReadResponse().equals("Welcome to Word Count "+player.p.uname+"! Resumed Game "+testGame.tag+". Current state is FULL."))
            throw new TestFailed("Incorrect response from player HELLO after disconnect");
        
        player.SendRestart("BAD");
        if(!player.ReadResponse().equals("Game BAD doesn't exist! Please enter correct tag or create a new game."))
            throw new TestFailed("Incorrect response to RESTART with invalid tag");

        player.SendRestart(testGame.tag);
        if(!player.ReadResponse().equals("Only the leader can restart the game. Please contact "+ testGame.leader.p.uname+"."))
            throw new TestFailed("Incorrect response to RESTART from non-leader");

        testGame.Restart();

        testGame.leader.SendJoinGame(testGame.tag);
        if(!testGame.leader.ReadResponse().equals("Game "+testGame.tag+" is full or already in progress. Connect back later."))
            throw new TestFailed("Incorrect response to JOIN_GAME after game restarted");

        wrongArgPlayer.stop();
        testGame.cleanUp();
    }
}

