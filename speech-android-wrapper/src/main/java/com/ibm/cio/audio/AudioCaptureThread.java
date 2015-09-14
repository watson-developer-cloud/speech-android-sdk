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

package com.ibm.cio.audio;

import java.lang.Thread;
import android.util.Log;
import android.media.AudioRecord;
import android.media.*;
import android.media.MediaRecorder.AudioSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author Daniel Bolanos dbolano@us.ibm.com
 * description: this thread captures audio from the phone's microphone, whenever the buffer
 *
 */
public class AudioCaptureThread extends Thread {
    // Use PROPRIETARY notice if class contains a main() method, otherwise use COPYRIGHT notice.
    public static final String COPYRIGHT_NOTICE = "(c) Copyright IBM Corp. 2015";
    private static final String TAG = "AudioCaptureThread";
    private boolean mStop = false;
    private boolean mStopped = false;
    private int mSamplingRate = -1;
    private IAudioConsumer mIAudioConsumer = null;

    // the thread receives high priority because it needs to do real time audio capture
    // THREAD_PRIORITY_URGENT_AUDIO = "Standard priority of the most important audio threads"
    public AudioCaptureThread(int iSamplingRate, IAudioConsumer IAudioConsumer) {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        mSamplingRate = iSamplingRate;
        mIAudioConsumer = IAudioConsumer;
    }

    // once the thread is started it runs nonstop until it is stopped from the outside
    @Override
    public void run() {
        Log.i(TAG, "thread started");
        AudioRecord recorder = null;

        try {
            // initialize the recorder
            int iN = AudioRecord.getMinBufferSize(mSamplingRate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
            short[] buffer = new short[iN]; // ASR latency depends on the length of this buffer, a short buffer is good for latency
            // because the ASR will process the speech sooner, however it will introduce some network overhead because each packet comes
            // with a fixed amount of protocol-data), also I have noticed that some servers cannot handle too many small packages

            recorder = new AudioRecord(AudioSource.MIC, mSamplingRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, iN);
            recorder.startRecording();

            while(!mStop) {
                int r = recorder.read(buffer,0,buffer.length);
                long v = 0;
                for (int i = 0; i < r; i++) {
                    v += buffer[i] * buffer[i];
                }
                double amplitude = v / (double) r;
                double volume = 0;
                if(amplitude > 0)
                    volume = 10 * Math.log10(amplitude);
                mIAudioConsumer.onAmplitude(amplitude, volume);

                // convert to an array of bytes and send it to the server
                ByteBuffer bufferBytes = ByteBuffer.allocate(r*2);
                bufferBytes.order(ByteOrder.LITTLE_ENDIAN);
                bufferBytes.asShortBuffer().put(buffer,0,r);
                byte[] bytes = bufferBytes.array();
                int length = bytes.length;
                //Log.i(TAG, "Writing new data to buffer: " + length + " bytes (samplingRate: " + mSamplingRate + ")");
                mIAudioConsumer.consume(bytes);
            }
        }
        catch(Throwable x) {
            Log.w(TAG, "Error reading voice audio", x);
        }
        // release resources
        finally {
            recorder.stop();
            recorder.release();
            mStopped = true;
        }
    }

    // this function is intended to be called from outside the thread in order to stop the thread
    public void end() {

        mStop = true;
        // waiting loop, it waits until the thread actually finishes
        while(mStopped == false) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        Log.i(TAG, "audio thread ended");
    }
}