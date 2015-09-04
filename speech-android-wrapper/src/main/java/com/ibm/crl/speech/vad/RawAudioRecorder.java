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
package com.ibm.crl.speech.vad;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.SystemClock;

import com.ibm.cio.audio.AudioConsumer;
import com.ibm.cio.util.Logger;
// TODO: Auto-generated Javadoc

/**
 * Audio recorder.
 *
 * @author Turta@crl.ibm
 */
public class RawAudioRecorder {
	// Use PROPRIETARY notice if class contains a main() method, otherwise use
	// COPYRIGHT notice.
	public static final String COPYRIGHT_NOTICE = "(c) Copyright IBM Corp. 2013";
	/** The Constant RESULT_AUDIO_ERROR. */
	public final static int RESULT_AUDIO_ERROR  = 1;
	
	/** The Constant RESULT_VAD_ERROR. */
	public final static int RESULT_VAD_ERROR  = 2;

	/** The Constant LOG_TAG. */
	private static final String LOG_TAG = RawAudioRecorder.class.getName();

	/** Default audio source. */
	private static final int DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.VOICE_RECOGNITION;
	
	/** Default sample rate. */
	private static final int DEFAULT_SAMPLE_RATE = 16000;
	
	/** Default audio data format. */
	private static final int RESOLUTION = AudioFormat.ENCODING_PCM_16BIT;
	
	/** The Constant RESOLUTION_IN_BYTES. */
	private static final short RESOLUTION_IN_BYTES = 2;
	
	/** Default audio channel. */
	private static final short CHANNELS = 1;
	
	/** Audio recorder. */
	private AudioRecord mRecorder = null;
	
	/** Voice activity detector. */
	private static VadProcessorJNI sp = null;
	
	/** Sample rate. */
	private final int mSampleRate;
	
	/** Buffer size. */
	private int mBufferSize;

	/** Number of frames written to byte array on each output. */
	private int mFramePeriod;
	
	/** The raw recorded audio data. */
	private byte[] rawData;
	
	/** Recorded audio data length. */
	private int mRecordedLength = 0;
	/** Buffer. */
	private byte[] mBuffer;
	private long beginRecordingTime = 0;
	private long recordingTime = 0;

    private AudioConsumer mAudioConsumer = null;
	
//	private volatile boolean recording;

	/**
	 * Instantiates a new raw audio recorder with specified audio source and sample rate.
	 *
	 * @param audioSource Identifier of the audio source (e.g. microphone)
	 * @param sampleRate Sample rate (e.g. 16000)
	 */
	public RawAudioRecorder(int audioSource, int sampleRate) {
		mSampleRate = sampleRate;
		rawData =  new byte[RESOLUTION_IN_BYTES * CHANNELS * mSampleRate * 35];
		try {
			setBufferSizeAndFramePeriod();
			mRecorder = new AudioRecord(audioSource, mSampleRate, AudioFormat.CHANNEL_IN_MONO, RESOLUTION, mBufferSize);
			CreateInstance(mSampleRate);
			mBuffer = new byte[mFramePeriod * RESOLUTION_IN_BYTES * CHANNELS];
//			Logger.d(LOG_TAG, "construct rawaudiorecorder");
		} catch (Exception e) {
			release();
			if (e.getMessage() == null) {
				Logger.e(LOG_TAG, "Unknown error occured while initializing recording");
			} else {
				Logger.e(LOG_TAG, e.getMessage());
			}
		}
	}
	
