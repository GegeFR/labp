/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.lua.impl;

import akka.actor.ActorSystem;
import java.util.concurrent.atomic.AtomicLong;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;
import test.lua.LUAWorker;
import test.lua.impl.lib.LabpLib;

/**
 *
 * @author egwepas
 */
public class WrappedGlobals {

    private final ActorSystem _system;
    private final Globals _globals;
    private final LuaValue _mainChunk;
    private final String _path;
    private LUAWorker _associatedActor;

    public WrappedGlobals(ActorSystem system, String path) {
        _path = path;
        _system = system;
        _globals = JsePlatform.standardGlobals();
        _globals.load(new LabpLib(this));
        _globals.finder = new LUAResourceFinder(_system);
        _mainChunk = _globals.loadfile(path);
    }

    public String getPath() {
        return _path;
    }

    public Globals getGlobals() {
        return _globals;
    }

    public LuaValue getChunk() {
        return _mainChunk;
    }

    public ActorSystem getSystem() {
        return _system;
    }

    public void setAssociatedActor(LUAWorker actor) {
        _associatedActor = actor;
    }

    public LUAWorker getAssociatedActor() {
        return _associatedActor;
    }
}
