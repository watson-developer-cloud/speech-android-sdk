package com.ibm.cio.opus;

import java.io.File;
import java.io.IOException;
import org.xiph.speex.AudioFileWriter;
import com.ibm.cio.audio.ChuckWebSocketUploader;

public class ChuckOpusWriter extends AudioFileWriter {
    private ChuckWebSocketUploader client;

    public ChuckOpusWriter(ChuckWebSocketUploader client){
        this.client = client;
    }

    @Override
    public void close() throws IOException {
        this.client.stop();
    }

    @Override
    public void open(File file) throws IOException {
    }

    @Override
    public void open(String filename) throws IOException {
    }

    @Override
    public void writeHeader(String comment) {}

    @Override
    public void writePacket(byte[] data, int offset, int len)
            throws IOException {
        this.client.upload(data);
    }
}
