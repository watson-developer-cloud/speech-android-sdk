package com.ibm.cio.speex;

import java.io.File;
import java.io.IOException;

import org.xiph.speex.AudioFileWriter;

import com.ibm.cio.audio.ChuckWebSocketUploader;
import com.ibm.cio.util.Logger;

public class ChuckSpeexWriter extends AudioFileWriter {
    private String TAG = this.getClass().getSimpleName();
    private ChuckWebSocketUploader client;

    public ChuckSpeexWriter(ChuckWebSocketUploader client){
        this.client = client;
    }

    @Override
    public void close() throws IOException {
        Logger.d(TAG, "Writer Closing...");
        this.client.stop();
    }

    @Override
    public void open(File file) throws IOException {}

    @Override
    public void open(String filename) throws IOException {}

    @Override
    public void writeHeader(String comment) {}

    @Override
    public void writePacket(byte[] data, int offset, int len)
            throws IOException {
        this.client.upload(data);
    }
}
