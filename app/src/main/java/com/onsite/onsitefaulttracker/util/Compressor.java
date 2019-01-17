package com.onsite.onsitefaulttracker.util;

import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Compressor {
    private static final int BUFFER = 2048;
    private static final int UPLOAD_LIMIT = 100 * (1024 * 1024); //File upload limit megaBytes

    private File[] _files;
    private String _zipFile;
    private ZipEntry entry;
    private BufferedInputStream origin;
    private int offset;
    //byte data[]; //= new byte[BUFFER];

    public Compressor(File[] files, String zipFile) {
        _files = files;
        _zipFile = zipFile;
    }

    public int zip(int offset) {
        long _size = 0;
        long totalBytes = 0;
        int i;
        try  {
            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream(_zipFile + "." + String.valueOf(offset) + ".zip");
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
            byte data[] = new byte[BUFFER];

            for(i = offset; i < _files.length; i++) {
                //Log.v("Compress", "Adding: " + _files[i]);
                _size = _files[i].length();
                totalBytes += _size;
                FileInputStream fi = new FileInputStream(_files[i]);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(_files[i].getAbsolutePath()
                        .substring(_files[i].getAbsolutePath().lastIndexOf("/") + 1));

                out.putNextEntry(entry);
                int count;
                if (totalBytes >= UPLOAD_LIMIT) {
                    origin.close();
                    out.close();
                    return i;
                }
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }
            Log.v("Compress", "Bytes: " + totalBytes + "B");
            out.close();

        } catch(Exception e) {
            e.printStackTrace();
            return -1;
        }
        return i;
    }

    public void zip() {
        long _size = 0;
        long totalBytes = 0;
        try  {
            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream(_zipFile);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
            byte data[] = new byte[BUFFER];

            for(int i = 0; i < _files.length; i++) {
                //Log.v("Compress", "Adding: " + _files[i]);
                _size = _files[i].length();
                totalBytes += _size;
                FileInputStream fi = new FileInputStream(_files[i]);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(_files[i].getAbsolutePath()
                        .substring(_files[i].getAbsolutePath().lastIndexOf("/") + 1));

                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();

            }
            Log.v("Compress", "Bytes: " + totalBytes + "B");
            out.close();

        } catch(Exception e) {
            e.printStackTrace();
        }

    }
}