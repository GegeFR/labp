/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.lfs.msg.wui;

import java.io.Serializable;

/**
 *
 * @author Gwen
 */
public class LFSDelete implements Serializable {

    public final String layer;

    private LFSDelete(String layer) {
        this.layer = layer;
    }

    public static LFSDelete create(String layer) {
        return new LFSDelete(layer);
    }
}
