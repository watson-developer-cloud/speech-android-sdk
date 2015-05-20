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
// TODO: Auto-generated Javadoc

/**
 * Voice activity detector.
 *
 * @author Turta@crl.ibm
 */
public class VadProcessorJNI {
	// Use PROPRIETARY notice if class contains a main() method, otherwise use
	// COPYRIGHT notice.
	public static final String COPYRIGHT_NOTICE = "(c) Copyright IBM Corp. 2013";
	/** Default sample rate. */
	private static final int DEFAULT_SAMPLERATE = 16000;

	/** Default frame size. */
	private static final int DEFAULT_FRAMESIZE = 320;

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
