/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pdc.login.backing;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;

import utils.MyCookie;

/**
 *
 * @author dmmaga
 */
@Named
@ViewScoped
public class FormView implements Serializable{
    @PostConstruct
    void init() {
        //al iniciar que se haga la conexión con LDAP
        try {
            //localhost o IP de la máquina virtual
            config.setLdapHost("192.168.122.195");
            config.setLdapPort(389);
            dn = new Dn(ADMIN_DN);
            connection = new LdapNetworkConnection(config);
            connection.setTimeOut(-1);
            connection.bind(dn, ADMIN_PWD);

        } catch (LdapException ex) {
            Logger.getLogger(FormView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
   //Variables del formulario con sus getter y setter
    private String usuario;
    private String nombre;
    private String apellido;
    private String contrasenia;
    private String busqueda;

    public String getBusqueda() {
        return busqueda;
    }

    public void setBusqueda(String busqueda) {
        this.busqueda = busqueda;
    }

    public String getContrasenia() {
        return contrasenia;
    }

    public void setContrasenia(String contrasenia) {
        this.contrasenia = contrasenia;
    }
    
        public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }
    
    //Variables para LDAP, modificar según dominio (pupusa, nuegado etc), poner credenciales
    public static final String DIR_ROOT = "ou=usuarios,dc=nuegado,dc=occ,dc=ues,dc=edu,dc=sv";
    public static final String ADMIN_DN = "cn=admin,dc=nuegado,dc=occ,dc=ues,dc=edu,dc=sv";
    public static final String ADMIN_PWD = "dmmagaldap";
    LdapConnectionConfig config = new LdapConnectionConfig();
    LdapConnection connection;
    Dn dn;

    //método para buscar un usuario en el árbol LDAP
    public void buscarLdap(){
        if (!busqueda.isEmpty()) {
            try {
            EntryCursor cursor = connection.search("ou=usuarios,dc=nuegado,dc=occ,dc=ues,dc=edu,dc=sv", "(uid="+busqueda+")", SearchScope.SUBTREE);
             System.out.println("IMPRIMIENDO RESULTADOS");
                for (Entry entry : cursor) {
                System.out.println(entry);
                }
            }
            catch ( LdapException ex) {
            Logger.getLogger(FormView.class.getName()).log(Level.SEVERE, null, ex);
            }
        }       
    }
    
    //método para crear un usuario en el árbol LDAP
    public void crearLdap(){
        if (!usuario.isEmpty() && !nombre.isEmpty() && !apellido.isEmpty() && !contrasenia.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            builder.append("uid="+usuario+","+DIR_ROOT);
            String dnInsertar=builder.toString();

            try {
                connection.add(
                        new DefaultEntry(
                                dnInsertar, // The Dn
                                "objectClass: inetOrgPerson",
                                "objectClass: AsteriskSIPUser",
                                "objectClass: AsteriskIAXUser",
                                "objectClass: organizationalPerson",
                                "objectClass: AsteriskExtension",
                                "objectClass: person",
                                "objectClass: top",
                                "AstAccountCallerID", nombre+" "+apellido ,
                                "cn", nombre,
                                "sn", apellido,
                                "uid", usuario,
                                "AstAccountRealmedPassword", contrasenia
                            ) );
            } catch (LdapException ex) {
                Logger.getLogger(FormView.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }
    
}
