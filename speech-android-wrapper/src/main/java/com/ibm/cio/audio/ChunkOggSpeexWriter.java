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
