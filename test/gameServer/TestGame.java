package test.gameServer;
import test.util.*;
import java.io.File;
import java.util.*;
import static test.Lab0CheckpointTests.gPortStr;

public class TestGame {
    private static String letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int MAX_PLAYERS = 8;
    private static final int MIN_PLAYERS = 4;

    public int playerCount;
    public List<TestGameClient> players;
    public List<TestGameClient> playersInGame;
    public Map<String, PlayerThread> playerThreads;
    public TestGameClient wordSelector;
    public TestGameClient leader;
    public String fileName;
    public long fileSize;
    public String tag;

    public TestGame(int playerCount) {
        players = new ArrayList<>();
        playersInGame = new ArrayList<>();
        playerThreads = new HashMap<>();
        fileName = "test.txt";
        this.playerCount = playerCount;
    }

    public String randSeq(int n) {
        String b = "";
        for (int i = 0; i < n; i++) {
            b += letters.charAt(new Random().nextInt(letters.length()));
        }
        return b;
    }

    protected void GameSetup() throws TestFailed {
        for (int i = 0; i < this.playerCount; i++) {
            // create a new player thread
            PlayerThread t = new PlayerThread("localhost:" + gPortStr, Integer.toString(i));

            playerThreads.put(Integer.toString(i), t);
            players.add(t.player);
            t.player.SendHello();
            if(!t.player.ReadResponse().equals("Welcome to Word Count " + t.player.p.uname + "! Do you want to create a new game or join an existing game?"))
                throw new TestFailed("incorrect response from valid player HELLO");
        }
        leader = players.get(0);
    }

    protected void NewGame() throws TestFailed {
        tag = randSeq(6);
        leader.SendNewGame(tag);
        playersInGame.add(leader);
        String resp = leader.ReadResponse();
        String expectedResponse = "Game " + tag + " created! You are the leader of the game. Waiting for players to join.";
        if(!resp.equals(expectedResponse))
            throw new TestFailed("incorrect response from valid leader NEW_GAME");

        TestGameClient badPlayer = players.get(1);
        badPlayer.SendNewGame(tag);
        String badResp = badPlayer.ReadResponse();
        String expectedErrorResponse = "Game " + tag + " already exists, please provide a new game tag.";
        if(!badResp.equals(expectedErrorResponse))
            throw new TestFailed("incorrect error response from non-leader NEW_GAME");
    }

    protected void JoinGame() throws TestFailed {
        TestGameClient badPlayer = players.get(1);

        // test invalid game tag
        badPlayer.SendJoinGame("BAD");
        String badResp = badPlayer.ReadResponse();
        String expectedErrorResponse = "Game BAD doesn't exist! Please enter correct tag or create a new game.";
        if(!badResp.equals(expectedErrorResponse))
            throw new TestFailed("incorrect error response from invalid tag in JOIN_GAME");

        boolean gameReady = false;

        for (int i = 1; i < players.size(); i++) {
            players.get(i).SendJoinGame(tag);
            String resp = players.get(i).ReadResponse();
            String expectedResponse;

            if (i < MIN_PLAYERS - 1) {
                expectedResponse = "Joined Game "+tag+". Current state is WAITING.";
                playersInGame.add(players.get(i));
            } else if (i == MAX_PLAYERS - 1) {
                playersInGame.add(players.get(i));
                expectedResponse = "Joined Game "+tag+". Current state is FULL.";
                gameReady = true;
            } else if (i >= MAX_PLAYERS) {
                expectedResponse = "Game "+tag+" is full or already in progress. Connect back later.";
                gameReady = true;
            } else {
                expectedResponse = "Joined Game "+tag+". Current state is READY.";
                playersInGame.add(players.get(i));
                gameReady = true;
            }

            if(!resp.equals(expectedResponse))
                throw new TestFailed("incorrect response from valid player JOIN_GAME");
        }

        if (gameReady) {
            if(!leader.ReadResponse().equals("Game "+tag+" is ready to start."))
                throw new TestFailed("incorrect message to leader when game ready after JOIN_GAME");
        }
    }

