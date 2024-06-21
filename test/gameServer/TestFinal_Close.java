package test.gameServer;

import test.util.*;
import java.util.Random;
import static test.Lab0CheckpointTests.gPortStr;

/** Tests leader closing game

    <p>
    This test sets up a game server and players, creates a game, 
    and has the leader close the game in various ways.
 */
public class TestFinal_Close extends Test {

    /** Test notice. */
    public static final String notice =
        "checking game closing logic using CLOSE";
    /** Prerequisites. */
//    public static final Class[] prerequisites = new Class[] {TestCheckpoint_JoinGame.class};

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
        // Create a game, have players join, end the game
        wrongArgPlayer.send("CLOSE\n");
        if(!wrongArgPlayer.ReadResponse().equals("Invalid arguments for command CLOSE."))
            throw new TestFailed("Incorrect response to CLOSE with invalid arguments");

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
        player.SendClose(testGame.tag);
        if(!player.ReadResponse().equals("New player must always start with HELLO!"))
            throw new TestFailed("Incorrect response to CLOSE before HELLO");
        
        player.SendHello();
        testGame.ReconnectPlayer(player);
        if(!player.ReadResponse().equals("Welcome to Word Count "+player.p.uname+"! Resumed Game "+testGame.tag+". Current state is FULL."))
            throw new TestFailed("Incorrect response from player HELLO after disconnect");        
        
        player.SendClose("BAD");
        if(!player.ReadResponse().equals("Game BAD doesn't exist! Please enter correct tag or create a new game."))
            throw new TestFailed("Incorrect response to CLOSE with invalid tag");

        player.SendClose(testGame.tag);

        if(!player.ReadResponse().equals("Only the leader can close the game. Please contact "+ testGame.leader.p.uname+"."))
            throw new TestFailed("Incorrect response to CLOSE from non-leader");

        testGame.Close();
        testGame.leader.SendJoinGame(testGame.tag);
        if(!testGame.leader.ReadResponse().equals("Game "+testGame.tag+" doesn't exist! Please enter correct tag or create a new game."))
            throw new TestFailed("Incorrect response to JOIN_GAME after game closed");

        wrongArgPlayer.stop();
        player.stop();
        testGame.ClearThreads();
    }
}

