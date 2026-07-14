package utils;

import java.util.HashMap;
import java.util.Map;

public class ModelAndView {

    private String view;
    private Map<String, Object> attributs = new HashMap<>();

    public ModelAndView(String view) {
        this.view = view;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public Map<String, Object> getAttributs() {
        return attributs;
    }

    public void setAttributs(Map<String, Object> attributs) {
        this.attributs = attributs;
    }

    public void addObject(String key, Object value) {
        this.attributs.put(key, value);
    }
}