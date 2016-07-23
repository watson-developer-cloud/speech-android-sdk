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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.NotYetConnectedException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.ibm.watson.developer_cloud.android.speech_to_text.v1.dto.SpeechConfiguration;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.ISpeechDelegate;

public class WebSocketUploader extends WebSocketClient implements IChunkUploader {
    // Use PROPRIETARY notice if class contains a main() method, otherwise use COPYRIGHT notice.
    public static final String COPYRIGHT_NOTICE = "(c) Copyright IBM Corp. 2015";
    private static final String TAG = WebSocketUploader.class.getName();

    private ISpeechEncoder encoder = null;
    private Thread initStreamToServerThread;

    private boolean uploadPrepared = false;

    /** STT delegate */
    private ISpeechDelegate delegate = null;
    /** Recorder delegate */
    private SpeechConfiguration sConfig = null;

    /**
     * Create an uploader which supports streaming.
     *
     * @param serverURL LMC server, delivery to back end server
     * @throws URISyntaxException
     */
    public WebSocketUploader(String serverURL, Map<String, String> header, SpeechConfiguration config) throws URISyntaxException {
        super(new URI(serverURL), new Draft_17(), header, config.connectionTimeout);
        Log.d(TAG, "New WebSocketUploader: " + serverURL);
        Log.d(TAG, serverURL);
        this.sConfig = config;

        if(sConfig.audioFormat.equals(SpeechConfiguration.AUDIO_FORMAT_DEFAULT)) {
            this.encoder = new RawEnc();
        }
        else if(sConfig.audioFormat.equals(SpeechConfiguration.AUDIO_FORMAT_OGGOPUS)){
            this.encoder = new OggOpusEnc();
        }

        if(serverURL.toLowerCase().startsWith("wss") || serverURL.toLowerCase().startsWith("https"))
            this.sConfig.isSSL = true;
        else this.sConfig.isSSL = false;
    }
    /**
     * Trust server
     *
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     */
    private void trustServer() throws KeyManagementException, NoSuchAlgorithmException, IOException {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] certs = new TrustManager[]{ new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[]{};
            }
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
        }};
        SSLContext sslContext = null;
        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, certs, new java.security.SecureRandom());
        SSLSocketFactory factory = sslContext.getSocketFactory();
        this.setSocket(factory.createSocket());
    }
    /**
     * 1. Initialize WebSocket connection </br>
     * 2. Init an encoder and writer
     *
     * @throws Exception
     */
    private void initStreamAudioToServer() throws Exception {
        Log.d(TAG, "Connecting...");
        //lifted up for initializing writer, using isRunning to control the flow
        this.encoder.initEncoderWithUploader(this);

        if(this.sConfig.isSSL)
            this.trustServer();

        boolean rc;
        rc = this.connectBlocking();

        if (rc) {
            Log.d(TAG, "Connected");
            this.sendSpeechHeader();
        }
        else{
            Log.e(TAG, "Connection failed!");
            this.uploadPrepared = false;
            throw new Exception("Connection failed!");
        }

    }

    @Override
    public int onHasData(byte[] buffer) {
        int uploadedAudioSize = 0;
        // NOW, WE HAVE STATUS OF UPLOAD PREPARING, UPLOAD PREPARING OK
        if (this.isUploadPrepared()) {
            try {
                uploadedAudioSize = encoder.encodeAndWrite(buffer);
                Log.d(TAG, "onHasData: " + uploadedAudioSize + " " + buffer.length);
                // TODO: Capturing data
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                Log.w(TAG, "waiting for establishing the connection");
                initStreamToServerThread.join();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return uploadedAudioSize;
    }

    @Override
    public boolean isUploadPrepared() {
        return this.uploadPrepared;
    }

    public void stopUploaderPrepareThread() {
        if (initStreamToServerThread != null) {
            initStreamToServerThread.interrupt();
        }
    }
    /**
     * Prepare connection
     */
    @Override
    public void prepare() {
        this.uploadPrepared = false;
        initStreamToServerThread = new Thread() {
            public void run() {
                try {
                    try {
                        initStreamAudioToServer();
                        Log.d(TAG, "WebSocket Connection established");
                    } catch (IOException e1) {
                        Log.e(TAG, "IOException: " + e1.getMessage());
                        throw e1;
                    } catch (InterruptedException e1) {
                        Log.e(TAG, "InterruptedException:" + e1.getMessage());
                        throw e1;
                    } catch (Exception e1) {
                        Log.e(TAG, "Exception: " + e1.getMessage());
                        throw e1;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "Connection failed: " + (e == null ? "null exception" : e.getMessage()));
                    uploadPrepared = false;
                    close();
                }
            };
        };
        initStreamToServerThread.setName("initStreamToServerThread");
        initStreamToServerThread.start();
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
            Log.e(TAG, ex.getLocalizedMessage());
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
            Log.e(TAG, ex.getLocalizedMessage());
        }
    }

    /**
     * Stop by sending out zero byte of data
     */
    public void stop(){
        byte[] stopData = new byte[0];
        this.upload(stopData);
    }

    @Override
    public void close() {
        Log.d(TAG, "closing the websocket");
        super.close();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.d(TAG, "WebSocket closed");
        this.uploadPrepared = false;

        Log.d(TAG, "### Code: " + code + " reason: " + reason + " remote: " + remote);
        if (delegate != null){
            delegate.onClose(code, reason, remote);
        }
    }

    @Override
    public void onError(Exception ex) {
        Log.e(TAG, "WebSocket error");
        String errorMessage = "";
        if(ex != null)
            errorMessage = ex.getMessage();
        // Send the error message to the delegate
        this.uploadPrepared = false;
        //this.sendMessage(ISpeechDelegate.ERROR);
        if (delegate != null){
            delegate.onError(errorMessage);
        }
    }

    @Override
    public void onMessage(String message) {

        Log.d(TAG + "onMessage", message);
        if (delegate != null){
            delegate.onMessage(message);
        }
    }

    @Override
    public void onOpen(ServerHandshake arg0) {
        Log.d(TAG, "WS connection opened successfully");
        this.uploadPrepared = true;
        if (delegate != null){
            delegate.onOpen();
        }
    }

    private void sendSpeechHeader() {

        JSONObject obj = new JSONObject();
        try {
            obj.put("action", "start");
            obj.put("content-type", this.sConfig.audioFormat);
            obj.put("interim_results", this.sConfig.returnInterimResults);
            obj.put("continuous", this.sConfig.continuous);
            obj.put("inactivity_timeout", this.sConfig.inactivityTimeout);

            if (this.sConfig.maxAlternatives > 1) {
                obj.put("max_alternatives", this.sConfig.maxAlternatives);
            }

            if (!(Float.isNaN(this.sConfig.wordAlternativesThreshold))) {
                obj.put("word_alternatives_threshold", this.sConfig.wordAlternativesThreshold);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String startHeader = obj.toString();
        this.upload(startHeader);
        this.encoder.onStart();
        Log.d(TAG, "Sending init message: " + startHeader);
    }

    /**
     * Set delegate
     *
     * @param delegate
     */
    public void setDelegate(ISpeechDelegate delegate) {
        this.delegate = delegate;
    }
}
