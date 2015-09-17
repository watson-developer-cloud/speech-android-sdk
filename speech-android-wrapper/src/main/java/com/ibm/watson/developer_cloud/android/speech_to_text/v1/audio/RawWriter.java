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

package com.ibm.watson.developer_cloud.android.speech_to_text.v1.audio;

import java.io.File;
import java.io.IOException;


/**
 * @author Viney
 *
 */
public class RawWriter extends AudioFileWriter{
    // Use PROPRIETARY notice if class contains a main() method, otherwise use COPYRIGHT notice.
    public static final String COPYRIGHT_NOTICE = "(c) Copyright IBM Corp. 2015";
    private IChunkUploader uploader;

    /**
     * Construct with WebSocketClient
     */
    public RawWriter(IChunkUploader uploader) {
        this.uploader = uploader;
    }

    @Override
    public void close() throws IOException {
        this.uploader.stop();
    }

    @Override
    public void open(File file) throws IOException {}


    @Override
    public void open(String filename) throws IOException {}


    @Override
    public void writeHeader(String comment) throws IOException {}

    @Override
    public void writePacket(byte[] data, int offset, int len) throws IOException {
        this.uploader.upload(data);
    }
}
