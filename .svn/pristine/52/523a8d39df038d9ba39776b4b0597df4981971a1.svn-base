package com.ibm.cio.audio;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.xiph.speex.OggCrc;
import org.xiph.speex.SpeexDecoder;

import android.util.Log;

public class VaniSpeexDec {
	private static final String TAG = VaniSpeexDec.class.getName();
	
	/** Speex Decoder */
	  protected SpeexDecoder speexDecoder;

	  /** Defines whether or not the perceptual enhancement is used. */
	  protected boolean enhanced  = true;
	  /** If input is raw, defines the decoder mode (0=NB, 1=WB and 2-UWB). */
	  private int mode          = 0;
	  /** If input is raw, defines the number of frmaes per packet. */
	  private int nframes       = 1;
	  /** If input is raw, defines the sample rate of the audio. */
	  private int sampleRate    = 8000;
	  /** If input is raw, defines th number of channels (1=mono, 2=stereo). */
	  private int channels      = 1;
	  /** The percentage of packets to lose in the packet loss simulation. */
//	  private int loss          = 0;
	
    public VaniSpeexDec() {
    	
    }
    
	public byte[] decode(InputStream is)
			 {
		byte[] header = new byte[2048];
		byte[] payload = new byte[65536];
		byte[] decdat = new byte[44100 * 2 * 2];
		final int OGG_HEADERSIZE = 27;
		final int OGG_SEGOFFSET = 26;
		final String OGGID = "OggS";
		int segments = 0;
		int curseg = 0;
		int bodybytes = 0;
		int decsize = 0;
		int packetNo = 0;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] decodedAudioByte;
		speexDecoder = new SpeexDecoder();
		// open the input stream
		DataInputStream dis = new DataInputStream(is);

		int origchksum;
		int chksum;

		try {
			// read until we get to EOF
			while (true) {
				// read the OGG header
				dis.readFully(header, 0, OGG_HEADERSIZE);
				origchksum = readInt(header, 22);
				header[22] = 0;
				header[23] = 0;
				header[24] = 0;
				header[25] = 0;
				chksum = OggCrc.checksum(0, header, 0, OGG_HEADERSIZE);
				// make sure its a OGG header
				if (!OGGID.equals(new String(header, 0, 4))) {
					Log.i(TAG, "missing ogg id!");
					decodedAudioByte = baos.toByteArray();
					Log.i(TAG, "not SPX file: " + decodedAudioByte.length);
					return decodedAudioByte;
//					return null;
				}
				/* how many segments are there? */
				segments = header[OGG_SEGOFFSET] & 0xFF;
				dis.readFully(header, OGG_HEADERSIZE, segments);
				chksum = OggCrc.checksum(chksum, header, OGG_HEADERSIZE, segments);
				/* decode each segment, writing output to wav */
				for (curseg = 0; curseg < segments; curseg++) {
					/* get the number of bytes in the segment */
					bodybytes = header[OGG_HEADERSIZE + curseg] & 0xFF;
					if (bodybytes == 255) {
						System.err.println("Sorry, don't handle 255 sizes!");
						decodedAudioByte = baos.toByteArray();
						return decodedAudioByte;
//						return null;
					}
					dis.readFully(payload, 0, bodybytes);
					chksum = OggCrc.checksum(chksum, payload, 0, bodybytes);

					/* decode the segment */
					/* if first packet, read the Speex header */
					if (packetNo == 0) {
						if (readSpeexHeader(payload, 0, bodybytes)) {
							packetNo++;
						} else {
							packetNo = 0;
						}
					} else if (packetNo == 1) { // Ogg Comment packet
						packetNo++;
					} else {
						speexDecoder.processData(payload, 0, bodybytes);
						for (int i = 1; i < nframes; i++) {
							speexDecoder.processData(false);
						}
						/* get the amount of decoded data */
						if ((decsize = speexDecoder.getProcessedData(decdat, 0)) > 0) {
//							System.out.println("decsize: " + decsize);
							byte[] processedAudio = new byte[decsize];
							System.arraycopy(decdat, 0, processedAudio, 0, decsize);
							baos.write(processedAudio);
						}
						packetNo++;
					}
				}
				if (chksum != origchksum) {
					System.out.println("Ogg CheckSums do not match");
					return null;
				}
			}
		} catch (EOFException eof) {
//			System.out.println("End of file");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		decodedAudioByte = baos.toByteArray();
		
		return decodedAudioByte;
	}
    
    /**
     * Converts Little Endian (Windows) bytes to an int (Java uses Big Endian).
     * @param data the data to read.
     * @param offset the offset from which to start reading.
     * @return the integer value of the reassembled bytes.
     */
    protected static int readInt(final byte[] data, final int offset) {
    	return (data[offset] & 0xff) |
             ((data[offset+1] & 0xff) <<  8) |
             ((data[offset+2] & 0xff) << 16) |
             (data[offset+3] << 24); // no 0xff on the last one to keep the sign
    }
    
    /**
     * Reads the header packet.
     * <pre>
     *  0 -  7: speex_string: "Speex   "
     *  8 - 27: speex_version: "speex-1.0"
     * 28 - 31: speex_version_id: 1
     * 32 - 35: header_size: 80
     * 36 - 39: rate
     * 40 - 43: mode: 0=narrowband, 1=wb, 2=uwb
     * 44 - 47: mode_bitstream_version: 4
     * 48 - 51: nb_channels
     * 52 - 55: bitrate: -1
     * 56 - 59: frame_size: 160
     * 60 - 63: vbr
     * 64 - 67: frames_per_packet
     * 68 - 71: extra_headers: 0
     * 72 - 75: reserved1
     * 76 - 79: reserved2
     * </pre>
     * @param packet
     * @param offset
     * @param bytes
     * @return
     */
	private boolean readSpeexHeader(final byte[] packet, final int offset, final int bytes) {
		if (bytes != 80) {
			System.out.println("Oooops");
			return false;
		}
		if (!"Speex   ".equals(new String(packet, offset, 8))) {
			return false;
		}
		mode = packet[40 + offset] & 0xFF;
		sampleRate = readInt(packet, offset + 36);
		channels = readInt(packet, offset + 48);
		nframes = readInt(packet, offset + 64);
		/*System.out.println("readSpeexHeader mode: " + mode);
		System.out.println("readSpeexHeader Sample Rate: " + sampleRate);
		System.out.println("readSpeexHeader Channels: " + channels);*/
		return speexDecoder.init(mode, sampleRate, channels, enhanced);
	}
}
