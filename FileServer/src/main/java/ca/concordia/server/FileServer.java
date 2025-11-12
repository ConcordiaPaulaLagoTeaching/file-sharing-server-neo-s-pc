package ca.concordia.server;
import ca.concordia.filesystem.FileSystemManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class FileServer {

    private FileSystemManager fsManager;
    private int port;
    public FileServer(int port, String fileSystemName, int totalSize){
       try{
        // Initialize the FileSystemManager
        FileSystemManager fsManager = new FileSystemManager(fileSystemName, totalSize);
        this.fsManager = fsManager;
       } catch (Exception e){
            System.err.println("Error: Could not initialize FileSystemManager!");
            this.fsManager = null;
       }
        this.port = port;
    }

    public void start(){
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Server started. Listening on port 12345...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Handling client: " + clientSocket);
                try (
                        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)
                ) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("Received from client: " + line);
                        String[] parts = line.split(" ");
                        String command = parts[0].toUpperCase();

                        switch (command) {
                            case "CREATE":
                                try{
                                    if (parts.length < 2){
                                       writer.println("ERROR: CREATE requires a file name!");
                                    } else {
                                        fsManager.createFile(parts[1]);
                                        writer.println("SUCCESS: File '" + parts[1] + "' created.");
                                    } 
                                }catch (Exception e) {
                                           writer.println("ERROR: " + e.getMessage());
                                    }
                                writer.flush();
                                break;
                            
                                //TODO: Implement other commands READ, WRITE, DELETE, LIST

                            case "READ":
                                try {
                                    if (parts.length < 2){
                                        writer.println("ERROR: Missing Filename!");
                                    } else {
                                        String result = fsManager.readFile(parts[1]);
                                        writer.println("File Content: " + result);
                                    }
                                    } catch (Exception e) {
                                        writer.println("ERROR: " + e.getMessage());
                                    }
                                    writer.flush();
                                    break;

                            case "WRITE":
                                try {
                                    if (parts.length < 3){
                                    writer.println("ERROR: Missing Filename!");
                                } else {
                                    String filename = parts[1];
                                    String content = parts[2];      //What comes after filename
                                    fsManager.writeFile(filename, content);
                                    writer.println("SUCCESS: File '" + filename + "' written.");
                                }
                            } catch (Exception e){
                                writer.println("ERROR: " + e.getMessage());
                            }
                            writer.flush();
                            break;

                            case "DELETE":
                                try {
                                    if (parts.length < 2) {
                                    writer.println("ERROR: Missing Filename!");
                                } else {
                                    String filename = parts[1];
                                    fsManager.deleteFile(filename);
                                    writer.println("SUCCESS: File '" + filename + "' deleted!");
                                }
                            } catch (Exception e) {
                                writer.println("ERROR: " + e.getMessage());
                            }
                            writer.flush();
                            break;

                            case "LIST":
                                try {
                                    String list = fsManager.listFiles();
                                    writer.println("Files: " + list);
                            } catch (Exception e) {
                                writer.println("ERROR: " + e.getMessage());
                            }
                            writer.flush();
                            break;
                            
                            case "QUIT":
                                writer.println("SUCCESS: Disconnecting.");
                                return;
                            default:
                                writer.println("ERROR: Unknown command.");
                                break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        clientSocket.close();
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Could not start server on port " + port);
        }
    }

}
