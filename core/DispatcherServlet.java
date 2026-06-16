package core;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

public class DispatcherServlet extends HttpServlet {

oo

    /*All controller 
    @Override
    public void init() throws ServletException {
        super.init();
        try {
             System.out.println("Fonction Init du DispatcherServlet appelée !");
            String packageName = "controller";
            // On remplace les points par des slashs pour le ClassLoader (ex: com.app.controller -> com/app/controller)
            String packagePath = packageName.replace('.', '/'); 
            // Récupération du ClassLoader
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            java.net.URL resource = classLoader.getResource(packagePath);
        
            if (resource != null) {
                // C'est ici que nous allons récupérer les fichiers
                java.io.File directory = new java.io.File(resource.getFile());
                if (directory.exists() && directory.isDirectory()) {
                // 📂 On récupère tous les fichiers du dossier
                java.io.File[] files = directory.listFiles();
                if (files != null) {
                    for (java.io.File file : files) {
                        // 🔍 On vérifie si c'est un fichier et s'il se termine par .class
                        if (file.isFile() && file.getName().endsWith(".class")) {
                            // On extrait le nom de la classe (ex: "MonController.class" -> "MonController")
                            String className = file.getName().substring(0, file.getName().length() - 6);
                            System.out.println("Classe trouvée : " + className);
                        }
                    }
                }
            }
            } else {
                System.out.println("Le package " + packageName + " est introuvable.");
            }
        } catch (Exception e) {
            throw new ServletException("Erreur lors de l'initialisation du DispatcherServlet", e);
        }
    } */

    @Override
    public void init() throws ServletException {
        try {
            System.out.println("[Framework] Initialisation du DispatcherServlet en cours...");
            String packageName = "controller";
            String packagePath = packageName.replace('.', '/'); 

            // 2. On récupère le chemin réel du dossier via le ClassLoader
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            java.net.URL resource = classLoader.getResource(packagePath);

            if (resource != null) {
                java.io.File directory = new java.io.File(resource.getFile());

                if (directory.exists() && directory.isDirectory()) {
                    java.io.File[] files = directory.listFiles();

                    if (files != null) {
                        for (java.io.File file : files) {
                            if (file.isFile() && file.getName().endsWith(".class")) {

                                String className = file.getName().substring(0, file.getName().length() - 6);

                                // On reconstruit le nom complet (ex: "mg.etuxxxx.controller.EtudiantController")
                                String totalClassName = packageName + "." + className;

                                // 🚀 4. Chargement dynamique de la classe en mémoire
                                Class<?> clazz = Class.forName(totalClassName);

                                // 🏷️ 5. Vérification de la présence de l'annotation @Controller
                                if (clazz.isAnnotationPresent(core.annotation.Controller.class)) {
                                    System.out.println("[Framework] Contrôleur valide trouvé : " + clazz.getName());

                                    // TODO: Plus tard, on stockera cette classe dans une liste ou une Map !
                                } else {
                                    System.out.println("[Framework] Classe ignorée (pas de @Controller) : " + clazz.getName());
                                }
                            }
                        }
                    }
                }
            } else {
                System.out.println("[Framework] Erreur : Le package " + packageName + " est introuvable.");
            }
        } catch (Exception e) {
            throw new ServletException("Erreur lors du scan des contrôleurs", e);
        }
    }

    public void affichage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String servletPath = request.getPathInfo();

        response.getWriter().println("Servlet Path: " + servletPath);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        affichage(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        affichage(request, response);

    }

   
}
