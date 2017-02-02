/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package quorum.view;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 *
 * @author eduardo
 */
public class View implements Serializable {

    //+1,-1, etc...
    private int[] updates;
    // members
    private transient int[] membership = null;
    private transient byte[] hash = null;

    public Map<Integer, InetSocketAddress> addresses = null;

    public View(int[] updates, Map<Integer, InetSocketAddress> addresses) {
        this.updates = updates;
        this.addresses = addresses;
        install();
    }

    public void setAddresses(Map<Integer, InetSocketAddress> addresses){
        this.addresses = addresses;
    }
    
    public void install() {
        if (this.membership == null) {
            LinkedList<Integer> l = new LinkedList<Integer>();
            for (int i = 0; i < this.updates.length; i++) {
                int atual = this.updates[i];
                if (atual >= 0) {
                    boolean leaved = false;
                    for (int j = 0; j < this.updates.length; j++) {
                        if (i != j && this.updates[j] < 0 && atual == Math.abs(this.updates[j])) {
                            leaved = true;
                        }
                    }
                    if (!leaved) {
                        l.add(atual);
                    }
                }

            }
            this.membership = new int[l.size()];
            for (int i = 0; i < l.size(); i++) {
                this.membership[i] = l.get(i).intValue();
            }
            this.hash = computeHash("SHA-1");
        }
    }

    public int getN() {
        install();
        return this.membership.length;
    }

    public int getF() {
        
        return (int) Math.floor(((getN() - 1) / 2));
    }

    public int getQuorum() {
        return (int) Math.ceil((double) (getN() + 1) / 2);
    }

    public int[] getMembership() {
        install();
        return membership;
    }

    public int[] getUpdates() {
        return updates;
    }

    public InetSocketAddress getAddress(int id) {
        return addresses.get(id);
    }

    public byte[] getHash() {
        install();
        return this.hash;
    }

    private byte[] computeHash(String algoritmo) {
        try {
            MessageDigest md = MessageDigest.getInstance(algoritmo);
            md.update(this.toString().getBytes());
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String toString() {

        StringBuffer b = new StringBuffer("[");
        for (int i = 0; i < this.updates.length - 1; i++) {
            b.append(this.updates[i] + ",");
        }
        b.append(this.updates[this.updates.length - 1] + "]");
        return b.toString();
    }

    //sou comparável com v
    public boolean isComparable(View v) {

        if (isSubSet(v) || v.isSubSet(this)) {
            return true;
        }

        return false;
    }

    public boolean containsUpdate(int update) {
        for (int i = 0; i < this.updates.length; i++) {
            if (this.updates[i] == update) {
                return true;
            }
        }
        return false;
    }

    public boolean isMember(int id) {
        install();
        for (int i = 0; i < membership.length; i++) {
            if (this.membership[i] == id) {
                return true;
            }
        }
        return false;
    }

    //sou um subset de v
    public boolean isSubSet(View v) {
        for (int i = 0; i < this.updates.length; i++) {
            /*boolean contains = false;
             for (int j = 0; j < v.getUpdates().length && !contains; j++) {
             if (this.updates[i] == v.getUpdates()[j]) {
             contains = true;
             }
             }
             if (!contains) {
             return false;
             }*/
            if (!v.containsUpdate(this.updates[i])) {
                return false;
            }

        }
        return true;
    }

    //sou igual a v
    public boolean equalsView(View v) {
        if (this.updates.length == v.getUpdates().length) {

            if (isSubSet(v) && v.isSubSet(this)) {
                // System.out.println("Vai retornar true");
                return true;
            }

        }
        // System.out.println("Vai retornar false");
        return false;

    }

    //sou igual a v
    public boolean equalsByHash(byte[] v) {

        if (this.hash.length != v.length) {
            return false;
        }
        for (int i = 0; i < this.hash.length; i++) {
            if (this.hash[i] != v[i]) {
                return false;
            }
        }

        return true;

    }

    @Override
    public boolean equals(Object obj) {
        //System.out.println("Chamou o equals");
        if (obj instanceof View) {
            //System.out.println("equals equalsView");
            return equalsView((View) obj);
        }
        //System.out.println("equals false");
        return false;
    }

    //sou mais atualizada do que v
    public boolean isMostUpToDateThan(View v) {

        if (v.isSubSet(this)) {
            return true;
        }

        return false;
    }

}
