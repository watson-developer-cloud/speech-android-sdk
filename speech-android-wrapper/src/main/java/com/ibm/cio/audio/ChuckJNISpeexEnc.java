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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.xiph.speex.AudioFileWriter;

import android.os.SystemClock;
import android.util.Log;

import com.ibm.cio.dto.SpeechConfiguration;
import com.ibm.cio.speex.ChuckSpeexWriter;
import com.ibm.cio.speex.FrequencyBand;
import com.ibm.cio.speex.JNISpeexEncoder;
import com.ibm.cio.util.Logger;
import com.ibm.cio.util.SpeechUtility;
import com.ibm.cio.watsonsdk.SpeechRecorderDelegate;

/**
 * JNI Speex encoder.
 */
public class ChuckJNISpeexEnc implements ISpeechEncoder {
    // Use PROPRIETARY notice if class contains a main() method, otherwise use COPYRIGHT notice.
    public static final String COPYRIGHT_NOTICE = "(c) Copyright IBM Corp. 2015";
    /** The Constant TAG. */
    private static final String TAG = ChuckJNISpeexEnc.class.getName();

    /** The Constant NARROW_BAND. */
    public static final int NARROW_BAND = 0;

    /** The Constant WIDE_BAND. */
    public static final int WIDE_BAND = 1;

    /** The Constant ULTRA_WIDE_BAND. */
    public static final int ULTRA_WIDE_BAND= 2;

    /** The Constant VERSION. */
    public static final String VERSION = "Java Speex Command Line Encoder v0.9.7 ($Revision: 1.5 $)";

    /** Speex paramters */
    SpeexParam pam;

    /** Audio writer. */
    AudioFileWriter writer;

    /** Speex encoder. */
    JNISpeexEncoder speexEncoder;
    private int spxFrameSize = 0;
    private SpeechRecorderDelegate delegate = null;

    /**
     * Create a speex encoder with channel = 1, sample rate = 16000Hz.
     */
    public ChuckJNISpeexEnc() {}

    /**
     * For WebsocketClient
     * @param client
     * @throws IOException
     */
    public void initEncoderWithWebSocketClient(ChuckWebSocketUploader client) throws IOException{
        pam = new  SpeexParam();
        pam.channels = 1;
        pam.sampleRate = SpeechConfiguration.SAMPLE_RATE;
        pam.mode = getEncMode(pam.sampleRate);

        // Construct a new ChuckSpeexWriter
        writer = new ChuckSpeexWriter(client);

        // Assign the encoder instance
        this.speexEncoder = new JNISpeexEncoder(FrequencyBand.WIDE_BAND, pam.QUALITY);
    }

    @Override
    public void onStart() {}

    /* (non-Javadoc)
     * @see com.ibm.cio.audio.SpeechEncoder#initEncodeAndWriteHeader(java.io.OutputStream)
     */
    public void initEncodeAndWriteHeader(OutputStream out) throws IOException {
        writer = new ChunkOggSpeexWriter(pam, out);
        writer.writeHeader("Encoded with: " + VERSION);
    }
    @Override
    public byte[] encode(byte[] rawAudio) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int pcmPacketSize = 2 * pam.channels * speexEncoder.getFrameSize(); // 640
        int offset = 0;
        int l = pcmPacketSize;
        while (offset < rawAudio.length) {
            if (l + offset > rawAudio.length)
                l = rawAudio.length - offset;
            byte[] encoded = speexEncoder.encode(SpeechUtility.toShorts(rawAudio, offset, l));
            try {
                if (spxFrameSize == 0)
                    spxFrameSize = encoded.length;
                bos.write(encoded);
            } catch (Exception e) {
                e.printStackTrace();
            }
            offset += l;
        }
        return bos.toByteArray();
    }
    @Override
    public void writeChunk(byte[] data) throws IOException {
        int offSet = 0;
        if (spxFrameSize == 0)
            spxFrameSize = 70;
        while (offSet < data.length) {
            if (offSet + spxFrameSize > data.length)
                spxFrameSize = data.length - offSet;
            writer.writePacket(data, offSet, spxFrameSize);
            offSet += spxFrameSize;
        }
    }
    /* (non-Javadoc)
     * @see com.ibm.cio.audio.SpeechEncoder#encodeAndWrite(byte[])
     */
    public int encodeAndWrite(byte[] audioData) throws IOException {
        int pcmPacketSize = 2 * pam.channels * speexEncoder.getFrameSize(); // 640
        int offset = 0;
        long t1;
        int l = pcmPacketSize;
        int uploadedAudioSize = 0;

        while (offset<audioData.length) {
            if (l + offset > audioData.length)
                l =  audioData.length - offset;

            byte[] encoded = speexEncoder.encode(SpeechUtility.toShorts(audioData, offset, l));

            if (encoded.length > 0) {
                uploadedAudioSize += encoded.length;
                writer.writePacket(encoded, 0, encoded.length);
            }
            offset += l;
        }
        this._onRecording(audioData);
        return uploadedAudioSize;
    }

    private void _onRecording(byte[] rawAudioData){
        if(this.delegate != null) delegate.onRecording(rawAudioData);
    }

    /**
     * Gets the encode mode.
     *
     * @param sampleRate the sample rate
     * @return the encode mode
     */
    private int getEncMode(int sampleRate) {
        if (sampleRate < 100) // Sample Rate has probably been given in kHz
            sampleRate *= 1000;

        if (sampleRate < 12000)
            return NARROW_BAND; // Narrowband
        else if (sampleRate < 24000)
            return WIDE_BAND; // Wideband
        else
            return ULTRA_WIDE_BAND; // Ultra-wideband
    }

    /**
     * Converts Little Endian (Windows) bytes to an int (Java uses Big Endian).
     * @param data the data to read.
     * @param offset the offset from which to start reading.
     * @return the integer value of the reassembled bytes.
     */
    protected static int readInt(final byte[] data, final int offset)
    {
        return (data[offset] & 0xff) |
                ((data[offset+1] & 0xff) <<  8) |
                ((data[offset+2] & 0xff) << 16) |
                (data[offset+3] << 24); // no 0xff on the last one to keep the sign
    }

    /**
     * Converts Little Endian (Windows) bytes to an short (Java uses Big Endian).
     * @param data the data to read.
     * @param offset the offset from which to start reading.
     * @return the integer value of the reassembled bytes.
     */
    protected static int readShort(final byte[] data, final int offset) {
        return (data[offset] & 0xff) |
                (data[offset+1] << 8); // no 0xff on the last one to keep the sign
    }

    /* (non-Javadoc)
     * @see com.ibm.cio.audio.SpeechEncoder#close()
     */
    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setDelegate(SpeechRecorderDelegate obj) {
        this.delegate = obj;
    }
}
