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

import java.io.IOException;
import java.io.OutputStream;

import com.ibm.cio.watsonsdk.SpeechRecorderDelegate;

// TODO: Auto-generated Javadoc
/**
 * Encoder interface.
 */
public interface VaniEncoder {
    // Use PROPRIETARY notice if class contains a main() method, otherwise use
    // COPYRIGHT notice.
    public static final String COPYRIGHT_NOTICE = "(c) Copyright IBM Corp. 2013";
    /**
     * In compression mode, construct an encoder and write (SPX) header code.
     * In non-compression mode, construct an output stream.
     *
     * @param out the OutputStream
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void initEncodeAndWriteHeader(OutputStream out) throws IOException ;
    /**
     * Init encoder with the websocket client
     * @param client
     * @throws IOException
     */
    public void initEncoderWithWebSocketClient(ChuckWebSocketUploader client) throws IOException;


    /**
     * In compression mode, encode raw audio data to SPX audio.
     *
     * @param b audio data will be written
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public byte[] encode(byte[] b);
    public void writeChunk(byte[] b) throws IOException ;
    /**
     * In compression mode, encode audio data (to SPX) before write to ouput stream.
     * In non-compression mode, write directly raw audio data to ouput stream.
     *
     * @param b audio data will be written
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public int encodeAndWrite(byte[] b) throws IOException ;
    /**
     * Get compression audio time in compression mode.
     * @return the time for compression audio.
     */
    public long getCompressionTime();
    /**
     * Close output stream.
     */
    void close();
    /**
     * Set recorder delegate
     * @param obj
     */
    void setDelegate(SpeechRecorderDelegate obj);
}
