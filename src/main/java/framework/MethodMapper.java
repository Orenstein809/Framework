package framework;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;

import jakarta.servlet.http.HttpServletRequest;

public class MethodMapper {

    /**
     * Reçoit la méthode à exécuter et la requête HTTP.
     * Construit le tableau d'objets (arguments) prêts pour method.invoke().
     *
     * Deux cas pour chaque paramètre de la méthode :
     *  - type simple (String, int, double, boolean...) : la valeur est lue par le NOM du paramètre ;
     *  - objet (ex: Employe) : l'objet est instancié puis chacun de ses attributs
     *    est rempli depuis la requête (voir populateObject).
     *
     * NB : le projet client doit être compilé avec l'option "-parameters" pour que
     * Reflection renvoie les vrais noms de paramètres (sinon arg0, arg1...).
     */
    public static Object[] resolveParameters(Method method, HttpServletRequest req) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> type = param.getType();
            String paramName = param.getName();

            if (isSimpleType(type)) {
                args[i] = convertValue(req.getParameter(paramName), type);
            } else {
                args[i] = populateObject(type, paramName, req);
            }
        }

        return args;
    }

    /**
     * Instancie un objet et remplit ses attributs depuis la requête.
     *
     * Pour chaque attribut, on cherche d'abord "nomParametre.attribut" (ex: employe.nom),
     * puis simplement "attribut" (ex: nom). L'attribut est écrit via son setter s'il existe,
     * sinon directement sur le champ. Les attributs de type objet sont remplis récursivement.
     */
    private static Object populateObject(Class<?> type, String prefix, HttpServletRequest req) throws Exception {
        Object instance = type.getDeclaredConstructor().newInstance();

        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                continue;
            }

            Class<?> fieldType = field.getType();
            Object value;

            if (isSimpleType(fieldType)) {
                String raw = req.getParameter(prefix + "." + field.getName());
                if (raw == null) {
                    raw = req.getParameter(field.getName());
                }
                value = convertValue(raw, fieldType);
            } else {
                value = populateObject(fieldType, prefix + "." + field.getName(), req);
            }

            setFieldValue(instance, field, value);
        }

        return instance;
    }

    /**
     * Écrit la valeur via le setter (setNom, setAge...) si il existe, sinon directement sur le champ.
     */
    private static void setFieldValue(Object instance, Field field, Object value) throws Exception {
        String name = field.getName();
        String setterName = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);

        try {
            Method setter = instance.getClass().getMethod(setterName, field.getType());
            setter.invoke(instance, value);
        } catch (NoSuchMethodException e) {
            field.setAccessible(true);
            field.set(instance, value);
        }
    }

    /**
     * Types qu'on sait convertir directement depuis une String.
     */
    private static boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || type == String.class
                || type == Integer.class
                || type == Double.class
                || type == Float.class
                || type == Long.class
                || type == Boolean.class;
    }

    /**
     * Convertit une valeur String vers un type primitif / Object équivalent.
     * Si la valeur est absente ou vide, renvoie la valeur par défaut (0, null, false...).
     */
    private static Object convertValue(String value, Class<?> targetType) {
        if (value == null || value.trim().isEmpty()) {
            return getDefaultValue(targetType);
        }

        try {
            if (targetType == String.class) {
                return value;
            } else if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(value.trim());
            } else if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(value.trim());
            } else if (targetType == float.class || targetType == Float.class) {
                return Float.parseFloat(value.trim());
            } else if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(value.trim());
            } else if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(value.trim());
            }
        } catch (NumberFormatException e) {
            // Si la conversion échoue (ex: "abc" vers int), on retourne la valeur par défaut
            return getDefaultValue(targetType);
        }

        return null;
    }

    /**
     * Retourne la valeur de repli (0 pour les nombres, false pour boolean, null pour String/Objets).
     */
    private static Object getDefaultValue(Class<?> type) {
        if (type == int.class) return 0;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0.0f;
        if (type == long.class) return 0L;
        if (type == boolean.class) return false;
        return null; // String et types Wrapper (Integer, Double...)
    }
}
