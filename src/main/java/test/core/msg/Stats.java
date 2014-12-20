/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.core.msg;

import java.io.Serializable;

/**
 *
 * @author Gwen
 */
public class Stats implements Serializable {

    public final long freeMem;
    public final long usedMem;

    private Stats(long freeMem, long usedMem) {
        this.freeMem = freeMem;
        this.usedMem = usedMem;
    }

    public static Stats create(long freeMem, long usedMem) {
        return new Stats(freeMem, usedMem);
    }
}
