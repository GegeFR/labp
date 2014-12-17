/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.lfs.msg.core;

import java.io.Serializable;

/**
 *
 * @author Gwen
 */
public class LFSGet implements Serializable {

    public final String callback;
    public final String context;
    public final String path;
    public final String layer;

    private LFSGet(String callback, String context, String path, String layer) {
        this.callback = callback;
        this.context = context;
        this.path = path;
        this.layer = layer;
    }
    
    public static LFSGet create(String path) {
        return new LFSGet(null, null, path, null);
    }
    
    public static LFSGet create(String callback, String context, String path, String layer) {
        return new LFSGet(callback, context, path, layer);
    }
    
    public Response createResponse(byte[] bytes, Throwable throwable) {
        return new Response(this, bytes, throwable);
    }

    public static class Response implements Serializable {

        public final byte[] bytes;
        public final Throwable throwable;
        public final LFSGet request;

        private Response(LFSGet request, byte[] bytes, Throwable throwable) {
            this.request = request;
            this.bytes = bytes;
            this.throwable = throwable;
        }
    }
}
