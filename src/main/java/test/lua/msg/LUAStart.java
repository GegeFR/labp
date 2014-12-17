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
public class LUAStart implements Serializable {

    public final String path;
    public final String name;
    public final Boolean shared;

    private LUAStart(String path, String name, Boolean shared) {
        this.path = path;
        this.shared = shared;
        
        if(null != name && name.contains("#")){
            throw new RuntimeException("invalid name for LUAWorker, # character is reserved");
        }
        
        this.name = name;
    }

    static public LUAStart create(String file, Boolean shared) {
        return new LUAStart(file, null, shared);
    }
    
    static public LUAStart create(String file, String name, Boolean shared) {
        return new LUAStart(file, name, shared);
    }
}
