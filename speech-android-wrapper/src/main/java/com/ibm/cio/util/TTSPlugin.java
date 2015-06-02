package com.ibm.cio.util;

import android.app.Application;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.ibm.cio.audio.VaniOpusDec;
import com.ibm.cio.audio.VaniSpeexDec;
import com.ibm.cio.audio.player.PlayerUtil;
import com.ibm.cio.opus.OpusDecoder;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.xiph.speex.PcmWaveWriter;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import java.util.LinkedList;
import java.util.List;

public class TTSPlugin extends Application{
	private static final String TAG = TTSPlugin.class.getName();

	public static final String CODEC_WAV = "audio/wav";
	public static final String CODEC_OPUS = "audio/opus";

	Context ct;
	AudioManager am;
	private String username;
	private String password;
	private String content;
	private String codec;
	private Handler decodeHandler;
	private String language;
	private int samplerate=48000;
	private String server;
	
	private AudioTrack audioTrack;
	private MediaPlayer	wavPlayer = null;

	//private static final int MIN_FRAME_COUNT = 600;


	public TTSPlugin(){
		this.codec = CODEC_WAV;
		this.decodeHandler = new Handler();
	}

	public void setCodec(String codec){
		this.codec = codec;
	}

	public void test(){
		new Thread(){
			@Override
			public void run() {
				_test();
			}
		}.start();
	}

	private void _test(){
		File file = new File(getBaseDir()+"Watson.opus");
		try {
			FileInputStream fis = new FileInputStream(file);
			byte[] buffer = new byte[fis.available()];
			fis.read(buffer);
			Logger.e(TAG, "Original audio.length=" + buffer.length);
			VaniOpusDec dec = new VaniOpusDec();

			byte[] audio = dec.decodeOggBytes(buffer);

			int bufferSize = AudioTrack.getMinBufferSize(samplerate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
			audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
					samplerate,
					AudioFormat.CHANNEL_OUT_MONO,
					AudioFormat.ENCODING_PCM_16BIT,
					bufferSize,
					AudioTrack.MODE_STREAM);
			audioTrack.play();

			Logger.e(TAG, "Decoded audio.length="+audio.length);
			audioTrack.write(audio, 0, audio.length);

			fis.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private byte[] ShortToByte_Twiddle_Method(final short[] input) {
		final int len = input.length;
		final byte[] buffer = new byte[len * 2];
		for (int i = 0; i < len; i++) {
			buffer[(i * 2) + 1] = (byte) (input[i]);
			buffer[(i * 2)] = (byte) (input[i] >> 8);
		}
		return buffer;
	}

//	public boolean execute(String action, JSONArray arguments) {
//
//		ct = getApplicationContext();
//		am = (AudioManager) ct.getSystemService(Context.AUDIO_SERVICE);
//		System.out.println("Call TTSPlugin, ringtone mode: " + am.getRingerMode() + "|ring volume: " + am.getStreamVolume(AudioManager.STREAM_RING));
//		
//		if (am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
//			if (action.equals("tts")) {
////				if (am.getStreamVolume(AudioManager.STREAM_RING) != 0) {  // Check if phone in vibrate/silent mode
//					try {
//						tts(arguments);
//					} catch (Exception e) {
//						e.printStackTrace(); 
//						//result = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
//					}
////				}
//				return true;
//			}  
//			if (action.equals("playWav")) {
////				if (am.getStreamVolume(AudioManager.STREAM_RING) != 0) { // Check if phone in vibrate/silent mode
//
//				Thread playWavthread = new Thread()
//			    {
//						public void run() {
//							// TODO Auto-generated method stub
//							playWav();
//			
//						}
//			    };
//			    playWavthread.start();
//
////				}
//				return true;
//			}
//		}
//		
//		if (action.equals("stopAudioPlayer")) {
//			stopAudioPlayer(arguments);
//			return true;
//		}
//		
//		return false;
//	}
	
	private void stopAudioPlayer(JSONArray arguments) {
		Log.i(TAG, "stop all AudioPlayer");
		//stop welcome
		if (wavPlayer!=null && wavPlayer.isPlaying()) {
			wavPlayer.stop();
			//wavPlayer.release();
		}
		//stop tts
		PlayerUtil.ins8k.stop();
		stopTtsPlayer();
	}

	private void stopTtsPlayer() {
//		synchronized (this) {
			if (audioTrack!=null && audioTrack.getState() != AudioTrack.STATE_UNINITIALIZED ) {
				Log.i(TAG, "stopTtsPlayer");
				// IMPORTANT: NOT use stop()
				// For an immediate stop, use pause(), followed by flush() to discard audio data that hasn't been played back yet.
				audioTrack.pause();
				audioTrack.flush();
				//audioTrack.release();
		 	}			
//		}
	}
	


	/*
	private void playWav(JSONArray arguments) {
		System.out.println("playWav welcome");
		soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
		AudioManager am = (AudioManager) this.ctx.getSystemService(this.ctx.getApplicationContext().AUDIO_SERVICE);
		float actualVolume = (float) am.getStreamVolume(AudioManager.STREAM_MUSIC);
		float maxVolume = (float) am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		final float volume = actualVolume / maxVolume;
		soundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
			
			@Override
			public void onLoadComplete(SoundPool sPool, int sampleId, int status) {
				// TODO Auto-generated method stub
				System.out.println("playWav soundPool onLoadComplete");
				streamId = soundPool.play(soundId, volume, volume, 1, 0, 1f);
			}
		});
		soundId = soundPool.load(this.ctx.getApplicationContext(), R.raw.welcome_8000, 1);
	}
	*/

	/**
	 * Text to speech
	 * @param arguments
	 */
	public void tts(String[] arguments) {
		Log.i(TAG, "tts called");
		try {
			parseParams(arguments);
		} catch (Exception e) {
			e.printStackTrace();
		}
//		System.out.println("tts: " + audioTrack);
		stopTtsPlayer();
		/*int bufferSize = samplerate/8;		
		if (bufferSize/2 < MIN_FRAME_COUNT) {
			bufferSize = MIN_FRAME_COUNT*2;
		}*/
		// IMPORTANT: minimum required buffer size for the successful creation of an AudioTrack instance in streaming mode.
		int bufferSize = AudioTrack.getMinBufferSize(samplerate, AudioFormat.CHANNEL_OUT_MONO,
				AudioFormat.ENCODING_PCM_16BIT);
		
		System.out.println("call tts, construct AudioTrack, sampleRate = " + samplerate + ", bufferSize="+bufferSize);
		
		synchronized (this) {
			audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
					samplerate,
					AudioFormat.CHANNEL_OUT_MONO,
					AudioFormat.ENCODING_PCM_16BIT,
					bufferSize,
					AudioTrack.MODE_STREAM);
			System.out.println("tts after new AudioTrack: " + audioTrack);
			// Start playback
			if (audioTrack != null)
				audioTrack.play();
		}
		
		TTSThread thread = new TTSThread();
		thread.setName("TTSThread");
		thread.start();
	}

	private void parseParams(String[] arguments){
		int i = 0;
		this.server = arguments[i++];
		this.username = arguments[i++];
		this.password = arguments[i++];
		this.content = arguments[i++];
	}
	
	/**
	 * Post text data to iTrans server and get returned audio data
	 * @param server iTrans server
	 * @param username
	 * @param password
	 * @param content
	 * @return {@link HttpResponse}
	 * @throws Exception
	 */
	public static HttpResponse createPost(String server, String username, String password, String content, String codec) throws Exception {
        String url = server;

        //HTTP GET Client
        HttpClient httpClient = new DefaultHttpClient();
        //Add params
        List<BasicNameValuePair> params = new LinkedList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("text", content));;
        params.add(new BasicNameValuePair("voice", "VoiceEnUsMichael"));
        params.add(new BasicNameValuePair("accept", codec));
        HttpGet httpGet = new HttpGet(url+"?"+ URLEncodedUtils.format(params, "utf-8"));
        httpGet.setHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(username, password), "UTF-8", false));
        HttpResponse executed = httpClient.execute(httpGet);

		return executed;
	}
	
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
				post = createPost(server, username, password, content, codec);
		        InputStream is = post.getEntity().getContent();

