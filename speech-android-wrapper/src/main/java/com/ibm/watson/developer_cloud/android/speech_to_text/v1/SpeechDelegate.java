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

package com.ibm.watson.developer_cloud.android.speech_to_text.v1;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.dto.QueryResult;

public interface SpeechDelegate {
	public final static int MESSAGE = 0;
	public final static int ERROR = -1;
	public final static int CLOSE = -2;
	public final static int OPEN = 1;
    public final static int WAIT = 2;

    /**
     * Receive message with status code
     * @param code
     * @param result
     */
	public void onMessage(int code, QueryResult result);

    /**
     * Recieve the data of amplitude and volume
     * @param amplitude
     * @param volume
     */
    public void onAmplitude(double amplitude, double volume);
}
