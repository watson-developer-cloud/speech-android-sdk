package com.ibm.cio.watsonsdksample;

import java.io.IOException;
import java.io.InputStream;

import java.io.StringWriter;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.ibm.cio.dto.SpeechConfiguration;
import com.ibm.cio.dto.QueryResult;
import com.ibm.cio.util.Logger;
import com.ibm.cio.watsonsdk.SpeechDelegate;
import com.ibm.cio.watsonsdk.SpeechToText;
import com.ibm.cio.watsonsdk.SpeechRecorderDelegate;
import com.ibm.cio.watsonsdk.TextToSpeech;
import com.ibm.cio.watsonsdk.TokenProvider;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity implements SpeechDelegate, SpeechRecorderDelegate {
	private static final String TAG = "MainActivity";

    // staging
    //private static String USERNAME_STT = "c9122908-2741-4610-93b9-f33a731ba920";
    //private static String PASSWORD_STT = "74jxojn8LV9i";
    // production
    private static String USERNAME_STT = "67158fa8-a9fb-4380-8368-d3f883da44fc";
    private static String PASSWORD_STT = "KOWFDbj9cG0B";
    // staging
    private static String USERNAME_TTS = "bdb86865-60a4-4e42-bfa8-4c91dfd583d2";
    private static String PASSWORD_TTS = "L3MIsuh4AGpz";

    private static String strSTTTokenFactoryURL = "http://speech-to-text-demo.mybluemix.net/token";
    private static String strTTSTokenFactoryURL = "http://text-to-speech-nodejs-tokenfactory.mybluemix.net/token";

    //private static String STT_URL = "wss://speech.tap.ibm.com/speech-to-text-beta/api";
    //private static String TTS_URL = "https://speech.tap.ibm.com/text-to-speech-beta/api";
//    private static String STT_URL = "wss://stream-s.watsonplatform.net/speech-to-text/api";

    private static String STT_URL = "wss://stream.watsonplatform.net/speech-to-text/api";
    private static String TTS_URL = "https://stream.watsonplatform.net/text-to-speech/api";

	TextView textResult;
	TextView textTTS;

	// Main UI Thread Handler
	private Handler handler = null;
    private boolean mRecognizing = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Strictmode needed to run the http/wss request for devices > Gingerbread
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

		setContentView(R.layout.activity_main);

        //Initialize the speech service
		this.initSpeechRecognition();

        addItemsOnSpinnerModels();
        addItemsOnSpinnerVoices();

		handler = new Handler();
	}

    /**
     * Initializing instance of SpeechToText and configuring the rest of parameters
     */
    private void initSpeechRecognition() {
        // Configuration
        SpeechConfiguration sConfig = new SpeechConfiguration();

        //STT
        SpeechToText.sharedInstance().initWithContext(this.getHost(STT_URL), this.getApplicationContext(), sConfig);
        SpeechToText.sharedInstance().setCredentials(this.USERNAME_STT, this.PASSWORD_STT);
        SpeechToText.sharedInstance().setTokenProvider(new EmptyTokenProvider(this.strSTTTokenFactoryURL));
        SpeechToText.sharedInstance().setModel("en-US_BroadbandModel");
        SpeechToText.sharedInstance().setDelegate(this);
//		SpeechToText.sharedInstance().setTimeout(0); // Optional - set the duration for delaying connection closure in millisecond
        //TTS
        TextToSpeech.sharedInstance().initWithContext(this.getHost(TTS_URL));
        TextToSpeech.sharedInstance().setCredentials(this.USERNAME_TTS, this.PASSWORD_TTS);
        TextToSpeech.sharedInstance().setTokenProvider(new MyTokenProvider(this.strTTSTokenFactoryURL));
        TextToSpeech.sharedInstance().setVoice("en-US_MichaelVoice");
    }

    public class ItemModel {

        public JSONObject mObject = null;

        public ItemModel(JSONObject object) {
            mObject = object;
        }

        public String toString() {
            try {
                return mObject.getString("description");
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    public void addItemsOnSpinnerModels() {

        Spinner spinner = (Spinner) findViewById(R.id.spinnerModels);

        JSONObject obj = SpeechToText.sharedInstance().getModels();
        ItemModel [] items = null;
        try {
            JSONArray models = obj.getJSONArray("models");
            items = new ItemModel[models.length()];
            for (int i = 0; i < models.length(); ++i) {
                items[i] = new ItemModel(models.getJSONObject(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ArrayAdapter spinnerArrayAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, items);
        spinner.setAdapter(spinnerArrayAdapter);
    }

    public class ItemVoice {

        public JSONObject mObject = null;

        public ItemVoice(JSONObject object) {
            mObject = object;
        }

        public String toString() {
            try {
                return mObject.getString("name");
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    public void addItemsOnSpinnerVoices() {

        Spinner spinner = (Spinner) findViewById(R.id.spinnerVoices);

        JSONObject obj = TextToSpeech.sharedInstance().getVoices();

        if(obj == null)
            return;
        ItemVoice [] items = null;
        try {
            JSONArray voices = obj.getJSONArray("voices");
            items = new ItemVoice[voices.length()];
            for (int i = 0; i < voices.length(); ++i) {
                items[i] = new ItemVoice(voices.getJSONObject(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ArrayAdapter spinnerArrayAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, items);
        spinner.setAdapter(spinnerArrayAdapter);
    }

    class MyTokenProvider implements TokenProvider {

        String m_strTokenFactoryURL = null;

        public MyTokenProvider(String strTokenFactoryURL) {
            m_strTokenFactoryURL = strTokenFactoryURL;
        }

        public String getToken() {

            Log.d(TAG, "attempting to get a token from: " + m_strTokenFactoryURL);
            try {
                // DISCLAIMER: the application developer should implement an authentication mechanism from the mobile app to the
                // server side app so the token factory in the server only provides tokens to authenticated clients
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

    class EmptyTokenProvider implements TokenProvider {

        public EmptyTokenProvider(String strTokenFactoryURL) {}

        public String getToken() {
            return "";
        }
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
	public void startRecord(View view) throws JSONException {
		Log.d(TAG, "record pressed");
        if (mRecognizing == false) {
            mRecognizing = true;
            Spinner spinner = (Spinner) findViewById(R.id.spinnerModels);
            spinner.setEnabled(false);
            ItemModel item = (ItemModel)spinner.getSelectedItem();
            SpeechToText.sharedInstance().setModel(item.mObject.getString("name"));
            displayStatus("connecting to the STT service...");
            SpeechToText.sharedInstance().recognize();
            SpeechToText.sharedInstance().setRecorderDelegate(this);
        } else {
            mRecognizing = false;
            Spinner spinner = (Spinner) findViewById(R.id.spinnerModels);
            spinner.setEnabled(true);
            SpeechToText.sharedInstance().stopRecognition();
        }
	}

	/**
	 * Display the faces results
	 * 
	 * @param result
	 */
	public void displayResult(final String result){
		final Runnable runnableUi = new Runnable(){  
	        @Override  
	        public void run() {   
	        	SpeechToText.sharedInstance().setTranscript(result);
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
     * Display the status
     *
     * @param status
     */
    public void displayStatus(final String status){
        final Runnable runnableUi = new Runnable(){
            @Override
            public void run() {
                SpeechToText.sharedInstance().setTranscript(status);
                textResult = (TextView) findViewById(R.id.sttStatus);
                textResult.setText(status);
            }
        };
        new Thread(){
            public void run(){
                handler.post(runnableUi);
            }
        }.start();
    }

    /**
     * Change the button's label
     */
    public void setButtonLabel(final int buttonId, final String label) {
        final Runnable runnableUi = new Runnable(){
            @Override
            public void run() {
                Button button = (Button)findViewById(buttonId);
                button.setText(label);
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
	public void playTTS(View view) throws JSONException {

        Spinner spinner = (Spinner) findViewById(R.id.spinnerVoices);
        spinner.setEnabled(false);
        ItemVoice item = (ItemVoice)spinner.getSelectedItem();
        TextToSpeech.sharedInstance().setVoice(item.mObject.getString("name"));

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
        spinner.setEnabled(true);
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
				Logger.i(TAG, "################ receivedMessage.Open, code: " + code + " result: " + result.getTranscript());
                displayStatus("successfully connected to the STT service");
                setButtonLabel(R.id.buttonRecord, "stop recording");
			break;
			case SpeechDelegate.CLOSE:
				Logger.i(TAG, "################ receivedMessage.Close, code: " + code + " result: " + result.getTranscript());
                displayStatus("connection closed");
                setButtonLabel(R.id.buttonRecord, "start recording");
                mRecognizing = false;

				break;
			case SpeechDelegate.ERROR:
                Logger.e(TAG, result.getTranscript());
                displayResult(result.getTranscript());
                mRecognizing = false;
				break;
			case SpeechDelegate.MESSAGE:
				displayResult(result.getTranscript()); // Instant results
				break;
		}
	}

	@Override
	public void onRecording(byte[] rawAudioData) {}
}
