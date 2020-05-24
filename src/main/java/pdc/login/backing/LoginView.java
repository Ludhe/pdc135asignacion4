/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pdc.login.backing;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import org.apache.directory.api.ldap.model.exception.LdapException;
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

    private String usuario;
    private String password;
    private MyCookie myCookie;
    Usuario user; //el usuario que se logge
    int connectionStatus;

    @PostConstruct
    void init() {
        myCookie = new MyCookie();
        if (myCookie.getCookieValue("session") != null) {
            myCookie.redirect("/form.jsf");
        }
    }

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
    //1: todo ok
    //2: error en las credenciales
    //3: error de conexión
    public void login() throws IOException, LdapException {
        if (usuario != null && !usuario.isEmpty() && password != null && !password.isEmpty()) {
            user = new Usuario(usuario, password);
            connectionStatus = Login.checkLogin(user);

            switch (connectionStatus) {
                case 1:
                    String s = Serializacion.toString(user);
                    myCookie.setCookieValue("session", s);
                    myCookie.redirect("/form.jsf");
                    break;
                case 2:
                    addMessage("Las credenciales ingresadas son incorrectas", true);
                    break;
                case 3:
                    addMessage("Error de conexión con el servidor", true);
                    break;
            }
        } else {
            addMessage("Ingrese los campos obligatorios", true);
        }
    }

    //Para mostrar en qué hostname está conectado
//    public String getHostname() {
//        InetAddress ip;
//        String hostname = "";
//        try {
//            ip = InetAddress.getLocalHost();
//            hostname = ip.getHostName();
//        } catch (UnknownHostException e) {
//            Logger.getLogger(this.getClass().getClass().getSimpleName()).log(Level.SEVERE, null, e);
//        }
//        return hostname;
//    }

    //Método para agregar agregar notificaciones
    public void addMessage(String summary, boolean isError) {
        FacesMessage message = new FacesMessage(isError ? FacesMessage.SEVERITY_ERROR : FacesMessage.SEVERITY_INFO, summary, null);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

}
