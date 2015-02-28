/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.lfs.msg.wui;

import java.io.Serializable;
import test.lfs.msg.core.LFSGet;

/**
 *
 * @author Gwen
 */
public class LFSExport implements Serializable {

    public final String layer;

    private LFSExport(String layer) {
        this.layer = layer;
    }

    public static LFSExport create(String layer) {
        return new LFSExport(layer);
    }

    public Response createResponse(byte[] bytes, Throwable throwable) {
        return new Response(this, bytes, throwable);
    }

    public static class Response implements Serializable {

        public final byte[] bytes;
        public final Throwable throwable;
        public final LFSExport request;

        private Response(LFSExport request, byte[] bytes, Throwable throwable) {
            this.request = request;
            this.bytes = bytes;
            this.throwable = throwable;
        }
    }
}
