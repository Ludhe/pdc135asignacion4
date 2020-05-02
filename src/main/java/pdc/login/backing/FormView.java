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
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.primefaces.PrimeFaces;
import pdc.login.entity.Usuario;
import pdc.login.entity.Usuarioldap;
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
        tempUser = new Usuarioldap();
        if (myCookie.getCookieValue("session") == null) {
            myCookie.redirect("/index.jsf");
        } else {
            String s = myCookie.getCookieValue("session");
            try {
                user = (Usuario) Serializacion.fromString(s);
                connection = Login.tryLogin(user);
                if (connection.isConnected()) {
                    usuariosList = getLdapUsers();
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
    public static final String hosti = "nuegado";
    public static final String DIR_ROOT = "ou=usuarios,dc="+hosti+",dc=occ,dc=ues,dc=edu,dc=sv";
    LdapConnection connection;
    //Variables del formulario con sus getter y setter
    private Usuarioldap tempUser;
    private String busqueda;
    private Usuarioldap selectedUsuario;
    private List<Usuarioldap> usuariosList;
    
    public Usuarioldap getTempUser() {
        return tempUser;
    }

    public void setTempUser(Usuarioldap tempUser) {
        this.tempUser = tempUser;
    }
    
    public String getBusqueda() {
        return busqueda;
    }

    public void setBusqueda(String busqueda) {
        this.busqueda = busqueda;
    }
    
    public Usuarioldap getSelectedUsuario() {
        return selectedUsuario;
    }

    public void setSelectedUsuario(Usuarioldap selectedUsuario) {
        this.selectedUsuario = selectedUsuario;
    }

    public List<Usuarioldap> getUsuariosList() {
        return usuariosList;
    }

    public void setUsuariosList(List<Usuarioldap> usuariosList) {
        this.usuariosList = usuariosList;
    }
    
    //método para buscar solamente un usuario en el árbol LDAP
    public void buscarUnoLdap() {
        if (!busqueda.isEmpty()) {
            try {
                EntryCursor cursor = connection.search(DIR_ROOT, "(uid=" + busqueda + ")", SearchScope.SUBTREE);
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
    public List<Usuarioldap> getLdapUsers() {
        List<Usuarioldap> list = new ArrayList<>();
        try {
            EntryCursor cursor = connection.search(DIR_ROOT, "(uid=*)", SearchScope.SUBTREE);
            for (Entry entry : cursor) {
                Usuarioldap userCursor = new Usuarioldap(entry.get("uid").getString(), entry.get("sn").getString(), entry.get("cn").getString(), null, entry.get("AstAccountCallerID").getString());
                list.add(userCursor);
            }
        } catch (LdapException ex) {
            Logger.getLogger(FormView.class.getName()).log(Level.SEVERE, null, ex);
        }
        return list;
    }

    //método para crear un usuario en el árbol LDAP
    public void crearLdap() {
        if (tempUser.isValid() && tempUser != null) {
            //encriptar contraseña
            String passMD5 = getHash(tempUser.getUid()+ ":asterisk:" + tempUser.getPass(), "MD5");
            tempUser.setPass(passMD5);

            StringBuilder builder = new StringBuilder();
            builder.append("uid=" + tempUser.getUid() + "," + DIR_ROOT);
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
                                "AstAccountCallerID", tempUser.getCn() + " " + tempUser.getSn(),
                                "cn", tempUser.getCn(),
                                "sn", tempUser.getSn(),
                                "uid", tempUser.getUid(),
                                "AstAccountRealmedPassword", passMD5,
                                "AstAccountType: friend",
                                "AstAccountHost: dynamic"
                        ));
                usuariosList = getLdapUsers();
                tempUser = new Usuarioldap();
                PrimeFaces current = PrimeFaces.current();
                    current.executeScript("PF('ouAgregar').hide();");
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

    public void eliminarLdap() throws LdapException {
        if (!selectedUsuario.getUid().isEmpty() && selectedUsuario != null) {
            connection.delete("uid=" + selectedUsuario.getUid() + ",ou=usuarios,dc="+hosti+",dc=occ,dc=ues,dc=edu,dc=sv");
            usuariosList = getLdapUsers();
            selectedUsuario = null;
        }
    }

    public void editarLdap() throws LdapException {
        if (selectedUsuario.isValid() && selectedUsuario != null) {
            Dn dn = new Dn("uid=" + selectedUsuario.getUid() + ",ou=usuarios,dc="+hosti+",dc=occ,dc=ues,dc=edu,dc=sv");
            String passMD5 = getHash(selectedUsuario.getUid()+ ":asterisk:" + selectedUsuario.getPass(), "MD5");
            selectedUsuario.setPass(passMD5);
            
            Modification modiNombre = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "cn", selectedUsuario.getCn());
            Modification modiApellido = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "sn", selectedUsuario.getSn());
            Modification modiContrasenia = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "AstAccountRealmedPassword", selectedUsuario.getPass());
            Modification modiID = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "AstAccountCallerID", selectedUsuario.getCallerid());
            
            connection.modify(dn, modiApellido, modiNombre, modiContrasenia, modiID);
            usuariosList = getLdapUsers();
            PrimeFaces current = PrimeFaces.current();
                    current.executeScript("PF('ouEditar').hide();");
        }

    }
}
