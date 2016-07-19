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

import com.ibm.watson.developer_cloud.android.speech_to_text.v1.audio.AudioCaptureRunnable;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.audio.FileCaptureRunnable;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.audio.IAudioConsumer;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.dto.STTConfiguration;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.audio.WebSocketUploader;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.audio.IChunkUploader;
import com.ibm.watson.developer_cloud.android.speech_common.v1.ITokenProvider;

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
import java.io.File;

import org.java_websocket.util.Base64;

import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.HashMap;

import android.util.Log;

/**
 * Speech Recognition Class for SDK functions
 * @author Viney Ugave (vaugave@us.ibm.com)
 */
public class SpeechToText {

    protected static final String TAG = "SpeechToText";
    private STTConfiguration sConfig = null;
    private AudioCaptureRunnable mAudioCaptureRunnable = null;
    private Thread mAudioCaptureThread = null;
    private FileCaptureRunnable mFileCaptureRunnable = null;
    private IChunkUploader uploader = null;
    private ISpeechToTextDelegate delegate = null;
    private boolean isNewRecordingAllowed = false;

    /**
     * Constructor
     */
    public SpeechToText() {

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
     * Init the shared instance with configurations
     * @param config STTConfiguration
     */
    public void initWithConfig(STTConfiguration config, ISpeechToTextDelegate delegate){
        SpeechToText.sharedInstance();
        _instance.sConfig = config;
        _instance.isNewRecordingAllowed = true;
        _instance.delegate = delegate;
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
            mUploader.writeData(data);
        }

        @Override
        public void onAmplitude(double amplitude, double volume) {
            delegate.onAmplitude(amplitude, volume);
        }

        @Override
        public void end() {
            mUploader.stop();
            isNewRecordingAllowed = true;
        }
    }

    /**
     * Start recording
     */
    private void startRecording() {
        this.uploader.prepare();

        STTIAudioConsumer audioConsumer = new STTIAudioConsumer(uploader);
        this.mAudioCaptureRunnable = new AudioCaptureRunnable(sConfig.audioSampleRate, audioConsumer);

        this.mAudioCaptureThread = new Thread(this.mAudioCaptureRunnable);
        this.mAudioCaptureThread.start();
    }

    /**
     * Start reading file
     *
     * @param file File
     */
    private void startReadingFile(File file) {
        this.uploader.prepare();

        STTIAudioConsumer audioConsumer = new STTIAudioConsumer(uploader);
        this.mFileCaptureRunnable = new FileCaptureRunnable(audioConsumer, file);
    }

