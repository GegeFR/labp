/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.lua.msg;

import java.io.Serializable;

/**
 *
 * @author Gwen
 */
public class LUACallback implements Serializable {

    public final Long id;
    public final Boolean remove;

    private LUACallback(Long id, Boolean remove) {
        this.id = id;
        this.remove = remove;
    }

    public static LUACallback create(Long id, Boolean remove) {
        return new LUACallback(id, remove);
    }
    
    public static LUACallback create(Long id) {
        return new LUACallback(id, true);
    }
}
