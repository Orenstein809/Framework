package utils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static Map<UrlMethode, MethodeClass> getmethodeClasses(List<Class<?>> controllerClasses) {
        Map<UrlMethode, MethodeClass> methodMap = new HashMap<>();
        for (Class<?> controllerClasse : controllerClasses) {
            Method[] methods = controllerClasse.getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(annotation.UrlMapping.class)) {
                    UrlMethode urlMethode = new UrlMethode(
                            method.getAnnotation(annotation.UrlMapping.class).value(),
                            method.getAnnotation(annotation.UrlMapping.class).method()
                    );
                    if (methodMap.containsKey(urlMethode)) {
                        throw new RuntimeException("Duplicate mapping for URL: " + urlMethode.getUrl() + " and method: " + urlMethode.getMethode());
                    }
                    MethodeClass methodeClass = new MethodeClass();
                    methodeClass.setControllerClass(controllerClasse);
                    methodeClass.setMethod(method);
                    methodMap.put(urlMethode, methodeClass);
                }
            }
        }
        return methodMap;
    }

    public static MethodeClass getMethodeClass(Map<UrlMethode, MethodeClass> methodMap, String url, String methode) {
        return methodMap.get(new UrlMethode(url, methode));
    }

    public static void findAllMethodesWithUrlMethod(String packageName, Map<UrlMethode, MethodeClass> mapToFill) {
        List<Class<?>> controllerClasses = getControllerClasses(packageName);
        Map<UrlMethode, MethodeClass> scannedMap = getmethodeClasses(controllerClasses);    
        mapToFill.putAll(scannedMap);
}

    public static void execute(Object result, HttpServletRequest request, HttpServletResponse response) {
        if (result instanceof ModelAndView) {
            ModelAndView modelAndView = (ModelAndView) result;
            try {
                if (modelAndView.getAttributs() != null) {
                    for (Map.Entry<String, Object> entry : modelAndView.getAttributs().entrySet()) {
                        request.setAttribute(entry.getKey(), entry.getValue());
                    }
                }
                request.getRequestDispatcher(modelAndView.getView()).forward(request, response);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
