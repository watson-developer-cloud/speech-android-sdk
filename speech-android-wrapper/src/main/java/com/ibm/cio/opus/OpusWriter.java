package com.ibm.cio.opus;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.xiph.speex.AudioFileWriter;

import com.ibm.cio.audio.VaniOpusUploader;
import com.ibm.cio.util.Logger;

public class OpusWriter extends AudioFileWriter {
    private String TAG = this.getClass().getSimpleName();
    private VaniOpusUploader client;

    public OpusWriter(VaniOpusUploader client){
        this.client = client;
    }
    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub
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
//	byte[] header;
//	boolean headerSent = false;

    @Override
    public void writeHeader(String comment) {
//		int headerlength = 15;
//	    this.header = new byte[headerlength];
//
//	    writeInt(header, 0, 20);	// 0-5. config - WB
//	    writeInt(header, 5, 0); 	// 6. mono
//	    writeInt(header, 6, 3);		// 7-8. an arbitrary number of frames in the packet
//
//	    writeInt(header, 8, 0);		// 9. v-no padding
//	    writeInt(header, 9, 0);		// 10. p-vbr
//	    writeInt(header, 10, 2);	// 11-15. frame count

//	    this.client.onHasData(header, false);
//	    this.client.upload(header);
    }

    @Override
    public void writePacket(byte[] data, int offset, int len)
            throws IOException {
//		if(this.headerSent == false){
//			this.client.upload(this.header);
//			this.headerSent = true;
//		}

//		byte[] buffer = new byte[len];
//		Logger.d(TAG, "Copying "+buffer.length+"/"+ data.length +" bytes, offset="+offset);
//		System.arraycopy(data, 0, buffer, 0, len);
        this.client.upload(data);
    }
}
