package com.ibm.crl.speech.vad;

/**
 * @author Turta@crl.ibm
 */
public class Log {

	private static final boolean DEBUG = false;

	public static final String LOG_TAG = "com.ibm.crl.speech";
	public static final String LOG_T ="@turta:";

	public static void i(String msg) {
		if (DEBUG) android.util.Log.i(LOG_TAG, msg);
	}

	public static void e(String msg) {
		if (DEBUG) android.util.Log.e(LOG_TAG, msg);
	}

	public static void i(String tag, String msg) {
		if (DEBUG) android.util.Log.i(tag, msg);
	}

	public static void e(String tag, String msg) {
		if (DEBUG) android.util.Log.e(tag, msg);
	}
}