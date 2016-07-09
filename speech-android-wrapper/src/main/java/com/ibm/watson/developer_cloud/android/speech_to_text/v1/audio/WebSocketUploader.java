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
import java.util.LinkedList;
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

import com.ibm.watson.developer_cloud.android.speech_to_text.v1.dto.STTConfiguration;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.ISpeechToTextDelegate;

public class WebSocketUploader extends WebSocketClient implements IChunkUploader {
    private class WebSocketAudio {
        public static final int STREAM_MARKER_DATA = 1;
        public static final int STREAM_MARKER_END = 2;
        byte[] buffer;
        int marker;
        public WebSocketAudio(byte[] buffer, int isMarker){
            this.marker = isMarker;
            this.buffer = buffer;
        }
    }
    // Use PROPRIETARY notice if class contains a main() method, otherwise use COPYRIGHT notice.
    public static final String COPYRIGHT_NOTICE = "(c) Copyright IBM Corp. 2016";
    private static final String TAG = WebSocketUploader.class.getSimpleName();
    /** Encoder */
    private ISpeechEncoder encoder = null;
    /** STT delegate */
    private ISpeechToTextDelegate delegate = null;
    /** Recorder delegate */
    private STTConfiguration sConfig = null;
    /** Audio buffer */
    private LinkedList<WebSocketAudio> audioBuffer = null;
    /** If the listening state ready for Audio  */
    private boolean isReadyForAudio = false;
    /** If the listening state ready for connection closure */
    private boolean isReadyForClosure = false;
    /** Connection status */
    public boolean isConnected = false;

    private boolean predicateWait = true;
    private boolean predicateConnect = true;

