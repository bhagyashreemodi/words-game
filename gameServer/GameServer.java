package gameServer;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The enum represents game state.
 */
enum GameState {

    /**
     * Waiting represents waiting for more players to join.
     */
    WAITING,
    /**
     * Full represents game is full of maximum players.
     */
    FULL,
    /**
     * Ready represents game has minimum required players and ready to start.
     */
    READY,
    /**
     * Running represents leader has started the game.
     */
    RUNNING
}

/**
 * The interface represents contract for the game server.
 */
interface Server {
    /**
     * The constant representing minimum players required to start the game.
     */
    int MIN_PLAYERS = 4;
    /**
     * The constant representing maximum players for a game.
     */
    int MAX_PLAYERS = 8;

    /**
     * The run method to start the server.
     */
    void run();     // interface method, prints errors, no return value

    /**
     * The close method to close the server.
     */
    void close();   // interface method, prints errors, no return value
}


/**
 * Game server class to handle all the game related operations.
 * It implements the Server interface.
 * It provides methods to start and close the server.
 * <p>
 * It also maintains a centralized state of all the games and players.
 * It accepts a connection request from a player and creates a new thread to handle the player.
 */
public class GameServer implements Server {

    /**
     * Port number for the server to listen on
     */
    private final int port;
    /**
     * Setup to accept connections from players.
     */
    private ServerSocket serverSocket;

    /**
     * Thread safe map to maintain all the active games.
     */
    private final ConcurrentHashMap<String, Game> games = new ConcurrentHashMap<>();
    /**
     * Thread safe map to maintain all the player threads and
     * used to identify the player reconnections.
     */
    private final ConcurrentHashMap<String, GameThread> playerThreads = new ConcurrentHashMap<>();

    /**
     * Constructor to create a GameServer
     * @param addr address of the server e.g. localhost:8080
     *             where localhost is the host name and 8080 is the port number
     */
    public GameServer(String addr) {
        String[] splitAddr = addr.split(":", 2);
        this.port = Integer.parseInt(splitAddr[1]);
    }

    /**
     * The entry point of application.
     * @param args the input arguments
     * usage: GameServer [port]
     */
    public static void main(String[] args) {
        // start the new server on port args[0]
        if (args.length != 1) {
            System.out.println("usage: GameServer <port>");
            return;
        }

        GameServer g = new GameServer("localhost:" + args[0]);
        if (g == null) {
            System.out.println("failed to start game server...exiting.");
            return;
        }

        // run the server
        g.run();
    }

