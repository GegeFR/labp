/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.lua;

import akka.actor.ActorRef;
import test.lua.msg.LUAStart;
import akka.actor.UntypedActor;
import test.lua.msg.LUACallback;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.Varargs;
import test.lua.impl.WrappedGlobals;
import test.lua.impl.WrappedGlobalsPool;

/**
 *
 * @author Gwen
 */
public class LUAWorker extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this.getClass());
    private final Boolean _shared;
    private final String _path;
    private final WrappedGlobals _wrappedGlobals;
    private final HashMap<Long, Varargs> _pendingCallbacks = new HashMap();
    private final AtomicLong _pendingCallbacksCounter = new AtomicLong();
    
    public LUAWorker(Boolean shared, String path) throws InterruptedException {
        super();
        if (null == shared) {
            _shared = false;
        }
        else {
            _shared = shared;
        }

        _path = path;

        if (_shared) {
            _wrappedGlobals = WrappedGlobalsPool.instance().get(_path);
        }
        else {
            _wrappedGlobals = new WrappedGlobals(getContext().system(), _path);
        }

    }

    public Long register(Varargs varargs) {
        _pendingCallbacks.put(_pendingCallbacksCounter.get(), varargs);
        return _pendingCallbacksCounter.getAndIncrement();
    }

    @Override
    public void preStart() throws Exception {
        log.info(self() + " :: preStart()");
    }

    @Override
    public void postStop() throws Exception {
        log.info(self() + " :: postStop()");
    }

    @Override
    public void onReceive(Object m) throws Exception {
        log.info(self() + " :: received class " + m.getClass());
        if (m instanceof LUAStart) {
            onReceive((LUAStart) m);
        }
        else if (m instanceof LUACallback) {
            onReceive((LUACallback) m);
        }
        else {
            log.warning("received unsupported message");
        }
    }

    public void onReceive(LUAStart m) {
        try {
            synchronized (_wrappedGlobals) {
                _wrappedGlobals.setAssociatedActor(this);
                _wrappedGlobals.getChunk().call();
            }
        }
        catch (Exception e) {
            log.error(e, "exception happened handling LUAStart");
        }
    }

    public void onReceive(LUACallback m) {
        try {
            log.info(self() + " :: callback id is " + m.id);

            Varargs args;

            if (m.remove) {
                args = _pendingCallbacks.remove(m.id);
            }
            else {
                args = _pendingCallbacks.get(m.id);
            }

            LuaFunction callback = (LuaFunction) args.arg(1);
            synchronized (_wrappedGlobals) {
                _wrappedGlobals.setAssociatedActor(this);
                callback.invoke(args.subargs(2));
            }
        }
        catch (Exception e) {
            log.error(e, "exception happened handling LUACallback");
        }
    }
}
