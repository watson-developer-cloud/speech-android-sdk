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

package com.ibm.watson.developer_cloud.android.speech_to_text.v1.audio;

import java.io.IOException;
import java.io.OutputStream;

import com.ibm.watson.developer_cloud.android.speech_to_text.v1.SpeechRecorderDelegate;

/**
 * Encoder interface.
 */
public interface ISpeechEncoder {
    // Use PROPRIETARY notice if class contains a main() method, otherwise use COPYRIGHT notice.
    public static final String COPYRIGHT_NOTICE = "(c) Copyright IBM Corp. 2015";
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
    void onStart();
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
