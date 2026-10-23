package framework;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

import framework.type.Fichier;

/**
 * Transforme les données de la requête HTTP (paramètres + fichiers) en arguments
 * prêts pour method.invoke().
 *
 * Conventions de nommage des champs du formulaire :
 *  - type simple ............ nom                      -> int age        => name="age"
 *  - fichier ................ nom                      -> Fichier photo  => name="photo"
 *  - objet .................. param-attribut           -> Employe emp    => name="emp-nom"
 *  - objet imbriqué ......... param-attr-attr          => name="emp-adresse-ville"
 *  - tableau / liste simple . nom ou nom[]             -> String[] tags  => name="tags" (répété)
 *  - liste de fichiers ...... nom ou nom[] (multiple)  -> List<Fichier>  => name="docs"
 *  - liste d'objets ......... nom[i]-attribut          -> List<Employe> employes => name="employes[0]-nom"
 *
 * Le séparateur "." est aussi accepté à la place de "-" (emp.nom, emp.adresse.ville).
 * Pour un objet, si "param-attribut" n'existe pas, on cherche simplement "attribut".
 *
 * NB : le projet client doit être compilé avec l'option "-parameters" pour que
 * Reflection renvoie les vrais noms de paramètres (sinon arg0, arg1...).
 */
public class MethodMapper {

    private static final String[] SEPARATORS = { "-", "." };
    private static final int MAX_ITEMS = 1000;

    public static Object[] resolveParameters(Method method, HttpServletRequest req) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> type = param.getType();
            String name = param.getName();

