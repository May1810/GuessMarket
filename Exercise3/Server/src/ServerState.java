import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerState {
    private static final Set<String> USERS = ConcurrentHashMap.newKeySet();

    private static final GuessMarketEngine ENGINE = new GuessMarketEngine();

    /** One lock for every servlet that reads or changes the shared engine. */
    public static final Object ENGINE_LOCK = new Object();

    private ServerState() { }

    public static synchronized boolean addUser(String name) {
        return USERS.add(name.toLowerCase());
    }

    public static boolean hasUser(String name) {
        return USERS.contains(name.toLowerCase());
    }

    public static GuessMarketEngine engine() {
        return ENGINE;
    }
}
