package Server;
import java.io.*;
import java.net.Socket;
import java.util.Map;

import Enums.AuthAction;
import Enums.AuthActionStatus;



public class ClientHandler implements Runnable {


    private final Socket clientSocket;
    private Server.User user;
    private Map < String, User > sessionsDictionnary;
    
    public ClientHandler(Socket socket,Map < String, User > sessionsDictionnary) {
        this.clientSocket = socket;
        //Passing the reference of the sessionsDictionnary
        this.sessionsDictionnary = sessionsDictionnary;
    }
    public AuthActionStatus handleSignup(DataInputStream input_client,DataOutputStream output_client)
    {
        try {
            String message = input_client.readUTF();
            AuthentificationManager AM = new AuthentificationManager();
            String credentials []= message.split(",");
            AuthActionStatus result = AM.newUserSession(credentials[0], credentials[1], AuthAction.SIGNUP, sessionsDictionnary);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return AuthActionStatus.SIGNUP_SUCCESS;
    }

    @Override
    public void run() {
        try {
            DataInputStream input_client = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream output_client = new DataOutputStream(clientSocket.getOutputStream());
            String choix = input_client.readUTF();
            switch (Integer.parseInt(choix)) {
                case 2:
                    handleSignup(input_client, output_client);
                    break;
            
                default:
                    break;
            }
        
        
        
        
        
    
        
        
        
        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        }
        
    }
}