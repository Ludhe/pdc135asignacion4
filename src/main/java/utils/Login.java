/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;

/**
 *
 * @author dmmaga
 */
public class Login {
    
    public Login(){
        
    }
    
    public static boolean tryLogin(String usuario, String password){
        LdapConnectionConfig connectionConfig = new LdapConnectionConfig();
        LdapConnection connection;
        Dn dn;
        connectionConfig.setLdapHost("192.168.122.195");
        connectionConfig.setLdapPort(389);
        usuario = "cn="+usuario+",dc=nuegado,dc=occ,dc=ues,dc=edu,dc=sv"; 
        try {
                dn = new Dn(usuario);
                connection = new LdapNetworkConnection(connectionConfig);
                connection.setTimeOut(-1);                   
                connection.bind(dn, password);
                System.out.println(connection);
            } catch (LdapException ex) {
                Logger.getLogger(Login.class.getName()).log(Level.SEVERE, null, ex);
                return false;                
            }
        return true;
    }
     
}
