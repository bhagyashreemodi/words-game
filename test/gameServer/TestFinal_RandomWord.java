package test.gameServer;

import test.util.*;
import java.util.Random;
import static test.Lab0CheckpointTests.gPortStr;

/** Tests word selection

    <p>
    This test starts and sets up a game server and players, creates a game, 
    has players join, starts it, uploads a file, then has the selector choose a word.
 */
public class TestFinal_RandomWord extends Test {

    /** Test notice. */
    public static final String notice =
        "checking word selection logic using RANDOM_WORD";
    /** Prerequisites. */
    public static final Class[] prerequisites = new Class[] {TestFinal_FileUpload.class};

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
        // Create and start game, upload file, select random word
        wrongArgPlayer.send("RANDOM_WORD tag\n");
        if(!wrongArgPlayer.ReadResponse().equals("Invalid arguments for command RANDOM_WORD."))
            throw new TestFailed("Incorrect response to RANDOM_WORD with invalid arguments");

        TestGame testGame = new TestGame(5);
        try {
            testGame.GameSetup();
            testGame.NewGame();
            testGame.JoinGame();
            testGame.StartGame();
        }
        catch(TestFailed e) { throw e; }

        // Multi game support
        TestGame secondGame = new TestGame(4);
        for (int i = 0; i < secondGame.playerCount; i++) {
            TestGame.PlayerThread t = new TestGame.PlayerThread(address, Integer.toString(i + 99));
            secondGame.playerThreads.put(Integer.toString(i + 99), t);
            secondGame.players.add(t.player);
            t.player.SendHello();

            if(!t.player.ReadResponse().equals("Welcome to Word Count "+t.player.p.uname+"! Do you want to create a new game or join an existing game?"))
                throw new TestFailed("Incorrect response to HELLO when joining a second game");
        }
        secondGame.leader = secondGame.players.get(0);
        try {
            secondGame.NewGame();
            secondGame.JoinGame();
        }
        catch(TestFailed e) { throw e; }

        TestGameClient secPlayer = secondGame.players.get(new Random().nextInt(secondGame.playerCount - 1) + 1);
        secondGame.DisconnectPlayer(secPlayer);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        secPlayer = new TestGameClient(address, secPlayer.p.uname);
        secPlayer.SendHello();
        if(!secPlayer.ReadResponse().equals("Welcome to Word Count "+secPlayer.p.uname+"! Resumed Game "+secondGame.tag+". Current state is READY."))
            throw new TestFailed("Incorrect response from player HELLO after reconnect to second game");
        secondGame.ReconnectPlayer(secPlayer);

        TestGameClient player = testGame.players.get(new Random().nextInt(testGame.playerCount - 1) + 1);
        testGame.DisconnectPlayer(player);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        testGame.FileUpload();

        player = new TestGameClient(address, player.p.uname);
        player.SendRandomWord(testGame.tag, "random");
        
        if(!player.ReadResponse().equals("New player must always start with HELLO!"))
            throw new TestFailed("Incorrect response to RANDOM_WORD before HELLO");
        
        player.SendHello();
        testGame.ReconnectPlayer(player);
        if(!player.ReadResponse().equals("Welcome to Word Count "+player.p.uname+"! Resumed Game "+testGame.tag+". Current state is RUNNING."))
            throw new TestFailed("Incorrect response from player HELLO after disconnect");

        player.SendRandomWord("BAD", "random");
        if(!player.ReadResponse().equals("Game BAD doesn't exist! Please enter correct tag or create a new game."))
            throw new TestFailed("Incorrect response to RANDOM_WORD with invalid tag");

        player.SendRandomWord(testGame.tag, "random");
        if(!player.ReadResponse().equals("Only the picker can pick the word. Please contact "+ testGame.wordSelector.p.uname+"."))
            throw new TestFailed("Incorrect response to RANDOM_WORD from non-selector");

        testGame.RandomWord("thee");

        // the same word cannot be picked twice
        testGame.wordSelector.SendRandomWord(testGame.tag, "thee");
        if(!testGame.wordSelector.ReadResponse().equals("Word thee is not a valid choice, choose another word."))
            throw new TestFailed("Incorrect response to RANDOM_WORD with duplicate word choice");

        wrongArgPlayer.stop();
        testGame.cleanUp();
        secondGame.cleanUp();
    }
}

