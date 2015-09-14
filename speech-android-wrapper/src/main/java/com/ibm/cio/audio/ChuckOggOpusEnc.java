/* ***************************************************************** */
/*                                                                   */
/* IBM Confidential                                                  */
/*                                                                   */
/* OCO Source Materials                                              */
/*                                                                   */
/* Copyright IBM Corp. 2015                                          */
/*                                                                   */
/* The source code for this program is not published or otherwise    */
/* divested of its trade secrets, irrespective of what has been      */
/* deposited with the U.S. Copyright Office.                         */
/*                                                                   */
/* ***************************************************************** */
package com.ibm.cio.audio;

import android.os.SystemClock;

import com.ibm.cio.dto.SpeechConfiguration;
import com.ibm.cio.opus.JNAOpus;
import com.ibm.cio.opus.OpusWriter;
import com.ibm.cio.util.Logger;
import com.ibm.cio.watsonsdk.SpeechRecorderDelegate;
import com.sun.jna.ptr.PointerByReference;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Ogg Opus Encoder
 */
public class ChuckOggOpusEnc extends OpusWriter implements ISpeechEncoder {
    // Use PROPRIETARY notice if class contains a main() method, otherwise use COPYRIGHT notice.
    public static final String COPYRIGHT_NOTICE = "(c) Copyright IBM Corp. 2015";
    /** The Constant TAG. */
    private static final String TAG = ChuckOggOpusEnc.class.getName();

    private OpusWriter writer = null;
    private PointerByReference opusEncoder;
    private int sampleRate = 16000;

    private SpeechRecorderDelegate delegate = null;

    public ChuckOggOpusEnc() {}
    /* (non-Javadoc)
     * @see com.ibm.cio.audio.VaniEncoder#initEncodeAndWriteHeader(java.io.OutputStream)
     */
    public void initEncodeAndWriteHeader(OutputStream out){}
    /**
     * For WebsocketClient
     * @param client
     * @throws IOException
     */
    public void initEncoderWithWebSocketClient(ChuckWebSocketUploader client) throws IOException{
        writer = new OpusWriter(client);

        IntBuffer error = IntBuffer.allocate(4);
        this.opusEncoder = JNAOpus.INSTANCE.opus_encoder_create(this.sampleRate, 1, JNAOpus.OPUS_APPLICATION_VOIP, error);
    }

    @Override
    public void onStart() {
        writer.writeHeader("encoder=Lavc56.20.100 libopus");
    }

    @Override
    public void writeChunk(byte[] data) throws IOException {
        long t0 = SystemClock.elapsedRealtime();
        writer.writePacket(data, 0, data.length);
        Logger.d(TAG, "writeChunk time: " + (SystemClock.elapsedRealtime() - t0));
    }
    @Override
    public byte[] encode(byte[] rawAudio) {
        int read = 0;
        ByteArrayInputStream ios = new ByteArrayInputStream(rawAudio);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        byte[] data = new byte[SpeechConfiguration.FRAME_SIZE*2];
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

                int opus_encoded = JNAOpus.INSTANCE.opus_encode(this.opusEncoder, shortBuffer, SpeechConfiguration.FRAME_SIZE, opusBuffer, bufferSize);

                opusBuffer.position(opus_encoded);
                opusBuffer.flip();

                byte[] opusData = new byte[opusBuffer.remaining()];
                opusBuffer.get(opusData, 0, opusData.length);

                try {
                    bos.write(opusData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] byteData = bos.toByteArray();
        try {
            bos.close();
            bos = null;
            ios.close();
            ios = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteData;
    }

    /**
     * Encode raw audio data into Opus format then call OpusWriter to write the Ogg packet
     *
     * @param rawAudio
     * @return
     * @throws IOException
     */
    public int encodeAndWrite(byte[] rawAudio) throws IOException {
        int read = 0;

        int uploadedAudioSize = 0;
        ByteArrayInputStream ios = new ByteArrayInputStream(rawAudio);

        byte[] data = new byte[SpeechConfiguration.FRAME_SIZE*2];
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

            int opus_encoded = JNAOpus.INSTANCE.opus_encode(this.opusEncoder, shortBuffer, SpeechConfiguration.FRAME_SIZE, opusBuffer, bufferSize);

            opusBuffer.position(opus_encoded);
            opusBuffer.flip();

            byte[] opusData = new byte[opusBuffer.remaining()];
            opusBuffer.get(opusData, 0, opusData.length);

            if (opus_encoded > 0) {
                uploadedAudioSize += opusData.length;
                writer.writePacket(opusData, 0, opusData.length);
            }
        }

        ios.close();
        ios = null;
        this._onRecording(rawAudio);

        return uploadedAudioSize;
    }

    private void _onRecording(byte[] rawAudioData){
        if(this.delegate != null) delegate.onRecording(rawAudioData);
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

    public void setDelegate(SpeechRecorderDelegate obj){
        this.delegate = obj;
    }
}
