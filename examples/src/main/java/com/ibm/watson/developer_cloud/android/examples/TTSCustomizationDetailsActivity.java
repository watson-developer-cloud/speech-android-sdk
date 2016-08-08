package com.ibm.watson.developer_cloud.android.examples;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ibm.watson.developer_cloud.android.text_to_speech.v1.ITextToSpeechDelegate;
import com.ibm.watson.developer_cloud.android.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.android.text_to_speech.v1.dto.TTSConfiguration;
import com.ibm.watson.developer_cloud.android.text_to_speech.v1.dto.TTSCustomizationWords;
import com.ibm.watson.developer_cloud.android.text_to_speech.v1.dto.TTSCustomWord;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by mihui on 7/4/16.
 */
public class TTSCustomizationDetailsActivity extends Activity {
    private static String TAG = "TTSCustomizationDetailsActivity";

    private TTSConfiguration tConfig = null;

    private String customizationId = "";
    private ListView wordsListView = null;
    private int selectedItemIndex = 0;
    private static final int ITEM_CANCEL = 1;
    private static final int ITEM_DELETE = 2;

    private MyWordsAdapter adapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ttscustomization_details);

        Intent intent = getIntent();
        this.customizationId = intent.getStringExtra("customization_id");

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm =  (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                if(imm != null) {
                    imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
                }
            }
        };

        this.setText(listener);
        this.setupUI(listener);
    }

    private void setText(View.OnClickListener listener) {
        Typeface roboto = Typeface.createFromAsset(getApplicationContext().getAssets(), "font/Roboto-Bold.ttf");
        Typeface notosans = Typeface.createFromAsset(getApplicationContext().getAssets(), "font/NotoSans-Regular.ttf");

        TextView viewTitle = (TextView)findViewById(R.id.ttsCustomizationDetailsTitle);
        String strTitle = getString(R.string.ttsTitle);
        SpannableString spannable = new SpannableString(strTitle);
        spannable.setSpan(new AbsoluteSizeSpan(47), 0, strTitle.length(), 0);
        spannable.setSpan(new CustomTypefaceSpan("", roboto), 0, strTitle.length(), 0);
        viewTitle.setText(spannable);
        viewTitle.setTextColor(0xFF325C80);

        TextView viewInstructions = (TextView)findViewById(R.id.ttsCustomizationDetailsDesc);
        String strInstructions = getString(R.string.ttsInstructions);
        SpannableString spannable2 = new SpannableString(strInstructions);
        spannable2.setSpan(new AbsoluteSizeSpan(30), 0, strInstructions.length(), 0);
        spannable2.setSpan(new CustomTypefaceSpan("", notosans), 0, strInstructions.length(), 0);
        viewInstructions.setText(spannable2);
        viewInstructions.setTextColor(0xFF121212);
    }

    private void setupUI(View.OnClickListener listener) {
        tConfig = SpeechHelper.makeTTSConfigWithContext(this.getApplicationContext());
        TextToSpeech.sharedInstance().initWithConfig(tConfig, new ITextToSpeechDelegate() {
            @Override
            public void onTTSStart() {

            }

            @Override
            public void onTTSWillPlay() {

            }

            @Override
            public void onTTSStopped() {

            }

            @Override
            public void onTTSError(int statusCode) {

            }
        });

        Button addButton = (Button)findViewById(R.id.addWord);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissKeyboard();
                //
                EditText wordField = (EditText)findViewById(R.id.wordTxt);
                EditText translationField = (EditText)findViewById(R.id.wordTranslation);
                TTSCustomWord ttsCustomWord = new TTSCustomWord(wordField.getText().toString(), translationField.getText().toString());

                boolean isAdded = TextToSpeech.sharedInstance().addWord(customizationId, ttsCustomWord);
                if(isAdded) {
                    adapter.customizationWords.words.add(ttsCustomWord);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(getApplicationContext(), "Added " + ttsCustomWord.word + " successfully.", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(getApplicationContext(), "Add " + ttsCustomWord.word + " failed.", Toast.LENGTH_SHORT).show();
                }
            }
        });


        TTSCustomizationWords ttsCustomizationWords = this.produceCustomizationWords();

        this.wordsListView = (ListView)findViewById(R.id.wordListView);

        this.adapter = new MyWordsAdapter(ttsCustomizationWords, tConfig.appContext, listener);
        this.wordsListView.setAdapter(this.adapter);
        this.wordsListView.setLongClickable(true);
        this.wordsListView.setOnItemLongClickListener(new ListView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                selectedItemIndex = position;
                wordsListView.showContextMenu();
                return true;
            }
        });

        this.wordsListView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                menu.setHeaderTitle("Please select Delete or Cancel");
                menu.add(0, ITEM_DELETE, 0, "Delete");
                menu.add(0, ITEM_CANCEL, 1, "Cancel");
            }
        });
    }

    private void dismissKeyboard() {
        InputMethodManager imm =  (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if(imm != null) {
            imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case ITEM_CANCEL:
                break;
            case ITEM_DELETE:
                this.onDeletingItem();
                break;
        }
        return false;
    }

    private void onDeletingItem() {
        TTSCustomWord ttsCustomWord = adapter.customizationWords.words.get(this.selectedItemIndex);
        boolean isDeleted = TextToSpeech.sharedInstance().deleteWord(this.customizationId, ttsCustomWord);

        if(isDeleted) {
            adapter.customizationWords.words.remove(this.selectedItemIndex);
            adapter.notifyDataSetChanged();
            Toast.makeText(getApplicationContext(), "Deleted " + ttsCustomWord.word + " successfully.", Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(getApplicationContext(), "Delete " + ttsCustomWord.word + " failed.", Toast.LENGTH_SHORT).show();
        }
    }

    private TTSCustomizationWords produceCustomizationWords() {
        TTSCustomizationWords details = new TTSCustomizationWords();
        details.words = new ArrayList<>();

        JSONObject voices = TextToSpeech.sharedInstance().getCustomizedWordList(this.customizationId);
        try {
            JSONArray wordList = voices.getJSONArray("words");

            for(int i = 0; i < wordList.length(); i++) {
                TTSCustomWord ttsCustomWord = new TTSCustomWord();
                ttsCustomWord.word = wordList.getJSONObject(i).getString("word");
                ttsCustomWord.translation = wordList.getJSONObject(i).getString("translation");

                details.words.add(ttsCustomWord);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return details;
    }


    private class MyWordsAdapter extends BaseAdapter {
        private TTSCustomizationWords customizationWords;

        private Context context = null;
        private LayoutInflater mInflater;
        private View.OnClickListener clickListener;

        public MyWordsAdapter(TTSCustomizationWords dataSource, Context parentContext, View.OnClickListener listener){
            this.customizationWords = dataSource;
            this.context = parentContext;
            this.mInflater = LayoutInflater.from(parentContext);
            this.clickListener = listener;
        }

        @Override
        public int getCount() {
            return this.customizationWords.words.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = mInflater.inflate(R.layout.activity_ttscustomization_details_listitem, null);
            final TTSCustomWord word = this.customizationWords.words.get(position);
            TextView titleTextView = (TextView) convertView.findViewById(R.id.titleTextView);
            TextView descriptionTextView = (TextView) convertView.findViewById(R.id.descriptionTextView);

            titleTextView.setText(word.word);
            descriptionTextView.setText(word.translation);

            Button oldTTSButton = (Button) convertView.findViewById(R.id.oldTTS);
            oldTTSButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TextToSpeech.sharedInstance().synthesize(word.word);
                }
            });

            Button newTTSButton = (Button) convertView.findViewById(R.id.newTTS);
            newTTSButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TextToSpeech.sharedInstance().synthesize(word.word, customizationId);
                }
            });

            return convertView;
        }
    }
}
