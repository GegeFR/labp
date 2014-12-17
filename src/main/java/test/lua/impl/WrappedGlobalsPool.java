/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.lua.impl;

import akka.actor.ActorSystem;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 *
 * @author Gwen
 */
public class WrappedGlobalsPool {

    private static WrappedGlobalsPool INSTANCE;

    public static void init(ActorSystem system) {
        INSTANCE = new WrappedGlobalsPool(system);
    }

    public static WrappedGlobalsPool instance() {
        return INSTANCE;
    }

    private final Map<String, LinkedList<WrappedGlobals>> _wrappedGlobalsByPath;
    private final ActorSystem _system;

    private WrappedGlobalsPool(ActorSystem system) {
        _system = system;
        _wrappedGlobalsByPath = new HashMap<>();
    }

    public WrappedGlobals get(String path) throws InterruptedException {
        WrappedGlobals ret;
        synchronized (_wrappedGlobalsByPath) {
            LinkedList<WrappedGlobals> queue;
            queue = _wrappedGlobalsByPath.get(path);
            if (null == queue) {
                queue = new LinkedList();
                _wrappedGlobalsByPath.put(path, queue);

                for (int i = 0; i < 10; i++) {
                    queue.add(new WrappedGlobals(_system, path));
                }
            }
            ret = queue.removeFirst();
            queue.addLast(ret);
        }

        return ret;
    }
}
