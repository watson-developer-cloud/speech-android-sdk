package com.ibm.cio.speex;

/**
 * Copyright 2012, Robert Forsman
 * speex-ndk@thoth.purplefrog.com
 */
public class JNISpeexDecoder
{

    private final int slot;

    public JNISpeexDecoder(FrequencyBand band)
    {
        slot = allocate(band.code);
    }

    @Override
    protected void finalize()
        throws Throwable
    {
        deallocate(slot);
    }

    public short[] decode(byte[] frame)
    {
        return decode(slot, frame);
    }

    private native static short[] decode(int slot, byte[] frame);

    /**
     * allocates a slot in the JNI implementation for our native bits.  Store it in the {@link #slot} field.
     * @param wideband true for wideband, false for narrowband
     * @return an index into a slot array in the JNI implementation for our encoder parameters.
     */
    protected native static int allocate(int wideband);

    /**
     * @param slot the return value from a previous call to {@link #allocate(int)}
     */
    protected native static void deallocate(int slot);

    static {
        System.loadLibrary("speex");
    }

}
