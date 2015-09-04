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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.HttpsURLConnection;

import android.os.SystemClock;

import com.ibm.cio.dto.QueryResult;
import com.ibm.cio.util.Logger;
import com.ibm.cio.util.VaniUtils;
import com.ibm.cio.watsonsdk.SpeechDelegate;

// TODO: Auto-generated Javadoc
/**
 * Uploader for streaming mode.
 */
public class VaniStreamUploader implements VaniUploader{
    // Use PROPRIETARY notice if class contains a main() method, otherwise use
    // COPYRIGHT notice.
    public static final String COPYRIGHT_NOTICE = "(c) Copyright IBM Corp. 2013";
    /** The Constant TAG. */
    private static final String TAG = VaniStreamUploader.class.getName();

    /** The Constant HTTP_TIMEOUT. */
    private static final int HTTP_TIMEOUT = 30*1000;//ms

    /** Thread for initiate a connection to back end. */
    private Thread initStreamToServerThread;

    /** Http connection. */
    private HttpURLConnection mConn;
//	private BufferedOutputStream mOs;
    /** Output stream. */
    private OutputStream mOs;

    /** LMC cookies. */
    private String lmcCookie;

    /** Server url. */
    private String serverURL;

//	private boolean isFinishInitStreamToServer;
    /** Flag <code>true/<code>false</code>. <code>True</code> if uploader has been prepared. */
    public boolean uploadPrepared = false;
    private long beginRequestTime = 0;
    private long requestEstablishingTime = 0;
    private long requestTime = 0;
    private long beginGetResponse = 0;
    private long responseTime = 0;
    private long dataTransmissionTime = 0;
    private long beginSendRequest = 0;
//	private BlockingQueue<byte[]> tmpData;
    /**
     * Error code when upload.
     */
    public int uploadErrorCode = 0;
    //	private int timeout = 15000;
    private SpeechDelegate delegate;

    /** Encoder. */
    private SpeechEncoder encoder;

    /** {@link Future} to get transcript from HttpURLConnection. */
    private Future<QueryResult> future = null;

    /** Thread pool to execute get transcript task. */
    private ExecutorService executor = Executors.newCachedThreadPool();

    /** Get query result task. */
    private Callable<QueryResult> getQueryResultTask = new Callable<QueryResult>() {

        @Override
        public QueryResult call() throws IOException {
            // TODO Auto-generated method stub
//			Logger.d(TAG, "get IS thread: " + Thread.currentThread().getId() + "|" + Thread.currentThread().getName());
            QueryResult queryResult = null;
            InputStream responseIs = null;
            responseIs = mConn.getInputStream();
            byte[] reponseData = VaniUtils.stream2bytes(responseIs);
            responseTime = SystemClock.elapsedRealtime() - beginGetResponse;
            Logger.d(TAG, "time get and read IS: " + responseTime);
            queryResult = QueryResult.create(reponseData);

//			sendMessage(SpeechDelegate.MESSAGE, queryResult);
            responseIs.close();

            return queryResult;
        }
    };
    /**
     * Create an uploader which supports streaming.
     *
     * @param encoder the encoder
     * @param serverURL LMC server, delivery to back end server
     * @param lmcCookie the cookie of LMC session
     */
    public VaniStreamUploader(SpeechEncoder encoder, String serverURL, String lmcCookie) {
        Logger.i(TAG, "Construct VaniStreamUploader");

        this.encoder = encoder;
        this.serverURL = serverURL;
        this.lmcCookie = lmcCookie;
//		tmpData = new LinkedBlockingQueue<byte[]>();
    }
    /* (non-Javadoc)
     * @see com.ibm.cio.audio.VaniUploader#isUploadPrepared()
     */
    public boolean isUploadPrepared() {
        return uploadPrepared;
    }
    public int getUploadErrorCode() {
        return uploadErrorCode;
    }
    /**
     * Write audio data to {@link HttpsURLConnection} when have.
     *
     * @param buffer the buffer
     */
    public int onHasData(byte[] buffer, boolean needEncode) {
        return writeBufferToOutputStream(buffer, needEncode);
    }
    /**
     * Start thread to construct a stream audio to back end server.
     */
    public void prepare() {
        uploadPrepared = false;
        initStreamToServerThread = new Thread() {
            public void run() {
                try {
                    initStreamAudioToServer();
                    uploadPrepared = true;
                    Logger.i(TAG, "Finish prepare upload, result = WIN");
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    Logger.i(TAG, "Finish prepare upload, result = FAIL: " + e.getMessage());
                    if (e.getMessage().contains("Connection closed by peer") || e.getMessage().contains("reset by peer") ) {
                        uploadErrorCode = -1;
                    }
                    sendMessage(SpeechDelegate.ERROR, QueryResult.createSimpleResult(e.getMessage()));
                    e.printStackTrace();
                    uploadPrepared = false;
                    mConn.disconnect();
                    sendMessage(SpeechDelegate.CLOSE, QueryResult.createSimpleResult(e.getMessage()));
                }
            };
        };
        initStreamToServerThread.setName("initStreamToServerThread");
        initStreamToServerThread.start();
    }
    /**
     * Send message to the delegate
     *
     * @param code
     * @param message
     */
    private void sendMessage(int code, QueryResult result){
        if(delegate != null){
            Logger.w(TAG, "INVOKING sendMessage FROM STREAM UPLOADER");
            delegate.receivedMessage(code, result);
        }
        else{
            Logger.w(TAG, "INVOKING sendMessage FAILED FROM VANI MANAGER");
        }
    }
    /**
     * Get transcription
     *
     * @return String
     */
    public String getTranscript(){
        return "";
//		return this.transcript == null ? "" : this.transcript;
    }

