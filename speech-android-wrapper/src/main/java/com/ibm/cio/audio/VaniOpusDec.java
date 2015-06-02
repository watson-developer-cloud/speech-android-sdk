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

import java.io.ByteArrayOutputStream;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

import org.xiph.speex.SpeexDecoder;

import android.os.SystemClock;

import com.ibm.cio.opus.OpusDecoder;

import com.ibm.cio.util.Logger;
import com.ibm.cio.util.VaniUtils;

/**
 * Speex decoder.
 */
public class VaniOpusDec {
    // Use PROPRIETARY notice if class contains a main() method, otherwise use
    // COPYRIGHT notice.
    public static final String COPYRIGHT_NOTICE = "(c) Copyright IBM Corp. 2013";
    /** The Constant TAG. */
    private static final String TAG = VaniOpusDec.class.getName();

    /** Speex decoder. */
    protected SpeexDecoder speexDecoder;

    /** Defines whether or not the perceptual enhancement is used. */
    protected boolean enhanced  = true;
    /** If input is raw, defines the decoder mode (0=NB, 1=WB and 2-UWB). */
    private int mode          = 0;
    /** If input is raw, defines the number of frmaes per packet. */
    private int nframes       = 1;
    /** If input is raw, defines the sample rate of the audio. */
    private int sampleRate    = 48000;
    /** If input is raw, defines th number of channels (1=mono, 2=stereo). */
    private int channels      = 1;
    /** The percentage of packets to lose in the packet loss simulation. */
    private int loss          = 0;

    private int rawDataLength = 0;
    private byte[] header = new byte[2048];
//    private byte[] payload = new byte[65536];
    private byte[] decdat = new byte[sampleRate * 2 * 2]; // 44100 * 2 * 2
    /**
     * Constructor.
     */
    public VaniOpusDec() {
    }

    public byte[] decodeOggBytes(byte[] oggData) {
        final int OGG_HEADERSIZE = 27;
        final int OGG_SEGOFFSET = 26;
        final String OGGID = "OggS";
        int segments = 0;
        int curseg = 0;
        int bodybytes = 0;
        int decsize = 0;
        int packetNo = 0;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] decodedAudioByte;
        int offSet = 0;
        int s2bTime = 0;
        long t1 = 0;

