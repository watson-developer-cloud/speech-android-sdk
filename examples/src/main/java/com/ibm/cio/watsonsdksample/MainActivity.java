package com.ibm.cio.watsonsdksample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.ibm.cio.dto.QueryResult;
import com.ibm.cio.util.Logger;
import com.ibm.cio.util.TTSPlugin;
import com.ibm.cio.watsonsdk.SpeechDelegate;
import com.ibm.cio.watsonsdk.SpeechToText;
import com.ibm.cio.watsonsdk.SpeechRecorderDelegate;
import com.ibm.cio.watsonsdk.TextToSpeech;
import com.ibm.cio.watsonsdk.TokenProvider;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

public class MainActivity extends Activity implements SpeechDelegate, SpeechRecorderDelegate {
	private static final String TAG = "MainActivity";

	// API credentials
    //private static String USERNAME_STT = "ivaniapi";
    //private static String PASSWORD_STT = "Zt1xSp33x";
    //private static String USERNAME_TTS = "ivaniapi";
    //private static String PASSWORD_TTS = "Zt1xSp33x";
    private static String USERNAME_STT = "c9122908-2741-4610-93b9-f33a731ba920";
    private static String PASSWORD_STT = "74jxojn8LV9i";
    private static String USERNAME_TTS = "bdb86865-60a4-4e42-bfa8-4c91dfd583d2";
    private static String PASSWORD_TTS = "L3MIsuh4AGpz";

    private static String strSTTTokenFactoryURL = "http://speech-to-text-demo.mybluemix.net/token";
    private static String strTTSTokenFactoryURL = "http://text-to-speech-nodejs-tokenfactory.mybluemix.net/token";

    //private static String STT_URL = "wss://speech.tap.ibm.com/speech-to-text-beta/api";
    //private static String TTS_URL = "https://speech.tap.ibm.com/text-to-speech-beta/api";
    private static String STT_URL = "wss://stream-s.watsonplatform.net/speech-to-text/api";
    private static String TTS_URL = "https://stream-s.watsonplatform.net/text-to-speech/api";

	TextView textResult;
	TextView textTTS;

	// Main UI Thread Handler
	private Handler handler = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Strictmode needed to run the http/wss request for devices > Gingerbread
				if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD) {
					StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
							.permitAll().build();
					StrictMode.setThreadPolicy(policy);
				}
				
		setContentView(R.layout.activity_main);

        //Initialize the speech service
		this.initSpeechRecognition();

		handler = new Handler();
	}

    class MyTokenProvider implements TokenProvider {

        String m_strTokenFactoryURL = null;

        public MyTokenProvider(String strTokenFactoryURL) {
            m_strTokenFactoryURL = strTokenFactoryURL;
        }

        public String getToken() {

            Log.d(TAG, "trying to get token from: " + m_strTokenFactoryURL);
            try {
                HttpClient httpClient = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(m_strTokenFactoryURL);
                HttpResponse executed = httpClient.execute(httpGet);
                InputStream is = executed.getEntity().getContent();
                StringWriter writer = new StringWriter();
                IOUtils.copy(is, writer, "UTF-8");
                String strToken = writer.toString();
                Log.d(TAG, strToken);
                return strToken;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }
	
	/**
	 * Initializing instance of SpeechToText and configuring the rest of parameters
	 */
	private void initSpeechRecognition() {
		//STT
		SpeechToText.sharedInstance().initWithContext(this.getHost(STT_URL), this.getApplicationContext());
        SpeechToText.sharedInstance().setCredentials(this.USERNAME_STT,this.PASSWORD_STT);
        SpeechToText.sharedInstance().setTokenProvider(new MyTokenProvider(this.strSTTTokenFactoryURL));
        SpeechToText.sharedInstance().setModel("en-US_BroadbandModel");
        SpeechToText.sharedInstance().setDelegate(this);
//		SpeechToText.sharedInstance().setTimeout(0); // Optional - set the duration for delaying connection closure in millisecond
		//TTS
		TextToSpeech.sharedInstance().initWithContext(this.getHost(TTS_URL), this.getApplicationContext());
		TextToSpeech.sharedInstance().setCredentials(this.USERNAME_TTS,this.PASSWORD_TTS);
        TextToSpeech.sharedInstance().setTokenProvider(new MyTokenProvider(this.strTTSTokenFactoryURL));
        //TextToSpeech.sharedInstance().setTokenProvider(new MyTokenProvider(this.strSTTTokenFactoryURL));
        TextToSpeech.sharedInstance().setVoice("en-US_MichaelVoice");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/**
	 * Called when the user clicks the Send button
	 * 
	 * @param view
	 */
	public void startRecord(View view) {
		Log.d(TAG, "record pressed");
		displayResult(SpeechDelegate.MESSAGE, "Result:");
		SpeechToText.sharedInstance().recognize();
		SpeechToText.sharedInstance().setRecorderDelegate(this);
	}

	/**
	 * Display the faces results
	 * 
	 * @param result
	 */
	public void displayResult(int code, final String result){
		final Runnable runnableUi = new Runnable(){  
	        @Override  
	        public void run() {   
	        	SpeechToText.sharedInstance().transcript = result;
	        	textResult = (TextView) findViewById(R.id.textResult);
	    		textResult.setText(result);
	        }
	    };
		new Thread(){  
            public void run(){    
                handler.post(runnableUi);
            }
        }.start();
	}

	/**
	 * Play TTS Audio data
	 * 
	 * @param view
	 */
	public void playTTS(View view){
		//Get text from text box
		textTTS = (TextView) findViewById(R.id.editText_TTS);
		String ttsText=textTTS.getText().toString();
		Logger.i(TAG, ttsText);
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(textTTS.getWindowToken(),
				InputMethodManager.HIDE_NOT_ALWAYS);

		//Call the sdk function
		TextToSpeech.sharedInstance().getVoices();
		TextToSpeech.sharedInstance().synthesize(ttsText);
	}
	
	public URI getHost(String url){
		try {
			return new URI(url);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Delegate function, receive messages from the Speech SDK
	 */
	@Override
	public void receivedMessage(int code, QueryResult result) {
		switch(code){
			case SpeechDelegate.OPEN:
				Logger.i(TAG, "################ receivedMessage.Open");
			break;
			case SpeechDelegate.CLOSE:
//				displayResult(result.getStatusCode(), result.getTranscript());
				Logger.i(TAG, "################ receivedMessage.Close"); // Final results
				break;
			case SpeechDelegate.ERROR:
				Logger.e(TAG, result.getTranscript());
				break;
			case SpeechDelegate.MESSAGE:
				displayResult(result.getStatusCode(), result.getTranscript()); // Instant results
				break;
		}
	}
	@Override
	public void onRecordingCompleted(byte[] rawAudioData) {
		// TODO Auto-generated method stub
//		Logger.e(TAG, "###"+rawAudioData.length+"###");
	}
}
