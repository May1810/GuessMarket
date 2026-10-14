import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

@WebServlet("/topup")
public class TopupServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        String username = req.getParameter("username");
        if (username == null || username.trim().isEmpty() || !ServerState.hasUser(username.trim())) {
            resp.setStatus(401);
            resp.getWriter().write(gson.toJson(Map.of("ok", false, "message", "Login required.")));
            return;
        }
        try {
            double amount = Double.parseDouble(req.getParameter("amount").trim());
            synchronized (ServerState.ENGINE_LOCK) {
                ServerState.engine().addCash(username.trim(), amount);
            }
            resp.getWriter().write(gson.toJson(Map.of("ok", true, "message", "ok")));
        } catch (Exception e) {
            resp.setStatus(400);
            resp.getWriter().write(gson.toJson(Map.of("ok", false, "message", e.getMessage())));
        }
    }
}
