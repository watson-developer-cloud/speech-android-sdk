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
    // Timeout
    public int inactivityTimeout = 600;

    // Data format
    public String audioFormat = AUDIO_FORMAT_DEFAULT;
    // VAD flag
    public boolean isUsingVAD = false;
    // Authentication flag
    public boolean isAuthNeeded = true;
    // SSL flag, this would be detected automatically
    public boolean isSSL = true;
    /**
     * Instantiate default configuration
     */
    public SpeechConfiguration(){}

    /**
     * Constructing configuration by parameters
     *
     * @param audioFormat
     */
    public SpeechConfiguration(String audioFormat){
        this.audioFormat = audioFormat;
    }

    /**
     * Constructing configuration by parameters
     *
     * @param audioFormat
     * @param isAuthNeeded
     */
    public SpeechConfiguration(String audioFormat, boolean isAuthNeeded){
        this.audioFormat = audioFormat;
        this.isAuthNeeded = isAuthNeeded;
    }
}
