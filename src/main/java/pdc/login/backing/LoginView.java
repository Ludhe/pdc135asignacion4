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
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
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
        if (myCookie.getCookieValue("session")!= null) {
            myCookie.redirect("/form.jsf");
        }
    }
    
    LdapConnectionConfig config = new LdapConnectionConfig();
    LdapConnection connection;
    Dn dn;

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
            if (usuario.equals("admin") && password.equals("dmmagaldap")) { //meper donas
                 try {
                    config.setLdapHost("192.168.122.195"); //ip (o localhost) de donde se encuentra el servidor ldap
                    config.setLdapPort(389);
                    usuario = "cn="+usuario+",dc=nuegado,dc=occ,dc=ues,dc=edu,dc=sv";
                    dn = new Dn(usuario);
                    connection = new LdapNetworkConnection(config);
                    connection.setTimeOut(-1);
                    connection.bind(dn, password);
                    
                    Usuario u = new Usuario(usuario, password);
                    String s = Serializacion.toString(u);
                    myCookie.setCookieValue("session", s);
                    myCookie.redirect("/form.jsf");
                } catch (LdapException ex) {
                    Logger.getLogger(FormView.class.getName()).log(Level.SEVERE, null, ex);
                     System.out.println("Ocurrió un error con el servidor (?");
                }
            } else {
                System.out.println("Credenciales incorrectas");
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
