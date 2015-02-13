/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.wui;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.contrib.pattern.DistributedPubSubExtension;
import akka.contrib.pattern.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import test.Constants;
import test.lfs.msg.wui.LFSLayers;

/**
 *
 * @author Gwen
 */
public class WUIActor extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this.getClass());
    private WUIServlet _servlet;

    @Override
    public void preStart() {
        ActorRef mediator = DistributedPubSubExtension.get(getContext().system()).mediator();
        mediator.tell(new DistributedPubSubMediator.Subscribe(Constants.TOPIC_WUI, self()), self());

        _servlet = new WUIServlet(getContext().system());
        //create and instantiate the servlet
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setWelcomeFiles(new String[]{"index.html", "index.htm"});
        resourceHandler.setBaseResource(Resource.newClassPathResource("/www/"));

        ServletHandler servletHandler = new ServletHandler();

        servletHandler.addServletWithMapping(new ServletHolder(_servlet), "/api/*");

        HandlerList handlerList = new HandlerList();
        handlerList.addHandler(resourceHandler);
        handlerList.addHandler(servletHandler);

        // start jetty container
        Server server = new Server(8080);
        server.setHandler(handlerList);
        try {
            server.start();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onReceive(Object m) throws Exception {
        log.info(self() + " :: received class " + m.getClass());
        if (m instanceof LFSLayers) {
            onReceive((LFSLayers) m);
        }
        else {
            log.warning("received unsupported message " + m.getClass());
        }
    }

    public void onReceive(LFSLayers m) {
        _servlet.wasPublished(m);
    }
}
