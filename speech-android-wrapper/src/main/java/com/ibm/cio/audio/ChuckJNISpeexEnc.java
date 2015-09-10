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

import com.ibm.cio.speex.ChuckSpeexWriter;
import com.ibm.cio.speex.FrequencyBand;
import com.ibm.cio.speex.JNISpeexEncoder;
import com.ibm.cio.util.Logger;
import com.ibm.cio.util.VaniUtils;
import com.ibm.cio.watsonsdk.SpeechRecorderDelegate;

// TODO: Auto-generated Javadoc
/**
 * JNI Speex encoder.
 */
public class ChuckJNISpeexEnc implements SpeechEncoder {
    // Use PROPRIETARY notice if class contains a main() method, otherwise use
    // COPYRIGHT notice.
    public static final String COPYRIGHT_NOTICE = "(c) Copyright IBM Corp. 2013";
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

    /** The temp. */
//	byte[] temp = new byte[640];//harded code 640

    /** Speex paramters */
    SpeexParam pam;

    /** Audio writer. */
    AudioFileWriter writer;

    /** Speex encoder. */
    JNISpeexEncoder speexEncoder;
    private int spxFrameSize = 0;
    private long compressDataTime = 0;
    private SpeechRecorderDelegate delegate = null;

    /**
     * Create a speex encoder with channel = 1, sample rate = 16000Hz.
     */
    public ChuckJNISpeexEnc() {
        Logger.i(TAG, "Construct ChuckSpeexEnc");

//		pam = new  SpeexParam();
//		pam.channels = 1;
//		pam.sampleRate = 16000;
//		pam.mode = getEncMode(pam.sampleRate);
//		this.compressDataTime = 0;
//		// Construct a new encoder
//		speexEncoder = new JNISpeexEncoder(FrequencyBand.WIDE_BAND, pam.quality);

    }

    /**
     * For WebsocketClient
     * @param client
     * @throws IOException
     */
    public void initEncoderWithWebSocketClient(ChuckWebSocketUploader client) throws IOException{
        Logger.i(TAG, "initEncoderWithWebSocketClient");

        pam = new  SpeexParam();
        pam.channels = 1;
        pam.sampleRate = 16000;
        pam.mode = getEncMode(pam.sampleRate);
        this.compressDataTime = 0;

        // Construct a new ChuckSpeexWriter
        writer = new ChuckSpeexWriter(client);

        // Assign the encoder instance
        this.speexEncoder = new JNISpeexEncoder(FrequencyBand.WIDE_BAND, pam.QUALITY);

//		speexEncoder = new JNISpeexEncoder(FrequencyBand.WIDE_BAND, pam.quality);

    }

    @Override
    public void onStart() {}

