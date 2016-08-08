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

package com.ibm.watson.developer_cloud.android.text_to_speech.v1.dto;

import android.content.Context;

import com.ibm.watson.developer_cloud.android.speech_common.v1.BaseConfiguration;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by mihui on 6/5/16.
 */
public class TTSConfiguration extends BaseConfiguration {
    public static final String WATSONSDK_DEFAULT_TTS_API_ENDPOINT = "https://stream.watsonplatform.net/text-to-speech/api";
    public static final String WATSONSDK_DEFAULT_TTS_VOICE = "en-US_MichaelVoice";

    public static final String CODEC_WAV = "audio/wav";
    public static final int CODEC_WAV_SAMPLE_RATE = 0;

    public static final String CODEC_OPUS = "audio/opus";
    public static final int CODEC_OPUS_SAMPLE_RATE = 48000;

    public static final int WATSONSDK_DEFAULT_TTS_TIMEOUT = 15000;

    public String voice;
    public String customizationId = null;
    public String codec;

    public Context appContext = null;

    // Authentication flag
    public boolean isAuthNeeded = true;

    public int requestTimeout = WATSONSDK_DEFAULT_TTS_TIMEOUT;

    public TTSConfiguration() {
        this.codec = CODEC_WAV;
        this.voice = WATSONSDK_DEFAULT_TTS_VOICE;
        this.appContext = null;

        this.setAPIURL(WATSONSDK_DEFAULT_TTS_API_ENDPOINT);
    }

    public TTSConfiguration(String customizationId) {
        this();
        this.customizationId = customizationId;
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

    public String getSynthesizeURL(){
        return this.apiURL + "/v1/synthesize";
    }

    public String getVoicesServiceURL() {
        return this.apiURL + "/v1/voices";
    }

    public String getCustomizedVoicesServiceURL() {
        return this.apiURL + "/v1/customizations";
    }

    public String getCustomizedVoicesServiceURL(String customizationId) {
        return this.apiURL + "/v1/customizations/"+customizationId;
    }

    public String getCustomizedWordServiceURL(String customizationId) {
        return this.apiURL + "/v1/customizations/"+customizationId+"/words";
    }
}
