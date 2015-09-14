/**
 * Copyright IBM Corporation 2015
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

package com.ibm.crl.speech.vad;

import com.ibm.cio.dto.SpeechConfiguration;

/**
 * Voice activity detector.
 *
 * @author Turta@crl.ibm
 */
public class VadProcessorJNI {
	// Use PROPRIETARY notice if class contains a main() method, otherwise use COPYRIGHT notice.
	public static final String COPYRIGHT_NOTICE = "(c) Copyright IBM Corp. 2015";
	/** Default sample rate. */
	private static final int DEFAULT_SAMPLERATE = SpeechConfiguration.SAMPLE_RATE;

	/** Default frame size. */
	private static final int DEFAULT_FRAMESIZE = SpeechConfiguration.FRAME_SIZE*2; // Leave original configuration for VAD

	/** The slot. */
	private final int slot;

	/**
	 * Instantiates a new VAD processor with default frame size and sample rate.
	 */
	public VadProcessorJNI() {
		this(DEFAULT_FRAMESIZE, DEFAULT_SAMPLERATE);
	}

	/**
	 * Instantiates a new VAD processor with default frame size and specified
	 * sample rate.
	 * 
	 * @param sampelrate
	 *            the sample rate
	 */
	public VadProcessorJNI(int sampelrate) {
		this(DEFAULT_FRAMESIZE, sampelrate);
	}

	/**
	 * Instantiates a new VAD processor with specified frame size and sample
	 * rate.
	 * 
	 * @param framesize
	 *            the frame size
	 * @param samplerate
	 *            the sample rate
	 */
	public VadProcessorJNI(int framesize, int samplerate) {
		slot = allocate(framesize, samplerate);
	}

	static {
		try {
			// Load and link with VAD lib
			System.loadLibrary("vad");
		} catch (java.lang.UnsatisfiedLinkError ex) {
			ex.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		deallocate(slot);
	}

	/**
	 * Process audio chunk.
	 * 
	 * @param bytes
	 *            the audio data to process
	 * @return the length of data has been processed
	 */
	public synchronized int preprocessChunk(byte[] bytes) {
		byte[] tmp = null;
		tmp = preprocessChunk(slot, bytes);
		return (tmp == null) ? 0 : tmp.length;
	}

	/**
	 * Check if user has stopped speaking.
	 * 
	 * @return true, if successful
	 */
	public synchronized boolean predicVAD() {
		return isPausing();
	}

	/**
	 * Process audio chunk.
	 * 
	 * @param slot
	 *            the slot
	 * @param bytes
	 *            the audio data to process
	 * @return the data has been processed
	 */
	private native static byte[] preprocessChunk(int slot, byte[] bytes);

	/**
	 * Allocate slot to process data.
	 * 
	 * @param framesize
	 *            the frame size
	 * @param samplerate
	 *            the sample rate
	 * @return the slot has been allocated
	 */
	protected native static int allocate(int framesize, int samplerate);

	/**
	 * Deallocate the slots has been allocated.
	 * 
	 * @param slot
	 *            the slot has been allocated
	 */
	protected native static void deallocate(int slot);

	/**
	 * Check if user has stopped speaking.
	 * 
	 * @return true, if is pausing
	 */
	protected native static boolean isPausing();
}
