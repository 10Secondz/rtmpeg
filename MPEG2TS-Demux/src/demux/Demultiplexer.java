package demux;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import utilities.TSutils;

/**
 * 
 * A de-multiplexer of MPEG2-TS file, the aim of this class is to provide frames
 * from an MPEG2-TS file that is in a H264 format.
 * 
 * @author Elias Khsheibun
 * @author Rashed Rashed
 * 
 */
public class Demultiplexer {

	private final static int packetSize = 188;
	private final static int pidH264 = 68;
	private static int packetNum = 0;
	private int packetBufferNum = 0;
	private final static int bufferSize = 10;
	private byte[][] buffer = new byte[bufferSize][packetSize];
	private int bufferPointer;
	private InputStream is;
	private long noOfPacketsInFile;
	private long frameNo;
	private long offset;
	private long timeStamp;

	private void fillBuffer() throws IOException {
		for (int i = 0; i < buffer.length && packetNum + 1 < noOfPacketsInFile; i++) {
			buffer[i] = getNextTSPacket();
			packetBufferNum++;
		}

		bufferPointer = 0;
	}

	public Demultiplexer(File file) throws IOException {
		is = new FileInputStream(file);
		noOfPacketsInFile = (file.length() / packetSize);
		fillBuffer();
		bufferPointer = 0;
		frameNo = 0;
		offset = 0;
		timeStamp = 0;
	}

	private byte[] getNextTSPacket() throws IOException {

		byte b[] = new byte[packetSize];
		if (is.read(b, 0, packetSize) == -1) {
			System.err.println("End of stream has been reached");
		}

		return b;
	}

	public Frame getNextFrame() throws IOException {

		List<byte[]> arrayList = new ArrayList<byte[]>();

		if (packetNum >= noOfPacketsInFile)
			return null;

		// The first loop for finding the start of a PES
		for (; packetNum < noOfPacketsInFile; packetNum++, bufferPointer++) {

			if (bufferPointer == buffer.length)
				fillBuffer();

			if (TSutils.getPID(buffer[bufferPointer]) != pidH264) {
				continue;
			}

			if (TSutils.isStartOfPES(buffer[bufferPointer])) {
				byte[] payload = getPayload(buffer[bufferPointer]);
				arrayList.add(payload);
				packetNum++;
				bufferPointer++;
				break;
			}
		}

		// Adding packet payloads until next PES is found
		for (; packetNum < noOfPacketsInFile; packetNum++, bufferPointer++) {
			if (bufferPointer == buffer.length)
				fillBuffer();

			if (TSutils.getPID(buffer[bufferPointer]) != pidH264) {
				continue;
			}

			if (!TSutils.isStartOfPES(buffer[bufferPointer])) {
				byte[] payload = getPayload(buffer[bufferPointer]);
				arrayList.add(payload);
			} else {
				break;
			}
		}

		if (packetNum > noOfPacketsInFile)
			return null;

		int totalSize = 0;
		for (byte[] b : arrayList) {
			totalSize += b.length;
		}

		byte[] frame = new byte[totalSize];
		int j = 0;
		for (byte[] b : arrayList) {
			for (int i = 0; i < b.length; i++)
				frame[j] = b[i];
			j += b.length;
		}
		Frame f = new Frame(frame, offset, frame.length, timeStamp);
		offset += frame.length;
		timeStamp++;
		frameNo++;
		return f;
	}

	private byte[] getPayload(byte[] bs) {
		// TODO Auto-generated method stub
		return new byte[100];
	}

	public static void main(String[] args) throws IOException {
		File file = new File("video.mpg");
		Demultiplexer demux = new Demultiplexer(file);
		int counter2 = 0;
		while (demux.getNextFrame() != null)
			counter2++;
		System.out.println("No of PES packets produced "+counter2);

		int counter = 0;
		int pesCounter = 0;
		System.out.println("The length of the file in bytes: "+file.length());
		demux = new Demultiplexer(file);
		byte [] b=null;
		for (int i = 0; i < file.length() / packetSize; i++) {
			b = demux.getNextTSPacket();
			if (TSutils.isStartOfPES(b) && TSutils.getPID(b) == pidH264)
				pesCounter++;
			if (TSutils.getPID(b) == pidH264)
				counter++;
		}
		System.out.println("No of PES packets: " + pesCounter);
		System.out.println("No of frames read: " + packetNum);
		System.out.println("No of frames with PID = " + pidH264 + " is: "
				+ counter);
	}

}
