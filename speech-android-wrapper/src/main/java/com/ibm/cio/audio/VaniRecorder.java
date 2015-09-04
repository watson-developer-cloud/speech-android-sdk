package com.ibm.cio.audio;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.ibm.cio.audio.player.PlayerUtil;
import com.ibm.cio.dto.QueryResult;
import com.ibm.cio.util.Logger;
import com.ibm.cio.watsonsdk.SpeechDelegate;
import com.ibm.cio.watsonsdk.VaniManager;

public class VaniRecorder {
    private final static String TAG = VaniRecorder.class.getName();
    private VaniUploader uploader;
    private VaniManager vani;
    private int audioRecordedLength;

    Thread executorThread = null;
    Thread prepareThread = null;

    BlockingQueue<byte[]> recordedData;

    /**
     * INITIALIZING : recorder is initializing; READY : recorder has been
     * initialized, recorder not yet started RECORDING : recording ERROR :
     * reconstruction needed STOPPED: reset needed
     */
    public enum State {
        INITIALIZING, READY, RECORDING, ERROR, STOPPED
    };

    public static final boolean RECORDING_UNCOMPRESSED = true;
    public static final boolean RECORDING_COMPRESSED = false;

    // The interval in which the recorded samples are output to the file, ms
    private static final int TIMER_INTERVAL = 160;// important, < 120 may make
    // streaming+spx not work.

    // Recorder used for uncompressed recording
    private AudioRecord audioRecorder = null;

    // Stores current amplitude (only in uncompressed mode)
    private int cAmplitude = 0;

    // Recorder state; see State
    private State state;

    // Number of channels, sample rate, sample size(size in bits), buffer size,
    // audio source, sample size(see AudioFormat)
    private short nChannels;
    private int sRate;
    private short bSamples;
    private int bufferSize;
    private int aSource;
    private int aFormat;

    // Number of frames written to file on each output(only in uncompressed
    // mode)
    private int framePeriod;

    // Buffer for output(only in uncompressed mode)
    private byte[] buffer;

    // private Queue onDataQueue;
    public int onPeriodicNotificationWaitCount = 0;

    public static VaniRecorder single;

    /**
     * Create a {@link VaniRecorder}. Initiate {@link AudioRecord}
     *
     *            the {@link VaniManager} vani
     * @param uploader
     *            the {@link VaniUploader}
     * @return {@link VaniRecorder}
     */
    public static VaniRecorder createVaniRecorder(VaniManager vani, VaniUploader uploader) {
        initRecorder();

        VaniRecorder result = single;

        result.cAmplitude = 0;
        result.state = State.INITIALIZING;
        result.vani = vani;
        result.uploader = uploader;
        return result;
    }

    /**
     *
     * Returns the state of the recorder in a RehearsalAudioRecord.State typed
     * object. Useful, as no exceptions are thrown.
     *
     * @return recorder state
     */
    public State getState() {
        return state;
    }

