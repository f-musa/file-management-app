package Server;

import java.io.*;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import Enums.AuthAction;
import Enums.AuthActionStatus;

import Utils.*;


/**
 * <h1>Client Handler</h1>
 * Classe charge de gerer un thread client 
 * <p>
 *
 *
 * @author Moussa Fall
 * @author Serigne Abdou Lat Sarr
 * @author Soulaymane Andich
 * @version 1.0
 * @since 2023-05-01
 */
public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private  User user;
    private Map < String, User > sessionsDictionnary;
    
    
    /**
     * Constructeur de la classe ClientHandler
     * 
     * @param socket le socket asscoié au client pris en charge par le handler
     * @param sessionsDictionnary le dictionnaire des differentes sessions utilisateurs en cours sur le serveur
     * 
     */

    public ClientHandler(Socket socket, Map < String, User > sessionsDictionnary) {
        this.clientSocket = socket;
        // Passing the reference of the sessionsDictionnary
        this.sessionsDictionnary = sessionsDictionnary;
    }
    
    /**
     * Methode qui permet 
     * 
     * 
     * @return String Renvoie l'adresse mac du client.
     */

    public AuthActionStatus handleSignup(DataInputStream input_client, DataOutputStream output_client) {
        try {
            String message = input_client.readUTF();
            AuthentificationManager AM = new AuthentificationManager();
            String credentials[] = message.split(",");
            AuthActionStatus result = AM.newAuthAction(credentials[0], credentials[1], AuthAction.SIGNUP,
                sessionsDictionnary);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    public AuthActionStatus userLogin(String message, DataInputStream input_client, DataOutputStream output_client) {
        try {
            // String message = input_client.readUTF();
            AuthentificationManager AM = new AuthentificationManager();
            String credentials[] = message.split(",");
            AuthActionStatus result = AM.newAuthAction(credentials[0], credentials[1], AuthAction.LOGIN,
                sessionsDictionnary);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String handleLogin(DataInputStream input_client, DataOutputStream output_client) {
        String username = "";
        try {
            Boolean isCredentialsValid = true;
            Integer status;
            do {

                String message = input_client.readUTF();
                System.out.println("In the loop and data received");
                AuthActionStatus result = userLogin(message, input_client, output_client);
                
                switch (result) {
                    case LOGIN_SUCCESS:
                        isCredentialsValid = true;
                        status = 1;
                        output_client.writeUTF(status.toString());

                        System.out.println("connexion réussi");
                        String credentials[] = message.split(",");
                        username = credentials[0];
                        // handleUserFirstLogin
                        if (!Utils.doesUserHaveRemoteDir_Server(credentials[0],input_client,output_client)) {
                            //Send connect state
                            output_client.writeUTF("First");

                            output_client.writeUTF(
                                "C'est votre premiere connexion. Veuillez specifier votre repertoire local:");

                            String infosLogin = input_client.readUTF();
                            String directory = infosLogin.split(",")[0];
                            String macAdress = infosLogin.split(",")[1];
                            
                            Utils.setUserDirectory(username, directory,macAdress);
                            output_client.writeUTF("Votre répertoire local (sur l'appareil "+macAdress+") est " + directory);
                            
                            String serverPath = "./Server/data/users/" + username +"/"+ Paths.get(directory).normalize().toString() + "/";
                            Utils.receiveFiles(username,serverPath,directory, input_client);
                        } else {
                            output_client.writeUTF("NoFirst");
                            output_client.writeUTF("Ceci n'est pas votre première connexion");

                        }
                        break;
                    case INCORRECT_PASSWORD:
                        status = -1;
                        output_client.writeUTF(status.toString());
                        isCredentialsValid = false;

                        System.out.println("Mot de passe incorrect");

                        break;
                    case USER_NOT_FOUND:
                        status = -2;
                        output_client.writeUTF(status.toString());
                        isCredentialsValid = false;

                        System.out.println("Utilisateur non trouve");
                        break;
                    default:
                        break;
                }
                System.out.println(isCredentialsValid.toString());
            } while (!isCredentialsValid);

        } catch (Exception e) {
            System.out.println(e);
        }
        return username;
    }

    @Override
    public void run() {
        try {

            DataInputStream input_client = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream output_client = new DataOutputStream(clientSocket.getOutputStream());

            String choix = input_client.readUTF();

            switch (Integer.parseInt(choix)) {
                case 1: // connexion
                    String username = handleLogin(input_client, output_client);
                    if(username.length()>0)
                    {
                        while(true)
                        {   //TODO ADD GRACEFUL SHUTDOWN
                            Utils.clearScreen();
                            System.out.println(Utils.ANSI_GREEN+Utils.bulletSymbol+"Syncronisation en cours avec le client .."+Utils.ANSI_RESET);

                            //Sync
                            Utils.syncServerWithClient(username,input_client,output_client);
                            //Pour l'affichage
                            Thread.sleep(10000);

                            Utils.clearScreen();
                            System.out.println(Utils.ANSI_GREEN+"Syncronisation terminee !"+Utils.ANSI_RESET);
                            System.out.println(Utils.ANSI_GREEN+"La prochaine syncronisation est dans une minute .."+Utils.ANSI_RESET);
                            Thread.sleep(60000);
                            Utils.clearScreen();    
                        }
                    }
                    break;
                case 2:
                    AuthActionStatus result = handleSignup(input_client, output_client);
                    switch (result) {
                        case ID_ALREADY_EXISTS:
                            System.out.println("utilisateur existe deja");
                            break;
                        case SIGNUP_SUCCESS:
                            System.out.println("inscription réussi !");
                            break;
                        case FAILURE:
                            System.out.println("error");
                            break;
                        default:
                            break;

                    }
                    break;

                default:
                    this.clientSocket.close();
                    break;
            }

        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        }

    }
}