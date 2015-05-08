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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import android.os.SystemClock;

import com.ibm.cio.dto.QueryResult;
import com.ibm.cio.util.Logger;
import com.ibm.cio.util.VaniUtils;
import com.ibm.cio.watsonsdk.SpeechDelegate;

// TODO: Auto-generated Javadoc
/**
 * Uploader for non-streaming mode.
 */
public class VaniNoneStreamUploader implements VaniUploader{
    // Use PROPRIETARY notice if class contains a main() method, otherwise use
    // COPYRIGHT notice.
    public static final String COPYRIGHT_NOTICE = "(c) Copyright IBM Corp. 2013";
    private static final String TAG = VaniNoneStreamUploader.class.getName();
    /** Output stream. */
    private OutputStream rawWriter;
    /** LMC cookies. */
    private String lmcCookie;
    /** Server url. */
    private String serverURL;
    /** Encoder. */
    private VaniEncoder encoder;
    /** Directory for save data. */
    private String baseDir;
    /** Flag <code>true/<code>false</code>. <code>True</code> if uploader has been prepared. */
    public boolean uploadPrepared = true;
    private long requestEstablishingTime = 0;
    private long dataTransmissionTime = 0;
    private long requestTime = 0;
    private long responseTime = 0;
    private long beginSendRequest = 0;
    //	private int timeout = 15000;
    private SpeechDelegate delegate;
    /**
     * Create a non-streaming uploader.
     *
     * @param encoder the encoder
     * @param serverURL the server url
     * @param lmcCookie the lmc cookie
     * @param baseDir file path to save audio recording data
     */
    public VaniNoneStreamUploader(VaniEncoder encoder, String serverURL, String lmcCookie, String baseDir) {
        this.encoder = encoder;
        this.serverURL = serverURL;
        this.lmcCookie = lmcCookie;

        this.baseDir = baseDir;
        this.requestTime = 0;
    }
    /**
     * None-streaming mode. Write recorded audio data to file.
     *
     * @param buffer the buffer
     */
    public int onHasData(byte[] buffer, boolean needEncode) {
        return writeBufferToFile(buffer, needEncode);
    }

    /* (non-Javadoc)
     * @see com.ibm.cio.audio.VaniUploader#isUploadPrepared()
     */
    public boolean isUploadPrepared() {
        return uploadPrepared;
    }

