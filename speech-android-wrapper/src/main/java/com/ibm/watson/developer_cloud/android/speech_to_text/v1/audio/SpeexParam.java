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

package com.ibm.watson.developer_cloud.android.speech_to_text.v1.audio;

public class SpeexParam {

    public static final int QUALITY = 8;

    /** Ogg Stream Serial Number */
    public int streamSerialNumber;
    /** Ogg Page count */
    public int pageCount = 0;

    /** Defines the encoder mode (0=NB, 1=WB and 2=UWB). */
    protected int mode = -1;
    /** Defines the encoder quality setting (integer from 0 to 10). */
    protected int quality = 8;
    /** Defines the encoders algorithmic complexity. */
    protected int complexity = 3;
    /** Defines the number of frames per speex packet. */
    protected int nframes = 1;
    /** Defines the sampling rate of the audio input. */
    protected int sampleRate = -1;
    /** Defines the number of channels of the audio input (1=mono, 2=stereo). */
    protected int channels = 1;
    /** Defines the encoder VBR quality setting (float from 0 to 10). */
    protected float vbr_quality = -1;
    /** Defines whether or not to use VBR (Variable Bit Rate). */
    protected boolean vbr = false;

}
