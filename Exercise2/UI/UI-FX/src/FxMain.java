import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class FxMain extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("LoginLayout.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("Guess Market");
        primaryStage.setScene(new Scene(root, 360, 220));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
