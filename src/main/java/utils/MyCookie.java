/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author dmmaga
 */
public class MyCookie {

    public MyCookie() {
    }

    public HttpServletResponse getResponse() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
        return response;
    }

    public HttpServletRequest getResquest() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) facesContext.getExternalContext().getRequest();
        return request;
    }

    /*
     * Método que crea la cookie, nombre y valor
     */
    public void setCookieValue(String name, String value) {
        Cookie cookie = null;
        Cookie[] cookies = getResquest().getCookies();
        for (Cookie c : cookies) {
            if (c.getName().equals(name)) {
                cookie = c;
                break;
            }
        }

        if (cookie != null) {
            cookie.setValue(value);
        } else {
            cookie = new Cookie(name, value);
            cookie.setPath(getResquest().getContextPath());
        }
        cookie.setMaxAge(-1);//Tiempo que dure la sesión del navegador
        getResponse().addCookie(cookie);
    }

    /*
     * Método que redirige a otra página si la cookie existe
     */
    public void redirect(String url) {
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        try {
            context.redirect(context.getRequestContextPath() + url);
        } catch (IOException ex) {
            Logger.getLogger(this.getClass().getSimpleName()).log(Level.SEVERE, null, ex);
        }
    }

    /*
     * Método que obtiene el valor de la cookie a partir del nombre
     */
    public String getCookieValue(String name) {
        Cookie[] cookies = getResquest().getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals(name)) {
                    return cookies[i].getValue();
                }
            }
        }
        return null;
    }

    /*
     * Método que elimina la cookie a partir del nombre
     */
    public void removeCookie(String name) {
        Cookie cookie = null;
        Cookie[] cookies = getResquest().getCookies();
        for (Cookie c : cookies) {
            if (c.getName().equals(name)) {
                cookie = c;
                break;
            }
        }

        if (cookie != null) {
            cookie.setValue("");
        } else {
            cookie = new Cookie(name, "");
            cookie.setPath(getResquest().getContextPath());
        }
        cookie.setMaxAge(0);
        getResponse().addCookie(cookie);
    }

}
