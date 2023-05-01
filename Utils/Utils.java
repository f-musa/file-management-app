package Utils;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Date;
import Enums.*;

import Server.AppServer;

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

    // sendFile function define here
    public synchronized static void sendFiles(String dirPath, DataOutputStream output_server) throws Exception {
        int bytes = 0;
        File dir = new File(dirPath);
        File[] liste = dir.listFiles();

        // send the number of file
        output_server.writeInt(liste.length);

        for (File file : liste) {
            String dateLastEdit = formatDateTime(Files.getLastModifiedTime(file.toPath()));
            output_server.writeUTF(file.getName() + "," + dateLastEdit); // send name of file
            FileInputStream fileInputStream = new FileInputStream(file);

            output_server.writeLong(file.length());
            byte[] buffer = new byte[4 * 1024];

            while ((bytes = fileInputStream.read(buffer)) != -1) {
                output_server.write(buffer, 0, bytes);
                output_server.flush();
            }

            fileInputStream.close();
        }

    }

    // receive file function is start here
    public synchronized static void receiveFiles(String username, String serverPath, String clientPath,
            DataInputStream input_client)
            throws Exception {
        int bytes = 0;
        int nb_file = input_client.readInt(); // read number file
        for (int i = 0; i < nb_file; i++) {
            String fileInfo = input_client.readUTF();
            String fileName = fileInfo.split(",")[0];
            String fileDate = fileInfo.split(",")[1];
            String chemin = serverPath + "/" + fileName;
            FileOutputStream fileOutputStream = new FileOutputStream(chemin);

            long size = input_client.readLong(); // read file size
            byte[] buffer = new byte[4 * 1024];

            while (size > 0 &&
                    (bytes = input_client.read(
                            buffer, 0,
                            (int) Math.min(buffer.length, size))) != -1) {
                fileOutputStream.write(buffer, 0, bytes);
                size -= bytes;
            }

            fileOutputStream.close();
            // Add file and date to user log file

            Path normalizedClientPath = Paths.get(clientPath + "/" + fileName).normalize();

            addFileToLog(username, normalizedClientPath.toString(), fileDate);
        }

    }

    public static void addFileToLog(String username, String filePath, String dateVersion) throws IOException {
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
        // We replace the content of the logFile with the updated version
        FileOutputStream updatedPathFile = new FileOutputStream("./Server/data/users/" + username + "/.logFiles.txt");
        updatedPathFile.write(fileToString.toString().getBytes());
        updatedPathFile.close();

    }

    public synchronized static boolean doesUserHaveRemoteDir_Server(String username, DataInputStream input_client,
            DataOutputStream output_client) {

        try {
            File file = new File("./Server/data/users/" + username + "/.userDevices.txt");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            String macAdress = input_client.readUTF();
            Boolean isNewDevice = true;
            while ((line = br.readLine()) != null) {
                if (line.contains(macAdress)) {
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

    public synchronized static boolean doesUserHaveRemoteDir_Client(DataInputStream input_server,
            DataOutputStream output_server, String macAdress) {
        Integer reponse = -1;
        try {
            output_server.writeUTF(macAdress);
            reponse = input_server.readInt();
        } catch (Exception e) {
            System.out.println(e);
        }

        if (reponse == -1)
            return false;
        return true;

    }

    public synchronized static void setUserDirectory(String username, String dirPath, String macAdress) {

        try {
            BufferedReader file = new BufferedReader(new FileReader(AppServer.usersDirLocations));
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
            // We replace the content of dirPath with the updated version
            FileOutputStream updatedPathFile = new FileOutputStream(AppServer.usersDirLocations);
            updatedPathFile.write(fileToString.toString().getBytes());
            updatedPathFile.close();
            // We create the new local directory in the remote directory of the client
            String newRemoteFolderPath = "./Server/data/users/" +
                    username + "/" + Paths.get(dirPath).getFileName().toString() + "/";
            new File(newRemoteFolderPath).mkdirs();
            file.close();

            // We update the device files :
            BufferedWriter bw = new BufferedWriter(
                    new FileWriter("./Server/data/users/" + username + "/.userDevices.txt", true));
            PrintWriter out = new PrintWriter(bw);
            out.println(macAdress);
            out.close();

        } catch (Exception e) {
            System.out.println(e);
        }

    }

    public static synchronized List<String> getUserDirectories(String username) {
        List<String> userDirs = new ArrayList<String>();
        try {
            BufferedReader file = new BufferedReader(new FileReader(AppServer.usersDirLocations));
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

    public static synchronized boolean serverFileStatusResponse(DataInputStream input_server,
            DataOutputStream output_server) {

        Integer response = 0;
        try {
            String filePath = input_server.readUTF();
            File f = new File(Paths.get(filePath).normalize().toString());
            // We send the name path to the file and the date of the last edit
            if (f.exists()) {
                DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                String dateTimeLastEdited = sdf.format(f.lastModified()).toString();
                output_server.writeUTF(dateTimeLastEdited.toString());
                response = input_server.readInt();
            } else {
                output_server.writeUTF("-NEWFILE-");
                response = 1;
            }

        } catch (Exception e) {
            System.out.println(e);
        }

        if (response == 1)
            // The server have the latest version of the file or the file is brand new so i
            // need an update

            return true;
        else
            // The server dont have the file, or have an outdated version so i don't need an
            // update

            return false;
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
            // Si le fichier n'est pas un nouveau fichier on regarde le resultat de la
            // comparaison des dates
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
        output.writeUTF(file.getName()); // send name of file
        FileInputStream fileInputStream = new FileInputStream(file);

        // Here we send the File to Server
        output.writeLong(file.length());
        // Here we break file into chunks
        byte[] buffer = new byte[4 * 1024];

        while ((bytes = fileInputStream.read(buffer)) != -1) {
            // Send the file to Server Socket
            output.write(buffer, 0, bytes);
            output.flush();
        }
        // close the file here
        fileInputStream.close();

    }

    public static void receiveFile(String path, DataInputStream input) throws Exception {
        int bytes = 0;

        String fileName = input.readUTF();
        Path chemin = Paths.get(path + "/" + fileName).normalize();
        File f = new File(chemin.toString());
        f.getParentFile().mkdirs();
        FileOutputStream fileOutputStream = new FileOutputStream(chemin.toString());

        long size = input.readLong(); // read file size
        byte[] buffer = new byte[4 * 1024];

        while (size > 0 &&
                (bytes = input.read(
                        buffer, 0,
                        (int) Math.min(buffer.length, size))) != -1) {
            // Here we write the file using write method
            fileOutputStream.write(buffer, 0, bytes);
            size -= bytes; // read upto file size
        }
        // Here we received file
        System.out.println("File Received");
        fileOutputStream.close();

    }

    public static synchronized void synClientWithServer(DataInputStream input_server, DataOutputStream output_server) {
        try {
            List<Path> filesReiceived = new ArrayList<Path>();
            List<String> localDirs = new ArrayList<String>();
            Integer numDirs = input_server.readInt();
            for (int i = 0; i < numDirs; i++) {
                String dirPath = input_server.readUTF();
                Boolean dirExists = (new File(dirPath)).exists();
                if (!dirExists) {
                    output_server.writeInt(-1);
                    // It's a new directory so we retreive all files of that directory
                    Integer numFiles = input_server.readInt();
                    for (int k = 0; k < numFiles; k++) {
                        receiveFile(dirPath, input_server);

                    }
                } else {
                    localDirs.add(dirPath);
                    output_server.writeInt(1);

                    //
                    Integer numFiles = input_server.readInt();
                    for (int k = 0; k < numFiles; k++) {

                        boolean clientNeedThisFile = serverFileStatusResponse(input_server, output_server);
                        String localFilePath = input_server.readUTF();

                        if (clientNeedThisFile) {
                            receiveFile(dirPath, input_server);
                            filesReiceived.add((Paths.get(localFilePath).toAbsolutePath().normalize()));

                        }
                        // Case when the client have the most recent version of the file
                        
                    }

                }

            }

            // THE CLIENT UPDATE THE SERVER WITH THE NEWEST FILES VERSION
            output_server.writeInt(localDirs.size());
            for (String dir : localDirs) {
                File currDir = new File(Paths.get(dir).normalize().toString());
                File[] files = currDir.listFiles();
                output_server.writeInt(files.length);
                for (File f : files) {
                    Path filePath = Paths.get(f.getAbsolutePath()).normalize();
                    if (filesReiceived.contains(filePath))
                    // We don't need to send this file because we have the latest version
                    {
                        output_server.writeInt(-1);
                    } else {
                        output_server.writeInt(1);
                        output_server.writeUTF(currDir.getName());
                        sendFile(f.getPath(), output_server);
                        DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                        String dateTimeLastEdited = sdf.format(f.lastModified()).toString();

                        output_server.writeUTF(currDir.getName() + "/" + f.getName() + "," + dateTimeLastEdited);
                    }
                }
            }

        } catch (Exception e) {
            System.out.println(e);
        }

    }

    public static synchronized void syncServerWithClient(String username, DataInputStream input_client,
            DataOutputStream output_client) {

        System.out.println("Sync en cours avec un clien t..");
        List<String> userDirs = getUserDirectories(username);
        try {

            output_client.writeInt(userDirs.size());
            for (String dir : userDirs) {
                output_client.writeUTF(dir);
                Integer doesClientHaveDir = input_client.readInt();
                if (doesClientHaveDir == -1) {
                    // Client don't have dir so we send all files from dir
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

                            // The file is either brand new or the client have an outdated version so i send
                            // it to the server

                            sendFile(f.getPath(), output_client);
                        }
                    }
                }
            }
            // GET UPDATES FROM THE CLIENT
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