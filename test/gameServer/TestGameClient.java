package test.gameServer;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * Test game client.
 * This game client sends request to and reads responses from GameServer
 */
public class TestGameClient {
    public Player p;
    private BufferedReader bufferedReader;
    private Socket s;
    private BufferedWriter out;

    public TestGameClient(String addr, String name) {
        String[] splitAddr = addr.split(":", 2);

        try {
            s = new Socket(splitAddr[0], Integer.parseInt(splitAddr[1]));

            // Socket ready to use
            out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(s.getInputStream()));
        } catch (UnknownHostException e) {
            System.out.println("Invalid server address: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("I/O error: " + e.getMessage());
        }

        p = new Player(name, s);
    }

    public void stop() {
        try {
            bufferedReader.close();
            out.close();
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends request to server
     *
     * @param msg message to be sent
     */
    protected void send(String msg) {
        try {
            if (s != null) {
                out.write(msg);
                out.flush();
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    protected void SendHello() {
        send("HELLO "+p.uname+"\n");
    }

    protected void SendNewGame(String tag) {
        send("NEW_GAME "+tag+"\n");
    }

    protected void SendJoinGame(String tag) {
        send("JOIN_GAME "+tag+"\n");
    }

    protected void SendStartGame(String tag) {
        send("START_GAME "+tag+"\n");
    }

    protected void SendFileUpload(String tag, String fileName, long fileSize) {
        String file_path = System.getProperty("user.dir") + "/test/" + fileName;

        Scanner myReader;
        try {
            myReader = new Scanner(new File(file_path));

            StringBuilder content = new StringBuilder();
            while (myReader.hasNextLine()) {
                content.append(myReader.nextLine() + " ");
            }
            myReader.close();

            send("FILE_UPLOAD "+tag+" "+fileName+" "+fileSize+" "+content+"\n");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    protected void SendRandomWord(String tag, String randomWord) {
        send("RANDOM_WORD "+tag+" "+randomWord+"\n");
    }

    protected void SendGuessCount(String tag, int guess) {
        send("WORD_COUNT "+tag+" "+guess+"\n");
    }

    protected void SendClose(String tag) {
        send("CLOSE "+tag+"\n");
    }

    protected void SendRestart(String tag) {
        send("RESTART "+tag+"\n");
    }

    protected void SendGoodbye() {
        send("GOODBYE\n");
    }

    /**
     * Reads responses from server
     *
     * @return string representation of the response
     */
    protected String ReadResponse() {
        String responseString = "";

        try {
            responseString = bufferedReader.readLine();
            while (responseString != null && responseString.length() == 0)
                responseString = bufferedReader.readLine();

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return responseString.trim();
    }
}

/**
 * Individual player in the game
 */
class Player {
    public String uname;
    public Socket sock;

    public Player(String name, Socket s) {
        this.uname = name;
        this.sock = s;
    }
}
