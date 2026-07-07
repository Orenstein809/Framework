package utils;

import java.util.Objects;

public class UrlMethode {

    private String url;
    private String methode;

    public UrlMethode(String url, String method) {
        this.url = url;
        this.methode = method;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setMethode(String methode) {
        this.methode = methode;
    }

    public String getUrl() {
        return this.url;
    }

    public String getMethode() {
        return this.methode;
    }

    public boolean isAllowed() {
        String url = this.getUrl();
        if ("POST".equals(url) || "GET".equals(url) || "PUT".equals(url) || "DELETE".equals(url)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        UrlMethode other = (UrlMethode) obj;
        return url.equals(other.url) && methode.equals(other.methode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, methode);
    }

}
