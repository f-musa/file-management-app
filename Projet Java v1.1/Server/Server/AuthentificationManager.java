package Server;

import java.io.*;
import java.util.Base64;
import java.util.Map;
import Server.Enums.*;




public class AuthentificationManager {
    
    

    private static String datafilePath = "./Server/data/authentification/db.txt";

    
    private synchronized static void registerUser(String username, String password) {
        try {
        
            PrintWriter writer = new PrintWriter(new FileOutputStream(new File(datafilePath), true));
            String encodedPassword = Base64.getEncoder().encodeToString(password.getBytes());

            writer.println(username + "," + encodedPassword);
            writer.close();

           
            File file = new File("./Server/data/users/" + username + "/.logFiles.txt");
            file.getParentFile().mkdirs();
            
            file.createNewFile();
            
            file = new File("./Server/data/users/" + username + "/.userDevices.txt");
            file.createNewFile();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

   
    public synchronized AuthActionStatus newAuthAction(String userId, String userPassword,
            AuthAction action, Map<String, User> sessionsDictionnary) {
        try {
           
            File file = new File(datafilePath);

            file.getParentFile().mkdirs();
            file.createNewFile();
            

            String currentLine;
            String data[];
            FileReader fr = new FileReader(datafilePath);

            BufferedReader br = new BufferedReader(fr);

            switch (action) {

                case LOGIN:

                    String encodedPassword = Base64.getEncoder().encodeToString(userPassword.getBytes());

                    while ((currentLine = br.readLine()) != null) {
                        data = currentLine.split(",");

                        
                        if (data[0].equals(userId) && data[1].equals(encodedPassword)) {

                            
                            if (sessionsDictionnary.containsKey(userId)) {
                                
                            } else {
                                sessionsDictionnary.put(
                                        userId,
                                        new User(userId, userPassword, UserState.CONNECTED));
                            }
                            fr.close();
                            br.close();
                            return AuthActionStatus.LOGIN_SUCCESS;
                        }

                        
                        else if (data[0].equals(userId) && !data[1].equals(encodedPassword)) {
                            fr.close();
                            br.close();
                            return AuthActionStatus.INCORRECT_PASSWORD;
                        }

                    }
                    
                    fr.close();
                    br.close();
                    return AuthActionStatus.USER_NOT_FOUND;

                case SIGNUP:
                    
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