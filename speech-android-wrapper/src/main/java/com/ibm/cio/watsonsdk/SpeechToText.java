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

package com.ibm.cio.watsonsdk;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;

import org.java_websocket.util.Base64;

import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import com.ibm.cio.audio.IAudioConsumer;
import com.ibm.cio.audio.AudioCaptureThread;
import com.ibm.cio.audio.ChuckOggOpusEnc;
import com.ibm.cio.audio.ChuckRawEnc;
import com.ibm.cio.dto.SpeechConfiguration;
import com.ibm.cio.audio.ISpeechEncoder;
import com.ibm.cio.audio.ChuckWebSocketUploader;
import com.ibm.cio.audio.IChunkUploader;
import com.ibm.cio.util.Logger;

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

/**Speech Recognition Class for SDK functions
 * @author Viney Ugave (vaugave@us.ibm.com)
 *
 */
public class SpeechToText {
    protected static final String TAG = "SpeechToText";

    private String transcript;

    private Context appCtx;
    private SpeechConfiguration sConfig;
    private AudioCaptureThread audioCaptureThread = null;

    private IChunkUploader uploader = null;
    private SpeechDelegate delegate = null;
    private String username;
    private String password;
    private String model;
    private TokenProvider tokenProvider = null;
    private URI hostURL;

    /** Audio encoder. */
    private ISpeechEncoder encoder;
    /** Flag <code>true/<code>false</code>. <code>True</code> if user has tapped on "X" button to dismiss recording diaLogger. */
    private volatile boolean isCancelled = false;
    /** UPLOADING TIIMEOUT  */
    private int UPLOADING_TIMEOUT = 5000; // default duration of closing connection

    /**
     * Constructor
     */
    public SpeechToText() {
        this.setTimeout(0);
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
            //Logger.d(TAG, "####### volume=" + volume + ", amplitude="+amplitude);
            if(delegate != null){
                delegate.onAmplitude(amplitude, volume);
            }
        }
    }

    /**
     * Start recording
     */
    private void startRecording() {
        Logger.i(TAG, "-> startRecording");
        uploader.prepare();
        STTIAudioConsumer audioConsumer = new STTIAudioConsumer(uploader);

        audioCaptureThread = new AudioCaptureThread(SpeechConfiguration.SAMPLE_RATE, audioConsumer);
        audioCaptureThread.start();
    }

    /**
     * Start recording audio
     */
    public void recognize() {
        Log.i(TAG, "recognize");
        // Initiate Uploader, Encoder

        try {
            HashMap<String, String> header = new HashMap<String, String>();
            if(sConfig.audioFormat.equals(SpeechConfiguration.AUDIO_FORMAT_DEFAULT)) {
                encoder = new ChuckRawEnc();
            }
            else if(sConfig.audioFormat.equals(SpeechConfiguration.AUDIO_FORMAT_OGGOPUS)){
                encoder = new ChuckOggOpusEnc();
            }

            header.put("Content-Type", sConfig.audioFormat);

            if(sConfig.isAuthNeeded) {
                if (this.tokenProvider != null) {
                    header.put("X-Watson-Authorization-Token", this.tokenProvider.getToken());
                    Logger.e(TAG, "ws connecting with token based authentication");
                } else {
                    String auth = "Basic " + Base64.encodeBytes((this.username + ":" + this.password).getBytes(Charset.forName("UTF-8")));
                    header.put("Authorization", auth);
                    Logger.e(TAG, "ws connecting with Basic Authentication");
                }
            }

            String wsURL = getHostURL().toString() + "/v1/recognize" + (this.model != null ? ("?model=" + this.model) : "");

            uploader = new ChuckWebSocketUploader(encoder, wsURL, header, sConfig);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        uploader.setTimeout(UPLOADING_TIMEOUT); // default timeout
        uploader.setDelegate(this.delegate);
        startRecording();
    }

    /**
     * Stop recognition
     */
    public void stopRecognition() {
        if(audioCaptureThread != null)
            audioCaptureThread.end();

        if(uploader != null)
            uploader.close();
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
     * Change default timeout
     *
     * @param timeout
     */
    public void setTimeout(int timeout){
        this.UPLOADING_TIMEOUT = timeout;
    }
    /**
     * @return the appCtx
     */
    public Context getAppCtx() { return appCtx; }
    /**
     * @param appCtx the appCtx to set
     */
    public void setAppCtx(Context appCtx) { this.appCtx = appCtx; }
    /**
     * @return the transcript
     */
    public String getTranscript() { return transcript; }
    /**
     * @param transcript the transcript to set
     */
    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }
    /**
     * @return the isCancelled
     */
    public boolean isCancelled() {
        return isCancelled;
    }
    /**
     * @param isCancelled the isCancelled to set
     */
    public void setCancelled(boolean isCancelled) { this.isCancelled = isCancelled; }
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
    public SpeechDelegate getDelegate() {
        return delegate;
    }
    /**
     * @param delegate the delegate to set
     */
    public void setDelegate(SpeechDelegate delegate) {
        this.delegate = delegate;
    }
    /**
     * Set the recorder delegate for the encoder
     */
    public void setRecorderDelegate(SpeechRecorderDelegate obj){
        if(encoder != null)
            encoder.setDelegate(obj);
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