    /**
     * Write header code to audio file.
     */
    public void prepare() {
        try {
            rawWriter = new FileOutputStream(getPCMFilePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the pCM file path.
     *
     * @return the pCM file path
     */
    private String getPCMFilePath() {
        return baseDir + "vaniAudio";
    }

    /**
     * None-streaming mode. Write recording audio data to file
     * @param buffer the audio data
     */
    private int writeBufferToFile(byte[] buffer, boolean needEncode) {
        int savedAudioSize = 0;
        try {
            if (needEncode) {
                byte[] tmp = encoder.encode(buffer);
                savedAudioSize = tmp.length;
                rawWriter.write(tmp); // Write buffer to file
            } else {
                savedAudioSize = buffer.length;
                rawWriter.write(buffer); // Write buffer to file
            }

            rawWriter.flush();
        } catch (Exception e) {
            Logger.e(TAG, "Error occured in updateListener, recording is aborted");
            e.printStackTrace();
        }
        return savedAudioSize;
    }

    /* (non-Javadoc)
     * @see com.ibm.cio.audio.VaniUploader#getQueryResultByAudio(long)
     */
    public QueryResult getQueryResultByAudio(long timeout) {
        return transcript();
    }

    /**
     * Get transcript from {@link HttpsURLConnection}.
     *
     * @return the query result
     * {@link QueryResult} </br>
     * null if {@link Exception}
     */
    private QueryResult transcript() {
        String urlString = this.serverURL;
        Logger.d(TAG, "Post to Url=" + VaniUtils.getHidedPassword(urlString));
        URL url;
        try {
            url = new URL(urlString);
            // VaniUtils.passUntrustedCertificate();
            // this does no network IO
            // Begin uploading audio
            long t0 = SystemClock.elapsedRealtime();
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Content-Type", "binary/octet-stream");
            connection.setAllowUserInteraction(true);

            connection.setRequestProperty("Cookie", lmcCookie);
            // this opens a connection
            connection.connect();
            OutputStream out = connection.getOutputStream();
            beginSendRequest = SystemClock.elapsedRealtime();
            // write data to http request
//			writeAudioDataToStream(out);
            encoder.initEncodeAndWriteHeader(out);
            long endInitConn = SystemClock.elapsedRealtime();
            requestEstablishingTime = endInitConn - t0;
            File myFile = new File(getPCMFilePath());
            FileInputStream fis = new FileInputStream(myFile);
            int len = (int) myFile.length();
            Logger.d(TAG, "the length of saved file: "+ len);
            byte[] b = new byte[len];
            len = fis.read(b);
            long endReadFile = SystemClock.elapsedRealtime();
//			bufferDataTime = endReadFile - endInitConn;
            encoder.writeChunk(b);
//			encoder.encodeAndWrite(b);
            // End uploading audio
//			dataTransmissionTime = SystemClock.elapsedRealtime() - endReadFile;
//			requestTime = SystemClock.elapsedRealtime() - t0;
            Logger.d(TAG, "uploading end! " + len);
            fis.close();
            out.flush();
            out.close();
            dataTransmissionTime = SystemClock.elapsedRealtime() - endReadFile;
            requestTime = SystemClock.elapsedRealtime() - t0;
            Logger.e(TAG, "[test] dataTransmissionTime: " + dataTransmissionTime + "|requestTime: " + requestTime);
            long beginGetResponse = SystemClock.elapsedRealtime();
            InputStream is = connection.getInputStream();
            responseTime = SystemClock.elapsedRealtime() - beginGetResponse;
            Logger.d(TAG, "responseTime non streaming: " + responseTime);
            QueryResult result = VaniUtils.parseQueryResult(is);

            is.close();
            connection.disconnect();

            return result;
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch (ProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } /*
		 * catch (NoSuchAlgorithmException e) { // TODO Auto-generated catch
		 * block System.out.println("NoSuchAlgorithmException");
		 * e.printStackTrace(); return null; } catch (KeyManagementException e)
		 * { System.out.println("KeyManagementException"); // TODO
		 * Auto-generated catch block e.printStackTrace(); return null; }
		 */catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Non-streaming uploader. Get audio data from file, encode if necessary end write to {@link HttpsURLConnection}
     *
     * @param out the {@link OutputStream}
     * @throws Exception the exception
     */
    private void writeAudioDataToStream(OutputStream os) throws Exception {
        encoder.initEncodeAndWriteHeader(os);
        File myFile = new File(getPCMFilePath());
        FileInputStream fis = new FileInputStream(myFile);
        int len = fis.read();
        while(len > 0){
            byte[] b= new byte[4096];//4k
            len = fis.read(b);
            encoder.encodeAndWrite(b);
        }
        fis.close();
    }

    /* (non-Javadoc)
     * @see com.ibm.cio.audio.VaniUploader#close()
     */
    @Override
    public void close() {
        try{
            rawWriter.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* (non-Javadoc)
     * @see com.ibm.cio.audio.VaniUploader#stopUploaderPrepareThread()
     */
    @Override
    public void stopUploaderPrepareThread() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.ibm.cio.audio.VaniUploader#stopGetQueryResultByAudio()
     */
    @Override
    public boolean stopGetQueryResultByAudio() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getUploadErrorCode() {
        // TODO Auto-generated method stub
        return 0;
    }
    @Override
    public long getRequestTime() {
        // TODO Auto-generated method stub
        return requestTime;
    }
    @Override
    public long getResponseTime() {
        // TODO Auto-generated method stub
        return responseTime;
    }
    @Override
    public long getDataTransmissionTime() {
        // TODO Auto-generated method stub
        return 0;
    }
    @Override
    public long getRequestEstablishingTime() {
        // TODO Auto-generated method stub
        return requestEstablishingTime;
    }
    @Override
    public void setTimeout(int timeout) {
        // TODO Auto-generated method stub
    }
    @Override
    public void setDelegate(SpeechDelegate delegate) {
        // TODO Auto-generated method stub
        this.delegate = delegate;
    }
}
