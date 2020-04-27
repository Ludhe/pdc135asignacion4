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
import org.apache.directory.ldap.client.api.LdapConnection;
import pdc.login.entity.Usuario;
import utils.MyCookie;
import utils.Serializacion;
import utils.Login;
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
        if (myCookie.getCookieValue("session")!= null) {
            myCookie.redirect("/form.jsf");
        }
    }

    private String usuario;
    private String password;
    private MyCookie myCookie;
    Usuario user; //el usuario que se logge
    LdapConnection connection;


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
            user = new Usuario(usuario, password);
            connection = Login.tryLogin(user);
            
            if (connection != null && connection.isConnected()) {                               
                String s = Serializacion.toString(user);
                myCookie.setCookieValue("session", s);
                myCookie.redirect("/form.jsf");
            } else {
                System.out.println("aqui debo manejar los errores");
            }                                       
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
