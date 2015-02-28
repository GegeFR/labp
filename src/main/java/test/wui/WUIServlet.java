/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.wui;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.rmi.ServerException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import test.Constants;
import test.base.akka.AkkaUtils;
import test.base.akka.LongPollingEvent;
import test.core.ClusterServicesRegistry;
import test.lfs.msg.core.LFSGet;
import test.lfs.msg.core.LFSList;
import test.lfs.msg.wui.LFSExport;
import test.lua.msg.LUAStart;

/**
 *
 * @author Gwen
 */
public class WUIServlet extends HttpServlet {

    protected final LoggingAdapter log;
    protected final ActorSystem _system;
    protected final Gson _gson = new Gson();

    private final Map<Class, List<AsyncContext>> _waitingContextes;
    private final Map<Class, LongPollingEvent> _lastPublished;

    private Long _seq = System.currentTimeMillis();

    public WUIServlet(ActorSystem system) {
        _system = system;
        log = Logging.getLogger(system, this.getClass());

        _waitingContextes = new HashMap();
        _lastPublished = new HashMap();
    }

    public synchronized void wasPublished(Object msg) {
        LongPollingEvent longPollingEvent = new LongPollingEvent(_seq++, msg);

        Class clazz = msg.getClass();
        List<AsyncContext> waitingContextes = _waitingContextes.get(clazz);
        if (null != waitingContextes && !waitingContextes.isEmpty()) {
            while (!waitingContextes.isEmpty()) {
                try {
                    AsyncContext context = waitingContextes.remove(0);
                    sendResponse(context, longPollingEvent);
                }
                catch (Exception e) {
                    //TODO: generate exception but should not happen (probably a timeouted client connexion)
                }
            }
        }

        _lastPublished.put(msg.getClass(), longPollingEvent);
    }

    private void sendResponse(AsyncContext context, LongPollingEvent pair) {
        try {
            HttpServletResponse response = (HttpServletResponse) context.getResponse();
            log.info("serialized message of class " + pair.getData().getClass() + " and seq " + pair.getSeq());
            response.getWriter().print(_gson.toJson(pair));
            response.setContentType("application/json");
        }
        catch (IOException e) {
            log.error(e, "an exception occured sending response");
        }
        finally {
            context.complete();
        }
    }

    /**
     * Messages from system to client
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected synchronized void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String[] parts = request.getRequestURI().split("/");
            Class clazz = Class.forName(parts[parts.length - 2]);
            long seq = Long.parseLong(parts[parts.length - 1]);

            request.startAsync();

            // do we send a response now or wait for a new event
            LongPollingEvent lastEvent = _lastPublished.get(clazz);
            if (lastEvent != null && lastEvent.getSeq() > seq) {
                sendResponse(request.getAsyncContext(), lastEvent);
            }
            else {
                List<AsyncContext> list = _waitingContextes.get(clazz);
                if (null == list) {
                    list = new LinkedList();
                    _waitingContextes.put(clazz, list);
                }

                list.add(request.getAsyncContext());
            }
        }
        catch (ClassNotFoundException | NumberFormatException | IllegalStateException e) {
            throw new ServerException("forward as ServletException", e);
        }
    }

    @Override
    protected synchronized void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        int lastSlash = requestURI.lastIndexOf("/");
        String resource = requestURI.substring(lastSlash + 1);

        try {
            Object object = _gson.fromJson(request.getReader(), Class.forName(resource));

            log.info("deserialized message of class " + object.getClass());

            // special treatment on special cases
            if (object instanceof LFSList || object instanceof LFSGet) {
                try {
                    ActorSelection lfsService = ClusterServicesRegistry.getInstance().getService(Constants.ROLE_LFS);
                    Object lfsResponse = AkkaUtils.ask(lfsService, object, 5000);
                    response.getWriter().print(_gson.toJson(lfsResponse));
                    response.getWriter().close();
                }
                catch (Exception e) {
                    throw new ServletException("forward as ServletException", e);
                }
            }
            else if (object instanceof LFSExport) {
                try {
                    ActorSelection lfsService = ClusterServicesRegistry.getInstance().getService(Constants.ROLE_LFS);
                    LFSExport.Response lfsResponse = (LFSExport.Response) AkkaUtils.ask(lfsService, object, 10000);

                    String filename;
                    if (null != lfsResponse.request.layer) {
                        filename = lfsResponse.request.layer;
                    }
                    else {
                        filename = "all";
                    }

                    Date date = new Date();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd-hh_mm_ss");
                    filename += "-" + sdf.format(date) + ".zip";

                    response.setContentType("application/zip");
                    response.setContentLength(lfsResponse.bytes.length);
                    response.setHeader("Content-Disposition", "attachment;filename=\"" + filename + "\"");
                    response.getOutputStream().write(lfsResponse.bytes);
                    response.getOutputStream().close();
                }
                catch (Exception e) {
                    throw new ServletException("forward as ServletException", e);
                }
            }
            else if (object instanceof LUAStart) {
                ActorSelection luaService = ClusterServicesRegistry.getInstance().getService(Constants.ROLE_LUA);
                luaService.tell(object, null);
            }
            else {
                // by default send message to and don't wait an answer
                ActorSelection lfsService = ClusterServicesRegistry.getInstance().getService(Constants.ROLE_LFS);
                lfsService.tell(object, null);
            }
        }
        catch (ClassNotFoundException | JsonSyntaxException e) {
            log.error(e, "exception happened");
        }
        catch (ServletException e) {
            log.error(e, "exception happened");
            throw e;
        }
    }
}
