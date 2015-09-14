package com.ibm.cio.watsonsdksample;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Vector;

import android.app.FragmentTransaction;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

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
    //private static String STT_URL = "wss://stream-s.watsonplatform.net/speech-to-text/api";

    //private static String STT_URL = "wss://stream-s.watsonplatform.net/speech-to-text/api";
    private static String STT_URL = "wss://stream.watsonplatform.net/speech-to-text/api";

    //private static String STT_URL = "ws://t430tb.watson.ibm.com:1080/speech-to-text/api";
    private static String TTS_URL = "https://stream-s.watsonplatform.net/text-to-speech/api";

	TextView textResult;
	TextView textTTS;

	// Main UI Thread Handler
	private Handler handler = null;
    private boolean mRecognizing = false;

    ActionBar.Tab tabSTT, tabTTS;
    FragmentTabSTT fragmentTabSTT = new FragmentTabSTT();
    FragmentTabTTS fragmentTabTTS = new FragmentTabTTS();

    public static class FragmentTabSTT extends Fragment {

        public View mView = null;
        public Context mContext = null;
        public JSONObject jsonModels = null;
        private Handler mHandler = null;

        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            mView = inflater.inflate(R.layout.tab_stt, container, false);
            mContext = getActivity().getApplicationContext();

            if (jsonModels == null) {
                jsonModels = new STTCommands().doInBackground();
            }
            addItemsOnSpinnerModels();
            setText();

            /*Button buttonRecord = (Button)mView.findViewById(R.id.buttonRecord);
            buttonRecord.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View arg0) {

                    Log.d(TAG, "onClickRecord");
                    //backward_img.setBackgroundColor(Color.BLUE);
                }
            });*/

            mHandler = new Handler();

            return mView;
        }

        protected void setText() {

            Typeface roboto = Typeface.createFromAsset(getActivity().getApplicationContext().getAssets(), "font/Roboto-Bold.ttf");
            Typeface notosans = Typeface.createFromAsset(getActivity().getApplicationContext().getAssets(), "font/NotoSans-Regular.ttf");

            // title
            TextView viewTitle = (TextView)mView.findViewById(R.id.title);
            String strTitle = getString(R.string.sttTitle);
            SpannableStringBuilder spannable = new SpannableStringBuilder(strTitle);
            spannable.setSpan(new AbsoluteSizeSpan(47), 0, strTitle.length(), 0);
            spannable.setSpan(new CustomTypefaceSpan("", roboto), 0, strTitle.length(), 0);
            viewTitle.setText(spannable);
            viewTitle.setTextColor(0xFF325C80);

            // instructions
            TextView viewInstructions = (TextView)mView.findViewById(R.id.instructions);
            String strInstructions = getString(R.string.sttInstructions);
            SpannableString spannable2 = new SpannableString(strInstructions);
            spannable2.setSpan(new AbsoluteSizeSpan(20), 0, strInstructions.length(), 0);
            spannable2.setSpan(new CustomTypefaceSpan("", notosans), 0, strInstructions.length(), 0);
            viewInstructions.setText(spannable2);
            viewInstructions.setTextColor(0xFF121212);
        }

        protected void addItemsOnSpinnerModels() {

            Spinner spinner = (Spinner)mView.findViewById(R.id.spinnerModels);
            int iIndexDefault = 0;

            JSONObject obj = jsonModels;
            ItemModel [] items = null;
            try {
                JSONArray models = obj.getJSONArray("models");
                // count the number of Broadband models (narrowband models are for telephony data)
                Vector<Integer> v = new Vector<>();
                for (int i = 0; i < models.length(); ++i) {
                    if (models.getJSONObject(i).getString("name").indexOf("Broadband") != -1) {
                        v.add(i);
                    }
                }
                items = new ItemModel[v.size()];
                int iItems = 0;
                for (int i = 0; i < v.size() ; ++i) {
                    items[iItems] = new ItemModel(models.getJSONObject(v.elementAt(i)));
                    if (models.getJSONObject(v.elementAt(i)).getString("name").equals(getString(R.string.modelDefault))) {
                        iIndexDefault = iItems;
                    }
                    ++iItems;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            ArrayAdapter spinnerArrayAdapter = new ArrayAdapter(getActivity(),
                    android.R.layout.simple_spinner_item, items);
            spinner.setAdapter(spinnerArrayAdapter);
            spinner.setSelection(iIndexDefault);
        }

        public void displayResult(final String result){
            final Runnable runnableUi = new Runnable(){
                @Override
                public void run() {
                    SpeechToText.sharedInstance().transcript = result;
                    TextView textResult = (TextView)mView.findViewById(R.id.textResult);
                    textResult.setText(result);
                }
            };

            new Thread(){
                public void run(){
                    mHandler.post(runnableUi);
                }
            }.start();
        }
    }

    public static class FragmentTabTTS extends Fragment {

        public View mView = null;
        public Context mContext = null;
        public JSONObject jsonVoices = null;
        private Handler mHandler = null;

        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            Log.d(TAG, "onCreateTTS");
            mView = inflater.inflate(R.layout.tab_tts, container, false);
            mContext = getActivity().getApplicationContext();

            if (jsonVoices == null) {
                jsonVoices = new TTSCommands().doInBackground();
            }
            addItemsOnSpinnerVoices();
            setText();
            updatePrompt(getString(R.string.voiceDefault));

            Spinner spinner = (Spinner)mView.findViewById(R.id.spinnerVoices);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {

                    Log.d(TAG, "setOnItemSelectedListener");
                    final Runnable runnableUi = new Runnable() {
                        @Override
                        public void run() {
                            FragmentTabTTS.this.updatePrompt(FragmentTabTTS.this.getSelectedVoice());
                        }
                    };
                    new Thread(){
                        public void run(){
                            mHandler.post(runnableUi);
                        }
                    }.start();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                    // your code here
                }
            });

            mHandler = new Handler();

            return mView;
        }

        protected void setText() {

            Typeface roboto = Typeface.createFromAsset(getActivity().getApplicationContext().getAssets(), "font/Roboto-Bold.ttf");
            Typeface notosans = Typeface.createFromAsset(getActivity().getApplicationContext().getAssets(), "font/NotoSans-Regular.ttf");

            TextView viewTitle = (TextView)mView.findViewById(R.id.title);
            String strTitle = getString(R.string.ttsTitle);
            SpannableString spannable = new SpannableString(strTitle);
            spannable.setSpan(new AbsoluteSizeSpan(47), 0, strTitle.length(), 0);
            spannable.setSpan(new CustomTypefaceSpan("", roboto), 0, strTitle.length(), 0);
            viewTitle.setText(spannable);
            viewTitle.setTextColor(0xFF325C80);

            TextView viewInstructions = (TextView)mView.findViewById(R.id.instructions);
            String strInstructions = getString(R.string.ttsInstructions);
            SpannableString spannable2 = new SpannableString(strInstructions);
            spannable2.setSpan(new AbsoluteSizeSpan(20), 0, strInstructions.length(), 0);
            spannable2.setSpan(new CustomTypefaceSpan("", notosans), 0, strInstructions.length(), 0);
            viewInstructions.setText(spannable2);
            viewInstructions.setTextColor(0xFF121212);
        }

        public void addItemsOnSpinnerVoices() {

            Spinner spinner = (Spinner)mView.findViewById(R.id.spinnerVoices);
            int iIndexDefault = 0;

            JSONObject obj = jsonVoices;
            ItemVoice [] items = null;
            try {
                JSONArray voices = obj.getJSONArray("voices");
                items = new ItemVoice[voices.length()];
                for (int i = 0; i < voices.length(); ++i) {
                    items[i] = new ItemVoice(voices.getJSONObject(i));
                    if (voices.getJSONObject(i).getString("name").equals(getString(R.string.voiceDefault))) {
                        iIndexDefault = i;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            ArrayAdapter spinnerArrayAdapter = new ArrayAdapter(getActivity(),
                    android.R.layout.simple_spinner_item, items);
            spinner.setAdapter(spinnerArrayAdapter);
            spinner.setSelection(iIndexDefault);
        }

        // return the selected voice
        public String getSelectedVoice() {

            // return the selected voice
            Spinner spinner = (Spinner)mView.findViewById(R.id.spinnerVoices);
            ItemVoice item = (ItemVoice)spinner.getSelectedItem();
            String strVoice = null;
            try {
                strVoice = item.mObject.getString("name");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return strVoice;
        }

        // update the prompt for the selected voice
        public void updatePrompt(final String strVoice) {

            TextView viewPrompt = (TextView)mView.findViewById(R.id.prompt);
            if (strVoice.startsWith("en-US") || strVoice.startsWith("en-GB")) {
                viewPrompt.setText(getString(R.string.ttsEnglishPrompt));
            } else if (strVoice.startsWith("es-ES")) {
                viewPrompt.setText(getString(R.string.ttsSpanishPrompt));
            } else if (strVoice.startsWith("fr-FR")) {
                viewPrompt.setText(getString(R.string.ttsFrenchPrompt));
            } else if (strVoice.startsWith("it-IT")) {
                viewPrompt.setText(getString(R.string.ttsItalianPrompt));
            } else if (strVoice.startsWith("de-DE")) {
                viewPrompt.setText(getString(R.string.ttsGermanPrompt));
            } else if (strVoice.startsWith("ja-JP")) {
                viewPrompt.setText(getString(R.string.ttsJapanesePrompt));
            }
        }
    }

    public class MyTabListener implements ActionBar.TabListener {
        Fragment fragment;

        public MyTabListener(Fragment fragment) {
            this.fragment = fragment;
        }

        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            ft.replace(R.id.fragment_container, fragment);
        }

        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            ft.remove(fragment);
        }

        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
            // nothing done here
        }
    }


    public static class STTCommands extends AsyncTask<Void, Void, JSONObject> {

        protected JSONObject doInBackground(Void... none) {

            return SpeechToText.sharedInstance().getModels();
        }
    }

    public static class TTSCommands extends AsyncTask<Void, Void, JSONObject> {

        protected JSONObject doInBackground(Void... none) {

            return TextToSpeech.sharedInstance().getVoices();
        }
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        //Initialize the speech service
        this.initSpeechRecognition();

        //JSONObject obj = SpeechToText.sharedInstance().getModels();

		// Strictmode needed to run the http/wss request for devices > Gingerbread
		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
					.permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}
				
		//setContentView(R.layout.activity_main);
        setContentView(R.layout.activity_tab_text);

		handler = new Handler();

        ////////////////////////////////////////

        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        tabSTT = actionBar.newTab().setText("Speech to Text");
        tabTTS = actionBar.newTab().setText("Text to Speech");

        tabSTT.setTabListener(new MyTabListener(fragmentTabSTT));
        tabTTS.setTabListener(new MyTabListener(fragmentTabTTS));

        actionBar.addTab(tabSTT);
        actionBar.addTab(tabTTS);

        //actionBar.setStackedBackgroundDrawable(new ColorDrawable(Color.parseColor("#B5C0D0")));

        //////////////////////////


	}

    public static class ItemModel {

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



    public static class ItemVoice {

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
	
	/**
	 * Initializing instance of SpeechToText and configuring the rest of parameters
	 */
	private void initSpeechRecognition() {
		//STT
		SpeechToText.sharedInstance().initWithContext(this.getHost(STT_URL), this.getApplicationContext(), false);
        SpeechToText.sharedInstance().setCredentials(this.USERNAME_STT,this.PASSWORD_STT);
        //SpeechToText.sharedInstance().setTokenProvider(new MyTokenProvider(this.strSTTTokenFactoryURL));
        SpeechToText.sharedInstance().setModel("en-US_BroadbandModel");
        SpeechToText.sharedInstance().setDelegate(this);
//		SpeechToText.sharedInstance().setTimeout(0); // Optional - set the duration for delaying connection closure in millisecond
		//TTS
		TextToSpeech.sharedInstance().initWithContext(this.getHost(TTS_URL), this.getApplicationContext());
		TextToSpeech.sharedInstance().setCredentials(this.USERNAME_TTS,this.PASSWORD_TTS);
        //TextToSpeech.sharedInstance().setTokenProvider(new MyTokenProvider(this.strTTSTokenFactoryURL));
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

            /*final MainActivity activity = this;
            final Runnable runnableUi = new Runnable(){
                @Override
                public void run() {
                    displayStatus("connecting to the STT service....");
                    SpeechToText.sharedInstance().recognize();
                    SpeechToText.sharedInstance().setRecorderDelegate(activity);
                }
            };
            new Thread(){
                public void run(){
                    handler.post(runnableUi);
                }
            }.start();*/

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
     * Display the status
     *
     * @param status
     */
    public void displayStatus(final String status){
        final Runnable runnableUi = new Runnable(){
            @Override
            public void run() {
                SpeechToText.sharedInstance().transcript = status;
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

        TextToSpeech.sharedInstance().setVoice(fragmentTabTTS.getSelectedVoice());
        Logger.i(TAG, fragmentTabTTS.getSelectedVoice());

		//Get text from text box
		textTTS = (TextView)fragmentTabTTS.mView.findViewById(R.id.prompt);
		String ttsText=textTTS.getText().toString();
		Logger.i(TAG, ttsText);
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(textTTS.getWindowToken(),
				InputMethodManager.HIDE_NOT_ALWAYS);

		//Call the sdk function
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
				Logger.i(TAG, "################ receivedMessage.Open, code: " + code + " result: " + result.getTranscript());
                displayStatus("successfully connected to the STT service");
                setButtonLabel(R.id.buttonRecord, "stop recording");
			break;
			case SpeechDelegate.CLOSE:
				Logger.i(TAG, "################ receivedMessage.Close, code: " + code + " result: " + result.getTranscript());
                displayStatus("connection closed");
                setButtonLabel(R.id.buttonRecord, "start recording");
				break;
			case SpeechDelegate.ERROR:
				Logger.e(TAG, result.getTranscript());
				fragmentTabSTT.displayResult(result.getTranscript());
				break;
			case SpeechDelegate.MESSAGE:
				//displayResult(result.getTranscript()); // Instant results
                fragmentTabSTT.displayResult(result.getTranscript());
				break;
		}
	}



	@Override
	public void onRecordingCompleted(byte[] rawAudioData) {
		// TODO Auto-generated method stub
//		Logger.e(TAG, "###"+rawAudioData.length+"###");
	}
}
