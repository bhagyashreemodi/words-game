package test.gameServer;

import test.util.*;
import java.util.Random;
import static test.Lab0CheckpointTests.gPortStr;

/** Tests players guessing word counts

    <p>
    This test sets up a game server and players, creates a game, has players join,
    starts it, gets a file, gets a word, then has players guess the word count.
 */
public class TestFinal_GuessCount extends Test {

    /** Test notice. */
    public static final String notice =
        "checking word count guessing logic using WORD_COUNT";
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
        // Run everything up to and including word count guessing
        wrongArgPlayer.send("WORD_COUNT tag\n");
        if(!wrongArgPlayer.ReadResponse().equals("Invalid arguments for command WORD_COUNT."))
            throw new TestFailed("Incorrect response to WORD_COUNT with invalid arguments");

        TestGame testGame = new TestGame(8);
        try {
            testGame.GameSetup();
            testGame.NewGame();
        }
        catch(TestFailed e) { throw e; }

        testGame.leader.send("INVALID \n");
        if(!testGame.leader.ReadResponse().equals("Error! Please send a valid command."))
            throw new TestFailed("Incorrect response to invalid command");

        try {
            testGame.JoinGame();
            testGame.StartGame();
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
        player.SendGuessCount(testGame.tag, 0);
        if(!player.ReadResponse().equals("New player must always start with HELLO!"))
            throw new TestFailed("Incorrect response to WORD_COUNT before HELLO");
        
        player.SendHello();
        testGame.ReconnectPlayer(player);
        if(!player.ReadResponse().equals("Welcome to Word Count "+player.p.uname+"! Resumed Game "+testGame.tag+". Current state is RUNNING."))
            throw new TestFailed("Incorrect response from player HELLO after disconnect");

        testGame.FileUpload();

        player.SendGuessCount("BAD", 0);
        if(!player.ReadResponse().equals("Game BAD doesn't exist! Please enter correct tag or create a new game."))
            throw new TestFailed("Incorrect response to WORD_COUNT with invalid tag");

        player.SendGuessCount(testGame.tag, 0);
        if(!player.ReadResponse().equals("No word has been selected yet for game "+testGame.tag+". Wait!"))
            throw new TestFailed("Incorrect response to WORD_COUNT with no selected word");

        testGame.RandomWord("thee");
        testGame.GuessCount();

        wrongArgPlayer.stop();
        testGame.cleanUp();
    }
} 

