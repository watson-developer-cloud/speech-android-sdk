package com.ibm.cio.watsonsdk;
import com.ibm.cio.dto.QueryResult;

public interface SpeechDelegate {
	public final static int MESSAGE = 0;
	public final static int ERROR = -1;
	public final static int CLOSE = -2;
	public final static int OPEN = 1;
    public final static int WAIT = 2;

    /**
     * Receive message with status code
     * @param code
     * @param result
     */
	public void onMessage(int code, QueryResult result);

    /**
     * Recieve the data of amplitude and volume
     * @param amplitude
     * @param volume
     */
    public void onAmplitude(double amplitude, double volume);
}
