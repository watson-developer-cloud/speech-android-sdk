package com.ibm.cio.opus;

import com.ibm.cio.util.Logger;

public class OpusDecoder {
    private static final String TAG = OpusDecoder.class.getSimpleName().toString();

    public static native int decode( String s, String o, int srate );

    static {
        System.loadLibrary("OpusDecode");
        Logger.e(TAG, "Opus decoder loaded...");
    }
}
