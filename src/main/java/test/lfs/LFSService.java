/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.lfs;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.contrib.pattern.DistributedPubSubExtension;
import akka.contrib.pattern.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import scala.PartialFunction;
import test.Constants;
import test.lfs.impl.LayeredFileSystem;
import test.lfs.msg.core.LFSGet;
import test.lfs.msg.core.LFSList;
import test.lfs.msg.core.LFSSet;
import test.lfs.msg.wui.LFSAskToPublish;
import test.lfs.msg.wui.LFSDisable;
import test.lfs.msg.wui.LFSDown;
import test.lfs.msg.wui.LFSEnable;
import test.lfs.msg.wui.LFSLayers;
import test.lfs.msg.wui.LFSUp;

/**
 *
 * @author Gwen
 */
public class LFSService extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this.getClass());

    private final LayeredFileSystem _filesystem;
    private ActorRef _distPubSubMediator;

    public LFSService() {
        super();
        _filesystem = LayeredFileSystem.getInstance();
    }

    @Override
    public void preStart() {
        _distPubSubMediator = DistributedPubSubExtension.get(getContext().system()).mediator();
    }

    @Override
    public void onReceive(Object m) throws Exception {
        log.info(self() + " :: received class " + m.getClass());

        if (m instanceof LFSGet) {
            onReceive((LFSGet) m);
        }
        else if (m instanceof LFSGet) {
            onReceive((LFSGet) m);
        }
        else if (m instanceof LFSSet) {
            onReceive((LFSSet) m);
        }
        else if (m instanceof LFSList) {
            onReceive((LFSList) m);
        }
        else if (m instanceof LFSAskToPublish) {
            onReceive((LFSAskToPublish) m);
        }
        else if (m instanceof LFSUp) {
            onReceive((LFSUp) m);
        }
        else if (m instanceof LFSDown) {
            onReceive((LFSDown) m);
        }
        else if (m instanceof LFSEnable) {
            onReceive((LFSEnable) m);
        }
        else if (m instanceof LFSDisable) {
            onReceive((LFSDisable) m);
        }
        else {
            log.warning("received unsupported message " + m.getClass());
        }
    }

    public void onReceive(LFSGet m) {
        try {
            byte[] bytes = _filesystem.getLayer(m.layer).get(m.path);
            sender().tell(m.createResponse(bytes, null), self());
        }
        catch (Exception e) {
            sender().tell(m.createResponse(null, e), self());
        }
    }

    public void onReceive(LFSSet m) {

    }

    public void onReceive(LFSList m) {
        try {
            String[] paths = _filesystem.getLayer(m.layer).list(m.path, m.recurse);
            sender().tell(m.createResponse(paths, null), self());
        }
        catch (Exception e) {
            sender().tell(m.createResponse(null, e), self());
        }
    }

    public void onReceive(LFSAskToPublish m) {
        doPublish();
    }

    public void onReceive(LFSUp m) {
        _filesystem.up(m.layer);
        doPublish();
    }

    public void onReceive(LFSDown m) {
        _filesystem.down(m.layer);
        doPublish();
    }

    public void onReceive(LFSEnable m) {
        _filesystem.enable(m.layer);
        doPublish();
    }

    public void onReceive(LFSDisable m) {
        _filesystem.disable(m.layer);
        doPublish();
    }

    private void doPublish() {
        String[] active = _filesystem.getActiveLayers();
        String[] inactive = _filesystem.getInactiveLayers();
        _distPubSubMediator.tell(new DistributedPubSubMediator.Publish(Constants.TOPIC_WUI, LFSLayers.create(active, inactive)), self());
    }

}
