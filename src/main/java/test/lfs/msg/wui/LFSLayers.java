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
public class LFSLayers implements Serializable {

    public final String[] active;
    public final String[] inactive;

    private LFSLayers(String[] active, String[] inactive) {
        this.active = active;
        this.inactive = inactive;
    }

    public static LFSLayers create(String[] active, String[] inactive) {
        return new LFSLayers(active, inactive);
    }
}
