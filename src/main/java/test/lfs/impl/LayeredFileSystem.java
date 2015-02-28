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
import test.utils.WithMeta;

/**
 *
 * @author Gwen
 */
public class LayeredFileSystem implements LFSInterface {

    private static final String BASE = "../lfs";
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

    private final LinkedList<WithMeta<LFSInterface, Boolean>> _layers;

    private LayeredFileSystem(ActorSystem system) {
        _system = system;
        _layers = new LinkedList();
        _log = Logging.getLogger(system, this.getClass());

        File file = new File(BASE);
        File[] layers = file.listFiles((File pathname) -> pathname.isDirectory() && !pathname.getName().contains(" "));

        // fill all layers but DEFAULT
        for (File layer : layers) {
            if (!layer.getName().equals(DEFAULT)) {
                _layers.add(new WithMeta(new LocalFileSystem(system, new File(BASE + "/" + layer.getName() + "/").toURI(), layer.getName()), false));
            }
        }

        // at bottom there is one active layer : default
        _layers.add(new WithMeta(new LocalFileSystem(system, new File(BASE + "/" + DEFAULT + "/").toURI(), DEFAULT), true));

    }

    public synchronized void enable(String name) {
        WithMeta<LFSInterface, Boolean> pair = getPair(name);
        if (null != pair) {
            pair.setMeta(Boolean.TRUE);
        }
    }

    public synchronized void disable(String name) {
        WithMeta<LFSInterface, Boolean> pair = getPair(name);
        if (null != pair) {
            pair.setMeta(Boolean.FALSE);
        }
    }

    public synchronized void up(String name) {
        int index = getLayerIndex(name);
        if (index != -1 && index > 0) {
            Collections.swap(_layers, index - 1, index);
        }
    }

    public synchronized void down(String name) {
        int index = getLayerIndex(name);
        if (index != -1 && index < _layers.size() - 1) {
            Collections.swap(_layers, index, index + 1);
        }
    }

    public synchronized WithMeta<LFSInterface, Boolean> getPair(String name) {
        for (WithMeta<LFSInterface, Boolean> pair : _layers) {
            if (pair.getObject().getName().equals(name)) {
                return pair;
            }
        }
        return null;
    }

    public synchronized LFSInterface getLayer(String name) {
        if(null == name){
            return this;
        }
        else{
            WithMeta<LFSInterface, Boolean> pair = getPair(name);
            if (null != pair) {
                return pair.getObject();
            }
            else {
                throw new RuntimeException("Could not find Layer named " + name);
            }
        }
    }

    private int getLayerIndex(String name) {
        int i = 0;
        for (WithMeta<LFSInterface, Boolean> pair : _layers) {
            if (pair.getObject().getName().equals(name)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public List<WithMeta<LFSInterface, Boolean>> getLayers() {
        return Collections.unmodifiableList(_layers);
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String[] list(String path, boolean recurse) throws Exception {
        Set<String> pathes = new HashSet();

        for (WithMeta<LFSInterface, Boolean> pair : _layers) {
            if (pair.getMeta()) {
                String[] list = pair.getObject().list(path, recurse);
                pathes.addAll(Arrays.asList(list));
            }
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
        for (WithMeta<LFSInterface, Boolean> pair : _layers) {
            if (pair.getMeta()) {
                if (pair.getObject().exists(path)) {
                    return pair.getObject();
                }
            }
        }
        return null;
    }

}
