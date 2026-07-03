package core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import utils.MethodeClass;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import utils.UrlMethode;

public class DispatcherServlet extends HttpServlet {

    List<Class<?>> controllerClasses = new ArrayList<>();
    Map<String, MethodeClass> methodMap1 = new HashMap<>();
    Map<UrlMethode, MethodeClass> methodMap = new HashMap<>();

    @Override
    public void init() throws ServletException {
        try {
            String controllersPackage = getServletConfig().getInitParameter("controller");
            controllerClasses = utils.ControllerUtils.getControllerClasses(controllersPackage);
            methodMap = utils.ControllerUtils.getmethodeClasses(controllerClasses);
        } catch (Exception e) {
            throw new ServletException("Erreur lors du scan des contrôleurs", e);
        }
    }

    public void affichage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String servletPath = request.getPathInfo();
        if (servletPath != null && methodMap.containsKey(servletPath)) {
            MethodeClass methodClass = methodMap.get(servletPath);
            response.getWriter().write("La methode correspondante a ete trouvee pour le chemin : " + servletPath);
            response.getWriter().write("Controller : " + methodClass.getcontrollerClass().getName() + ", Methode : "
                    + methodClass.getMethods().getName());
        } else {
            response.getWriter().write("Aucune methode correspondante trouvee pour le chemin. Methode dispo : ");
            for (String path : methodMap1.keySet()) {
                response.getWriter().write(path + " ");
            }
        }
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
