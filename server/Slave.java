import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class Slave implements Runnable {
    TeacherContainer tbase = new TeacherContainer();

    private Socket socket;

    public Slave(Socket socket) {
        this.socket = socket;
    }

    private static boolean checkUsers(String username, String password) {
        boolean isConnected = false;
        try {
            File file = new File("users.txt");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                String savedUsername = parts[0];
                String savedPassword = parts[1];
                if (username.equals(savedUsername) && password.equals(savedPassword)) {
                    isConnected = true;
                    break;
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isConnected;
    }

    private static void registerUser(String username, String password) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter("users.txt", true));
            bw.write(username + "," + password);
            bw.newLine();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            System.out.println("Voulez-vous vous connecter ou vous inscrire ? (connexion/inscription)");

            boolean isConnected = false;

            ObjectOutputStream output_client = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream input_client = new ObjectInputStream(socket.getInputStream());

            String choix = (String) input_client.readObject();
            System.out.println(choix + " du client !");
            switch (choix) {
                case "connexion": {
                    output_client.writeObject("Veuillez entrer votre nom d'utilisateur:");
                    String users = (String) input_client.readObject();
                    output_client.writeObject("Veuillez entrer votre mot de passe:");
                    String mdp = (String) input_client.readObject();
                    isConnected = checkUsers(users, mdp);

                    if (isConnected) {
                        output_client.writeObject("Connexion réussie !");
                        break;
                    } else {
                        output_client.writeObject("Nom d'utilisateur ou mot de passe incorrect !");
                    }
                }
                    break;
                case "inscription": {
                    output_client.writeObject("Veuillez entrer votre nom d'utilisateur:");
                    String username = (String) input_client.readObject();
                    output_client.writeObject("Veuillez entrer votre mot de passe:");
                    String password = (String) input_client.readObject();
                    registerUser(username, password);
                    // enregistrer les informations de l'utilisateur dans le fichier
                    output_client.writeObject("Inscription réussie !");
                    isConnected = true;
                }
                    break;
                default:
                    output_client.writeObject("Veuillez entrer une commande valide.");

            }

            Thread.sleep(2000);

        } catch (Exception E) {
            System.out.println(E);
            E.printStackTrace();
        }
    }
}
