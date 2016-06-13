 /**
  * © Copyright IBM Corporation 2015
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  **/

package com.ibm.watson.developer_cloud.android.examples;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Vector;

import android.app.FragmentTransaction;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
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

// IBM Watson SDK
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.audio.FileCaptureThread;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.dto.STTConfiguration;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.ISpeechToTextDelegate;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.android.text_to_speech.v1.ITextToSpeechDelegate;
import com.ibm.watson.developer_cloud.android.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.android.speech_common.v1.ITokenProvider;
import com.ibm.watson.developer_cloud.android.text_to_speech.v1.dto.TTSConfiguration;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {

	private static final String TAG = "MainActivity";

	TextView textTTS;

    ActionBar.Tab tabSTT, tabTTS;
    FragmentTabSTT fragmentTabSTT = new FragmentTabSTT();
    FragmentTabTTS fragmentTabTTS = new FragmentTabTTS();

    public static class FragmentTabSTT extends Fragment implements ISpeechToTextDelegate {

        // session recognition results
        private static String mRecognitionResults = "";

        private enum ConnectionState {
            IDLE, CONNECTING, CONNECTED
        }

        ConnectionState mState = ConnectionState.IDLE;
        public View mView = null;
        public Context mContext = null;
        public JSONObject jsonModels = null;
        private Handler mHandler = null;
        private FileCaptureThread mFileCaptureThread = null;

        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            mView = inflater.inflate(R.layout.tab_stt, container, false);
            mContext = getActivity().getApplicationContext();
            mHandler = new Handler();

            setText();
            if (initSTT() == false) {
                displayResult("Error: no authentication credentials/token available, please enter your authentication information");
                return mView;
            }

            if (jsonModels == null) {
                jsonModels = new STTCommands().doInBackground();
                if (jsonModels == null) {
                    displayResult("Please, check internet connection.");
                    return mView;
                }
            }
            addItemsOnSpinnerModels();

            displayStatus("please, press the button to start speaking");

            Button buttonRecord = (Button)mView.findViewById(R.id.buttonRecord);
            buttonRecord.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View arg0) {

                    if (mState == ConnectionState.IDLE) {
                        mState = ConnectionState.CONNECTING;
                        Log.d(TAG, "onClickRecord: IDLE -> CONNECTING");
                        Spinner spinner = (Spinner)mView.findViewById(R.id.spinnerModels);
                        spinner.setEnabled(false);
                        mRecognitionResults = "";
                        displayResult(mRecognitionResults);
                        ItemModel item = (ItemModel)spinner.getSelectedItem();
                        SpeechToText.sharedInstance().setModel(item.getModelName());
                        displayStatus("connecting to the STT service...");
                        // start recognition
                        new AsyncTask<Void, Void, Void>(){
                            @Override
                            protected Void doInBackground(Void... none) {
                                SpeechToText.sharedInstance().recognize();
//                                File file = new File(getActivity().getExternalFilesDir(null), "sample.wav");
//                                mFileCaptureThread = SpeechToText.sharedInstance().recognizeWithFile(file);
                                return null;
                            }
                        }.execute();
                        setButtonLabel(R.id.buttonRecord, "Connecting...");
                        setButtonState(true);
                    }
                    else if (mState == ConnectionState.CONNECTED) {
                        mState = ConnectionState.IDLE;
                        Log.d(TAG, "onClickRecord: CONNECTED -> IDLE");
                        Spinner spinner = (Spinner)mView.findViewById(R.id.spinnerModels);
                        spinner.setEnabled(true);
                        SpeechToText.sharedInstance().stopRecognition();
                        setButtonState(false);
                    }
                }
            });

            return mView;
        }

        private String getModelSelected() {

            Spinner spinner = (Spinner)mView.findViewById(R.id.spinnerModels);
            ItemModel item = (ItemModel)spinner.getSelectedItem();
            return item.getModelName();
        }

        // initialize the connection to the Watson STT service
        private boolean initSTT() {
            // DISCLAIMER: please enter your credentials or token factory in the lines below

            STTConfiguration sConfig = new STTConfiguration(STTConfiguration.AUDIO_FORMAT_OGGOPUS);
//            STTConfiguration sConfig = new STTConfiguration(STTConfiguration.AUDIO_FORMAT_DEFAULT);
            sConfig.basicAuthUsername = getString(R.string.STTUsername);
            sConfig.basicAuthPassword = getString(R.string.STTPassword);

            SpeechToText.sharedInstance().initWithConfig(sConfig);

            String tokenFactoryURL = getString(R.string.defaultTokenFactory);
            // token factory is the preferred authentication method (service credentials are not distributed in the client app)
            if (tokenFactoryURL.equals(getString(R.string.defaultTokenFactory)) == false) {
                SpeechToText.sharedInstance().setTokenProvider(new MyTokenProvider(tokenFactoryURL));
            }

            SpeechToText.sharedInstance().setDelegate(this);

            return true;
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

        public class ItemModel {

            private JSONObject mObject = null;

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

            public String getModelName() {
                try {
                    return mObject.getString("name");
                } catch (JSONException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }

        protected void addItemsOnSpinnerModels() {

            Spinner spinner = (Spinner)mView.findViewById(R.id.spinnerModels);
            int iIndexDefault = 0;

            JSONObject obj = jsonModels;
            ItemModel [] items = null;
            try {
                JSONArray models = obj.getJSONArray("models");

                // count the number of Broadband models (narrowband models will be ignored since they are for telephony data)
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
                    if (models.getJSONObject(v.elementAt(i)).getString("name").equals(STTConfiguration.WATSONSDK_DEFAULT_STT_MODEL)) {
                        iIndexDefault = iItems;
                    }
                    ++iItems;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (items != null) {
		        ArrayAdapter<ItemModel> spinnerArrayAdapter = new ArrayAdapter<ItemModel>(getActivity(), android.R.layout.simple_spinner_item, items);
		        spinner.setAdapter(spinnerArrayAdapter);
                spinner.setSelection(iIndexDefault);
            }
        }

        public void displayResult(final String result) {
            final Runnable runnableUi = new Runnable(){
                @Override
                public void run() {
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

        public void displayStatus(final String status) {
            /*final Runnable runnableUi = new Runnable(){
                @Override
                public void run() {
                    TextView textResult = (TextView)mView.findViewById(R.id.sttStatus);
                    textResult.setText(status);
                }
            };
            new Thread(){
                public void run(){
                    mHandler.post(runnableUi);
                }
            }.start();*/
        }

        /**
         * Change the button's label
         */
        public void setButtonLabel(final int buttonId, final String label) {
            final Runnable runnableUi = new Runnable(){
                @Override
                public void run() {
                    Button button = (Button)mView.findViewById(buttonId);
                    button.setText(label);
                }
            };
            new Thread(){
                public void run(){
                    mHandler.post(runnableUi);
                }
            }.start();
        }

        /**
         * Change the button's drawable
         */
        public void setButtonState(final boolean bRecording) {

            final Runnable runnableUi = new Runnable(){
                @Override
                public void run() {
                    int iDrawable = bRecording ? R.drawable.button_record_stop : R.drawable.button_record_start;
                    Button btnRecord = (Button)mView.findViewById(R.id.buttonRecord);
                    btnRecord.setBackground(getResources().getDrawable(iDrawable));
                }
            };
            new Thread(){
                public void run(){
                    mHandler.post(runnableUi);
                }
            }.start();
        }

        // delegages ----------------------------------------------

        public void onOpen() {
            Log.d(TAG, "onOpen");
            displayStatus("successfully connected to the STT service");
            setButtonLabel(R.id.buttonRecord, "Stop recording");
            mState = ConnectionState.CONNECTED;
        }

        public void onBegin(){
            if(mFileCaptureThread != null) {
                mFileCaptureThread.start();
            }
        }

        public void onError(String error) {
            Log.d(TAG, "onError");
            Log.e(TAG, error);
            displayResult(error);
            mState = ConnectionState.IDLE;
            SpeechToText.sharedInstance().stopRecording();
        }

        public void onClose(int code, String reason, boolean remote) {
            Log.d(TAG, "onClose, code: " + code + " reason: " + reason);
            displayStatus("connection closed");
            setButtonLabel(R.id.buttonRecord, "Record");
            mState = ConnectionState.IDLE;
        }

        public void onMessage(String message) {
            try {
                JSONObject resultObject = null;
                resultObject = new JSONObject(message);
                JSONArray resultList = resultObject.getJSONArray("results");
                for (int i = 0; i < resultList.length(); i++) {
                    JSONObject obj = resultList.getJSONObject(i);
                    JSONArray alternativeList = obj.getJSONArray("alternatives");
                    if(alternativeList.length() > 0) {
                        String str = alternativeList.getJSONObject(0).getString("transcript");
                        // remove whitespaces if the language requires it
                        String model = this.getModelSelected();
                        if (model.startsWith("ja-JP") || model.startsWith("zh-CN")) {
                            str = str.replaceAll("\\s+", "");
                        }
                        String strFormatted = Character.toUpperCase(str.charAt(0)) + str.substring(1);
                        boolean isFinal = obj.getString("final").equals("true");

                        if (isFinal) {
                            String stopMarker = (model.startsWith("ja-JP") || model.startsWith("zh-CN")) ? "。" : ". ";
                            mRecognitionResults += strFormatted.substring(0, strFormatted.length() - 1) + stopMarker;

                            displayResult(mRecognitionResults);
                            SpeechToText.sharedInstance().endRecognition();
                        }
                        else {
                            displayResult(mRecognitionResults + strFormatted);
                        }
                    }
                }
            } catch (JSONException e) {
                // data error, and should be handled on SDK level
                e.printStackTrace();
            }
        }

        public void onAmplitude(double amplitude, double volume) {
            //Logger.e(TAG, "amplitude=" + amplitude + ", volume=" + volume);
        }
    }

    public static class FragmentTabTTS extends Fragment implements ITextToSpeechDelegate {

        public View mView = null;
        public Context mContext = null;
        public JSONObject jsonVoices = null;
        private Handler mHandler = null;

        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            Log.d(TAG, "onCreateTTS");
            mView = inflater.inflate(R.layout.tab_tts, container, false);
            mContext = getActivity().getApplicationContext();

            setText();
            if (!initTTS()) {
                TextView viewPrompt = (TextView) mView.findViewById(R.id.prompt);
                viewPrompt.setText("Error: no authentication credentials or token available, please enter your authentication information");
                return mView;
            }

            if (jsonVoices == null) {
                jsonVoices = new TTSCommands().doInBackground();
                if (jsonVoices == null) {
                    return mView;
                }
            }
            addItemsOnSpinnerVoices();
            updatePrompt(TTSConfiguration.WATSONSDK_DEFAULT_TTS_VOICE);

            Spinner spinner = (Spinner) mView.findViewById(R.id.spinnerVoices);
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
                    new Thread() {
                        public void run() {
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

        private boolean initTTS() {
            // DISCLAIMER: please enter your credentials or token factory in the lines below

            TTSConfiguration tConfig = new TTSConfiguration();
            tConfig.basicAuthUsername = getString(R.string.TTSUsername);
            tConfig.basicAuthPassword = getString(R.string.TTSPassword);
            tConfig.codec = TTSConfiguration.CODEC_OPUS;
            tConfig.appContext = this.getActivity().getApplicationContext();
            TextToSpeech.sharedInstance().initWithConfig(tConfig);

            String tokenFactoryURL = getString(R.string.defaultTokenFactory);
            // token factory is the preferred authentication method (service credentials are not distributed in the client app)
            if (tokenFactoryURL.equals(getString(R.string.defaultTokenFactory)) == false) {
                TextToSpeech.sharedInstance().setTokenProvider(new MyTokenProvider(tokenFactoryURL));
            }

            TextToSpeech.sharedInstance().setDelegate(this);
            return true;
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

        @Override
        public void onTTSStart() {
            Log.d(TAG, "### onTTSStart ###");
        }

        @Override
        public void onTTSWillPlay() {
            Log.d(TAG, "### onTTSWillPlay ###");
        }

        @Override
        public void onTTSStopped() {
            Log.d(TAG, "### onTTSStopped ###");
        }

        @Override
        public void onTTSError(int statusCode) {
            Log.d(TAG, "### onTTSError: "+statusCode+" ###");
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

            Spinner spinner = (Spinner)mView.findViewById(R.id.spinnerVoices);
            int iIndexDefault = 0;

            JSONObject obj = jsonVoices;
            ItemVoice [] items = null;
            try {
                JSONArray voices = obj.getJSONArray("voices");
                items = new ItemVoice[voices.length()];
                for (int i = 0; i < voices.length(); ++i) {
                    items[i] = new ItemVoice(voices.getJSONObject(i));
                    if (voices.getJSONObject(i).getString("name").equals(TTSConfiguration.WATSONSDK_DEFAULT_TTS_VOICE)) {
                        iIndexDefault = i;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (items != null) {
		        ArrayAdapter<ItemVoice> spinnerArrayAdapter = new ArrayAdapter<ItemVoice>(getActivity(), android.R.layout.simple_spinner_item, items);
		        spinner.setAdapter(spinnerArrayAdapter);
		        spinner.setSelection(iIndexDefault);
            }
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

		// Strictmode needed to run the http/wss request for devices > Gingerbread
		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
					.permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		//setContentView(R.layout.activity_main);
        setContentView(R.layout.activity_tab_text);

        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        tabSTT = actionBar.newTab().setText("Speech to Text");
        tabTTS = actionBar.newTab().setText("Text to Speech");

        tabSTT.setTabListener(new MyTabListener(fragmentTabSTT));
        tabTTS.setTabListener(new MyTabListener(fragmentTabTTS));

        actionBar.addTab(tabSTT);
        actionBar.addTab(tabTTS);

        //actionBar.setStackedBackgroundDrawable(new ColorDrawable(Color.parseColor("#B5C0D0")));
	}

    static class MyTokenProvider implements ITokenProvider {

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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/**
	 * Play TTS Audio data
	 *
	 * @param view
	 */
	public void playTTS(View view) throws JSONException {

        TextToSpeech.sharedInstance().setVoice(fragmentTabTTS.getSelectedVoice());
        Log.d(TAG, fragmentTabTTS.getSelectedVoice());

		//Get text from text box
		textTTS = (TextView)fragmentTabTTS.mView.findViewById(R.id.prompt);
		String ttsText=textTTS.getText().toString();
		Log.d(TAG, ttsText);
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(textTTS.getWindowToken(),
				InputMethodManager.HIDE_NOT_ALWAYS);

		//Call the sdk function
		TextToSpeech.sharedInstance().synthesize(ttsText);
	}

    /**
     * Got to customization view
     * @param item
     */
    public void gotoTTSCustomization(MenuItem item) {
        Log.e(TAG, "gotoTTSCustomization");
    }
}