    /**
     * Recognize with a file
     * @param file File
     * @return FileCaptureRunnable
     */
    public FileCaptureRunnable recognizeWithFile(File file){
        Log.d(TAG, "recognizeWithFile:" + isNewRecordingAllowed);
        try {
            HashMap<String, String> header = new HashMap<String, String>();
            header.put("Content-Type", sConfig.audioFormat + "; rate=" + sConfig.audioSampleRate);

            if (sConfig.isAuthNeeded) {
                if (sConfig.hasTokenProvider()) {
                    header.put("X-Watson-Authorization-Token", sConfig.requestToken());
                    Log.d(TAG, "ws connecting with token based authentication");
                } else {
                    String auth = "Basic " + Base64.encodeBytes((this.sConfig.basicAuthUsername + ":" + this.sConfig.basicAuthPassword).getBytes(Charset.forName("UTF-8")));
                    header.put("Authorization", auth);
                    Log.d(TAG, "ws connecting with Basic Authentication");
                }
            }

            String wsURL = sConfig.apiURL.replace("https", "wss").replace("http", "ws") + "/v1/recognize" + (this.sConfig.languageModel != null ? ("?model=" + this.sConfig.languageModel) : "");

            this.uploader = new WebSocketUploader(wsURL, header, sConfig);
            this.uploader.setDelegate(this.delegate);
            this.startReadingFile(file);
            return this.mFileCaptureRunnable;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Start recording audio
     */
    public void recognize() {
        Log.d(TAG, "recognize");
        if(isNewRecordingAllowed) {
            try {
                HashMap<String, String> header = new HashMap<String, String>();
                header.put("Content-Type", sConfig.audioFormat + "; rate=" + sConfig.audioSampleRate);

                if (sConfig.isAuthNeeded) {
                    if (sConfig.hasTokenProvider()) {
                        header.put("X-Watson-Authorization-Token", sConfig.requestToken());
                        Log.d(TAG, "ws connecting with token based authentication");
                    } else {
                        String auth = "Basic " + Base64.encodeBytes((this.sConfig.basicAuthUsername + ":" + this.sConfig.basicAuthPassword).getBytes(Charset.forName("UTF-8")));
                        header.put("Authorization", auth);
                        Log.d(TAG, "ws connecting with Basic Authentication");
                    }
                }

                if(sConfig.xWatsonLearningOptOut) {
                    header.put("X-Watson-Learning-Opt-Out", "true");
                }

                String wsURL = sConfig.apiURL.replace("https", "wss").replace("http", "ws") + "/v1/recognize" + (this.sConfig.languageModel != null ? ("?model=" + this.sConfig.languageModel) : "");

                this.uploader = new WebSocketUploader(wsURL, header, sConfig);
                this.uploader.setDelegate(this.delegate);
                this.startRecording();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            this.isNewRecordingAllowed = false;
        }
        else {
            this.delegate.onError("A voice query is already in progress");
        }
    }

    /**
     * Stop audio recording
     */
    public void stopRecording(){
        if(mAudioCaptureRunnable != null)
            mAudioCaptureRunnable.end();

        this.isNewRecordingAllowed = true;
    }

    /**
     *
     */
    public void disConnect() {
        if(this.uploader != null)
            this.uploader.close();
    }

    /**
     * Send out end of stream data
     */
    public void endTransmission() {
        if(this.uploader != null) { // && this.sConfig.continuous
            this.uploader.stop();
        }
    }

    /**
     * Stop recognition
     */
    public void stopRecognition() {
        this.stopRecording();
        this.endTransmission();
        this.disConnect();
    }

    /**
     * End recognition, but does not close connection
     * Wait for closure signal
     */
    public void endRecognition() {
        this.stopRecording();
        this.endTransmission();
    }

    /**
     * Build authentication header
     * @param httpGet HttpGet
     */
    private void buildAuthenticationHeader(HttpGet httpGet) {
        if(sConfig.isAuthNeeded) {
            // use token based authentication if possible, otherwise Basic Authentication will be used
            if (sConfig.hasTokenProvider()) {
                Log.d(TAG, "using token based authentication");
                httpGet.setHeader("X-Watson-Authorization-Token", sConfig.requestToken());
            }
            else {
                Log.d(TAG, "using basic authentication");
                httpGet.setHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(this.sConfig.basicAuthUsername, this.sConfig.basicAuthPassword), "UTF-8", false));
            }
        }
    }

    /**
     * Get the list of models for the speech to text service
     * @return JSONObject
     */
    public JSONObject getModels() {
        JSONObject object = null;

        try {
            Log.d(TAG, "starting getModels");
            HttpClient httpClient = new DefaultHttpClient();
            String strHTTPURL = this.sConfig.getModelsURL();
            HttpGet httpGet = new HttpGet(strHTTPURL);
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
     * @param strModel String
     * @return JSONObject
     */
    public JSONObject getModelInfo(String strModel) {
        JSONObject object = null;

        try {
            HttpClient httpClient = new DefaultHttpClient();
            String strHTTPURL = this.sConfig.getModelsURL(strModel);
            HttpGet httpGet = new HttpGet(strHTTPURL);
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
     * Change API URL
     *
     * @param val the hostURL to set
     */
    public void setAPIURL(String val) {
        this.sConfig.setAPIURL(val);
    }

    /**
     * @return the delegate
     */
    public ISpeechToTextDelegate getDelegate() {
        return delegate;
    }

    /**
     * @param val the delegate to set
     */
    public void setDelegate(ISpeechToTextDelegate val) {
        this.delegate = val;
    }

    /**
     * Set API credentials
     * @param username String
     * @param password String
     */
    public void setCredentials(String username, String password) {
        this.sConfig.basicAuthUsername = username;
        this.sConfig.basicAuthPassword = password;
    }

    /**
     * Set token
     * @param token String
     */
    public void setToken(String token) {
        this.sConfig.token = token;
    }
    /**
     * Set token provider (for token based authentication)
     * @see STTConfiguration class
     */
    public void setTokenProvider(ITokenProvider tokenProvider) { this.sConfig.setTokenProvider(tokenProvider); }

    /**
     * Set STT model
     * @param model String
     */
    public void setModel(String model) {
        this.sConfig.languageModel = model;
    }

    /**
     * Get STT model
     * @return String
     */
    public String getModel(){
        return this.sConfig.languageModel;
    }

    /**
     * Set codec and sample rate
     * @param audioFormat String
     * @param sampleRate int
     */
    public void setCodec(String audioFormat, int sampleRate){
        this.sConfig.audioFormat = audioFormat;
        this.sConfig.audioSampleRate = sampleRate;
    }
}

