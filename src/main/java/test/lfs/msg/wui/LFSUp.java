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
public class LFSUp implements Serializable {

    public final String layer;

    private LFSUp(String layer) {
        this.layer = layer;
    }

    public static LFSUp create(String layer) {
        return new LFSUp(layer);
    }
}
