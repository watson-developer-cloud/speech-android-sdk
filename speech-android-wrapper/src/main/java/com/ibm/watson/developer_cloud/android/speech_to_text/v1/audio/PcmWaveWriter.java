/******************************************************************************
 *                                                                            *
 * Copyright (c) 1999-2003 Wimba S.A., All Rights Reserved.                   *
 *                                                                            *
 * COPYRIGHT:                                                                 *
 *      This software is the property of Wimba S.A.                           *
 *      This software is redistributed under the Xiph.org variant of          *
 *      the BSD license.                                                      *
 *      Redistribution and use in source and binary forms, with or without    *
 *      modification, are permitted provided that the following conditions    *
 *      are met:                                                              *
 *      - Redistributions of source code must retain the above copyright      *
 *      notice, this list of conditions and the following disclaimer.         *
 *      - Redistributions in binary form must reproduce the above copyright   *
 *      notice, this list of conditions and the following disclaimer in the   *
 *      documentation and/or other materials provided with the distribution.  *
 *      - Neither the name of Wimba, the Xiph.org Foundation nor the names of *
 *      its contributors may be used to endorse or promote products derived   *
 *      from this software without specific prior written permission.         *
 *                                                                            *
 * WARRANTIES:                                                                *
 *      This software is made available by the authors in the hope            *
 *      that it will be useful, but without any warranty.                     *
 *      Wimba S.A. is not liable for any consequence related to the           *
 *      use of the provided software.                                         *
 *                                                                            *
 * Class: PcmWaveWriter.java                                                  *
 *                                                                            *
 * Author: James LAWRENCE                                                     *
 * Modified by: Marc GIMPEL                                                   *
 *                                                                            *
 * Date: March 2003                                                           *
 *                                                                            *
 ******************************************************************************/

/* $Id: PcmWaveWriter.java,v 1.2 2004/10/21 16:21:57 mgimpel Exp $ */

package com.ibm.watson.developer_cloud.android.speech_to_text.v1.audio;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Writes basic PCM wave files from binary audio data.
 *
 * <p>Here's an example that writes 2 seconds of silence
 * <pre>
 * PcmWaveWriter s_wsw = new PcmWaveWriter(2, 44100);
 * byte[] silence = new byte[16*2*44100];
 * wsw.Open("C:\\out.wav");
 * wsw.WriteHeader(); 
 * wsw.WriteData(silence, 0, silence.length);
 * wsw.WriteData(silence, 0, silence.length);
 * wsw.Close(); 
 * </pre>
 *
 * @author Jim Lawrence, helloNetwork.com
 * @author Marc Gimpel, Wimba S.A. (mgimpel@horizonwimba.com)
 * @version $Revision: 1.2 $
 */
public class PcmWaveWriter extends AudioFileWriter {

    private static final String TAG = "PcmWaveWriter";

    /** Wave type code of PCM */
    public static final short WAVE_FORMAT_PCM = (short) 0x01;
    private RandomAccessFile raf;
    /** Defines the sampling rate of the audio input. */
    private int     sampleRate;
    /** Defines the number of channels of the audio input (1=mono, 2=stereo). */
    private int     channels;

    /**
     * Constructor.
     */
    public PcmWaveWriter() {}

    /**
     * Constructor.
     * @param sampleRate the number of samples per second.
     * @param channels   the number of audio channels (1=mono, 2=stereo, ...).
     */
    public PcmWaveWriter(final int sampleRate, final int channels)
    {
        this();
        this.channels   = channels;
        this.sampleRate = sampleRate;
    }

    /**
     * Closes the output file.
     * MUST be called to have a correct stream.
     * @exception IOException if there was an exception closing the Audio Writer.
     */
    public void close()
            throws IOException
    {
        raf.close();
    }

    /**
     * Open the output file.
     * @param file - file to open.
     * @exception IOException if there was an exception opening the Audio Writer.
     */
    public void open(final File file)
            throws IOException
    {
        file.delete();
        raf = new RandomAccessFile(file, "rw");
    }

    /**
     * Open the output file.
     * @param filename filename to open.
     * @exception IOException if there was an exception opening the Audio Writer.
     */
    public void open(final String filename)
            throws IOException
    {
        open(new File(filename));
    }

