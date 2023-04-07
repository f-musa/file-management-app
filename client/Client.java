import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class Client {
    public static void main(String[] args) throws ClassNotFoundException {
        try {
            // Connexion au serveur
            Socket socket = new Socket("127.0.0.1", 12345);
            System.out.println("Connected to server");

            // Flux d'entrée/sortie
            ObjectOutputStream output_server = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream input_server = new ObjectInputStream(socket.getInputStream());

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            // choix du client (connexion ou inscription)
            String choix = reader.readLine();
            output_server.writeObject(choix);
            String message1 = (String) input_server.readObject();
            System.out.println(message1);
            String username = reader.readLine();
            output_server.writeObject(username);
            String message2 = (String) input_server.readObject();
            System.out.println(message2);
            String password = reader.readLine();
            output_server.writeObject(password);
            String b = (String) input_server.readObject();
            System.out.println(b);

            // Fermeture de la connexion
            socket.close();
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
        }
    }
}
