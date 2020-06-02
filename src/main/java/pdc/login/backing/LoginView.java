/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pdc.login.backing;

import java.io.IOException;
import java.io.Serializable;
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
        //En el inicio validar si la cookie existe, si ya existe, redirije al formulario
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

    /*
    * Método que se ejecuta al dar click al botón iniciar del Login
    * Case 1: Se inició la sesión sin errores
    * Case 2: Hubo un error con las credenciales proporcionadas
    * Case 3: Hubo un error en la conexión con el servidor
     */
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

    /*
    * Método que agrega las notificaciones tipo "growl" de Primefaces
    * @param summary pequeño resumen en texto de la notificación a mostrar
    * @param isError boolean: true si el mensaje es de error, false si el mensaje es solo una notificación  
     */
    public void addMessage(String summary, boolean isError) {
        FacesMessage message = new FacesMessage(isError ? FacesMessage.SEVERITY_ERROR : FacesMessage.SEVERITY_INFO, summary, null);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

}
