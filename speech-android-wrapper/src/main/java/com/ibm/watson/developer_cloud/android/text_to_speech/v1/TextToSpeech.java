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

package com.ibm.watson.developer_cloud.android.text_to_speech.v1;

import android.util.Log;

import com.ibm.watson.developer_cloud.android.speech_common.v1.TokenProvider;

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
import java.net.URI;

/**
 * Speech Recognition Class for SDK functions
 * @author Viney Ugave (vaugave@us.ibm.com)
  */
public class TextToSpeech {
    protected static final String TAG = "TextToSpeech";

    private TTSUtility ttsUtility;
    private String username;
    private String password;
    private URI hostURL;
    private TokenProvider tokenProvider = null;
    private String voice;
    private boolean learningOptOut = false;

    /**Speech Recognition Shared Instance
     *
     */
    private static TextToSpeech _instance = null;

    public static TextToSpeech sharedInstance(){
        if(_instance == null){
            synchronized(TextToSpeech.class){
                _instance = new TextToSpeech();
            }
        }
        return _instance;
    }

    /**
     * Init the shared instance with the context
     * @param uri
     */
    public void initWithContext(URI uri){
        this.setHostURL(uri);
    }

    /**
     * Send request of TTS
     * @param ttsString
     */
    public void synthesize(String ttsString) {
        Log.d(TAG, "synthesize called: " + this.hostURL.toString() + "/v1/synthesize");
        String[] Arguments = {
                this.hostURL.toString()+"/v1/synthesize",
                this.username,
                this.password,
                this.voice,
                ttsString,
                this.tokenProvider == null ? null : this.tokenProvider.getToken(),
                this.learningOptOut ? "true" : null
        };
        try {
            ttsUtility = new TTSUtility();
            ttsUtility.setCodec(TTSUtility.CODEC_WAV);
            ttsUtility.synthesize(Arguments);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

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

    public JSONObject getVoices() {

        JSONObject object = null;

        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(this.hostURL+"/v1/voices");
            Log.d(TAG,"url: " + this.hostURL+"/v1/voices");
            this.buildAuthenticationHeader(httpGet);
            httpGet.setHeader("accept", "application/json");
            HttpResponse executed = httpClient.execute(httpGet);
            InputStream is = executed.getEntity().getContent();

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
     * Set credentials
     * @param username
     * @param password
     */
    public void setCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Set host URL
     * @param hostURL
     */
    public void setHostURL(URI hostURL) {
        this.hostURL = hostURL;
    }

    /**
     * Set token provider (for token based authentication)
     */
    public void setTokenProvider(TokenProvider tokenProvider) { this.tokenProvider = tokenProvider; }

    /**
     * Set TTS voice
     */
    public void setVoice(String voice) {
        this.voice = voice;
    }

    /**
     * Set X-Watson-Learning-OptOut
     * @param optOut
     */
    public void setLearningOptOut(boolean optOut) { this.learningOptOut = optOut; }
}