    private void updateAmplitude() {
        if (bSamples == 16) {
            for (int i = 0; i < buffer.length / 2; i++) { // 16bit sample size
                short curSample = getShort(buffer[i * 2], buffer[i * 2 + 1]);
                if (curSample > cAmplitude) { // Check amplitude
                    cAmplitude = curSample;
                }
            }
        } else { // 8bit sample size
            for (int i = 0; i < buffer.length; i++) {
                if (buffer[i] > cAmplitude) { // Check amplitude
                    cAmplitude = buffer[i];
                }
            }
        }
    }
    /*
     * OnRecordPositionUpdateListener
     *
     * Method used for recording.
     */
    private AudioRecord.OnRecordPositionUpdateListener updateListener = new AudioRecord.OnRecordPositionUpdateListener() {
        public void onPeriodicNotification(final AudioRecord recorder) {
            if (state == State.ERROR) {
                audioRecorder.stop();
                return;
            }
            // TODO: optimization is needed
            onPeriodicNotificationWaitCount++;
            final byte[] tmpBuffer = new byte[buffer.length];
            audioRecorder.read(tmpBuffer, 0, tmpBuffer.length); // fill buffer
            Thread t = new Thread() {
                public void run() {
                    try {
                        recordedData.put(tmpBuffer);
                        audioRecordedLength += tmpBuffer.length;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                };
            };
            t.start();
            synchronized (VaniRecorder.this) {
                VaniRecorder.this.notify(); // notify to onHasDataThread of RecorderPlugin
            }
        }
        //
        public void onMarkerReached(AudioRecord recorder) {
            System.out.println("W:onMarkerReached");
        }
    };

    /**
     *
     *
     * Default constructor </br>
     *
     * Instantiates a new recorder, in case of compressed recording the
     * parameters can be left as 0. In case of errors, no exception is thrown,
     * but the state is set to ERROR
     *
     */
    public VaniRecorder(int audioSource, int sampleRate, int channelConfig, int audioFormat) {
        System.out.println("Contructor for StreamedAudioRecorder: "
                + "audioSource=" + audioSource + ",sampleRate=" + sampleRate
                + ",channelConfig=" + channelConfig + ", audioFormat="
                + audioFormat);

        try {
            if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) {
                bSamples = 16;
            } else {
                bSamples = 8;
            }

            if (channelConfig == AudioFormat.CHANNEL_IN_MONO) {
                nChannels = 1;
            } else {
                nChannels = 2;
            }

            aSource = audioSource;
            sRate = sampleRate;
            aFormat = audioFormat;

            framePeriod = (sampleRate / 1000) * TIMER_INTERVAL; // 16*160
            bufferSize = (framePeriod * 2 * bSamples * nChannels) / 8;
            Log.d(VaniRecorder.class.getSimpleName(), "bufferSize="
                    + bufferSize);

            if (bufferSize < AudioRecord.getMinBufferSize(sampleRate,
                    channelConfig, audioFormat)) { // Check to make sure buffer
                // size is not smaller than
                // the smallest allowed one
                int oldBufferSize = bufferSize;
                bufferSize = AudioRecord.getMinBufferSize(sampleRate,
                        channelConfig, audioFormat);
                // Set frame period and timer interval accordingly
                framePeriod = bufferSize / (2 * bSamples * nChannels / 8);
                Log.i(TAG, "Increasing buffer size from " + oldBufferSize
                        + " to " + Integer.toString(bufferSize));
            }

            bufferSize = bufferSize * 5;

            audioRecorder = new AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize);

            if (audioRecorder.getState() != AudioRecord.STATE_INITIALIZED)
                throw new Exception("AudioRecord initialization failed");

            audioRecorder.setRecordPositionUpdateListener(updateListener);
            audioRecorder.setPositionNotificationPeriod(framePeriod);

            cAmplitude = 0;

            state = State.INITIALIZING;
        } catch (Exception e) {
            if (e.getMessage() != null) {
                Log.e(VaniRecorder.class.getName(), e.getMessage());
            } else {
                Log.e(VaniRecorder.class.getName(),
                        "Unknown error occured while initializing recording");
            }
            e.printStackTrace();
            state = State.ERROR;
        }
    }

    /**
     *
     * Returns the largest amplitude sampled since the last call to this method.
     *
     * @return returns the largest amplitude since the last call, or 0 when not
     *         in recording state.
     *
     */
    public int getMaxAmplitude() {
        if (state == State.RECORDING) {
            int result = cAmplitude;
            cAmplitude = 0;
            return result;
        } else {
            return 0;
        }
    }

    /**
     *
     * Prepares the recorder for recording, in case the recorder is not in the
     * INITIALIZING state and the file path was not set the recorder is set to
     * the ERROR state, which makes a reconstruction necessary. In case
     * uncompressed recording is toggled, the header of the wave file is
     * written. In case of an exception, the state is changed to ERROR
     *
     */
    public void prepare() {
        try {

            if (state == State.INITIALIZING) {
                if ((audioRecorder.getState() == AudioRecord.STATE_INITIALIZED)) {
                    buffer = new byte[framePeriod * bSamples / 8 * nChannels];
                    state = State.READY;
                } else {
                    Log.e(VaniRecorder.class.getName(),
                            "prepare() method called on uninitialized recorder");
                    state = State.ERROR;
                }
            } else {
                Log.e(VaniRecorder.class.getName(),
                        "prepare() method called on illegal state");
                // release();
                state = State.ERROR;
            }
        } catch (Exception e) {
            if (e.getMessage() != null) {
                Log.e(VaniRecorder.class.getName(), e.getMessage());
            } else {
                Log.e(VaniRecorder.class.getName(),
                        "Unknown error occured in prepare()");
            }
            state = State.ERROR;
            e.printStackTrace();
        }
    }

    /**
     *
     *
     * Releases the resources associated with this class, and removes the
     * unnecessary files, when necessary
     *
     */
    public void release() {
        if (state == State.STOPPED) {
            if (audioRecorder != null) {
                Log.d("CALL", "audioRecorder.release()");
                audioRecorder.release();
            }
        } else {
            // plugin.showErrorResult(VaniMessage.INVALID_RECODER_STATE);
            // TODO call delegate
        }

    }

    public void getTranscript(int ringtoneMode, long gettingTranscriptTimeout) {
        System.out.println("getAudioRecordedLengthInMs()="+ this.getAudioRecordedLengthInMs());
        // try {
        QueryResult result = uploader.getQueryResultByAudio(gettingTranscriptTimeout);
        // Query status
        int code = SpeechDelegate.ERROR;
        if (result != null) {
            // Set transcript received from iTrans
            String transcript = result.getTranscript();
            System.out.println("Transcript = "+transcript);
            vani.setTranscript(transcript);

            if (result.getStatusCode() != QueryResult.UNKNOWN_ERROR) {
                code = SpeechDelegate.MESSAGE;
                //Parse Faces data
                if (vani.getVaniService().equals(VaniManager.VANI_SERVICE_FACES)) {
                    if (result.getListFaces().indexOf('{') != -1
                            && result.getListFaces().indexOf('{') == result
                            .getListFaces().lastIndexOf('{')) {
                        // only one
                        System.out.println("QuerResult : Got one result ");
                    }
                    else if(vani.isUsingWebSocket()){
                        System.out.println("SpeechDelegate.MESSAGE: getTranscript(int ringtoneMode, long gettingTranscriptTimeout)");
                        code = SpeechDelegate.MESSAGE;
                    }
                    else {
                        System.out.println("QuerResult : Got faces list ");
                        if (vani.isUseTTS() && ringtoneMode == 2) { // ringtone
                            // mode =
                            // AudioManager.RINGER_MODE_NORMAL
                            System.out.println("QuerResult : "
                                    + result.getListFaces());
                            if ("".equals(result.getTranscript())) // I don't understand
                                PlayerUtil.ins8k.playIdontUnderstand(vani.getAppCtx());
                            else
                                // PlayerUtil.ins8k.playPCM(result.getTtsIFound());
                                PlayerUtil.ins8k.playSPX(result.getTtsIFound());
                        }
                    }
                    // TODO return result
                    String facesResult = "{'code':0, 'text':'"
                            + result.getTranscript() + "', 'jobId':'"
                            + result.getJobId() + "', 'listFaces':"
                            + result.getListFaces()
                            + ", 'audioRecordedLength': "
                            + getAudioRecordedLengthInMs() + ",'s2tTime':"
                            + result.getS2tTime() + ",'ttsTime':"
                            + result.getTtsTime() + ",'qryfaceTime':"
                            + result.getQryFaceTime() + "}";
                    vani.showFacesResult(facesResult);
                }
//				else{
//					vani.setTranscript(result.getListFaces());
//				}
            } else { // statusCode = 401
                // TODO show error result
                System.out.println("QuerResult : Got Status CODE 401");
                // vani.showErrorResult(new
                // VaniMessage(VaniMessage.IO_ERROR_CODE,
                // "Exception when get transcript"));
            }

        } else {
            // plugin.showErrorResult(VaniMessage.NO_ITRANS_RESPONSE);
            System.out.println("QuerResult : Got Null result");
            code = SpeechDelegate.ERROR;
            if (vani.isUseTTS() && ringtoneMode == 2)
                PlayerUtil.ins8k.playIdontUnderstand(vani.getAppCtx());
            // / TODO show result

            // vani.showRawResult("{'code':0, 'text':'', 'jobId':'' " +
            // ", 'audioRecordedLength': " + getAudioRecordedLengthInMs() + "}"
            // );
        }
        this.sendMessage(code, result);
		/*
		 * } catch (IOException e) { e.printStackTrace();
		 * System.out.println("Exception when get transcript:"+e.toString());
		 * plugin.showErrorResult(new VaniMessage(VaniMessage.IO_ERROR_CODE,
		 * e.getMessage())); }
		 */
    }

    /**
     * Send message to the delegate
     *
     * @param code
     * @param result
     */
    private void sendMessage(int code, QueryResult result){
        if(vani.getDelegate() != null){
            Logger.w(TAG, "INVOKING sendMessage FROM STREAM UPLOADER");
            vani.getDelegate().receivedMessage(code, result);
        }
        else{
            Logger.w(TAG, "INVOKING sendMessage FAILED FROM VANI MANAGER");
        }
    }

    /**
     *
     *
     * Resets the recorder to the INITIALIZING state, as if it was just created.
     * In case the class was in RECORDING state, the recording is stopped. In
     * case of exceptions the class is set to the ERROR state.
     *
     */
    public void reset() {
        System.out.println("Resetting....");
        try {
            if (state != State.ERROR) {
                release();
                cAmplitude = 0; // Reset amplitude
                audioRecorder = new AudioRecord(aSource, sRate, nChannels + 1,
                        aFormat, bufferSize);
                state = State.INITIALIZING;
            }
        } catch (Exception e) {
            Log.e(VaniRecorder.class.getName(), e.getMessage());
            state = State.ERROR;
            e.printStackTrace();
        }
    }

    /**
     *
     *
     * Starts the recording, and sets the state to RECORDING. Call after
     * prepare().
     *
     */
    public void start() {
        if (state == State.READY) {
            audioRecordedLength = 0;

            audioRecorder.startRecording();
            audioRecorder.read(buffer, 0, buffer.length);
            recordedData = new LinkedBlockingQueue<byte[]>();
            System.out.println("buffer size when start record: "
                    + buffer.length);
            state = State.RECORDING;
        } else {
            Log.e(VaniRecorder.class.getName(),
                    "start() called on illegal state");
            state = State.ERROR;
        }
    }

    /**
     *
     *
     * Stops the recording, and sets the state to STOPPED. In case of further
     * usage, a reset is needed. Also finalizes the wave file in case of
     * uncompressed recording.
     *
     */
    public void stop() {
        // Log.d("", "enter stop!state="+state);
		/*
		 * try { Thread.sleep(500);//wait more 0.5 second } catch
		 * (InterruptedException e) { e.printStackTrace(); }
		 */
        if (state == State.RECORDING) {
            audioRecorder.stop();
            // Log.d("", "just call stop!audioRecorder.stop");
            state = State.STOPPED;

        } else {
            Log.e(VaniRecorder.class.getName(),
                    "stop() called on illegal state");
            state = State.ERROR;

            audioRecorder.stop();
            // audioRecorder.release();
            // TODO return not recognized error
            // vani.
            // plugin.showNotRecognize();
        }
    }

    /*
     *
     * Converts a byte[2] to a short, in LITTLE_ENDIAN format
     */
    private short getShort(byte argB1, byte argB2) {
        return (short) (argB1 | (argB2 << 8));
    }

    public void uploadData() {
        // audioRecorder.read(buffer, 0, buffer.length); // Fill buffer

        try {
            uploader.onHasData(recordedData.take(), vani.isUseCompression());

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        updateAmplitude();
    }

    public void close() {
        uploader.close();
        try {
            audioRecorder.stop();
            // audioRecorder.release();
        } catch (Exception e) {
            System.out.println("Error when VaniRecorder.close() :"
                    + e.getMessage());
        }
    }

    /**
     * Initiate {@link AudioRecord} with sample rate = 16000Hz, audio data
     * format = PCM 16 bits
     */
    public synchronized static void initRecorder() {
        if (single == null) {
            single = new VaniRecorder(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SpeechConfiguration.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
            );
        }
    }

    public int getAudioRecordedLengthInMs() {
        return audioRecordedLength / (16 * 2);
    }
}