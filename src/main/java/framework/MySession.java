package framework;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Session propre au framework : une instance par client, identifiée par le cookie "app-test".
 * Un contrôleur la reçoit en l'ajoutant simplement comme paramètre de sa méthode.
 */
public class MySession {

    // synchronisée : plusieurs requêtes du même client peuvent arriver en parallèle
    private final Map<String, Object> attributes = Collections.synchronizedMap(new HashMap<>());

    public Object get(String key) {
        return attributes.get(key);
    }

    public void set(String key, Object value) {
        attributes.put(key, value);
    }

    public void remove(String key) {
        attributes.remove(key);
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void clear() {
        attributes.clear();
    }
}
