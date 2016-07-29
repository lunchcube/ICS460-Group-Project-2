

import java.io.*;
import java.net.*;

import javax.swing.*;

public class PacketSender implements Runnable {
	Thread thisThread;
	DatagramSocket socket;
	Sender sender;
	int sleepTime = Integer.parseInt(JOptionPane.showInputDialog("How much time between sender messages printing to screen?"));

	PacketSender(DatagramSocket socket, Sender instance) {
		thisThread = new Thread(this);
		thisThread.start();
		this.socket = socket;
		this.sender = instance;
	}

	@Override
	public void run() {
		while(true) {
			try {
				byte [] ackIn = new byte[4];
				DatagramPacket packet = new DatagramPacket(ackIn,ackIn.length);
				socket.receive(packet);
				sender.processACK(packet);
				thisThread.sleep(sleepTime);  //Change this to for message delay on screen to make it readable
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
