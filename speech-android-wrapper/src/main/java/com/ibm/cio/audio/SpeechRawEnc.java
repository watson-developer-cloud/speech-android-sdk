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
