package framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Protège une méthode de contrôleur : elle n'est exécutée que si la session du client
 * contient l'attribut "authenticated" égal à true (session.set("authenticated", true)).
 * Sinon, la requête est redirigée (forward) vers /error.jsp.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auth {
}
