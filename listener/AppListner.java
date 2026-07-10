package listener;

import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.annotation.WebListener;
import utils.MethodeClass; 
import utils.UrlMethode;   
import utils.ControllerUtils;

import java.util.HashMap;
import java.util.Map;

@WebListener
public class AppListner implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        String controllersPackage = sce.getServletContext().getInitParameter("controller");
        
        Map<UrlMethode, MethodeClass> listeInfoMethodeAndController = new HashMap<>();
        
        try {
            ControllerUtils.findAllMethodesWithUrlMethod(controllersPackage, listeInfoMethodeAndController);
            
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
        sce.getServletContext().removeAttribute("listeInfoMethodeAndController");
        System.out.println("Application arrêtée !");
    }
}