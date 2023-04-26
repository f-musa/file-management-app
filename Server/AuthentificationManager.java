package Server;

import java.io.*;
import java.util.Base64;
import java.util.Map;
import Enums.*;


public class AuthentificationManager {
    //Synchronized methods used as multiple threads may want to acess datafile at the same time

    private static String datafilePath = "./Server/data/authentification/db.txt";

    
    
    private synchronized static void registerUser(String username, String password) {
        try {
            System.out.println("Im in register user");
            PrintWriter writer = new PrintWriter(datafilePath, "UTF-8");
            String encodedPassword = Base64.getEncoder().encodeToString(password.getBytes());
            
            writer.println(username + "," + encodedPassword);
            writer.close();
            
            /* create a folder for the user  */
            File file = new File("data/users/"+username+"/init.txt");
            file.getParentFile().mkdirs();
            //file.createNewFile();

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public synchronized AuthActionStatus newUserSession(String userId, String userPassword, 
                        AuthAction userAction, Map < String, User > sessionsDictionnary) {
        try {
            System.out.println("Hello from Auth Manager");
            System.out.println("Hello from Auth Manager 2 ");
            /* Open the datafile if exists, create the datafile and its parent folders if not */
            File file = new File(datafilePath);
            
            file.getParentFile().mkdirs();
            file.createNewFile();
            /* */
            
            String currentLine;
            String data[];
            FileReader fr = new FileReader(datafilePath);
            
            BufferedReader br = new BufferedReader(fr);
            
            switch (userAction) {

                case LOGIN:

                    String encodedPassword = Base64.getEncoder().encodeToString(userPassword.getBytes());

                    while ((currentLine = br.readLine()) != null) {
                        data = currentLine.split(",");
                        
                        //USERS EXISTS AND PASSWORD IS CORRECT
                        if (data[0].equals(userId) && data[1].equals(encodedPassword)) {
                            // we check if the user is already logged in
                            if (sessionsDictionnary.containsKey(userId)) {
                                //TODO (update number of sessions in sessionDictionnary)
                            } else {
                                sessionsDictionnary.put(
                                    userId,
                                    new User(userId, userPassword, UserState.CONNECTED)
                                );
                            }
                            fr.close();
                            br.close();
                            return AuthActionStatus.LOGIN_SUCCESS;
                        }

                        //USER EXISTS BUT PASSWORD IS INCORRECT
                        else
                        if (data[0].equals(userId) && !data[1].equals(encodedPassword)) {
                            fr.close();
                            br.close();
                            return AuthActionStatus.INCORRECT_PASSWORD;
                        }

                    }
                    //USER CREDENTIALS ARE INCORRECT
                    fr.close();
                    br.close();
                    return AuthActionStatus.USER_NOT_FOUND;

                case SIGNUP:
                    //USER ALREADY EXISTS
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
                    //USER DOES NOT EXISTS, we save credentials in db file and add him to the sessionsDictionnary
                    registerUser(userId, userPassword);
                    sessionsDictionnary.put(
                        userId,
                        new User(userId, userPassword, UserState.CONNECTED)
                    );
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