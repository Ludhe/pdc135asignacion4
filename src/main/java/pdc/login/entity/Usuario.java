package pdc.login.entity;

import java.io.Serializable;

/**
 *
 * @author dmmaga
 */
public class Usuario implements Serializable{
    
    private final String user;
    private final String pass;

    public Usuario(String user, String pass) {
        this.user = user;
        this.pass = pass;
    }

    public String getUser() {
        return user;
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
        if (user != null ? !user.equals(usuario.user) : usuario.user != null) {
            return false;
        }
        if (pass != null ? !pass.equals(usuario.pass) : usuario.pass != null) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int result = user != null ? user.hashCode() : 0;
        result = 31 * result + (user != null ? pass.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "Usuario: " + user + ", contraseña" + pass + ")";
    }
    
}
