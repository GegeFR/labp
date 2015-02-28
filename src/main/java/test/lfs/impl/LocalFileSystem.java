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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Gwen
 */
public class LocalFileSystem implements LFSInterface {

    private final LoggingAdapter log;
    private final URI _root;
    private final String _name;

    public LocalFileSystem(ActorSystem system, URI root, String name) {
        log = Logging.getLogger(system, this.getClass());

        if (!root.isAbsolute()) {
            throw new RuntimeException("root must be absolute");
        }

        _root = root;
        _name = name;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public String[] list(String path, boolean recurse) {
        File file = new File(toOuterFS(_root, path));
        log.debug("matched " + path + " to " + file.toURI());

        LinkedList<File> results = new LinkedList();
        doList(file, results, recurse);

        String[] result = new String[results.size()];
        int j = 0;
        for (File child : results) {
            result[j++] = toInnerFS(_root, child.toURI(), child.isDirectory());
        }

        return result;
    }

    public void doList(File file, List<File> results, boolean recurse) {
        File[] children = file.listFiles();

        for (File child : children) {
            results.add(child);
            if (recurse && child.isDirectory()) {
                doList(child, results, recurse);
            }
        }
    }

    @Override
    public byte[] get(String path) throws FileNotFoundException, IOException {
        File file = new File(toOuterFS(_root, path));
        log.debug("matched " + path + " to " + file.toURI());

        long size = file.length();
        if (size > Integer.MAX_VALUE) {
            throw new RuntimeException("File is too big : greater than " + Integer.MAX_VALUE + " bytes");
        }
        else {
            byte[] content = new byte[(int) size];
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(content);
            }
            return content;
        }
    }

    @Override
    public boolean exists(String path) {
        File file = new File(toOuterFS(_root, path));
        log.debug("matched " + path + " to " + file.toURI());
        return file.exists();
    }

    @Override
    public boolean isFolder(String path) {
        File file = new File(toOuterFS(_root, path));
        log.debug("matched " + path + " to " + file.toURI());
        return file.isDirectory();
    }

    @Override
    public boolean isFile(String path) {
        File file = new File(toOuterFS(_root, path));
        return file.isFile();
    }

    private String toInnerFS(URI root, URI outerURI, boolean isFolder) {
        if (isFolder) {
            return "/" + root.relativize(outerURI).getPath() + "/";
        }
        else {
            return "/" + root.relativize(outerURI).getPath();
        }
    }

    private URI toOuterFS(URI root, String innerPath) {
        try {
            while(innerPath.startsWith("/")){
                innerPath = innerPath.substring(1);
            }
            
            return root.resolve(new URI(null, null, innerPath, null));
        }
        catch (Exception e) {
            throw new RuntimeException("should not happen", e);
        }
    }
}
