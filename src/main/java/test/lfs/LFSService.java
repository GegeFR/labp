/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.lfs;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
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
public class LFSService extends AbstractActor {

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
    public PartialFunction receive() {

        return ReceiveBuilder.match(LFSGet.class, m -> {
            log.info("received " + m.getClass());
            try {
                byte[] bytes = _filesystem.getLayer(m.layer).get(m.path);
                sender().tell(m.createResponse(bytes, null), self());
            }
            catch (Exception e) {
                sender().tell(m.createResponse(null, e), self());
            }
        }).match(LFSSet.class, m -> {
            log.info("received " + m.getClass());
//            try {
//                _filesystem.getLayer(m.layer).set(m.path, m.bytes);
//                sender().tell(m.createResponse(null), self());
//            }
//            catch (Exception e) {
//                sender().tell(m.createResponse(e), self());
//            }
        }).match(LFSList.class, m -> {
            log.info("received " + m.getClass());
            try {
                String[] paths = _filesystem.getLayer(m.layer).list(m.path, m.recurse);
                sender().tell(m.createResponse(paths, null), self());
            }
            catch (Exception e) {
                sender().tell(m.createResponse(null, e), self());
            }
        }).match(LFSAskToPublish.class, m -> {
            log.info("received " + m.getClass());

            doPublish();
        }).match(LFSUp.class, m -> {
            log.info("received " + m.getClass());
            _filesystem.up(m.layer);
            doPublish();
        }).match(LFSDown.class, m -> {
            log.info("received " + m.getClass());
            _filesystem.down(m.layer);
            doPublish();
        }).match(LFSEnable.class, m -> {
            log.info("received " + m.getClass());
            _filesystem.enable(m.layer);
            doPublish();
        }).match(LFSDisable.class, m -> {
            log.info("received " + m.getClass());
            _filesystem.disable(m.layer);
            doPublish();
        }).matchAny(m -> {
            log.info("received unsupported message " + m.getClass());
        }).build();
    }

    private void doPublish() {
        String[] active = _filesystem.getActiveLayers();
        String[] inactive = _filesystem.getInactiveLayers();
        _distPubSubMediator.tell(new DistributedPubSubMediator.Publish(Constants.TOPIC_WUI, LFSLayers.create(active, inactive)), self());
    }
}
