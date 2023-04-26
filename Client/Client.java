package Client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import Enums.*;
import Client.Utils.*;

public class Client {
    public static final int PORT = 12345;
    public static final String menuPrincipal = "-----------Bienvenue Sur SyncFile-----------\n"+
                                                "1------Connexion\n"+
                                                "2------Inscription\n";
    public AuthActionStatus handleSignup(DataInputStream input_server, DataOutputStream output_server)
    {
        try {
            Utils.clearScreen();
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Veuillez fournir un identifiant : ");
            String id = reader.readLine();
            System.out.println("Veuillez fournir un mot de passe : ");
            String mdp = reader.readLine();
    
            output_server.writeUTF(id+","+mdp);
        } catch (Exception e) {
            System.out.println(e);
        }
        
        
        return AuthActionStatus.SIGNUP_SUCCESS;
    }
    public static void main(String[] args)  {
        try {
            // Connexion au serveur
            Socket socket = new Socket("127.0.0.1", PORT);
            System.out.println("Connected to server");

            // Flux d'entrée/sortie
            DataOutputStream output_server = new DataOutputStream(socket.getOutputStream());
            DataInputStream input_server = new DataInputStream(socket.getInputStream());

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            try {
                System.out.println(menuPrincipal);
                System.out.println("Veuillez choisir une option");
                Integer choix = Integer.parseInt(reader.readLine());
                output_server.writeUTF(choix.toString());
                switch (choix) {
                    case 2:
                        Client C = new Client();
                        C.handleSignup(input_server,output_server);
                        break;
                
                    default:
                        break;
                }



                    
            } catch (Exception e) {
                System.out.println(e);
            }
            
            // Fermeture de la connexion
            socket.close();
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
        }
    }
}
