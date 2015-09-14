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

import javax.net.ssl.HttpsURLConnection;

import com.ibm.cio.dto.QueryResult;
import com.ibm.cio.watsonsdk.SpeechDelegate;

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
    public int onHasData(byte[] buffer);

    /**
     * Checks if uploader has been prepared.
     *
     * @return true, if uploader is prepared
     */
    public boolean isUploadPrepared();
    /**
     * Get upload preparing error code
     * @return error code
     */
    public int getUploadErrorCode();
    /**
     * Stop upload preparing thread.
     */
    public void stopUploaderPrepareThread();

    /**
     * Get transcript from {@link HttpsURLConnection}.
     *
     * @param timeout timeout of getting data (in ms)
     * @return {@link QueryResult}
     */
    public QueryResult getQueryResultByAudio(long timeout);

    /**
     * Start thread to construct an upload http connection to back end server.
     */
    public void prepare();
    /**
     * Set timeout value in second
     *
     * @param timeout
     */
    public void setTimeout(int timeout);

    /**
     * Set Delegate
     *
     * @param delegate
     */
    public void setDelegate(SpeechDelegate delegate);

    /**
     * Close upload http connection.
     */
    public void close();
}
