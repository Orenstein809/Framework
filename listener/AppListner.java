package listener;

import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.annotation.WebListener;
import utils.MethodeClass; // Nouvelle classe
import utils.UrlMethode;   // Nouvelle classe
import utils.ControllerUtils;

import java.util.HashMap;
import java.util.Map;

@WebListener
public class AppListner implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // 1. Récupérer le package cible configuré (ex: "controllers")
        String controllersPackage = sce.getServletContext().getInitParameter("controller");
        
        // 2. Initialiser la Map avec tes nouveaux types d'objets
        Map<UrlMethode, MethodeClass> listeInfoMethodeAndController = new HashMap<>();
        
        try {
            // 3. Remplir la map via la méthode de scan (à adapter dans ControllerUtils)
            ControllerUtils.findAllMethodesWithUrlMethod(controllersPackage, listeInfoMethodeAndController);
            
            // 4. Stocker la map dans le ServletContext pour le Dispatcher
            sce.getServletContext().setAttribute("listeInfoMethodeAndController", listeInfoMethodeAndController);
            System.out.println("Application démarrée et routes chargées !");
            
        } catch (Exception e) {
            System.out.println("Erreur lors du chargement des contrôleurs : " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Nettoyage à l'arrêt du serveur
        sce.getServletContext().removeAttribute("listeInfoMethodeAndController");
        System.out.println("Application arrêtée !");
    }
}