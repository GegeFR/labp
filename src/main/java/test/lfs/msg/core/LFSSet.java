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
public class LFSSet implements Serializable {

    public final String callback;
    public final String context;
    public final String path;
    public final String layer;
    public final byte[] bytes;

    private LFSSet(String callback, String context, String path, String layer, byte[] bytes) {
        this.callback = callback;
        this.context = context;
        this.path = path;
        this.layer = layer;
        this.bytes = bytes;
    }

    public Response createResponse(Throwable throwable) {
        return new Response(this, throwable);
    }

    public static class Response implements Serializable {

        public final Throwable throwable;
        public final LFSSet request;

        private Response(LFSSet request, Throwable throwable) {
            this.request = request;
            this.throwable = throwable;
        }
    }
}
