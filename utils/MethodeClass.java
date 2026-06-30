package utils;

import java.lang.reflect.Method;

public class MethodeClass {
    Class<?> controllerClass;
    Method methods;

    public void setControllerClass(Class<?> controllerClass) {
        this.controllerClass = controllerClass;
    }

    public void setMethod(Method met) {
        this.methods = met;
    }

    public Class<?> getcontrollerClass() {
        return controllerClass;
    }

    public Method getMethods() {
        return methods;
    }

}
