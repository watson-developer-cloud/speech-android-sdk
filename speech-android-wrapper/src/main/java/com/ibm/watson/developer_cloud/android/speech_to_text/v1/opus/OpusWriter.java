/**
 * Copyright IBM Corporation 2015
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package com.ibm.watson.developer_cloud.android.speech_to_text.v1.opus;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.xiph.speex.AudioFileWriter;
import org.xiph.speex.OggCrc;

import com.ibm.watson.developer_cloud.android.speech_to_text.v1.audio.ChuckWebSocketUploader;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.dto.SpeechConfiguration;
import com.ibm.watson.developer_cloud.android.speech_common.v1.util.Logger;

public class OpusWriter extends AudioFileWriter {
    private String TAG = this.getClass().getSimpleName();
    private ChuckWebSocketUploader client;
    /** Number of packets in an Ogg page (must be less than 255) */
    public static final int PACKETS_PER_OGG_PAGE = 50;
    /** Defines the sampling rate of the audio input. */
    protected int sampleRate;
    /** Ogg Stream Serial Number */
    protected int streamSerialNumber;
    /** Data buffer */
    private byte[] dataBuffer;
    /** Pointer within the Data buffer */
    private int dataBufferPtr;
    /** Header buffer */
    private byte[] headerBuffer;
    /** Pointer within the Header buffer */
    private int headerBufferPtr;
    /** Ogg Page count */
    protected int pageCount;
    /** Opus packet count within an Ogg Page */
    private int packetCount;
    /**
     * Absolute granule position
     * (the number of audio samples from beginning of file to end of Ogg Packet).
     */
    private long granulepos;
    /** Frame size */
    private int frameSize;

    public OpusWriter(){}

    /**
     * Setting up the OggOpus Writer
     * @param client
     */
    public OpusWriter(ChuckWebSocketUploader client){
        this.client = client;
        if (streamSerialNumber == 0)
            streamSerialNumber = new Random().nextInt();
        dataBuffer         = new byte[65565];
        dataBufferPtr      = 0;
        headerBuffer       = new byte[255];
        headerBufferPtr    = 0;
        pageCount          = 0;
        packetCount        = 0;
        granulepos         = 0;
        sampleRate         = 16000;
        this.frameSize     = SpeechConfiguration.FRAME_SIZE;
    }
    @Override
    public void close() throws IOException {
        Logger.d(TAG, "Opus Writer Closing...");
        flush(true);
        this.client.stop();
    }

    @Override
    public void open(File file) throws IOException {}

    @Override
    public void open(String filename) throws IOException {}

    @Override
    public void writeHeader(String comment) {
        Logger.d(TAG, "Opus Writer Headering...");

        byte[] header;
        byte[] data;
        int chksum;

        /* writes the OGG header page */
        header = buildOggPageHeader(2, 0, streamSerialNumber, pageCount++, 1, new byte[] {19});
        data = buildOpusHeader(sampleRate);
        chksum = OggCrc.checksum(0, header, 0, header.length);
        chksum = OggCrc.checksum(chksum, data, 0, data.length);
        writeInt(header, 22, chksum);
        this.write(header);
        this.write(data);

        /* Writes the OGG comment page */
        header = buildOggPageHeader(0, 0, streamSerialNumber, pageCount++, 1, new byte[]{(byte) (comment.length() + 8)});
        data = buildOpusComment(comment);
        chksum = OggCrc.checksum(0, header, 0, header.length);
        chksum = OggCrc.checksum(chksum, data, 0, data.length);
        writeInt(header, 22, chksum);
        this.write(header);
        this.write(data);
    }

    @Override
    public void writePacket(byte[] data, int offset, int len)
            throws IOException {
        // if nothing to write
        if (len <= 0) {
            return;
        }
        if (packetCount > PACKETS_PER_OGG_PAGE) {
            flush(false);
        }
        System.arraycopy(data, offset, dataBuffer, dataBufferPtr, len);
        dataBufferPtr += len;
        headerBuffer[headerBufferPtr++]=(byte)len;
        packetCount++;
        granulepos += this.frameSize*2;
    }

    /**
     * Flush the Ogg page out of the buffers into the file.
     * @param eos - end of stream
     * @exception IOException
     */
    protected void flush(final boolean eos) throws IOException{
        int chksum;
        byte[] header;
        /* Writes the OGG header page */
        header = buildOggPageHeader((eos ? 4 : 0), granulepos, streamSerialNumber, pageCount++, packetCount, headerBuffer);
        chksum = OggCrc.checksum(0, header, 0, header.length);
        chksum = OggCrc.checksum(chksum, dataBuffer, 0, dataBufferPtr);
        writeInt(header, 22, chksum);

        this.write(header);
        this.write(dataBuffer, 0, dataBufferPtr);

        dataBufferPtr   = 0;
        headerBufferPtr = 0;
        packetCount     = 0;
    }

    /**
     * Write data into Socket
     * @param data
     */
    public void write(byte[] data){
        this.client.upload(data);
    }

    /**
     * Write data into Socket
     * @param data
     * @param offset
     * @param count
     */
    public void write(byte[] data, int offset, int count){
        byte[] tmp = new byte[count];
        System.arraycopy(data, offset, tmp, 0, count);

        this.client.upload(tmp);
    }
}