    /* (non-Javadoc)
     * @see com.ibm.cio.audio.SpeechEncoder#initEncodeAndWriteHeader(java.io.OutputStream)
     */
    public void initEncodeAndWriteHeader(OutputStream out) throws IOException {
        Logger.e(TAG, "initEncodeAndWriteHeader");
        long t1 = System.currentTimeMillis();
        Logger.i(TAG, "initEncodeAndWriteHeader at: " + t1);
        //byte[] temp = new byte[2560]; // stereo UWB requires one to read 2560b
        // DataInputStream dis = new DataInputStream(new FileInputStream(srcPCMFile));

        // Construct a new encoder
//		speexEncoder = new JNISpeexEncoder(FrequencyBand.WIDE_BAND, pam.quality);

        writer = new VaniOggSpeexWriter(pam, out);
        //writer.open(destSpxFile);
        writer.writeHeader("Encoded with: " + VERSION);
        Logger.i(TAG, "initEncodeAndWriteHeader end after: " + (System.currentTimeMillis() - t1));
    }
    @Override
    public byte[] encode(byte[] rawAudio) {
        // TODO Auto-generated method stub
        Logger.i(TAG, "encodeAndWrite data length: " + rawAudio.length
                + ", pam.nframes=" + pam.nframes);
//		long t1 = SystemClock.elapsedRealtime();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int pcmPacketSize = 2 * pam.channels * speexEncoder.getFrameSize(); // 640
//		Logger.d(TAG, "[encode] pcmPacketSize: " + pcmPacketSize);
        // read until we get to EOF
        int offset = 0;
        int l = pcmPacketSize;
        while (offset < rawAudio.length) {
            if (l + offset > rawAudio.length)
                l = rawAudio.length - offset;
            long t1 = SystemClock.elapsedRealtime();
            byte[] encoded = speexEncoder.encode(VaniUtils.toShorts(rawAudio, offset, l));
            compressDataTime += SystemClock.elapsedRealtime() - t1;
            try {
                if (spxFrameSize == 0)
                    spxFrameSize = encoded.length;
//				Logger.d(TAG, "encode frame size 0: " + spxFrameSize);
                bos.write(encoded);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
//			Logger.d(TAG, "encode time: " + (SystemClock.elapsedRealtime() - t1));
            offset += l;
        }
//		compressDataTime += SystemClock.elapsedRealtime() - t1;
//		Logger.d(TAG, "encode time: " + (SystemClock.elapsedRealtime() - t1));
        return bos.toByteArray();
    }
    @Override
    public void writeChunk(byte[] data) throws IOException {
        // TODO Auto-generated method stub
        Logger.d(TAG, "writeChunk frame size: " + spxFrameSize);
        long t0 = SystemClock.elapsedRealtime();
        int offSet = 0;
        if (spxFrameSize == 0)
            spxFrameSize = 70;
        Logger.d(TAG, "writeChunk frame size 2: " + spxFrameSize);
        while (offSet < data.length) {
            if (offSet + spxFrameSize > data.length)
                spxFrameSize = data.length - offSet;
            writer.writePacket(data, offSet, spxFrameSize);
            offSet += spxFrameSize;
        }
        Logger.d(TAG, "writeChunk time: " + (SystemClock.elapsedRealtime() - t0));
    }
    /* (non-Javadoc)
     * @see com.ibm.cio.audio.SpeechEncoder#encodeAndWrite(byte[])
     */
    public int encodeAndWrite(byte[] audioData) throws IOException {
        Logger.i(TAG, "encodeAndWrite data length: " + audioData.length + ", pam.nframes=" + pam.nframes);
        //byte[] temp = new byte[3840];;    // stereo UWB requires one to read 3840=framePeriod*bSamples/8*nChannels
        int pcmPacketSize = 2 * pam.channels * speexEncoder.getFrameSize(); // 640
        Logger.d(TAG, "[encodeAndWrite] pcmPacketSize: " + pcmPacketSize);
        // read until we get to EOF
        int offset = 0;
//		  long totalEnc = 0;
        long t1, t2;
        int l = pcmPacketSize;
        int uploadedAudioSize = 0;

        while (offset<audioData.length) {
            if (l + offset > audioData.length)
                l =  audioData.length - offset;

//	    	  	temp = new byte[pcmPacketSize];

//	    	  	System.arraycopy(audioData, offset, temp, 0, l);

            t1 = SystemClock.elapsedRealtime();
            byte[] encoded = speexEncoder.encode(VaniUtils.toShorts(audioData, offset, l));
//	        	Logger.i(TAG, "encodeAndWrite size: " + encoded.length);
//	        	Logger.d(TAG, "encode time: " + (SystemClock.elapsedRealtime() - t1));
            compressDataTime += SystemClock.elapsedRealtime() - t1;

            if (encoded.length > 0) {
                uploadedAudioSize += encoded.length;
                Log.d(TAG, "Writing and uploading speex data");
                writer.writePacket(encoded, 0, encoded.length);
            }
            t2 = SystemClock.elapsedRealtime();
//		        totalEnc += t2-t1;

            offset += l;
        }
        this._onRecordingCompleted(audioData);
//	      Logger.i(TAG, "encodeAndWrite time: " + totalEnc + "|uploadedAudioSize: " + uploadedAudioSize);
        return uploadedAudioSize;
    }

    private void _onRecordingCompleted(byte[] rawAudioData){
        if(this.delegate != null) delegate.onRecordingCompleted(rawAudioData);
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
        //speexEncoder
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public long getCompressionTime() {
        // TODO Auto-generated method stub
        return this.compressDataTime;
    }

    @Override
    public void setDelegate(SpeechRecorderDelegate obj) {
        // TODO Auto-generated method stub
        this.delegate = obj;
    }
}