    /**
     * Create an uploader which supports streaming.
     *
     * @param serverURL server URL
     * @throws URISyntaxException
     */
    public WebSocketUploader(String serverURL, Map<String, String> header, STTConfiguration config) throws URISyntaxException {
        super(new URI(serverURL), new Draft_17(), header, config.connectionTimeout);
        Log.d(TAG, "New WebSocketUploader: " + serverURL);
        Log.d(TAG, serverURL);
        this.sConfig = config;

        if(sConfig.audioFormat.equals(STTConfiguration.AUDIO_FORMAT_DEFAULT)) {
            this.encoder = new RawEnc();
        }
        else if(sConfig.audioFormat.equals(STTConfiguration.AUDIO_FORMAT_OGGOPUS)){
            this.encoder = new OggOpusEnc();
        }

        if(serverURL.toLowerCase().startsWith("wss") || serverURL.toLowerCase().startsWith("https"))
            this.sConfig.isSSL = true;
        else this.sConfig.isSSL = false;

        this.isConnected = false;
        this.isReadyForAudio = false;
        this.isReadyForClosure = false;

        if(this.audioBuffer == null)
            this.audioBuffer = new LinkedList<WebSocketAudio>();
        this.audioBuffer.clear();

        this.predicateWait = true;
        this.predicateConnect = true;
    }
    /**
     * Trust server
     *
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     */
    private void trustServer() throws NoSuchAlgorithmException, KeyManagementException, IOException {
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
     * 1. Initialize WebSocket connection
     * 2. Init an encoder and writer
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     * @throws IOException
     */
    private void initStreamAudioToServer() throws NoSuchAlgorithmException, KeyManagementException, IOException {
        Log.d(TAG, "Connecting...");
        //lifted up for initializing writer, using isRunning to control the flow
        this.encoder.initEncoderWithUploader(this);

        if(this.sConfig.isSSL)
            this.trustServer();

        this.connect();
    }

    @Override
    public int onHasData(byte[] buffer) {
        return this.writeData(new WebSocketAudio(buffer, WebSocketAudio.STREAM_MARKER_DATA));
    }

    /**
     * Write data
     * @param buffer AudioBuffer
     * @return int
     */
    private synchronized int writeData(WebSocketAudio buffer) {
        int uploadedAudioSize = 0;
        // NOW, WE HAVE STATUS OF UPLOAD PREPARING, UPLOAD PREPARING OK
        if (this.isConnected && this.isReadyForAudio) {
            try {
                if(audioBuffer.size() > 0) {
                    for(int i = 0 ; i < audioBuffer.size(); i++){
                        WebSocketAudio anAudioBuffer = audioBuffer.get(i);
                        if (anAudioBuffer.marker == WebSocketAudio.STREAM_MARKER_DATA) {
                            if (anAudioBuffer.buffer.length > 0)
                                uploadedAudioSize += encoder.encodeAndWrite(anAudioBuffer.buffer);
                        }
                        else {
                            uploadedAudioSize = encoder.write(anAudioBuffer.buffer);
                            if (anAudioBuffer.marker == WebSocketAudio.STREAM_MARKER_END) {
                                this.isReadyForClosure = true;
                                Log.i(TAG, "Ending with buffer " + uploadedAudioSize + " bytes: " + new String(anAudioBuffer.buffer));
                            }
                        }
                    }
                    Log.i(TAG, "Sending out buffered data " + uploadedAudioSize + " bytes, size=" + audioBuffer.size());
                    audioBuffer.clear();
                }
                else {
                    this.predicateWait = false;
                }

                if(buffer.marker == WebSocketAudio.STREAM_MARKER_DATA){
                    if(buffer.buffer.length > 0)
                        uploadedAudioSize += encoder.encodeAndWrite(buffer.buffer);
                }
                else {
                    uploadedAudioSize += encoder.write(buffer.buffer);

                    if(buffer.marker == WebSocketAudio.STREAM_MARKER_END) {
                        this.isReadyForClosure = true;
                        Log.i(TAG, "Ending with data " + uploadedAudioSize + " bytes: " + new String(buffer.buffer));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            if(this.isConnected) {
                if(this.predicateWait) {
                    this.predicateWait = false;
                    this.predicateConnect = true;
                    Log.d(TAG, "Buffering data and wait for 1st response");
                }
            }
            else{
                if(this.predicateConnect) {
                    this.predicateConnect = false;
                    Log.w(TAG, "Buffering data and establishing connection");
                }
            }
            // buffer data
            if(this.isReadyForClosure) {
                //
            }
            else {
                audioBuffer.add(buffer);
            }
        }
        return uploadedAudioSize;
    }

    @Override
    public boolean isUploadPrepared() {
        return this.isConnected;
    }

    /**
     * Prepare connection
     */
    @Override
    public void prepare() {
        this.isConnected = false;
        this.isReadyForAudio = false;
        this.isReadyForClosure = false;

        try {
            try {
                initStreamAudioToServer();
            } catch (NoSuchAlgorithmException | KeyManagementException | IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            Log.e(TAG, "Connection failed: " + (e.getMessage()));
            isConnected = false;
            this.close();
        }
    }

    /**
     * Write string into socket
     *
     * @param message String
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
        Log.w(TAG, "Sending closure data...");
        JSONObject obj = new JSONObject();
        try {
            obj.put("action", "stop");
            this.writeData(new WebSocketAudio(obj.toString().getBytes(), WebSocketAudio.STREAM_MARKER_END));
        } catch (JSONException e) {
            e.printStackTrace();
            this.writeData(new WebSocketAudio(new byte[0], WebSocketAudio.STREAM_MARKER_END));
        }

    }

    @Override
    public void close() {
        Log.d(TAG, "Closing the WebSocket");

        this.isReadyForAudio = false;
        this.isConnected = false;
        this.isReadyForClosure = false;

        super.close();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.d(TAG, "WebSocket closed");

        this.isConnected = false;
        this.isReadyForAudio = false;
        this.isReadyForClosure = false;

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

        this.close(1000, errorMessage);

        this.isConnected = false;
        this.isReadyForAudio = false;
        this.isReadyForClosure = false;

        if (delegate != null){
            delegate.onError(errorMessage);
        }
    }

    @Override
    public void onMessage(String message) {
        Log.e(TAG, message);
        try {
            JSONObject jObj = new JSONObject(message);
            // state message
            if(jObj.has("state")) {
                // monitor state to determine connection closure condition
                String state = jObj.getString("state");
                Log.d(TAG, "onMessage: State: " + state + ", isReadyForClosure=" + this.isReadyForClosure + ", isReadyForAudio="+this.isReadyForAudio + "");
                if(state.equals("listening")) {
                    if(this.isReadyForClosure && (this.isReadyForAudio || !this.sConfig.continuous)) {
                        Log.i(TAG, "Disconnecting...");
                        this.isReadyForAudio = false;
                        this.close(1000, "Closure data has been sent");
                    }
                    else {
                        this.isReadyForAudio = true;
                        if(this.delegate != null){
                            this.delegate.onBegin();
                        }
                        Log.i(TAG, "Start sending audio data");
                    }
                }
            }
            // results message
            if (jObj.has("results")) {
                if (delegate != null){
                    delegate.onMessage(message);
                }
            }
            if (jObj.has("error")) {
                String errorMessage = jObj.getString("error");
                this.onError(new Exception(errorMessage));
            }
        }
        catch (JSONException e) {
            // data error
            Log.e(TAG, "onMessage: Error parsing JSON");
            e.printStackTrace();
            this.onError(e);
        }
    }

    @Override
    public void onOpen(ServerHandshake arg0) {
        Log.d(TAG, "WS connection opened successfully");
        this.isConnected = true;
        if (delegate != null){
            delegate.onOpen();
        }
        this.sendSpeechHeader();
    }

    private void sendSpeechHeader() {
        JSONObject obj = new JSONObject();

        try {
            obj.put("action", "start");
            obj.put("content-type", this.sConfig.audioFormat + "; rate=" + this.sConfig.audioSampleRate);
            obj.put("interim_results", this.sConfig.interimResults);
            obj.put("continuous", this.sConfig.continuous);
            obj.put("inactivity_timeout", this.sConfig.inactivityTimeout);

            if(this.sConfig.maxAlternatives > 1)
                obj.put("max_alternatives", this.sConfig.maxAlternatives);

            if(this.sConfig.keywordsThreshold >= 0 && this.sConfig.keywordsThreshold <= 1)
                obj.put("keywords_threshold", this.sConfig.keywordsThreshold);

            if(this.sConfig.wordAlternativesThreshold >= 0 && this.sConfig.wordAlternativesThreshold <= 1)
                obj.put("word_alternatives_threshold", this.sConfig.wordAlternativesThreshold);

            if(this.sConfig.keywords != null && this.sConfig.keywords.length() > 0){
                obj.put("keywords", this.sConfig.keywords);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        String startHeader = obj.toString();
        this.upload(startHeader);

        this.encoder.onStart();
        Log.d(TAG, "Sending header message: " + startHeader);
    }

    /**
     * Set delegate
     *
     * @param delegate
     */
    public void setDelegate(ISpeechToTextDelegate delegate) {
        this.delegate = delegate;
    }
}
