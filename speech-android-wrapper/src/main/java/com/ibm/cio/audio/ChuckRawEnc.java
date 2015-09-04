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
 * Non-encode.
 */
public class ChuckRawEnc implements SpeechEncoder {
    // Use PROPRIETARY notice if class contains a main() method, otherwise use
    // COPYRIGHT notice.
    public static final String COPYRIGHT_NOTICE = "(c) Copyright IBM Corp. 2013";
    /** The Constant TAG. */
    private static final String TAG = ChuckRawEnc.class.getName();
    /** Output stream */
    private OutputStream out;
    private ChuckRawWriter writer = null;

    private SpeechRecorderDelegate delegate = null;
    /**
     * Constructor.
     */
    public ChuckRawEnc() {
    }

    /* (non-Javadoc)
     * @see com.ibm.cio.audio.SpeechEncoder#initEncodeAndWriteHeader(java.io.OutputStream)
     */
    public void initEncodeAndWriteHeader(OutputStream out){}
    /**
     * For WebsocketClient
     * @param client
     * @throws java.io.IOException
     */
    public void initEncoderWithWebSocketClient(ChuckWebSocketUploader client) throws IOException{
//		this.client = client;
        writer = new ChuckRawWriter(client);
//		writer.writeHeader("");

    }

    @Override
    public void onStart() {}

    /* (non-Javadoc)
     * @see com.ibm.cio.audio.SpeechEncoder#encodeAndWrite(byte[])
     */
    @Override
    public int encodeAndWrite(byte[] b) throws IOException {

        writer.writePacket(b, 0, b.length);
        if(this.delegate != null)
            this.delegate.onRecordingCompleted(b);
        return b.length;
    }
    /* (non-Javadoc)
     * @see com.ibm.cio.audio.SpeechEncoder#close()
     */
    public void close() {
        if(this.writer != null){
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public long getCompressionTime() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public byte[] encode(byte[] b) {
        // TODO Auto-generated method stub
        return b;
    }

    @Override
    public void writeChunk(byte[] b) throws IOException {
        // TODO Auto-generated method stub
        out.write(b);
    }

    @Override
    public void setDelegate(SpeechRecorderDelegate obj) {
        // TODO Auto-generated method stub
        this.delegate = obj;
    }

    protected void _onRecordingCompleted(byte[] rawAudioData){
        if(this.delegate != null) delegate.onRecordingCompleted(rawAudioData);
    }
}
