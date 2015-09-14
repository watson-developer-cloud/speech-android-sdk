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
