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

import android.os.SystemClock;

import com.ibm.cio.dto.QueryResult;
import com.ibm.cio.util.Logger;
import com.ibm.cio.watsonsdk.SpeechDelegate;

public class VaniOpusUploader extends WebSocketClient implements VaniUploader{
    private static final String TAG = VaniOpusUploader.class.getName();

    private VaniJNAOpusEnc encoder = null;
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
    public VaniOpusUploader(VaniEncoder encoder, String serverURL, Map<String, String> header) throws URISyntaxException {
        super( new URI(serverURL), new Draft_17(), header);
        Logger.i(TAG, "### New VaniOpusUploader ###");
        Logger.d(TAG, serverURL);
        this.encoder = (VaniJNAOpusEnc) encoder; // for WebSocket only
    }
    /**
     * Trust server
     *
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     */
    private void trustServer() throws KeyManagementException, NoSuchAlgorithmException {
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
        sslContext.init( null, certs, new java.security.SecureRandom() );
        this.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(sslContext));
    }
    /**
     * 1. Initialize websocket connection to iTrans </br>
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

        boolean rc;
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
     */
    private void sendMessage(int code){
        if(delegate != null)
            delegate.receivedMessage(code, this.fetchTranscript(this.timeout));
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
        this.upload("STOP"); // Close streaming
    }

    @Override
    public void close() {
        this.stop();
        super.close();
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
//		Logger.i(TAG, message);
        this.transcript = message;
        // Send instant message to the delegate
        this.sendMessage(SpeechDelegate.MESSAGE);
    }

    @Override
    public void onOpen(ServerHandshake arg0) {
        Logger.i(TAG, "********** Opened **********");
        this.uploadPrepared = true;
        beginSendRequest = SystemClock.elapsedRealtime();
        requestEstablishingTime = (beginSendRequest - beginRequestTime);
        Logger.i(TAG, "requestEstablishingTime: " + requestEstablishingTime);

        this.transcript = this.fetchTranscript(this.timeout).getTranscript();
        this.sendMessage(SpeechDelegate.OPEN);
    }

    /**
     * Set timeout
     *
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
