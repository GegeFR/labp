/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.lua;

import test.lua.msg.LUAStart;
import akka.actor.AbstractActor;
import test.lua.msg.LUACallback;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.Varargs;
import scala.PartialFunction;
import test.lua.impl.WrappedGlobals;
import test.lua.impl.WrappedGlobalsPool;

/**
 *
 * @author Gwen
 */
public class LUAWorker extends AbstractActor {

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
    public PartialFunction receive() {
        return ReceiveBuilder.match(LUAStart.class, m -> {
            log.info(self() + " :: received class " + m.getClass());
            try {

                synchronized (_wrappedGlobals) {
                    _wrappedGlobals.setAssociatedActor(this);
                    _wrappedGlobals.getChunk().call();
                }
            }
            catch (Exception e) {
                log.error(e, "exception happened handling LUAStart");
            }
        }).match(LUACallback.class, m -> {
            log.info(self() + " :: received class " + m.getClass());
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
        }).matchAny(m -> {
            log.warning("received unsupported message");
        }).build();
    }
}
