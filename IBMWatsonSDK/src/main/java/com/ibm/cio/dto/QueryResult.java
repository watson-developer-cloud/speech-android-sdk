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
package com.ibm.cio.dto;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ibm.cio.util.Logger;

// TODO: Auto-generated Javadoc
/**
 * Result of query.
 *
 * @author chienlk
 */
public class QueryResult {
    // Use PROPRIETARY notice if class contains a main() method, otherwise use
    // COPYRIGHT notice.
    public static final String COPYRIGHT_NOTICE = "(c) Copyright IBM Corp. 2013";
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

    /** List faces/answers. */
    private String listFaces;

    /** The job id. */
    private String jobId = "";

    /** Audio data of "I found...". */
    private byte[] ttsIFound;

    /** Duration of speech to text. */
    private String s2tTime = "0";

    /** Duration of text to speech. */
    private String ttsTime = "0";

    /** Duration of query faces/answers. */
    private String qryFaceTime = "0";

    //// NEW FACTORS
    /** Time duration for completing uploading audio data (ms). */
    private String requestTime;
    /** Duration of query faces/answers. */
    private String responseTime;
    /** Size of response data (byte). */
    private int responseLength = 0;
    /** Size of TTS response (byte). */
    private int responseTTSLength = 0;
    /** [SERVER] Size of request from Client side (byte). */
    private String requestLength = "0";
    /** [SERVER] Time duration of processing all the queries from VBE, e.g. iTrans/S2T/T2S/FacesAPI or AnwsersAPI (ms). */
    private String processingVBETime = "0";

    /** The status code. */
    private int statusCode = STATUS_OK; // OK status

    private String statusMessage = STATUS_OK_MESSAGE;  // Status message

    private byte[] rawResult = null;

