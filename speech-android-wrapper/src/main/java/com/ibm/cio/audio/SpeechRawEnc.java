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

/**
 * Non-encode.
 */
public class SpeechRawEnc implements ISpeechEncoder {
    // Use PROPRIETARY notice if class contains a main() method, otherwise use COPYRIGHT notice.
    public static final String COPYRIGHT_NOTICE = "(c) Copyright IBM Corp. 2015";
    /** Output stream */
    private OutputStream out;
    private SpeechRecorderDelegate delegate = null;

    /**
     * Constructor.
     */
    public SpeechRawEnc() {}

    /* (non-Javadoc)
     * @see com.ibm.cio.audio.SpeechEncoder#initEncodeAndWriteHeader(java.io.OutputStream)
     */
    @Override
    public void initEncodeAndWriteHeader(OutputStream out) throws IOException {
        this.out = out;
    }

    @Override
    public void onStart() {}

    /* (non-Javadoc)
     * @see com.ibm.cio.audio.SpeechEncoder#encodeAndWrite(byte[])
     */
    @Override
    public int encodeAndWrite(byte[] rawAudioData) throws IOException {
        out.write(rawAudioData);
        this._onRecording(rawAudioData);
        return rawAudioData.length;
    }

    /* (non-Javadoc)
     * @see com.ibm.cio.audio.SpeechEncoder#close()
     */
    @Override
    public void close() {
        try {
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public byte[] encode(byte[] b) {
        return b;
    }

    @Override
    public void setDelegate(SpeechRecorderDelegate obj) {
        this.delegate = obj;
    }

    @Override
    public void initEncoderWithWebSocketClient(ChuckWebSocketUploader client)
            throws IOException {
    }

    private void _onRecording(byte[] rawAudioData){
        if(this.delegate != null) delegate.onRecording(rawAudioData);
    }
}