        try {
            // read until we get to EOF
            while (offSet < oggData.length) {
                // validate header
                if (offSet + OGG_HEADERSIZE > oggData.length) {
                    decodedAudioByte = baos.toByteArray();
                    Logger.e(TAG, "Invalid header: " + decodedAudioByte.length);
                    return decodedAudioByte;
                }

                System.arraycopy(oggData, offSet, header, 0, OGG_HEADERSIZE);
                offSet += OGG_HEADERSIZE;
                rawDataLength += OGG_HEADERSIZE;

				// validate header
                if (!OGGID.equals(new String(header, 0, 4))) {
                    decodedAudioByte = baos.toByteArray();
                    Logger.e(TAG, "Invalid header: " + decodedAudioByte.length);
                    return decodedAudioByte;
                }

                // how many segments are there?
                segments = header[OGG_SEGOFFSET] & 0xFF;
                System.arraycopy(oggData, offSet, header, OGG_HEADERSIZE, segments);
                offSet += segments;
                rawDataLength += segments;

                // decoding segments
                if (segments == 1) {
                    bodybytes = header[OGG_HEADERSIZE + curseg] & 0xFF;
                    Logger.e(TAG, "["+packetNo+"] #segments="+segments+", bodybytes=" + bodybytes + ", curseg=" + curseg + "");
                    byte[] payload = new byte[bodybytes];
                    System.arraycopy(oggData, offSet, payload, 0, bodybytes);
                    if (packetNo == 0) {
                        if (readOpusHeader(payload, 0, bodybytes)) {
                            packetNo++;
                        }
                        else {
                            packetNo = 0;
                        }
                    }
                    // Ogg Comment packet
                    else if (packetNo == 1) {
                        if(readOpusComments(payload, offSet, bodybytes)){
                            packetNo++;
                        }
                        else{
                            Logger.e(TAG, "Sorry, comments does not right");
                            decodedAudioByte = baos.toByteArray();
                            return decodedAudioByte;
                        }
                    }
                    offSet += bodybytes;
                    rawDataLength += bodybytes;
                }
                else {
                    int bOffset = offSet;
                    for (curseg = 0; curseg < segments; curseg++) {
                        // get the number of bytes in the segment
                        bodybytes = header[OGG_HEADERSIZE + curseg] & 0xFF;
                        Logger.e(TAG, "["+packetNo+"] #segments="+segments+", bodybytes=" + bodybytes + ", curseg=" + curseg + "");
                        if (bodybytes == 255) {
                            Logger.d(TAG, "Sorry, don't handle 255 sizes!");
                            decodedAudioByte = baos.toByteArray();
                            return decodedAudioByte;
                        }
                        // dis.readFully(payload, 0, bodybytes);
                        // System.arraycopy(spxData, offSet, payload, 0, bodybytes);
                        offSet += bodybytes;
//						Logger.d(TAG, "[decode] spx bytes: " + (bodybytes));
                        rawDataLength += bodybytes;
                        // chksum = OggCrc.checksum(chksum, payload, 0, bodybytes);
                    }

//					Logger.d(TAG, "[decode] data offset: " + bOffset + "|" + offSet);
//                    short[] decodedShort = decodeSpx(oggData, bOffset, offSet, nSpeexDecoder);
                    t1 = SystemClock.elapsedRealtime();

                    byte[] opusData = new byte[oggData.length-bOffset];
                    System.arraycopy(oggData, bOffset, opusData, 0, opusData.length);
                    byte[] decodedByte = this.getOpusBytes(opusData);
                    s2bTime += (SystemClock.elapsedRealtime() - t1);
                    baos.write(decodedByte);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        decodedAudioByte = baos.toByteArray();
        Logger.d(TAG, "[decode] opus total: " + rawDataLength + "|s2bTime: " + s2bTime);
        return decodedAudioByte;
    }

    private byte[] getOpusBytes(byte[] oggData){
        List<ByteBuffer> packets = new ArrayList<>();
        ByteBuffer dataBuffer = ByteBuffer.allocate(oggData.length);
        dataBuffer.put(oggData);
        packets.add(dataBuffer);
        ShortBuffer sb = OpusDecoder.decode(packets);
        short[] decodedShort = sb.array();
        byte[] decodedByte = VaniUtils.toBytes(decodedShort);
        return decodedByte;
    }

    /**
     * read opus header
     * @param packet
     * @param offset
     * @param bodybytes
     * @return
     */
    private boolean readOpusHeader(final byte[] packet, final int offset, final int bodybytes) {
        if ("OpusHead".equals(new String(packet, offset, 8))) {
//            mode = packet[40 + offset] & 0xFF;
//            int version = readInt(packet, offset+9);
//            Logger.e(TAG, "version="+version);

//            int channelCount = readInt(packet, offset+9);
//            Logger.e(TAG, "channel count="+channelCount);

//            int preSkip = readInt(packet, offset+10);
//            Logger.e(TAG, "preSkip="+preSkip);

            sampleRate = readInt(packet, offset + 12);
            Logger.e(TAG, "sampleRate="+sampleRate);
//
//            int gain = readInt(packet, offset+16);
//            Logger.e(TAG, "gain="+gain);

//            channels = readInt(packet, offset + 48);
//            Logger.e(TAG, "channels="+channels);
//            nframes = readInt(packet, offset + 64);
//            Logger.e(TAG, "nframes="+nframes);
            return true;
        }
        return false;
    }

    private boolean readOpusComments(final byte[] packet, final int offset, final int bodybytes) {
        String opusTags = new String(packet, 0, 8);
        if ("OpusTags".equals(opusTags)) {
            return true;
        }
        return false;
    }

    /**
     * Converts Little Endian (Windows) bytes to an int (Java uses Big Endian).
     * @param data the data to read.
     * @param offset the offset from which to start reading.
     * @return the integer value of the reassembled bytes.
     */
    protected static int readInt(final byte[] data, final int offset) {
        return (data[offset] & 0xff) |
                ((data[offset+1] & 0xff) <<  8) |
                ((data[offset+2] & 0xff) << 16) |
                (data[offset+3] << 24); // no 0xff on the last one to keep the sign
    }
}
