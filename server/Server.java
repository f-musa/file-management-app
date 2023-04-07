import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    // static List<Teacher> database = new ArrayList<>();

    private ExecutorService pool;
    private int port;
    private int poolSize;
    private boolean isFinished;
    private ServerSocket serverSocket;

    public Server(int port, int poolSize) throws IOException {
        this.port = port;
        this.poolSize = poolSize;
        this.serverSocket = new ServerSocket(port);
        this.pool = Executors.newFixedThreadPool(poolSize);
    }

    public ExecutorService getExecutorService() {
        return this.pool;
    }

    public void setExecutorService(ExecutorService pool) {
        this.pool = pool;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getpoolSize() {
        return this.poolSize;
    }

    public void setpoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public boolean getisFinished() {
        return this.isFinished;
    }

    public void setisFinished(boolean isFinished) {
        this.isFinished = isFinished;
    }

    public void manageRequest() {
        try {
            System.out.println("Waiting for connections..");
            while (true) {
                this.pool.execute(new Slave(serverSocket.accept()));
                System.out.println("Bienvenu dans le serveur !  ");
            }
        } catch (IOException E) {
            E.printStackTrace();
        }

    }
}
