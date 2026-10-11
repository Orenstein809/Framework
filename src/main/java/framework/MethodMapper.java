package framework;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import jakarta.servlet.http.HttpServletRequest;

public class MethodMapper {


    public static Object[] resolveParameters(Method method, HttpServletRequest req) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];

    
            String paramName = param.getName();

  
            String rawValue = req.getParameter(paramName);

            System.out.println("Paramètre '" + paramName + "' = " + rawValue);


            args[i] = convertValue(rawValue, param.getType());
        }

        return args;
    }


    private static Object convertValue(String value, Class<?> targetType) {
        if (value == null || value.trim().isEmpty()) {
            return getDefaultValue(targetType);
        }

        try {
            if (targetType == String.class) {
                return value;
            } else if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(value.trim());
            } else if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(value.trim());
            } else if (targetType == float.class || targetType == Float.class) {
                return Float.parseFloat(value.trim());
            } else if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(value.trim());
            } else if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(value.trim());
            }
        } catch (NumberFormatException e) {
            
            return getDefaultValue(targetType);
        }

        return null;
    }

    private static Object getDefaultValue(Class<?> type) {
        if (type == int.class) return 0;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0.0f;
        if (type == long.class) return 0L;
        if (type == boolean.class) return false;
        return null; 
    }
}
