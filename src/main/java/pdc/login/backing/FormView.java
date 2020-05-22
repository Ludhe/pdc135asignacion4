/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pdc.login.backing;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
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
    public static final String DOMAIN = "tipicos";
    public static final String DIR_ROOT = "ou=usuarios,dc=" + DOMAIN + ",dc=occ,dc=ues,dc=edu,dc=sv";
    private LdapConnection connection;
    private LdapConnection masterConnection;
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
        makeConnection(s);

    }

    public void makeConnection(String cookieValue) {
        try {
            user = (Usuario) Serializacion.fromString(cookieValue);

        } catch (IOException | ClassNotFoundException e) {
            Logger.getLogger(this.getClass().getClass().getSimpleName()).log(Level.SEVERE, null, e);
            myCookie.removeCookie("session");
            myCookie.redirect("/index.jsf");
        }
        
        try {
            connection = Login.tryLogin(user, 390);
            usuariosList = getLdapUsers();
        } catch (LdapException e) {
            addMessage("Se perdió conexión con el árbol de LDAP", true);
            myCookie.removeCookie("session");
            myCookie.redirect("/index.jsf");
        }
        
        try {
            masterConnection = Login.tryLogin(user, 389);
        } catch (LdapException e) {
            System.out.println("Error al conectarse con ldap maestro");
            Logger.getLogger(this.getClass().getClass().getSimpleName()).log(Level.SEVERE, null, e);
        }
    }

    public LdapConnection getMasterConnection() {
        return masterConnection;
    }

    public void setMasterConnection(LdapConnection masterConnection) {
        this.masterConnection = masterConnection;
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

    //método para el buscador 
    public void filterLdap() {

        if (!busqueda.isEmpty()) {
//            if (connection.isConnected()) {
                try {
                    EntryCursor cursor = connection.search(DIR_ROOT, "(&(objectClass=AsteriskSIPUser)(uid=" + busqueda + "*))", SearchScope.SUBTREE);
                    usuariosList = new ArrayList<>();
                    for (Entry entry : cursor) {
                        Usuarioldap userCursor = new Usuarioldap(entry.get("uid").getString(), entry.get("sn").getString(), entry.get("cn").getString(), null, entry.get("AstAccountCallerID").getString());
                        usuariosList.add(userCursor);
                    }
                } catch (LdapException ex) {
                    Logger.getLogger(FormView.class.getName()).log(Level.SEVERE, null, ex);
                }
//            } else {
//                addMessage("Se perdió conexión con el árbol de LDAP", true);
//                usuariosList = getLdapUsers();
//            }
        }
    }

    //método para devolver todos los  usuarios en el árbol LDAP
    public List<Usuarioldap> getLdapUsers() {
        List<Usuarioldap> list = new ArrayList<>();
        try {
            EntryCursor cursor = connection.search(DIR_ROOT, "(objectClass=AsteriskSIPUser)", SearchScope.SUBTREE);
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
//            if (masterConnection.isConnected()) {
                //encriptar contraseña
                String passMD5 = getHash(tempUser.getUid() + ":asterisk:" + tempUser.getPass(), "MD5");
                tempUser.setPass(passMD5);

                StringBuilder builder = new StringBuilder();
                builder.append("uid=").append(tempUser.getUid()).append("," + DIR_ROOT);
                String dnInsertar = builder.toString();

                try {
                    masterConnection.add(
                            new DefaultEntry(
                                    dnInsertar, // The Dn
                                    "objectClass: inetOrgPerson",
                                    "objectClass: AsteriskSIPUser",
                                    "objectClass: organizationalPerson",
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

//            } else {
//                addMessage("En ese momento el servidor no está disponible para crear usuarios", true);
//            }
        }
    }

    //Método para editar el usuario seleccionado
    public void editarLdap() {
        if (selectedUsuario.isValid() && selectedUsuario != null) {
//            if (masterConnection.isConnected()) {
                try {
                    Dn dn = new Dn("uid=" + selectedUsuario.getUid() + "," + DIR_ROOT);
                    String passMD5 = getHash(selectedUsuario.getUid() + ":asterisk:" + selectedUsuario.getPass(), "MD5");
                    selectedUsuario.setPass(passMD5);
                    Modification modCN = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "cn", selectedUsuario.getCn());
                    Modification modSN = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "sn", selectedUsuario.getSn());
                    Modification modPass = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "AstAccountRealmedPassword", selectedUsuario.getPass());
                    Modification modCallerID = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "AstAccountCallerID", selectedUsuario.getCn() + " " + selectedUsuario.getSn());

                    masterConnection.modify(dn, modCN, modSN, modPass, modCallerID);
                    usuariosList = getLdapUsers();
                    PrimeFaces current = PrimeFaces.current();
                    current.executeScript("PF('ouEditar').hide();");
                    addMessage("Se editó el usuario", false);
                } catch (LdapException ex) {
                    Logger.getLogger(FormView.class.getName()).log(Level.SEVERE, null, ex);
                    addMessage("Ocurrió un error con el servidor al tratar de editar el usuario", true);
                }
//            } else {
//                addMessage("En ese momento el servidor no está disponible para editar usuarios", true);
//            }
        }
    }

    //Método para eliminar el usuario que está seleccionado
    public void eliminarLdap() {
        if (!selectedUsuario.getUid().isEmpty() && selectedUsuario != null) {
//            if (masterConnection.isConnected()) {
                try {
                    masterConnection.delete("uid=" + selectedUsuario.getUid() + "," + DIR_ROOT);
                    usuariosList = getLdapUsers();
                    selectedUsuario = null;
                    addMessage("Se eliminó el usuario", false);
                } catch (LdapException ex) {
                    Logger.getLogger(FormView.class.getName()).log(Level.SEVERE, null, ex);
                    addMessage("Ocurrió un error con el servidor al tratar de eliminar el usuario", true);
                }
//            } else {
//                addMessage("En ese momento el servidor no está disponible para eliminar usuarios", true);
//            }
        }
    }

    //Método para cerrar la conexión y la sesión
    public void logout() {
        try {
            connection.unBind();
            connection.close();
            masterConnection.unBind();
            masterConnection.close();
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
            StringBuilder sb = new StringBuilder();
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

    //Método para agregar los mensajes a las notificaciones 
    public void addMessage(String summary, boolean isError) {
        FacesMessage message = new FacesMessage(isError ? FacesMessage.SEVERITY_ERROR : FacesMessage.SEVERITY_INFO, summary, null);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }
    
    //Para mostrar en qué hostname está conectado
    public String getHostname() {
        InetAddress ip;
        String hostname = "";
        try {
            ip = InetAddress.getLocalHost();
            hostname = ip.getHostName();
        } catch (UnknownHostException e) {
            Logger.getLogger(this.getClass().getClass().getSimpleName()).log(Level.SEVERE, null, e);
        }
        return hostname;
    }
}
