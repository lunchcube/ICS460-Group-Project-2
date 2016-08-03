
import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;


public class Reciever {

    private int headerSize = 12;
    public  DatagramSocket socket;
	private InetAddress sender;
    private int port = 1025;
    private int sendingPort;
    private static long windowSize;
    public static int maxPackSize;

	private int maxFrames;
    private int lastSeqNum = -1;
    private int[] recPackArray;
    private byte[][] recBytes;
    private String filename = null;
    private FileOutputStream fileOut;
    private boolean initConn = false;
    private long fileSize=0;
    private long startTime;
    private long endTime;
    private int lastFrameRcv = -1;
    private int largeAccFrame = 0;
    private static int packDrop;

	public static void main(String[] args) {

		Reciever receiver = new Reciever();
		windowSize = Integer.parseInt(JOptionPane.showInputDialog("What is the window size?"));
		maxPackSize = Integer.parseInt(JOptionPane.showInputDialog("What is the packet size in bytes?"));
		packDrop = Integer.parseInt(JOptionPane.showInputDialog("What is the chance of ACK drop?"));
		receiver.filename = ("./received/" + JOptionPane.showInputDialog("What is the received file name?"));
		System.out.println("receiver filename: " + receiver.filename);
		receiver.receiveFile();
	}

	private void calculatePackets() {
		try {
			int mtu = socket.getSendBufferSize();
			int result = (int)Math.ceil(windowSize / (double)mtu);
			if (result == 1) {
				this.maxFrames = result;
			}
			else {
				this.maxFrames = (int)Math.floor(windowSize / (double)mtu);
			}

			this.recPackArray = new int[maxFrames];
			this.recBytes = new byte[maxFrames][maxPackSize - headerSize];
			for (int i = 0; i < recPackArray.length; i++) {
				this.recPackArray[i] = 0;
			}
			this.largeAccFrame = lastFrameRcv + maxFrames;
		}
		catch (SocketException e) {
			e.printStackTrace();
		}
	}

	private boolean sendACK(int seqNum) {
		byte[] ack = intToByteArray(seqNum);

		try {
            if(!probability(packDrop)) {
                socket.send(new DatagramPacket(ack, ack.length, this.sender, sendingPort));
                return true;
            }
            else {
                System.out.println("Dropped ack for message #" + seqNum + ".\n");
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
//		try {
//			socket.send(new DatagramPacket(ack, ack.length, this.sender, sendingPort));
//			return true;
//		} catch (IOException e) {
//			e.printStackTrace();
//			return false;
//		}
	}

	public boolean probability(int percent) {
        Random r = new Random();
        return r.nextInt(100) < percent;
    }

	public void processPacket(DatagramPacket packet) {
		this.sender = packet.getAddress();
		this.sendingPort = packet.getPort();

		byte[] packetData = packet.getData();

		byte[] seq_num_bytes = new byte[4];
		byte[] eop_bytes = new byte[4];
		byte[] last_packet_bytes = new byte[4];
		byte[] payload;


		System.arraycopy(packetData, 0, seq_num_bytes, 0, seq_num_bytes.length);
		System.arraycopy(packetData, seq_num_bytes.length, eop_bytes, 0, eop_bytes.length);
		System.arraycopy(packetData, seq_num_bytes.length+eop_bytes.length, last_packet_bytes, 0, last_packet_bytes.length);
		int seqNum = byteArrayToInt(seq_num_bytes);
		int eop = byteArrayToInt(eop_bytes);
		int lastPacket = byteArrayToInt(last_packet_bytes);

		if(seqNum == 0 && !initConn){
			initConn = true;
			this.startTime = System.currentTimeMillis();
			System.out.println("Sender: " + packet.getAddress() + ":" + packet.getPort());
		}

		if(lastPacket != 1) {
			payload = new byte[(maxPackSize - headerSize)];
			System.arraycopy(packetData, headerSize, payload, 0, payload.length);
		}
		else {
			this.lastSeqNum = seqNum;
			payload = new byte[eop - headerSize];
			System.arraycopy(packetData, headerSize, payload, 0, eop-headerSize);
		}

		boolean acceptPacket = (seqNum <= largeAccFrame);
		if (acceptPacket) {
			acceptPacket(seqNum, eop, lastPacket, payload);
		}
	}

	private void acceptPacket(int seqNum, int eop, int last_packet, byte[] payload) {

		if(lastFrameRcv >= seqNum){
			sendACK(seqNum);
		}
		else {
			recPackArray[seqNum-lastFrameRcv-1] = 1;
			recBytes[seqNum-lastFrameRcv-1] = payload;
			int adjustedWindow = 0;
			int i_val = 0;
			for (int i = 0; i < recPackArray.length; i++) {
				if(recPackArray[i] == 1) {
					lastFrameRcv += 1;
					largeAccFrame += 1;
					this.fileSize += payload.length;
					System.out.println("Message #" + seqNum + " received.");
					System.out.println("Current window: " + seqNum + "-" + (seqNum + windowSize));
					processPayload(lastFrameRcv, recBytes[i]);
					sendACK(lastFrameRcv);
					System.out.println("Sent acknowledgement for message #" + seqNum + "\n");
				}
				else {
					adjustedWindow = i;
					break;
				}
				i_val = i;
			}

			if(i_val == recPackArray.length-1) {
				adjustedWindow = recPackArray.length;
			}

			for (int i = 0; i < adjustedWindow; i++) {
				recPackArray[i] = 0;
			}
		}
	}

	private void processPayload(int seqNum, byte[] payload) {
		try {
			this.fileOut.write(payload);
			this.fileOut.flush();

			if(seqNum == lastSeqNum){
				this.fileOut.close();
				this.endTime = System.currentTimeMillis();
				double runtime = ((this.endTime-this.startTime) / (double)1000);
				System.out.println("Successfully received " + this.filename + " (" + this.fileSize + " bytes) in "
				    + runtime + " seconds");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void receiveFile() {
		try {
			System.out.println("Receiver listening on UDP port " + this.port);
            this.socket = new DatagramSocket(port);
            calculatePackets();
            this.fileOut = new FileOutputStream(new File(this.filename));
            new PacketReceiver(this);
        } catch(Exception e) {
        	e.printStackTrace();
        }
	}

	public String getFilename() {
		return filename;
	}

	public int getLocalPort() {
		return port;
	}

	public void setFilename(String fname) {
		this.filename = fname;
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
}
