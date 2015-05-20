package com.ibm.cio.watsonsdk;

public interface SpeechRecorderDelegate {
	public void onRecordingCompleted(byte[] rawAudioData);
}