    /**
     * Writes the initial data chunks that start the wave file.
     * Prepares file for data samples to written.
     * @param comment ignored by the WAV header.
     * @exception IOException
     */
    public void writeHeader(final String comment)
            throws IOException
    {
        /* writes the RIFF chunk indicating wave format */
        byte[] chkid = "RIFF".getBytes();
        raf.write(chkid, 0, chkid.length);
        writeInt(raf, 0); /* total length must be blank */
        chkid = "WAVE".getBytes();
        raf.write(chkid, 0, chkid.length);

        /* format subchunk: of size 16 */
        chkid = "fmt ".getBytes();
        raf.write(chkid, 0, chkid.length);

        writeInt(raf, 16);                            // Size of format chunk
        writeShort(raf, WAVE_FORMAT_PCM);             // Format tag: PCM
        writeShort(raf, (short) channels);             // Number of channels
        writeInt(raf, sampleRate);                    // Sampling frequency
        writeInt(raf, sampleRate * channels * 2);         // Average bytes per second
        writeShort(raf, (short) (channels * 2));      // Blocksize of data
        writeShort(raf, (short) 16);                  // Bit per sample
        /* write the start of data chunk */
        chkid = "data".getBytes();
        raf.write(chkid, 0, chkid.length);
        writeInt(raf, 0);
    }

    /**
     * Saves PCM data to WAV file
     * @param pcmdata
     * : byte array containing the PCM data
     * @param srate
     * : Sample rate
     * @param channel
     * : no. of channels
     * @param format
     * : PCM format (16 bit)
     * @throws IOException
     */
    public byte[] saveWav(byte[] pcmdata, int srate, int channel, int format) {

        byte[] header = new byte[44];
        byte[] data = pcmdata;

        long totalDataLen = data.length + 36;
        long bitrate = srate * channel * format;

        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = (byte) format;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;
        header[21] = 0;
        header[22] = (byte) channel;
        header[23] = 0;
        header[24] = (byte) (srate & 0xff);
        header[25] = (byte) ((srate >> 8) & 0xff);
        header[26] = (byte) ((srate >> 16) & 0xff);
        header[27] = (byte) ((srate >> 24) & 0xff);
        header[28] = (byte) ((bitrate / 8) & 0xff);
        header[29] = (byte) (((bitrate / 8) >> 8) & 0xff);
        header[30] = (byte) (((bitrate / 8) >> 16) & 0xff);
        header[31] = (byte) (((bitrate / 8) >> 24) & 0xff);
        header[32] = (byte) ((channel * format) / 8);
        header[33] = 0;
        header[34] = 16;
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (data.length & 0xff);
        header[41] = (byte) ((data.length >> 8) & 0xff);
        header[42] = (byte) ((data.length >> 16) & 0xff);
        header[43] = (byte) ((data.length >> 24) & 0xff);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        try {
            outputStream.write(header);
            outputStream.write(data);

        } catch (IOException e) {
            Log.e(TAG, "Error writing data to wav buffer");
            e.printStackTrace();
        }
        return outputStream.toByteArray();
    }

    /**
     * Saves PCM data to WAV file
     * @param pcmdata
     * : byte array containing the PCM data
     * @param srate
     * : Sample rate
     * @param channel
     * : no. of channels
     * @param format
     * : PCM format (16 bit)
     * @throws IOException
     */
    public void saveWavFile(byte[] pcmdata, int srate, int channel, int format) {

        byte[] header = new byte[44];
        byte[] data = pcmdata;

        long totalDataLen = data.length + 36;
        long bitrate = srate * channel * format;

        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = (byte) format;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;
        header[21] = 0;
        header[22] = (byte) channel;
        header[23] = 0;
        header[24] = (byte) (srate & 0xff);
        header[25] = (byte) ((srate >> 8) & 0xff);
        header[26] = (byte) ((srate >> 16) & 0xff);
        header[27] = (byte) ((srate >> 24) & 0xff);
        header[28] = (byte) ((bitrate / 8) & 0xff);
        header[29] = (byte) (((bitrate / 8) >> 8) & 0xff);
        header[30] = (byte) (((bitrate / 8) >> 16) & 0xff);
        header[31] = (byte) (((bitrate / 8) >> 24) & 0xff);
        header[32] = (byte) ((channel * format) / 8);
        header[33] = 0;
        header[34] = 16;
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (data.length & 0xff);
        header[41] = (byte) ((data.length >> 8) & 0xff);
        header[42] = (byte) ((data.length >> 16) & 0xff);
        header[43] = (byte) ((data.length >> 24) & 0xff);

        try {
            raf.write(header, 0, 44);
            raf.write(data);
            raf.close();
        } catch (IOException e) {
            Log.e(TAG, "Error writing data to wav file");
            e.printStackTrace();
        }

        Log.e(TAG, "wrote Wav File");
    }

    /**
     * Writes a packet of audio.
     * @param data audio data
     * @param offset the offset from which to start reading the data.
     * @param len the length of data to read.
     * @exception IOException
     */
    public void writePacket(final byte[] data,
                            final int offset,
                            final int len)
            throws IOException
    {
        raf.write(data, offset, len);
    }
}