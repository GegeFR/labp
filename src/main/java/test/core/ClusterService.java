/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.core;

import akka.actor.ActorSelection;
import akka.actor.Address;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.ClusterMetricsChanged;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.cluster.Member;
import akka.cluster.NodeMetrics;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import test.Constants;

/**
 *
 * @author Gwen
 */
public class ClusterService extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final Cluster cluster = Cluster.get(getContext().system());

    @Override
    public void preStart() {
        cluster.subscribe(self(), ClusterEvent.initialStateAsEvents(), MemberEvent.class, UnreachableMember.class);
    }

    @Override
    public void postStop() {
        cluster.unsubscribe(self());
    }

    @Override
    public void onReceive(Object m) throws Exception {
        log.info(self() + " :: received class " + m.getClass());

        if (m instanceof MemberUp) {
            onReceive((MemberUp) m);
        }
        else if (m instanceof ClusterMetricsChanged) {
            onReceive((ClusterMetricsChanged) m);
        }
        else {
            log.debug("received unsupported message " + m.getClass());
        }
    }

    public void onReceive(MemberUp m) {
        try {
            Member member = m.member();
            Address address = member.address();
            ClusterServicesRegistry.getInstance().registerMember(address, 1.0);

            if (member.hasRole(Constants.ROLE_LFS)) {
                ActorSelection selection = context().actorSelection(member.address() + "/user/LFSService");
                ClusterServicesRegistry.getInstance().registerService(Constants.ROLE_LFS, address, selection);
                log.debug("Registered LFSService {} from remote member {}", selection, member);
            }
            if (member.hasRole(Constants.ROLE_LUA)) {
                ActorSelection selection = context().actorSelection(member.address() + "/user/LUAService");
                ClusterServicesRegistry.getInstance().registerService(Constants.ROLE_LUA, address, selection);
                log.debug("Register LUAService {} from remote member {}", selection, member);
            }
            if (member.hasRole(Constants.ROLE_WUI)) {
                ActorSelection selection = context().actorSelection(member.address() + "/user/WUIService");
                ClusterServicesRegistry.getInstance().registerService(Constants.ROLE_WUI, address, selection);
                log.debug("Register WUIService {} fromremote member {}", selection, member);
            }
        }
        catch (Exception e) {
            log.error(e, "exception happened handling LUAStart");
        }

    }

    public void onReceive(ClusterMetricsChanged m) {
        try {
            for (NodeMetrics nodeMetrics : m.getNodeMetrics()) {
                Address address = nodeMetrics.address();

                double memoryUsed = nodeMetrics.metric("heap-memory-used").get().value().doubleValue();
                double memoryMax = nodeMetrics.metric("heap-memory-max").get().value().doubleValue();

                // 0 if all used
                // 1 if all free
                double weight = 1 - (memoryUsed / memoryMax);

                //TODO : configurable thresholds
                // set to 0 (don't use node) if less than 20% available memory
                if (weight < 0.2) {
                    log.warning("node has availability lower than 0.2: set to 0");
                    weight = 0;
                }

                ClusterServicesRegistry.getInstance().updateMember(address, weight);
            }

            ClusterServicesRegistry.getInstance().recomputeAll();
        }
        catch (Exception e) {
            log.error(e, "exception happened handling LUAStart");
        }
    }
}
