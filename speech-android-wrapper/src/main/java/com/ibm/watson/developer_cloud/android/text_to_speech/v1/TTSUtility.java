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

import android.app.Application;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Environment;
import android.util.Log;

import com.ibm.watson.developer_cloud.android.speech_to_text.v1.opus.OggOpus;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.ibm.watson.developer_cloud.android.speech_to_text.v1.audio.PcmWaveWriter;

import java.io.File;

import java.io.FileNotFoundException;

import java.io.IOException;
import java.io.InputStream;

import java.io.RandomAccessFile;

import java.util.LinkedList;
import java.util.List;

public class TTSUtility extends Application {
	private static final String TAG = TTSUtility.class.getName();

	public static final String CODEC_WAV = "audio/wav";
    public static final int CODEC_WAV_SAMPLE_RATE = 0;

	public static final String CODEC_OPUS = "audio/opus";
    public static final int CODEC_OPUS_SAMPLE_RATE = 48000;

	private String username;
	private String password;
    private String token;
    private boolean learningOptOut;
    private String voice;
	private String content;
	private String codec;
	private int sampleRate;
	private String server;
	private AudioTrack audioTrack;


	public TTSUtility(){
		this.codec = CODEC_WAV;
        // By default, the sample rate would be detected by the SDK if the value is set to zero
        // However, the metadata is not reliable, need to decode at the maximum sample rate
        this.sampleRate = 48000;
	}

    /**
     * Set codec
     * @param codec
     */
	public void setCodec(String codec){
		this.codec = codec;
	}

    /**
     * Stop player
     */
	private void stopTtsPlayer() {
        if (audioTrack != null && audioTrack.getState() != AudioTrack.STATE_UNINITIALIZED ) {
            // IMPORTANT: NOT use stop()
            // For an immediate stop, use pause(), followed by flush() to discard audio data that hasn't been played back yet.
            audioTrack.pause();
            audioTrack.flush();
        }
	}

	/**
	 * Text to speech
	 * @param arguments
	 */
	public void synthesize(String[] arguments) {
		Log.i(TAG, "Start requesting TTS... ("+this.codec+")");
		try {
			parseParams(arguments);
		} catch (Exception e) {
			e.printStackTrace();
		}

        if(this.codec == CODEC_WAV){
            this.sampleRate = CODEC_WAV_SAMPLE_RATE;
        }
        else{
            this.sampleRate = CODEC_OPUS_SAMPLE_RATE;
        }

		TTSThread thread = new TTSThread();
		thread.setName("TTSThread");
		thread.start();
	}

    private void initPlayer(){
        stopTtsPlayer();
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
    }

	private void parseParams(String[] arguments){
		int i = 0;
		this.server = arguments[i++];
		this.username = arguments[i++];
		this.password = arguments[i++];
        this.voice = arguments[i++];
		this.content = arguments[i++];
        this.token = arguments[i++];
        this.learningOptOut = Boolean.valueOf(arguments[i++]);
	}

    /**
	 * Post text data to the server and get returned audio data
	 * @param server iTrans server
	 * @param username
	 * @param password
	 * @param content
	 * @return {@link HttpResponse}
	 * @throws Exception
	 */
	public static HttpResponse createPost(String server, String username, String password, String token, boolean learningOptOut, String content, String voice, String codec) throws Exception {
        String url = server;

        //HTTP GET Client
        HttpClient httpClient = new DefaultHttpClient();
        //Add params
        List<BasicNameValuePair> params = new LinkedList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("text", content));
        params.add(new BasicNameValuePair("voice", voice));
        params.add(new BasicNameValuePair("accept", codec));
        HttpGet httpGet = new HttpGet(url+"?"+ URLEncodedUtils.format(params, "utf-8"));
        // use token based authentication if possible, otherwise Basic Authentication will be used
        if (token != null) {
            Log.d(TAG, "using token based authentication");
            httpGet.setHeader("X-Watson-Authorization-Token",token);
        } else {
            Log.d(TAG, "using basic authentication");
            httpGet.setHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(username, password), "UTF-8", false));
        }

        if (learningOptOut) {
            Log.d(TAG, "setting X-Watson-Learning-OptOut");
            httpGet.setHeader("X-Watson-Learning-Opt-Out", "true");
        }
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
            baseDir = "/data/data/" + getApplicationContext().getPackageName() + "/";
        }

        return baseDir;
    }

	/**
	 * Thread to post text data to iTrans server and play returned audio data 
	 * @author chienlk
	 *
	 */
	public class TTSThread extends Thread {
		@Override
		public void run() {
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
			
			HttpResponse post;
			try {
				post = createPost(server, username, password, token, learningOptOut, content, voice, codec);
		        InputStream is = post.getEntity().getContent();

				byte[] data = null;
				if(codec == CODEC_WAV) {
					data = analyzeWavData(is);
				}
				else if(codec == CODEC_OPUS){
					data = analyzeOpusData(is);
				}
                initPlayer();
                audioTrack.write(data, 0, data.length);
                is.close();

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
                Log.i(TAG, "Stopping audioTrack...");
				if (audioTrack != null && audioTrack.getState() != AudioTrack.STATE_UNINITIALIZED) {
					audioTrack.release();
				}
			}
		}
	}

    private byte[] analyzeOpusData(InputStream is) {
        String inFilePath = getBaseDir()+"Watson.opus";
        String outFilePath = getBaseDir()+"Watson.pcm";
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
}
