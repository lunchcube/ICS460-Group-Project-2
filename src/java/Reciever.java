
import java.io.*;
import java.net.*;

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
    private long end_time;
    private int lastFrameRcv = -1;
    private int largeAccFrame = 0;

	public static void main(String[] args) {

		Reciever receiver = new Reciever();
		windowSize = Integer.parseInt(JOptionPane.showInputDialog("What is the window size?"));
		maxPackSize = Integer.parseInt(JOptionPane.showInputDialog("What is the packet size in bytes?"));
		receiver.filename = ("./received/" + JOptionPane.showInputDialog("What is the received file name?"));
		System.out.println("receiver filename: " + receiver.filename);
		receiver.receiveFile();
	}

	private void calculatePackets() {
		try {
			int mtu = socket.getSendBufferSize();
			int result = (int)Math.ceil(windowSize/(double)mtu);
			if (result==1) {
				this.maxFrames = result;
			}
			else {
				this.maxFrames = (int)Math.floor(windowSize/(double)mtu);
			}

			this.recPackArray = new int[maxFrames];
			this.recBytes = new byte[maxFrames][maxPackSize-headerSize];
			for (int i = 0; i < recPackArray.length; i++) {
				this.recPackArray[i]=0;
			}
			this.largeAccFrame = lastFrameRcv + maxFrames;
		}
		catch (SocketException e) {
			e.printStackTrace();
		}
	}

	private boolean sendACK(int seq_num) {
		byte[] ack = intToByteArray(seq_num);
		try {
			socket.send(new DatagramPacket(ack, ack.length, this.sender, sendingPort));
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
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
		int seq_num = byteArrayToInt(seq_num_bytes);
		int eop = byteArrayToInt(eop_bytes);
		int last_packet = byteArrayToInt(last_packet_bytes);

		if(seq_num==0 && !initConn){
			initConn = true;
			this.startTime = System.currentTimeMillis();
			System.out.println("Sender: "+packet.getAddress()+":"+packet.getPort());
		}

		if(last_packet!=1)
		{
			payload = new byte[(maxPackSize - headerSize)];
			System.arraycopy(packetData, headerSize, payload, 0, payload.length);
		}
		else {
			this.lastSeqNum = seq_num;
			payload = new byte[eop-headerSize];
			System.arraycopy(packetData, headerSize, payload, 0, eop-headerSize);
		}

		boolean acceptPacket = (seq_num<=largeAccFrame);
		if (acceptPacket) {
			acceptPacket(seq_num, eop, last_packet, payload);
		}
	}

	private void acceptPacket(int seq_num, int eop, int last_packet, byte[] payload) {

		if(lastFrameRcv >= seq_num){
			sendACK(seq_num);
		}
		else {
			recPackArray[seq_num-lastFrameRcv-1] = 1;
			recBytes[seq_num-lastFrameRcv-1] = payload;
			int adjustedWindow = 0;
			int i_val = 0;
			for (int i = 0; i < recPackArray.length; i++) {
				if(recPackArray[i]==1) {
					lastFrameRcv+=1;
					largeAccFrame+=1;
					this.fileSize+=payload.length;
					System.out.println("Message #" + seq_num + " received.");
					processPayload(lastFrameRcv, recBytes[i]);
					sendACK(lastFrameRcv);
					System.out.println("Sent acknowledgement for message #" + seq_num + "\n");
				}
				else {
					adjustedWindow = i;
					break;
				}
				i_val = i;
			}

			if(i_val==recPackArray.length-1) {
				adjustedWindow = recPackArray.length;
			}

			for (int i = 0; i < adjustedWindow; i++) {
				recPackArray[i]=0;
			}
		}
	}

	private void processPayload(int seq_num, byte[] payload) {
		try {
			this.fileOut.write(payload);
			this.fileOut.flush();

			if(seq_num==lastSeqNum){
				this.fileOut.close();
				this.end_time = System.currentTimeMillis();
				double runtime = ((this.end_time-this.startTime)/(double)1000);
				System.out.println("Successfully received " + this.filename + " (" + this.fileSize + " bytes) in "
				    + runtime + " seconds");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
		// ----------
		return (int)(
		(0xff & data[0]) << 24 |
		(0xff & data[1]) << 16 |
		(0xff & data[2]) << 8 |
		(0xff & data[3]) << 0
		);
	}
}