	/**
	 * Creates the Voice activity detector.
	 *
	 * @param mSampleRate the sample rate of audio
	 */
	public static void CreateInstance(int mSampleRate){
		if(sp==null) {
			Logger.i(LOG_TAG, "create sp");
			sp = new VadProcessorJNI(mSampleRate);
			byte[] pad = new byte[5120];
			for (int i=0; i<10; i++) {
				sp.preprocessChunk(pad);
			}
		}
	}
	/**
	 * Instantiates a new audio recorder with default audio source and specified sample rate.
	 *
	 * @param sampleRate the sample rate
	 */
	public RawAudioRecorder(int sampleRate, AudioConsumer audioConsumer) {
		this(DEFAULT_AUDIO_SOURCE, sampleRate);
        this.mAudioConsumer = audioConsumer;
	}
	/**
	 * Instantiates a new audio recorder with default audio source and sample rate.
	 */
	public RawAudioRecorder() {
		this(DEFAULT_AUDIO_SOURCE, DEFAULT_SAMPLE_RATE);
	}
	/**
	 * Reads audio data from the audio hardware for recording into a buffer.
	 *
	 * @param recorder the recorder
	 * @return the number of bytes that were read
	 */
	private int read(AudioRecord recorder) {
		int numberOfBytes = recorder.read(mBuffer, 0, mBuffer.length);
//		Logger.d(LOG_TAG, "read audio: " + mBuffer.length + "|" + numberOfBytes);
		if (numberOfBytes == 0 || numberOfBytes == AudioRecord.ERROR_INVALID_OPERATION
				|| numberOfBytes == AudioRecord.ERROR_BAD_VALUE) {
			Logger.e(LOG_TAG, "Read zero bytes");

            if(numberOfBytes==0){Logger.d(LOG_TAG,"as number of bytes are zero");}
            if(numberOfBytes == AudioRecord.ERROR_INVALID_OPERATION){Logger.d(LOG_TAG,"ERROR INVALID OP");}
            if(numberOfBytes == AudioRecord.ERROR_BAD_VALUE){Logger.d(LOG_TAG,"ERROR BAD VAL");}

            return -1;
		}else{
            long v = 0;
            for (int i = 0; i < mBuffer.length; i++) {
                v += mBuffer[i] * mBuffer[i];
            }
            double amplitude = v / (double) numberOfBytes;
            double volume = 0;
            if(amplitude > 0)
                volume = 10 * Math.log10(amplitude);
            this.mAudioConsumer.onAmplitude(amplitude, volume);

            add(mBuffer, numberOfBytes);
        }
		return numberOfBytes;
	}
	/**
	 * Sets the buffer size and frame period.
	 */
	private void setBufferSizeAndFramePeriod() {
		mFramePeriod = (16000/1000) * 160;
		mBufferSize = (mFramePeriod * 2 * 16 * 1) / 8;
//		Logger.d(LOG_TAG, "bufferSize="+ mBufferSize + "|framePeriod=" + mFramePeriod);
//		Logger.d(LOG_TAG, "buffersize min: " + AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, RESOLUTION));
		 if (mBufferSize < AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, RESOLUTION))
		 { // Check to make sure buffer size is not smaller than the smallest allowed one 
			 mBufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, RESOLUTION);
			 // Set frame period and timer interval accordingly
			 mFramePeriod = mBufferSize / ( 2 * 16 * 1 / 8 );
		 }
		 
		 mBufferSize = mBufferSize*5;
	}
	/**
	 * Get all recored audio data.
	 *
	 * @return bytes that have been recorded since the beginning
	 */
	public byte[] getCompleteRecording() {
		return getCurrentRawRecording(7040); // REMOVE 0.22 s at first to remove noise
//		return getCurrentRawRecording(0);
	}
	/**
	 * Gets the current recorded audio data.
	 *
	 * @param startPos the starting index of the content
	 * @return the data
	 */
	private byte[] getCurrentRawRecording(int startPos) {
		if (getLength() > startPos) {
			int len = getLength() - startPos;
			byte[] bytes = new byte[len];
			System.arraycopy(rawData, startPos, bytes, 0, len);
			return bytes;	
		} else
			return new byte[0];
	}
	/**
	 * Gets the length of recorded audio data.
	 *
	 * @return the length
	 */
	public int getLength() {
		return mRecordedLength;
	}
	/**
	 * Checks if user has stopped speaking.
	 *
	 * @return true, if is pausing
	 */
	public boolean isPausing() {
		return sp.predicVAD();
	}
	/**
	 * Release the native AudioRecord resource.
	 */
	public synchronized void release()
	{
		if (mRecorder != null) {
			if (mRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
				stop();
			}
			mRecorder.release();
			mRecorder = null;
		}
	}
	/**
	 * <p>Starts the recording, and sets the state to RECORDING.</p>
	 */
	public void start() {
		if (mRecorder != null && mRecorder.getState() == AudioRecord.STATE_INITIALIZED) {
			beginRecordingTime = SystemClock.elapsedRealtime();
			mRecorder.startRecording();
//			mRecorder.read(mBuffer, 0, mBuffer.length);
			
			if (mRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
				new Thread() {
					public void run() {
						while (mRecorder != null && mRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
							int status = read(mRecorder);
//							if (status < 0) {
//								break;
//							}
						}
					}
				}.start();
			} else {
				Logger.e(LOG_TAG, "startRecording() failed");
			}
		} else {
			Logger.e(LOG_TAG, "start() called on illegal state");
		}
	}
	/**
	 * Stop recording.
	 */
	public void stop() {
		Logger.d(LOG_TAG, "stop");
//		recording = false;
		
		if (mRecorder != null && mRecorder.getState() == AudioRecord.STATE_INITIALIZED &&
				mRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
			try {
				mRecorder.stop();
				recordingTime = SystemClock.elapsedRealtime() - beginRecordingTime;
			} catch (IllegalStateException e) {
				Logger.e(LOG_TAG, "native stop() called in illegal state: " + e.getMessage());
			}
		} else {
			Logger.e(LOG_TAG, "stop() called in illegal state");
		}
	}
	/**
	 * <p>Copy the given byte array into the total recording array and process it by Voice activity detector.</p>
	 * @param buffer audio buffer
	 */
	private void add(byte[] buffer, int length) {
		if (rawData.length >= mRecordedLength + buffer.length) {
			System.arraycopy(buffer, 0, rawData, mRecordedLength, length);
			for (int i=0; i<3; i++) { // speed up VAD detection
				sp.preprocessChunk(buffer);
			}
			// sp.preprocessChunk(buffer);
			mRecordedLength += length;
		} else {
			Logger.e(LOG_TAG, "Recorder buffer overflow: " + mRecordedLength);
			release();
		}
	}
	/**
	 * Gets recent sound pressure.
	 *
	 * @return volume indicator that shows the average volume of the last read buffer
	 */
    public float getRmsdb() {
            long sumOfSquares = getRms(mRecordedLength, mBuffer.length);
            double rootMeanSquare = Math.sqrt(sumOfSquares / (mBuffer.length / 2));
            if (rootMeanSquare > 1) {
                    Logger.i(LOG_TAG, "getRmsdb(): " + rootMeanSquare);
                    return (float) (10 * Math.log10(rootMeanSquare));
            }
            return 0;
    }
	/**
	 * Gets the sum of squares of samples which is extracted from recorded audio data.
	 *
	 * @param end the length of recorded audio data
	 * @param span the span
	 * @return the sum of squares of samples
	 */
	private long getRms(int end, int span) {
		int begin = end - span;
		if (begin < 0) {
			begin = 0;
		}
		// make sure begin is even
		if (0 != (begin % 2)) {
			begin++;
		}

		long sum = 0;
		for (int i = begin; i < end; i+=2) {
			short curSample = (short) (rawData[i]| rawData[i+1]<<8);
			sum += curSample * curSample;
		}
		return sum;
	}
	public long getRecordingTime() {
		return recordingTime;
	}
}
