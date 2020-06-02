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

    /**
     * Variables necesarias para el login
     */
    private static String USER;
    private final static String DOMAIN = "pupusa";
    //25.16.250.146
    private final static String TREE_IP = "localhost";
    private static LdapConnectionConfig connectionConfig = new LdapConnectionConfig();
    private static LdapConnection connection;
    private static Dn dn;

    /**
     * Método para hacer la conexión con la base de datos de directorio
     *
     * @param usuario Es el usuario con el que se va a iniciar sesión
     * @param puerto Puerto de la IP en el cuál se encuentra la base de datos
     * @return Regresa la conexión con la base de datos
     * @throws org.apache.directory.api.ldap.model.exception.LdapException
     */
    public static LdapConnection tryLogin(Usuario usuario, int puerto) throws LdapException {

        connectionConfig.setLdapHost(TREE_IP);
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

    /**
     * Método para checkear si se puede iniciar sesión en el servidor con las
     * credenciales dadas
     *
     * @param usuario Es el usuario con el que se va a iniciar sesión
     * @return 1: Se inició la sesión sin errores
     *         2: Hubo un error con las credenciales proporcionadas 
     *         3: Hubo un error en la conexión con el servidor
     * ************* 390 port
     * @throws java.io.IOException
     */
    public static int checkLogin(Usuario usuario) throws IOException {

        connectionConfig.setLdapHost(TREE_IP);
        connectionConfig.setLdapPort(389);
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
