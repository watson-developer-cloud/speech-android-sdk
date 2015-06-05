package com.ibm.cio.audio;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.NotYetConnectedException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.Future;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.java_websocket.client.DefaultSSLWebSocketClientFactory;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.SystemClock;
import android.util.Log;

import com.ibm.cio.dto.QueryResult;
import com.ibm.cio.util.Logger;
import com.ibm.cio.watsonsdk.SpeechDelegate;

public class ChuckWebSocketUploader extends WebSocketClient implements VaniUploader{
    private static final String TAG = ChuckWebSocketUploader.class.getName();

    private VaniEncoder encoder = null;
    //	private String serverURL = "";
//	private String lmcCookie = "";
    private String transcript = "";
    private Thread initStreamToServerThread;

    private boolean uploadPrepared = false;
    private int uploadErrorCode = 0;
    private long beginRequestTime = 0;
    private long requestEstablishingTime = 0;
    private long requestTime = 0;
    private long beginGetResponse = 0;
    private long responseTime = 0;
    private long dataTransmissionTime = 0;
    private long beginSendRequest = 0;
    private long timeout = 0;

    private SpeechDelegate delegate = null;
    private Future<QueryResult> future = null;

    /**
     * Create an uploader which supports streaming.
     *
     * @param encoder the encoder
     * @param serverURL LMC server, delivery to back end server
     * @throws URISyntaxException
     */
    public ChuckWebSocketUploader(VaniEncoder encoder, String serverURL, Map<String, String> header) throws URISyntaxException {
        super(new URI(serverURL), new Draft_17(), header);
//		super( new URI(serverURL), new Draft_17());
        Logger.i(TAG, "### New ChuckWebSocketUploader ###");
        Logger.d(TAG, serverURL);
        this.encoder = encoder; // for WebSocket only
    }
    /**
     * Trust server
     *
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     */
    private void trustServer() throws KeyManagementException, NoSuchAlgorithmException {
        Logger.d(TAG, "Trusting server");
        // Create a trust manager that does not validate certificate chains  
        TrustManager[] certs = new TrustManager[]{ new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[]{};
            }
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException{ }
        }};
        SSLContext sslContext = null;
        sslContext = SSLContext.getInstance( "TLS" );
        sslContext.init(null, certs, new java.security.SecureRandom());
        this.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(sslContext));
    }
    /**
     * 1. Initialize websocket connection to chuck </br>
     * 2. Init an encoder and writer
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws InterruptedException
     * @throws Exception
     */
    private void initStreamAudioToServer() throws IOException, InterruptedException, Exception {
        Logger.i(TAG, "********** Connecting... **********");
        beginRequestTime = SystemClock.elapsedRealtime();
        Logger.i(TAG, "prepareUploader, initStreamAudioToServer begin at: " + beginRequestTime);
        this.encoder.initEncoderWithWebSocketClient(this);//lifted up for initializing writer, using isRunning to control the flow

        this.trustServer();

        boolean rc = false;
        rc = this.connectBlocking();

        if(!rc){
            Logger.e(TAG, "********** Connection failed! **********");
            this.uploadPrepared = false;
            throw new Exception("Connection failed.");
        }
        Logger.i(TAG, "********** Connected **********");
    }
    /**
     * Send message to the delegate
     *
     * @param code
     * @param message
     */
    private void sendMessage(int code){
        if(delegate != null){
            delegate.receivedMessage(code, this.fetchTranscript(this.timeout));
        }
    }
    @Override
    public int onHasData(byte[] buffer, boolean needEncode) {
        int uploadedAudioSize = 0;
        // NOW, WE HAVE STATUS OF UPLOAD PREPARING, UPLOAD PREPARING OK
        if (this.isUploadPrepared()) {
            try {
                if (needEncode) {
                    uploadedAudioSize = encoder.encodeAndWrite(buffer);
                }
                else{
                    encoder.writeChunk(buffer);
                }
            } catch (IOException e) {
                Logger.e(TAG, "Error occured in writeBufferToOutputStream, recording is aborted");
                e.printStackTrace();
            }
        }
        else {
            try {
                System.out.print("=");
                initStreamToServerThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Logger.d(TAG, "Finish Join prepare upload, result = " + this.isUploadPrepared());
            }
        }
        return uploadedAudioSize;
    }

    @Override
    public boolean isUploadPrepared() {
        return this.uploadPrepared;
    }

    @Override
    public int getUploadErrorCode() {
        return this.uploadErrorCode;
    }

    @Override
    public void stopUploaderPrepareThread() {
        if (initStreamToServerThread != null) {
            Logger.i(TAG, "stopUploaderPrepareThread");
            initStreamToServerThread.interrupt();
        }
    }

    @Override
    public QueryResult getQueryResultByAudio(long t) {
        t = this.timeout;
        if(timeout > 0){
            try {
                new Thread(){
                    public void run(){
                        try {
                            Logger.e(TAG, "Wait for "+timeout+" ms...");
                            Thread.sleep(timeout);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        finally{
                            Logger.e(TAG, "Then close...");
                            encoder.close();
                        }
                    }
                }.start();
            }
            catch (Exception e) {
                // TODO: handle exception
                Logger.e(TAG, "encoder close FAIL");
                e.printStackTrace();
            }
        }
        else{
            encoder.close();
        }
        return fetchTranscript(t);
    }

    /**
     * Fetch transcript from {@link HttpsURLConnection}.
     *
     * @param timeout timeout of getting {@link QueryResult} (in ms)
     * @return the query result
     * {@link QueryResult} </br>
     * null if {@link IOException}
     */
    private QueryResult fetchTranscript(long timeout) {
        return QueryResult.createSimpleResult(this.getTranscript());
    }

    @Override
    public boolean stopGetQueryResultByAudio() {
        Logger.i(TAG, "stopGetQueryResultByAudio");
        if (future != null)
            return future.cancel(true);
        return false;
    }

    @Override
    public void prepare() {
        this.uploadPrepared = false;
        initStreamToServerThread = new Thread() {
            public void run() {
                try {
                    try {
                        initStreamAudioToServer();
                        Logger.i(TAG, "### WebSocket Connection established");
                    } catch (IOException e1) {
                        Logger.e(TAG, "### IOException: "+e1.getMessage());
                        // TODO Auto-generated catch block
                        throw e1;
                    } catch (InterruptedException e1) {
                        Logger.e(TAG, "### InterruptedException:"+e1.getMessage());
                        // TODO Auto-generated catch block
                        throw e1;
                    } catch (Exception e1) {
                        Logger.e(TAG, "### Exception: "+e1.getMessage());
                        // TODO Auto-generated catch block
                        throw e1;
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    Logger.e(TAG, "Connection failed: " + (e == null ? "NULL EXCEPTION" : e.getMessage()));
                    if (e.getMessage() == null || e.getMessage().contains("Connection closed by peer") ||  e.getMessage().contains("reset by peer") || e.getMessage().contains("Connection failed")) {
                        uploadErrorCode = -1;
                    }
                    e.printStackTrace();
                    uploadPrepared = false;
                    close();
                }
            };
        };
        initStreamToServerThread.setName("initStreamToServerThread");
        initStreamToServerThread.start();
    }

    @Override
    public long getRequestTime() {
        return this.requestTime;
    }

    @Override
    public long getResponseTime() {
        return this.responseTime;
    }

    @Override
    public long getDataTransmissionTime() {
        return this.dataTransmissionTime;
    }

    @Override
    public long getRequestEstablishingTime() {
        return this.requestEstablishingTime;
    }

    /**
     * Get transcription
     *
     * @return String
     */
    public String getTranscript(){
        return this.transcript == null ? "" : this.transcript;
    }

    /**
     * Write string into socket
     *
     * @param message
     */
    public void upload(String message){
        try{
            this.send(message);
        }
        catch(NotYetConnectedException ex){
            this.transcript = ex.getLocalizedMessage();
            // Send the error message to the delegate
//	    	this.sendMessage(SpeechDelegate.ERROR);
        }
    }

    /**
     * Write data into socket
     *
     * @param data
     */
    public void upload(byte[] data){
        try{
            this.send(data);
        }
        catch(NotYetConnectedException ex){
            this.transcript = ex.getLocalizedMessage();
            // Send the error message to the delegate
//	    	this.sendMessage(SpeechDelegate.ERROR);
        }
    }

    public void stop(){
        Logger.w(TAG, "Sending 0 byte to stop the recognition...");
        byte[] stopData = new byte[0];
        this.upload(stopData); // Close streaming
    }

    @Override
    public void close() {
        this.stop();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Logger.i(TAG, "********** Closed **********");

        // Send the last part of messages to the delegate
        this.sendMessage(SpeechDelegate.CLOSE);

        beginGetResponse = SystemClock.elapsedRealtime();
        dataTransmissionTime = beginGetResponse - beginSendRequest;
        requestTime = beginGetResponse - beginRequestTime;
        this.uploadPrepared = false;
        Logger.d(TAG, "### Response Time: " + responseTime);
    }

    @Override
    public void onError(Exception ex) {
        Logger.e(TAG, "********** Error **********");
        Logger.e(TAG, "Error:"+ex.getMessage());
        this.transcript = ex.getLocalizedMessage();
        // Send the error message to the delegate
        this.uploadPrepared = false;
        this.sendMessage(SpeechDelegate.ERROR);
    }
    /**
     *	0: Final transcription
     *	1: Partial transcription
     *	2: Stable transcription
     */
    @Override
    public void onMessage(String message) {
        Log.d(TAG + ":onMessage", message);
        String parsedData = parseMessage(message);
        // Send instant message to the delegate
        if (parsedData.equals("JSONerror")) {
            Log.d(TAG, "Has JSON error so not sending the result");
        }
        else if(parsedData.equals("")){
            Log.d(TAG, "Empty transcription, ignoring...");
        }
        else{
            this.transcript = parsedData;
            this.sendMessage(SpeechDelegate.MESSAGE);
        }
    }

    @Override
    public void onOpen(ServerHandshake arg0) {
        Logger.i(TAG, "********** WS connection opened Successfully **********");
        this.uploadPrepared = true;
        beginSendRequest = SystemClock.elapsedRealtime();
        requestEstablishingTime = (beginSendRequest - beginRequestTime);
        Logger.i(TAG, "requestEstablishingTime: " + requestEstablishingTime);

//      this.transcript = this.fetchTranscript(this.timeout).getTranscript();
//		String json = "{\"grammar\":null,\"name\":\"listen\",\"dictionary\":null,\"wantResults\":[\"topHypothesis\",\"splitTopHypothesis\"],\"content-type\":\"audio/l16; rate=16000\",\"wantInterimResults\":true,\"EOUMode\":\"PTT\"}";
        String json = "{\"action\":\"start\",\"content-type\":\"audio/l16; rate=16000\",\"interim_results\":true,\"continuous\": true}";
        upload(json);

        this.sendMessage(SpeechDelegate.OPEN);
    }

    /**
     * Parse the JSON from the WS connection
     * @param text
     * @return
     */
    String parseMessage(String data){
        String result="";

        try {
            JSONObject jObj = new JSONObject(data);

            if(jObj.has("state")){
                //if has status
                Log.d(TAG, "Found JSON status: "+ jObj.getString("state"));
//				result="state: "+jObj.getString("state");
//                result="";

            }else if(jObj.has("results")){
                //if has result
                Log.d(TAG, "Found JSON results ");

                JSONArray jArr = jObj.getJSONArray("results");
                for (int i=0; i < jArr.length(); i++) {
                    JSONObject obj = jArr.getJSONObject(i);
                    //check if final
                    if(obj.getString("final").equals("true")){
                        //get transcript
                        JSONArray jArr1 = obj.getJSONArray("alternatives");
                        for (int j=0; j < jArr1.length(); j++) {
                            JSONObject obj1 = jArr1.getJSONObject(j);
                            result=obj1.getString("transcript");
                        }
                        //close connection
                        super.close();
                    }else{
                        //get transcript
                        JSONArray jArr1 = obj.getJSONArray("alternatives");
                        for (int j=0; j < jArr1.length(); j++) {
                            JSONObject obj1 = jArr1.getJSONObject(j);
                            result=obj1.getString("transcript");
                        }
                    }
                }

            }else if(jObj.has("name")){
                result="JSONerror";

            }else{
                result="Unexpected response from the server. Cannot parse JSON : "+"\n"+data;
            }

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON");
            result="JSONerror";
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Set timeout
     *
     * @param int
     */
    @Override
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Set delegate
     *
     * @param delegate
     */
    public void setDelegate(SpeechDelegate delegate) {
        this.delegate = delegate;
    }
}
