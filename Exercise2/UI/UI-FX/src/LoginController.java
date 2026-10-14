import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    @FXML
    private void onLogin(ActionEvent event) {
        // שולפים את השם מהשדה טקסט
        String name = usernameField.getText() == null ? "" : usernameField.getText().trim();

        if (name.isEmpty()) {
            errorLabel.setText("Username is required.");
            return;
        }

        // מכבים את הכפתור זמנית כדי שלא ילחצו פעמיים
        loginButton.setDisable(true);
        errorLabel.setText("Connecting...");

        // יוצרים "משימת רקע" כדי לשלוח את הבקשה לשרת בלי לתקוע את המסך
        Task<HttpResponse<String>> task = new Task<>() {
            @Override
            protected HttpResponse<String> call() throws Exception {
                String body = "username=" + URLEncoder.encode(name, StandardCharsets.UTF_8);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ServerConfig.BASE + "/login"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            }
        };

        // מה קורה כשהשרת מחזיר לנו תשובה?
        task.setOnSucceeded(e -> {
            loginButton.setDisable(false);
            HttpResponse<String> response = task.getValue();
            int status = response.statusCode();

            if (status == 200) {
                // התחברות צלחה! שומרים את השם ופותחים את המסך הראשי
                Session.userName = name;
                openMainWindow();
            } else {
                // שגיאה (למשל 409) - מציגים את ההודעה מהשרת
                errorLabel.setText(messageFromJson(response.body()));
            }
        });

        // מה קורה אם השרת בכלל לא עובד/מכובה?
        task.setOnFailed(e -> {
            loginButton.setDisable(false);
            errorLabel.setText("Cannot reach the server. Is Tomcat running?");
        });

        // מתחילים את משימת הרקע
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void openMainWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MainLayout.fxml"));
            Parent root = loader.load();

            MainController main = loader.getController();
            main.loadEventsFromServer();
            main.loadUsersFromServer();
            main.startAutoRefresh();

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setTitle("Guess Market - " + Session.userName);
            stage.setOnCloseRequest(e -> main.stopAutoRefresh());
            stage.setScene(new Scene(root, 1100, 700));
        } catch (Exception ex) {
            errorLabel.setText("Could not open main window: " + ex.getMessage());
        }
    }

    // פונקציית עזר קטנה ששולפת את השגיאה מתוך ה-JSON בלי להשתמש עדיין ב-Gson בצד לקוח
    private String messageFromJson(String json) {
        String key = "\"message\":\"";
        int start = json.indexOf(key);
        if (start < 0) {
            return json;
        }
        start += key.length();
        int end = json.indexOf('"', start);
        return end < 0 ? json : json.substring(start, end);
    }
}