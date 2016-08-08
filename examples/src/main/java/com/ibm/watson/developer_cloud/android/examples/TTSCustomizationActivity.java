package com.ibm.watson.developer_cloud.android.examples;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.util.Log;
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
import com.ibm.watson.developer_cloud.android.text_to_speech.v1.dto.TTSCustomVoiceUpdate;
import com.ibm.watson.developer_cloud.android.text_to_speech.v1.dto.TTSCustomWord;
import com.ibm.watson.developer_cloud.android.text_to_speech.v1.dto.TTSVoiceCustomization;
import com.ibm.watson.developer_cloud.android.text_to_speech.v1.dto.TTSCustomization;
import com.ibm.watson.developer_cloud.android.text_to_speech.v1.dto.TTSVoiceCustomizations;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class TTSCustomizationActivity extends Activity {
    private static final String TAG = "TTSCustomization";

    private static final String TTS_CUSTOM_VOICE_LANGUAGE_EN_US = "en-US";
    private static final int SHOW_CUSTOMIZATION_DETAILS_ACTIVITY = 3;

    private ListView voicesListView = null;
    private MyVoicesAdapter adapter = null;

    private static final int ITEM_CANCEL = 1;
    private static final int ITEM_DELETE = 2;
    private int selectedItemIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ttscustomization);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissKeyboard();
            }
        };

        this.setText();
        this.setupUI(listener);
    }

    private void dismissKeyboard(){
        InputMethodManager imm =  (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if(imm != null) {
            imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
        }
    }

    private void setupUI(View.OnClickListener listener) {
        TTSConfiguration tConfig = SpeechHelper.makeTTSConfigWithContext(this.getApplicationContext());
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

        final Button langButton = (Button)findViewById(R.id.languageButton);
        langButton.setText(TTS_CUSTOM_VOICE_LANGUAGE_EN_US);
        langButton.setOnClickListener(listener);

        final EditText nameText = (EditText)findViewById(R.id.nameField);
        final EditText descText = (EditText)findViewById(R.id.descField);

        Button createButton = (Button)findViewById(R.id.createButton);
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissKeyboard();
                TTSVoiceCustomization customVoice = new TTSVoiceCustomization();
                customVoice.name = nameText.getText().toString();
                customVoice.description = descText.getText().toString();
                customVoice.language = langButton.getText().toString();

                JSONObject result = TextToSpeech.sharedInstance().createVoiceModelWithCustomVoice(customVoice);
                if(result == null) {
                    Toast.makeText(getApplicationContext(), "Create " + customVoice.name + " failed.", Toast.LENGTH_SHORT).show();
                }
                else {
                    try {
                        String customizationId = result.getString("customization_id");
                        TTSCustomVoiceUpdate ttsCustomVoiceUpdate = new TTSCustomVoiceUpdate();

                        ttsCustomVoiceUpdate.name = customVoice.name;
                        ttsCustomVoiceUpdate.description = customVoice.description;
                        ttsCustomVoiceUpdate.words = new ArrayList<>();
                        ttsCustomVoiceUpdate.words.add(new TTSCustomWord("UT", "Utilization"));
                        ttsCustomVoiceUpdate.words.add(new TTSCustomWord("MIHUI", "Me Who A"));
                        ttsCustomVoiceUpdate.words.add(new TTSCustomWord("LOL", "Laughing out loud"));
                        ttsCustomVoiceUpdate.words.add(new TTSCustomWord("IEEE", "I triple E"));

                        boolean isUpdated = TextToSpeech.sharedInstance().updateVoiceModelWithCustomVoice(customizationId ,ttsCustomVoiceUpdate);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    adapter.customizations = produceVoiceCustomizations();
                    adapter.notifyDataSetChanged();
                    Toast.makeText(getApplicationContext(), "Created " + customVoice.name + " successfully.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        TTSVoiceCustomizations customizations = this.produceVoiceCustomizations();

        this.voicesListView = (ListView)findViewById(R.id.listView);

        this.adapter = new MyVoicesAdapter(customizations, tConfig.appContext, listener);
        this.voicesListView.setAdapter(this.adapter);

        this.voicesListView.setLongClickable(true);
        this.voicesListView.setOnItemLongClickListener(new ListView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                selectedItemIndex = position;
                voicesListView.showContextMenu();
                return true;
            }
        });
        this.voicesListView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                menu.setHeaderTitle("Please select Delete or Cancel");
                menu.add(0, ITEM_DELETE, 0, "Delete");
                menu.add(0, ITEM_CANCEL, 1, "Cancel");
            }
        });
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
        dismissKeyboard();

        TTSCustomization details = adapter.customizations.customizations.get(this.selectedItemIndex);
        boolean isDeleted = TextToSpeech.sharedInstance().deleteVoiceModel(details.customization_id);

        if(isDeleted) {
            adapter.customizations.customizations.remove(this.selectedItemIndex);
            adapter.notifyDataSetChanged();
            Toast.makeText(getApplicationContext(), "Deleted " + details.name + " successfully.", Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(getApplicationContext(), "Delete " + details.name + " failed.", Toast.LENGTH_SHORT).show();
        }
    }

    private TTSVoiceCustomizations produceVoiceCustomizations() {
        TTSVoiceCustomizations obj = new TTSVoiceCustomizations();
        JSONObject voices = TextToSpeech.sharedInstance().getCustomizedVoiceModels();
        try {
            JSONArray voiceList = voices.getJSONArray("customizations");
            for(int i = 0; i < voiceList.length(); i++) {

                TTSCustomization details = new TTSCustomization();
                details.name = voiceList.getJSONObject(i).getString("name");
                details.description = voiceList.getJSONObject(i).getString("description");
                details.customization_id = voiceList.getJSONObject(i).getString("customization_id");
                details.language = voiceList.getJSONObject(i).getString("language");
                details.last_modified = voiceList.getJSONObject(i).getString("last_modified");
                details.owner = voiceList.getJSONObject(i).getString("owner");
                details.created = voiceList.getJSONObject(i).getString("created");

                obj.customizations.add(details);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    protected void setText() {
        Typeface roboto = Typeface.createFromAsset(getApplicationContext().getAssets(), "font/Roboto-Bold.ttf");
        Typeface notosans = Typeface.createFromAsset(getApplicationContext().getAssets(), "font/NotoSans-Regular.ttf");

        TextView viewTitle = (TextView)findViewById(R.id.ttsCustomizationTitle);
        String strTitle = getString(R.string.ttsTitle);
        SpannableString spannable = new SpannableString(strTitle);
        spannable.setSpan(new AbsoluteSizeSpan(47), 0, strTitle.length(), 0);
        spannable.setSpan(new CustomTypefaceSpan("", roboto), 0, strTitle.length(), 0);
        viewTitle.setText(spannable);
        viewTitle.setTextColor(0xFF325C80);

        TextView viewInstructions = (TextView)findViewById(R.id.ttsCustomizationDesc);
        String strInstructions = getString(R.string.ttsInstructions);
        SpannableString spannable2 = new SpannableString(strInstructions);
        spannable2.setSpan(new AbsoluteSizeSpan(30), 0, strInstructions.length(), 0);
        spannable2.setSpan(new CustomTypefaceSpan("", notosans), 0, strInstructions.length(), 0);
        viewInstructions.setText(spannable2);
        viewInstructions.setTextColor(0xFF121212);
    }

    private class MyVoicesAdapter extends BaseAdapter {
        private TTSVoiceCustomizations customizations;
        private Context context;
        private LayoutInflater mInflater;
        private View.OnClickListener clickListener;

        public MyVoicesAdapter(TTSVoiceCustomizations dataSource, Context parentContext, View.OnClickListener listener){
            this.customizations = dataSource;
            this.context = parentContext;
            this.mInflater = LayoutInflater.from(parentContext);
            this.clickListener = listener;
        }

        @Override
        public int getCount() {
            return this.customizations.customizations.size();
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
            convertView = mInflater.inflate(R.layout.activity_ttscustomization_listitem, null);
            final TTSCustomization voiceModel = this.customizations.customizations.get(position);
            TextView titleTextView = (TextView) convertView.findViewById(R.id.titleTextView);
            TextView descriptionTextView = (TextView) convertView.findViewById(R.id.descriptionTextView);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            Calendar.getInstance().setTimeInMillis(Long.parseLong(voiceModel.created) * 1000);
            String date = sdf.format(Calendar.getInstance().getTime());

            titleTextView.setText("["+voiceModel.language+"] " + voiceModel.name);
            descriptionTextView.setText("["+date+"] " + voiceModel.description);

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickListener.onClick(v);
                    gotoTTSCustomizationDetails(voiceModel.customization_id);
                }
            });

            return convertView;
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode){
            case SHOW_CUSTOMIZATION_DETAILS_ACTIVITY:
                if(resultCode == RESULT_OK){
                    // OK
                }
                else if(resultCode == RESULT_FIRST_USER){
                    // defined success/error
                }
                else{
                    // user cancelled
                }
                break;
        }
    }
    /**
     * Got to customization view
     * @param customization_id
     */
    public void gotoTTSCustomizationDetails(String customization_id) {
        Log.e(TAG, "gotoTTSCustomizationDetails+"+customization_id);
        Intent intent = new Intent();
        intent.putExtra("customization_id", customization_id);
        intent.setClass(TTSCustomizationActivity.this, TTSCustomizationDetailsActivity.class);
        startActivityForResult(intent, SHOW_CUSTOMIZATION_DETAILS_ACTIVITY);
        overridePendingTransition(R.anim.slide_from_right, R.anim.slide_out_to_left);
    }
}
