/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pdc.login.backing;

import java.io.IOException;
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
import pdc.login.entity.Usuario;

import utils.MyCookie;
import utils.Serializacion;

/**
 *
 * @author dmmaga
 */
@Named
@ViewScoped
public class FormView implements Serializable{
    @PostConstruct
    void init() {
        
        myCookie = new MyCookie();
        if (myCookie.getCookieValue("session") == null) {
            myCookie.redirect("/index.jsf");
        } else {
            String s = myCookie.getCookieValue("session");
            try {
                user = (Usuario) Serializacion.fromString(s);
            } catch (IOException | ClassNotFoundException e) {
                Logger.getLogger(this.getClass().getClass().getSimpleName()).log(Level.SEVERE, null, e);
            }
        }
        
        //voy a poner esto en una clase, te lo prometo ah xd
        try {
            config.setLdapHost("192.168.122.195");
            config.setLdapPort(389);
            dn = new Dn(user.getUid());
            connection = new LdapNetworkConnection(config);
            connection.setTimeOut(-1);
            connection.bind(dn, user.getPass());

        } catch (LdapException ex) {
            Logger.getLogger(FormView.class.getName()).log(Level.SEVERE, null, ex);
        }
        //esto no debe ir aquí
        buscarLdap();
    }
    
    //usuario de la conexión y su respectiva cookie
    private MyCookie myCookie;
    private Usuario user;
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
    
    //Variables del directorio LDAP, modificar dominio
    public static final String DIR_ROOT = "ou=usuarios,dc=nuegado,dc=occ,dc=ues,dc=edu,dc=sv";
    
    //esto lo voy a borrar cuando este la clase
    LdapConnectionConfig config = new LdapConnectionConfig();
    LdapConnection connection;
    Dn dn;

    //método para buscar solamente un usuario en el árbol LDAP
    public void buscarUnoLdap(){
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
    
    //método para devolver todos los  usuarios en el árbol LDAP
    public void buscarLdap(){
        try {
        EntryCursor cursor = connection.search("ou=usuarios,dc=nuegado,dc=occ,dc=ues,dc=edu,dc=sv", "(uid=*)", SearchScope.SUBTREE);
         System.out.println("IMPRIMIENDO RESULTADOS");
            for (Entry entry : cursor) {
            System.out.println(entry.get("uid"));
            }
        }
        catch ( LdapException ex) {
        Logger.getLogger(FormView.class.getName()).log(Level.SEVERE, null, ex);
        }       
    }
    
    //método para crear un usuario en el árbol LDAP
    public void crearLdap(){
        if (!usuario.isEmpty() && !nombre.isEmpty() && !apellido.isEmpty() && !contrasenia.isEmpty()) {
            //encriptar contraseña
            System.out.println(contrasenia);            
            contrasenia = getHash(usuario+":asterisk:"+contrasenia, "MD5");
            System.out.println(contrasenia);
            
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
                                "AstAccountCallerID", nombre+" "+apellido,
                                "cn", nombre,
                                "sn", apellido,
                                "uid", usuario,
                                "AstAccountRealmedPassword", contrasenia,
                                "AstAccountType: friend",
                                "AstAccountHost: dynamic"
                            ) );
            } catch (LdapException ex) {
                Logger.getLogger(FormView.class.getName()).log(Level.SEVERE, null, ex);
            }
        }        
    }
    
    //método para encriptar la contraseña
    /* Retorna un hash a partir de un tipo y un texto */
    public static String getHash(String txt, String hashType) {
            try {
                    java.security.MessageDigest md = java.security.MessageDigest
                                    .getInstance(hashType);
                    byte[] array = md.digest(txt.getBytes());
                    StringBuffer sb = new StringBuffer();
                    for (int i = 0; i < array.length; ++i) {
                            sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100)
                                            .substring(1, 3));
                    }
                    return sb.toString();
            } catch (java.security.NoSuchAlgorithmException e) {
                    System.out.println(e.getMessage());
            }
            return null;
    }
    
    
    
}
