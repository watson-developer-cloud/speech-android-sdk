package com.ibm.watson.developer_cloud.android.text_to_speech.v1.dto;

/**
 * Created by mihui on 8/6/16.
 */
public class TTSCustomWord {
    public String word;
    public String translation;

    public TTSCustomWord() {
        this.word = "";
        this.translation = "";
    }

    public TTSCustomWord(String _word, String _translation) {
        this.word = _word;
        this.translation = _translation;
    }

}
