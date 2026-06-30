package utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ControllerUtils {

    public static List<Class<?>> getFiles(String packageName) {
        List<Class<?>> controllerClasses = new ArrayList<>();
        try {
            String packagePath = packageName.replace('.', '/');
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            java.net.URL resource = classLoader.getResource(packagePath);

            if (resource == null) {
                System.out.println("Le package " + packageName + " est introuvable (ressource vide).");
                return controllerClasses;
            }
            java.io.File directory = new java.io.File(resource.toURI());

            if (directory.exists() && directory.isDirectory()) {
                java.io.File[] files = directory.listFiles();
                if (files != null) {
                    for (java.io.File file : files) {
                        if (file.isFile() && file.getName().endsWith(".class")) {
                            String className = file.getName().substring(0, file.getName().length() - 6);
                            String totalClassName = packageName + "." + className;
                            Class<?> clazz = Class.forName(totalClassName);
                            controllerClasses.add(clazz);
                        }
                    }
                }
            } else {
                System.out.println("Le chemin " + packageName + " n'est pas un dossier valide.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du scan des contrôleurs", e);
        }

        return controllerClasses;
    }

    public static List<Class<?>> getControllerClasses(String packageName) {
        List<Class<?>> controllerClasses = new ArrayList<>();
        List<Class<?>> allClasses = getFiles(packageName);
        for (Class<?> clazz : allClasses) {
            if (clazz.isAnnotationPresent(annotation.Controller.class)) {
                controllerClasses.add(clazz);

            }
        }

        return controllerClasses;
    }

    public static Map<String, MethodeClass> getMethodeClasses(List<Class<?>> controllerClasses) {
        Map<String, MethodeClass> methodMap = new HashMap<>();
        for (Class<?> controllerClass : controllerClasses) {
            java.lang.reflect.Method[] methods = controllerClass.getDeclaredMethods();
            for (java.lang.reflect.Method method : methods) {
                if (method.isAnnotationPresent(annotation.UrlMapping.class)) {
                    utils.MethodeClass methodeClass = new utils.MethodeClass();
                    methodeClass.controllerClass = controllerClass;
                    methodeClass.methods = method;
                    methodMap.put(method.getAnnotation(annotation.UrlMapping.class).value(), methodeClass);
                }
            }
        }
        return methodMap;
    }
}
