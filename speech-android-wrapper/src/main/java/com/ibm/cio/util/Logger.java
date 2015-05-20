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
package com.ibm.cio.util;

import android.util.Log;
/**
 * Logging utility for development process.
 * @author chienlk
 *
 */
public class Logger {
	private static boolean DEBUG = true;
	
	public static void i(String tag, String msg) {
		if (DEBUG)
			Log.i(tag, msg);
	}
	
	public static void d(String tag, String msg) {
		if (DEBUG)
			Log.d(tag, msg);
	}
	
	public static void e(String tag, String msg) {
		if (DEBUG)
			Log.e(tag, msg);
	}
	
	public static void w(String tag, String msg) {
		if (DEBUG)
			Log.w(tag, msg);
	}
}
