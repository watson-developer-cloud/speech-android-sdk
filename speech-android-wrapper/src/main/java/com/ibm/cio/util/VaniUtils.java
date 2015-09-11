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
import android.os.SystemClock;

import com.ibm.cio.dto.QueryResult;

/**
 * Utilities.
 * @author chienlk
 *
 */
public class VaniUtils {
	// Use PROPRIETARY notice if class contains a main() method, otherwise use
	// COPYRIGHT notice.
	public static final String COPYRIGHT_NOTICE = "(c) Copyright IBM Corp. 2013";
	private static final String TAG = VaniUtils.class.getName();

	/**
	 * <p>Get data from input stream.</p>
	 *
	 * @param is the input stream
	 * @return data in byte array <br />
	 * 		   [] if IOException
	 */
	public static byte[] stream2bytes(InputStream is) {
		try {
			long beginReadIs = SystemClock.elapsedRealtime();
			BufferedInputStream bis = new BufferedInputStream(is);
			ByteArrayBuffer baf = new ByteArrayBuffer(16384);
			int nRead;
			byte[] bf = new byte[4096];
			long downloadDataTime = 0;
			long readStep = SystemClock.elapsedRealtime();
			while ((nRead = bis.read(bf)) != -1) {
				Logger.d(TAG, "time get read is real read: " + nRead + "|time: " + (SystemClock.elapsedRealtime() - readStep));
				downloadDataTime += (SystemClock.elapsedRealtime() - readStep);
				baf.append(bf, 0, nRead);
				readStep = SystemClock.elapsedRealtime();
			}
			Logger.d(TAG, "time get read is: " + (SystemClock.elapsedRealtime() - beginReadIs) + ", data lenght="+baf.toByteArray().length);
			return baf.toByteArray();
			
			/*ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int nRead;
			byte[] data = new byte[16384];
			while ((nRead = is.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}
			buffer.flush();

			return buffer.toByteArray();*/
		} catch (IOException e) {
			// LMC Cookies expired -> File not found, EOFExecption???
			e.printStackTrace();
		}
        return new byte[0];
	}
	public static byte[] stream2bytes2(InputStream is) {
		try {
			long beginReadIs = SystemClock.elapsedRealtime();
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			int nRead;
			byte[] bf = new byte[4096];
			Logger.d(TAG, "time get read is data available: " + is.available());
			long readStep = SystemClock.elapsedRealtime();
			while ((nRead = is.read(bf)) != -1) {
				Logger.d(TAG, "time get read is real read: " + nRead + "|time: " + (SystemClock.elapsedRealtime() - readStep));
				bos.write(bf, 0, nRead);
				readStep = SystemClock.elapsedRealtime();
			}
			Logger.d(TAG, "time get read is 2: " + (SystemClock.elapsedRealtime() - beginReadIs));
			bos.flush();
			return bos.toByteArray();
			
			/*ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int nRead;
			byte[] data = new byte[16384];
			while ((nRead = is.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}
			buffer.flush();

			return buffer.toByteArray();*/
		} catch (IOException e) {
			e.printStackTrace();

			return new byte[0];
		}
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
	 * Gets the hided password.
	 *
	 * @param urlString the url string
	 * @return the hided password
	 */
	public static String getHidedPassword(String urlString) {
		String ret = "";
		
		int idx1 = urlString.indexOf("password");
		int idx2 = urlString.indexOf("&", idx1);
		
		ret = urlString.substring(0, idx1) + "password=xxx" + urlString.substring(idx2, urlString.length());
		
		return ret;
	}
	
	/**
	 * Pass untrusted certificate.
	 *
	 * @throws NoSuchAlgorithmException the no such algorithm exception
	 * @throws KeyManagementException the key management exception
	 */
	public static void passUntrustedCertificate() throws NoSuchAlgorithmException, KeyManagementException {
		Logger.i(TAG, "passUntrustedCertificate");
		TrustManager[] trustAllCerts = new TrustManager[] {
				new X509TrustManager() {
					
					@Override
					public X509Certificate[] getAcceptedIssuers() {
						// TODO Auto-generated method stub
						return null;
					}
					
					@Override
					public void checkServerTrusted(X509Certificate[] arg0, String arg1)
							throws CertificateException {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void checkClientTrusted(X509Certificate[] arg0, String arg1)
							throws CertificateException {
						// TODO Auto-generated method stub
						
					}
				}	
			};
			
			SSLContext sslc = SSLContext.getInstance("SSL");
			sslc.init(null, trustAllCerts, new SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sslc.getSocketFactory());
			HostnameVerifier allHostValid = new HostnameVerifier() {
				
				@Override
				public boolean verify(String arg0, SSLSession arg1) {
					// TODO Auto-generated method stub
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
	
	public static void saveWavFile(byte[] d, String filePath) {
		Logger.e(TAG, "saveWavFile: " + filePath);
		PcmWaveWriter wR = new PcmWaveWriter(8000, 1);
		String fileName = filePath + "a.wav";
		try {
			wR.open(fileName);
			wR.writeHeader("by jspeex");
			wR.writePacket(d, 0, d.length);
			wR.close();
			Logger.i(TAG, "save file OK");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Logger.d(TAG, "save file FAIL");
			e.printStackTrace();
		}
	}
	
	public static void saveRawFile(byte[] d, String filePath) {
		Logger.e(TAG, "saveRawFile: " + filePath);
//		String fileName = filePath + "a.pcm";
		try {
			OutputStream os = new FileOutputStream(filePath);
			os.write(d);
			os.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/*try {
			wR.open(fileName);
			wR.writeHeader("by jspeex");
			wR.writePacket(d, 0, d.length);
			wR.close();
			Logger.i(TAG, "save file OK");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Logger.d(TAG, "save file FAIL");
			e.printStackTrace();
		}*/
	}
}
