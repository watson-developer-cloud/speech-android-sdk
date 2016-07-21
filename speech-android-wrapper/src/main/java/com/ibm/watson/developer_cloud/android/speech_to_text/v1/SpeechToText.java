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

package com.ibm.watson.developer_cloud.android.speech_to_text.v1;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.HashMap;

import org.java_websocket.util.Base64;

import android.content.Context;
import android.util.Log;

import com.ibm.watson.developer_cloud.android.speech_to_text.v1.audio.IAudioConsumer;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.audio.AudioCaptureThread;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.dto.SpeechConfiguration;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.audio.WebSocketUploader;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.audio.IChunkUploader;
import com.ibm.watson.developer_cloud.android.speech_common.v1.TokenProvider;

// HTTP library
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Speech Recognition Class for SDK functions
 * @author Viney Ugave (vaugave@us.ibm.com)
 */
public class SpeechToText {

    protected static final String TAG = "SpeechToText";
    //private String transcript;
    private Context appCtx;
    private SpeechConfiguration sConfig;
    private AudioCaptureThread audioCaptureThread = null;
    private IChunkUploader uploader = null;
    private ISpeechDelegate delegate = null;
    private String username;
    private String password;
    private String model;
    private TokenProvider tokenProvider = null;
    private URI hostURL;
    /** UPLOADING TIIMEOUT  */
    //private int UPLOADING_TIMEOUT = 5000; // default duration of closing connection

    /**
     * Constructor
     */
    public SpeechToText() {
        this.sConfig = null;
    }

    /**
     * Speech Recognition Shared Instance
     */
    private static SpeechToText _instance = null;

    public static SpeechToText sharedInstance(){
        if(_instance == null){
            synchronized(SpeechToText.class){
                _instance = new SpeechToText();
            }
        }
        return _instance;
    }

    /**
     * Init the shared instance with the context
     * @param uri
     * @param ctx
     * @param sc
     */
    public void initWithContext(URI uri, Context ctx, SpeechConfiguration sc){
        this.setHostURL(uri);
        this.appCtx = ctx;
        this.sConfig = sc;
    }

    /**
     * Audio consumer
     */
    private class STTIAudioConsumer implements IAudioConsumer {
        private IChunkUploader mUploader = null;

        public STTIAudioConsumer(IChunkUploader uploader) {
            mUploader = uploader;
        }

        public void consume(byte [] data) {
            mUploader.onHasData(data);
        }

        @Override
        public void onAmplitude(double amplitude, double volume) {
            if(delegate != null){
                delegate.onAmplitude(amplitude, volume);
            }
        }
    }

    /**
     * Start recording
     */
    private void startRecording() {
        uploader.prepare();
        STTIAudioConsumer audioConsumer = new STTIAudioConsumer(uploader);

        audioCaptureThread = new AudioCaptureThread(SpeechConfiguration.SAMPLE_RATE, audioConsumer);
        audioCaptureThread.start();
    }

