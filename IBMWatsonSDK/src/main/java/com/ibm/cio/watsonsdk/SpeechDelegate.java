package com.ibm.cio.watsonsdk;
import com.ibm.cio.dto.QueryResult;

public interface SpeechDelegate {
//	public static int STATUS_CODE_AUTHENTICATION_REQUIRED = 401;
//
//	public void transcriptionFinishedCallback(String result);
//	public void transcriptionErrorCallback(JSONObject error);

	public final static int MESSAGE = 0;
	public final static int ERROR = -1;
	public final static int CLOSE = -2;
	public final static int OPEN = 1;
    public final static int WAIT = 2;

	public void receivedMessage(int code, QueryResult result);
}
