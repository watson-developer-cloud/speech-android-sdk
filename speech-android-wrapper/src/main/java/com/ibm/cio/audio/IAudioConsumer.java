package com.ibm.cio.audio;

/**
 * Created by daniel on 8/24/15.
 */
public interface IAudioConsumer {
    // Use PROPRIETARY notice if class contains a main() method, otherwise use COPYRIGHT notice.
    public static final String COPYRIGHT_NOTICE = "(c) Copyright IBM Corp. 2015";
    // function that consumes the audio data
    public void consume(byte [] data);
    public void onAmplitude(double amplitude, double volume);
}
