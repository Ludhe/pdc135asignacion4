/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pdc.login.entity;

import java.io.Serializable;

/**
 *
 * @author dmmaga
 */
public class Usuarioldap implements Serializable{


    
    private String uid;
    private String sn;
    private String cn;
    private String pass;

    public Usuarioldap(String uid, String sn, String cn, String pass) {
        this.uid = uid;
        this.sn = sn;
        this.cn = cn;
        this.pass = pass;
    }

   
    public Usuarioldap() {
    }
    
    public void setUid(String uid) {
       this.uid = uid;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }   

    public String getPass() {
        return pass;
    }
    
    public String getUid() {
        return uid;
    }

    public String getSn() {
        return sn;
    }

    public String getCn() {
        return cn;
    }
    
    public boolean isValid(){
        return !this.cn.isEmpty() && !this.uid.isEmpty() && !this.sn.isEmpty() && !this.pass.isEmpty();
    }
    
}
