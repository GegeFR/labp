/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.lua;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.contrib.pattern.DistributedPubSubExtension;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import test.lua.impl.WrappedGlobalsPool;
import test.lua.msg.LUAStart;

/**
 *
 * @author Gwen
 */
public class LUAService extends UntypedActor {

    private final static char ID_SEPARATOR = '$';
    private final static long PUBLISH_MIN_PERIOD = 1000;

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this.getClass());

    private ActorRef _distPubSubMediator;

    private final AtomicLong id = new AtomicLong(0);

    private final HashMap<String, Long> countPerName = new HashMap<>();

    private long _lastPublish = 0;

    private synchronized void publishStatsIfNecessary() {
        long now = System.currentTimeMillis();
        if (now - _lastPublish > 1000) {
            _lastPublish = now;

            //extract names[] and counts[]
            int i = 0;
            String[] names = new String[countPerName.size()];
            Long[] counts = new Long[countPerName.size()];
            for (Entry<String, Long> entry : countPerName.entrySet()) {
                names[i] = entry.getKey();
                counts[i] = entry.getValue();
            }
            //TODO : send to wui
        }
    }

    private String extractName(String name) {
        int index = name.lastIndexOf(ID_SEPARATOR);

        if (-1 != index) {
            throw new RuntimeException("bad name, should not happen");
        }

        return name.substring(0, index);
    }

    private synchronized void increment(String nameWithId) {
        String name = extractName(nameWithId);

        Long count = countPerName.get(name);
        if (null != count) {
            countPerName.put(name, count + 1);
        }
        else {
            countPerName.put(name, new Long(1));
        }
    }

    private synchronized void decrement(String nameWithId) {
        String name = extractName(nameWithId);

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
    public void onReceive(Object m) throws Exception {
        log.info(self() + " :: received class " + m.getClass());

        if (m instanceof LUAStart) {
            onReceive((LUAStart) m);
        }
        else if (m instanceof Terminated) {
            onReceive((Terminated) m);
        }
        else {
            log.warning("received unsupported message");
        }
    }

    public void onReceive(LUAStart m) {
        try {
            String name = m.name + ID_SEPARATOR + Long.toString(id.incrementAndGet());
            ActorRef worker = getContext().system().actorOf(Props.create(LUAWorker.class, m.shared, m.path), name);
            context().watch(worker);
            worker.tell(m, worker);
            increment(name);

            //_distPubSubMediator.tell(new DistributedPubSubMediator.Publish(Constants.TOPIC_WUI, null), self());
        }
        catch (Exception e) {
            log.error(e, "exception happened handling LUAStart");
        }
    }

    public void onReceive(Terminated m) {
        String name = m.actor().path().toString();
        name = name.substring(name.lastIndexOf("/") + 1);
        decrement(name);
    }

}
