package com.ibm.cio.opus;

public class OggOpus {
    public static native void initAudio();
    public static native void startRecorder( int sample_rate);
    public static native void stopRecorder();
    public static native int encode( String s, int sample_rate );
    public static native int decode( String s, String o, int sample_rate );
    public static native float volume();
    static {
        System.loadLibrary("oggopus");
    }
}
