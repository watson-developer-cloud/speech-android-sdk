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

import com.ibm.watson.developer_cloud.android.speech_to_text.v1.ISpeechDelegate;

/**
 * The uploader interface.
 */
public interface IChunkUploader {
    // Use PROPRIETARY notice if class contains a main() method, otherwise use COPYRIGHT notice.
    public static final String COPYRIGHT_NOTICE = "(c) Copyright IBM Corp. 2015";
    /**
     * On has data.
     *
     * @param buffer the buffer
     */
    int onHasData(byte[] buffer);
    /**
     * Checks if uploader has been prepared.
     *
     * @return true, if uploader is prepared
     */
    boolean isUploadPrepared();
    /**
     * Upload data
     * @param data
     */
    void upload(byte[] data);
    /**
     * Stop uploading
     */
    void stop();
    /**
     * Start thread to construct an upload http connection to back end server.
     */
    void prepare();

    /**
     * Set Delegate
     *
     * @param delegate
     */
    void setDelegate(ISpeechDelegate delegate);
    /**
     * Close connection.
     */
    void close();
}
