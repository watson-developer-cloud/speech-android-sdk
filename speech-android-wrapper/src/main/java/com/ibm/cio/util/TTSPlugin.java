package com.ibm.cio.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.xiph.speex.PcmWaveWriter;

import android.app.Application;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.Environment;
import android.util.Log;

import com.ibm.cio.audio.VaniSpeexDec;
import com.ibm.cio.audio.player.PlayerUtil;

public class TTSPlugin extends Application{
	private static final String TAG = TTSPlugin.class.getName();
	
	Context ct;
	AudioManager am;
	private String lmcCookie;
	private String username;
	private String password;
	private String content;
	private String language;
	private int samplerate;
	private String outputtype;
	private String server;
	
	private AudioTrack audioTrack;
	private MediaPlayer	wavPlayer = null;
	
	public int streamId;
	private String port;
	
	
	//private static final int MIN_FRAME_COUNT = 600;

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
	
	private void playWav() {
		//only welcome, need more implement
		Log.i(TAG, "Play welcome_8000!");
//		wavPlayer = MediaPlayer.create(ct, R.raw.welcome_8000);
//		wavPlayer.start();
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
		this.lmcCookie = arguments[i++];
		this.server = arguments[i++];
		this.port = arguments[i++];
		this.username = arguments[i++];
		this.password = arguments[i++];
		this.content = arguments[i++];
		this.language = arguments[i++];
		this.samplerate = Integer.parseInt(arguments[i++]);
		this.outputtype = arguments[i++];
	}
	
	/**
	 * Post text data to iTrans server and get returned audio data
	 * @param lmcCookie
	 * @param server iTrans server
	 * @param port
	 * @param username
	 * @param password
	 * @param content
	 * @param language
	 * @param samplerate
	 * @param outputtype
	 * @return {@link HttpResponse}
	 * @throws Exception
	 */
	public static HttpResponse createPost(String lmcCookie, String server,String port, String username, String password
			, String content, String language, String samplerate, String outputtype) throws Exception {
		HttpClient client = new DefaultHttpClient();
		//client.getState().setCookiePolicy(CookiePolicy.COMPATIBILITY);
		
		HttpPost post=null;
		String url = "https://"+server+ ":" + port +"/itrans/api/synTts";
//		System.out.println("Will post to " + url);
		post = new HttpPost(url);
		post.setHeader("Cookie", lmcCookie);
		
		// Add your data
	    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
	    nameValuePairs.add(new BasicNameValuePair("username", username));
	    nameValuePairs.add(new BasicNameValuePair("password", password));
	    nameValuePairs.add(new BasicNameValuePair("content", content));
	    nameValuePairs.add(new BasicNameValuePair("language", language));
	    nameValuePairs.add(new BasicNameValuePair("samplerate", samplerate));
	    nameValuePairs.add(new BasicNameValuePair("outputtype", outputtype));
	    nameValuePairs.add(new BasicNameValuePair("timeout", "15000"));
	    nameValuePairs.add(new BasicNameValuePair("temp", "true"));
	    
	    post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		
		HttpResponse executed = client.execute(post);
		
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
	
	void saveWavFile(byte[] d) {
		PcmWaveWriter wR = new PcmWaveWriter(8000, 1);
		String fileName = getBaseDir() + "a.wav";
		try {			
			wR.open(fileName);
			wR.writeHeader("by jspeex");
			wR.writePacket(d, 0, d.length);
			wR.close();
			Log.i(TAG, "save file OK");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.d(TAG, "save file FAIL");
			e.printStackTrace();
		}
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
				post = createPost(lmcCookie, server, port, username, password, content, language, samplerate+"", outputtype);
		        InputStream is=post.getEntity().getContent();
		        byte[] pcm = new VaniSpeexDec().decode(is); // Decode SPX stream to PCM
//		        System.out.println("Audio length PCM = " + pcm.length);
		        audioTrack.write(pcm, 0, pcm.length);
//		        saveWavFile(pcm);
		        
				/*
				 * For outputtype = PCM
				 * 
				 * byte[] data = new byte[1024];
				int length, totalLength = 0;
				while((length=is.read(data))!=-1) {
					//os.write(data,0,length);
					audioTrack.write(data, 0, length);
					totalLength += length;
					
				}
				is.close();
				*/
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

	
}
