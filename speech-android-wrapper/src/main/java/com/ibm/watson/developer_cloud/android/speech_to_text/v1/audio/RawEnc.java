/**
 * © Copyright IBM Corporation 2015
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