//                /*
//                *For outputtype = SpeeX
//                **/
//		        byte[] pcm = new VaniSpeexDec().decode(is); // Decode SPX stream to PCM
//		        System.out.println("Audio length PCM = " + pcm.length);
//		        audioTrack.write(pcm, 0, pcm.length);
//		        saveWavFile(pcm);
		        
				/*
				 * For outputtype = WAV
				 * */

				if(codec == CODEC_WAV) {
//					byte[] temp = stripHeaderAndSaveWav(is);
					byte[] temp = stripHeader(is);
					audioTrack.write(temp, 0, temp.length);
				}
				else if(codec == CODEC_OPUS){
//					File file = new File(getBaseDir()+"Watson.opus");
//					file.delete();
//					RandomAccessFile raf = new RandomAccessFile(file, "rw");
//					raf.write(IOUtils.toByteArray(is));
					// TODO
				}

//                int bytesRead = 0;
//                int bufferSize = 4096;
//                byte[] buffer = new byte[bufferSize];
//                while ((bytesRead = is.read(buffer, 0, bufferSize)) != -1) {
//                    audioTrack.write(buffer, 0, bytesRead);
//                }
//                is.close();

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				System.out.println("call stop audioTrack");
				if (audioTrack.getState() != AudioTrack.STATE_UNINITIALIZED) {
//					audioTrack.stop();
					audioTrack.release();
//					System.out.println("tts after release: " + audioTrack + ", state: " + audioTrack.getState());
				}
					
			}
		}

	}


	private void playWav() {
        //only welcome, need more implement
        Log.i(TAG, "Playing Wav file");
        String fileName = getBaseDir() + "a.wav";
//		wavPlayer = new MediaPlayer();
//        wavPlayer.setDataSource(ct,Uri.fromFile(fileName));
//		wavPlayer.start();
    }

	public byte[] stripHeader(InputStream i){
		byte[] d = new byte[0];
		try {
			int headSize=44;
			int metaDataSize=48;
			i.skip(headSize+metaDataSize);
			d = IOUtils.toByteArray(i);
		} catch (IOException e) {
			Log.d(TAG,"Error while formatting header");
		}
		return d;
	}

    public byte[] stripHeaderAndSaveWav(InputStream i){

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
        //To save wav file
//        saveWavFile(d);
    }

    public byte[] saveWav(byte[] d){
        PcmWaveWriter wR = new PcmWaveWriter(48000, 1);
        return wR.saveWav(d, 48000, 1, 16);
    }

    void saveWavFile(byte[] d) {
        String fileName = getBaseDir() + "a.wav";
        try {
            PcmWaveWriter wR = new PcmWaveWriter(48000, 1);
            wR.open(fileName);
            wR.saveWavFile(d,48000,1,16);
            wR.close();
            Log.i(TAG, "save file OK");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.d(TAG, "save file FAIL");
            e.printStackTrace();
        }
    }

    // convert InputStream to String
    private static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));

            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();

    }


	
}