    /* (non-Javadoc)
     * @see com.ibm.cio.audio.VaniUploader#stopUploaderPrepareThread()
     */
    public void stopUploaderPrepareThread() {
        if (initStreamToServerThread != null) {
            Logger.i(TAG, "stopUploaderPrepareThread");
            initStreamToServerThread.interrupt();
        }
    }
    /**
     * Streaming uploader. Encode audio data if necessary and write it to {@link HttpsURLConnection}.
     * @param buffer the audio data will be uploaded
     */
    private int writeBufferToOutputStream(byte[] buffer, boolean needEncode) {
        int uploadedAudioSize = 0;
        // wait finish prepare!
        if (!uploadPrepared) {
            try {
                Logger.e(TAG, "has data join");
                initStreamToServerThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Logger.d(TAG, "Finish Join prepare upload, result = " + uploadPrepared);
            }
        }
        // Now, we have status of upload preparing
        if (uploadPrepared) { // upload preparing OK
            try {
                // randomAccessWriter.write(buffer); // Write buffer to file, for testing only
//				long t0 = SystemClock.elapsedRealtime();
                if (needEncode) {
                    uploadedAudioSize = encoder.encodeAndWrite(buffer);
                } else
                    encoder.writeChunk(buffer);
                // Write chunk to connection time
//				dataTransmissionTime += (SystemClock.elapsedRealtime() - t0);
//				Logger.d(TAG, "dataTransmissionTime: " + (SystemClock.elapsedRealtime() - t0));
            } catch (IOException e) {
                Logger.e(TAG, "Error occured in writeBufferToOutputStream, recording is aborted");
                sendMessage(SpeechDelegate.ERROR, QueryResult.createSimpleResult(e.getMessage()));
                e.printStackTrace();
            }
        } else {
            Logger.i(TAG, "uploadPrepare when upload to server: " + uploadPrepared);
        }
        return uploadedAudioSize;
    }
    /**
     * 1. Init a {@link HttpsURLConnection} to back end server </br>
     * 2. Init an encorder and write header code to {@link URLConnection}
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void initStreamAudioToServer() throws IOException {
        beginRequestTime = SystemClock.elapsedRealtime();
        Logger.i(TAG, "prepareUploader, initStreamAudioToServer begin at: " + beginRequestTime);
        URL url = new URL(serverURL);
        // this does no network IO
        mConn = (HttpURLConnection) url.openConnection();
        mConn.setRequestMethod("POST");
        mConn.setReadTimeout(HTTP_TIMEOUT); // 15s
        mConn.setDoOutput(true);
        mConn.setDoInput(true);
        mConn.setRequestProperty("Host", url.getHost());
        mConn.setRequestProperty("Connection", "close"); // "Keep-Alive"
        System.setProperty("http.keepAlive", "false");
        mConn.setUseCaches(false);
        mConn.setRequestProperty("Content-Type", "application/octet-stream");
        mConn.setRequestProperty("Cookie", lmcCookie);
        mConn.setChunkedStreamingMode(4096);
        // this opens a connection, then sends POST & headers.
        // Maybe get exception: Connection reset/closed by peer after ~14 time connect. Why???
        mOs = mConn.getOutputStream();
        beginSendRequest = SystemClock.elapsedRealtime();
        requestEstablishingTime += (beginSendRequest - beginRequestTime);
        Logger.e(TAG, "done getOutputStream");
        encoder.initEncodeAndWriteHeader(mOs);
        this.sendMessage(SpeechDelegate.OPEN, QueryResult.createSimpleResult(this.getTranscript()));
        // Init connection and write header time
//		requestEstablishingTime += (SystemClock.elapsedRealtime() - beginRequestTime);
        Logger.i(TAG, "requestEstablishingTime: " + requestEstablishingTime);
    }
    /* (non-Javadoc)
     * @see com.ibm.cio.audio.VaniUploader#stopGetQueryResultByAudio()
     */
    public boolean stopGetQueryResultByAudio() {
        Logger.i(TAG, "stopGetQueryResultByAudio");
        if (future != null)
            return future.cancel(true);
        else
            return false;
    }
    /**
     * Fetch transcript from {@link HttpsURLConnection}.
     *
     * @param timeout timeout of getting {@link QueryResult} (in ms)
     * @return the query result
     * {@link QueryResult} </br>
     * null if {@link IOException}
     */
    private QueryResult fetchTranscript(long timeout) {
//		Logger.e(TAG, "fetchTranscript: "+ tmpData.size());
		/*ByteArrayBuffer baf = new ByteArrayBuffer(16384);
		while (tmpData.size() != 0) {
			try {
				byte[] tm = tmpData.take();
				baf.append(tm, 0, tm.length);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		byte[] d = baf.toByteArray();
		VaniUtils.saveWavFile(d, VaniUtils.getBaseDir(faces_vani.facesVani));*/
        long doneUploadTime = SystemClock.elapsedRealtime();
        dataTransmissionTime = doneUploadTime - beginSendRequest;
        requestTime = doneUploadTime - beginRequestTime;
        QueryResult queryResult = null;
        beginGetResponse = doneUploadTime;
        future = executor.submit(getQueryResultTask);
        try {
            queryResult = future.get(timeout, TimeUnit.MILLISECONDS);
//			return queryResult;
        } catch (TimeoutException ex) {
            Logger.i(TAG, "111111111 TimeoutException");
            queryResult = new QueryResult(QueryResult.TIME_OUT, QueryResult.TIME_OUT_MESSAGE); // THINKING was TIMED OUT
            // handle the timeout
        } catch (CancellationException e) {
            Logger.i(TAG, "111111111 CancellationException");
            queryResult = new QueryResult(QueryResult.CANCEL_ALL, QueryResult.CANCEL_ALL_MESSAGE); // has been cancelled
            // handle the interrupts
        } catch (InterruptedException e) {
            Logger.i(TAG, "111111111 InterruptedException");
            queryResult = new QueryResult(QueryResult.CANCEL_ALL, QueryResult.CANCEL_ALL_MESSAGE); // has been cancelled
            // handle the interrupts
        } catch (ExecutionException e) { // Reload app
            Logger.i(TAG, "111111111 ExecutionException");
            queryResult = new QueryResult(QueryResult.UNKNOWN_ERROR, QueryResult.UNKNOWN_ERROR_MESSAGE);
            // handle other exceptions
        } finally {
            Logger.i(TAG, "finally fetchTranscript");
            if(mConn != null)
                mConn.disconnect();
            mConn = null;
            future.cancel(true);// may or may not desire this
        }
        return queryResult;
    }
    /**
     * Close the encoder and return {@link QueryResult}.
     *
     * @param timeout the timeout
     * @return the query result by audio
     */
    public QueryResult getQueryResultByAudio(long timeout) {
        try {
            encoder.close();
        } catch (Exception e) {
            // TODO: handle exception
            Logger.e(TAG, "encoder close FAIL");
            e.printStackTrace();
        }
        return fetchTranscript(timeout);
    }
    /* (non-Javadoc)
     * @see com.ibm.cio.audio.VaniUploader#close()
     */
    @Override
    public void close() {
        try{
            mConn.disconnect();
        }catch (Exception e) {
            e.printStackTrace();
        }
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
        return dataTransmissionTime;
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
