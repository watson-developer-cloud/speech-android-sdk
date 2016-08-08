package com.ibm.watson.developer_cloud.android.examples;

import android.content.Context;
import android.util.Log;

import com.ibm.watson.developer_cloud.android.speech_common.v1.ITokenProvider;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.dto.STTConfiguration;
import com.ibm.watson.developer_cloud.android.text_to_speech.v1.dto.TTSConfiguration;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

/**
 * Created by mihui on 8/6/16.
 */
public class SpeechHelper {

    private static final String TAG = "SpeechHelper";

    private static STTConfiguration sConfig = null;
    public static STTConfiguration makeSTTConfigWithContext(Context context) {
        if(sConfig == null) {
            sConfig = new STTConfiguration(STTConfiguration.AUDIO_FORMAT_OGGOPUS, STTConfiguration.SAMPLE_RATE_OGGOPUS);
//            STTConfiguration sConfig = new STTConfiguration(STTConfiguration.AUDIO_FORMAT_DEFAULT, STTConfiguration.SAMPLE_RATE_DEFAULT);
            sConfig.basicAuthUsername = context.getString(R.string.STTUsername);
            sConfig.basicAuthPassword = context.getString(R.string.STTPassword);

            String tokenFactoryURL = context.getString(R.string.defaultTokenFactory);
            // token factory is the preferred authentication method (service credentials are not distributed in the client app)
            if (!tokenFactoryURL.equals(context.getString(R.string.defaultTokenFactory))) {
                // SpeechToText.sharedInstance().setTokenProvider(new MyTokenProvider(tokenFactoryURL));
                sConfig.setTokenProvider(new MyTokenProvider(tokenFactoryURL));
            }
        }
        return sConfig;
    }

    private static TTSConfiguration tConfig = null;
    public static TTSConfiguration makeTTSConfigWithContext(Context context) {
        if(tConfig == null) {
            tConfig = new TTSConfiguration();
            tConfig.basicAuthUsername = context.getString(R.string.TTSUsername);
            tConfig.basicAuthPassword = context.getString(R.string.TTSPassword);
            tConfig.codec = TTSConfiguration.CODEC_OPUS;
            tConfig.appContext = context.getApplicationContext();

            String tokenFactoryURL = context.getString(R.string.defaultTokenFactory);
            // token factory is the preferred authentication method (service credentials are not distributed in the client app)
            if (tokenFactoryURL.equals(context.getString(R.string.defaultTokenFactory)) == false) {
//                TextToSpeech.sharedInstance().setTokenProvider(new MyTokenProvider(tokenFactoryURL));
                tConfig.setTokenProvider(new MyTokenProvider(tokenFactoryURL));
            }
        }
        return tConfig;
    }

    static class MyTokenProvider implements ITokenProvider {

        String m_strTokenFactoryURL = null;

        public MyTokenProvider(String strTokenFactoryURL) {
            m_strTokenFactoryURL = strTokenFactoryURL;
        }

        public String getToken() {

            Log.d(TAG, "Attempting to get a token from: " + m_strTokenFactoryURL);
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
}
