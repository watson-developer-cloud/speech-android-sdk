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

package com.ibm.watson.developer_cloud.android.speech_common.v1.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.util.ByteArrayBuffer;
import org.xiph.speex.PcmWaveWriter;

import android.content.Context;
import android.os.Environment;

/**
 * Speech Utility.
 * @author chienlk
 *
 */
public class SpeechUtility {
	// Use PROPRIETARY notice if class contains a main() method, otherwise use COPYRIGHT notice.
	public static final String COPYRIGHT_NOTICE = "(c) Copyright IBM Corp. 2015";
	private static final String TAG = SpeechUtility.class.getName();

	/**
	 * <p>Get data from input stream.</p>
	 *
	 * @param is the input stream
	 * @return data in byte array <br />
	 * 		   [] if IOException
	 */
	public static byte[] stream2bytes(InputStream is) {
		try {
			BufferedInputStream bis = new BufferedInputStream(is);
			ByteArrayBuffer baf = new ByteArrayBuffer(16384);
			int nRead;
			byte[] bf = new byte[4096];
			while ((nRead = bis.read(bf)) != -1) {
				baf.append(bf, 0, nRead);
			}
			return baf.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		}
        return new byte[0];
	}
	public static byte[] stream2bytes2(InputStream is) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			int nRead;
			byte[] bf = new byte[4096];
			while ((nRead = is.read(bf)) != -1) {
				bos.write(bf, 0, nRead);
			}
			bos.flush();
			return bos.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		}
        return new byte[0];
	}

	 /**
 	 * Gets the spx file path.
 	 *
 	 * @param pcmFilePath the pcm file path
 	 * @return the spx file path
 	 */
 	public static String getSpxFilePath(String pcmFilePath) {
		 return pcmFilePath==null ? "": pcmFilePath.replace("pcm", "spx");
    }

	/**
	 * Pass untrusted certificate.
	 *
	 * @throws NoSuchAlgorithmException the no such algorithm exception
	 * @throws KeyManagementException the key management exception
	 */
	public static void passUntrustedCertificate() throws NoSuchAlgorithmException, KeyManagementException {
		TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkServerTrusted(X509Certificate[] arg0, String arg1)
                        throws CertificateException {
                }

                @Override
                public void checkClientTrusted(X509Certificate[] arg0, String arg1)
                        throws CertificateException {
                }
            }
        };
        SSLContext sslc = SSLContext.getInstance("SSL");
        sslc.init(null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sslc.getSocketFactory());
        HostnameVerifier allHostValid = new HostnameVerifier() {
            @Override
            public boolean verify(String arg0, SSLSession arg1) {
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(allHostValid);
	}

	/**
	 * Turn short[] to byte[].
	 *
	 * @param shorts the short
	 * @return the byte[]
	 */
	public static byte[] toBytes(short[] shorts) {
		// to turn shorts back to bytes.
		byte[] bytes2 = new byte[shorts.length * 2];
		ByteBuffer.wrap(bytes2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts);
		
		return bytes2;
	}
	/**
	 * Turn byte[]  to short[].
	 * @param bytes the byte array will be converted.
	 * @param startPos the start index
	 * @param byteNumber the length
	 * @return
	 */
	public static short[] toShorts(byte[] bytes, int startPos, int byteNumber) {
		// to turn bytes to shorts as either big endian or little endian.
		short[] shorts = new short[byteNumber/2];
		ByteBuffer.wrap(bytes, startPos, byteNumber).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);

		return shorts;
	}
	/**
	 * Get directory to save data on external/internal storage.
	 * @param context application context
	 * @return directory
	 */
	public static String getBaseDir(Context context) {
		String baseDir = "";
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			baseDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
		} else {
			baseDir = "/data/data/" + context.getPackageName() + "/";
		}

		return baseDir;
	}
	
	public static boolean saveWavFile(byte[] d, String filePath) {
		PcmWaveWriter wR = new PcmWaveWriter(8000, 1);
		String fileName = filePath + "a.wav";
		try {
			wR.open(fileName);
			wR.writeHeader("by jspeex");
			wR.writePacket(d, 0, d.length);
			wR.close();
			Logger.i(TAG, "save file OK");
            return true;
		} catch (IOException e) {
			Logger.d(TAG, "save file FAIL");
			e.printStackTrace();
		}
        return false;
	}

	public static boolean saveRawFile(byte[] d, String filePath) {
		Logger.e(TAG, "saveRawFile: " + filePath);
		try {
			OutputStream os = new FileOutputStream(filePath);
			os.write(d);
			os.close();
            return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        return false;
	}
}
