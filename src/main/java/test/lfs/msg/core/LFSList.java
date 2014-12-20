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
public class LFSList implements Serializable {

    public final String callback;
    public final String context;
    public final String path;
    public final String layer;
    public final Boolean recurse;

    private LFSList(String callback, String context, String path, String layer, Boolean recurse) {
        this.callback = callback;
        this.context = context;
        this.path = path;
        this.layer = layer;
        this.recurse = recurse;
    }

    public static LFSList create(String path, Boolean recurse) {
        return new LFSList(null, null, path, null, recurse);
    }

    public static LFSList create(String callback, String context, String path, String layer, Boolean recurse) {
        return new LFSList(callback, context, path, layer, recurse);
    }

    public Response createResponse(String[] paths, Throwable throwable) {
        return new Response(this, paths, throwable);
    }

    public static class Response implements Serializable {

        public final String[] paths;
        public final Throwable throwable;
        public final LFSList request;

        private Response(LFSList request, String[] paths, Throwable throwable) {
            this.request = request;
            this.paths = paths;
            this.throwable = throwable;
        }
    }
}
