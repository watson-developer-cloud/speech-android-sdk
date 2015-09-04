package com.ibm.cio.watsonsdk;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import com.ibm.cio.audio.AudioConsumer;
import com.ibm.cio.audio.ChuckWebSocketUploader;
import com.ibm.cio.audio.RecognizerIntentService;
import com.ibm.cio.audio.RecognizerIntentService.RecognizerBinder;
import com.ibm.cio.audio.RecognizerIntentService.State;
import com.ibm.cio.audio.SpeechConfiguration;
import com.ibm.cio.audio.SpeechEncoder;
import com.ibm.cio.audio.SpeechJNAOpusEnc;
import com.ibm.cio.audio.SpeechJNISpeexEnc;
import com.ibm.cio.audio.VaniNoneStreamUploader;
import com.ibm.cio.audio.SpeechRawEnc;
import com.ibm.cio.audio.VaniRecorder;
import com.ibm.cio.audio.VaniStreamUploader;
import com.ibm.cio.audio.VaniUploader;
import com.ibm.cio.audio.player.PlayerUtil;
import com.ibm.cio.dto.QueryResult;
import com.ibm.cio.util.Logger;
import com.ibm.cio.util.TTSPlugin;
import com.ibm.cio.util.VaniUtils;
import com.ibm.crl.speech.vad.RawAudioRecorder;

public class VaniManager {

    // English based speech models
    public static final String SPEECH_MODEL_en_US_GEN_16000 = "en_US_GEN_16000"; 	// general English US accent model from broadcast news
    public static final String SPEECH_MODEL_en_UK_GEN_16000 = "en_UK_GEN_16000"; 	// general english UK accent model, from eu parliamentary speeches mostly uk english
    public static final String SPEECH_MODEL_en_US_IBM_16000 = "en_US_IBM_16000"; 	// ibm specific english model, including ibm terms
    public static final String SPEECH_MODEL_en_US_CI3_16000 = "en_US_CI3_16000"; 	// this is a name recognition model, populated with ibm employee data
    public static final String SPEECH_MODEL_en_US_MED_16000 = "en_US_MED_16000"; 	// military medical
    public static final String SPEECH_MODEL_en_US_GO2_16000 = "en_US_GO2_16000"; 	// cio vani 4 model, names and commands
    public static final String SPEECH_MODEL_en_US_GO6_16000 = "en_US_GO6_16000";    // new cio vani model for names and commands
    public static final String SPEECH_MODEL_en_IN_GEN_16000 = "en_IN_GEN_16000"; 	// indian accent model
    public static final String SPEECH_MODEL_en_US_GEN_8000 = "en_US_GEN_8000"; 		// trained from large vocabulary telephone conversations
    public static final String SPEECH_MODEL_en_IN_GEN_8000 = "en_IN_GEN_8000"; 		// trained from large vocabulary telephone conversations

    // Chinese speech models
    public static final String SPEECH_MODEL_zh_CN_GEN_16000 = "zh_CN_GEN_16000"; 	// broadcast news Mandarin
    public static final String SPEECH_MODEL_zh_CN_FMB_16000 = "zh_CN_FMB_16000";
    public static final String SPEECH_MODEL_zh_CN_GEN_8000 = "zh_CN_GEN_8000";		// trained from large vocabulary telephone conversations

    // Arabic
    public static final String SPEECH_MODEL_ar_IQ_MED_16000 = "ar_IQ_MED_16000"; 	// iraq
    public static final String SPEECH_MODEL_ar_SA_GEN_16000 = "ar_SA_GEN_16000"; 	// saudi arabia
    public static final String SPEECH_MODEL_ar_IQ_GEN_8000 = "ar_IQ_GEN_8000"; 		// trained from large vocabulary telephone conversations

    // Spanish
    public static final String SPEECH_MODEL_es_ES_GEN_16000 = "es_ES_GEN_1600";
    public static final String SPEECH_MODEL_es_ES_MCM_16000 = "es_ES_MCM_16000";
    public static final String SPEECH_MODEL_es_ES_GEN_8000 = "es_ES_GEN_8000";

    // Japanese
    public static final String SPEECH_MODEL_ja_JP_GEN_16000 = "ja_JP_GEN_16000";
    public static final String SPEECH_MODEL_ja_JP_GEN_8000 = "ja_JP_GEN_8000"; 		// trained from large vocabulary telephone conversations
    public static final String SPEECH_MODEL_ja_JP_FIN_8000 = "ja_JP_FIN_8000"; 		// trained from large vocabulary telephone conversations at financial institutions

    // Farsi (Iran)
    public static final String SPEECH_MODEL_fa_IR_GEN_16000 = "fa_IR_GEN_16000";

    // application types
    public static final String VANI_SERVICE_FACES = "faces";
    public static final String VANI_SERVICE_ANSWERS = "answers";
    public static final String VANI_SERVICE_BPM = "BPM";

    private static final String TAG = VaniManager.class.getName();
    private long gettingTranscriptTimeout = 10000;
    private boolean useStreaming;
    private boolean useCompression;
    private boolean useTTS;
    private boolean useVAD;
    private boolean useWebSocket;
    private boolean useVaniBackend;
    private boolean isCertificateValidationDisabled;

    private String sessionCookie;
    private String speechModel;
    private String vaniService;
    private String itransUsername;
    private String itransPassword;
    private URI vaniHost;
    private String ttsServer;
    private String ttsPort;
    public String facesResult;
    public String transcript;

    private VaniRecorder mRecorder;
    private VaniUploader uploader;
    /** Audio encoder. */
    private SpeechEncoder encoder;
    private TTSPlugin ttsPlugin;

