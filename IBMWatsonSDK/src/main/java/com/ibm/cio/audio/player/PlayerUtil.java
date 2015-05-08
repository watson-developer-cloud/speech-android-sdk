package com.ibm.cio.audio.player;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.SystemClock;
import android.util.Log;

import com.ibm.cio.audio.VaniSpeexDec;
import com.ibm.cio.util.Logger;


public class PlayerUtil {
	private static final String TAG = PlayerUtil.class.getName();
	public static PlayerUtil ins8k = new PlayerUtil(8000);
	
	private PlayerUtil(int samplerate){
		initAudioTrack(samplerate);
	}
	
	AudioTrack audioTrack;
	Thread tPlaySpx;
	private MediaPlayer	wavPlayer = null;
    private long decodingTTSTime = 0;
	public static Object LOCK = new Object();

	public void playPCM(final byte[] pcm) {
		System.out.println("playPCM: " + pcm.length);
		Thread t = new Thread() {
			@Override
			public void run() {
				// Start playback
				if (audioTrack != null) {
					audioTrack.stop();
					
					audioTrack.setNotificationMarkerPosition(pcm.length/2);
					
					audioTrack.play();
					
					audioTrack.write(pcm, 0, pcm.length);
				}
			}
		};
		t.start();
	}
	
	public void playSPX(final byte[] spxData) {
		System.out.println("playSPX: " + spxData.length + " bytes");
		/*InputStream audioIS = new ByteArrayInputStream(spxData);
		final byte[] pcm = new VaniSpeexDec().decode(audioIS);*/
		audioTrack.play();
		tPlaySpx = new Thread() {
			@Override
			public void run() {
				InputStream audioIS = new ByteArrayInputStream(spxData);
                long beginDecode = SystemClock.elapsedRealtime();
				byte[] pcm = new VaniSpeexDec().decode(audioIS);
                decodingTTSTime = SystemClock.elapsedRealtime() - beginDecode;
                synchronized (PlayerUtil.LOCK) {
                    PlayerUtil.LOCK.notify();
                    Logger.e(TAG, "### NOTIFIED");
                }
				// Start playback
				if (audioTrack != null && pcm != null) {
//					audioTrack.setNotificationMarkerPosition(pcm.length/2);
					
//					audioTrack.play();
					audioTrack.write(pcm, 0, pcm.length);
					audioTrack.stop();
				}
			}
		};
		tPlaySpx.start();
	}
	
	public void playIdontUnderstand(Context context) {
		Log.i(TAG, "playIdontUnderstand");
		// TODO fix below
		//wavPlayer = MediaPlayer.create(context, R.raw.dont_understand_8000);
		//wavPlayer.start();
	}

	private void initAudioTrack(int samplerate) {
		Log.i(TAG, "initAudioTrack");
		int bufferSize = AudioTrack.getMinBufferSize(samplerate, AudioFormat.CHANNEL_OUT_MONO,
				AudioFormat.ENCODING_PCM_16BIT);
		
		audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
				samplerate,
				AudioFormat.CHANNEL_OUT_MONO,
				AudioFormat.ENCODING_PCM_16BIT,
				bufferSize,
				AudioTrack.MODE_STREAM);
		
		/*audioTrack.setPlaybackPositionUpdateListener(new OnPlaybackPositionUpdateListener() {
			@Override
			public void onPeriodicNotification(AudioTrack track) {
			}
			
			@Override
			public void onMarkerReached(AudioTrack track) {
				Log.d("PlayerUtil", "Audio track end of file reached...");
				audioTrack.stop();
			}
		});*/
	}

	public void stop() {
//		For an immediate stop, use pause(), followed by flush() to discard audio data that hasn't been played back yet.
		if (audioTrack!=null && audioTrack.getState() != AudioTrack.STATE_UNINITIALIZED ) {
			audioTrack.pause();
			audioTrack.flush();
		}
		// stop "I don't understand..."
		if (wavPlayer!=null && wavPlayer.isPlaying()) {
			wavPlayer.stop();
			//wavPlayer.release();
		}
	}

    public long getTTSDecodingTime(){
        return this.decodingTTSTime;
    }

    public void resetTTSDecodingTime(){
        this.decodingTTSTime = 0;
    }
}
