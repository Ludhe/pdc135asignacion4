/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pdc.login.backing;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnection;
import pdc.login.entity.Usuario;
import utils.Login;
import utils.MyCookie;
import utils.Serializacion;

/**
 *
 * @author dmmaga
 */
@Named
@ViewScoped
public class FormView implements Serializable {

    @PostConstruct
    void init() {
        myCookie = new MyCookie();
        if (myCookie.getCookieValue("session") == null) {
            myCookie.redirect("/index.jsf");
        } else {
            String s = myCookie.getCookieValue("session");
            try {
                user = (Usuario) Serializacion.fromString(s);
                connection = Login.tryLogin(user);
                if (connection.isConnected()) {
                }
            } catch (IOException | ClassNotFoundException e) {
                Logger.getLogger(this.getClass().getClass().getSimpleName()).log(Level.SEVERE, null, e);
            }
        }
    }

    //usuario de la conexión y su respectiva cookie
    private MyCookie myCookie;
    private Usuario user;
    //Variables del directorio LDAP, modificar dominio
    //public static final String host = "yuca";
    public static final String DIR_ROOT = "ou=usuarios,dc=nuegado,dc=occ,dc=ues,dc=edu,dc=sv";
    LdapConnection connection;
    //Variables del formulario con sus getter y setter
    private String usuario;
    private String nombre;
    private String apellido;
    private String contrasenia;
    private String busqueda;
    private FormView view;

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

    public FormView getView() {
        return view;
    }

    public void setView(FormView view) {
        this.view = view;
    }

    //método para buscar solamente un usuario en el árbol LDAP
    public void buscarUnoLdap() {
        if (!busqueda.isEmpty()) {
            try {
                EntryCursor cursor = connection.search("ou=usuarios,dc=nuegado,dc=occ,dc=ues,dc=edu,dc=sv", "(uid=" + busqueda + ")", SearchScope.SUBTREE);
                System.out.println("IMPRIMIENDO RESULTADOS");
                for (Entry entry : cursor) {
                    System.out.println(entry);
                }
            } catch (LdapException ex) {
                Logger.getLogger(FormView.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    //método para devolver todos los  usuarios en el árbol LDAP
    public List<FormView> buscarLdap() {
        List<FormView> list = new ArrayList<>();
        FormView view;
        try {
            EntryCursor cursor = connection.search("ou=usuarios,dc=nuegado,dc=occ,dc=ues,dc=edu,dc=sv", "(uid=*)", SearchScope.SUBTREE);
            //System.out.println("IMPRIMIENDO RESULTADOS");
            for (Entry entry : cursor) {
                //System.out.println(entry.get("uid"));
                view = new FormView();
                if (cursor == null) {
                    view.setNombre("sin datos");
                    view.setApellido("sin datos");
                    view.setUsuario("sin datos");
                    list.add(view);
                } else {
                    view.setNombre(entry.get("cn").getString());
                    view.setApellido(entry.get("sn").getString());
                    view.setUsuario(entry.get("uid").getString());
                    //view.setContrasenia(entry.get("AstAccountRealmedPassword").getString());
                    view.setBusqueda(entry.get("uid").getString());
                    //System.out.println("objeto");
                    //System.out.println(view.getNombre() + " " + view.getApellido() + " " + view.getUsuario());
                    list.add(view);
                }
            }
        } catch (LdapException ex) {
            Logger.getLogger(FormView.class.getName()).log(Level.SEVERE, null, ex);
        }
        return list;
    }

    //método para crear un usuario en el árbol LDAP
    public void crearLdap() {
        if (!usuario.isEmpty() && !nombre.isEmpty() && !apellido.isEmpty() && !contrasenia.isEmpty()) {
            //encriptar contraseña
            System.out.println(contrasenia);
            contrasenia = getHash(usuario + ":asterisk:" + contrasenia, "MD5");
            System.out.println(contrasenia);

            StringBuilder builder = new StringBuilder();
            builder.append("uid=" + usuario + "," + DIR_ROOT);
            String dnInsertar = builder.toString();

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
                                "AstAccountCallerID", nombre + " " + apellido,
                                "cn", nombre,
                                "sn", apellido,
                                "uid", usuario,
                                "AstAccountRealmedPassword", contrasenia,
                                "AstAccountType: friend",
                                "AstAccountHost: dynamic"
                        ));
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

    public void logout() throws IOException {
        try {
            connection.unBind();
            connection.close();
        } catch (LdapException ex) {
            Logger.getLogger(FormView.class.getName()).log(Level.SEVERE, null, ex);
        }
        myCookie.removeCookie("session");
        myCookie.redirect("/index.jsf");
    }

    public void eliminar() throws LdapException {
        //System.out.println("curso seleccionado "+ this.view.getNombre());
        if (!(this.view == null)) {
            connection.delete("uid=" + this.view.getUsuario() + ",ou=usuarios,dc=nuegado,dc=occ,dc=ues,dc=edu,dc=sv");
        }
    }

    public void Editar() throws LdapException {
        if (!(this.view.getUsuario().isEmpty() && this.view.getNombre().isEmpty() && this.view.getApellido().isEmpty() && this.view.getContrasenia().isEmpty())) {
            //System.out.println("Usuario 0.0 " + this.view.getUsuario());
            //this.view.contrasenia = getHash(this.view.getUsuario() + ":asterisk:" + this.view.getContrasenia(), "MD5");
            Modification modiNombre = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "cn", this.view.getNombre());
            Modification modiApellido = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "sn", this.view.getApellido());
            Modification modiContrasenia = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "AstAccountRealmedPassword", this.view.getContrasenia());
            Modification modiID = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "AstAccountCallerID", this.view.getNombre() + " " + this.view.getApellido());
            
            connection.modify("uid=" + this.view.getUsuario() + ",ou=usuarios,dc=nuegado,dc=occ,dc=ues,dc=edu,dc=sv",modiApellido,modiNombre,modiContrasenia,modiID);
        }

    }
}
