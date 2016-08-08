/**
 * © Copyright IBM Corporation 2015
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

package com.ibm.watson.developer_cloud.android.speech_to_text.v1.audio;

import java.lang.Thread;
import android.util.Log;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author Daniel Bolanos dbolano@us.ibm.com
 * description: this thread captures audio from the phone's microphone, whenever the buffer
 *
 */
public class AudioCaptureRunnable implements Runnable {

    private static final String TAG = "AudioCaptureThread";
    private boolean isRecording = false;
    private boolean mStopped = false;
    private int mSamplingRate = -1;
    private IAudioConsumer mIAudioConsumer = null;

    // the thread receives high priority because it needs to do real time audio capture
    // THREAD_PRIORITY_URGENT_AUDIO = "Standard priority of the most important audio threads"
    public AudioCaptureRunnable(int iSamplingRate, IAudioConsumer IAudioConsumer) {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        this.mSamplingRate = iSamplingRate;
        this.mIAudioConsumer = IAudioConsumer;
    }

    // once the thread is started it runs nonstop until it is stopped from the outside
    @Override
    public void run() {
        AudioRecord recorder = null;

        try {
            int bufferSize = Math.max(this.mSamplingRate / 2, AudioRecord.getMinBufferSize(this.mSamplingRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT));
            byte[] buffer = new byte[bufferSize];

            recorder = new AudioRecord(AudioSource.MIC, mSamplingRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            recorder.startRecording();
            this.isRecording = true;

            Log.d(TAG, "Recording started");
            while(isRecording) {
                int byteCount = recorder.read(buffer, 0, buffer.length);
                if(byteCount > 0) {
                    long v = 0;
                    for (int i = 0; i < byteCount; i++) {
                        v += buffer[i] * buffer[i];
                    }
                    double amplitude = v / (double) byteCount;
                    double volume = 0;
                    if (amplitude > 0) volume = 10 * Math.log10(amplitude);

                    mIAudioConsumer.onAmplitude(amplitude, volume);

                    // convert to an array of bytes and send it to the server
                    byte[] bytes = ByteBuffer.allocate(byteCount).order(ByteOrder.LITTLE_ENDIAN).put(buffer, 0, byteCount).array();
                    mIAudioConsumer.consume(bytes);
                }
            }
        }
        catch(Throwable x) {
            Log.e(TAG, "Error reading voice audio", x);
        }
        // release resources
        finally {
            if (recorder == null) {
                // Thrown when a method is invoked with an argument which it can not reasonably deal with.
                this.mIAudioConsumer.onError(AudioRecord.STATE_UNINITIALIZED, "Audio recorder argument error.");
            }
            else {
                if(recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                    recorder.stop();
                    recorder.release();
                }
                else if(recorder.getState() == AudioRecord.STATE_UNINITIALIZED) {
                    this.mIAudioConsumer.onError(AudioRecord.STATE_UNINITIALIZED, "Audio recorder is not initialized.");
                }
            }
            mStopped = true;
            Log.d(TAG, "Recording stopped!");
        }
    }

    // this function is intended to be called from outside the thread in order to stop the thread
    public void end() {
        this.isRecording = false;
        // waiting loop, it waits until the thread actually finishes
        while(!mStopped) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
    }
}