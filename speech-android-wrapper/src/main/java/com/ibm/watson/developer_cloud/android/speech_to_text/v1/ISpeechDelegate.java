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

public interface ISpeechDelegate {

    /**
     * called once the connection with the STT service has been established
     */
    void onOpen();

    /**
     * called if there is an error using the STT service
     */
    void onError(String error);

    /**
     * called once the connection with the STT service has been terminated
     */
    void onClose(int code, String reason, boolean remote);

    /**
     * called every time a data message comes from the STT service
     */
    void onMessage(String message);

    /**
     * Receive the data of amplitude and volume
     */
    void onAmplitude(double amplitude, double volume);
}
