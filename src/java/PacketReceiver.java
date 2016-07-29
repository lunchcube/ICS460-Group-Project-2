

import java.io.*;
import java.net.*;

import javax.swing.*;

public class PacketReceiver implements Runnable {

	private Reciever reciever;
	Thread thread;
	int sleepTime = Integer.parseInt(JOptionPane.showInputDialog("How much time between receiver messages printing to screen?"));

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
				thread.sleep(sleepTime);  //Change this to delay messages being shown on the screen.
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
