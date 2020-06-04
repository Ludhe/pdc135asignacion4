/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pdc.login.backing;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapEntryAlreadyExistsException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.bouncycastle.cert.X509CertificateHolder;
import org.primefaces.PrimeFaces;
import pdc.login.entity.Usuario;
import pdc.login.entity.Usuarioldap;
import utils.Certificado;
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
    private Usuarioldap selectedUsuario;
    private List<Usuarioldap> usuariosList;
    //Variables para certificados
    File caCert = new File("/certificadosRadius/ca.pem");
    Certificado certUtils = new Certificado();

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

    @PostConstruct
    void init() {
        //En el inicio validar si la cookie existe, si no existe, redirije al login
        myCookie = new MyCookie();
        tempUser = new Usuarioldap();
        if (myCookie.getCookieValue("session") == null) {
            myCookie.redirect("/index.jsf");
        }
    }

    /**
     * Método que se ejecute cuando la página ya haya terminado de cargar,
     * obtiene los usuarios, llena la tabla y abre las conexiónes con el árbol.
     */
    public void onload() {
        String s = myCookie.getCookieValue("session");
        makeConnection(s);
        makeMasterConnection(s);
    }

    /**
     * Método que realiza la conexión de solo lectura con el árbol LDAP
     *
     * @param cookieValue Valor de la cookie del usuario con el que se va a
     * hacer la conexión puerto 390******
     */
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
    }

    /**
     * Método que realiza la conexión de lectura/escritura con el árbol LDAP
     *
     * @param cookieValue Valor de la cookie del usuario con el que se va a
     * hacer la conexión
     */
    public void makeMasterConnection(String cookieValue) {
        try {
            user = (Usuario) Serializacion.fromString(cookieValue);

        } catch (IOException | ClassNotFoundException e) {
            Logger.getLogger(this.getClass().getClass().getSimpleName()).log(Level.SEVERE, null, e);
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

    /**
     * Método para devolver todos los usuarios del tipo "inetOrgPerson"
     *
     * @return Una lista de tipo Usuario, con los usuarios que se hayan
     * encontrado
     */
    public List<Usuarioldap> getLdapUsers() {
        List<Usuarioldap> list = new ArrayList<>();
        if (connection.isConnected()) {
            try {
                EntryCursor cursor = connection.search(DIR_ROOT, "(objectClass=inetOrgPerson)", SearchScope.SUBTREE);
                for (Entry entry : cursor) {
                    Usuarioldap userCursor = new Usuarioldap(entry.get("uid").getString(), entry.get("sn").getString(), entry.get("cn").getString(), null);
                    list.add(userCursor);
                }
            } catch (LdapException ex) {
                Logger.getLogger(FormView.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            addMessage("Se perdió conexión con el árbol de LDAP", true);
            usuariosList = getLdapUsers();
        }
        return list;
    }

    /**
     * Método para crear un usuario en el árbol LDAP
     *
     * @throws java.lang.Exception
     */
    public void crearLdap() throws Exception {
        if (tempUser.isValid() && tempUser != null) {
            if (masterConnection.isConnected()) {
                //Para los certificados
                X509CertificateHolder caHolder = certUtils.readPemCertificates(caCert);
                X509Certificate cert = certUtils.convertCertificate(caHolder);
                Pair<PrivateKey, Certificate> pair = certUtils.generateCert(tempUser.getUid() + "@tipicos.uesocc", cert);
                String encodedCert = certUtils.writeUserCert(tempUser, pair);
                //certUtils.writeEncryptedUserKey(tempUser.getUid(), pair);
                //encriptar contraseña
                String passMD5 = getHash(tempUser.getPass(), "MD5");
                tempUser.setPass(passMD5);
                //Para LDAP
                StringBuilder builder = new StringBuilder();
                builder.append("uid=").append(tempUser.getUid()).append("," + DIR_ROOT);
                String dnInsertar = builder.toString();
                try {
                    masterConnection.add(
                            new DefaultEntry(
                                    dnInsertar, // The Dn
                                    "objectClass: inetOrgPerson",
                                    "objectClass: organizationalPerson",
                                    "objectClass: person",
                                    "objectClass: top",
                                    "cn", tempUser.getCn(),
                                    "sn", tempUser.getSn(),
                                    "uid", tempUser.getUid(),
                                    "userPassword", passMD5,
                                    "userPKCS12", encodedCert
                            ));
                    usuariosList = getLdapUsers();

                    PrimeFaces current = PrimeFaces.current();
                    tempUser = new Usuarioldap();
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
            } else {
                addMessage("En ese momento el servidor no está disponible para crear usuarios", true);
            }
        }
    }

    public void downloadCertAndKey() throws IOException, LdapException {
        byte[] data = null;
        EntryCursor cursor = connection.search(DIR_ROOT, "(uid=" + selectedUsuario.getUid() + ")", SearchScope.SUBTREE);
        for (Entry entry : cursor) {
            data = Base64.getDecoder().decode(entry.get("userPKCS12").getString());
        }

        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        externalContext.responseReset(); // Some JSF component library or some Filter might have set some headers in the buffer beforehand. We want to get rid of them, else it may collide.
        externalContext.setResponseContentLength(data.length);
        externalContext.setResponseContentType("application/pkcs12");
        externalContext.setResponseHeader("Content-Disposition", "attachment; filename=\"" + selectedUsuario.getUid() + "-cert.pfx\"");

        BufferedInputStream input = null;
        BufferedOutputStream output = null;

        try {
            input = new BufferedInputStream(new ByteArrayInputStream(data));
            output = new BufferedOutputStream(externalContext.getResponseOutputStream());

            byte[] buffer = new byte[10240];
            for (int length; (length = input.read(buffer)) > 0;) {
                output.write(buffer, 0, length);
            }
        } finally {
            if (output != null) {
                output.flush();
                output.close();
            }
            if (input != null) {
                input.close();
            }
        }

        facesContext.responseComplete();
    }

    public void descargarCACert() throws FileNotFoundException, IOException {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        externalContext.responseReset(); // Some JSF component library or some Filter might have set some headers in the buffer beforehand. We want to get rid of them, else it may collide.
        externalContext.setResponseContentLength((int) caCert.length());
        externalContext.setResponseContentType("application/pem-certificate-chain");
        externalContext.setResponseHeader("Content-Disposition", "attachment; filename=\"CACert.pem\"");

        BufferedInputStream input = null;
        BufferedOutputStream output = null;

        try {
            input = new BufferedInputStream(new FileInputStream(caCert));
            output = new BufferedOutputStream(externalContext.getResponseOutputStream());

            byte[] buffer = new byte[10240];
            for (int length; (length = input.read(buffer)) > 0;) {
                output.write(buffer, 0, length);
            }
        } finally {
            if (output != null) {
                output.flush();
                output.close();
            }
            if (input != null) {
                input.close();
            }
        }

        facesContext.responseComplete();
    }

    /**
     * Método para cerrar las conexiones y eliminar la cookie
     */
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

    /**
     * Método para encriptar la contraseña
     *
     * @param txt Texto que se va a encriptar
     * @param hashType Qué tipo de hash "SHA1" o "MD5"
     * @return Regresa el hash del texto encriptado
     */
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

    /*
    * Método que agrega las notificaciones tipo "growl" de Primefaces
    * @param summary pequeño resumen en texto de la notificación a mostrar
    * @param isError boolean: true si el mensaje es de error, false si el mensaje es solo una notificación  
     */
    public void addMessage(String summary, boolean isError) {
        FacesMessage message = new FacesMessage(isError ? FacesMessage.SEVERITY_ERROR : FacesMessage.SEVERITY_INFO, summary, null);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

}
