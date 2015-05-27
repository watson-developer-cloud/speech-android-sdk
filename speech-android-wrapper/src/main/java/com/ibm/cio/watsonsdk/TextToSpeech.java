package com.ibm.cio.watsonsdk;

import android.util.Log;

import com.ibm.cio.util.TTSPlugin;

/**Speech Recognition Class for SDK functions
 * @author Viney Ugave (vaugave@us.ibm.com)
 *
 */
public class TextToSpeech {

    private TTSPlugin ttsPlugin;

    private String sessionCookie;
    private String speechModel;
    private String vaniService;
    private String itransUsername;
    private String itransPassword;
    private String username;


    public void playTtsForString(String ttsString) {
        Log.i(TAG, "playTtsForString called");
        String[] Arguments = { this.sessionCookie, this.ttsServer,
                this.ttsPort, this.itransUsername, this.itransPassword,
                ttsString, "en_US", "8000", "spx" };
//		for(int i=0;i<Arguments.length;i++){
//			System.out.println(Arguments[i]);
//		}
        try {
            ttsPlugin= new TTSPlugin();
            ttsPlugin.tts(Arguments);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Log.e(TAG, "Error calling TTSplugin");
            e.printStackTrace();
        }
    }
}
