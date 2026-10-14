import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name = req.getParameter("username");
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        // בדיקה 1: האם השם ריק?
        if (name == null || name.trim().isEmpty()) {
            resp.setStatus(400);
            resp.getWriter().write(gson.toJson(Map.of("ok", false, "message", "Username is required.")));
            return;
        }

        String trimmed = name.trim();

        // בדיקה 2: האם השם כבר קיים במערכת?
        if (!ServerState.addUser(trimmed)) {
            resp.setStatus(409);
            resp.getWriter().write(gson.toJson(Map.of("ok", false, "message",
                    "Username '" + trimmed + "' is already logged in.")));
            return;
        }

        synchronized (ServerState.ENGINE_LOCK) {
            ServerState.engine().registerUser(trimmed);
        }
        // הכל תקין - התחברות צלחה
        resp.getWriter().write(gson.toJson(Map.of("ok", true, "message", "Welcome " + trimmed)));
    }
}