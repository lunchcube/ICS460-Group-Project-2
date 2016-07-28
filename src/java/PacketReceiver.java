

import java.io.*;
import java.net.*;

public class PacketReceiver implements Runnable {

	private Reciever reciever;
	Thread thread;

	public PacketReceiver(Reciever reciever) {

		if(reciever != null) {
			this.reciever = reciever;
			thread = new Thread(this);
			thread.start();
		}
	}

	public void run() {
		while(true) {
			try {
				byte [] bytesIn = new byte[reciever.maxPacketSize];
				DatagramPacket packet = new DatagramPacket(bytesIn,bytesIn.length);
				reciever.socket.receive(packet);
				reciever.processPacket(packet);
				//thread.sleep(5000);  //Change this to delay messages being shown on the screen.
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
