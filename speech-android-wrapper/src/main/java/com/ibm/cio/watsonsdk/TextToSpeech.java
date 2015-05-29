package com.ibm.cio.watsonsdk;

import android.content.Context;
import android.util.Log;

import com.ibm.cio.util.TTSPlugin;

import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

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
        String[] Arguments = { this.hostURL.toString()+"/v1/synthesize", this.username, this.password,
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

    public void voices(){

        try {
            //HTTP GET Client
            HttpClient httpClient = new DefaultHttpClient();
            //Add params
            List<BasicNameValuePair> params = new LinkedList<BasicNameValuePair>();
            params.add(new BasicNameValuePair("accept", "application/json"));
            HttpGet httpGet = new HttpGet(this.hostURL+"/v1/voices"+"?"+ URLEncodedUtils.format(params, "utf-8"));
            httpGet.setHeader(BasicScheme.authenticate(
                    new UsernamePasswordCredentials(this.username, this.password), "UTF-8",
                    false));
            HttpResponse executed = httpClient.execute(httpGet);
            InputStream is=executed.getEntity().getContent();
            Log.d(TAG,getStringFromInputStream(is));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // convert InputStream to String
    private static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));

            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();

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
