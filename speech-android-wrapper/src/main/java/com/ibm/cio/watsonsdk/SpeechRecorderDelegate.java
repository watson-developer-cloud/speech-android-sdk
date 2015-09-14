package com.ibm.cio.watsonsdk;

public interface SpeechRecorderDelegate {
	public void onRecording(byte[] rawAudioData);
}
