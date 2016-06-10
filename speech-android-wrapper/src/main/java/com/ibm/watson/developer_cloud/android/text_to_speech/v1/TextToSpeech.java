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

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Environment;
import android.util.Log;

import com.ibm.watson.developer_cloud.android.speech_common.v1.ITokenProvider;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.audio.PcmWaveWriter;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.opus.OggOpus;
import com.ibm.watson.developer_cloud.android.text_to_speech.v1.dto.TTSConfiguration;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;

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

    private ITokenProvider tokenProvider = null;
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
     * @param config
     */
    public void initWithConfig(TTSConfiguration config){
        TextToSpeech.sharedInstance();
        _instance.tConfig = config;
    }

    /**
     * Send request of TTS
     * @param ttsString
     */
    public void synthesize(String ttsString) {
        this.synthesize(ttsString, null);
    }

    /**
     * Send request of TTS
     * @param ttsString
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

        if(this.delegate != null)
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

    private static void buildAuthenticationHeader(TTSConfiguration config, HttpGet httpGet, ITokenProvider tokenProvider) {
        if (config.isAuthNeeded) {
            // use token based authentication if possible, otherwise Basic Authentication will be used
            if (tokenProvider != null) {
                Log.d(TAG, "using token based authentication");
                httpGet.setHeader("X-Watson-Authorization-Token", tokenProvider.getToken());
            }
            else {
                Log.d(TAG, "using basic authentication");
                httpGet.setHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(config.basicAuthUsername, config.basicAuthPassword), "UTF-8", false));
            }
        }
    }

    public JSONObject getVoices() {

        JSONObject object = null;

        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(this.tConfig.getVoicesServiceURL());

            buildAuthenticationHeader(this.tConfig, httpGet, this.tokenProvider);

            httpGet.setHeader("accept", "application/json");
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
     * @param username
     * @param password
     */
    public void setCredentials(String username, String password) {
        this.tConfig.basicAuthUsername = username;
        this.tConfig.basicAuthPassword = password;
    }

    /**
     * Set token provider (for token based authentication)
     */
    public void setTokenProvider(ITokenProvider tokenProvider) { this.tokenProvider = tokenProvider; }

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
     * @param content
     * @return {@link HttpResponse}
     * @throws Exception
     */
    public static HttpResponse createPost(String content, TTSConfiguration config, ITokenProvider tokenProvider) throws Exception {
        //HTTP GET Client
        HttpClient httpClient = new DefaultHttpClient();
        //Add params
        List<BasicNameValuePair> params = new LinkedList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("text", content));
        params.add(new BasicNameValuePair("voice", config.voice));
        params.add(new BasicNameValuePair("accept", config.codec));
        if(config.customizationId != null){
            params.add(new BasicNameValuePair("customization_id", config.customizationId));
        }
        String requestUrl = config.getSynthesizeURL() + "?"+ URLEncodedUtils.format(params, "utf-8");
        Log.e(TAG, requestUrl);
        HttpGet httpGet = new HttpGet(requestUrl);
        // use token based authentication if possible, otherwise Basic Authentication will be used
        buildAuthenticationHeader(config, httpGet, tokenProvider);
        HttpResponse executed = httpClient.execute(httpGet);

        return executed;
    }
    /**
     * Get storage path
     * @return
     */
    private String getBaseDir() {
        String baseDir;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            baseDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
        } else {
            baseDir = "/data/data/" + this.tConfig.appContext.getPackageName() + "/";
        }

        return baseDir;
    }

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

            sampleRate = OggOpus.decode(inFilePath, outFilePath, sampleRate); // zero means to detect the sample rate by decoder

            RandomAccessFile outRaf = new RandomAccessFile(outFile, "r");

            byte[] data = new byte[(int)outRaf.length()];

            int outLength = outRaf.read(data);

            inRaf.close();
            outRaf.close();
            if(outLength == 0){
                throw new IOException("Data reading failed");
            }
            return data;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    /**
     * Analyze sample rate and return the PCM data
     * @param i
     * @return
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

    public byte[] saveWav(byte[] d){
        PcmWaveWriter wR = new PcmWaveWriter(sampleRate, 1);
        return wR.saveWav(d, sampleRate, 1, 16);
    }

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
            if (audioTrack != null)
                audioTrack.play();
        }

        if(this.delegate != null)
            this.delegate.onTTSWillPlay();
    }

    /**
     * Set delegate for callbacks
     * @param val
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
                post = createPost(content, tConfig, tokenProvider);
                int statusCode = post.getStatusLine().getStatusCode();
                if(statusCode == 200) {
                    InputStream is = post.getEntity().getContent();

                    byte[] data = null;
                    if (tConfig.codec == TTSConfiguration.CODEC_WAV) {
                        data = analyzeWavData(is);
                    } else if (tConfig.codec == TTSConfiguration.CODEC_OPUS) {
                        data = analyzeOpusData(is);
                    }
                    initPlayer();
                    audioTrack.write(data, 0, data.length);
                    is.close();
                }
                else{
                    if(delegate != null){
                        delegate.onTTSError(statusCode);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if(delegate != null){
                    delegate.onTTSError(0);
                }
            } finally {
                Log.i(TAG, "Stopping audioTrack...");
                if (audioTrack != null && audioTrack.getState() != AudioTrack.STATE_UNINITIALIZED) {
                    audioTrack.release();
                }
                if(delegate != null) {
                    delegate.onTTSStopped();
                }
            }
        }
    }
}
