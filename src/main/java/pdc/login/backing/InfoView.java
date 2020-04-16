/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pdc.login.backing;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import pdc.login.entity.Usuario;
import utils.MyCookie;
import utils.Serializacion;

/**
 *
 * @author dmmaga
 */
@Named
@ViewScoped
public class InfoView implements Serializable {

    private Usuario usuario;
    private MyCookie myCookie;

    @PostConstruct
    void init() {

        myCookie = new MyCookie();
        if (myCookie.getCookieValue("usuario") == null) {
            myCookie.redirect("/index.jsf");
        } else {

            String s = myCookie.getCookieValue("usuario");
            try {
                usuario = (Usuario) Serializacion.fromString(s);
            } catch (IOException | ClassNotFoundException e) {
                Logger.getLogger(this.getClass().getClass().getSimpleName()).log(Level.SEVERE, null, e);
            }
        }
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public void logout() {
        myCookie.removeCookie("usuario");
        myCookie.redirect("/index.jsf");
    }
     
    //Para mostrar en qué hostname está conectado
    public String getHostname() {
        InetAddress ip;
        String hostname = "";
        try {
            ip = InetAddress.getLocalHost();
            hostname = ip.getHostName();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hostname;
    }
    
}
