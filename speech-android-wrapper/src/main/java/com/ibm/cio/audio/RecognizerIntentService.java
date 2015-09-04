/* ***************************************************************** */
/*                                                                   */
/* IBM Confidential                                                  */
/*                                                                   */
/* OCO Source Materials                                              */
/*                                                                   */
/* Copyright IBM Corp. 2013                                          */
/*                                                                   */
/* The source code for this program is not published or otherwise    */
/* divested of its trade secrets, irrespective of what has been      */
/* deposited with the U.S. Copyright Office.                         */
/*                                                                   */
/* ***************************************************************** */
package com.ibm.cio.audio;

import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;

import com.ibm.crl.speech.vad.RawAudioRecorder;

// TODO: Auto-generated Javadoc
/**
 * Service to recording audio.
 * @author Turta@crl.ibm
 */
public class RecognizerIntentService extends Service {
	// Use PROPRIETARY notice if class contains a main() method, otherwise use
	// COPYRIGHT notice.
	public static final String COPYRIGHT_NOTICE = "(c) Copyright IBM Corp. 2013";
	private static final String TAG = RecognizerIntentService.class.getName();

	private final IBinder mBinder = new RecognizerBinder();

//	private volatile Looper mSendLooper;
//	private volatile Handler mSendHandler;
//	private Runnable mSendTask;

	/**
	 * Audio recorder.
	 */
	private RawAudioRecorder mRecorder;
	/**
	 * Error code of service.
	 */
	private int mErrorCode;

//	private int mChunkCount = 0;
	/**
	 * Starting time of service
	 */
	private long mStartTime = 0;
	/**
	 * Status of recorder.
	 */
	public enum State {
		// Service created or resources released
		IDLE,
		// Recognizer session created
		INITIALIZED,
		// Started the recording
		RECORDING,
		// Finished recording
		PROCESSING,
		// Got an error
		ERROR;
	}
	/**
	 * Current status
	 */
	private State mState = null;


	public class RecognizerBinder extends Binder {
		public RecognizerIntentService getService() {
			return RecognizerIntentService.this;
		}
	}

	public interface OnErrorListener {
		public boolean onError(int errorCode, Exception e);
	}
	
	@Override
	public void onCreate() {
		setState(State.IDLE);
	}


	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}


	@Override
	public void onDestroy() {
		releaseResources();
	}
	/**
	 * Get current status of service.
	 * @return status
	 */
	public State getState() {
		return mState;
	}
	/**
	 * Get starting time of recording.
	 * @return time when the recording started
	 */
	public long getStartTime() {
		return mStartTime;
	}
	/**
	 * Check if service is working.
	 * @return <code>true</code> if currently recording or processing
	 */
	public boolean isWorking() {
		State currentState = getState();
		return currentState == State.RECORDING || currentState == State.PROCESSING;
	}
	/**
	 * Get length of recorded audio data.
	 * @return length of the current recording in bytes
	 */
	public int getLength() {
		if (mRecorder == null) {
			return 0;
		}
		return mRecorder.getLength();
	}
	public long getRecordingTime() {
		if (mRecorder == null) {
			return 0;
		}
		return mRecorder.getRecordingTime();
	}
	/**
	 * Get recent sound pressure.
	 * @return dB value of recent sound pressure
	 */
	public float getRmsdb() {
		if (mRecorder == null) {
			return 0;
		}
		return mRecorder.getRmsdb();
	}
	/**
	 * Check if user has stopped speak.
	 * @return <code>true</code> if currently recording non-speech
	 */
	public boolean isPausing() {
		return mRecorder != null && mRecorder.isPausing();
	}
	/**
	 * Get all recorded audio data.
	 * @return complete audio data from the beginning of the recording in bytes
	 */
	public byte[] getCompleteRecording() {
		if (mRecorder == null) {
			return new byte[0];
		}
		return mRecorder.getCompleteRecording();
	}

	/*public int getChunkCount() {
		return mChunkCount;
	}*/

	/**
	 * Get error code of service.
	 * @return
	 */
	public int getErrorCode() {
		return mErrorCode;
	}
	/**
	 * Initiate status of recorder.
	 * @return
	 */
	public boolean init() {
		setState(State.INITIALIZED);
		return true;
	
	}
	/**
	 * <p>Start recording with the given sample rate.</p>
	 *
	 * @param sampleRate sample rate in Hz, e.g. 16000
	 */
	public boolean start(int sampleRate, AudioConsumer audioConsumer) {
		if (mState != State.INITIALIZED) {
			processError(RawAudioRecorder.RESULT_VAD_ERROR, null);
			return false;
		}
		try {
			startRecording(sampleRate, audioConsumer);
			mStartTime = SystemClock.elapsedRealtime();
//			startChunkProcessing(false);
			setState(State.RECORDING);
			return true;
		} catch (IOException e) {
			processError(RawAudioRecorder.RESULT_AUDIO_ERROR, e);
		}
		return false;
	}
	/**
	 * <p>Stops the recording, finishes chunk sending, sends off the
	 * last chunk (in another thread).</p>
	 */
	public boolean stop() {
		if (mState != State.RECORDING || mRecorder == null) {
			android.util.Log.d(TAG, "stop service FAIL: " + State.RECORDING);
			processError(RawAudioRecorder.RESULT_VAD_ERROR, null);
			return false;
		}
		mRecorder.stop();
		setState(State.PROCESSING);
//		mSendHandler.removeCallbacks(mSendTask);

		return true;
	}
	/**
	 * <p>Starting chunk processing in a separate thread so that would not block the UI.</p>
	 *
	 * TODO
	 *
	private void startChunkProcessing(final boolean consumeAll) {
		mChunkCount = 0;
		HandlerThread thread = new HandlerThread("SendHandlerThread", Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();
		mSendLooper = thread.getLooper();
		mSendHandler = new Handler(mSendLooper);

		mSendTask = new Runnable() {
			public void run() {
				if (mRecorder != null) {
					try {
						Thread.sleep(100);
					} catch (Exception e) {
						Log.i(TAG,e.toString());
					}
					if (consumeAll == false) {
						mSendHandler.postDelayed(this, 300);
					}
				}
			}
		};
		mSendHandler.postDelayed(mSendTask, 100);
	}*/
	/**
	 * <p>Starts recording from the microphone with the given sample rate.</p>
	 *
	 * @throws IOException if recorder could not be created
	 */
	private void startRecording(int recordingRate, AudioConsumer audioConsumer) throws IOException {
//		VaniRecorder.releaseRecorder();
		mRecorder = new RawAudioRecorder(recordingRate, audioConsumer);
		mRecorder.start();
	}
	/**
	 * Release resources.
	 */
	private void releaseResources() {
		/*if (mSendLooper != null) {
			mSendLooper.quit();
			mSendLooper = null;
		}

		if (mSendHandler != null) {
			mSendHandler.removeCallbacks(mSendTask);
		}*/

		if (mRecorder != null) {
			mRecorder.release();
			mRecorder = null;
		}
	}
	/**
	 * Change status of recorder.
	 * @param state
	 */
	private void setState(State state) {
		mState = state;
	}
	/**
	 * Release resources, reset flags, stop service for next call.
	 */
	public void processContinu(){
		releaseResources(); // mRecorder = null
		setState(State.IDLE);
		stopSelf();
	}
	/**
	 * Release resources, reset status if have error.
	 * @param errorCode
	 * @param e
	 */
	private void processError(int errorCode, Exception e) {
		mErrorCode = errorCode;
		releaseResources();
		setState(State.ERROR);		
	}
}