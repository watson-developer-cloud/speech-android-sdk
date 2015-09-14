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

package com.ibm.cio.speex;

/**
 * Copyright 2012, Robert Forsman
 * speex-ndk@thoth.purplefrog.com
 */
public class JNISpeexDecoder
{

    private final int slot;

    public JNISpeexDecoder(FrequencyBand band)
    {
        slot = allocate(band.code);
    }

    @Override
    protected void finalize()
        throws Throwable
    {
        deallocate(slot);
    }

    public short[] decode(byte[] frame)
    {
        return decode(slot, frame);
    }

    private native static short[] decode(int slot, byte[] frame);

    /**
     * allocates a slot in the JNI implementation for our native bits.  Store it in the {@link #slot} field.
     * @param wideband true for wideband, false for narrowband
     * @return an index into a slot array in the JNI implementation for our encoder parameters.
     */
    protected native static int allocate(int wideband);

    /**
     * @param slot the return value from a previous call to {@link #allocate(int)}
     */
    protected native static void deallocate(int slot);

    static {
        System.loadLibrary("speex");
    }

}
