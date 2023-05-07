package Server.Utils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Date;
import Server.Enums.*;

import Server.ServerApp;





public class Utils {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_WHITE = "\u001B[37m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_WHITE_BACKGROUND = "\u001B[47m";
    public static final String ANSI_GREEN_BACKGROUND = "\u001B[42m";
    public static final char bulletSymbol = '\u2023';

    
    public static void clearScreen() {
        try {
            final String os = System.getProperty("os.name");

            if (os.contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                Runtime.getRuntime().exec("clear");
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    
    public static synchronized  void receiveInitialFiles(String username, String serverPath, String clientPath,
            DataInputStream input_client)
        {
            try {
                int bytes = 0;
        int nb_file = input_client.readInt(); 
        for (int i = 0; i < nb_file; i++) {
            String fileInfo = input_client.readUTF();
            String fileName = fileInfo.split(",")[0];
            String fileDate = fileInfo.split(",")[1];
            String chemin = serverPath + "/" + fileName;
            FileOutputStream fileOutputStream = new FileOutputStream(chemin);

            long size = input_client.readLong(); 
            byte[] buffer = new byte[4 * 1024];

            while (size > 0 &&
                    (bytes = input_client.read(
                            buffer, 0,
                            (int) Math.min(buffer.length, size))) != -1) {
                fileOutputStream.write(buffer, 0, bytes);
                size -= bytes;
            }

            fileOutputStream.close();
            

            Path normalizedClientPath = Paths.get(clientPath + "/" + fileName).normalize();

            addFileToLog(username, normalizedClientPath.toString(), fileDate);
        }

            } catch (Exception e) {
System.out.println(e.getLocalizedMessage());            }
        
    }

    
    public static synchronized  void addFileToLog(String username, String filePath, String dateVersion) throws IOException {
        BufferedReader file = new BufferedReader(new FileReader("./Server/data/users/" + username + "/.logFiles.txt"));
        StringBuffer fileToString = new StringBuffer();
        String userLine;
        Boolean isPresentInFile = false;

        while ((userLine = file.readLine()) != null) {
            String currentPath = userLine.split(",")[0];
            Path curr = Paths.get(currentPath).normalize();
            Path fPath = Paths.get(filePath).normalize();
            if (curr.compareTo(fPath) == 0) {
                fileToString.append(filePath + "," + dateVersion);
                fileToString.append("\n");
                isPresentInFile = true;
            } else {
                fileToString.append(userLine);
                fileToString.append("\n");
            }
        }
        if (!isPresentInFile) {
            fileToString.append(filePath + "," + dateVersion);
            fileToString.append("\n");
        }
        file.close();
        
        FileOutputStream updatedPathFile = new FileOutputStream("./Server/data/users/" + username + "/.logFiles.txt");
        updatedPathFile.write(fileToString.toString().getBytes());
        updatedPathFile.close();

    }

    
    public static synchronized  boolean doesUserHaveRemoteDir_Server(String username, DataInputStream input_client,
            DataOutputStream output_client) {

        try {
            String deviceId = input_client.readUTF();
            Boolean isNewDevice = true;
            
            if(deviceId.equals("N/A"))
            {
                output_client.writeInt(-1);
                return false;
            }
            
            File file = new File("./Server/data/users/" + username + "/.userDevices.txt");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            
            while ((line = br.readLine()) != null) {
                if (line.contains(deviceId)) {
                    isNewDevice = false;
                    break;
                }
            }
            br.close();
            if (isNewDevice) {
                output_client.writeInt(-1);
                return false;
            }
            output_client.writeInt(1);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }
    
    
    public static synchronized  void setUserDirectory(String username, String dirPath, String deviceId) {

        try {
            BufferedReader file = new BufferedReader(new FileReader(ServerApp.usersDirLocations));
            StringBuffer fileToString = new StringBuffer();
            String userLine;
            Boolean isPresentInFile = false;

            while ((userLine = file.readLine()) != null) {
                if (userLine.contains(username)) {
                    userLine += "," + dirPath;
                    isPresentInFile = true;
                }

                fileToString.append(userLine);
                fileToString.append("\n");
            }

            if (!isPresentInFile) {
                fileToString.append(username + "," + dirPath);
                fileToString.append("\n");
            }
            
            FileOutputStream updatedPathFile = new FileOutputStream(ServerApp.usersDirLocations);
            updatedPathFile.write(fileToString.toString().getBytes());
            updatedPathFile.close();
            
            String newRemoteFolderPath = "./Server/data/users/" +
                    username + "/" + Paths.get(dirPath).getFileName().toString() + "/";
            new File(newRemoteFolderPath).mkdirs();
            file.close();

            
            BufferedWriter bw = new BufferedWriter(
                    new FileWriter("./Server/data/users/" + username + "/.userDevices.txt", true));
            PrintWriter out = new PrintWriter(bw);
            out.println(deviceId);
            out.close();

        } catch (Exception e) {
            System.out.println(e);
        }

    }
     
    public static synchronized List<String> getUserDirectories(String username) {
        List<String> userDirs = new ArrayList<String>();
        try {
            BufferedReader file = new BufferedReader(new FileReader(ServerApp.usersDirLocations));
            String userLine;

            while ((userLine = file.readLine()) != null) {
                if (userLine.contains(username)) {
                    String[] elems = userLine.split(",");

                    for (int i = 1; i < elems.length; i++) {
                        userDirs.add(elems[i]);
                    }

                    break;
                }
            }

            file.close();
        } catch (Exception e) {
            System.out.println(e);
        }

        return userDirs;
    }
    
   
    public static synchronized FileStatus serverFileStatus(String username, String fileName, String dirName,
            DataInputStream input_client,
            DataOutputStream output_client) {
        try {
            String localName = dirName + "/" + fileName;
            output_client.writeUTF(localName);
            String dateLocalFile = input_client.readUTF();
            Integer whoIsAhead = 0;

            if (!(dateLocalFile.equals("-NEWFILE-"))) {

                BufferedReader userFilesLog = new BufferedReader(
                        new FileReader("./Server/data/" + "users/" + username + "/.logFiles.txt"));
                String line;

                while ((line = userFilesLog.readLine()) != null) {
                    String path = (line.split(","))[0];
                    String name = (new File(path)).getName();
                    if (name.equals(fileName)) {
                        String dateServer = (line.split(","))[1];

                        Date dateServerFile = (Date) new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.ENGLISH)
                                .parse(dateServer);

                        Date dateClientFile = (Date) new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.ENGLISH)
                                .parse(dateLocalFile);
                        whoIsAhead = dateServerFile.compareTo(dateClientFile);

                        break;

                    }
                }
                userFilesLog.close();
            } else {

                return FileStatus.SERVER_IS_AHEAD;
            }
            
            
            if (whoIsAhead > 0) {
                output_client.writeInt(1);
                return FileStatus.SERVER_IS_AHEAD;

            } else if (whoIsAhead == 0) {
                output_client.writeInt(-1);
                return FileStatus.SAME_VERSION;
            } else {
                output_client.writeInt(-1);
            }

        } catch (Exception e) {
            System.out.println(e);
        }

        return FileStatus.CLIENT_IS_AHEAD;
    }

   
    public static String formatDateTime(FileTime fileTime) {

        LocalDateTime localDateTime = fileTime
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        return localDateTime.format(
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
    }

   
    public static void sendFile(String path, DataOutputStream output) throws Exception {
        int bytes = 0;

        File file = new File(path);
        output.writeUTF(file.getName()); 
        FileInputStream fileInputStream = new FileInputStream(file);

        
        output.writeLong(file.length());
        
        byte[] buffer = new byte[4 * 1024];

        while ((bytes = fileInputStream.read(buffer)) != -1) {
            
            output.write(buffer, 0, bytes);
            output.flush();
        }
        
        fileInputStream.close();

    }
    
    public static void receiveFile(String path, DataInputStream input) throws Exception {
        int bytes = 0;

        String fileName = input.readUTF();
        Path chemin = Paths.get(path + "/" + fileName).normalize();
        File f = new File(chemin.toString());
        f.getParentFile().mkdirs();
        FileOutputStream fileOutputStream = new FileOutputStream(chemin.toString());

        long size = input.readLong(); 
        byte[] buffer = new byte[4 * 1024];

        while (size > 0 &&
                (bytes = input.read(
                        buffer, 0,
                        (int) Math.min(buffer.length, size))) != -1) {
            
            fileOutputStream.write(buffer, 0, bytes);
            size -= bytes; 
        }
        
        System.out.println("File Received");
        fileOutputStream.close();

    }

    
   

    public static synchronized void syncServerWithClient(String username, DataInputStream input_client,
            DataOutputStream output_client) {

        System.out.println("Sync en cours avec l'utilisateur "+username+" ..");
        List<String> userDirs = getUserDirectories(username);
        try {

            output_client.writeInt(userDirs.size());
            for (String dir : userDirs) {
                output_client.writeUTF(dir);
                Integer doesClientHaveDir = input_client.readInt();
                if (doesClientHaveDir == -1) {
                    
                    Path path = Paths.get("./Server/data/users/" + username + "/" + dir).normalize();
                    File curr = new File(path.toString());
                    File[] liste = curr.listFiles();
                    output_client.writeInt(liste.length);
                    for (File f : liste) {
                        sendFile(f.getPath(), output_client);
                    }
                } else {
                    Path dirPath = Paths.get("./Server/data/users/" + username + "/" + dir).normalize();
                    File currDir = new File(dirPath.toString());
                    File[] liste = currDir.listFiles();
                    output_client.writeInt(liste.length);
                    for (File f : liste) {
                        FileStatus fs = serverFileStatus(username, f.getName(), dir, input_client, output_client);
                        output_client.writeUTF(currDir.getName() + "/" + f.getName());

                        if (fs == FileStatus.SERVER_IS_AHEAD) {

                            
                            

                            sendFile(f.getPath(), output_client);
                        }
                    }
                }
            }
            
            Integer numOfDirs = input_client.readInt();
            for (int k = 0; k < numOfDirs; k++) {
                Integer numOfFiles = input_client.readInt();
                for (int j = 0; j < numOfFiles; j++) {
                    Integer doesReiceive = input_client.readInt();
                    if (doesReiceive == 1) {
                        String dirName = input_client.readUTF();
                        Path path = Paths.get("./Server/data/users/" + username + "/" + dirName).normalize();
                        receiveFile(path.toString(), input_client);
                        String filesInfo = input_client.readUTF();

                        addFileToLog(username, filesInfo.split(",")[0], filesInfo.split(",")[1]);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
        }

    }
}