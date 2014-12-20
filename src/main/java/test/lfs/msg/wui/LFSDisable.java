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
public class LFSDisable implements Serializable {

    public final String layer;

    private LFSDisable(String layer) {
        this.layer = layer;
    }

    public static LFSDisable create(String layer) {
        return new LFSDisable(layer);
    }
}
