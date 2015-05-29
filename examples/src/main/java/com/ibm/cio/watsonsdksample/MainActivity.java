package com.ibm.cio.watsonsdksample;

import java.net.URI;
import java.net.URISyntaxException;

import android.app.Activity;
import android.content.Context;
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
import com.ibm.cio.watsonsdk.SpeechDelegate;
import com.ibm.cio.watsonsdk.SpeechToText;
import com.ibm.cio.watsonsdk.SpeechRecorderDelegate;
import com.ibm.cio.watsonsdk.TextToSpeech;

public class MainActivity extends Activity implements SpeechDelegate, SpeechRecorderDelegate{
	private static final String TAG = "MainActivity";

	// API credentials
	private static String USERNAME = "ivaniapi";
	private static String PASSWORD = "Zt1xSp33x";
    // SDK Service URL
    private static String STT_URL = "wss://speech.tap.ibm.com/speech-to-text-beta/api";
	private static String TTS_URL = "https://speech.tap.ibm.com/text-to-speech-beta/api";

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

	
	/**
	 * Initializing instance of SpeechToText and configuring the rest of parameters
	 */
	private void initSpeechRecognition(){
		//STT
		SpeechToText.sharedInstance().initWithContext(this.getHost(STT_URL), this.getApplicationContext());
        SpeechToText.sharedInstance().setUsername(this.USERNAME);
        SpeechToText.sharedInstance().setPassword(this.PASSWORD);
        SpeechToText.sharedInstance().setDelegate(this);
//		SpeechToText.sharedInstance().setTimeout(0); // Optional - set the duration for delaying connection closure in millisecond
		//TTS
		TextToSpeech.sharedInstance().initWithContext(this.getHost(TTS_URL), this.getApplicationContext());
		TextToSpeech.sharedInstance().setUsername(this.USERNAME);
		TextToSpeech.sharedInstance().setPassword(this.PASSWORD);

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
		TextToSpeech.sharedInstance().voices();
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
