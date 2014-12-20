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
public class Ping implements Serializable {

    private Ping() {
    }

    public static Ping create() {
        return new Ping();
    }

    public Response createResponse() {
        return new Response(this);
    }

    public static class Response implements Serializable {

        public final Ping request;

        private Response(Ping request) {
            this.request = request;
        }
    }
}
