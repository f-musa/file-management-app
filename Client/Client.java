package Client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.Thread.*;
import java.util.List;
import Enums.*;
import Utils.*;

/**
 * <h1>Client</h1>
 * Classe principale de l'application cote client
 * <p>
 *
 *
 * @author Moussa Fall
 * @author Serigne Abdou Lat Sarr
 * @author Soulaymane Andich
 * @version 1.0
 * @since 2023-05-01
 */

public class Client {
    public static final int PORT = 12345;
    public static final String menuPrincipal = "-----------Bienvenue Sur SyncFile-----------\n" +
            "1------Connexion\n" +
            "2------Inscription\n" +
            "3------Quitter\n";

    /**
     * Methode qui renvoie l'adresse Mac de l'appareil client (permet de
     * differencier les differents appareils)
     * 
     * 
     * @return String Renvoie l'adresse mac du client.
     */
    public String getMacAdress() {
        byte[] hardwareAddress = null;
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            NetworkInterface ni = NetworkInterface.getByInetAddress(localHost);
            hardwareAddress = ni.getHardwareAddress();
        } catch (Exception e) {
            System.out.println(e);
        }
        String[] hexadecimal = new String[hardwareAddress.length];
        for (int i = 0; i < hardwareAddress.length; i++) {
            hexadecimal[i] = String.format("%02X", hardwareAddress[i]);
        }
        String macAddress = String.join("-", hexadecimal);
        return macAddress;
    }


    /**
     * Cette fonction gere l'inscription du client 
     * 
     * 
     * @param input_server 
     * @param output_server 
     * @return AuthActionStatus Elle retourne le statut de l'operation d'inscription (Reussi, etc .)
     */
    public AuthActionStatus handleSignup(DataInputStream input_server, DataOutputStream output_server) {
        try {
            Utils.clearScreen();
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            Boolean isCredentialsValid = true;
            String id, mdp;
            do {
                System.out.println("Veuillez fournir un identifiant : ");
                id = reader.readLine();
                System.out.println("Veuillez fournir un mot de passe : ");
                mdp = reader.readLine();
                if (id.length() < 5 || mdp.length() < 5) {
                    System.out.println(
                            "\u001B[31m" + "L'id et le mot de passe doivent faire plus de 5 lettres ! " + "\u001B[0m");
                    System.out.println("\u001B[31m" + "Veuillez fournir des informations valides .." + "\u001B[0m");
                    Thread.sleep(2000);
                    isCredentialsValid = false;
                    Utils.clearScreen();
                } else
                    isCredentialsValid = true;

            } while (!isCredentialsValid);

            output_server.writeUTF(id + "," + mdp);
        } catch (Exception e) {
            System.out.println(e);
        }

        return AuthActionStatus.SIGNUP_SUCCESS;
    }
    /**
     * Cette fonction gere la connexion du client 
     * 
     * 
     * @param input_server 
     * @param output_server 
     * @return AuthActionStatus Elle retourne le statut de l'operation de connexion (Reussi, Mot de passe incorect,etc . )
     */
    
    public AuthActionStatus handleLogin(DataInputStream input_server, DataOutputStream output_server) {
        try {
            Integer status = 1;

            do {

                Utils.clearScreen();
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("Veuillez fournir un identifiant : ");
                String id = reader.readLine();
                System.out.println("Veuillez fournir un mot de passe : ");
                String mdp = reader.readLine();

                output_server.writeUTF(id + "," + mdp);
                status = Integer.parseInt(input_server.readUTF());

                if (status != 1) {
                    System.out.println("\u001B[31m" + "Credentials Invalides ! " + "\u001B[0m");
                    System.out.println("\u001B[31m" + "Veuillez fournir des informations valides .." + "\u001B[0m");
                    Thread.sleep(2000);
                }

            } while (status != 1);

            // handleUserFirstLogin

            // procedure pour verifier si l'appareil se connecte pour la premiere fois
            Utils.doesUserHaveRemoteDir_Client(input_server, output_server, getMacAdress());

            String connect_sate = input_server.readUTF();
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            // on verifie si client se connecte pour la premiere fois, si oui on envoie
            // aussi le contenu du repertoire local
            if (connect_sate.equals("First")) {
                String mess = input_server.readUTF();
                String path;
                Boolean doesDirExists = true;
                do {
                    System.out.println(mess);
                    path = reader.readLine();
                    File dir = new File(path);
                    if (!dir.exists()) {
                        System.out.println(
                                "\u001B[31m" + "Le repertoire que vous avez specifie n'existe pas ! " +
                                        "\u001B[0m");
                        System.out.println(
                                "\u001B[31m" + "Veuillez fournir un chemin correct .. " + "\u001B[0m");
                        doesDirExists = false;
                        Thread.sleep(2000);
                        Utils.clearScreen();
                    } else
                        doesDirExists = true;
                } while (!doesDirExists);
                Path normalizedPath = Paths.get(path).normalize();
                output_server.writeUTF(normalizedPath.toString() + "," + getMacAdress()); // envoie de la mac adress et
                                                                                          // du repertoire specifié au
                                                                                          // serveur
                String path_sp = input_server.readUTF(); //
                System.out.println(path_sp);
                Utils.sendFiles(normalizedPath.toString(), output_server); // recuperation du contenue du dossier local
            } else if (connect_sate.equals("NoFirst")) {
                String path_sp = input_server.readUTF();
                System.out.println(path_sp);
            }

        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
        }
        return AuthActionStatus.LOGIN_SUCCESS;
    }

    /**
     * Cette fonction est le main, le point d'entree de l'application client 
     * 
     * 
     * @param args 
     */
    public static void main(String[] args) {
        try {
            // Connexion au serveur
            Socket socket = new Socket("127.0.0.1", PORT);
            System.out.println("Connected to server");

            // Flux d'entrée/sortie
            DataOutputStream output_server = new DataOutputStream(socket.getOutputStream());
            DataInputStream input_server = new DataInputStream(socket.getInputStream());

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            Client C = new Client();
            try {
                System.out.println(menuPrincipal);
                System.out.println("Veuillez choisir une option");
                Integer choix = Integer.parseInt(reader.readLine());
                output_server.writeUTF(choix.toString());
                switch (choix) {
                    case 1:
                        C.handleLogin(input_server, output_server);

                        while (true) {
                            Utils.clearScreen();
                            System.out.println(Utils.ANSI_GREEN + Utils.bulletSymbol
                                    + "Syncronisation en cours avec le serveur .." + Utils.ANSI_RESET);

                            // Sync
                            Utils.synClientWithServer(input_server, output_server);
                            // Pour l'affichage
                            Thread.sleep(10000);

                            Utils.clearScreen();
                            System.out.println(Utils.ANSI_GREEN + "Syncronisation terminee ! " + Utils.ANSI_RESET);
                            System.out.println(Utils.ANSI_GREEN + "Prochaine syncronisation dans une minute .."
                                    + Utils.ANSI_RESET);
                            Thread.sleep(60000);
                        }

                    case 2:
                        C.handleSignup(input_server, output_server);
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