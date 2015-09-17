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
package com.ibm.watson.developer_cloud.android.speech_to_text.v1.audio;

import java.io.IOException;

/**
 * Raw data encoder
 */
public class RawEnc implements ISpeechEncoder {
    // Use PROPRIETARY notice if class contains a main() method, otherwise use COPYRIGHT notice.
    public static final String COPYRIGHT_NOTICE = "(c) Copyright IBM Corp. 2015";
    /** Data writer */
    private RawWriter writer = null;
    /**
     * Constructor.
     */
    public RawEnc() {}
    /**
     * For WebSocketClient
     * @param uploader
     * @throws java.io.IOException
     */
    public void initEncoderWithUploader(IChunkUploader uploader) throws IOException{
        this.writer = new RawWriter(uploader);
    }
    /**
     * On encode begin
     */
    @Override
    public void onStart() {}
    /* (non-Javadoc)
     * @see com.ibm.watson.developer_cloud.android.speech_to_text.v1.audio.SpeechEncoder#encodeAndWrite(byte[])
     */
    @Override
    public int encodeAndWrite(byte[] b) throws IOException {
        writer.writePacket(b, 0, b.length);
        return b.length;
    }
    /* (non-Javadoc)
     * @see com.ibm.watson.developer_cloud.android.speech_to_text.v1.audio.SpeechEncoder#close()
     */
    public void close() {
        if(this.writer != null){
            try {
                this.writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
