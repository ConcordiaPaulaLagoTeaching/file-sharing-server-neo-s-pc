package ca.concordia.server;

import ca.concordia.filesystem.FileSystemManager;
import java.io.*;
import java.net.*;

public class FileServer {

    private final FileSystemManager fs;
    private final int port;

    public FileServer(int port, String fsName, int totalSize) throws Exception {
        this.port = port;
        this.fs = new FileSystemManager(fsName, totalSize);
    }
    @SuppressWarnings("resource")
    public void start() throws Exception {
        ServerSocket server = new ServerSocket(port);
        System.out.println("Server listening on " + port);

        while (true) {
            Socket client = server.accept();
            new Thread(new ClientHandler(client, fs)).start();
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket sock;
        private final FileSystemManager fs;

        ClientHandler(Socket s, FileSystemManager fs) {
            this.sock = s;
            this.fs = fs;
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                 PrintWriter out = new PrintWriter(sock.getOutputStream(), true)) {

                String line;
                while ((line = in.readLine()) != null) {

                    String[] parts = line.trim().split(" ", 3);
                    if (parts.length == 0) {
                        out.println("ERROR: malformed command");
                        continue;
                    }

                    String cmd = parts[0].toUpperCase();

                    try {
                        switch (cmd) {
                            case "CREATE":
                                if (parts.length < 2) { out.println("ERROR: malformed command"); break; }
                                fs.createFile(parts[1]);
                                out.println("OK");
                                break;
                                //TODO Implement other commands READ, WRITE, DELETE, LIST

                            case "READ":
                                if (parts.length < 2) { out.println("ERROR: malformed command"); break; }
                                byte[] data = fs.readFile(parts[1]);
                                out.println("OK " + new String(data));
                                break;

                            case "WRITE":
                                if (parts.length < 3) { out.println("ERROR: malformed command"); break; }
                                fs.writeFile(parts[1], parts[2].getBytes());
                                out.println("OK");
                                break;

                            case "DELETE":
                                if (parts.length < 2) { out.println("ERROR: malformed command"); break; }
                                fs.deleteFile(parts[1]);
                                out.println("OK");
                                break;

                            case "LIST":
                                String[] names = fs.listFiles();
                                out.println("OK " + String.join(",", names));
                                break;

                            case "QUIT":
                                out.println("OK closing");
                                return;

                            default:
                                out.println("ERROR: unknown command");
                        }
                    } catch (Exception e) {
                        out.println(e.getMessage());
                    }
                }

            } catch (IOException ignored) {
            } finally {
                try { sock.close(); } catch (Exception ignored) {}
            }
        }
    }
}