            if (isSimpleType(type)) {
                args[i] = convertValue(req.getParameter(name), type);
            } else if (type == Fichier.class) {
                args[i] = extractFichier(req, name);
            } else if (isCollectionOrArray(type)) {
                args[i] = populateCollectionOrArray(type, param.getParameterizedType(), req, name);
            } else {
                args[i] = populateObject(type, req, name);
            }
        }

        return args;
    }

    // ------------------------------------------------------------------
    // OBJETS
    // ------------------------------------------------------------------

    private static Object populateObject(Class<?> clazz, HttpServletRequest req, String prefix) throws Exception {
        Object instance = clazz.getDeclaredConstructor().newInstance();

        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                continue;
            }

            Class<?> fieldType = field.getType();
            String fieldName = field.getName();

            if (isSimpleType(fieldType)) {
                String raw = findParameter(req, prefix, fieldName);
                if (raw != null) {
                    setFieldValue(instance, field, convertValue(raw, fieldType));
                }
            } else if (fieldType == Fichier.class) {
                Fichier fichier = findFichier(req, prefix, fieldName);
                if (fichier != null) {
                    setFieldValue(instance, field, fichier);
                }
            } else if (isCollectionOrArray(fieldType)) {
                String key = findMatchingKey(req, prefix, fieldName);
                if (key != null) {
                    setFieldValue(instance, field,
                            populateCollectionOrArray(fieldType, field.getGenericType(), req, key));
                }
            } else if (!isJdkType(fieldType)) {
                String key = findMatchingKey(req, prefix, fieldName);
                if (key != null) {
                    setFieldValue(instance, field, populateObject(fieldType, req, key));
                }
            }
        }

        return instance;
    }

    /**
     * Écrit la valeur via le setter (setNom, setAge...) s'il existe, sinon directement sur le champ.
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

    // ------------------------------------------------------------------
    // TABLEAUX ET COLLECTIONS
    // ------------------------------------------------------------------

    private static Object populateCollectionOrArray(Class<?> targetType, Type genericType,
                                                    HttpServletRequest req, String prefix) throws Exception {
        Class<?> elementType = Object.class;
        if (targetType.isArray()) {
            elementType = targetType.getComponentType();
        } else if (genericType instanceof ParameterizedType) {
            Type actualArg = ((ParameterizedType) genericType).getActualTypeArguments()[0];
            if (actualArg instanceof Class) {
                elementType = (Class<?>) actualArg;
            }
        }

        List<Object> elements = new ArrayList<>();

        if (isSimpleType(elementType)) {
            // name="tags" répété, ou name="tags[]"
            String[] values = req.getParameterValues(prefix);
            if (values == null) {
                values = req.getParameterValues(prefix + "[]");
            }
            if (values != null) {
                for (String v : values) {
                    elements.add(convertValue(v, elementType));
                }
            } else {
                // name="tags[0]", name="tags[1]"...
                for (int i = 0; i < MAX_ITEMS; i++) {
                    String v = req.getParameter(prefix + "[" + i + "]");
                    if (v == null) break;
                    elements.add(convertValue(v, elementType));
                }
            }
        } else if (elementType == Fichier.class) {
            // <input type="file" name="docs" multiple>
            try {
                for (Part p : req.getParts()) {
                    if ((p.getName().equals(prefix) || p.getName().equals(prefix + "[]")) && p.getSize() > 0) {
                        elements.add(convertPartToFichier(p));
                    }
                }
            } catch (Exception ignored) {
                // requête non multipart
            }
            // name="docs[0]", name="docs[1]"... (ou docs-0)
            for (int i = 0; i < MAX_ITEMS; i++) {
                Fichier f = extractFichier(req, prefix + "[" + i + "]");
                if (f == null) {
                    f = extractFichier(req, prefix + "-" + i);
                }
                if (f == null) break;
                elements.add(f);
            }
        } else if (!isJdkType(elementType)) {
            // name="employes[0]-nom", name="employes[1]-nom"...
            for (int i = 0; i < MAX_ITEMS; i++) {
                String itemPrefix = prefix + "[" + i + "]";
                if (!hasMatchingParameterOrPart(req, itemPrefix)) break;
                elements.add(populateObject(elementType, req, itemPrefix));
            }
        }

        return buildResult(targetType, elementType, elements);
    }

    private static Object buildResult(Class<?> targetType, Class<?> elementType, List<Object> elements) {
        if (targetType.isArray()) {
            Object array = Array.newInstance(elementType, elements.size());
            for (int i = 0; i < elements.size(); i++) {
                Array.set(array, i, elements.get(i));
            }
            return array;
        }
        Collection<Object> collection = createCollectionInstance(targetType);
        collection.addAll(elements);
        return collection;
    }

    @SuppressWarnings("unchecked")
    private static Collection<Object> createCollectionInstance(Class<?> collectionType) {
        if (!collectionType.isInterface() && !java.lang.reflect.Modifier.isAbstract(collectionType.getModifiers())) {
            try {
                return (Collection<Object>) collectionType.getDeclaredConstructor().newInstance();
            } catch (Exception ignored) {
                // on retombe sur les valeurs par défaut ci-dessous
            }
        }
        if (Set.class.isAssignableFrom(collectionType)) {
            return new LinkedHashSet<>();
        }
        return new ArrayList<>();
    }

    // ------------------------------------------------------------------
    // FICHIERS
    // ------------------------------------------------------------------

    private static Fichier extractFichier(HttpServletRequest req, String paramName) {
        try {
            Part part = req.getPart(paramName);
            if (part != null && part.getSize() > 0) {
                return convertPartToFichier(part);
            }
        } catch (Exception e) {
            // requête non multipart ou part introuvable
        }
        return null;
    }

    private static Fichier findFichier(HttpServletRequest req, String prefix, String fieldName) {
        if (prefix != null && !prefix.isEmpty()) {
            for (String sep : SEPARATORS) {
                Fichier f = extractFichier(req, prefix + sep + fieldName);
                if (f != null) return f;
            }
        }
        return extractFichier(req, fieldName);
    }

    private static Fichier convertPartToFichier(Part part) throws Exception {
        String name = "";
        String extension = "";

        String submitted = part.getSubmittedFileName();
        if (submitted != null && !submitted.isEmpty()) {
            // certains navigateurs envoient le chemin complet du fichier
            String pureName = new File(submitted.replace('\\', '/')).getName();
            int lastDot = pureName.lastIndexOf('.');
            if (lastDot > 0) {
                name = pureName.substring(0, lastDot);
                extension = pureName.substring(lastDot + 1);
            } else {
                name = pureName;
            }
        }

        byte[] binary;
        try (InputStream is = part.getInputStream()) {
            binary = is.readAllBytes();
        }

        return new Fichier(name, extension, binary);
    }

    // ------------------------------------------------------------------
    // RECHERCHE DES CLES DANS LA REQUETE
    // ------------------------------------------------------------------

    /**
     * Valeur du paramètre "prefix-fieldName" (ou "prefix.fieldName"), sinon "fieldName" seul.
     */
    private static String findParameter(HttpServletRequest req, String prefix, String fieldName) {
        if (prefix != null && !prefix.isEmpty()) {
            for (String sep : SEPARATORS) {
                String v = req.getParameter(prefix + sep + fieldName);
                if (v != null) return v;
            }
        }
        return req.getParameter(fieldName);
    }

    /**
     * Retourne la clé ("prefix-fieldName" ou "prefix.fieldName") sous laquelle la requête
     * contient des données, ou null s'il n'y en a pas.
     */
    private static String findMatchingKey(HttpServletRequest req, String prefix, String fieldName) {
        if (prefix == null || prefix.isEmpty()) {
            return hasMatchingParameterOrPart(req, fieldName) ? fieldName : null;
        }
        for (String sep : SEPARATORS) {
            String key = prefix + sep + fieldName;
            if (hasMatchingParameterOrPart(req, key)) return key;
        }
        return null;
    }

    private static boolean matches(String name, String prefix) {
        if (name.equals(prefix) || name.startsWith(prefix + "[")) return true;
        for (String sep : SEPARATORS) {
            if (name.startsWith(prefix + sep)) return true;
        }
        return false;
    }

    private static boolean hasMatchingParameterOrPart(HttpServletRequest req, String prefix) {
        Enumeration<String> paramNames = req.getParameterNames();
        while (paramNames.hasMoreElements()) {
            if (matches(paramNames.nextElement(), prefix)) return true;
        }

        try {
            for (Part part : req.getParts()) {
                if (matches(part.getName(), prefix)) return true;
            }
        } catch (Exception ignored) {
            // requête non multipart
        }

        return false;
    }

    // ------------------------------------------------------------------
    // TYPES ET CONVERSIONS
    // ------------------------------------------------------------------

    private static boolean isCollectionOrArray(Class<?> type) {
        return type.isArray() || Collection.class.isAssignableFrom(type);
    }

    /**
     * Classes du JDK (Date, BigDecimal...) : on ne peut pas les remplir champ par champ.
     */
    private static boolean isJdkType(Class<?> type) {
        String n = type.getName();
        return n.startsWith("java.") || n.startsWith("javax.") || n.startsWith("jakarta.");
    }

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
     * Si la valeur est absente, vide ou invalide, renvoie la valeur par défaut (0, null, false...).
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
            return getDefaultValue(targetType);
        }

        return null;
    }

    private static Object getDefaultValue(Class<?> type) {
        if (type == int.class) return 0;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0.0f;
        if (type == long.class) return 0L;
        if (type == boolean.class) return false;
        return null;
    }
}