    protected void StartGame() throws TestFailed {
        leader.SendStartGame(tag);
        if(!leader.ReadResponse().equals("Game " + tag + " is running. Please upload the file."))
            throw new TestFailed("incorrect response from valid leader START_GAME");

        for (int i = 1; i < playersInGame.size(); i++) {
            if(!playersInGame.get(i).ReadResponse().equals("Game "+tag+" is running. Waiting for "+leader.p.uname+" to upload the file."))
                throw new TestFailed("incorrect message to player after leader START_GAME");
        }
    }

    protected void Restart() throws TestFailed {
        leader.SendRestart(tag);
        for (TestGameClient p : playersInGame) {
            if(!p.ReadResponse().equals("New game started!"))
                throw new TestFailed("incorrect message to player when game RESTART");
        }
    }

    protected void LeaderGoodbye() throws TestFailed {
        leader.SendGoodbye();
        for (TestGameClient p : playersInGame) {
            if(!p.ReadResponse().equals("Bye!"))
                throw new TestFailed("incorrect message to player when leader says GOODBYE");
        }

        File f = new File("./" + tag);
        f.mkdirs();
        leader.SendJoinGame(tag);
        String expectedResponse = "Game "+tag+" doesn't exist! Please enter correct tag or create a new game.";
        if(!leader.ReadResponse().equals(expectedResponse))
            throw new TestFailed("incorrect response when leader says GOODBYE then tries to JOIN_GAME");
        f.delete();
    }

    protected void SelecterGoodbye() throws TestFailed {
        // word selector leaves during game
        wordSelector.SendGoodbye();
        if(!wordSelector.ReadResponse().equals("Bye!"))
            throw new TestFailed("incorrect message to selector who says GOODBYE");
        playersInGame.remove(wordSelector);
        players.remove(wordSelector);

        if(!leader.ReadResponse().equals("Player left game. Game "+tag+" is RUNNING."))
            throw new TestFailed("incorrect message to leader when selector says GOODBYE");

        // other player leaves during game
        TestGameClient player = playersInGame.get(1 + new Random().nextInt(playersInGame.size() - 1));
        while (player == wordSelector) {
            player = playersInGame.get(1 + new Random().nextInt(playersInGame.size()));
        }
        player.SendGoodbye();

        String[] expectedResps = {"Bye!", "Upload completed! Please select a word from "+fileName+"."};
        List<String> expectedTitlesList = Arrays.asList(expectedResps);
        if(!expectedTitlesList.contains((player.ReadResponse())))
            throw new TestFailed("incorrect message to player after GOODBYE");
        if(!leader.ReadResponse().equals("Player left game. Game "+tag+" is RUNNING."))
            throw new TestFailed("incorrect message to leader after player GOODBYE");
        playersInGame.remove(player);

        //close the game
        leader.SendClose(tag);
        TestGameClient newSelector = null;
        int selectorCount = 0, playerCount = 0;

        for (TestGameClient p : playersInGame) {
            String playerResp = p.ReadResponse();
            if (playerResp.equals("Bye!"))
                playerCount++;
            else if (playerResp.equals(expectedResps[1])) {
                selectorCount++;
                newSelector = p;
            }
        }

        if((selectorCount != 1) || (playersInGame.size()-1 != playerCount))
            throw new TestFailed("incorrect number of players after GOODBYE");
        if(!newSelector.ReadResponse().equals("Bye!"))
            throw new TestFailed("incorrect message to new selector after player GOODBYE");
    }

    protected void PlayerGoodbye() throws TestFailed {
        TestGameClient player = playersInGame.get(1 + new Random().nextInt(playersInGame.size() - 1));

        player.SendGoodbye();
        playersInGame.remove(player);
        if(!player.ReadResponse().equals("Bye!"))
            throw new TestFailed("incorrect message to player after GOODBYE");

        String expectedResponse;
        if (playersInGame.size() < MIN_PLAYERS) {
            expectedResponse = "Player left game. Game "+tag+" is WAITING.";
        } else {
            expectedResponse = "Player left game. Game "+tag+" is READY.";
        }
        if(!leader.ReadResponse().equals(expectedResponse))
            throw new TestFailed("incorrect leader response after player GOODBYE");
    }

