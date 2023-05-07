package Client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import Client.Enums.AuthActionStatus;
import Client.Utils.*;


public class ClientApp {
    public static final int PORT = 12345;
    public static final String menuPrincipal = "-----------Bienvenue Sur SyncFile-----------\n" +
            "1------Connexion\n" +
            "2------Inscription\n" +
            "3------Quitter\n";

    
    public String getDeviceId() {
        String id ="N/A";
        try {
            File file = new File("./deviceConfig.txt");
            if(file.exists())
            {
                BufferedReader brTest = new BufferedReader(new FileReader(file));
                id = brTest .readLine();
                brTest.close();
            }
            
        } catch (Exception e) {
            System.out.println(e);
        }
    return id;
    }

    public String registerDeviceId(){
        String deviceId ="";
        try {
            
            deviceId = UUID.randomUUID().toString().replace("-", "");
            File file = new File("./deviceConfig.txt");
            file.createNewFile();
            PrintWriter writer = new PrintWriter(new FileOutputStream(file, false));
            writer.write(deviceId);
            writer.close(); 

        } catch (Exception e) {
            
        }
        return deviceId;
    }

    

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

            String deviceId = registerDeviceId();
            output_server.writeUTF(id + "," + mdp+","+deviceId);
        } catch (Exception e) {
            System.out.println(e);
        }

        return AuthActionStatus.SIGNUP_SUCCESS;
    }
    
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

            

            
            Utils.doesUserHaveRemoteDir_Client(input_server, output_server, getDeviceId());

            String connect_sate = input_server.readUTF();
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            
            
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
               
                
                String deviceId = registerDeviceId();
                output_server.writeUTF(normalizedPath.toString() + "," + deviceId); 
                                                                                          
                                                                                          
                String path_sp = input_server.readUTF(); 
                System.out.println(path_sp);
                System.out.println("111");
                Utils.sendInitialFiles(normalizedPath.toString(), output_server); 
            } else if (connect_sate.equals("NoFirst")) {
                String path_sp = input_server.readUTF();
                System.out.println(path_sp);
            }

        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
        }
        return AuthActionStatus.LOGIN_SUCCESS;
    }

    
    public static void main(String[] args) {
        try {
            
            Socket socket = new Socket("127.0.0.1", PORT);
            System.out.println("Connected to server");

            
            DataOutputStream output_server = new DataOutputStream(socket.getOutputStream());
            DataInputStream input_server = new DataInputStream(socket.getInputStream());

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            ClientApp C = new ClientApp();
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

                            
                            Utils.synClientWithServer(input_server, output_server);
                            
                            Thread.sleep(10000);

                            Utils.clearScreen();
                            System.out.println(Utils.ANSI_GREEN + "Syncronisation terminee ! " + Utils.ANSI_RESET);
                            System.out.println(Utils.ANSI_GREEN + "Prochaine syncronisation dans trente secondes .."
                                    + Utils.ANSI_RESET);
                            Thread.sleep(30000);
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

            
            socket.close();
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
        }
    }
}