import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static Map<String, PrintWriter> clientWriters = new HashMap<>();
    private static Map<Socket, String> clientNames = new HashMap<>();

    public static void main(String[] args) {
        System.out.println("Chat server started...");
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientName;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // 接收用户名
                clientName = in.readLine();
                synchronized (clientWriters) {
                    clientWriters.put(clientName, out);
                    clientNames.put(socket, clientName);
                    broadcastUserList();
                }

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/private ")) {
                        handlePrivateMessage(message);
                    } else {
                        broadcastMessage(clientName + ": " + message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                synchronized (clientWriters) {
                    clientWriters.remove(clientName);
                    clientNames.remove(socket);
                    broadcastUserList();
                }
                broadcastMessage(clientName + " has left the chat.");
            }
        }

        private void handlePrivateMessage(String message) {
            String[] parts = message.split(" ", 3);
            if (parts.length == 3) {
                String targetUser = parts[1];
                String privateMessage = parts[2];
                PrintWriter targetWriter = clientWriters.get(targetUser);
                if (targetWriter != null) {
                    targetWriter.println("Private from " + clientName + ": " + privateMessage);
                } else {
                    out.println("User " + targetUser + " not found.");
                }
            }
        }

        private void broadcastMessage(String message) {
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters.values()) {
                    writer.println(message);
                }
            }
        }

        private void broadcastUserList() {
            synchronized (clientWriters) {
                StringBuilder userListMessage = new StringBuilder("USERLIST ");
                for (String user : clientWriters.keySet()) {
                    userListMessage.append(user).append(",");
                }
                String userList = userListMessage.toString();
                for (PrintWriter writer : clientWriters.values()) {
                    writer.println(userList);
                }
            }
        }
    }
}