    /**
     * Instantiates a new query result.
     *
     * @param transcript the transcript
     * @param listFaces the list faces/answers
     * @param ttsIFound the audio data of "I found..."
     * @param jobId the job id
     * @param s2tTime the speech to text time
     * @param ttsTIme the text to speech time
     * @param qryFaceTime the query faces/answers time
     */
    public QueryResult(String transcript, String listFaces, byte[] ttsIFound,
                       String jobId,String s2tTime, String ttsTIme, String qryFaceTime,
                       String requestLength, String processingVBETime, int responseLength) {
        super();
        this.transcript = transcript;
        this.statusCode = STATUS_OK;
        this.statusMessage = STATUS_OK_MESSAGE;
        this.listFaces = listFaces;
        this.ttsIFound = ttsIFound;
        this.jobId = jobId;
        this.s2tTime = s2tTime;
        this.ttsTime = ttsTIme;
        this.qryFaceTime = qryFaceTime;
        this.responseLength = responseLength;
        this.requestLength = requestLength;
        this.processingVBETime = processingVBETime;
        this.responseTTSLength = ttsIFound.length;
        this.rawResult = null;
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

    /**
     * Gets the list faces/answers.
     *
     * @return the list faces
     */
    public String getListFaces() {
        return listFaces;
    }

    /**
     * Sets the list faces/answers.
     */
    public void setListFaces(String val) {
        this.listFaces = val;
    }

    /**
     * Gets the tts i found.
     *
     * @return the tts i found
     */
    public byte[] getTtsIFound() {
        return ttsIFound;
    }
    /**
     * Gets the tts i found.
     *
     * @return the tts i found
     */
    public void setTtsIFound(byte[] ttsData) {
        this.ttsIFound = ttsData;
        this.responseTTSLength = ttsData.length;
    }
    /**
     * Gets the job id.
     *
     * @return the job id
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * Sets the job id.
     */
    public void setJobId(String val) {
        this.jobId = val;
    }


    /**
     * Gets the speech to text time.
     *
     * @return the s2t time
     */
    public String getS2tTime() {
        return s2tTime;
    }

    /**
     * Sets the speech to text time.
     *
     * @return the s2t time
     */
    public void setS2tTime(String val) {
        this.s2tTime = val;
    }

    /**
     * Gets the text to speech time.
     *
     * @return the tts time
     */
    public String getTtsTime() {
        return ttsTime;
    }

    /**
     * Sets the text to speech time.
     */
    public void setTtsTime(String val) {
        this.ttsTime = val;
    }

    /**
     * Gets the query faces/answers time.
     *
     * @return the query faces/answers time
     */
    public String getQryFaceTime() {
        return qryFaceTime;
    }

    /**
     * Sets the query faces/answers time.
     *
     * @return the query faces/answers time
     */
    public void setQryFaceTime(String val) {
        this.qryFaceTime = val;
    }

    public String getRequestTime() {
        return requestTime;
    }

    public String getResponseTime() {
        return responseTime;
    }

    public int getResponseLength() { return responseLength; }

    public void setResponseLength(int length) { responseLength = length; }

    public int getResponseTTSLength() {
        return responseTTSLength;
    }

    public String getRequestLength() {
        return requestLength;
    }

    public void setRequestLength(String val) {
        this.requestLength = val;
    }

    public String getProcessingVBETime() {
        return processingVBETime;
    }

    public void setProcessingVBETime(String val) {
        this.processingVBETime = val;
    }

    public byte[] getRawResult(){
        return this.rawResult;
    }

    public void setRawResult(byte[] rawData){
        this.rawResult = rawData;
    }

    /**
     * Extract result from all data.
     *
     * @param bytes all bytes data
     * @param headerLen the header length
     * @param len the data length
     * @param index the index of content
     * @return data of result (byte[])
     */
    private static byte[] getAsBytes(byte[] bytes, int headerLen, int[] len, int index) {
        int from = headerLen;
        for (int i=0; i < index; i ++) {
            from += len[i];
        }

        byte[] tmp = new byte[len[index]];
        System.arraycopy(bytes, from, tmp, 0, len[index]);

        return tmp;
    }
    /**
     * Only prepare the raw result data,
     * this is mainly used for the VANI API Integration
     *
     * @param bytes
     * @return QueryResult
     */
    public static QueryResult initWithRawData(byte[] bytes){
        QueryResult result = QueryResult.createSimpleResult("");
        result.rawResult = bytes;
        return result;
    }
    /**
     * Create a QueryResult instance from transcription
     *
     * @param transcript
     * @return
     */
    public static QueryResult createSimpleResult(String transcript){
        QueryResult result = new QueryResult(transcript, "[]", new byte[]{}, "","","","","","", transcript.length());
        return result;
    }

    public void analyzeRawData(){
        if (this.rawResult != null && this.rawResult.length > 0){
            Logger.i(TAG, "================bytes======" + this.rawResult.length);
            int unit = 3, n = 9;
            if (this.rawResult.length < unit*n) {
                return;
            }

            String isoString = new String(this.rawResult);
            Logger.e(TAG, "RESPONSE STRING: ");
            Logger.e(TAG, isoString);

            int headerLen = unit*n;
            int[] len = new int[n];

            for (int i=0; i < n; i++) {
                len[i] = getHeaderLen(this.rawResult, unit*i);
            }
            String transcript = new String(getAsBytes(this.rawResult, headerLen, len, 0));
            Logger.i(TAG, "================transcript======" + transcript);

            String list = new String(getAsBytes(this.rawResult, headerLen, len, 1));
            Logger.i(TAG, "=================list=====" + list);

            byte[] tts = getAsBytes(this.rawResult, headerLen, len, 2);
            Logger.i(TAG, "=================TTS len=====" + tts.length);

            String jobId = new String(getAsBytes(this.rawResult, headerLen, len, 3));
            Logger.i(TAG, "=================jobId =====" + jobId);

            String s2tTime = new String(getAsBytes(this.rawResult, headerLen, len, 4));
            Logger.i(TAG, "=================s2tTime =====" + s2tTime);

            String ttsTime = new String(getAsBytes(this.rawResult, headerLen, len, 5));
            Logger.i(TAG, "=================ttsTime =====" + ttsTime);

            String qryFaceTime = new String(getAsBytes(this.rawResult, headerLen, len, 6));
            Logger.i(TAG, "=================qryFaceTime =====" + qryFaceTime);

            String requestLength = new String(getAsBytes(this.rawResult, headerLen, len, 7));
            Logger.i(TAG, "=================requestLength =====" + requestLength);
            String processingVBETime = new String(getAsBytes(this.rawResult, headerLen, len, 8));
            Logger.i(TAG, "=================processingVBETime =====" + processingVBETime);

            this.setTranscript(transcript);
            this.setListFaces(list);
            this.setTtsIFound(tts);
            this.setJobId(jobId);
            this.setS2tTime(s2tTime);
            this.setTtsTime(ttsTime);
            this.setQryFaceTime(qryFaceTime);
            this.setRequestLength(requestLength);
            this.setProcessingVBETime(processingVBETime);
            this.setResponseLength(this.rawResult.length);
            if(transcript.equals("")){
                this.setStatusCode(EMPTY_TRANSCRIPTION_ERROR);
                this.setStatusMessage(EMPTY_TRANSCRIPTION_ERROR_MESSAGE);
            }
            else{
                this.setStatusCode(STATUS_OK);
                this.setStatusMessage(STATUS_OK_MESSAGE);
            }
        }
        else{
            this.setStatusCode(QueryResult.UNKNOWN_ERROR);
            this.setStatusMessage(QueryResult.UNKNOWN_ERROR_MESSAGE);
        }
    }

    /**
     * Extract all fields from byte[] data and then create {@link QueryResult}.
     *
     * @param bytes the byte[] data from input stream - response of back end service
     * @return the query result
     * {@link QueryResult}
     */
    public static QueryResult create(byte[] bytes) {
        QueryResult result = new QueryResult("", "[]", new byte[]{}, "","0","0","0","0","0", 0);
        if(bytes != null){
            result.setRawResult(bytes);
            result.analyzeRawData();
        }
        return result;
    }
    /**
     * Creates a JSONObject from error result.
     * @return JSONObject contains status code, error message and job id
     */
    public JSONObject toFailureJson() {
        JSONObject jObj = new JSONObject();
        try {
            jObj.put("code", this.statusCode);
            jObj.put("status", this.statusMessage);
            jObj.put("text", this.transcript);
            jObj.put("jobId", this.jobId);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return jObj;
    }
    /**
     * Creates a JSONObject from success result.
     * @return JSONObject contains all response informations
     */
    public JSONObject toSuccessJson() {
        JSONObject jObj = new JSONObject();
//		jObj.toString();
        try {
            jObj.put("code", this.statusCode);
            jObj.put("status", this.statusMessage);
            jObj.put("text", this.transcript);
            jObj.put("jobId", this.jobId);
            JSONArray jArrSearchResult = new JSONArray(this.listFaces);
            jObj.put("listFaces", jArrSearchResult);
            jObj.put("s2tTime", this.s2tTime);
            jObj.put("ttsTime", this.ttsTime);
            jObj.put("qryfaceTime", this.qryFaceTime);
            // NEW FACTORS
            jObj.put("responseTTSLength", this.ttsIFound.length);
            jObj.put("responseLength", this.responseLength);
            jObj.put("requestLength", this.requestLength);
            jObj.put("processingVBETime", this.processingVBETime);

//            Logger.d(TAG, "[perfLogger] responseTTSLength " + this.ttsIFound.length + "| responseLength: " + this.responseLength);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return jObj;
    }
    /**
     * Gets the header length of each result part.
     *
     * @param bytes the byte[] data from input stream - response of back end service
     * @param start the starting index
     * @return the header length
     */
    private static int getHeaderLen(byte[] bytes, int start) {
        int  MAX = 100;
        int result = bytes[start] * MAX*MAX + bytes[start+1] * MAX + bytes[start+2];
        return result;
    }
}
