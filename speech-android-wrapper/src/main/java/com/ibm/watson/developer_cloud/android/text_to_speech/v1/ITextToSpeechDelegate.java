package com.ibm.watson.developer_cloud.android.text_to_speech.v1;

/**
 * Created by mihui on 5/29/16.
 */
public interface ITextToSpeechDelegate {
    /**
     * This method will be called when TTS request start
     */
    public void onTTSStart();

    /**
     * This method will be called when the TTS audio data is back, but before audio plays
     */
    public void onTTSWillPlay();

    /**
     * This method will be called when the TTS request is finished and audio play stops
     */
    public void onTTSStopped();

    /**
     * This mehtod will be called when there is an error of requesting TTS
     * @param statusCode
     */
    public void onTTSError(int statusCode);
}

