package com.ibm.cio.audio;

/**
 * Created by mihui on 9/2/15.
 */
public class SpeechConfiguration {
    // PCM format
    public static final String AUDIO_FORMAT_DEFAULT = "audio/l16;rate=16000";
    // OggOpus format
    public static final String AUDIO_FORMAT_OGGOPUS = "audio/ogg;codecs=opus";

    // Frame size
    public static final int FRAME_SIZE = 160;
    // Sample rate
    public static final int SAMPLE_RATE = 16000;

    // Data format
    public String audioFormat = AUDIO_FORMAT_DEFAULT;
    // Timeout
    public int inactivityTimeout = 600;
    // Authentication flag
    public boolean isAuthNeeded = true;
    // SSL flag
    public boolean isSSL = true;

    public void enableOpusTesting(){
        this.audioFormat = AUDIO_FORMAT_OGGOPUS;
        this.isAuthNeeded = false;
        this.isSSL = false;
    }

    public void enableWavTesting(){
        this.audioFormat = AUDIO_FORMAT_DEFAULT;
        this.isAuthNeeded = false;
        this.isSSL = false;
    }
}
