package com.ibm.cio.watsonsdk;

import android.content.Context;
import android.util.Log;

import com.ibm.cio.util.TTSPlugin;

import java.net.URI;

/**Speech Recognition Class for SDK functions
 * @author Viney Ugave (vaugave@us.ibm.com)
 *
 */
public class TextToSpeech {

    protected static final String TAG = "TextToSpeech";

    private TTSPlugin ttsPlugin;

    private Context appCtx;

    private String username;
    private String password;
    private URI hostURL;

    /**Speech Recognition Shared Instance
     *
     */
    private static TextToSpeech _instance = null;

    public static TextToSpeech sharedInstance(){
        if(_instance == null){
            synchronized(SpeechToText.class){
                _instance = new TextToSpeech();
            }
        }
        return _instance;
    }

    /**
     * Init the shared instance with the context when VAD is being used
     * @param uri
     * @param ctx
     */
    public void initWithContext(URI uri, Context ctx){
        this.setHostURL(uri);
        this.appCtx = ctx;

    }

    public void synthesize(String ttsString) {
        Log.i(TAG, "synthesize called");
        String[] Arguments = { this.hostURL.toString(), this.username, this.password,
                ttsString};
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public URI getHostURL() {
        return hostURL;
    }

    public void setHostURL(URI hostURL) {
        this.hostURL = hostURL;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Context getAppCtx() {
        return appCtx;
    }

    public void setAppCtx(Context appCtx) {
        this.appCtx = appCtx;
    }

}
