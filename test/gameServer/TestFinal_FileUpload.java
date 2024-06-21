package test.gameServer;

import test.util.*;
import java.util.Random;
import static test.Lab0CheckpointTests.gPortStr;

/** Tests uploading a text file

    <p>
    This test starts and sets up a game server and players, creates a game, 
    has players join the game, starts it, and has the leader upload a text file.
 */
public class TestFinal_FileUpload extends Test {

    /** Test notice. */
    public static final String notice =
        "checking file upload logic using FILE_UPLOAD";
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
        // Create a game, start the game, upload text files
        wrongArgPlayer.send("FILE_UPLOAD tag invalid\n");
        if(!wrongArgPlayer.ReadResponse().equals("Invalid arguments for command FILE_UPLOAD."))
            throw new TestFailed("Incorrect response to FILE_UPLOAD with invalid arguments");

        TestGame testGame = new TestGame(8);
        try {
            testGame.GameSetup();
            testGame.NewGame();
            testGame.JoinGame();
        }
        catch(TestFailed e) { throw e; }

        testGame.DisconnectPlayer(testGame.leader);

        TestGameClient tobeLeader = testGame.players.get(1);
        if(!tobeLeader.ReadResponse().equals("You are the new leader for game " + testGame.tag + "!"))
            throw new TestFailed("Incorrect response from expected new leader after leader disconnect");

        TestGameClient wasLeader = new TestGameClient(address, testGame.leader.p.uname);
        testGame.ReconnectPlayer(wasLeader);
        testGame.leader = tobeLeader;

        wasLeader.SendHello();
        if(!wasLeader.ReadResponse().equals("Welcome to Word Count " + wasLeader.p.uname + "! Resumed Game " + testGame.tag + ". Current state is FULL."))
            throw new TestFailed("Incorrect response from previous leader HELLO after reconnect");

        wasLeader.SendStartGame(testGame.tag);
        if(!wasLeader.ReadResponse().equals("Only the leader can start the game. Please contact " + tobeLeader.p.uname + "."))
            throw new TestFailed("Incorrect response from previous leader START_GAME after reconnect");

        try {
            testGame.cleanUp();
        }
        catch(TestFailed e) { throw e; }
        wasLeader.stop();

        testGame = new TestGame(8);
        try {
            testGame.GameSetup();
            testGame.NewGame();
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
        player.SendFileUpload(testGame.tag, testGame.fileName, testGame.fileSize);
        
        if(!player.ReadResponse().equals("New player must always start with HELLO!"))
            throw new TestFailed("Incorrect response to FILE_UPLOAD before HELLO");
        
        player.SendHello();
        testGame.ReconnectPlayer(player);
        if(!player.ReadResponse().equals("Welcome to Word Count "+player.p.uname+"! Resumed Game "+testGame.tag+". Current state is RUNNING."))
            throw new TestFailed("Incorrect response from player HELLO after reconnect");        
        
        player.SendFileUpload("BAD", testGame.fileName, testGame.fileSize);
        if(!player.ReadResponse().equals("Game BAD doesn't exist! Please enter correct tag or create a new game."))
            throw new TestFailed("Incorrect response to FILE_UPLOAD with invalid tag");

        player.SendFileUpload(testGame.tag, testGame.fileName, testGame.fileSize);
        if(!player.ReadResponse().equals("Only the leader can upload the file. Please contact " + testGame.leader.p.uname + "."))
            throw new TestFailed("Incorrect response to FILE_UPLOAD from non-leader");
            
        testGame.FileUpload();

        wrongArgPlayer.stop();
        testGame.cleanUp();
    }
}

