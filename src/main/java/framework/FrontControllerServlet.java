package framework;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import framework.annotation.Auth;
import framework.annotation.Controller;
import framework.annotation.UrlMapping;
import framework.annotation.WebApi;

@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2,  // 2MB
    maxFileSize = 1024 * 1024 * 10,       // 10MB
    maxRequestSize = 1024 * 1024 * 50     // 50MB
)
public class FrontControllerServlet extends HttpServlet {

    private List<String> controllers = new ArrayList<>();
    private Map<String, VerbAction> mappings = new HashMap<>();
    private Gson gson = new Gson();

    private static final String SESSION_MAP_KEY = "appSessionsMap";
    private static final String SESSION_COOKIE_NAME = "app-test";
    private static final String AUTH_SESSION_KEY = "authenticated";
    private static final String AUTH_ERROR_VIEW = "/error.jsp";

    @Override
    public void init() throws ServletException {
        super.init();

        // Map globale des sessions (uuid -> MySession), partagée dans le ServletContext
        ServletContext context = getServletContext();
        if (context.getAttribute(SESSION_MAP_KEY) == null) {
            context.setAttribute(SESSION_MAP_KEY, new ConcurrentHashMap<String, MySession>());
        }

        try {
            
            Enumeration<java.net.URL> roots = Thread.currentThread()
                    .getContextClassLoader()
                    .getResources("");

            while (roots.hasMoreElements()) {
                String path = roots.nextElement().getFile();
                File dir = new File(URLDecoder.decode(path, "UTF-8"));
                scanClasses(dir, "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scanClasses(File dir, String pkg) {
        if (dir == null || !dir.exists()) return;

        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                scanClasses(f, pkg + (pkg.isEmpty() ? "" : ".") + f.getName());
            } else if (f.getName().endsWith(".class")) {
                try {
                    String className = pkg + "." + f.getName().replace(".class", "");
                    Class<?> clazz = Class.forName(className);

                    if (clazz.isAnnotationPresent(Controller.class)) {
                        controllers.add(clazz.getSimpleName());
                        Object instance = clazz.getDeclaredConstructor().newInstance();

                        for (Method m : clazz.getDeclaredMethods()) {
                            if (m.isAnnotationPresent(UrlMapping.class)) {
                                String url = m.getAnnotation(UrlMapping.class).value();
                                mappings.put(url, new VerbAction(instance, m));
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Retrouve la session du client grâce au cookie "app-test", ou en crée une nouvelle.
     */
    private MySession getOrCreateSession(HttpServletRequest req, HttpServletResponse resp) {
        @SuppressWarnings("unchecked")
        Map<String, MySession> sessionsMap =
                (Map<String, MySession>) getServletContext().getAttribute(SESSION_MAP_KEY);

        String sessionUuid = null;
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    sessionUuid = cookie.getValue();
                    break;
                }
            }
        }

        MySession clientSession = (sessionUuid != null) ? sessionsMap.get(sessionUuid) : null;

        // Cookie absent ou session inconnue : on en crée une nouvelle
        if (clientSession == null) {
            sessionUuid = UUID.randomUUID().toString();
            clientSession = new MySession();
            sessionsMap.put(sessionUuid, clientSession);

            Cookie sessionCookie = new Cookie(SESSION_COOKIE_NAME, sessionUuid);
            sessionCookie.setPath(req.getContextPath().isEmpty() ? "/" : req.getContextPath());
            sessionCookie.setMaxAge(60 * 60 * 24); // 1 jour
            sessionCookie.setHttpOnly(true);
            resp.addCookie(sessionCookie);
        }

        return clientSession;
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        MySession clientSession = getOrCreateSession(req, resp);
        // Accessible aussi depuis les vues (JSP) : ${mySession.attributes.xxx}
        req.setAttribute("mySession", clientSession);

        String uri = req.getRequestURI();
        String contexte = req.getContextPath();
        String path = uri.substring(contexte.length());

        if (mappings.containsKey(path)) {
            VerbAction verbAction = mappings.get(path);
            Method method = verbAction.getMethod();
            Object controller = verbAction.getControllerInstance();

            // Méthode protégée par @Auth : la session doit contenir authenticated = true
            if (method.isAnnotationPresent(Auth.class)
                    && !Boolean.TRUE.equals(clientSession.get(AUTH_SESSION_KEY))) {
                req.getRequestDispatcher(AUTH_ERROR_VIEW).forward(req, resp);
                return;
            }

            try {
                // Matching des paramètres HTTP avec les arguments de la méthode (+ injection de MySession)
                Object[] args = MethodMapper.resolveParameters(method, req, clientSession);

                System.out.println("--> Execution de la méthode : " + method.getName());
                Object result = method.invoke(controller, args);
                System.out.println("<-- Fin execution. Résultat retourné : " + result);

                if (method.isAnnotationPresent(WebApi.class)) {
                    if (result instanceof String) {
                        
                        resp.setContentType("text/plain;charset=UTF-8");
                        PrintWriter out = resp.getWriter();
                        out.print(result);
                        out.flush();
                    } else {
                        
                        resp.setContentType("application/json;charset=UTF-8");
                        String json = gson.toJson(result);
                        PrintWriter out = resp.getWriter();
                        out.print(json);
                        out.flush();
                    }
                } else {
                    
                    if (result instanceof String) {
                        String viewPath = (String) result;
                        req.getRequestDispatcher(viewPath).forward(req, resp);
                    } else {
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                                "Type de retour non valide pour le rendu de vue HTML/JSP");
                    }
                }

            } catch (Exception e) {
                throw new ServletException("Erreur lors de l'exécution de la méthode " + method.getName(), e);
            }
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Aucune route associée à l'URL : " + path);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        processRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        processRequest(req, resp);
    }
}
