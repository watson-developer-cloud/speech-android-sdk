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

package com.ibm.watson.developer_cloud.android.text_to_speech.v1;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Environment;
import android.util.Log;

import com.ibm.watson.developer_cloud.android.speech_common.v1.ITokenProvider;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.audio.PcmWaveWriter;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.opus.OggOpus;
import com.ibm.watson.developer_cloud.android.text_to_speech.v1.dto.TTSConfiguration;
import com.ibm.watson.developer_cloud.android.text_to_speech.v1.dto.TTSCustomVoiceUpdate;
import com.ibm.watson.developer_cloud.android.text_to_speech.v1.dto.TTSVoiceCustomization;
import com.ibm.watson.developer_cloud.android.text_to_speech.v1.dto.TTSCustomWord;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;

import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;

/**
 * Speech Recognition Class for SDK functions
 * @author Viney Ugave (vaugave@us.ibm.com)
  */
public class TextToSpeech {
    protected static final String TAG = "TextToSpeech";
    private String content;

    private int sampleRate;
    private AudioTrack audioTrack;

    private ITextToSpeechDelegate delegate = null;
    private TTSConfiguration tConfig = null;

    public TextToSpeech(){
        // By default, the sample rate would be detected by the SDK if the value is set to zero
        // However, the metadata is not reliable, need to decode at the maximum sample rate
        this.sampleRate = TTSConfiguration.CODEC_OPUS_SAMPLE_RATE;
    }

    /**
     * Speech Recognition Shared Instance
     */
    private static TextToSpeech _instance = null;

    public static TextToSpeech sharedInstance(){
        if(_instance == null){
            synchronized(TextToSpeech.class){
                _instance = new TextToSpeech();
            }
        }
        return _instance;
    }

    /**
     * Init the shared instance with configurations
     * @param config TTSConfiguration
     */
    public void initWithConfig(TTSConfiguration config, ITextToSpeechDelegate delegate){
        TextToSpeech.sharedInstance();
        _instance.tConfig = config;
        _instance.delegate = delegate;
    }

    /**
     * Send request of TTS
     * @param ttsString String
     */
    public void synthesize(String ttsString) {
        this.synthesize(ttsString, null);
    }

    /**
     * Send request of TTS
     * @param ttsString String
     */
    public void synthesize(String ttsString, String customizationId) {
        Log.d(TAG, "Synthesize["+this.tConfig.codec+"]: " + this.tConfig.getSynthesizeURL());

        this.content = ttsString;

        if(this.tConfig.codec.equals(TTSConfiguration.CODEC_WAV)){
            this.sampleRate = TTSConfiguration.CODEC_WAV_SAMPLE_RATE;
        }
        else{
            this.sampleRate = TTSConfiguration.CODEC_OPUS_SAMPLE_RATE;
        }

        this.tConfig.customizationId = customizationId;

        this.delegate.onTTSStart();

        TTSThread thread = new TTSThread();
        thread.setName("TTSThread");
        thread.start();
    }

    public void stopAudio(){
        if (audioTrack != null && audioTrack.getState() != AudioTrack.STATE_UNINITIALIZED ) {
            // IMPORTANT: NOT use stop()
            // For an immediate stop, use pause(), followed by flush() to discard audio data that hasn't been played back yet.
            audioTrack.pause();
            audioTrack.flush();
        }
    }

