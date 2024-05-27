import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RegisterAndLoginApp extends Application {
    private TextField registerUsernameField;
    private PasswordField registerPasswordField;
    private Button registerButton;

    private TextField loginUsernameField;
    private PasswordField loginPasswordField;
    private Button loginButton;

    private Map<String, String> users = new HashMap<>();

    @Override
    public void start(Stage primaryStage) {
        registerUsernameField = new TextField();
        registerUsernameField.setPromptText("Username");

        registerPasswordField = new PasswordField();
        registerPasswordField.setPromptText("Password");

        registerButton = new Button("Register");
        registerButton.setOnAction(e -> register());

        VBox registerVBox = new VBox(10);
        registerVBox.setPadding(new Insets(20));
        registerVBox.getChildren().addAll(registerUsernameField, registerPasswordField, registerButton);

        loginUsernameField = new TextField();
        loginUsernameField.setPromptText("Username");

        loginPasswordField = new PasswordField();
        loginPasswordField.setPromptText("Password");

        loginButton = new Button("Login");
        loginButton.setOnAction(e -> login(primaryStage)); // 传入 primaryStage

        VBox loginVBox = new VBox(10);
        loginVBox.setPadding(new Insets(20));
        loginVBox.getChildren().addAll(loginUsernameField, loginPasswordField, loginButton);

        TabPane tabPane = new TabPane();
        Tab registerTab = new Tab("Register", registerVBox);
        Tab loginTab = new Tab("Login", loginVBox);
        tabPane.getTabs().addAll(registerTab, loginTab);

        Scene scene = new Scene(tabPane, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Register and Login");
        primaryStage.show();
    }

    private void register() {
        String username = registerUsernameField.getText();
        String password = hashPassword(registerPasswordField.getText());

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", null, "Please enter username and password.");
            return;
        }

        if (users.containsKey(username)) {
            showAlert(Alert.AlertType.ERROR, "Error", null, "Username already exists.");
            return;
        }

        users.put(username, password);
        showAlert(Alert.AlertType.INFORMATION, "Success", null, "User registered successfully.");
    }

    private void login(Stage primaryStage) {
        String username = loginUsernameField.getText();
        String password = hashPassword(loginPasswordField.getText());

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", null, "Please enter username and password.");
            return;
        }

        if (!users.containsKey(username) || !users.get(username).equals(password)) {
            showAlert(Alert.AlertType.ERROR, "Error", null, "Invalid username or password.");
            return;
        }

        showAlert(Alert.AlertType.INFORMATION, "Success", null, "Login successful.");

        // 登录成功后打开聊天窗口
        openChatWindow(username);
    }

    private void openChatWindow(String username) {
        Stage chatStage = new Stage();
        TextField messageField = new TextField();
        TextArea messageArea = new TextArea();
        ListView<String> userList = new ListView<>();
        Button sendButton = new Button("Send");

        VBox chatLayout = new VBox(10);
        chatLayout.setPadding(new Insets(20));
        chatLayout.getChildren().addAll(messageArea, userList, messageField, sendButton);

        Scene chatScene = new Scene(chatLayout, 400, 500);
        chatStage.setScene(chatScene);
        chatStage.setTitle("Chat Window");
        chatStage.show();

        connectToServer(username, messageField, messageArea, userList, sendButton);
    }

    private void connectToServer(String username, TextField messageField, TextArea messageArea, ListView<String> userList, Button sendButton) {
        try {
            Socket socket = new Socket("localhost", 12345);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // 发送用户名到服务器
            out.println(username);

            Set<String> users = ConcurrentHashMap.newKeySet();

            new Thread(() -> {
                String message;
                try {
                    while ((message = in.readLine()) != null) {
                        if (message.startsWith("USERLIST ")) {
                            updateUsers(message.substring(9), users, userList);
                        } else {
                            messageArea.appendText(message + "\n");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            sendButton.setOnAction(e -> sendMessage(messageField, out, messageArea, userList));
            messageField.setOnAction(e -> sendMessage(messageField, out, messageArea, userList));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateUsers(String userListMessage, Set<String> users, ListView<String> userList) {
        String[] userArray = userListMessage.split(",");
        users.clear();
        for (String user : userArray) {
            if (!user.isEmpty()) {
                users.add(user);
            }
        }
        userList.getItems().setAll(users);
    }

    private void sendMessage(TextField messageField, PrintWriter out, TextArea messageArea, ListView<String> userList) {
        String message = messageField.getText();
        if (message != null && !message.trim().isEmpty()) {
            String targetUser = userList.getSelectionModel().getSelectedItem();
            if (targetUser != null) {
                out.println("/private " + targetUser + " " + message);
            } else {
                out.println(message);
            }
            messageArea.appendText("Me: " + message + "\n");
            messageField.clear();
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
