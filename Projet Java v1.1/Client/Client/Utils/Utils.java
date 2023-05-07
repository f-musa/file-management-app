package Client.Utils;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;





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


    public static synchronized  void sendInitialFiles(String dirPath, DataOutputStream output_server)  {
        try {
            
            int bytes = 0;
        File dir = new File(dirPath);
        System.out.println("0");

        File[] liste = dir.listFiles();
        
        output_server.writeInt(liste.length);
        System.out.println("0");

        for (File file : liste) {
            String dateLastEdit = formatDateTime(Files.getLastModifiedTime(file.toPath()));
            output_server.writeUTF(file.getName() + "," + dateLastEdit); 
            FileInputStream fileInputStream = new FileInputStream(file);
            System.out.println("0");

            output_server.writeLong(file.length());
            byte[] buffer = new byte[4 * 1024];
            System.out.println("0");

            while ((bytes = fileInputStream.read(buffer)) != -1) {
                output_server.write(buffer, 0, bytes);
                output_server.flush();
            }

            fileInputStream.close();
        }

    
        } catch (Exception e) 
        {
            System.out.println(e.getLocalizedMessage());
        }
        
    }
    
    

    public synchronized static boolean doesUserHaveRemoteDir_Client(DataInputStream input_server,
            DataOutputStream output_server, String deviceId) {
        Integer reponse = -1;
        try {
            output_server.writeUTF(deviceId);
            reponse = input_server.readInt();
        } catch (Exception e) {
            System.out.println(e);
        }

        if (reponse == -1)
            return false;
        return true;

    }


    public static synchronized boolean serverFileStatusResponse(DataInputStream input_server,
            DataOutputStream output_server) {

        Integer response = 0;
        try {
            String filePath = input_server.readUTF();
            File f = new File(Paths.get(filePath).normalize().toString());
            
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
            
            

            return true;
        else
            
            

            return false;
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
                    
                    Integer numFiles = input_server.readInt();
                    for (int k = 0; k < numFiles; k++) {
                        receiveFile(dirPath, input_server);

                    }
                } else {
                    localDirs.add(dirPath);
                    output_server.writeInt(1);

                    
                    Integer numFiles = input_server.readInt();
                    for (int k = 0; k < numFiles; k++) {

                        boolean clientNeedThisFile = serverFileStatusResponse(input_server, output_server);
                        String localFilePath = input_server.readUTF();

                        if (clientNeedThisFile) {
                            receiveFile(dirPath, input_server);                            
                            filesReiceived.add((Paths.get(localFilePath).toAbsolutePath().normalize()));
                        }
                        
                        
                    }

                }

            }

            
            output_server.writeInt(localDirs.size());
            for (String dir : localDirs) {
                File currDir = new File(Paths.get(dir).normalize().toString());
                File[] files = currDir.listFiles();
                output_server.writeInt(files.length);
                for (File f : files) {
                    Path filePath = Paths.get(f.getAbsolutePath()).normalize();

                    Boolean serverHaveFile = false;
                    for(Path received  : filesReiceived)
                    {
                        if((received.compareTo(filePath)) == 0)
                        {
                            serverHaveFile=true;
                            break;
                        }
                    }
                    if (serverHaveFile)
                    
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
    

}