    private void buildAuthenticationHeader(HttpRequestBase httpGet) {
        if (_instance.tConfig.isAuthNeeded) {
            // use token based authentication if possible, otherwise Basic Authentication will be used
            if (tConfig.hasTokenProvider()) {
                Log.d(TAG, "using token based authentication");
                httpGet.setHeader("X-Watson-Authorization-Token", _instance.tConfig.requestToken());
            }
            else {
                Log.d(TAG, "using basic authentication");
                httpGet.setHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(_instance.tConfig.basicAuthUsername, _instance.tConfig.basicAuthPassword), "UTF-8", false));
            }
        }

        if(_instance.tConfig.xWatsonLearningOptOut) {
            httpGet.setHeader("X-Watson-Learning-Opt-Out", "true");
        }
    }

    public JSONObject getCustomizedVoiceModels() {
        return this.performGetForJSONObject(this.tConfig.getCustomizedVoicesServiceURL());
    }

    public JSONObject getCustomizedWordList(String customizationId) {
        return this.performGetForJSONObject(this.tConfig.getCustomizedWordServiceURL(customizationId));
    }

    public JSONObject getVoices() {
        return this.performGetForJSONObject(this.tConfig.getVoicesServiceURL());
    }

    public JSONObject createVoiceModelWithCustomVoice(TTSVoiceCustomization customVoice) {
        JSONObject parameters = new JSONObject();
        HttpEntity entity;
        try {
            parameters.put("name", customVoice.name);
            parameters.put("description", customVoice.description);
            parameters.put("language", customVoice.language);
            entity = new StringEntity(parameters.toString());
            return this.performPostForJSONObject(this.tConfig.getCustomizedVoicesServiceURL(), entity);
        } catch (JSONException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean updateVoiceModelWithCustomVoice(String customizationId, TTSCustomVoiceUpdate ttsCustomVoiceUpdate){
        String urlString;

        JSONObject parameters = new JSONObject();
        HttpEntity entity;
        try {
            urlString = this.tConfig.getCustomizedWordServiceURL(customizationId);

            JSONArray words = new JSONArray();

            for (TTSCustomWord word : ttsCustomVoiceUpdate.words) {
                JSONObject obj = new JSONObject();
                obj.put("word", word.word);
                obj.put("translation", word.translation);
                words.put(obj);
            }

            parameters.put("name", ttsCustomVoiceUpdate.name);
            parameters.put("description", ttsCustomVoiceUpdate.description);
            parameters.put("words", words);
            entity = new StringEntity(parameters.toString());

            return this.performPost(urlString, entity);

        } catch (JSONException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean addWord(String customizationId, TTSCustomWord ttsCustomWord){
        String urlString;

        JSONObject parameters = new JSONObject();
        HttpEntity entity;

        try {
            urlString = this.tConfig.getCustomizedWordServiceURL(customizationId) + "/" + URLEncoder.encode(ttsCustomWord.word, "UTF-8");
            parameters.put("translation", ttsCustomWord.translation);
            entity = new StringEntity(parameters.toString());
            return this.performPut(urlString, entity);

        } catch (JSONException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean deleteWord(String customizationId, TTSCustomWord ttsCustomWord){
        try {
            String urlString = this.tConfig.getCustomizedWordServiceURL(customizationId) + "/" + URLEncoder.encode(ttsCustomWord.word, "UTF-8");
            return this.performDelete(urlString);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean deleteVoiceModel(String customizationId){
        return this.performDelete(this.tConfig.getCustomizedVoicesServiceURL(customizationId));
    }

    private JSONObject performPostForJSONObject(String url, HttpEntity entity) {
        JSONObject object = null;

        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);

            buildAuthenticationHeader(httpPost);

            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(entity);
            HttpResponse executed = httpClient.execute(httpPost);
            InputStream is = executed.getEntity().getContent();

            // get the JSON object containing the models from the InputStream
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder responseStrBuilder = new StringBuilder();
            String inputStr;
            while ((inputStr = streamReader.readLine()) != null)
                responseStrBuilder.append(inputStr);
            object = new JSONObject(responseStrBuilder.toString());
            Log.d(TAG, object.toString());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return object;
    }

    private Boolean performPost(String url, HttpEntity entity) {
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);

            buildAuthenticationHeader(httpPost);

            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(entity);

            HttpResponse executed = httpClient.execute(httpPost);

            int statusCode = executed.getStatusLine().getStatusCode();
            if(statusCode == 200 || statusCode == 201 || statusCode == 204) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private Boolean performPut(String url, HttpEntity entity) {
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPut httpPut = new HttpPut(url);

            buildAuthenticationHeader(httpPut);

            httpPut.setHeader("Content-Type", "application/json");
            httpPut.setEntity(entity);

            HttpResponse executed = httpClient.execute(httpPut);

            int statusCode = executed.getStatusLine().getStatusCode();
            if(statusCode == 200 || statusCode == 201 || statusCode == 204) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private Boolean performDelete(String url) {
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpDelete httpDelete = new HttpDelete(url);

            buildAuthenticationHeader(httpDelete);

            httpDelete.setHeader("Content-Type", "application/json");

            HttpResponse executed = httpClient.execute(httpDelete);

            int statusCode = executed.getStatusLine().getStatusCode();
            if(statusCode == 200 || statusCode == 201 || statusCode == 204) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private JSONObject performGetForJSONObject(String url) {
        JSONObject object = null;

        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(url);

            buildAuthenticationHeader(httpGet);

            httpGet.setHeader("Content-Type", "application/json");
            HttpResponse executed = httpClient.execute(httpGet);
            InputStream is = executed.getEntity().getContent();

            // get the JSON object containing the models from the InputStream
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder responseStrBuilder = new StringBuilder();
            String inputStr;
            while ((inputStr = streamReader.readLine()) != null)
                responseStrBuilder.append(inputStr);
            object = new JSONObject(responseStrBuilder.toString());
            Log.d(TAG, object.toString());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return object;
    }

    /**
     * Set credentials
     * @param username String
     * @param password String
     */
    public void setCredentials(String username, String password) {
        this.tConfig.basicAuthUsername = username;
        this.tConfig.basicAuthPassword = password;
    }

    /**
     * Set token
     * @param token String
     */
    public void setToken(String token) {
        this.tConfig.token = token;
    }
    /**
     * Set token provider (for token based authentication)
     * @see TTSConfiguration class
     */
    public void setTokenProvider(ITokenProvider tokenProvider) { this.tConfig.setTokenProvider(tokenProvider); }

    /**
     * Set TTS voice
     */
    public void setVoice(String voice) {
        this.tConfig.voice = voice;
    }

    /**
     * Get TTS voice
     * @return String
     */
    public String getVoice(){
        return this.tConfig.voice;
    }

    /**
     * Post text data to the server and get returned audio data
     * @param content String
     * @return {@link HttpResponse}
     * @throws Exception
     */
    private HttpResponse createPost(String content) throws Exception {
        //HTTP GET Client
        HttpClient httpClient = new DefaultHttpClient();

        if(tConfig.requestTimeout > 0) {
            // request timeout
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, tConfig.requestTimeout);
            // same as request timeout, so the whole process wouldn't be more than 30s
            httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, tConfig.requestTimeout);
        }

        //Add params
        List<BasicNameValuePair> params = new LinkedList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("text", content));
        params.add(new BasicNameValuePair("voice", tConfig.voice));
        params.add(new BasicNameValuePair("accept", tConfig.codec));
        if(tConfig.customizationId != null){
            params.add(new BasicNameValuePair("customization_id", tConfig.customizationId));
        }
        String requestUrl = tConfig.getSynthesizeURL() + "?"+ URLEncodedUtils.format(params, "utf-8");
        Log.e(TAG, requestUrl);
        HttpGet httpGet = new HttpGet(requestUrl);
        // use token based authentication if possible, otherwise Basic Authentication will be used
        buildAuthenticationHeader(httpGet);

        return httpClient.execute(httpGet);
    }

    /**
     * Get storage path
     * @return String
     */
    private String getBaseDir() {
        String baseDir;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            baseDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
        }
        else {
            baseDir = "/data/data/" + this.tConfig.appContext.getPackageName() + "/";
            // this.tConfig.appContext.getFilesDir() + "/";
        }

        return baseDir;
    }

    /**
     * Analyze opus data
     * @param is InputStream
     * @return byte[]
     */
    private byte[] analyzeOpusData(InputStream is) {
        String inFilePath = getBaseDir()+"watson.opus";
        String outFilePath = getBaseDir()+"watson.pcm";

        File inFile = new File(inFilePath);
        File outFile = new File(outFilePath);

        outFile.deleteOnExit();
        inFile.deleteOnExit();

        try {
            RandomAccessFile inRaf = new RandomAccessFile(inFile, "rw");
            byte[] opus = IOUtils.toByteArray(is);
            inRaf.write(opus);

            this.sampleRate = OggOpus.decode(inFilePath, outFilePath, sampleRate); // zero means to detect the sample rate by decoder

            RandomAccessFile outRaf = new RandomAccessFile(outFile, "r");

            byte[] data = new byte[(int)outRaf.length()];

            int outLength = outRaf.read(data);

            inRaf.close();
            outRaf.close();
            if(outLength == 0){
                throw new IOException("Data reading failed");
            }
            return data;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    /**
     * Analyze sample rate and return the PCM data
     * @param i InputStream
     * @return byte[]
     */
    public byte[] analyzeWavData(InputStream i){
        try {
            int headSize=44, metaDataSize=48;
            byte[] data = IOUtils.toByteArray(i);
            if(data.length < headSize){
                throw new IOException("Wrong Wav header");
            }

            if(this.sampleRate == 0 && data.length > 28) {
                this.sampleRate = readInt(data, 24); // 24 is the position of sample rate in wav format
            }

            int destPos = headSize + metaDataSize;
            int rawLength = data.length - destPos;

            byte[] d = new byte[rawLength];
            System.arraycopy(data, destPos, d, 0, rawLength);
            return d;
        } catch (IOException e) {
            Log.e(TAG, "Error while formatting header");
        }
        return new byte[0];
    }

    /**
     * Strip header and save Wav data
     * @param i InputStream
     * @return byte[]
     */
    public byte[] stripHeaderAndSaveWav(InputStream i) {
        byte[] d = new byte[0];
        try {
            int headSize=44;
            int metaDataSize=48;
            i.skip(headSize+metaDataSize);
            d = IOUtils.toByteArray(i);
        } catch (IOException e) {
            Log.d(TAG,"Error while formatting header");
        }
        return saveWav(d);
    }

    /**
     * Save wave data
     * @param d byte[]
     * @return byte[]
     */
    public byte[] saveWav(byte[] d){
        PcmWaveWriter wR = new PcmWaveWriter(sampleRate, 1);
        return wR.saveWav(d, sampleRate, 1, 16);
    }

    /**
     * Save wav file
     * @param d byte[]
     */
    void saveWavFile(byte[] d) {
        String fileName = getBaseDir() + "a.wav";
        try {
            PcmWaveWriter wR = new PcmWaveWriter(sampleRate, 1);
            wR.open(fileName);
            wR.saveWavFile(d,sampleRate,1,16);
            wR.close();
            Log.i(TAG, "save file OK");
        } catch (IOException e) {
            Log.d(TAG, "save file FAIL");
            e.printStackTrace();
        }
    }

    /**
     * Converts Little Endian (Windows) bytes to an int (Java uses Big Endian).
     * @param data the data to read.
     * @param offset the offset from which to start reading.
     * @return the integer value of the reassembled bytes.
     */
    protected static int readInt(final byte[] data, final int offset)
    {
        return (data[offset] & 0xff) |
                ((data[offset+1] & 0xff) <<  8) |
                ((data[offset+2] & 0xff) << 16) |
                (data[offset+3] << 24); // no 0xff on the last one to keep the sign
    }

    /**
     * Converts Little Endian (Windows) bytes to an short (Java uses Big Endian).
     * @param data the data to read.
     * @param offset the offset from which to start reading.
     * @return the integer value of the reassembled bytes.
     */
    protected static int readShort(final byte[] data, final int offset)
    {
        return (data[offset] & 0xff) |
                (data[offset+1] << 8); // no 0xff on the last one to keep the sign
    }

    private void initPlayer(){
        this.stopAudio();
        // IMPORTANT: minimum required buffer size for the successful creation of an AudioTrack instance in streaming mode.
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

        synchronized (this) {
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STREAM);
            audioTrack.play();
        }

        this.delegate.onTTSWillPlay();
    }

    /**
     * Set delegate for callbacks
     * @param val ITextToSpeechDelegate
     */
    public void setDelegate(ITextToSpeechDelegate val){
        this.delegate = val;
    }

    /**
     * Thread to post text data to iTrans server and play returned audio data
     */
    private class TTSThread extends Thread {
        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            HttpResponse post;
            try {
                post = createPost(content);
                int statusCode = post.getStatusLine().getStatusCode();
                if(statusCode == 200) {
                    InputStream is = post.getEntity().getContent();

                    byte[] data = null;
                    if (tConfig.codec.equals(TTSConfiguration.CODEC_WAV)) {
                        data = analyzeWavData(is);
                    } else if (tConfig.codec.equals(TTSConfiguration.CODEC_OPUS)) {
                        data = analyzeOpusData(is);
                    }
                    if(data != null) {
                        initPlayer();
                        audioTrack.write(data, 0, data.length);
                    }
                    is.close();
                }
                else{
                    delegate.onTTSError(statusCode);
                }
            } catch (Exception e) {
                e.printStackTrace();
                delegate.onTTSError(0);
            } finally {
                Log.i(TAG, "Stopping audioTrack...");
                if (audioTrack != null && audioTrack.getState() != AudioTrack.STATE_UNINITIALIZED) {
                    audioTrack.release();
                }
                delegate.onTTSStopped();
            }
        }
    }
}