    /**
     * Starts the game server and accepts connections from players.
     */
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            while (true) {
                Socket playerSocket = serverSocket.accept();
                GameThread newPlayerThread = new GameThread(playerSocket);
                newPlayerThread.start();
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    /**
     * Shuts down the game server and clears all the active games and players.
     */
    public void close() {
        try {
            serverSocket.close();
            playerThreads.clear();
            games.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Each GameThread supports Socket interaction with a single Player.
     * <p>
     * It maintains the state of the player and handles all the commands from the player.
     */
    private class GameThread extends Thread {
        /**
         * Player object to maintain the state of the player.
         */
        private Player player;
        /**
         * Socket object to maintain the connection with the player.
         */
        private final Socket playerSocket;
        /**
         * Reader to read the input from the player.
         */
        private BufferedReader reader;
        /**
         * Writer to send the output to the player.
         */
        private BufferedWriter writer;

        /**
         * Constructor to create a GameThread
         * @param playerSocket socket object to maintain the connection with the player
         */
        GameThread(Socket playerSocket) {
            this.playerSocket = playerSocket;
        }

        /**
         * Starts the thread and handles all the commands from the player.
         * It also handles the player disconnection.
         */
        public void run() {
            try {
                reader = new BufferedReader((new InputStreamReader(playerSocket.getInputStream())));
                writer = new BufferedWriter(new OutputStreamWriter(playerSocket.getOutputStream()));
                String inputCommand;
                int count = 0;
                while (true) {
                    count++;
                    inputCommand = reader.readLine();
                    if (inputCommand != null) {
                        handle(inputCommand);
                    }
                    else if(count > 100000 && isSocketConnected(playerSocket)){
                        count = 0;
                    }
                    else {
                        handlePlayerDisconnect();
                        break;
                    }
                }
            } catch (IOException e) {
                System.out.println("Exception caught "+e.getMessage());
            }

        }

        /**
         * Checks if the socket is still connected.
         * @param socket socket object to send the heartbeat over the connection with the player
         * @return true if socket is connected else false
         */
        private static boolean isSocketConnected(Socket socket) {
            try {
                OutputStream out = socket.getOutputStream();
                out.write("heartbeat".getBytes());
                out.flush();

                InputStream in = socket.getInputStream();
                byte[] buffer = new byte[9];
                int bytesRead = in.read(buffer);

                return bytesRead == 9;
            } catch (IOException e) {
                return false;
            }
        }

        /**
         * Handles the player disconnection.
         * It removes the player from all the active games and assigns a new leader if the player was a leader.
         * @throws IOException thrown if there is an error in sending the response to the player
         */
        private void handlePlayerDisconnect() throws IOException {
            if(games != null && !games.isEmpty() && player != null) {
                System.out.println("Player  disconnected :"+player.getUsername());
                for(Game game: games.values()) {
                    if(game.isLeader(player.getUsername())) {
                        System.out.println("Leader Player  disconnected"+":"+player.getUsername());
                        game.assignNewLeader();
                    }
                    else {
                        System.out.println("Non Leader Player  disconnected"+":"+player.getUsername());
                        game.getPlayers().remove(player.getUsername());
                        System.out.println("Remaining players: "+game.getPlayers()+" in game "+game.gameId);
                    }
                }
            }
        }

        /**
         * Validates the input command and delegates to the command handler.
         * @param inputCommand command from the player
         * @throws IOException thrown if there is an error in sending the response to the player
         */
        private void handle(String inputCommand) throws IOException {
            System.out.println("Received command:" + inputCommand);
            if (isValid(inputCommand)) {
                invokeCommandHandler(inputCommand);
            } else
                respond("Error! Please send a valid command.");
        }

        /**
         * Invokes the command handler based on the input command.
         * @param inputCommand command from the player
         * @throws IOException thrown if there is an error in sending the response to the player
         */
        private void invokeCommandHandler(String inputCommand) throws IOException {
            String[] fullCommand = inputCommand.split("\\s+");
            switch (fullCommand[0]) {
                case "HELLO":
                    executeHello(fullCommand);
                    break;
                case "NEW_GAME":
                    executeNewGame(fullCommand);
                    break;
                case "CLOSE":
                    executeCloseGame(fullCommand);
                    break;
                case "JOIN_GAME":
                    executeJoinGame(fullCommand);
                    break;
                case "START_GAME":
                    executeStartGame(fullCommand);
                    break;
                case "FILE_UPLOAD":
                    executeFileUpload(fullCommand);
                    break;
                case "RANDOM_WORD":
                    executeRandomWord(fullCommand);
                    break;
                case "WORD_COUNT":
                    executeWordCount(fullCommand);
                    break;
                case "RESTART":
                    executeRestart(fullCommand);
                    break;
                case "GOODBYE":
                    executeGoodbye(fullCommand);
                    break;
                default:
                    respond("Error! Please send a valid command.");
            }
        }

        /**
         * Executes the command GOODBYE.
         * It removes the player from all the active games and closes the connection with the player.
         * @param fullCommand command from the player
         * @throws IOException thrown if there is an error in sending the response to the player
         */
        private void executeGoodbye(String[] fullCommand) throws IOException {
            if(fullCommand.length != 1) {
                respond("Invalid arguments for command " + fullCommand[0] + ".");
            } else if (isNewPlayer()) {
                respond("New player must always start with HELLO!");
            } else {
                System.out.println("Executing goodbye for player "+player.getUsername()+":"+player.getGamesJoined());
                if (player.getGamesJoined() != null && !player.getGamesJoined().isEmpty()) {
                    for (String gameId : player.getGamesJoined()) {
                        if(games.containsKey(gameId)) {
                            Game game = games.get(gameId);
                            if(game.isLeader(player.getUsername())) {
                                System.out.println("Leader Player  disconnected."+game.getPlayers()+":"+player.getUsername());
                                executeCloseGame(new String[]{"CLOSE", gameId});
                                playerThreads.remove(player.getUsername());
                                return;
                            }
                            else if(game.isWordPicker(player.getUsername())) {
                                System.out.println("Word Picker Player saying goodbye:"+player.getUsername());
                                game.getPlayers().remove(player.getUsername());
                                game.assignWordPicker();
                                game.respondLeader("Player left game. Game "+gameId+" is RUNNING.");
                                game.respondWordPicker("Upload completed! Please select a word from "+game.getCurrentlyUploadedFile()+".");
                            }
                            else if (game.isNotInWordPickerState()) {
                                game.removePlayerAndResetState(player.getUsername());
                            }
                            else {
                                game.getPlayers().remove(player.getUsername());
                                game.respondLeader("Player left game. Game "+gameId+" is "+game.getState()+".");
                            }
                        }
                    }
                }
                respond("Bye!");
                playerThreads.remove(player.getUsername());
                reader.close();
                writer.close();
                playerSocket.close();
            }
        }

        /**
         * Executes the command RESTART.
         * It restarts the game if the player is a leader, else it responds with an error.
         * @param fullCommand command from the player
         * @throws IOException thrown if there is an error in sending the response to the player
         */
        private void executeRestart(String[] fullCommand) throws IOException {
            if(fullCommand.length != 2) {
                respond("Invalid arguments for command " + fullCommand[0] + ".");
            } else if (isNewPlayer()) {
                respond("New player must always start with HELLO!");
            } else {
                String gameId = fullCommand[1];
                if(games.containsKey(gameId)) {
                    Game game = games.get(gameId);
                    if(!game.isLeader(player.getUsername())) {
                        respond("Only the leader can restart the game. Please contact "+game.getLeader()+".");
                    }
                    else {
                        game.restart();
                    }
                }
                else {
                    respond("Game " + gameId + " doesn't exist! Please enter correct tag or create a new game.");
                }
            }
        }

        /**
         * Executes the command WORD_COUNT.
         * It adds the player's guess to the game if the player is not a word picker,
         * and calculates the winner if all the players have guessed.
         * @param fullCommand command from the player
         * @throws IOException thrown if there is an error in sending the response to the player
         */
        private void executeWordCount(String[] fullCommand) throws IOException {
            if(fullCommand.length != 3) {
                respond("Invalid arguments for command " + fullCommand[0] + ".");
            } else if (isNewPlayer()) {
                respond("New player must always start with HELLO!");
            } else {
                String gameId = fullCommand[1];
                if(games.containsKey(gameId)) {
                    Game game = games.get(gameId);
                    if(!game.isWordPicked()) {
                        respond("No word has been selected yet for game "+gameId+". Wait!");
                    }
                    else {
                        game.addPlayersGuess(player.getUsername(), Integer.parseInt(fullCommand[2]));
                    }
                }
                else {
                    respond("Game " + gameId + " doesn't exist! Please enter correct tag or create a new game.");
                }

            }
        }

        /**
         * Executes the command RANDOM_WORD.
         * It adds the player's guess to the game if the player is a word picker
         * @param fullCommand command from the player
         * @throws IOException thrown if there is an error in sending the response to the player
         */
        private void executeRandomWord(String[] fullCommand) throws IOException {
            if(fullCommand.length != 3) {
                respond("Invalid arguments for command " + fullCommand[0] + ".");
            } else if (isNewPlayer()) {
                respond("New player must always start with HELLO!");
            } else {
                String gameId = fullCommand[1];
                String randomWord = fullCommand[2];
                if (games.containsKey(gameId)) {
                    Game game = games.get(gameId);
                    if(!game.isFileUploaded()) {
                        respond("No file uploaded. Please contact "+game.getLeader()+".");
                    }
                    else if(!game.isWordPicker(player.getUsername())) {
                        respond("Only the picker can pick the word. Please contact "+game.getWordPicker()+".");
                    }
                    else if(!game.isValidWord(randomWord)) {
                        respond("Word "+randomWord+" is not a valid choice, choose another word.");
                    }
                    else {
                        game.addToPickedWords(randomWord);
                        game.respondPlayers("Word selected is "+randomWord+"! Guess the word count.",false,false);
                    }
                } else {
                    respond("Game " + gameId + " doesn't exist! Please enter correct tag or create a new game.");
                }
            }
        }

        /**
         * Executes the command FILE_UPLOAD.
         * It uploads the file to the game if the player is a leader and the file is not already used in the same game session.
         * @param fullCommand command from the player
         * @throws IOException thrown if there is an error in sending the response to the player
         */
        private void executeFileUpload(String[] fullCommand) throws IOException {
            if(fullCommand.length < 5) {
                respond("Invalid arguments for command " + fullCommand[0] + ".");
            } else if (isNewPlayer()) {
                respond("New player must always start with HELLO!");
            } else {
                String gameId = fullCommand[1];
                String fileName = fullCommand[2];
                if (games.containsKey(gameId)) {
                    Game game = games.get(gameId);
                    if (!game.isLeader(player.getUsername())) {
                        respond("Only the leader can upload the file. Please contact " + game.getLeader() + ".");
                    } else if (game.isFileAlreadyUsed(fileName)) {
                        respond("Upload failed! File " + fileName + " already exists for game "+gameId+".");
                    } else {
                        game.uploadFile(fullCommand);
                    }
                }else {
                    respond("Game " + gameId + " doesn't exist! Please enter correct tag or create a new game.");
                }
            }
        }

        /**
         * Executes the command START_GAME.
         * It starts the game if the player is a leader and the game has minimum required players,
         * else it responds with an error.
         * @param fullCommand command from the player
         * @throws IOException thrown if there is an error in sending the response to the player
         */
        private void executeStartGame(String[] fullCommand) throws IOException {
            if (fullCommand.length != 2) {
                respond("Invalid arguments for command " + fullCommand[0] + ".");
            } else if (isNewPlayer()) {
                respond("New player must always start with HELLO!");
            } else {
                String gameId = fullCommand[1];
                if (games.containsKey(gameId)) {
                    Game game = games.get(gameId);
                    if (game.getState() == GameState.RUNNING) {
                        respond("Game " + gameId + " has already started! Please create a new game.");
                    } else if (!game.isLeader(player.getUsername())) {
                        respond("Only the leader can start the game. Please contact " + game.getLeader() + ".");
                    } else if (game.getPlayers().size() < Server.MIN_PLAYERS) {
                        int requiredPlayers = Server.MIN_PLAYERS - game.getPlayers().size();
                        respond("Can't start the game " + gameId + ", waiting for " +
                                requiredPlayers + " more players.");
                    } else {
                        game.setState(GameState.RUNNING);
                        game.respondLeader("Game " + gameId + " is running. Please upload the file.");
                        game.respondPlayers("Game " + gameId + " is running. Waiting for " + game.getLeader() + " to upload the file.", true, false);
                    }
                } else {
                    respond("Game " + gameId + " doesn't exist! Please enter correct tag or create a new game.");
                }
            }
        }

        /**
         * Executes the command JOIN_GAME.
         * It adds the player to the game if the game is not full or already running,
         * else it responds with an error.
         * @param fullCommand command from the player
         * @throws IOException thrown if there is an error in sending the response to the player
         */
        private void executeJoinGame(String[] fullCommand) throws IOException {
            if (fullCommand.length != 2) {
                respond("Invalid arguments for command " + fullCommand[0] + ".");
            } else if (isNewPlayer()) {
                respond("New player must always start with HELLO!");
            } else {
                String gameId = fullCommand[1];
                if (games.containsKey(gameId)) {
                    Game game = games.get(gameId);
                    if (game.getState() == GameState.RUNNING || game.isFull()) {
                        respond("Game " + gameId + " is full or already in progress. Connect back later.");
                    } else {
                        game.addPlayer(player.getUsername());
                        player.joinGame(gameId);
                        respond("Joined Game " + gameId + ". Current state is " + game.getState() + ".");
                    }
                } else {
                    respond("Game " + gameId + " doesn't exist! Please enter correct tag or create a new game.");
                }
            }
        }

        /**
         * Executes the command CLOSE.
         * It closes the game if the player is a leader, else it responds with an error.
         * It also removes the player from the game and game from the active games.
         * @param fullCommand command from the player
         * @throws IOException thrown if there is an error in sending the response to the player
         */
        private void executeCloseGame(String[] fullCommand) throws IOException {
            if (fullCommand.length != 2) {
                respond("Invalid arguments for command " + fullCommand[0] + ".");
            } else if (isNewPlayer()) {
                respond("New player must always start with HELLO!");
            } else {
                String gameId = fullCommand[1];
                if (games.containsKey(gameId)) {
                    System.out.println("Closing game " + gameId);
                    Game game = games.get(gameId);
                    if (game.isLeader(player.getUsername())) {
                        games.remove(gameId);
                        player.leaveGame(gameId);
                        game.removeGame();
                        game.respondPlayers("Bye!", false, false);
                        System.out.println("Game " + gameId + " closed.");
                    } else {
                        respond("Only the leader can close the game. Please contact " + game.getLeader() + ".");
                    }
                } else {
                    respond("Game " + gameId + " doesn't exist! Please enter correct tag or create a new game.");
                }
            }
        }

        /**
         * Executes the command NEW_GAME.
         * It creates a new game if the game doesn't already exist,
         * else it responds with an error.
         * @param fullCommand command from the player
         * @throws IOException thrown if there is an error in sending the response to the player
         */
        private void executeNewGame(String[] fullCommand) throws IOException {
            if (fullCommand.length != 2) {
                respond("Invalid arguments for command " + fullCommand[0] + ".");
            } else if (isNewPlayer()) {
                respond("New player must always start with HELLO!");
            } else {
                String gameId = fullCommand[1];
                if (games.containsKey(gameId)) {
                    respond("Game " + gameId + " already exists, please provide a new game tag.");
                } else {
                    Game game = new Game(gameId, player.getUsername());
                    games.put(gameId, game);
                    player.joinGame(gameId);
                    respond("Game " + gameId + " created! You are the leader of the game. Waiting for players to join.");
                }
            }
        }

        /**
         * Executes the command HELLO.
         * It registers the player if it's a new player, else it resumes the player's state.
         * It also checks if the player is already in any active games and adds the player back to the game.
         * @param fullCommand command from the player
         * @throws IOException thrown if there is an error in sending the response to the player
         */
        private void executeHello(String[] fullCommand) throws IOException {
            if (fullCommand.length == 1) {
                respond("Invalid user name. Try again.");
            } else if (fullCommand.length > 2) {
                respond("Invalid arguments for command " + fullCommand[0] + ".");
            } else {
                String username = fullCommand[1];
                if (playerThreads.containsKey(username)) {
                    System.out.println("Player " + username + " already exists.");
                    this.player = playerThreads.get(username).getPlayer();
                    playerThreads.put(username, this);
                    List<String> gameIdsToRemove = new ArrayList<>();
                    if (player.getGamesJoined() != null && !player.getGamesJoined().isEmpty()) {
                        System.out.println("Player " + username + " is in games " + player.getGamesJoined());
                        System.out.println("Server Games:"+games.keySet());
                        for (String gameId : player.getGamesJoined()) {
                            if(games.containsKey(gameId)) {
                                Game game = games.get(gameId);
                                System.out.println("Adding player:"+username+" back to the game:"+gameId);
                                game.addPlayer(username);
                                respond("Welcome to Word Count " + username + "! Resumed Game "
                                        + gameId + ". Current state is " + game.getState() + ".");
                            }
                            else {
                                System.out.println("Player " + username + " is not in any active game");
                                gameIdsToRemove.add(gameId);
                                respond("Welcome to Word Count " + username + "! Do you want to create a new game or join an existing game?");
                            }
                        }
                        player.getGamesJoined().removeAll(gameIdsToRemove);
                        return;
                    }
                }
                player = new Player(username);
                playerThreads.put(username, this);
                respond("Welcome to Word Count " + username + "! Do you want to create a new game or join an existing game?");

            }
        }

        /**
         * Player is registered after it's first command as HELLO. This method helps to identify if the player is new.
         * @return true if the player is new else false i.e. player is already registered
         */
        private boolean isNewPlayer() {
            return player == null || !player.isRegistered();
        }

        /**
         * Responds to the player with the message.
         * @param message message to be sent to the player.
         * @throws IOException thrown if there is an error in sending the response to the player.
         */
        private void respond(String message) throws IOException {
            writer.write(message);
            writer.newLine();
            writer.flush();
        }

        /**
         * Validates the input command to not empty or blank.
         * @param input command from the player
         * @return true if the command is valid else false
         * @throws IOException thrown if there is an error in sending the response to the player
         */
        private boolean isValid(String input) throws IOException {
            return input != null && !input.isBlank();
        }

        /**
         * Returns the player object.
         * @return player object
         */
        public Player getPlayer() {
            return player;
        }
    }

    /**
     * Player class to maintains the state of the individual player.
     * It also provides methods to join and leave the game.
     */
    class Player {
        /**
         * Flag to identify if the player is registered.
         */
        private final boolean isRegistered;
        /**
         * Username of the player.
         */
        private final String username;

        /**
         * List of games joined by the player.
         */
        private List<String> gamesJoined;

        /**
         * Constructor to create a Player.
         * It is created after the first HELLO command from the player.
         * @param username username of the player
         */
        public Player(String username) {
            this.username = username;
            isRegistered = true;
        }

        /**
         * Returns the username of the player.
         * @return username of the player
         */
        public String getUsername() {
            return username;
        }

        /**
         * Returns the flag to identify if the player is registered.
         * @return true if the player is registered else false
         */
        public boolean isRegistered() {
            return isRegistered;
        }

        /**
         * Adds the game to the list of games joined by the player.
         * @param gameId game id of the game to be added
         */
        public void joinGame(String gameId) {
            if (gamesJoined == null) {
                gamesJoined = Collections.synchronizedList(new ArrayList<>());
            }
            gamesJoined.add(gameId);
        }

        /**
         * Removes the game from the list of games joined by the player.
         * @param gameId game id of the game to be removed
         */
        public void leaveGame(String gameId) {
            if (gamesJoined != null) {
                System.out.println("Removing game " + gameId + " from player " + username);
                gamesJoined.remove(gameId);
            }
        }

        /**
         * Returns the list of games joined by the player.
         * @return list of games joined by the player
         */
        public List<String> getGamesJoined() {
            return gamesJoined;
        }
    }

    /**
     * Game object keeps track of the game status,
     * and maintains the state of the game.
     * <p>
     * It also provides methods to support behaviour of the game commands.
     */
    class Game {
        /**
         * Game id of the game.
         */
        private final String gameId;
        /**
         * Index of the leader in the list of players.
         */
        private final Integer leader;

        /**
         * Index of the word picker in the list of players. It is chosen randomly.
         */
        private Integer wordPicker;
        /**
         * List of players in the game.
         */
        private final List<String> players;

        /**
         * State of the game.
         */
        private GameState state;

        /**
         * List of files used in the game previously. It is used to avoid uploading the same file again in the same game session.
         * The file at last index is the file currently being used in the running game.
         */
        private final List<String> filesUsed;
        /**
         * List of words picked in the game previously.
         * It is used to avoid picking the same word again in the same game.
         */
        private final List<String> pickedWords;

        /**
         * Word picked by the word picker for the running game.
         */
        private String currentWord;

        /**
         * Count of the word present in the uploaded file picked by the word picker for the running game.
         */
        private Integer currentWordOccurrenceCount = 0;

        /**
         * File content of the file uploaded by the leader for the running game.
         */
        private String currentFileContent;

        /**
         * Map of player's username and corresponding guesses for the running game.
         */
        private ConcurrentHashMap<String, Integer> playerGuesses;

        /**
         * Constructor to create a Game. It is initialized with the leader of the game by NEW_GAME command.
         * It also initializes the list of players, files used and picked words as empty collection.
         * @param gameId game id of the game
         * @param leaderUname username of the leader of the game
         */
        Game(String gameId, String leaderUname) {
            this.gameId = gameId;
            this.leader = 0;
            this.players = Collections.synchronizedList(new ArrayList<>());
            this.filesUsed = new ArrayList<>();
            this.pickedWords = new ArrayList<>();
            this.players.add(leaderUname);
            this.state = GameState.WAITING;
        }

        /**
         * Sends messages to all the players in the game.
         * @param message message to be sent to the players.
         * @param skipLeader flag to skip sending a message to the leader of the game if sent as true.
         * @param skipWordPicker flag to skip sending a message to the word picker of the game if sent as true.
         * @throws IOException thrown if there is an error in sending the response to the player
         */
        public void respondPlayers(String message, boolean skipLeader, boolean skipWordPicker) throws IOException {
            if (players != null && !players.isEmpty()) {
                for (String player : players) {
                    if((skipLeader && player.equals(players.get(leader))) ||
                            (skipWordPicker && wordPicker != null && player.equals(players.get(wordPicker)))) {
                        continue;
                    }
                    else {
                        GameThread playerThread = playerThreads.get(player);
                        playerThread.respond(message);
                    }
                }
            }
        }

        /**
         * Sends message to the leader of the game.
         * @param message message to be sent to the leader.
         * @throws IOException thrown if there is an error in sending the response to the player
         */
        public void respondLeader(String message) throws IOException {
            playerThreads.get(players.get(leader)).respond(message);
        }

        /**
         * Returns the leader of the game.
         * @return leader of the game
         */
        public String getLeader() {
            return players.get(leader);
        }

        /**
         * Checks if the user is the leader of the game.
         * @param username username of the player to be checked
         * @return true if the player is the leader else false
         */
        public boolean isLeader(String username) {
            return players.get(leader).equals(username);
        }

        /**
         * returns the list of players in the game.
         * @return list of the players in the game.
         */
        public List<String> getPlayers() {
            return players;
        }

        /**
         * Adds the player from the game and sets the state of the game as per the rules defined.
         * @param playerUname username of the player to be added
         * @throws IOException thrown if there is an error in sending the response to the player
         */
        public void addPlayer(String playerUname) throws IOException {
            if(!this.players.contains(playerUname)) {
                this.players.add(playerUname);
            }
            if (GameState.WAITING.equals(this.state) && MIN_PLAYERS == players.size()) {
                setState(GameState.READY);
                respondLeader("Game " + gameId + " is ready to start.");
            } else if (!GameState.RUNNING.equals(state) && MAX_PLAYERS == players.size()) {
                setState(GameState.FULL);
            }
        }

        /**
         * Returns the current state of the game.
         * @return current state of the game
         */
        public GameState getState() {
            return state;
        }

        /**
         * Sets the state of the game.
         * @param state state of the game
         */
        public void setState(GameState state) {
            this.state = state;
        }

        /**
         * Checks if the gae is full i.e. has maximum number of players.
         * @return true if the game has maximum number of players i.e. 8 else false
         */
        public boolean isFull() {
            return players != null && MAX_PLAYERS == players.size();
        }

        /**
         * Assigns a new leader to the game, if the previous leader disconnects abruptly.
         * @throws IOException thrown if there is an error in sending the response to the player
         */
        public void assignNewLeader() throws IOException {
            if (players != null && !players.isEmpty()) {
                players.remove(leader.intValue());
                String newLeader = players.get(leader);
                System.out.println("New leader for game "+gameId+" is "+ newLeader);
                playerThreads.get(newLeader).respond("You are the new leader for game "+gameId+"!");
            }
        }

        /**
         * Removes the game from the list maintained by all the players as joined games.
         */
        public void removeGame() {
            if (players != null && !players.isEmpty()) {
                System.out.println("current players "+players);
                for (String player : players) {
                    playerThreads.get(player).getPlayer().leaveGame(gameId);
                }
            }
        }

        /**
         * Uploads the file to the game and responds to all players as per the requirement.
         * @param fullCommand command from the player
         * @throws IOException thrown if there is an error in sending the response to the player
         */
        public void uploadFile(String[] fullCommand) throws IOException {

            StringBuilder fileContent = new StringBuilder();
            for (int i = 4; i < fullCommand.length; i++) {
                fileContent.append(fullCommand[i].toLowerCase()).append(" ");
            }
            System.out.println("File uploaded successfully");
            this.currentFileContent = fileContent.toString();
            String fileName = fullCommand[2];
            this.filesUsed.add(fileName);
            assignWordPicker();
            respondPlayers("Upload completed! Waiting for word selection.",false,true);
            respondWordPicker("Upload completed! Please select a word from "+fileName+".");
        }

        /**
         * Sends message to the word picker of the game.
         * @param message message to be sent to the word picker.
         * @throws IOException thrown if there is an error in sending the response to the player
         */
        private void respondWordPicker(String message) throws IOException {
            if (players != null && !players.isEmpty()) {
                GameThread playerThread = playerThreads.get(players.get(wordPicker));
                playerThread.respond(message);
            }
        }

        /**
         * Assigns a word picker to the game randomly except for the leader.
         */
        private void assignWordPicker() {
            if (players != null && !players.isEmpty()) {
                this.wordPicker = new Random().nextInt(players.size()-1) + 1;
                System.out.println("Word picker for game "+gameId+" is "+ players.get(wordPicker));
            }
        }

        /**
         * Checks if the file is already used in the same game session.
         * @param fileName file name to be checked
         * @return true if the file is already used in the same game session else false
         */
        public boolean isFileAlreadyUsed(String fileName) {
            return filesUsed != null && filesUsed.contains(fileName);
        }

        /**
         * Checks if the file is uploaded by the leader for the running game.
         * @return true if the file is uploaded by the leader for the running game else false
         */
        public boolean isFileUploaded() {
            return currentFileContent != null && !currentFileContent.isEmpty();
        }

        /**
         * Checks if the player is the word picker for the running game.
         * @param username username of the player to be checked
         * @return true if the player is the word picker else false
         */
        public boolean isWordPicker(String username) {
            return players != null && wordPicker != null && players.get(wordPicker).equals(username);
        }

        /**
         * returns the word picker's username for the running game.
         * @return word picker of the game
         */
        public String getWordPicker() {
            return players.get(wordPicker);
        }

        /**
         * Checks if the word picked by the word picker is not used previously and present in the file at least once.
         * @param randomWord selected by the word picker word to be checked
         * @return true if the word is not already picked and is present in the uploaded file else false
         */
        public boolean isValidWord(String randomWord) {
            return !pickedWords.contains(randomWord) && currentFileContent != null && currentFileContent.contains(randomWord.toLowerCase());
        }

        /**
         * Adds the word picked by the word picker to the list of picked words and sets the value to currentWord.
         * It also calculates the count of the word in the uploaded file.
         * @param randomWord selected by the word picker word to be added
         */
        public void addToPickedWords(String randomWord) {
            pickedWords.add(randomWord);
            currentWord = randomWord;

            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(currentWord) + "\\b");
            Matcher matcher = pattern.matcher(currentFileContent);
            while (matcher.find()) {
                currentWordOccurrenceCount++;
            }
            System.out.println("Word selected is "+currentWord+" and available count is "+currentWordOccurrenceCount);
        }

        /**
         * Checks if the word is picked by the word picker for the running game.
         * @return true if the word is picked by the word picker for the running game else false
         */
        public boolean isWordPicked() {
            return currentWord != null && !currentWord.isEmpty();
        }

        /**
         * It adds the player's guess to the game and calculates the winner if all the players have guessed.
         * @param username username of the player
         * @param guess count of the word guessed by the player
         * @throws IOException thrown if there is an error in sending the response to the player
         */
        public void addPlayersGuess(String username, Integer guess) throws IOException {
            if (playerGuesses == null) {
                playerGuesses = new ConcurrentHashMap<>();
            }
            System.out.println("Player " + username + " guessed with count " + guess);
            playerGuesses.put(username, guess);
            if (playerGuesses.size() == players.size()) {
                int minDifference = -1;
                String winner = "";
                for (Map.Entry<String, Integer> playerGuess : playerGuesses.entrySet()) {
                    int diff = Math.abs(currentWordOccurrenceCount - playerGuess.getValue());
                    if (minDifference == -1) {
                        minDifference = diff;
                        winner = playerGuess.getKey();
                    } else if (minDifference > diff) {
                        minDifference = diff;
                        winner = playerGuess.getKey();
                    }
                }
                System.out.println("Player " + winner + " won with difference " + minDifference + " wins!");
                for (String player : players) {
                    if (winner.equals(player))
                        playerThreads.get(player).respond("Congratulations you are the winner!");
                    else
                        playerThreads.get(player).respond("Sorry you lose! Better luck next time.");
                }
                respondLeader("Game " + gameId + " complete. Do you want to restart or close the game?");
            }

        }

        /**
         * Restarts the game by resetting the state of the game.
         * It assigns a new word picker and responds to all the players.
         * @throws IOException thrown if there is an error in sending the response to the player
         */
        public void restart() throws IOException {
            currentWord = null;
            currentWordOccurrenceCount = 0;
            wordPicker = null;
            if(playerGuesses != null)
                playerGuesses.clear();
            assignWordPicker();
            respondPlayers("New game started!",false,false);
        }

        /**
         * returns true if the game is not in word picker state else false
         * @return true if the game is not in word picker state else false
         */
        public boolean isNotInWordPickerState() {
            return wordPicker == null;
        }

        /**
         * Removes the player from the game after a player sends GOODBYE and sets the state of the game.
         * @param username username of the player to be removed
         * @throws IOException thrown if there is an error in sending the response to the player
         */
        public void removePlayerAndResetState(String username) throws IOException {
            if (players != null && !players.isEmpty()) {
                players.remove(username);
            }
            if(players.size() < MIN_PLAYERS) {
                state = GameState.WAITING;
            }
            else if(GameState.WAITING.equals(state) && players.size() == MAX_PLAYERS) {
                state = GameState.FULL;
            }
            else if(GameState.WAITING.equals(state)) {
                state = GameState.READY;
            }
            System.out.println("Player "+username+" left game "+gameId+" state "+state);
            respondLeader("Player left game. Game "+gameId+" is "+state+".");
        }

        /**
         * returns the file name of the file uploaded by the leader for the running game.
         * @return file name of the file uploaded by the leader for the running game
         */
        public String getCurrentlyUploadedFile() {
            return filesUsed.get(filesUsed.size()-1);
        }
    }
}

