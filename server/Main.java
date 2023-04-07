import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {

        Server server = null;

        try {
            server = new Server(12345, 10);
            // server.setisFinished(false);
            server.manageRequest();

        } catch (Exception E) {
            System.out.println(E);
            E.printStackTrace();
        } finally {
            if (server != null)
                server.getExecutorService().shutdown();
        }
    }
}
