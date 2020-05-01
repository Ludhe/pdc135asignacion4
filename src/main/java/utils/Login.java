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
import pdc.login.entity.Usuario;

/**
 *
 * @author dmmaga
 */
public class Login {
    
    public Login(){
        
    }
    
    public static LdapConnection tryLogin(Usuario usuario){
        LdapConnectionConfig connectionConfig = new LdapConnectionConfig();
        LdapConnection connection;
        Dn dn;
        String user;
        connectionConfig.setLdapHost("192.168.122.195");
        connectionConfig.setLdapPort(389);
        user = "cn="+usuario.getUid()+",dc=nuegado,dc=occ,dc=ues,dc=edu,dc=sv"; 
        try {
                dn = new Dn(user);
                connection = new LdapNetworkConnection(connectionConfig);
                connection.setTimeOut(-1);                   
                connection.bind(dn, usuario.getPass());
            } catch (LdapException ex) {
                Logger.getLogger(Login.class.getName()).log(Level.SEVERE, null, ex);               
                return null;                
            }
        return connection;
    }
    
    
}
