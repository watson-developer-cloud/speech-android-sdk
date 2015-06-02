package com.ibm.cio.opus;

import com.ibm.cio.util.Logger;
import com.sun.jna.Native;
import com.sun.jna.ptr.PointerByReference;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.List;

/**
 * Created by mihui on 6/1/15.
 */
public class OpusDecoder {
    static {
        try {
            System.loadLibrary("opus");
        } catch (UnsatisfiedLinkError e1) {
            try {
                File f = Native.extractFromResourcePath("opus");
                System.load(f.getAbsolutePath());
            } catch (Exception e2) {
                e1.printStackTrace();
                e2.printStackTrace();
            }
        }
    }

    public OpusDecoder(){

    }

    public static ShortBuffer decode(List<ByteBuffer> packets) {
        IntBuffer error = IntBuffer.allocate(4);
        PointerByReference opusDecoder = JNAOpus.INSTANCE.opus_decoder_create(8000, 1, error);

        ShortBuffer shortBuffer = ShortBuffer.allocate(1024 * 1024);
        for (ByteBuffer dataBuffer : packets) {
            byte[] transferedBytes = new byte[dataBuffer.remaining()];
            dataBuffer.get(transferedBytes);
            int decoded = JNAOpus.INSTANCE.opus_decode(opusDecoder, transferedBytes, transferedBytes.length, shortBuffer, 80, 0);
            shortBuffer.position(shortBuffer.position() + decoded);
        }
        shortBuffer.flip();

        JNAOpus.INSTANCE.opus_decoder_destroy(opusDecoder);
        return shortBuffer;
    }

}
