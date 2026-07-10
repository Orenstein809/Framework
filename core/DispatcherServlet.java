package core;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.util.Map;
import java.util.HashMap;
import utils.ControllerUtils;
import utils.MethodeClass; // Mis à jour
import utils.UrlMethode;   // Mis à jour

public class DispatcherServlet extends HttpServlet {
    
    // Utilisation des nouvelles classes d'utilitaires
    private Map<UrlMethode, MethodeClass> listeInfoMethodeAndController = new HashMap<>();

    @Override
    public void init() throws ServletException {
        // Le Dispatcher récupère la Map déjà créée par le Listener au démarrage
        Object attribute = this.getServletContext().getAttribute("listeInfoMethodeAndController");
        if (attribute != null) {
            this.listeInfoMethodeAndController = (Map<UrlMethode, MethodeClass>) attribute;
        }
    }
    
    public void affichage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String servletPath = request.getRequestURI();
        String nameApplication = request.getContextPath();
        String url = servletPath.substring(nameApplication.length());
        String method = request.getMethod();

        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().println(
            "<!doctype html>\n<html lang=\"fr\">\n<head><title>Liste des controllers</title></head>\n<body>\n" +
            "<h1>Statut du Dispatcher</h1>\n<ul>"
        );

        // Recherche de la correspondance URL/Méthode via ton utilitaire
        MethodeClass infoMethodeAndController = ControllerUtils.findClassByUrlMethod(listeInfoMethodeAndController, url, method);

        if (infoMethodeAndController == null) {
            response.getWriter().println("<p style='color:red;'>Aucune méthode trouvée pour l'URL : " + url + " [" + method + "]</p>");
            response.getWriter().println("<h3>Routes disponibles :</h3>");
            for (Map.Entry<UrlMethode, MethodeClass> entry : listeInfoMethodeAndController.entrySet()) {
                response.getWriter().println("<li><b>" + entry.getKey().getUrl() + "</b> (" + entry.getKey().getMethode() + ") -> " + entry.getValue().getMethodeName() + "</li>");
            }
        } else {
            try {
                // Invocation dynamique de la méthode du contrôleur
                Object result = infoMethodeAndController.execute();
                ControllerUtils.execute(result, request, response);
            } catch (Exception e) {
                response.getWriter().println("<p>Erreur d'exécution : " + e.getMessage() + "</p>");
            }
        }

        response.getWriter().println("</ul></body></html>");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String uri = request.getRequestURI();
        // Filtrage des fichiers statiques
        if (uri.endsWith(".html") || uri.endsWith(".css") || uri.endsWith(".js") || 
            uri.endsWith(".png") || uri.endsWith(".jpg") || uri.endsWith(".gif") || 
            uri.endsWith(".ico") || uri.endsWith(".svg") || uri.endsWith(".jsp")) {

            request.getServletContext().getNamedDispatcher("default").forward(request, response);
            return;
        }
        affichage(request, response);
    }
}