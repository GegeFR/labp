/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package test.base.akka;

/**
 *
 * @author Gwen
 * @param <T>
 */
public class LongPollingEvent {
    private final long seq;
    private final Object data;
    
    public LongPollingEvent(long seq, Object data){
        this.seq = seq;
        this.data = data;
    }
    
    public long getSeq(){
        return seq;
    }
    
    public Object getData(){
        return data;
    }
}
