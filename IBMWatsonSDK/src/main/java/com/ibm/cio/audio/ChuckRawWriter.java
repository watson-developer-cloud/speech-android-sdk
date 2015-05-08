/**
 *
 */
package com.ibm.cio.audio;

import java.io.File;
import java.io.IOException;

import org.xiph.speex.AudioFileWriter;

import com.ibm.cio.util.Logger;


/**
 * @author Viney
 *
 */
public class ChuckRawWriter extends AudioFileWriter{

    private String TAG = this.getClass().getSimpleName();
    private ChuckWebSocketUploader client;


    /**
     *
     */
    public ChuckRawWriter(ChuckWebSocketUploader client) {
        this.client = client;
    }


    @Override
    public void close() throws IOException {
        Logger.d(TAG, "Writer Closing...");
        this.client.stop();
    }


    @Override
    public void open(File file) throws IOException {
        // TODO Auto-generated method stub

    }


    @Override
    public void open(String filename) throws IOException {
        // TODO Auto-generated method stub

    }


    @Override
    public void writeHeader(String comment) throws IOException {
        // TODO Auto-generated method stub

    }


    @Override
    public void writePacket(byte[] data, int offset, int len)
            throws IOException {
        this.client.upload(data);

    }

}
