/* ***************************************************************** */
/*                                                                   */
/* IBM Confidential                                                  */
/*                                                                   */
/* OCO Source Materials                                              */
/*                                                                   */
/* Copyright IBM Corp. 2015                                          */
/*                                                                   */
/* The source code for this program is not published or otherwise    */
/* divested of its trade secrets, irrespective of what has been      */
/* deposited with the U.S. Copyright Office.                         */
/*                                                                   */
/* ***************************************************************** */
package com.ibm.cio.dto;

/**
 * Result of query.
 *
 * @author chienlk
 */
public class QueryResult {
    // Use PROPRIETARY notice if class contains a main() method, otherwise use COPYRIGHT notice.
    public static final String COPYRIGHT_NOTICE = "(c) Copyright IBM Corp. 2015";
    public static final int CONNECTION_FAILED = 100;
    public static final String CONNECTION_FAILED_MESSAGE = "Network is unreachable";
    public static final int TIME_OUT = 101;
    public static final String TIME_OUT_MESSAGE = "Thinking timeout";
    public static final int CANCEL_ALL = 102;
    public static final String CANCEL_ALL_MESSAGE = "Thinking cancelled";
    public static final int CONNECTION_CLOSED = 103;
    public static final String CONNECTION_CLOSED_MESSAGE = "Connection reset or closed by peer";
    public static final int STOP_SOON = 104;
    public static final String STOP_SOON_MESSAGE = "Stop too soon";

    public static final int WAIT = 105;
    public static final String WAIT_MESSAGE = "Thinking";

    public static final int BAD_REQUEST = 400;
    public static final String BAD_REQUEST_MESSAGE = "Bad request";

    public static final int AUTHENTICATION_FAILED = 401;
    public static final String AUTHENTICATION_FAILED_MESSAGE = "Authentication failed/Access denied";

    public static final int UNKNOWN_ERROR = 106;
    public static final String UNKNOWN_ERROR_MESSAGE = "IOException when read input stream";

    public static final int EMPTY_TRANSCRIPTION_ERROR = 107;
    public static final String EMPTY_TRANSCRIPTION_ERROR_MESSAGE = "Empty transcription";

    public static final int STATUS_OK = 88;
    public static final String STATUS_OK_MESSAGE = "Success";

    private static final String TAG = QueryResult.class.getName();
    /** The transcript. */
    private String transcript = "";

    /** The status code. */
    private int statusCode = STATUS_OK; // OK status

    private String statusMessage = STATUS_OK_MESSAGE;  // Status message

    /**
     * Instantiates a new query result.
     *
     * @param transcript the transcript
     */
    public QueryResult(String transcript) {
        super();
        this.transcript = transcript;
        this.statusCode = STATUS_OK;
        this.statusMessage = STATUS_OK_MESSAGE;
    }

    /**
     * Instantiates a new query result.
     *
     * @param status the status code
     */
    public QueryResult (int status, String message) {
        super();
        this.setStatusCode(status);
        this.setStatusMessage(message);
    }

    /**
     * Gets the status code of query.
     *
     * @return the status code of query
     */
    public int getStatusCode() {
        return this.statusCode;
    }

    /**
     * Sets the status code of query.
     *
     * @param statusCode the new status code
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Gets the status message of query.
     *
     * @return the status message of query
     */
    public String getStatusMessage() {
        return this.statusMessage;
    }

    /**
     * Sets the status code of query.
     *
     * @param message
     */
    public void setStatusMessage(String message) {
        this.statusMessage = message;
    }

    /**
     * Gets the transcript.
     *
     * @return the transcript
     */
    public String getTranscript() {
        return transcript == null ? "" : transcript;
    }

    /**
     * Gets the transcript.
     *
     * @return the transcript
     */
    public void setTranscript(String val) {
        this.transcript = val;
    }

}
