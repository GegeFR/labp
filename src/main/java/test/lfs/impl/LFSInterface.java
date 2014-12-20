/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.lfs.impl;

/**
 *
 * @author Gwen
 */
public interface LFSInterface {

    public String getName();

    public String[] list(String path, boolean recurse) throws Exception;

    public byte[] get(String path) throws Exception;

    public boolean exists(String path) throws Exception;

    public boolean isFolder(String path) throws Exception;

    public boolean isFile(String path) throws Exception;
}
