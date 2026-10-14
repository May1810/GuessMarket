import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TestConnection {
    public static void main(String[] args) {
        try {
            // יצירת "לקוח" אינטרנט
            HttpClient client = HttpClient.newHttpClient();

            // הכנת הבקשה לכתובת של השרת שלך
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/GuessMarket/hello"))
                    .build();

            // שליחת הבקשה ושמירת התשובה
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // הדפסת התשובה למסך
            System.out.println("Response from server: " + response.body());

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}