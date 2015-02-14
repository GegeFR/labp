/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.utils;

/**
 *
 * @author Gege
 */
public class WithMeta<T ,M> {

    private final T object;
    private M meta;

    public WithMeta(T left, M right) {
        this.object = left;
        this.meta = right;
    }

    public T getObject() {
        return object;
    }

    public M getMeta() {
        return meta;
    }

    public void setMeta(M val) {
        meta = val;
    }
    
    @Override
    public int hashCode() {
        return object.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof WithMeta)) {
            return false;
        }
        WithMeta pairo = (WithMeta) o;
        return this.object.equals(pairo.getObject()) && this.meta.equals(pairo.getMeta());
    }

}
