/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.lua.impl;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.luaj.vm2.lib.ResourceFinder;
import test.Constants;
import test.base.akka.AkkaUtils;
import test.core.ClusterServicesRegistry;
import test.lfs.msg.core.LFSGet;

/**
 *
 * @author Gwen
 */
public class LUAResourceFinder implements ResourceFinder {
    private final LoggingAdapter log; 

    public LUAResourceFinder(ActorSystem system) {
        log = Logging.getLogger(system, this.getClass());
        log.debug("LUAResourceFinder initialized");
        
    }

    @Override
    public InputStream findResource(String filename) {
        log.debug("findResource({})", filename);

        try {
            ActorSelection lfsService = ClusterServicesRegistry.getInstance().getService(Constants.ROLE_LFS);
            LFSGet.Response response = (LFSGet.Response) AkkaUtils.ask(lfsService, LFSGet.create(filename), 5000);

            if (response.throwable != null) {
                throw response.throwable;
            }

            return new ByteArrayInputStream(response.bytes);
        }
        catch (Throwable t) {
            return null;
        }
    }
}
