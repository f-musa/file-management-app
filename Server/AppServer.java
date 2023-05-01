package Server;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class AppServer {
    // Users Dictionnary to manage and keep track of the different users sessions
    public static Map<String, User> sessionsDictionnary;
    public static final int NUMPROCS = 4;
    public static final int PORT = 12345;
    private ServerSocket serverSocket;
    private ExecutorService executor;

    public static String usersDirLocations = "./Server/data/users_dir/path.txt";

    public AppServer() throws IOException

    {
        this.serverSocket = new ServerSocket(PORT);
        AppServer.sessionsDictionnary = new HashMap<>();
        this.executor = Executors.newFixedThreadPool(NUMPROCS);
    }

    public void start() {
        Socket client;
        try {
            System.out.println("En attente de connexion ...");
            while (true) {
                client = serverSocket.accept();
                System.out.println("Nouvelle tentative de connexion !");
                this.executor.execute(new ClientHandler(client, sessionsDictionnary));
                System.out.println("Nouvelle Connexion");
            }

        } catch (IOException E) {
            E.printStackTrace();
        }

    }

    public static void main(String arg[]) {
        try {
            AppServer S = new AppServer();
            File file = new File(AppServer.usersDirLocations);
            // if the file path.txt does not exists yet, we create it and return false
            if (!file.isFile()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            S.start();
        } catch (Exception E) {
            E.printStackTrace();
        }
    }
}
