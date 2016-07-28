

import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;


public class Sender {

	private int headerSize = 12;
	DatagramSocket socket;
    private int receiverPort = 1025;
    private int senderPort = 1024;
    private InetSocketAddress receiver = new InetSocketAddress("localhost", receiverPort);
    private long windowSize;
    private long timeout;
    private int maxPacketSize;
	private int maxNumFrames;
    private int lastSeqNum;
    private String filename;
    private byte[] data;
    private boolean initial_connection = false;
    private long file_size=0;
    private long start_time;
    private long end_time;
    private int last_ack_received = -1;
    private int last_frame_sent = -1;
    private TimeoutSender[] sender_window;
    private int packetDropChance = 0;

	public Sender() {
		super();
	}

	public static void main(String[] args) {
		Sender sender = new Sender();
		sender.windowSize = Integer.parseInt(JOptionPane.showInputDialog("What is the sender window size?"));
		sender.maxPacketSize = Integer.parseInt(JOptionPane.showInputDialog("What is the packet size in bytes?"));
		sender.timeout = Integer.parseInt(JOptionPane.showInputDialog("What is the timeout in MS?"));
		sender.packetDropChance = Integer.parseInt(JOptionPane.showInputDialog("What is the chance of packet drop?"));

        sender.setFilename(JOptionPane.showInputDialog("What is the filename to be sent?"));
		sender.sendFile();
	}

	private void calculatePackets() {
		try {

			int mtu = socket.getSendBufferSize();
			int result = (int)Math.ceil(windowSize/(double)mtu);
			if (result==1) {
				this.maxNumFrames = result;
			}
			else {
				this.maxNumFrames = (int)Math.floor(windowSize/(double)mtu);
			}
			this.sender_window = new TimeoutSender[maxNumFrames];
			for (int i = 0; i < sender_window.length; i++) {
				this.sender_window[i]=null;
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	public void transmit(int seqNum) {

		byte[] dataPart = getData(seqNum);
		int eop = headerSize + dataPart.length;
		int last_packet = 0;
		if(seqNum==lastSeqNum) {
			last_packet = 1;
		}

		byte[] packetToSend = new byte[maxPacketSize];
		byte[] seq_num_bytes = intToBytes(seqNum);
		byte[] headerBytes = intToBytes(eop);
		byte[] lastPacket = intToBytes(last_packet);

		System.arraycopy(seq_num_bytes, 0, packetToSend, 0, seq_num_bytes.length);
		System.arraycopy(headerBytes, 0, packetToSend, seq_num_bytes.length, headerBytes.length);
		System.arraycopy(lastPacket, 0, packetToSend, seq_num_bytes.length+headerBytes.length, lastPacket.length);
		System.arraycopy(dataPart, 0, packetToSend, headerSize, dataPart.length);

		DatagramPacket datagram = new DatagramPacket(packetToSend, packetToSend.length, receiver.getAddress(), receiver.getPort());

		try
		{
			if(!probability(packetDropChance))
			{
				socket.send(datagram);
				System.out.println("Sent message #" +seqNum + ". Length= "+dataPart.length+" bytes");
			}
			else
			{
				System.out.println("Dropped message #" +seqNum + ". Length= " + dataPart.length + " bytes");
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

	}

	private byte[] getData(int seq_num){
		int room_for_data = (maxPacketSize - headerSize);
		byte[] subData;
		int start = ((seq_num)*room_for_data);
		int end = start + (room_for_data - 1);
		int dataLength = this.data.length;

		if(end<=dataLength){
			subData = new byte[room_for_data];
			System.arraycopy(this.data, start, subData, 0, room_for_data);
		}
		else{

			subData = new byte[dataLength-start];
			System.arraycopy(this.data, start, subData, 0, dataLength-start);
		}

		return subData;

	}

	private void prepareFile() throws Exception{


		File filepath = new File(this.filename);
        InputStream is = new FileInputStream(filepath);

        long length = filepath.length();
        this.file_size = length;
        this.data = new byte[(int)length];

		this.lastSeqNum = (int)Math.ceil(data.length/(double)maxPacketSize) - 1;

        int offset = 0;
        int numRead = 0;
        while (offset < this.data.length && (numRead=is.read(this.data, offset, this.data.length-offset)) >= 0)
        {
        	offset += numRead;
        }
        if (offset < this.data.length)
        {
            throw new Exception("Could not completely read file "+filepath.getName());
        }

        is.close();
	}

	public void processACK(DatagramPacket packet) {
		int seq_num = bytesToInt(packet.getData());

		if(seq_num==0 && !initial_connection)
		{
			initial_connection = true;
			System.out.println("Listening on: "+packet.getAddress()+":"+packet.getPort());
		}

		System.out.println("Recieved  acknowledgment for message #"+seq_num);

		if(seq_num>last_ack_received)
		{
			this.last_ack_received = seq_num;

			updateTimeoutThreads(seq_num);
		}
	}

	private void updateTimeoutThreads(int seq_num)
	{
		for (int i = 0; i < sender_window.length; i++)
		{
			if(sender_window[i]!=null){

				TimeoutSender timeout_thread = sender_window[i];

				if(timeout_thread.seq_num<=seq_num)
				{
					timeout_thread.finished=true;

					last_frame_sent+=1;
					if(last_frame_sent<=lastSeqNum)
					{
						transmit(last_frame_sent);
						sender_window[i] = new TimeoutSender(last_frame_sent, (int)timeout, this);
					}//end if
					else{
						this.end_time = System.currentTimeMillis();
						double runtime = ((this.end_time-this.start_time)/(double)1000);
						System.out.println("Successfully transferred "+this.filename+" ("+this.file_size+" bytes) in "+runtime+" seconds");
					}
				}
			}
		}
	}

	public boolean probability(int percent)
	{
	    Random r=new Random();
	    return r.nextInt(100)<percent;
	}

	public boolean sendFile()
	{
		try
		{
			this.start_time = System.currentTimeMillis();
			System.out.println("Sender "+InetAddress.getLocalHost().getHostAddress()+" listening on UDP port "+this.senderPort);
            this.socket = new DatagramSocket(senderPort);
            new PacketSender(socket, this);
            calculatePackets();
			prepareFile();

            for (int i = 0; i < sender_window.length; i++)
            {
            	last_frame_sent+=1;

				if(last_frame_sent<=lastSeqNum)
				{
					transmit(last_frame_sent);
					sender_window[i] = new TimeoutSender(last_frame_sent, (int)timeout,this);
				}
			}
            return true;
        }
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public String getFilename()
	{
		return filename;
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


	public void setReceiver(InetSocketAddress receiver)
	{
		this.receiver = receiver;
	}

	public static byte[] intToBytes(int data)
	{
		return new byte[] {
		(byte)((data >> 24) & 0xff),
		(byte)((data >> 16) & 0xff),
		(byte)((data >> 8) & 0xff),
		(byte)((data >> 0) & 0xff),
		};
	}

	public static int bytesToInt(byte[] data) {
		if (data == null || data.length != 4) return 0x0;
		// ----------
		return (int)( // NOTE: type cast not necessary for int
		(0xff & data[0]) << 24 |
		(0xff & data[1]) << 16 |
		(0xff & data[2]) << 8 |
		(0xff & data[3]) << 0
		);
	}

    public int getMax_packet_size()
    {
		return maxPacketSize;
	}

	public int getProbabilityOfDrop() {
		return packetDropChance;
	}


	public void setProbabilityOfDrop(int probabilityOfDrop) {
		this.packetDropChance = probabilityOfDrop;
	}

}