/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.lua;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.contrib.pattern.DistributedPubSubExtension;
import akka.contrib.pattern.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;
import scala.PartialFunction;
import test.Constants;
import test.lua.impl.WrappedGlobalsPool;
import test.lua.msg.LUAStart;

/**
 *
 * @author Gwen
 */
public class LUAService extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this.getClass());

    private ActorRef _distPubSubMediator;

    private final AtomicLong id = new AtomicLong(0);

    private final HashMap<String, Long> countPerName = new HashMap<>();

    private String nameForCount(String name) {
        if (null == name) {
            name = "default";
        }

        int index = name.lastIndexOf("#");

        if (-1 != index) {
            return name.substring(0, index);
        }
        else {
            return name;
        }
    }

    private synchronized void increment(String name) {
        name = nameForCount(name);

        Long count = countPerName.get(name);
        if (null != count) {
            countPerName.put(name, count + 1);
        }
        else {
            countPerName.put(name, new Long(1));
        }
    }

    private synchronized void decrement(String name) {
        name = nameForCount(name);

        Long count = countPerName.get(name);
        if (null != count) {
            if (count == 1) {
                countPerName.remove(name);
            }
            else {
                countPerName.put(name, count - 1);
            }
        }
        else {
            countPerName.put(name, new Long(-1));
        }
    }

    @Override
    public void preStart() {
        _distPubSubMediator = DistributedPubSubExtension.get(getContext().system()).mediator();

        if (null == WrappedGlobalsPool.instance()) {
            WrappedGlobalsPool.init(getContext().system());
        }
    }

    @Override
    public PartialFunction receive() {
        return ReceiveBuilder.match(LUAStart.class, m -> {
            log.warning("received " + m.getClass());
            try {
                String name = m.name + "$" + Long.toString(id.incrementAndGet());
                ActorRef worker = getContext().system().actorOf(Props.create(LUAWorker.class, m.shared, m.path), name);
                context().watch(worker);
                worker.tell(m, worker);
                increment(name);

                _distPubSubMediator.tell(new DistributedPubSubMediator.Publish(Constants.TOPIC_WUI, null), self());
            }
            catch (Exception e) {
                log.error(e, "exception happened handling LUAStart");
            }
        }).match(Terminated.class, m -> {
            log.warning("received " + m.getClass() + " for actor " + m.actor().path());
            String name = m.actor().path().toString();
            name = name.substring(name.lastIndexOf("/") + 1);
            decrement(name);
        }).matchAny(m -> {
            log.warning("received unsupported message");
        }).build();
    }
}
