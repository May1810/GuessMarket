import com.google.gson.Gson;
import dto.BuyResultDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

@WebServlet("/order")
public class OrderServlet extends HttpServlet {
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
            int eventId = Integer.parseInt(req.getParameter("eventId").trim());
            String optionName = req.getParameter("optionName");
            String side = req.getParameter("side");
            int quantity = Integer.parseInt(req.getParameter("quantity").trim());
            double price = Double.parseDouble(req.getParameter("price").trim());
            BuyResultDTO result;
            synchronized (ServerState.ENGINE_LOCK) {
                result = ServerState.engine().placeOrder(
                        username.trim(), eventId, optionName, side, quantity, price);
            }
            String message = result.getMessage() == null ? "Order processed." : result.getMessage();
            resp.getWriter().write(gson.toJson(Map.of("ok", true, "message", message)));
        } catch (Exception e) {
            resp.setStatus(400);
            String message = e.getMessage() == null ? "Order failed." : e.getMessage();
            resp.getWriter().write(gson.toJson(Map.of("ok", false, "message", message)));
        }
    }
}
