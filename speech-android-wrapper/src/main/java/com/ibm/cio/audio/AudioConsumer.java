package com.ibm.cio.audio;

/**
 * Created by daniel on 8/24/15.
 */
public interface AudioConsumer {

    // function that consumes the audio data
    public void consume(byte [] data);
}