    protected void FileUpload() throws TestFailed {
        int selectWordCount = 0;

        File file = new File("test/" + fileName);
        if (file.exists()) {
            fileSize = file.length();
        }

        leader.SendFileUpload(tag, fileName, fileSize);
        if(!leader.ReadResponse().equals("Upload completed! Waiting for word selection."))
            throw new TestFailed("incorrect leader response after FILE_UPLOAD");

        int waitingCount = 1;
        for (int i = 1; i < playersInGame.size(); i++) {
            String playerResp = playersInGame.get(i).ReadResponse();

            if (playerResp.equals("Upload completed! Waiting for word selection.")) {
                waitingCount += 1;
            } else if (playerResp.equals("Upload completed! Please select a word from "+fileName+".")) {
                selectWordCount += 1;
                this.wordSelector = playersInGame.get(i);
            }
        }

        if((selectWordCount != 1) || (playersInGame.size()-1 != waitingCount))
            throw new TestFailed("incorrect player response count after FILE_UPLOAD");

        leader.SendFileUpload(tag, fileName, fileSize);
        
        if(!leader.ReadResponse().equals("Upload failed! File "+fileName+" already exists for game "+tag+"."))
            throw new TestFailed("incorrect response to duplicate FILE_UPLOAD by leader");
    }

    protected void RandomWord(String word) throws TestFailed {
        wordSelector.SendRandomWord(tag, "BAD");
        if(!wordSelector.ReadResponse().equals("Word BAD is not a valid choice, choose another word."))
            throw new TestFailed("incorrect response to invalid RANDOM_WORD selection");

        wordSelector.SendRandomWord(tag, word);

        for (TestGameClient player : playersInGame) {
            if(!player.ReadResponse().equals("Word selected is "+word+"! Guess the word count."))
                throw new TestFailed("incorrect response to player after RANDOM_WORD selection");
        }
    }

    protected void GuessCount() throws TestFailed {
        int winnerCount = 0, loserCount = 0;
        String winnerResponse = "Congratulations you are the winner!";
        String loserResponse = "Sorry you lose! Better luck next time.";

        Random rand = new Random();

        for (TestGameClient player : playersInGame) {
            int guess = rand.nextInt((int) fileSize);
            player.SendGuessCount(tag, guess);
        }

        for (TestGameClient player : playersInGame) {
            String playerResp = player.ReadResponse();

            if (playerResp.equals(winnerResponse)) {
                winnerCount += 1;
            } else if (playerResp.equals(loserResponse)) {
                loserCount += 1;
            }
        }
        if((winnerCount != 1) || (playersInGame.size()-1 != loserCount))
            throw new TestFailed("incorrect player response count after WORD_COUNT");

        if(!leader.ReadResponse().equals("Game "+tag+" complete. Do you want to restart or close the game?"))
            throw new TestFailed("incorrect response to leader after WORD_COUNT");
    }

    protected void Close() throws TestFailed {
        leader.SendClose(tag);
        for (TestGameClient player : playersInGame) {
            if(!player.ReadResponse().equals("Bye!"))
                throw new TestFailed("incorrect message to player after CLOSE");
        }
    }

    public void DisconnectPlayer(TestGameClient player) {
        playerThreads.get(player.p.uname).player.stop();
        playerThreads.get(player.p.uname).interrupt();
        playersInGame.remove(player);
        playerCount--;
    }

    public void ReconnectPlayer(TestGameClient player) {
        playersInGame.add(player);
        playerCount++;
    }

    public void ClearThreads() {
        for (PlayerThread t : playerThreads.values()) {
            t.player.stop();
        }
    }

    public void cleanUp() throws TestFailed {
        try {
            Close();
        } catch (TestFailed t) {
            throw t;
        }
        ClearThreads();
    }

    static class PlayerThread extends Thread {
        TestGameClient player;

        PlayerThread(String addr, String name) {
            player = new TestGameClient(addr, name);
            start();
        }
    }
}

