package com.ibm.cio.watsonsdk;
import com.ibm.cio.dto.QueryResult;

public interface SpeechDelegate {
	public final static int MESSAGE = 0;
	public final static int ERROR = -1;
	public final static int CLOSE = -2;
	public final static int OPEN = 1;
    public final static int WAIT = 2;

	public void receivedMessage(int code, QueryResult result);
}
