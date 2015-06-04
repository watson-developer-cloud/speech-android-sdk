package com.ibm.cio.opus;

import com.ibm.cio.util.Logger;

public class OggOpus {
    private static final String TAG = OggOpus.class.getSimpleName().toString();

    public static native int decode( String s, String o, int srate );

    static {
        System.loadLibrary("oggopus");
        Logger.e(TAG, "OggOpus library is loaded...");
    }
}
