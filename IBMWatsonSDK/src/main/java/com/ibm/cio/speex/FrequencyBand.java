package com.ibm.cio.speex;

/**
* Created by IntelliJ IDEA.
* User: thoth
* Date: 9/12/12
* Time: 5:51 PM
* To change this template use File | Settings | File Templates.
*/
public enum FrequencyBand
{
    /**
     * 8 KHz sample rate
     */
    NARROW_BAND(0),
    /**
     * 16 KHz sample rate
     */
    WIDE_BAND(1),
    /**
     * 32 KHz sample rate
     */
    ULTRA_WIDE_BAND(2);

    public final int code;
    
    FrequencyBand(int code)
    {
        this.code = code;
    }
}
