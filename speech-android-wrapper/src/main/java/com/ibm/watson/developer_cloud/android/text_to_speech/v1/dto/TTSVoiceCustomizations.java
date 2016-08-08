package com.ibm.watson.developer_cloud.android.text_to_speech.v1.dto;

import java.util.ArrayList;

/**
 * Created by mihui on 6/29/16.
 */
public class TTSVoiceCustomizations {
    public ArrayList<TTSCustomization> customizations;
    public TTSVoiceCustomizations(){
        customizations = new ArrayList<>();
    }
}
