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