    /**
     * Start recording audio
     */
    public void recognize() {
        Log.d(TAG, "recognize");
        try {
            HashMap<String, String> header = new HashMap<String, String>();
            header.put("Content-Type", sConfig.audioFormat);

            if(sConfig.isAuthNeeded) {
                if (this.tokenProvider != null) {
                    header.put("X-Watson-Authorization-Token", this.tokenProvider.getToken());
                    Log.d(TAG, "ws connecting with token based authentication");
                } else {
                    String auth = "Basic " + Base64.encodeBytes((this.username + ":" + this.password).getBytes(Charset.forName("UTF-8")));
                    header.put("Authorization", auth);
                    Log.d(TAG, "ws connecting with Basic Authentication");
                }
            }

            if (sConfig.learningOptOut) {
                header.put("X-Watson-Learning-OptOut", "true");
                Log.d(TAG, "ws setting X-Watson-Learning-OptOut");
            }

            String wsURL = getHostURL().toString() + "/v1/recognize" + (this.model != null ? ("?model=" + this.model) : "");

            uploader = new WebSocketUploader(wsURL, header, sConfig);
            uploader.setDelegate(this.delegate);
            this.startRecording();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stop audio recording
     */
    public void stopRecording(){
        if(audioCaptureThread != null)
            audioCaptureThread.end();
    }

    /**
     * Stop recognition
     */
    public void stopRecognition() {
        this.stopRecording();
        if(uploader != null) {
            uploader.stop();
            uploader.close();
        }
    }

    /**
     * Build authentication header
     * @param httpGet
     */
    private void buildAuthenticationHeader(HttpGet httpGet) {
        // use token based authentication if possible, otherwise Basic Authentication will be used
        if (this.tokenProvider != null) {
            Log.d(TAG, "using token based authentication");
            httpGet.setHeader("X-Watson-Authorization-Token",this.tokenProvider.getToken());
        } else {
            Log.d(TAG, "using basic authentication");
            httpGet.setHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(this.username, this.password), "UTF-8",false));
        }
    }

    /**
     * Get the list of models for the speech to text service
     * @return
     */
    public JSONObject getModels() {
        JSONObject object = null;

        try {
            Log.d(TAG, "starting getModels");
            HttpClient httpClient = new DefaultHttpClient();
            String strHTTPURL = this.hostURL.toString().replace("wss","https").replace("ws", "http");
            HttpGet httpGet = new HttpGet(strHTTPURL+"/v1/models");
            this.buildAuthenticationHeader(httpGet);
            httpGet.setHeader("accept","application/json");
            HttpResponse executed = httpClient.execute(httpGet);
            InputStream is=executed.getEntity().getContent();

            // get the JSON object containing the models from the InputStream
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder responseStrBuilder = new StringBuilder();
            String inputStr;
            while ((inputStr = streamReader.readLine()) != null)
                responseStrBuilder.append(inputStr);
            Log.d(TAG, "response: " + responseStrBuilder.toString());
            object = new JSONObject(responseStrBuilder.toString());
            Log.d(TAG, object.toString());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return object;
    }

    /**
     * Get information about the model
     * @param strModel
     * @return
     */
    public JSONObject getModelInfo(String strModel) {
        JSONObject object = null;

        try {
            HttpClient httpClient = new DefaultHttpClient();
            String strHTTPURL = this.hostURL.toString().replace("wss", "https").replace("ws", "http");
            HttpGet httpGet = new HttpGet(strHTTPURL+"/v1/models/en-US_NarrowbandModel");
            this.buildAuthenticationHeader(httpGet);
            httpGet.setHeader("accept","application/json");
            HttpResponse executed = httpClient.execute(httpGet);
            InputStream is=executed.getEntity().getContent();

            // get the JSON object containing the models from the InputStream
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder responseStrBuilder = new StringBuilder();
            String inputStr;
            while ((inputStr = streamReader.readLine()) != null)
                responseStrBuilder.append(inputStr);
            object = new JSONObject(responseStrBuilder.toString());
            Log.d(TAG, object.toString());

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return object;
    }

    /**
     * @return the hostURL
     */
    public URI getHostURL() {
        return hostURL;
    }
    /**
     * @param hostURL the hostURL to set
     */
    public void setHostURL(URI hostURL) {
        this.hostURL = hostURL;
    }
    /**
     * @return the delegate
     */
    public ISpeechDelegate getDelegate() {
        return delegate;
    }
    /**
     * @param val the delegate to set
     */
    public void setDelegate(ISpeechDelegate val) {
        this.delegate = val;
    }
    /**
     * Set API credentials
     * @param username
     */
    public void setCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }
    /**
     * Set token provider (for token based authentication)
     */
    public void setTokenProvider(TokenProvider tokenProvider) { this.tokenProvider = tokenProvider; }
    /**
     * Set STT model
     */
    public void setModel(String model) {
        this.model = model;
    }
}

