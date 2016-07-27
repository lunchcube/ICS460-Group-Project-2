package com.Echo;

import java.io.*;
import java.net.*;

import javax.swing.*;

public class UDPEchoClient {

    public final static int PORT = 1025; // might not work under port 1024

    public static int timeout = Integer.parseInt(JOptionPane.showInputDialog
        ("What should the timeout be in milliseconds?"));
    public static int seqNumber = 1;
    public static int nextSeqNum = 2;
    private static DatagramSocket socket;
    private static int ackNum = 0;


    public static void main(String[] args) {

        String hostname = "localhost";

        if (args.length > 0) hostname = args[0];
        try {
            InetAddress ia = InetAddress.getByName(hostname);
            socket = new DatagramSocket();
            SenderThread sender = new SenderThread(socket, ia, PORT);
            sender.start();
            Thread receiver = new ReceiverThread(socket);
            receiver.start();
            } catch (UnknownHostException ex) {System.err.println(ex);
            } catch (SocketException ex){ System.err.println(ex);
        }
    }

    //Send Acknowledgment of received packet
    protected void sendAck() throws IOException {

        socket.send(null);
    }
}
    class SenderThread extends Thread {
        private InetAddress server;
        private DatagramSocket socket;
        private int port;
        private volatile boolean stopped = false;

        SenderThread(DatagramSocket socket, InetAddress address, int port) {
            this.server = address;
            this.port = port;
            this.socket = socket;
            this.socket.connect(server, port);
        }
        public void halt(){
            this.stopped = true;
        }
        @Override
        public void run(){
            try {
                BufferedReader userInput = new BufferedReader(new
                    InputStreamReader(System.in));
                while(true) {
                    if (stopped) return;
                    String theLine = userInput.readLine();
                    if (theLine.equals(".")) break;
                    byte[] data = theLine.getBytes("UTF-8");
                    DatagramPacket output = new DatagramPacket(data,
                        data.length, server, port);
                    socket.send(output);
                    Thread.yield();
                }
            } catch (IOException ex) {System.err.println(ex);}
        }
        public static byte[] intToByteArray(int data) {
            return new byte[] {
            (byte)((data >> 24) & 0xff),
            (byte)((data >> 16) & 0xff),
            (byte)((data >> 8) & 0xff),
            (byte)((data >> 0) & 0xff),
            };
        }

        @SuppressWarnings("cast")
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

    class ReceiverThread extends Thread {
        private DatagramSocket socket;
        private volatile boolean stopped = false;
        private int curSeqNum = 1;
        private int nextSeqNum = 1;

        ReceiverThread(DatagramSocket socket){
            this.socket = socket;
        }
        public void halt() {
            this.stopped = true;
        }
        @Override
        public void run() {
            byte[] buffer = new byte[65507];
            while (true){
                if(stopped) return;
                DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                try{
                    socket.receive(dp);
                    //Send ackknowledgement here
                    System.out.println("Current sequence number: " + curSeqNum);
                    nextSeqNum++;
                    System.out.println("Next sequence number expected: " + nextSeqNum);
                    curSeqNum++;

                    String s = new String(dp.getData(), 0, dp.getLength(),"UTF-8");
                    System.out.println(s);
                    Thread.yield();
                } catch (IOException ex){
                    System.err.println(ex);
                }
            }
        }
    }
