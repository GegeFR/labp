/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.lfs.impl;

import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author Gege
 */
public class Utils {

    public static byte[] export(LFSInterface lfs) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        String[] pathes = lfs.list("/", true);

        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            for (String path : pathes) {
                if (lfs.isFile(path)) {
                    ZipEntry zipEntry = new ZipEntry(path);
                    zos.putNextEntry(zipEntry);
                    zos.write(lfs.get(path));
                    zos.closeEntry();
                }
            }
        }

        return bos.toByteArray();
    }
}
