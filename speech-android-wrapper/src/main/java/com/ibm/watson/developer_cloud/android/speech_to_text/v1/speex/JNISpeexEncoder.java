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

import com.ibm.watson.developer_cloud.android.speech_common.v1.util.Logger;

/**
 * A thin wrapper around some JNI wrapped around the Xiph Speex library.
 *
 * The samples read from an {@link android.media.AudioTrack} are compatible with the library.
 *
 * 
 *
 * Copyright 2012, Robert Forsman
 * speex-ndk@thoth.purplefrog.com
 */
public class JNISpeexEncoder
{
    
    /** The slot. */
    private final int slot;

    /**
     * Instantiates a new jNI speex encoder.
     *
     * @param band the band
     * @param quality the quality
     */
    public JNISpeexEncoder(FrequencyBand band, int quality)
    {
        slot = allocate(band.code, quality);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize()
        throws Throwable
    {
        deallocate(slot);
    }

    /**
     * Gets the frame size.
     *
     * @return the frame size
     */
    public synchronized int getFrameSize()
    {
        return getFrameSize(slot);
    }

    /**
     * Encode.
     *
     * @param samples must have length == {@link #getFrameSize()}
     * @return a compressed audio frame suitable for use with {@link JNISpeexDecoder#decode(byte[])}.  Getting it across the network in one piece with the right framing is <i>your</i> problem.
     */
    public synchronized byte[] encode(short[] samples)
    {
        return encode(slot, samples);
    }

    /**
     * Encode.
     *
     * @param slot the slot
     * @param samples the samples
     * @return the byte[]
     */
    private native static byte[] encode(int slot, short[] samples);
    
    /**
     * Gets the frame size.
     *
     * @param slot the slot
     * @return the frame size
     */
    private native static int getFrameSize(int slot);

    /**
     * allocates a slot in the JNI implementation for our native bits.  Store it in the {@link #slot} field.
     *
     * @param band_code 0 = narrowband, 1 = wideband, 2 = ultra-wide band
     * @param quality from 0 to 10 inclusive, used by the speex library
     * @return an index into a slot array in the JNI implementation for our encoder parameters.
     */
    protected native static int allocate(int band_code, int quality);

    /**
     * Deallocate.
     *
     * @param slot the return value from a previous call to {@link #allocate(int, int)}
     */
    protected native static void deallocate(int slot);

    static {
        System.loadLibrary("speex");
    }

    /**
     * The main method.
     *
     * @param argv the arguments
     */
    public static void main(String[] argv)
    {
        short[] bogus = new short[666];

        byte[] frame = new JNISpeexEncoder(FrequencyBand.WIDE_BAND, 9).encode(bogus);

        Logger.i(JNISpeexEncoder.class.getSimpleName(), "Frame size: " + frame.length);
    }
}