    private Context appCtx;
    private Thread prepareThread;
    private Thread onHasDataThread;
    boolean shouldStopRecording;
    boolean doneUploadData;
    boolean stillDoesNotCallStartRecord = false;
    private SpeechDelegate delegate = null;

    AudioManager mAm;
	/*For VAD*/
    /** Update the byte count every 250 ms. */
    private static final int TASK_BYTES_INTERVAL = 100;//250;
    /** Start the task almost immediately. */
    private static final int TASK_BYTES_DELAY = 100; //to be edit 10 = immediately
    /** Time interval to check for VAD pause / max time limit. */
    private static final int TASK_STOP_INTERVAL = 0;//600;
    /** Delay of stopping runnable. */
    private static final int TASK_STOP_DELAY = 1000;//1500;
//	private static final int DELAY_AFTER_START_BEEP = 200;
    /** Max recording time in milliseconds (VAD timeout). */
    private int mMaxRecordingTime = 5000;
    /** Max thinking time in milliseconds. */
    private int THINKING_TIMEOUT = 15000; // 30000
    /** UPLOADING TIIMEOUT  */
    private int UPLOADING_TIMEOUT = 0; // default duration of closing connection
    /** Handler to schedule stopping runnable. */
    private Handler mHandlerStop = new Handler();
    /** Stopping runnable. */
    private Runnable mRunnableStop;
    /** Handler to schedule save audio data runnable. */
    private Handler mHandlerBytes = new Handler();
    /** Save audio data runnable. */
    private Runnable mRunnableBytes;
    /** Service to record audio. */
    private RecognizerIntentService mService;
    private boolean mStartRecording = false;
    /** Flag <code>true/<code>false</code>. <code>True</code> if recording service was connected. */
    private boolean mIsBound = false;
    /** Recorded audio data. */
    private BlockingQueue<byte[]> recordedData;
//	private BlockingQueue<byte[]> compressedData;
    /** Current offset of audio data. */
    private int currentOffset = 0;
    /** Number chunk of recorded audio data. */
    private volatile int numberData = 0;
//	private volatile int numberCompressedData = 0;
    /** Length of uploaded raw audio data. */
//	private int audioUploadedLength = 0;
    /** Length of uploaded spx audio data. */
//	private int spxAudioUploadedLength = 0;
    /** Main activity. */
//	private Activity mActivity;
    /** Begin thinking (recognizing, query and return result) time. */
    private long beginThinking = 0;
//	private long tUploadChunkDone = 0;
//	private int dataBufferTime = 0;
//	private long requestTransmisionTime = 0;
    /** Flag <code>true/<code>false</code>. <code>True</code> if user has tapped on "X" button to dismiss recording diaLogger. */
    private volatile boolean isCancelled = false;
    /** Flag <code>true/<code>false</code>. <code>True</code> if user has tapped on mic button to stop recording process. */
    private volatile boolean stopByUser = false;
    /** Flag <code>true/<code>false</code>. <code>True</code> if stopVadRecording action has been called. */
//	private boolean calledStopAction = false;
    /** Callback function */
//	private SpeechDelegate returnTranscription;
    /** Monitor status (started/stopped) of service. */
    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            Logger.i(TAG, "Service connected");
            mService = ((RecognizerBinder) service).getService();

