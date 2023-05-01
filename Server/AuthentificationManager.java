package Server;

import java.io.*;
import java.util.Base64;
import java.util.Map;
import Enums.*;

public class AuthentificationManager {
    // Synchronized methods used as multiple threads may want to acess datafile at
    // the same time

    private static String datafilePath = "./Server/data/authentification/db.txt";

    private synchronized static boolean checkUsers(String username, String password) {
        boolean isConnected = false;
        try {
            File file = new File(datafilePath);

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

    private synchronized static void registerUser(String username, String password) {
        try {
        
            PrintWriter writer = new PrintWriter(new FileOutputStream(new File(datafilePath), true));
            String encodedPassword = Base64.getEncoder().encodeToString(password.getBytes());

            writer.println(username + "," + encodedPassword);
            writer.close();

            /* create a folder for the user */
            File file = new File("./Server/data/users/" + username + "/.logFiles.txt");
            file.getParentFile().mkdirs();
            //.logFiles contiendra les noms des fichiers du repertoire et les dernieres dates d'edition
            file.createNewFile();
            //Contiendra la liste des appareils utilisateurs
            file = new File("./Server/data/users/" + username + "/.userDevices.txt");
            file.createNewFile();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public synchronized AuthActionStatus newAuthAction(String userId, String userPassword,
            AuthAction action, Map<String, User> sessionsDictionnary) {
        try {
            /*
             * Open the datafile if exists, create the datafile and its parent folders if
             * not
             */
            File file = new File(datafilePath);

            file.getParentFile().mkdirs();
            file.createNewFile();
            /* */

            String currentLine;
            String data[];
            FileReader fr = new FileReader(datafilePath);

            BufferedReader br = new BufferedReader(fr);

            switch (action) {

                case LOGIN:

                    String encodedPassword = Base64.getEncoder().encodeToString(userPassword.getBytes());

                    while ((currentLine = br.readLine()) != null) {
                        data = currentLine.split(",");

                        // USERS EXISTS AND PASSWORD IS CORRECT
                        if (data[0].equals(userId) && data[1].equals(encodedPassword)) {

                            // we check if the user is already logged in
                            if (sessionsDictionnary.containsKey(userId)) {
                                // TODO (update number of sessions in sessionDictionnary)
                            } else {
                                sessionsDictionnary.put(
                                        userId,
                                        new User(userId, userPassword, UserState.CONNECTED));
                            }
                            fr.close();
                            br.close();
                            return AuthActionStatus.LOGIN_SUCCESS;
                        }

                        // USER EXISTS BUT PASSWORD IS INCORRECT
                        else if (data[0].equals(userId) && !data[1].equals(encodedPassword)) {
                            fr.close();
                            br.close();
                            return AuthActionStatus.INCORRECT_PASSWORD;
                        }

                    }
                    // USER CREDENTIALS ARE INCORRECT
                    fr.close();
                    br.close();
                    return AuthActionStatus.USER_NOT_FOUND;

                case SIGNUP:
                    // USER ALREADY EXISTS
                    System.out.println("Im in the loop");
                    while ((currentLine = br.readLine()) != null) {
                        data = currentLine.split(",");
                        if (data[0].equals(userId)) {
                            fr.close();
                            br.close();
                            return AuthActionStatus.ID_ALREADY_EXISTS;
                        }
                    }
                    fr.close();
                    br.close();
                    // USER DOES NOT EXISTS, we save credentials in db file and add him to the
                    // sessionsDictionnary
                    registerUser(userId, userPassword);
                    sessionsDictionnary.put(
                            userId,
                            new User(userId, userPassword, UserState.CONNECTED));
                    return AuthActionStatus.SIGNUP_SUCCESS;
                default:
                    ;

                    fr.close();
                    br.close();
                    return AuthActionStatus.FAILURE;
            }

        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        }
        return null;

    }
}