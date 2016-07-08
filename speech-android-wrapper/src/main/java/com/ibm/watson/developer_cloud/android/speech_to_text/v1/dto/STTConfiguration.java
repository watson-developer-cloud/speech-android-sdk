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
    public static final String AUDIO_FORMAT_DEFAULT = "audio/l16";
    // OggOpus format
    public static final String AUDIO_FORMAT_OGGOPUS = "audio/ogg;codecs=opus";
    // Audio channels
    public static final int AUDIO_CHANNELS = 1;
    // Frame size
    public static final int FRAME_SIZE_OGGOPUS = 160;
    // Sample rate
    public static final int SAMPLE_RATE_OGGOPUS = 16000;
    // Sample rate
    public static final int SAMPLE_RATE_DEFAULT = 48000;
    // Timeout in seconds
    public int inactivityTimeout = 30;
    // continuous
    public boolean continuous = false;
    // interim_results
    public boolean interimResults = true;
    // language model
    public String languageModel = WATSONSDK_DEFAULT_STT_MODEL;
    // Data format
    public String audioFormat = AUDIO_FORMAT_DEFAULT;
    // Audio sample rate
    public int audioSampleRate = SAMPLE_RATE_DEFAULT;
    // Authentication flag
    public boolean isAuthNeeded = true;
    // SSL flag, this would be detected automatically
    public boolean isSSL = true;
    // Default timeout duration for a connection
    public int connectionTimeout = 30000;
    // Keyword spotting: The keyword spotting feature lets you detect specified strings in the transcript generated for input audio by the service. The service can spot the same keyword multiple times and report each occurrence. By default, the service does no keyword spotting.
    public double keywordsThreshold = -1;
    // Maximum alternatives: The max_alternatives parameter accepts an integer value that tells the service to return the n-best alternative hypotheses. By default, the service returns only a single transcription result, which is equivalent to setting the parameter to 1.
    public int maxAlternatives = 1;
    // Specifies a minimum level of confidence that the service must have to report a hypothesis for a word from the input audio. Specify a probability value between 0 and 1 inclusive. A hypothesis must have at least the specified confidence to be returned as a word alternative. Omit the parameter or specify a value of null (the default) to return no word alternatives.
    public double wordAlternativesThreshold = -1;
    // keyword list
    public JSONArray keywords = null;


    /**
     * Instantiate default configuration
     */
    public STTConfiguration(){
        this.setAPIURL(WATSONSDK_DEFAULT_STT_API_ENDPOINT);
    }

    /**
     * Constructing configuration by parameters
     *
     * @param audioFormat String
     * @param audioSampleRate int
     */
    public STTConfiguration(String audioFormat, int audioSampleRate){
        this();
        this.audioFormat = audioFormat;
        this.audioSampleRate = audioSampleRate;
    }

    /**
     * Constructing configuration by parameters
     *
     * @param audioFormat String
     * @param audioSampleRate int
     * @param isAuthNeeded boolean
     */
    public STTConfiguration(String audioFormat, int audioSampleRate, boolean isAuthNeeded){
        this(audioFormat, audioSampleRate);
        this.isAuthNeeded = isAuthNeeded;
    }

    /**
     * Change API URL as well as the apiEndpoint
     * @param val String
     */
    public void setAPIURL(String val){
        this.apiURL = val;
        try {
            this.apiEndpoint = new URI(this.apiURL);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get model list service URL
     * @return String
     */
    public String getModelsURL() {
        return this.apiURL + "/v1/models";
    }

    /**
     * Get model info service URL
     * @param model String
     * @return String
     */
    public String getModelsURL(String model) {
        return this.getModelsURL() + "/" + model;
    }

    /**
     * Set audio format and sample rate
     * @param audioFormat String
     * @param audioSampleRate int
     */
    public void setAudioFormat(String audioFormat, int audioSampleRate) {
        this.audioSampleRate = audioSampleRate;
        this.audioFormat = audioFormat;
    }
}
