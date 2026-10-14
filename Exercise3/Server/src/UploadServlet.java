import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

@WebServlet("/upload")
@MultipartConfig // חובה! בלי זה השרת לא יידע לקרוא קבצים שנשלחים אליו
public class UploadServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        Gson gson = new Gson();

        // 1. בודקים מי שלח את הבקשה
        String username = req.getParameter("username");
        if (username == null || username.trim().isEmpty() || !ServerState.hasUser(username.trim())) {
            resp.setStatus(401); // 401 Unauthorized
            resp.getWriter().write(gson.toJson(Map.of("ok", false, "message", "Login required.")));
            return;
        }

        // 2. שולפים את הקובץ מתוך הבקשה
        Part filePart = req.getPart("file");
        if (filePart == null || filePart.getSize() == 0) {
            resp.setStatus(400); // 400 Bad Request
            resp.getWriter().write(gson.toJson(Map.of("ok", false, "message", "Missing file.")));
            return;
        }

        // 3. קוראים את הקובץ לזיכרון בלבד (בלי לשמור על הדיסק!) ומעבירים למנוע
        try {
            byte[] bytes = filePart.getInputStream().readAllBytes();
            int addedCount;
            synchronized (ServerState.ENGINE_LOCK) {
                addedCount = ServerState.engine().addEventsFromXml(new ByteArrayInputStream(bytes), username.trim());
            }

            // אם הגענו לפה, הפונקציה סיימה בהצלחה (לא נזרק Exception)
            resp.setStatus(200);
            resp.getWriter().write(gson.toJson(Map.of(
                    "ok", true,
                    "message", "Uploaded " + addedCount + " events."
            )));

        } catch (IllegalArgumentException e) {
            // אם המנוע זרק שגיאה (כפילות שם, עמלה לא חוקית וכו') - תופסים אותה ומחזירים ללקוח
            resp.setStatus(400);
            resp.getWriter().write(gson.toJson(Map.of("ok", false, "message", e.getMessage())));
        } catch (Exception e) {
            // שגיאות אחרות (למשל JAXBException שלא עטפנו)
            resp.setStatus(400);
            resp.getWriter().write(gson.toJson(Map.of("ok", false, "message", "Error parsing file: " + e.getMessage())));
        }
    }
}