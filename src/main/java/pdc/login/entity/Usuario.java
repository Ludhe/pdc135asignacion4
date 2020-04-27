package pdc.login.entity;

import java.io.Serializable;
import org.apache.directory.ldap.client.api.LdapConnection;

/**
 *
 * @author dmmaga
 */
public class Usuario implements Serializable{
    
    private final String uid;
    private final String pass;
    private LdapConnection connection;

    public Usuario(String uid, String pass) {
        this.uid = uid;
        this.pass = pass;
    }

    public String getUid() {
        return uid;
    }

    public String getPass() {
        return pass;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Usuario usuario = (Usuario) o;
        if (uid != null ? !uid.equals(usuario.uid) : usuario.uid != null) {
            return false;
        }
        if (pass != null ? !pass.equals(usuario.pass) : usuario.pass != null) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int result = uid != null ? uid.hashCode() : 0;
        result = 31 * result + (uid != null ? pass.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "Usuario: " + uid + ", contraseña" + pass + ")";
    }
    
}
