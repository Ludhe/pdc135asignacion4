/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import pdc.login.entity.Usuario;

/**
 *
 * @author dmmaga
 */
public class Login {

    public Login() {

    }

    //Método para realizar la conexión con la base de datos
    public static LdapConnection tryLogin(Usuario usuario, int puerto) throws LdapException {
        LdapConnectionConfig connectionConfig = new LdapConnectionConfig();
        LdapConnection connection;
        Dn dn;
        final String USER;
        final String DOMAIN = "tipicos";      
        connectionConfig.setLdapHost("ldap://25.16.250.146");
        connectionConfig.setLdapPort(puerto);
        USER = "cn=" + usuario.getUid() + ",dc=" + DOMAIN + ",dc=occ,dc=ues,dc=edu,dc=sv";
        try {
            dn = new Dn(USER);
            connection = new LdapNetworkConnection(connectionConfig);
            connection.setTimeOut(-1);
            connection.bind(dn, usuario.getPass());
        } catch (LdapException ex) {
            Logger.getLogger(Login.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        }
        return connection;
    }
    

    //Método para validar a la hora de hacer el login
    //1: todo ok
    //2: error en las credenciales
    //3: error de conexión
    public static int checkLogin(Usuario usuario) throws IOException {
        LdapConnectionConfig connectionConfig = new LdapConnectionConfig();
        LdapConnection connection;
        Dn dn;
        final String USER;
        final String DOMAIN = "tipicos";
        connectionConfig.setLdapHost("ldap://25.16.250.146");
        connectionConfig.setLdapPort(390);
        USER = "cn=" + usuario.getUid() + ",dc=" + DOMAIN + ",dc=occ,dc=ues,dc=edu,dc=sv";
        try {
            dn = new Dn(USER);
            connection = new LdapNetworkConnection(connectionConfig);
            connection.setTimeOut(-1);
            connection.bind(dn, usuario.getPass());
            connection.unBind();
            connection.close();
        } catch (LdapException ex) {
            if (ex.getClass() == LdapAuthenticationException.class) {
                Logger.getLogger(Login.class.getName()).log(Level.SEVERE, null, ex);
                return 2;
            }
            Logger.getLogger(Login.class.getName()).log(Level.SEVERE, null, ex);
            return 3;
        }
        return 1;
    }

}
