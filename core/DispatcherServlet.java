package core;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.util.Map;
import java.util.HashMap;
import utils.ControllerUtils;
import utils.MethodeClass; 
import utils.UrlMethode;
import utils.ModelAndView; 

public class DispatcherServlet extends HttpServlet {
    
    private Map<UrlMethode, MethodeClass> listeInfoMethodeAndController = new HashMap<>();

    @Override
    public void init() throws ServletException {
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

        MethodeClass infoMethodeAndController = ControllerUtils.getMethodeClass(listeInfoMethodeAndController, url, method);

        if (infoMethodeAndController == null) {
            // Affichage de la page de diagnostic par défaut si aucune route n'est trouvée
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().println(
                "<!doctype html>\n<html lang=\"fr\">\n<head><title>Liste des controllers</title></head>\n<body>\n" +
                "<h1>Statut du Dispatcher</h1>\n<ul>"
            );
            response.getWriter().println("<p style='color:red;'>Aucune méthode trouvée pour l'URL : " + url + " [" + method + "]</p>");
            response.getWriter().println("<h3>Routes disponibles :</h3>");
            for (Map.Entry<UrlMethode, MethodeClass> entry : listeInfoMethodeAndController.entrySet()) {
                String nomMethodeJava = (entry.getValue().getMethods() != null) ? entry.getValue().getMethods().getName() : "Inconnue";
                response.getWriter().println("<li><b>" + entry.getKey().getUrl() + "</b> (" + entry.getKey().getMethode() + ") -> " + nomMethodeJava + "</li>");
            }
            response.getWriter().println("</ul></body></html>");
        } else {
            try {
                Class<?> clazz = infoMethodeAndController.getcontrollerClass();
                java.lang.reflect.Method meth = infoMethodeAndController.getMethods();

                // Instanciation dynamique du contrôleur
                Object controllerInstance = clazz.getDeclaredConstructor().newInstance();

                // Invocation de la méthode du contrôleur
                Object result = meth.invoke(controllerInstance);

                // Si le résultat est un ModelAndView, on le traite via notre utilitaire
                if (result instanceof ModelAndView) {
                    ControllerUtils.execute(result, request, response);
                } else if (result != null) {
                    // Sinon, affichage brut classique de l'objet
                    response.setContentType("text/html;charset=UTF-8");
                    response.getWriter().println("<h3>Résultat de l'exécution :</h3>");
                    response.getWriter().println("<p>" + result.toString() + "</p>");
                }

            } catch (Exception e) {
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().println("<p style='color:red;'>Erreur d'exécution : " + e.getMessage() + "</p>");
                e.printStackTrace();
            }
        }
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
        if (uri.endsWith(".html") || uri.endsWith(".css") || uri.endsWith(".js") || 
            uri.endsWith(".png") || uri.endsWith(".jpg") || uri.endsWith(".gif") || 
            uri.endsWith(".ico") || uri.endsWith(".svg") || uri.endsWith(".jsp")) {

            request.getServletContext().getNamedDispatcher("default").forward(request, response);
            return;
        }
        affichage(request, response);
    }
}