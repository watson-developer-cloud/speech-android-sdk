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

package com.ibm.watson.developer_cloud.android.speech_to_text.v1.speex;

/**
* Created by IntelliJ IDEA.
* User: thoth
* Date: 9/12/12
* Time: 5:51 PM
* To change this template use File | Settings | File Templates.
*/
public enum FrequencyBand
{
    /**
     * 8 KHz sample rate
     */
    NARROW_BAND(0),
    /**
     * 16 KHz sample rate
     */
    WIDE_BAND(1),
    /**
     * 32 KHz sample rate
     */
    ULTRA_WIDE_BAND(2);

    public final int code;
    
    FrequencyBand(int code)
    {
        this.code = code;
    }
}
