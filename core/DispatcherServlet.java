package core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

public class DispatcherServlet extends HttpServlet {
    List<Class<?>> controllerClasses = new ArrayList<>();

    @Override
    public void init() throws ServletException {
        try {
            String controllersPackage = getServletConfig().getInitParameter("controller");
            controllerClasses = utils.ControllerUtils.getControllerClasses(controllersPackage);
            System.out.println("Classes de contrôleur trouvées dans le package '" + controllersPackage + "':");
            for (Class<?> clazz : controllerClasses) {
                System.out.println(clazz.getName());
            }
        } catch (Exception e) {
            throw new ServletException("Erreur lors du scan des contrôleurs", e);
        }
    }

    public void affichage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String servletPath = request.getPathInfo();
        response.getWriter().println("Servlet Path: " + servletPath);
        for (Class<?> controllerClass : controllerClasses) {
            response.getWriter().println("Controller Class: " + controllerClass.getName());
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
