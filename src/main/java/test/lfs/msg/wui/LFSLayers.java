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

    public final String[] names;
    public final boolean[] statuses;

    private LFSLayers(String[] names, boolean[] statuses) {
        this.names = names;
        this.statuses = statuses;
    }

    public static LFSLayers create(String[] names, boolean[] statuses) {
        return new LFSLayers(names, statuses);
    }
}
