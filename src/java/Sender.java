

import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;


public class Sender {

	private int headSize = 12;
	DatagramSocket socket;
    private int receivePort = 1025;
    private int sendPort = 1024;
    private InetSocketAddress receiver = new InetSocketAddress("localhost", receivePort);
    private long windowSize;
    private long timeout;
    private int maxPackSize;
	private int maxNumFrames;
    private int lastSeqNum;
    public static String filename;
    private byte[] data;
    private boolean initialConnection = false;
    private long fileSize=0;
    private long startTime;
    private long endTime;
    private int lastAckRec = -1;
    private int lastFrameSent = -1;
    private Timeout[] sendWindow;
    private int packDrop = 0;

	public Sender() {
		super();
	}

	public static void main(String[] args) {
		Sender sender = new Sender();
		sender.windowSize = Integer.parseInt(JOptionPane.showInputDialog("What is the sender window size?"));
		sender.maxPackSize = Integer.parseInt(JOptionPane.showInputDialog("What is the packet size in bytes?"));
		sender.timeout = Integer.parseInt(JOptionPane.showInputDialog("What is the timeout in MS?"));
		sender.packDrop = Integer.parseInt(JOptionPane.showInputDialog("What is the chance of packet drop?"));
        sender.setFilename(JOptionPane.showInputDialog("What is the filename to be sent?"));
		sender.sendFile();
	}

	private void calculatePackets() {
		try {
			int mtu = socket.getSendBufferSize();
			int result = (int)Math.ceil(windowSize / (double)mtu);
			if (result == 1) {
				this.maxNumFrames = result;
			}
			else {
				this.maxNumFrames = (int)Math.floor(windowSize / (double)mtu);
			}
			this.sendWindow = new Timeout[maxNumFrames];
			for (int i = 0; i < sendWindow.length; i++) {
				this.sendWindow[i] = null;
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	public void transmit(int seqNum) {
		byte[] dataPart = getData(seqNum);
		int eop = headSize + dataPart.length;
		int last_packet = 0;
		if(seqNum == lastSeqNum) {
			last_packet = 1;
		}

		byte[] packetToSend = new byte[maxPackSize];
		byte[] seq_num_bytes = intToByteArray(seqNum);
		byte[] headerBytes = intToByteArray(eop);
		byte[] lastPacket = intToByteArray(last_packet);

		//Get the sequence number
		System.arraycopy(seq_num_bytes, 0, packetToSend, 0, seq_num_bytes.length);
		System.arraycopy(headerBytes, 0, packetToSend, seq_num_bytes.length, headerBytes.length);
		System.arraycopy(lastPacket, 0, packetToSend, seq_num_bytes.length+headerBytes.length, lastPacket.length);
		System.arraycopy(dataPart, 0, packetToSend, headSize, dataPart.length);

		DatagramPacket datagram = new DatagramPacket(packetToSend, packetToSend.length, receiver.getAddress(), receiver.getPort());

		try {
			if(!probability(packDrop)) {
				socket.send(datagram);
				System.out.println("Sent message #" + seqNum + ".");
			}
			else {
				System.out.println("Dropped message #" + seqNum + ".");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private byte[] getData(int seqNum){
		int roomForData = (maxPackSize - headSize);
		byte[] subData;
		int start = ((seqNum) * roomForData);
		int end = start + (roomForData - 1);
		int dataLength = this.data.length;

		if(end <= dataLength) {
			subData = new byte[roomForData];
			System.arraycopy(this.data, start, subData, 0, roomForData);
		}
		else {
			subData = new byte[dataLength - start];
			System.arraycopy(this.data, start, subData, 0, dataLength - start);
		}
		return subData;
	}

	private void prepareFile() throws Exception {

		File filepath = new File(this.filename);
        InputStream is = new FileInputStream(filepath);

        long length = filepath.length();
        this.fileSize = length;
        this.data = new byte[(int)length];

		this.lastSeqNum = (int)Math.ceil(data.length / (double)maxPackSize) - 1;

        int offset = 0;
        int numRead = 0;
        while (offset < this.data.length && (numRead = is.read(this.data, offset, this.data.length-offset)) >= 0) {
        	offset += numRead;
        }
        if (offset < this.data.length) {
            throw new Exception("Could not completely read file " + filepath.getName());
        }
        is.close();
	}

	public void processACK(DatagramPacket packet) {
		int seqNum = byteArrayToInt(packet.getData());

		if(seqNum == 0 && !initialConnection) {
			initialConnection = true;
			System.out.println("Listening on: " + packet.getAddress() + ":" + packet.getPort());
		}
		System.out.println("Recieved  acknowledgment for message #" + seqNum + "\n");

		if(seqNum > lastAckRec) {
			this.lastAckRec = seqNum;
			updateTimeoutThreads(seqNum);
		}
	}

	private void updateTimeoutThreads(int seqNum) {
		for (int i = 0; i < sendWindow.length; i++)
		{
			if(sendWindow[i] != null){
				Timeout timeoutThread = sendWindow[i];

				if(timeoutThread.seqNum <= seqNum) {
					timeoutThread.finished=true;
					lastFrameSent += 1;
					if(lastFrameSent <= lastSeqNum) {
						transmit(lastFrameSent);
						sendWindow[i] = new Timeout(lastFrameSent, (int)timeout, this);
					}
					else {
						this.endTime = System.currentTimeMillis();
						double runtime = ((this.endTime-this.startTime) / (double)1000);
						System.out.println("Successfully transferred " + this.filename + " (" + this.fileSize
						    + " bytes) in " + runtime + " seconds");
					}
				}
			}
		}
	}

	public boolean probability(int percent) {
	    Random r = new Random();
	    return r.nextInt(100) < percent;
	}

	public boolean sendFile() {
		try {
			this.startTime = System.currentTimeMillis();
			System.out.println("Sender " + InetAddress.getLocalHost().getHostAddress() + " listening on UDP port "
			    + this.sendPort);
            this.socket = new DatagramSocket(sendPort);
            new PacketSender(socket, this);
            calculatePackets();
			prepareFile();

            for (int i = 0; i < sendWindow.length; i++) {
            	lastFrameSent += 1;

				if(lastFrameSent<=lastSeqNum) {
					transmit(lastFrameSent);
					sendWindow[i] = new Timeout(lastFrameSent, (int)timeout,this);
				}
			}
            return true;
        } catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public InetSocketAddress getReceiver() {
		return receiver;
	}

	public long getTimeout() {
		return timeout;
	}

	public void setFilename(String fname) {
		this.filename = fname;
	}


	public void setReceiver(InetSocketAddress receiver) {
		this.receiver = receiver;
	}

	public static byte[] intToByteArray(int data) {
		return new byte[] {
		(byte)((data >> 24) & 0xff),
		(byte)((data >> 16) & 0xff),
		(byte)((data >> 8) & 0xff),
		(byte)((data >> 0) & 0xff),
		};
	}

	public static int byteArrayToInt(byte[] data) {
		if (data == null || data.length != 4) return 0x0;
		return (int)(
		(0xff & data[0]) << 24 |
		(0xff & data[1]) << 16 |
		(0xff & data[2]) << 8 |
		(0xff & data[3]) << 0
		);
	}

	public int getProbabilityOfDrop() {
		return packDrop;
	}


	public void setProbabilityOfDrop(int probabilityOfDrop) {
		this.packDrop = probabilityOfDrop;
	}

}