            if (mStartRecording && ! mService.isWorking()) {
                startRecording();
            } else {
//				setGui();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
            Logger.i(TAG, "Service disconnected");
        }
    };
    /**
     * Connect to recording service {@link RecognizerIntentService}.
     */
    private void doBindService() {
        try {
            // This can be called also on an already running service
            this.appCtx.startService(new Intent(this.appCtx, RecognizerIntentService.class));

            this.appCtx.bindService(new Intent(this.appCtx, RecognizerIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
            mIsBound = true;
            Logger.i(TAG, "Service is bound");
        } catch (Exception e) {
            // TODO: handle exception
            Logger.e(TAG, "FAIL doBindService");
            e.printStackTrace();
        }

    }
    /**
     * Disconnect from recording service {@link RecognizerIntentService}.
     */
    private void doUnbindService() {
        if (mIsBound) {
            mService.stop();
            this.appCtx.unbindService(mConnection);
            mIsBound = false;
            mService = null;
            Logger.i(TAG, "Service is UNBOUND");
        }
//		callbackContext.success(); // return callback to "stopVadService" action
    }

    public Context getAppCtx() {
        return appCtx;
    }

    public void setAppCtx(Context appCtx) {
        this.appCtx = appCtx;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

    /**
     * Constructor of VaniManager
     */
    public VaniManager() {
        this.setUseTTS(false);
        this.setUseCompression(true);
        this.setUseStreaming(false);
        this.setSpeechModel(SPEECH_MODEL_en_US_CI3_16000);
        this.setCertificateValidationDisabled(false);
        this.setVaniService(VANI_SERVICE_FACES);
        this.setItransUsername("test");
        this.setItransPassword("test");
    }

    /**
     * Constructor of VaniManager
     */
    public VaniManager(URI uri, Context ctx) {
        this.setVaniHost(uri);

        // set default values
        this.setUseTTS(false);
        this.setUseCompression(true);
        this.setUseStreaming(false);
        this.setSpeechModel(SPEECH_MODEL_en_US_CI3_16000);
        this.setCertificateValidationDisabled(false);
        this.setVaniService(VANI_SERVICE_FACES);
        this.setItransUsername("test");
        this.setItransPassword("test");
        this.appCtx = ctx;
    }

    private static VaniManager _instance = null;
    public static VaniManager sharedInstance(){
        if(_instance == null){
            synchronized(VaniManager.class){
                _instance = new VaniManager();
            }
        }
        return _instance;
    }
    /**
     * Creates a <code>VaniManager</code> with VAD on.
     */
    public VaniManager(URI uri, Context ctx, boolean isUsingVad) {
        // set default values
        this.setUseVAD(true);
        this.setUseTTS(false);
        this.setUseCompression(true);
        this.setUseStreaming(false);
        this.setSpeechModel(SPEECH_MODEL_en_US_CI3_16000);
        this.setCertificateValidationDisabled(false);
        this.setVaniService(VANI_SERVICE_FACES);
        this.setItransUsername("test");
        this.setItransPassword("test");
        this.initWithContext(uri, ctx, isUsingVad);
    }
    public void initWithContext(URI uri, Context ctx, boolean isUsingVad){
        this.setVaniHost(uri);
        this.appCtx = ctx;
        if(isUsingVad)
            this.initVadService();
        else
            this.doUnbindService();
    }
    /**
     * Get Vani delegate
     *
     * @return SpeechDelegate
     */
    public SpeechDelegate getDelegate(){
        return this.delegate;
    }
    /**
     * Set Vani delegate
     */
    public void setDelegate(SpeechDelegate delegate){
        this.delegate = delegate;
    }
    /**
     * Send message to the delegate
     *
     * @param code
     * @param result
     */
    private void sendMessage(int code, QueryResult result){
        if(this.delegate != null){
            Logger.w(TAG, "INVOKING sendMessage FROM VANI MANAGER");
            this.delegate.receivedMessage(code, result);
        }
        else{
            Logger.w(TAG, "INVOKING sendMessage FAILED FROM VANI MANAGER");
        }
    }
    /**
     * Remove any pending post of Runnable. Stop recording service and reset flags.
     */
    private void stopServiceRecording() {
        Logger.i(TAG, "stopServiceRecording by user: " + stopByUser);
        shouldStopRecording = true;
        mHandlerBytes.removeCallbacks(mRunnableBytes);
        mHandlerStop.removeCallbacks(mRunnableStop);
        mService.stop(); // state = State.PROCESSING
        handleRecording();
        // save raw file
//		VaniUtils.saveRawFile(mService.getCompleteRecording(), VaniUtils.getBaseDir(mActivity));
        // Return OK to javascript for action "startVadRecording", trigger of "stopVadRecording" action
//		curStartCallbackCtx.success("{'code':0, 'text':'OK'}");
    }

    /**
     * After Logging in, initiate recoder and beep player.
     * Construct Runnable to save audio data.
     * Connect to recording service.
     */
    public void initVadService() {
        Logger.i(TAG, "initVadService");
        RawAudioRecorder.CreateInstance(SpeechConfiguration.SAMPLE_RATE);

//		initBeepPlayer();
        // Save the current recording data each second to array
        mRunnableBytes = new Runnable() {
            public void run() {
                if (mService != null && mService.getLength() > 0) {
                    if (isCancelled) {
                        Logger.i(TAG, "mRunnableBytes is cancelled");
                        return;
                    }
                    try {
//						Logger.i(TAG, "mRunnableBytes is cancelled 2");
                        byte[] allAudio = mService.getCompleteRecording();
                        byte[] tmp = new byte[allAudio.length - currentOffset];
                        System.arraycopy(allAudio, currentOffset, tmp, 0, tmp.length);
                        if (tmp.length > 0) {
                            if (VaniManager.this.useCompression) {
                                // Encode audio before insert to buffer
                                byte[] encodedData = encoder.encode(tmp);
                                Logger.d(TAG, "[Vad] Encoded length="+encodedData.length);
                                recordedData.put(encodedData);
                            } else
                                recordedData.put(tmp);
                            // update currentOffset, numberData
                            // Logger.d(TAG, "[encode] temp size: " + tmp.length);
                            currentOffset += tmp.length;
//							audioUploadedLength += tmp.length;
                            numberData++;
                        }
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                mHandlerBytes.postDelayed(this, TASK_BYTES_INTERVAL);
            }
        };
        // Decide if we should stop recording
        // 1. Max recording time has passed
        // 2. Speaker stopped speaking
        // 3. User tap mic to stop
        mRunnableStop = new Runnable() {
            public void run() {
                if (mService != null) {
                    if (isCancelled) {
                        Logger.i(TAG, "mRunnableStop is cancelled");
                        return;
                    }
                    else if (mMaxRecordingTime < (SystemClock.elapsedRealtime() - mService
                            .getStartTime())) {
                        Logger.i(TAG, "Max recording time exceeded");
                        stopServiceRecording();
                    } else if (mService.isPausing()) {
                        Logger.i(TAG, "Speaker finished speaking");
                        stopServiceRecording();
                    } else if (stopByUser) {
                        Logger.i(TAG, "Stop by USER");
                        stopServiceRecording();
                    } else {
                        mHandlerStop.postDelayed(this, TASK_STOP_INTERVAL);
                    }
                }
            }
        };

        doBindService();
    }

    public URI getVaniHost() {
        return vaniHost;
    }

    public void setVaniHost(URI vaniHost) {
        this.vaniHost = vaniHost;
        setTTSHost(this.vaniHost.toString());
    }

    public void setTTSHost(String url) {
        String ServerPort = url;
        this.ttsServer = ServerPort.substring(ServerPort.indexOf("/")+2,ServerPort.lastIndexOf(":"));
        this.ttsPort = ServerPort.substring(ServerPort.lastIndexOf(":")+1);

    }

    public boolean isUseStreaming() {
        return useStreaming;
    }

    public void setUseStreaming(boolean useStreaming) {
        this.useStreaming = useStreaming;
    }

    public boolean isUseCompression() {
        return useCompression;
    }

    public void setUseCompression(boolean useCompression) {
        this.useCompression = useCompression;
    }

    public boolean isUseTTS() {
        return useTTS;
    }

    public void setUseTTS(boolean useTTS) {
        this.useTTS = useTTS;
    }

    public boolean isUsingWebSocket(){
        return this.useWebSocket;
    }

    public void setUseWebSocket(boolean useWebSocket) {
        this.useWebSocket = useWebSocket;
    }

    public boolean isUseVAD() {
        return useVAD;
    }

    public void setUseVAD(boolean useVAD) {
        this.useVAD = useVAD;
    }

    public boolean isUseVaniBackend() {
        return useVaniBackend;
    }

    public void setUseVaniBackend(boolean useVaniBackend) {
        this.useVaniBackend = useVaniBackend;
    }

    public boolean isCertificateValidationDisabled() {
        return isCertificateValidationDisabled;
    }

    public void setCertificateValidationDisabled(
            boolean isCertificateValidationDisabled) {
        this.isCertificateValidationDisabled = isCertificateValidationDisabled;
    }

    public String getSessionCookie() {
        return sessionCookie;
    }

    public void setSessionCookie(String sessionCookie) {
        this.sessionCookie = sessionCookie;
    }

    public String getSpeechModel() {
        return speechModel;
    }

    public void setSpeechModel(String speechModel) {
        this.speechModel = speechModel;
    }

    public String getVaniService() {
        return vaniService;
    }

    public void setVaniService(String vaniService) {
        this.vaniService = vaniService;
    }

    public String getItransUsername() {
        return itransUsername;
    }

    public void setItransUsername(String itransUsername) {
        this.itransUsername = itransUsername;
    }

    public String getItransPassword() {
        return itransPassword;
    }

    public void setItransPassword(String itransPassword) {
        this.itransPassword = itransPassword;
    }

    public String getITransUrl() {
        String urlString = "";

        if(this.isUsingWebSocket()){
            // /itrans/api/wsAsr?username=test&password=test&type=streaming&store=false&format=opus&outputtype=ctm&model-id=2&save-wav=true&switch-wav=true&timeout=15000
            // TODO: add params for application,
            // TODO: Confirm if model-name could be changed
            urlString = this.vaniHost
                    + "/itrans/api/wsAsr?username="
                    + this.itransUsername
                    + "&password="
                    + this.itransPassword
                    + "&type=streaming&store=false&outputtype=ctm&save-wav=true&timeout=15000&format=" //&encoding=xml&temp=false
                    + (this.useCompression ? "opus" : "PCM")
                    + "&switch-wav=true&model-id=2"; //&model-name=" + this.speechModel;
//			urlString = this.vaniHost.toString();
            Logger.d(TAG, "###########################");
            Logger.d(TAG, urlString);
            Logger.d(TAG, "###########################");
            return urlString;
        }

        if (this.useVaniBackend) {
            // /queryAllByAudio435?username=faces&password=faces4vani&store=true&outputtype=plaintext&save-wav=true&timeout=15000&encoding=xml&temp=false&ttsType=spx&model-name=en_US_CI3_16000&format=spx&switch-wav=true&tts=true&queryService=bpm&canBeCommand=false&fromUser=vaugave@us.ibm.com&apiId=09409477556788&vad=on
            // TODO add params for application,
            urlString = this.vaniHost
                    + "/queryAllByAudio489?username="
                    + this.itransUsername
                    + "&password="
                    + this.itransPassword
                    + "&store=true&outputtype=plaintext&save-wav=false&timeout=15000&encoding=xml&temp=false&ttsType=spx&format="
                    + (this.useCompression ? "spx" : "PCM")
                    + "&switch-wav=true&model-name="
                    + this.speechModel
                    + "&tts="
                    + (this.useTTS ? "true" : "false")
                    + "&queryService="
                    + this.vaniService
                    + "&canBeCommand=false&fromUser=smartrob@uk.ibm.com&apiId=09409477556788&vad=off";
        }
        else {
            urlString = this.vaniHost
                    + "/itrans/api/synTranscription?username="
                    + this.itransUsername
                    + "&password="
                    + this.itransPassword
                    + "&store=false&outputtype=plaintext&save-wav=false&timeout=15000&encoding=xml&temp=false&format="
                    + (this.useCompression ? "spx" : "PCM")
                    + "&switch-wav=true&model-name=" + this.speechModel;
        }

        Logger.e(TAG, urlString);

        return urlString;
    }

    private String getBaseDir() {
        String baseDir;
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            baseDir = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + "/";
        } else {
            // baseDir = "/data/data/" + this.appCtx.getPackageName() + "/";
            baseDir = this.appCtx.getFilesDir().getPath();
        }

        return baseDir;
    }
    /**
     * 1. Start {@link Handler} to save audio data recorded.
     * <br>
     * 2. Start thread to detect the end moment of recording process.
     */
    private void prepareRecording() {
        Logger.i(TAG, "prepareRecording: " + shouldStopRecording);
        // Schedule save byte runnable
        mHandlerBytes.postDelayed(mRunnableBytes, TASK_BYTES_DELAY);
        // Schedule stopping runnable
        mHandlerStop.postDelayed(mRunnableStop, TASK_STOP_DELAY);
    }
    /**
     * Remove any pending posts of Runnable r that are in the message queue. Clear recorded audio.
     */
    private void stopAllHandler() {
        Logger.i(TAG, "stopAllHandler");
        try {
            isCancelled = true;
//			onHasDataThread.stop();
            mHandlerBytes.removeCallbacks(mRunnableBytes);
            mHandlerStop.removeCallbacks(mRunnableStop);
            recordedData.clear();
//			compressedData.clear();
        } catch (Exception e) {
            // TODO: handle exception
            Logger.d(TAG, "removeCallbacks FAIL");
        }
        // Reset current offset of audio data[]
        currentOffset = 0;
    }
    /**
     * Get transcript and show result. Then, reset all.
     * @param timeout
     */
    public void getVADTranscript(long timeout) {
        QueryResult	result = null;
        long t0 = System.currentTimeMillis();
        result = uploader.getQueryResultByAudio(timeout);
        Logger.i(TAG, "getVADTranscript time = " + (System.currentTimeMillis() - t0));
        Logger.e(TAG, result.getTranscript());
        if (!isCancelled) {
//			showRawResult(result, callbackCtx);
            if (result != null) {
                //Set transcript received from iTrans
                String transcript = result.getTranscript();
                setTranscript(transcript);
                if(this.isUsingWebSocket()){
                    Logger.i(TAG, "this.isUsingWebSocket(): getVADTranscript(long timeout)");
                    this.sendMessage(SpeechDelegate.MESSAGE, result);
                }
                else if (result.getStatusCode() != 401 && result.getStatusCode() != 101 && result.getStatusCode() != 102) { // SUCCESS result
                    if (result.getListFaces().indexOf('{')!=-1 && result.getListFaces().indexOf('{') == result.getListFaces().lastIndexOf('{')) {
                        //only one
                    } else {
                        if (isUseTTS() && mAm.getRingerMode() == 2) { // ringtone mode = AudioManager.RINGER_MODE_NORMAL
                            if ("".equals(result.getTranscript())) // I don't understand
                                PlayerUtil.ins8k.playIdontUnderstand(getAppCtx());
                            else if (result.getTtsIFound().length > 0) {
                                PlayerUtil.ins8k.playSPX(result.getTtsIFound());
                            }
                        }
                    }
                    facesResult = result.toSuccessJson().toString();

                    if(getVaniService().equals(VANI_SERVICE_FACES)){
//						this.returnTranscription.transcriptionFinishedCallback(facesResult);
                        this.sendMessage(SpeechDelegate.MESSAGE, result);
                        //
                    }else{
                        //For non FACES Service
//						this.returnTranscription.transcriptionFinishedCallback(getTranscript());
                        this.sendMessage(SpeechDelegate.MESSAGE, result);
                    }
                } else { // FAILURE result, statusCode = 101 (Time out)/102 (Cancel all)/401 (IOException)
                    Logger.w(TAG, "Failed to get transcription");
//					this.returnTranscription.transcriptionErrorCallback(result.toFailureJson());
                    this.sendMessage(SpeechDelegate.ERROR, result);
                }
            }
            else {
                Logger.w(TAG, "Query result: ERROR code 401");
                if (isUseTTS() && mAm.getRingerMode() == 2)
                    PlayerUtil.ins8k.playIdontUnderstand(getAppCtx());
            }
            stopAllHandler();
            mService.processContinu();
        } else
            Logger.i(TAG, "getVADTranscript has been cancelled");

    }
    /**
     * Will be called after VAD detecting or VAD timeout.<br>
     * Waiting for audio data has been uploaded, then get query result and return to Javascript.
     * @param callbackCtx callback context of action
     */
    private void finishRecord() {
        Logger.i(TAG, "finishRecord");
        beginThinking = SystemClock.elapsedRealtime();
        // Listen to onHasDataThread for getting result of recognizing
        if (!doneUploadData) // DON'T wait when data has been uploaded (when recording time quite long)
            synchronized (uploader) {
                try {
                    uploader.wait(THINKING_TIMEOUT + 1000); // Wait for done upload data. Active after 5s if NOT received notification
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
//		VaniUtils.saveWavFile(mService.getCompleteRecording(), VaniUtils.getBaseDir(mActivity));
        Logger.i(TAG, "finishRecord upload done at: " + new Date().getTime() + "|" + numberData + "|" + isCancelled);
        final long gettingTranscriptTimeout = THINKING_TIMEOUT - (SystemClock.elapsedRealtime() - beginThinking);
        Logger.i(TAG, "gettingTranscriptTimeout = " + gettingTranscriptTimeout);
        if (!isCancelled) {
            if (gettingTranscriptTimeout > 0) {
                if (!uploader.isUploadPrepared()) { // FAIL to prepare connection for uploading audio
                    Logger.e(TAG, "uploader prepare thread NOT done!!!");
                    if (onHasDataThread != null)
                        onHasDataThread.interrupt();
                    uploader.stopUploaderPrepareThread();
                    stopAllHandler();
                    mService.processContinu();

                    QueryResult result = null;

                    if (uploader.getUploadErrorCode() < 0) {
                        result = new QueryResult(QueryResult.CONNECTION_CLOSED, QueryResult.CONNECTION_CLOSED_MESSAGE);
//						showResult("{'code':103, 'text':'Connection reset or closed by peer', 'jobId':''}", callbackCtx);
                    } else {
                        result = new QueryResult(QueryResult.CONNECTION_FAILED, QueryResult.CONNECTION_FAILED_MESSAGE);
//						showResult("{'code':100, 'text':'Network is unreachable', 'jobId':''}", callbackCtx);	
                    }
//					showResult(result.toFailureJson(), callbackCtx);
                    isCancelled = true; // To stop onHasDataThread if failed interrupt it
//					this.returnTranscription.transcriptionErrorCallback(result.toFailureJson());
                    this.sendMessage(SpeechDelegate.ERROR, result);
                } else {
                    getVADTranscript(gettingTranscriptTimeout);
                }
            } else { // Timeout prepare uploader (>15s), alert "Thinking timeout"
//				showResult(new QueryResult(QueryResult.TIME_OUT, QueryResult.TIME_OUT_MESSAGE).toFailureJson(), callbackCtx);
                if (onHasDataThread != null)
                    onHasDataThread.interrupt();
                isCancelled = true; // To stop onHasDataThread if failed interrupt it
                uploader.stopUploaderPrepareThread();
                stopAllHandler();
                mService.processContinu();
                Logger.i(TAG, "Timeout prepare uploader (>15s)");
//				this.returnTranscription.transcriptionErrorCallback(new QueryResult(QueryResult.TIME_OUT, QueryResult.TIME_OUT_MESSAGE).toFailureJson());
                this.sendMessage(SpeechDelegate.ERROR, new QueryResult(QueryResult.TIME_OUT, QueryResult.TIME_OUT_MESSAGE));
            }
        } else {
            Logger.i(TAG, "Thinking cancelled");
//			this.returnTranscription.transcriptionErrorCallback(new QueryResult(QueryResult.CANCEL_ALL, QueryResult.CANCEL_ALL_MESSAGE).toFailureJson());
            this.sendMessage(SpeechDelegate.ERROR, new QueryResult(QueryResult.CANCEL_ALL, QueryResult.CANCEL_ALL_MESSAGE));
//			callbackCtx.success("{'code':102, 'text':'Thinking cancelled', 'jobId':''}");
        }
    }
    /**
     * Control recording process based on its status ({@link RecognizerIntentService}).
     */
    private void handleRecording() {
        if (mService == null) {
            return;
        }
        switch(mService.getState()) {
            case RECORDING:
                prepareRecording();
                break;
            case PROCESSING:
                finishRecord();
                break;
            default:
                break;
        }
    }
    private class STTAudioConsumer implements AudioConsumer {

        private VaniUploader mUploader = null;

        public STTAudioConsumer(VaniUploader uploader) {

            mUploader = uploader;
        }

        public void consume(byte [] data) {
            //Logger.i(TAG, "consume called with " + data.length + " bytes");
            mUploader.onHasData(data, isUseCompression());
        }

        @Override
        public void onAmplitude(double amplitude, double volume) {
            Logger.d(TAG, "####### volume=" + volume);
        }
    }

    /**
     * Start recording process:
     * 1. Prepare uploader. Start thread to listen if have audio data, then upload it.
     * <br>
     * 2. Start service to record audio.
     */
    private void startRecordingWithVAD() {
        Logger.i(TAG, "startRecordingWithVAD");
//		doneUploadData = false;
//		shouldStopRecording = false;
        isCancelled = false;
        numberData = 0;
//		numberCompressedData = 0;
//		tUploadChunkDone = 0;
//		dataBufferTime = 0;
        recordedData = new LinkedBlockingQueue<byte[]>();
//		compressedData = new LinkedBlockingQueue<byte[]>();
//		calledStopAction = false;
        if (mIsBound) {
            if (mService.getState() == State.RECORDING) {
                stopServiceRecording();
            } else {
                // Prepare uploader with thread
                uploader.prepare();
                onHasDataThread = new Thread() { // wait for uploading audio data
                    public void run() {
                        while (!isCancelled) {
                            // uploader prepare FAIL or uploading data DONE, notify to stop recording
                            // NOTE: Need time to have recording audio data
                            if ((shouldStopRecording && numberData == 0)/* || !uploader.isUploadPrepared()*/) {
                                doneUploadData = true;
//								requestTransmisionTime = SystemClock.elapsedRealtime() - uploader.getBeginSendRequest();
                                synchronized (uploader) {
                                    uploader.notify();
                                }
                                break;
                            }
                            try {
                                if (numberData > 0) {
                                    byte[] dataToUpload = recordedData.take();
                                    if (dataToUpload != null) {
//										long tHasData = SystemClock.elapsedRealtime();
//										if (tUploadChunkDone > 0) {
//											dataBufferTime += (tHasData - tUploadChunkDone);
//											Logger.d(TAG, "bufferDataTime trace: " + (tHasData - tUploadChunkDone));
//										}
                                        uploader.onHasData(dataToUpload, false); // synchronize
//										tUploadChunkDone = SystemClock.elapsedRealtime();
//										audioUploadedLength += dataToUpload.length;
//										spxAudioUploadedLength += dataToUpload.length;
                                        numberData--;
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    }
                };
                onHasDataThread.setName("onHasDataThread");
                onHasDataThread.start();

                if (mService.init()) { // State = State.INITIALIZED
                    Logger.i(TAG, "startServiceRecording");
//					audioUploadedLength = 0;
//					spxAudioUploadedLength = 0;
                    STTAudioConsumer audioConsumer = new STTAudioConsumer(uploader);
                    mService.start(SpeechConfiguration.SAMPLE_RATE, audioConsumer); // recording was started, State = State.RECORDING
                    handleRecording();
                }
            }
        } else {
            mStartRecording = true;
            doBindService();
        }
    }

    private void startRecordingWithoutVAD() {
        Logger.i(TAG, "startRecordingWithoutVAD");
        try {
            mRecorder = VaniRecorder.createVaniRecorder(this, uploader); //createVaniRecorder();
            mRecorder.prepare();

            if (isCertificateValidationDisabled()) { // Need to pass untrusted
                // certificate
                Log.i(TAG, "Use VPN & HTTPs");
                VaniUtils.passUntrustedCertificate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // playBeep();
        Thread t = new Thread() {
            public void run() {
                if (shouldStopRecording){
                    Logger.e(TAG, "####### ERROR: shouldStopRecording=true");
                }
                else{
                    Logger.i(TAG, "### doStartRecord()");
                    doStartRecord();
                }
            };
        };
        t.setName("startRecording");
        t.start();
    }

    /**
     * Change default timeout
     *
     * @param timeout
     */
    public void setTimeout(int timeout){
        this.UPLOADING_TIMEOUT = timeout;
    }

    /**
     * Start recording audio:
     * <p>
     * 1. Create and prepare recorder ({@link VaniRecorder}) </br> 2. Play beep
     * </p>
     * @throws VaniException
     */
    public void startRecording() {
        Log.i(TAG, "startRecording");
        shouldStopRecording = false;
        // stillDoesNotCallStartRecord = true;
        mAm = (AudioManager) appCtx.getSystemService(Context.AUDIO_SERVICE);
        doneUploadData = false;
        // Initiate Uploader, Encoder
        Log.i(TAG, "### USING WEBSOCKET="+(this.isUsingWebSocket()?"Yes":"No")+"");
        Log.i(TAG, "### USING STREAMING="+(this.isUseStreaming()?"Yes":"No")+"");
        Log.i(TAG, "### USING COMPRESSION="+(this.isUseCompression()?"Yes":"No")+"");
        Log.i(TAG, "### USING VAD="+(this.isUseVAD()?"Yes":"No")+"");
        if (this.isUsingWebSocket()){
            encoder = new SpeechJNAOpusEnc();

        }
        else if (this.useCompression) {
            encoder = new SpeechJNISpeexEnc();
        }
        else {
            encoder = new SpeechRawEnc();
        }
        if (this.isUsingWebSocket()){
            try {
                HashMap<String, String> header = new HashMap<String, String>();
                header.put("Cookie", this.sessionCookie);
                uploader = new ChuckWebSocketUploader(encoder, getITransUrl(), header, new SpeechConfiguration());

            } catch (URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else if (this.useStreaming) {
            uploader = new VaniStreamUploader(encoder, getITransUrl(), this.sessionCookie);
        }
        else {
            uploader = new VaniNoneStreamUploader(encoder, getITransUrl(), this.sessionCookie, getBaseDir());
        }
        uploader.setTimeout(UPLOADING_TIMEOUT); // default timeout
        uploader.setDelegate(this.delegate);
        if (this.useVAD) { // Record audio in service
            startRecordingWithVAD();
        } else {
            startRecordingWithoutVAD();
        }
    }

    /**
     * Start {@link AudioRecord}
     */
    private void doStartRecord() {
        // IMPORTANT: Have to check if user did NOT release touchend event to
        // stop recording before call start recording
//		Logger.w(TAG, "mRecorder == null: "+(mRecorder == null)+", shouldStopRecording: "+shouldStopRecording);
        if (mRecorder != null && !shouldStopRecording) {
            mRecorder.start();
            // Prepare uploader
            prepareThread = new Thread() {
                public void run() {
                    uploader.prepare();
                    // mRecorder.prepare();
                };
            };
            prepareThread.start();

            // stillDoesNotCallStartRecord = false;
            // Create one thread to listen from
            // AudioRecord.OnRecordPositionUpdateListener and upload data
            onHasDataThread = new Thread(){
                public void run() {
                    while (true) {
                        // uploader prepare FAIL or uploading data DONE, notify
                        // to stop recording
                        // NOTE: Need time to have recording audio data
                        if ((shouldStopRecording && mRecorder.onPeriodicNotificationWaitCount == 0)){
//								|| !uploader.isUploadPrepared()) {
                            // doneUploadData = true;
                            synchronized (VaniManager.this) {
                                VaniManager.this.notify();
                            }
                            Logger.e(TAG, "return");
                            break;
                        }
                        try {
                            if (mRecorder.onPeriodicNotificationWaitCount == 0)
                                synchronized (mRecorder) {
                                    mRecorder.wait(3000); // Wait for having
                                    // audio data to
                                    // upload. Active
                                    // after 3s if NOT
                                    // received
                                    // notification
                                    // (when the last
                                    // notification has
                                    // been risen but
                                    // not received)
                                }

                            if (mRecorder.onPeriodicNotificationWaitCount > 0) {
                                // wait finish prepare!
                                try {
//									System.out.println("join prepareThread");
//									Logger.e(TAG, "prepareThread.join();");
                                    prepareThread.join();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
//								System.out.println("mRecorder.uploadData();");
                                mRecorder.uploadData();
//								System.out.println("mRecorder.onPeriodicNotificationWaitCount--");
                                mRecorder.onPeriodicNotificationWaitCount--;
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                }
            };
            onHasDataThread.setName("onHasDataThread");
            onHasDataThread.start();
        }
    }

    /**
     * Stop recording audio:
     * <p>
     * 1. Stop {@link AudioRecord} </br> 2. Get transcript
     * </p>
     */
    public void stopRecording() {
        System.out.println("stopRecording");
        shouldStopRecording = true;
        if (mRecorder != null) {
            if (stillDoesNotCallStartRecord) {
                System.out.println("WARN: stillDoesNotCallStartRecord!");

                releaseAll();
                // TODO surface result
                // showRawResult("{'code':0, 'text':'', 'jobId':''}");
                return;
            }

            mRecorder.stop();
            // Listen to onHasDataThread for getting result of recognizing
            synchronized (this) {
                try {
                    this.wait(10000); // Wait for done upload data. Active after
                    // 10s if NOT received notification
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (!uploader.isUploadPrepared()) { // FAIL to prepare connection
                // for uploading audio
                // TODO surface result
                // showRawResult("{'code':100, 'text':'Network is unreachable', 'jobId':''}");
            } else
                mRecorder.getTranscript(mAm.getRingerMode(), gettingTranscriptTimeout);

            // mRecorder.release();
        } else {
            // TODO surface result
            // showRawResult("{'code':3, 'text':'Error, you call stop recording before init it!', 'jobId':''}");
        }
    }

    /**
     * Play beep and record after
     *
     * public void playBeep() { initBeepPlayer(); if
     * (mAm.getStreamVolume(AudioManager.STREAM_MUSIC) != 0) { Log.i(TAG,
     * "beepPlayer start: " + beepPlayer.getDuration() + "|" +
     * beepPlayer.getCurrentPosition()); beepPlayer.start(); } else {
     * System.out.println("beepPlayer end"); this.beepCompleted(); }
     *
     * }
     */
	/*
	 * private void initBeepPlayer() { Log.i(TAG, "initBeepPlayer"); beepPlayer
	 * = MediaPlayer.create(this.appCtx, R.raw.beep);
	 * 
	 * 
	 * beepPlayer.setOnCompletionListener(new OnCompletionListener() { public
	 * void onCompletion(MediaPlayer mp) { RecorderPlugin.this.beepCompleted();
	 * // beepPlayer.reset(); } });
	 * 
	 * beepPlayer.setOnErrorListener(new OnErrorListener() {
	 * 
	 * @Override public boolean onError(MediaPlayer mp, int what, int extra) {
	 * Log.d(TAG, "beepPlayer onError: " + what + "|" + extra); return false; }
	 * }); }
	 */

    private void releaseAll() {
        if (mRecorder != null) {
            mRecorder.close();
        }
    }

    /**
     * Functon to get faces result string after QueryResult
     *
     * @param result
     */
    public void showFacesResult(String result) {
        facesResult = result;
        System.out.println(result);
    }

    public void playTtsForString(String ttsString) {
        Log.i(TAG, "playTtsForString called");
        String[] Arguments = { this.sessionCookie, this.ttsServer,
                this.ttsPort, this.itransUsername, this.itransPassword,
                ttsString, "en_US", "8000", "spx" };
//		for(int i=0;i<Arguments.length;i++){
//			System.out.println(Arguments[i]);
//		}
        setUseTTS(true);
        try {
            ttsPlugin= new TTSPlugin();
            ttsPlugin.tts(Arguments);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Log.e(TAG, "Error calling TTSplugin");
            e.printStackTrace();
        }
    }

    public void setTranscript(String itransTranscript) {
        this.transcript = itransTranscript;
    }

    public String getTranscript(){
        return this.transcript;
    }

    /**
     * Get all supported language models
     *
     * @return
     */
    public static HashMap<String, String> getSupportedLanguageModels(){
        HashMap<String, String> list = new HashMap<String, String>();
//		list.put(SPEECH_MODEL_ar_IQ_GEN_8000, SPEECH_MODEL_ar_IQ_GEN_8000);
        list.put(SPEECH_MODEL_ar_IQ_MED_16000, SPEECH_MODEL_ar_IQ_MED_16000);
        list.put(SPEECH_MODEL_ar_SA_GEN_16000, SPEECH_MODEL_ar_SA_GEN_16000);
        list.put(SPEECH_MODEL_en_IN_GEN_16000, SPEECH_MODEL_en_IN_GEN_16000);
//		list.put(SPEECH_MODEL_en_IN_GEN_8000, SPEECH_MODEL_en_IN_GEN_8000);
        list.put(SPEECH_MODEL_en_UK_GEN_16000, SPEECH_MODEL_en_UK_GEN_16000);
        list.put(SPEECH_MODEL_en_US_CI3_16000, SPEECH_MODEL_en_US_CI3_16000);
        list.put(SPEECH_MODEL_en_US_GEN_16000, SPEECH_MODEL_en_US_GEN_16000);
//		list.put(SPEECH_MODEL_en_US_GEN_8000, SPEECH_MODEL_en_US_GEN_8000);
        list.put(SPEECH_MODEL_en_US_GO2_16000, SPEECH_MODEL_en_US_GO2_16000);
        list.put(SPEECH_MODEL_en_US_IBM_16000, SPEECH_MODEL_en_US_IBM_16000);
        list.put(SPEECH_MODEL_en_US_MED_16000, SPEECH_MODEL_en_US_MED_16000);
        list.put(SPEECH_MODEL_es_ES_GEN_16000, SPEECH_MODEL_es_ES_GEN_16000);
//		list.put(SPEECH_MODEL_es_ES_GEN_8000, SPEECH_MODEL_es_ES_GEN_8000);
        list.put(SPEECH_MODEL_es_ES_MCM_16000, SPEECH_MODEL_es_ES_MCM_16000);
        list.put(SPEECH_MODEL_fa_IR_GEN_16000, SPEECH_MODEL_fa_IR_GEN_16000);
//		list.put(SPEECH_MODEL_ja_JP_FIN_8000, SPEECH_MODEL_ja_JP_FIN_8000);
        list.put(SPEECH_MODEL_ja_JP_GEN_16000, SPEECH_MODEL_ja_JP_GEN_16000);
//		list.put(SPEECH_MODEL_ja_JP_GEN_8000, SPEECH_MODEL_ja_JP_GEN_8000);
        list.put(SPEECH_MODEL_zh_CN_FMB_16000, SPEECH_MODEL_zh_CN_FMB_16000);
        list.put(SPEECH_MODEL_zh_CN_GEN_16000, SPEECH_MODEL_zh_CN_GEN_16000);
//		list.put(SPEECH_MODEL_zh_CN_GEN_8000, SPEECH_MODEL_zh_CN_GEN_8000);
        return list;
    }

    public void setRecorderDelegate(SpeechRecorderDelegate obj){
        encoder.setDelegate(obj);
    }
}
