package com.ibm.cio.dto;

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
    // VAD flag
    public boolean isUsingVAD = false;

    // This method is only used for testing purpose
    public void enableOpusTesting(){
        this.audioFormat = AUDIO_FORMAT_OGGOPUS;
        this.isAuthNeeded = false;
        this.isSSL = false;
        this.isUsingVAD = false;
    }

    // This method is only used for testing purpose
    public void enableWavTesting(){
        this.audioFormat = AUDIO_FORMAT_DEFAULT;
        this.isAuthNeeded = false;
        this.isSSL = false;
        this.isUsingVAD = false;
    }
}
