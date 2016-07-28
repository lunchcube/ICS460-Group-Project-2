

import java.io.*;
import java.net.*;

public class PacketSender implements Runnable {
	Thread thisThread;
	DatagramSocket socket;
	Sender sender;

	PacketSender(DatagramSocket socket, Sender instance){
		thisThread = new Thread(this);
		thisThread.start();
		this.socket = socket;
		this.sender = instance;
	}

	@Override
	public void run() {
		while(true)
		{
			try
			{
				byte [] ackIn = new byte[4];
				DatagramPacket packet = new DatagramPacket(ackIn,ackIn.length);
				socket.receive(packet);
				sender.processACK(packet);
				thisThread.sleep(5000);  //Change this to for message delay on screen to make it readable
			}
			catch (IOException e)
			{
				e.printStackTrace();
			} catch ( InterruptedException x ) {
                x.printStackTrace();
            }
		}
	}
}
