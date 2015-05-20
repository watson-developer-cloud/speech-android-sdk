/* ***************************************************************** */
/*                                                                   */
/* IBM Confidential                                                  */
/*                                                                   */
/* OCO Source Materials                                              */
/*                                                                   */
/* Copyright IBM Corp. 2013                                          */
/*                                                                   */
/* The source code for this program is not published or otherwise    */
/* divested of its trade secrets, irrespective of what has been      */
/* deposited with the U.S. Copyright Office.                         */
/*                                                                   */
/* ***************************************************************** */
package com.ibm.cio.audio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import android.os.SystemClock;

import com.ibm.cio.opus.ChuckOpusWriter;
import com.ibm.cio.opus.JNAOpus;
import com.ibm.cio.util.Logger;
import com.ibm.cio.watsonsdk.SpeechRecorderDelegate;
import com.sun.jna.ptr.PointerByReference;

// TODO: Auto-generated Javadoc
/**
 * JNI Speex encoder.
 */
public class ChuckJNAOpusEnc implements VaniEncoder {
    // Use PROPRIETARY notice if class contains a main() method, otherwise use
    // COPYRIGHT notice.
    public static final String COPYRIGHT_NOTICE = "(c) Copyright IBM Corp. 2014";
    /** The Constant TAG. */
    private static final String TAG = ChuckJNAOpusEnc.class.getName();

    private ChuckOpusWriter writer = null;
    private PointerByReference opusEncoder;
    private int framesize = 160;
    private int sampleRate = 16000;
    private long compressDataTime = 0;
    //	private VaniOpusUploader client;
    private SpeechRecorderDelegate delegate = null;
    //
    public ChuckJNAOpusEnc() {
        Logger.i(TAG, "Construct VaniJNAOpusEnc");
        this.compressDataTime = 0;
    }
    /* (non-Javadoc)
     * @see com.ibm.cio.audio.VaniEncoder#initEncodeAndWriteHeader(java.io.OutputStream)
     */
    public void initEncodeAndWriteHeader(OutputStream out){
//		writer = new OpusWriter();
//		writer.open(destSpxFile);
//		writer.writeHeader("Encoded with: " + JNAOpus.INSTANCE.opus_get_version_string());
    }
    /**
     * For WebsocketClient
     * @param client
     * @throws IOException
     */
    public void initEncoderWithWebSocketClient(ChuckWebSocketUploader client) throws IOException{
//		this.client = client;
        writer = new ChuckOpusWriter(client);
//		writer.writeHeader("");

        IntBuffer error = IntBuffer.allocate(4);
        this.opusEncoder = JNAOpus.INSTANCE.opus_encoder_create(this.sampleRate, 1, JNAOpus.OPUS_APPLICATION_VOIP, error);
    }
    @Override
    public void writeChunk(byte[] data) throws IOException {
        long t0 = SystemClock.elapsedRealtime();
        writer.writePacket(data, 0, data.length);
        Logger.d(TAG, "writeChunk time: " + (SystemClock.elapsedRealtime() - t0));
    }
    @Override
    public byte[] encode(byte[] rawAudio) {
//		Log.d(TAG, "[encode] Audio Length Passed="+rawAudio.length);
        int read = 0;
        ByteArrayInputStream ios = new ByteArrayInputStream(rawAudio);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        byte[] data = new byte[this.framesize*2];
        //
        int bufferSize;
        try {
            while((read = ios.read(data)) > 0){
                bufferSize = read;
                byte[] pcmbuffer = new byte[read];
                System.arraycopy(data, 0, pcmbuffer, 0, read);

                ShortBuffer shortBuffer = ShortBuffer.allocate(bufferSize);
                for (int i = 0; i < read; i += 2) {
                    int b1 = pcmbuffer[i] & 0xff;
                    int b2 = pcmbuffer[i+1] << 8;
                    shortBuffer.put((short) (b1 | b2));
                }
                shortBuffer.flip();
                ByteBuffer opusBuffer = ByteBuffer.allocate(bufferSize);
                long t1 = SystemClock.elapsedRealtime();

                int opus_encoded = JNAOpus.INSTANCE.opus_encode(this.opusEncoder, shortBuffer, this.framesize, opusBuffer, bufferSize);

                compressDataTime += SystemClock.elapsedRealtime() - t1;
                opusBuffer.position(opus_encoded);
                opusBuffer.flip();
                byte[] opusdata = new byte[opusBuffer.remaining()+1];
                opusdata[0] = (byte) opus_encoded;
                opusBuffer.get(opusdata, 1, opusdata.length-1);

                try {
                    bos.write(opusdata);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        byte[] byteData = bos.toByteArray();
        try {
            bos.close();
            bos = null;
            ios.close();
            ios = null;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return byteData;
    }
    /* (non-Javadoc)
     * @see com.ibm.cio.audio.VaniEncoder#encodeAndWrite(byte[])
     */
    public int encodeAndWrite(byte[] rawAudio) throws IOException {
//		Log.d(TAG, "[encodeAndWrite] Audio Length Passed="+rawAudio.length);
        int read = 0;
//		long totalEnc = 0;
        int uploadedAudioSize = 0;
        ByteArrayInputStream ios = new ByteArrayInputStream(rawAudio);
//		Logger.e(TAG, "@@@rawAudio="+rawAudio.length);
        byte[] data = new byte[this.framesize*2];
        int bufferSize;

        while((read = ios.read(data)) > 0){
            bufferSize = read;
            byte[] pcmbuffer = new byte[read];
            System.arraycopy(data, 0, pcmbuffer, 0, read);

            ShortBuffer shortBuffer = ShortBuffer.allocate(bufferSize);
            for (int i = 0; i < read; i += 2) {
                int b1 = pcmbuffer[i] & 0xff;
                int b2 = pcmbuffer[i+1] << 8;
                shortBuffer.put((short) (b1 | b2));
            }
            shortBuffer.flip();
            ByteBuffer opusBuffer = ByteBuffer.allocate(bufferSize);
            long t1 = SystemClock.elapsedRealtime();

            int opus_encoded = JNAOpus.INSTANCE.opus_encode(this.opusEncoder, shortBuffer, this.framesize, opusBuffer, bufferSize);

            compressDataTime += SystemClock.elapsedRealtime() - t1;
            opusBuffer.position(opus_encoded);
            opusBuffer.flip();
            byte[] opusdata = new byte[opusBuffer.remaining()+1];
            opusdata[0] = (byte) opus_encoded;
            opusBuffer.get(opusdata, 1, opusdata.length-1);

            if (opus_encoded > 0) {
                uploadedAudioSize += opusdata.length;
                writer.writePacket(opusdata, 0, opusdata.length);
            }
//			long t2 = SystemClock.elapsedRealtime();
//			totalEnc += t2 - t1;
        }

        ios.close();
        ios = null;
//		Logger.i(TAG, "encodeAndWrite time: " + totalEnc + ", uploadedAudioSize: " + uploadedAudioSize);
        this._onRecordingCompleted(rawAudio);
        return uploadedAudioSize;
    }

    private void _onRecordingCompleted(byte[] rawAudioData){
        if(this.delegate != null) delegate.onRecordingCompleted(rawAudioData);
    }
    /* (non-Javadoc)
     * @see com.ibm.cio.audio.VaniEncoder#close()
     */
    public void close() {
        try {
            writer.close();
            JNAOpus.INSTANCE.opus_encoder_destroy(this.opusEncoder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public long getCompressionTime() {
        // TODO Auto-generated method stub
        return this.compressDataTime;
    }

    public void setDelegate(SpeechRecorderDelegate obj){
        this.delegate = obj;
    }

}
