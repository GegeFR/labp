package test;

import akka.actor.ActorRef;
import test.core.ClusterService;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent.ClusterDomainEvent;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.kernel.Bootable;
import test.base.akka.AkkaUtils;
import test.core.ClusterServicesRegistry;
import test.lfs.LFSService;
import test.lfs.impl.LayeredFileSystem;
import test.lua.LUAService;
import test.wui.WUIActor;

public class Bootstrap implements Bootable {

    private final ActorSystem system = ActorSystem.create("ClusterSystem");
    private final LoggingAdapter log = Logging.getLogger(system, this.getClass());

    @Override
    public void startup() {
        try {
            ClusterServicesRegistry.initInstance(system);

            // create local *service* actors according to local node role
            if (AkkaUtils.myRole(system, Constants.ROLE_LUA)) {
                ActorRef ref = system.actorOf(Props.create(LUAService.class), "LUAService");
                log.info("created actor " + ref);
            }

            if (AkkaUtils.myRole(system, Constants.ROLE_LFS)) {
                LayeredFileSystem.initInstance(system);
                ActorRef ref = system.actorOf(Props.create(LFSService.class), "LFSService");
                log.info("created actor " + ref);
            }

            if (AkkaUtils.myRole(system, Constants.ROLE_WUI)) {
                ActorRef ref = system.actorOf(Props.create(WUIActor.class), "WUIService");
                log.info("created actor " + ref);
            }

            // create an actor that handles cluster domain events then add subscription of cluster events
            ActorRef clusterListener = system.actorOf(Props.create(ClusterService.class), "ClusterService");
            Cluster.get(system).subscribe(clusterListener, ClusterDomainEvent.class);
        }
        catch (Exception e) {
            log.error(e, "exception");
        }
    }

    @Override
    public void shutdown() {
        system.shutdown();
    }
}
