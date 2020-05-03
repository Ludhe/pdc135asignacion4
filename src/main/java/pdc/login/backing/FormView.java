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
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapEntryAlreadyExistsException;
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

    //usuario de la conexión y su respectiva cookie
    private MyCookie myCookie;
    private Usuario user;
    //Variables del directorio LDAP, modificar dominio
    public static final String DOMAIN = "nuegado";
    public static final String DIR_ROOT = "ou=usuarios,dc=" + DOMAIN + ",dc=occ,dc=ues,dc=edu,dc=sv";
    private LdapConnection connection;
    //Variables del formulario 
    private Usuarioldap tempUser;
    private String busqueda;
    private Usuarioldap selectedUsuario;
    private List<Usuarioldap> usuariosList;

    @PostConstruct
    void init() {
        myCookie = new MyCookie();
        tempUser = new Usuarioldap();
        if (myCookie.getCookieValue("session") == null) {
            myCookie.redirect("/index.jsf");
        }

    }

    public void onload() {
        String s = myCookie.getCookieValue("session");
        try {
            user = (Usuario) Serializacion.fromString(s);
            try {
                connection = Login.tryLogin(user);
                usuariosList = getLdapUsers();
            } catch (Exception e) {
                addMessage("Se perdió conexión con el árbol de LDAP", true);
            }
        } catch (IOException | ClassNotFoundException e) {
            Logger.getLogger(this.getClass().getClass().getSimpleName()).log(Level.SEVERE, null, e);
            myCookie.removeCookie("session");
            myCookie.redirect("/index.jsf");
        }

    }    
    
    public LdapConnection getConnection() {
        return connection;
    }

    public void setConnection(LdapConnection connection) {
        this.connection = connection;
    }

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

    //método para buscar en el árbol LDAP
    public void filterLdap() {

        if (!busqueda.isEmpty()) {

            try {
                EntryCursor cursor = connection.search(DIR_ROOT, "(uid=" + busqueda + "*)", SearchScope.SUBTREE);
                usuariosList = new ArrayList<>();
                for (Entry entry : cursor) {
                    Usuarioldap userCursor = new Usuarioldap(entry.get("uid").getString(), entry.get("sn").getString(), entry.get("cn").getString(), null, entry.get("AstAccountCallerID").getString());
                    usuariosList.add(userCursor);
                }
            } catch (LdapException ex) {
                Logger.getLogger(FormView.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            usuariosList = getLdapUsers();
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
            String passMD5 = getHash(tempUser.getUid() + ":asterisk:" + tempUser.getPass(), "MD5");
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
                addMessage("Se creó el nuevo usuario", false);
            } catch (LdapException ex) {
                Logger.getLogger(FormView.class.getName()).log(Level.SEVERE, null, ex);
                if (ex.getClass() == LdapEntryAlreadyExistsException.class) {
                    addMessage("El usuario ya existe dentro de la base de datos", true);
                } else {
                    addMessage("Ocurrió un error con el servidor al tratar de crear el usuario", true);
                }
            }
        }
    }

    //Método para editar el usuario seleccionado
    public void editarLdap() {
        if (selectedUsuario.isValid() && selectedUsuario != null) {
            try {
                Dn dn = new Dn("uid=" + selectedUsuario.getUid() + "," + DIR_ROOT);
                String passMD5 = getHash(selectedUsuario.getUid() + ":asterisk:" + selectedUsuario.getPass(), "MD5");
                selectedUsuario.setPass(passMD5);
                Modification modCN = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "cn", selectedUsuario.getCn());
                Modification modSN = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "sn", selectedUsuario.getSn());
                Modification modPass = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "AstAccountRealmedPassword", selectedUsuario.getPass());
                Modification modCallerID = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "AstAccountCallerID", selectedUsuario.getCn() + " " + selectedUsuario.getSn());

                connection.modify(dn, modCN, modSN, modPass, modCallerID);
                usuariosList = getLdapUsers();
                PrimeFaces current = PrimeFaces.current();
                current.executeScript("PF('ouEditar').hide();");
                addMessage("Se editó el usuario", false);
            } catch (LdapException ex) {
                Logger.getLogger(FormView.class.getName()).log(Level.SEVERE, null, ex);
                addMessage("Ocurrió un error con el servidor al tratar de editar el usuario", true);
            }

        }

    }

    //Método para eliminar el usuario que está seleccionado
    public void eliminarLdap() {
        if (!selectedUsuario.getUid().isEmpty() && selectedUsuario != null) {
            try {
                connection.delete("uid=" + selectedUsuario.getUid() + "," + DIR_ROOT);
                usuariosList = getLdapUsers();
                selectedUsuario = null;
                addMessage("Se eliminó el usuario", false);
            } catch (LdapException ex) {
                Logger.getLogger(FormView.class.getName()).log(Level.SEVERE, null, ex);
                addMessage("Ocurrió un error con el servidor al tratar de eliminar el usuario", true);
            }
        }
    }

    //Método para cerrar la conexión y la sesión
    public void logout() {
        try {
            connection.unBind();
            connection.close();
        } catch (LdapException | IOException ex) {
            Logger.getLogger(FormView.class.getName()).log(Level.SEVERE, null, ex);
            addMessage("Ocurrió un error con el servidor al tratar cerrar la conexión", true);
        }
        myCookie.removeCookie("session");
        myCookie.redirect("/index.jsf");
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

    public void addMessage(String summary, boolean isError) {
        FacesMessage message = new FacesMessage(isError ? FacesMessage.SEVERITY_ERROR : FacesMessage.SEVERITY_INFO, summary, null);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }
}
