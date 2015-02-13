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
public class LUAStats implements Serializable {

    public final String[] names;
    public final Long[] counts;

    private LUAStats(String[] names, Long[] counts) {
        this.names = names;
        this.counts = counts;
    }

    static public LUAStats create(String[] names, Long[] counts) {
        return new LUAStats(names, counts);
    }
}
