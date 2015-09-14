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

import java.io.OutputStream;

import org.xiph.speex.OggSpeexWriter;

/**
 * Ogg Speex Writer
 * @author chienlk
 *
 */
public class ChunkOggSpeexWriter extends OggSpeexWriter {
	SpeexParam pam;
	/**
	 * Builds an Ogg Speex Writer. 
	 * @param pam encoder mode (0=NB, 1=WB, 2=UWB), sampleRate, channels (1=mono, 2=stereo,...), 
	 * number of frames per speex packet, vbr (Variable Bit Rate)
	 * @param out the OutputStream write to
	 */
	public ChunkOggSpeexWriter(SpeexParam pam, OutputStream out) {
		super(pam.mode, pam.sampleRate, pam.channels, pam.nframes, pam.vbr);
		this.pam = pam;
		this.setSerialNumber(pam.streamSerialNumber);		
		this.out = out;
		this.size = 0; 
		
	}
}
