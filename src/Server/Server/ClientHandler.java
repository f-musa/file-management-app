package Server;

import java.io.*;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.Map;

import Server.Enums.AuthAction;
import Server.Enums.AuthActionStatus;

import Server.Utils.*;


public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    
    private Map < String, User > sessionsDictionnary;
    
    

    public ClientHandler(Socket socket, Map < String, User > sessionsDictionnary) {
        this.clientSocket = socket;
        
        this.sessionsDictionnary = sessionsDictionnary;
    }
    

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
                AuthActionStatus result = userLogin(message, input_client, output_client);
                
                switch (result) {
                    case LOGIN_SUCCESS:
                        isCredentialsValid = true;
                        status = 1;
                        output_client.writeUTF(status.toString());

                        String credentials[] = message.split(",");
                        username = credentials[0];
                        System.out.println("Nouvelle connexion de "+username+ " !");

                        
                        if (!Utils.doesUserHaveRemoteDir_Server(credentials[0],input_client,output_client)) {
                            
                            output_client.writeUTF("First");

                            output_client.writeUTF(
                                "C'est votre premiere connexion. Veuillez specifier votre repertoire local:");

                            String infosLogin = input_client.readUTF();
                            String directory = infosLogin.split(",")[0];
                            String deviceId = infosLogin.split(",")[1];
                            
                            Utils.setUserDirectory(username, directory,deviceId);
                            output_client.writeUTF("Votre repertoire local (sur l'appareil "+deviceId+") est " + directory);
                            
                            String serverPath = "./Server/data/users/" + username +"/"+ Paths.get(directory).normalize().toString() + "/";
                            Utils.receiveInitialFiles(username,serverPath,directory, input_client);
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
                case 1: 
                    String username = handleLogin(input_client, output_client);
                    if(username.length()>0)
                    {
                        while(true)
                        {   
                            Utils.clearScreen();
                            System.out.println(Utils.ANSI_GREEN+Utils.bulletSymbol+"Syncronisation en cours avec le client .."+Utils.ANSI_RESET);

                            
                            Utils.syncServerWithClient(username,input_client,output_client);
                            
                            Thread.sleep(10000);

                            Utils.clearScreen();
                            System.out.println(Utils.ANSI_GREEN+"Syncronisation terminee !"+Utils.ANSI_RESET);
                            System.out.println(Utils.ANSI_GREEN+"La prochaine syncronisation est dans trente secondes .."+Utils.ANSI_RESET);
                            Thread.sleep(30000);
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
                            System.out.println("inscription reussi !");
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