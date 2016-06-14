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

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author mihui mihui@cn.ibm.com
 * description: this thread captures data from file stored on your device
 *
 */
public class FileCaptureThread implements Runnable {

    private static final String TAG = "FileCaptureThread";
    private IAudioConsumer mIAudioConsumer = null;
    private File mFile = null;

    public FileCaptureThread(IAudioConsumer IAudioConsumer, File file) {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        mIAudioConsumer = IAudioConsumer;
        mFile = file;
    }

    @Override
    public void run() {
        int length = (int) this.mFile.length();
        byte[] buffer = new byte[length];
        FileInputStream in = null;
        try {
            in = new FileInputStream(this.mFile);
            int r;
            if ((r = in.read(buffer, 0, buffer.length)) != -1) {
                long v = 0;
                for (int i = 0; i < r; i++) {
                    v += buffer[i] * buffer[i];
                }
                double amplitude = v / (double) r;
                double volume = 0;
                if (amplitude > 0)
                    volume = 10 * Math.log10(amplitude);
                mIAudioConsumer.onAmplitude(amplitude, volume);

                // convert to an array of bytes and send it to the server
                ByteBuffer bufferBytes = ByteBuffer.allocate(r * 2);
                bufferBytes.order(ByteOrder.LITTLE_ENDIAN);
                bufferBytes.put(buffer, 0, r);
                byte[] bytes = bufferBytes.array();
                in.close();
                mIAudioConsumer.consume(bytes);
                mIAudioConsumer.end();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}