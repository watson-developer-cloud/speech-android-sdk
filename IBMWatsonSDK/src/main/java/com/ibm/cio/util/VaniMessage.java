package com.ibm.cio.util;

public class VaniMessage {
	public static final String UN_DEFINED_ERROR_CODE = "99";
	public static final String IO_ERROR_CODE = "401";

	public static final VaniMessage INVALID_RECODER_STATE = new VaniMessage("0", "Error, invalid recorder state!");

	public static final VaniMessage NO_ITRANS_RESPONSE =new VaniMessage("3", "Error, No Response!");

	public static VaniMessage STOP_BEFORE_INIT = new VaniMessage("1", "Error, invalid recorder state!");
	
	private String code;
	private String message;

	public VaniMessage(String code, String message) {
		super();
		this.code = code;
		this.message = message;
	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
}
