/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pdc.login.backing;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
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
public class LoginView implements Serializable {

    @PostConstruct
    void init() {
        myCookie = new MyCookie();
        if (myCookie.getCookieValue("usuario")!= null) {
            myCookie.redirect("/info.jsf");
        }
    }

    private String usuario;
    private String password;
    private MyCookie myCookie;

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    //Método al dar click al botón iniciar
    public void login() throws IOException {
        if (!usuario.isEmpty() && !password.isEmpty()) {
            Usuario u = new Usuario(usuario, password);
            String s = Serializacion.toString(u);
            myCookie.setCookieValue("usuario", s);
            myCookie.redirect("/info.jsf");
        }
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
