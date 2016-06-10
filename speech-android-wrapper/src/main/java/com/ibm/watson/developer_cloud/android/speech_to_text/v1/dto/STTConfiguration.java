/**
 * © Copyright IBM Corporation 2016
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


package com.ibm.watson.developer_cloud.android.speech_to_text.v1.dto;

import com.ibm.watson.developer_cloud.android.speech_common.v1.AuthConfiguration;

import org.json.JSONArray;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by mihui on 9/2/15.
 */
public class STTConfiguration extends AuthConfiguration {
    public static final String WATSONSDK_DEFAULT_STT_API_ENDPOINT = "https://stream.watsonplatform.net/speech-to-text/api";
    public static final String WATSONSDK_DEFAULT_STT_MODEL = "en-US_BroadbandModel";
    // PCM format
    public static final String AUDIO_FORMAT_DEFAULT = "audio/l16;rate=16000";
    // OggOpus format
    public static final String AUDIO_FORMAT_OGGOPUS = "audio/ogg;codecs=opus";
    // Audio channels
    public static final int AUDIO_CHANNELS = 1;
    // Frame size
    public static final int FRAME_SIZE = 160;
    // Sample rate
    public static final int SAMPLE_RATE = 16000;
    // Timeout
    public int inactivityTimeout = 600;
    // continuous
    public boolean continuous = false;
    // interim_results
    public boolean interimResults = true;
    // language model
    public String languageModel = WATSONSDK_DEFAULT_STT_MODEL;
    // Data format
    public String audioFormat = AUDIO_FORMAT_DEFAULT;
    // Authentication flag
    public boolean isAuthNeeded = true;
    // SSL flag, this would be detected automatically
    public boolean isSSL = true;
    // Default timeout duration for a connection
    public int connectionTimeout = 30000;

    // Keyword spotting
    public double keywordsThreshold = -1;
    // Maximum alternatives
    public int maxAlternatives = 1;

    // keyword list
    public JSONArray keywords;

    /**
     * Instantiate default configuration
     */
    public STTConfiguration(){
        this.setAPIURL(WATSONSDK_DEFAULT_STT_API_ENDPOINT);
    }

    /**
     * Constructing configuration by parameters
     *
     * @param audioFormat
     */
    public STTConfiguration(String audioFormat){
        this();
        this.audioFormat = audioFormat;
    }

    /**
     * Constructing configuration by parameters
     *
     * @param audioFormat
     * @param isAuthNeeded
     */
    public STTConfiguration(String audioFormat, boolean isAuthNeeded){
        this(audioFormat);
        this.isAuthNeeded = isAuthNeeded;
    }

    /**
     * Change API URL as well as the apiEndpoint
     * @param val
     */
    public void setAPIURL(String val){
        this.apiURL = val;
        try {
            this.apiEndpoint = new URI(this.apiURL);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }


    public String getModelsURL() {
        return this.apiURL + "/v1/models";
    }

    public String getModelsURL(String model) {
        return this.getModelsURL() + "/" + model;
    }
}
