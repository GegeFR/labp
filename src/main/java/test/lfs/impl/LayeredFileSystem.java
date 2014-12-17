/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.lfs.impl;

import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Gwen
 */
public class LayeredFileSystem implements LFSInterface {

    private static final String BASE = "../lfs/";
    private static final String DEFAULT = "default";

    private static LayeredFileSystem INSTANCE;

    public static LayeredFileSystem initInstance(ActorSystem system) {
        return INSTANCE = new LayeredFileSystem(system);
    }

    public static LayeredFileSystem getInstance() {
        return INSTANCE;
    }

    private final LoggingAdapter _log;
    private final ActorSystem _system;

    private final LinkedList<LFSInterface> _activeLayers;
    private final LinkedList<LFSInterface> _inactiveLayers;

    private LayeredFileSystem(ActorSystem system) {
        _system = system;
        _activeLayers = new LinkedList();
        _inactiveLayers = new LinkedList();
        _log = Logging.getLogger(system, this.getClass());

        // at startup there is only one active layer : default
        _activeLayers.add(new LocalFileSystem(system, new File(BASE + DEFAULT + "/").toURI(), DEFAULT));

        File file = new File(BASE);
        File[] layers = file.listFiles((File pathname) -> pathname.isDirectory() && !pathname.getName().contains(" "));

        for (File layer : layers) {
            if (!layer.getName().equals(DEFAULT)) {
                _inactiveLayers.add(new LocalFileSystem(system, new File(BASE + layer.getName() + "/").toURI(), layer.getName()));
            }
        }
    }

    public synchronized void enable(String name) {
        LFSInterface layer = findLayerIn(_inactiveLayers, name);
        if (null != layer) {
            _inactiveLayers.remove(layer);
            _activeLayers.addFirst(layer);
        }
    }

    public synchronized void disable(String name) {
        LFSInterface layer = findLayerIn(_activeLayers, name);
        if (null != layer) {
            _activeLayers.remove(layer);
            _inactiveLayers.addFirst(layer);
        }
    }

    public synchronized void up(String name) {
        int index = findIndexIn(_activeLayers, name);
        if (index != -1) {
            if(index > 0){
                Collections.swap(_activeLayers, index, index - 1);
            }
        }
    }
    
    public synchronized void down(String name) {
        int index = findIndexIn(_activeLayers, name);
                _log.info("_activeLayers.size()="+_activeLayers.size());
                _log.info("name)="+name);
                _log.info("index="+index);
        if (index != -1) {
            if(index < _activeLayers.size() - 1){
                Collections.swap(_activeLayers, index, index + 1);
            }
        }
    }

    public synchronized LFSInterface getLayer(String name) {
        if (null == name) {
            return this;
        }
        else {
            LFSInterface res = findLayerIn(_activeLayers, name);
            if (null != res) {
                return res;
            }

            res = findLayerIn(_inactiveLayers, name);
            if (null != res) {
                return res;
            }

            return null;
        }
    }

    private LFSInterface findLayerIn(List<LFSInterface> list, String name) {
        for (LFSInterface fs : list) {
            if (fs.getName().equals(name)) {
                return fs;
            }
        }
        return null;
    }

    private int findIndexIn(List<LFSInterface> list, String name) {
        int i = 0;
        for (LFSInterface fs : list) {
            if (fs.getName().equals(name)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public String[] getActiveLayers() {
        String[] res = new String[_activeLayers.size()];
        int i = 0;
        for (LFSInterface fs : _activeLayers) {
            res[i++] = fs.getName();
        }
        return res;
    }

    public String[] getInactiveLayers() {
        String[] res = new String[_inactiveLayers.size()];
        int i = 0;
        for (LFSInterface fs : _inactiveLayers) {
            res[i++] = fs.getName();
        }
        return res;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String[] list(String path, boolean recurse) throws Exception {
        Set<String> pathes = new HashSet();

        for (LFSInterface fs : _activeLayers) {
            String[] list = fs.list(path, recurse);
            pathes.addAll(Arrays.asList(list));
        }

        String[] res = new String[pathes.size()];
        res = pathes.toArray(res);

        return res;
    }

    @Override
    public byte[] get(String path) throws Exception {
        LFSInterface lfs = findLayerForPath(path);
        if (null != lfs) {
            return lfs.get(path);
        }
        else {
            throw new FileNotFoundException(path);
        }
    }

    @Override
    public boolean exists(String path) throws Exception {
        LFSInterface lfs = findLayerForPath(path);
        // if we found an FS then the path exists
        return lfs != null;
    }

    @Override
    public boolean isFolder(String path) throws Exception {
        LFSInterface lfs = findLayerForPath(path);
        if (null != lfs) {
            return lfs.isFolder(path);
        }
        else {
            throw new FileNotFoundException(path);
        }
    }

    @Override
    public boolean isFile(String path) throws Exception {
        LFSInterface lfs = findLayerForPath(path);
        if (null != lfs) {
            return lfs.isFile(path);
        }
        else {
            throw new FileNotFoundException(path);
        }
    }

    private synchronized LFSInterface findLayerForPath(String path) throws Exception {
        for (LFSInterface fs : _activeLayers) {
            if (fs.exists(path)) {
                return fs;
            }
        }
        return null;
    }

}
