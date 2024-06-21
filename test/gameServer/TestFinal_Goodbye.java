package test.gameServer;

import test.util.*;
import java.util.Random;
import static test.Lab0CheckpointTests.gPortStr;

/** Tests players disconnecting from the game server

    <p>
    This test involves various scenarios in which players depart from the game server.
 */
public class TestFinal_Goodbye extends Test {

    /** Test notice. */
    public static final String notice =
        "checking departure logic using GOODBYE";
    /** Prerequisites. */
    public static final Class[] prerequisites = new Class[] {TestFinal_RandomWord.class};

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
        // Players leave the game server in various ways
        wrongArgPlayer.send("GOODBYE invalid\n");
        if(!wrongArgPlayer.ReadResponse().equals("Invalid arguments for command GOODBYE."))
            throw new TestFailed("Incorrect response to GOODBYE with invalid arguments");

        TestGame testGame = new TestGame(6);

        TestGameClient tempPlayer = new TestGameClient(address, "20");
        tempPlayer.SendHello();
        if(!tempPlayer.ReadResponse().equals("Welcome to Word Count "+tempPlayer.p.uname+"! Do you want to create a new game or join an existing game?"))
            throw new TestFailed("Incorrect response to HELLO from new player");

        tempPlayer.SendGoodbye();
        if(!tempPlayer.ReadResponse().equals("Bye!"))
            throw new TestFailed("Incorrect response to GOODBYE from new player");

        tempPlayer.stop();

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
        player.SendGoodbye();
        if(!player.ReadResponse().equals("New player must always start with HELLO!"))
            throw new TestFailed("Incorrect response to GOODBYE before HELLO");

        player.SendHello();
        testGame.ReconnectPlayer(player);
        if(!player.ReadResponse().equals("Welcome to Word Count "+player.p.uname+"! Resumed Game "+testGame.tag+". Current state is READY."))
            throw new TestFailed("Incorrect response from player HELLO after disconnect");

        testGame.PlayerGoodbye();
        testGame.PlayerGoodbye();
        testGame.PlayerGoodbye();

        TestGameClient newPlayer = new TestGameClient(address, "736");
        newPlayer.SendHello();
        if(!newPlayer.ReadResponse().equals("Welcome to Word Count "+newPlayer.p.uname+"! Do you want to create a new game or join an existing game?"))
            throw new TestFailed("Incorrect response from new player HELLO");
        newPlayer.SendJoinGame(testGame.tag);
        if(!newPlayer.ReadResponse().equals("Joined Game "+testGame.tag+". Current state is READY."))
            throw new TestFailed("Incorrect response from new player JOIN_GAME");
        if(!testGame.leader.ReadResponse().equals("Game "+testGame.tag+" is ready to start."))
            throw new TestFailed("Incorrect message to leader when game is READY");

        testGame.ReconnectPlayer(newPlayer);
    
        try {
            testGame.StartGame();
            testGame.FileUpload();
            testGame.LeaderGoodbye();
            testGame.ClearThreads();
        }
        catch(TestFailed e) { throw e; }

        testGame = new TestGame(6);
        try {
            testGame.GameSetup();
            testGame.NewGame();
            testGame.JoinGame();
            testGame.StartGame();
            testGame.FileUpload();

            testGame.RandomWord("thee");
            testGame.SelecterGoodbye();
            testGame.ClearThreads();
        }
        catch(TestFailed e) { throw e; }

        wrongArgPlayer.stop();
        player.stop();
        newPlayer.stop();
        testGame.ClearThreads();
    }
}

