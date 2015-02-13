/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.lua.impl.lib;

import akka.actor.PoisonPill;
import java.util.concurrent.TimeUnit;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import scala.concurrent.duration.Duration;
import test.lua.LUAWorker;
import test.lua.impl.WrappedGlobals;
import test.lua.msg.LUACallback;

/**
 *
 * @author Gwen
 */
public class Core extends TwoArgFunction {

    private final WrappedGlobals _wrapper;

    public Core(WrappedGlobals wrapper) {
        _wrapper = wrapper;
    }

    /**
     * Method to clone varargs in order to work around a bug where the first arg gets changed (probably some shared reference inside of luaj) This caused issues with the callbacks
     *
     * @param args
     * @return
     */
    public static Varargs cloneVarargs(Varargs args) {
        LuaValue[] values = new LuaValue[args.narg()];
        for (int i = 0; i < args.narg(); i++) {
            values[i] = args.arg(i + 1);
        }

        return LuaValue.varargsOf(values);
    }

    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        env.set("labp_pause", new labp_pause());
        env.set("labp_die", new labp_die());
        return env;
    }

    private final class labp_pause extends VarArgFunction {

        @Override
        public Varargs invoke(Varargs args) {
            args = cloneVarargs(args);

            LuaValue[] values = new LuaValue[args.narg()];
            for (int i = 0; i < args.narg(); i++) {
                values[i] = args.arg(i + 1);
            }

            args = LuaValue.varargsOf(values);

            if (!args.isnumber(1)) {
                throw new LuaError("first arg must be a number (pause duration in ms) but is " + args.arg(1).typename());
            }

            if (!args.isfunction(2)) {
                throw new LuaError("second arg must be a function (callback) but is " + args.arg(2).typename());
            }

            LuaNumber delay = (LuaNumber) args.arg(1);

            Long id = _wrapper.getAssociatedActor().register(args.subargs(2));

            _wrapper.getSystem().scheduler().scheduleOnce(
                    Duration.create(delay.toint(), TimeUnit.MILLISECONDS),
                    _wrapper.getAssociatedActor().self(),
                    LUACallback.create(id),
                    _wrapper.getSystem().dispatcher(),
                    null);

            return LuaValue.NIL;
        }
    }

    private final class labp_die extends ZeroArgFunction {

        @Override
        public LuaValue call() {
            LUAWorker actor = _wrapper.getAssociatedActor();
            actor.self().tell(PoisonPill.getInstance(), actor.self());
            return LuaValue.NIL;
        }
    }
}
