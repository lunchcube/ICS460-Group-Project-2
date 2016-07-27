package com.Echo;

import java.io.*;
import java.net.*;
import java.util.logging.*;

import javax.swing.*;

public class UDPEchoServer implements Runnable{

    public final static int DEFAULT_PORT = 1025;
    static UDPEchoServer server;
    static Thread t;


    private static int lastAckReceived = 0;
    private static int lastAckSent = 0;
    private static int expectedAckNum = 0;
    private static int lastPacketSent = 0;
    private static int packetNum = 1;

    public static int windowSize = Integer.parseInt(JOptionPane.showInputDialog
        ("What is the sliding window size?"));

    static DatagramPacket[] windowStorage = new DatagramPacket[windowSize];

    private int bufferSize = Integer.parseInt(JOptionPane.showInputDialog
        ("What is the size of the packet (in bytes)?")); // in bytes
    private final int port;
    private final Logger logger = Logger.getLogger(UDPEchoServer.class.getCanonicalName());
    private volatile boolean isShutDown = false;
    public static double packetDropChance = Double.parseDouble(JOptionPane.showInputDialog
        ("What is the percentage chance of a packet drop (in decimal form)?"));

    public UDPEchoServer(int port, int bufferSize){
        this.bufferSize = bufferSize;
        this.port = port;
    }
    public UDPEchoServer (int port){
        this(port, 8192);
    }
    public UDPEchoServer(){
        this(DEFAULT_PORT);
    }
    public boolean dropPacket(){
        if(Math.random() * 100 < packetDropChance * 100){
            return true;
        }
        else
            return false;

    }
    @Override
    public void run(){
        byte[] buffer = new byte[bufferSize];
        try (DatagramSocket socket = new DatagramSocket(port)) {
            socket.setSoTimeout(10000); // check every 10 seconds for shutdown
            while(true){
                if (isShutDown) return;
                DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(incoming);
                    this.respond(socket, incoming);
                } catch (SocketTimeoutException ex){
                    if (isShutDown) return;
                } catch (IOException ex) {
                    logger.log(Level.WARNING, ex.getMessage(), ex);
                }
            } //end while
        } catch (SocketException ex) {
            logger.log(Level.SEVERE, "Could not bind to port: " + port, ex);
        }
    }

    public void shutDown() {
        this.isShutDown = true;
    }

    public void goBackN(){
        if ((lastPacketSent - lastAckReceived) >= windowSize){

        }
    }

    public void respond(DatagramSocket socket, DatagramPacket packet) throws IOException {
        DatagramPacket outgoing = new DatagramPacket(packet.getData(),
            packet.getLength(), packet.getAddress(), packet.getPort());
            byte[] nextPacketNum = intToByteArray(packetNum);
            if(dropPacket()){
                System.out.println("Packet # " + packetNum + " dropped!");
                try{
                    System.out.println("Trying to resend packet #" + packetNum + ".....");
                    socket.send(outgoing);
                    System.out.println("Packet number sent: " + packetNum + "\n");
                    packetNum++;
                }catch (Exception x) {
                    // TODO: handle exception
                }
            }
            else{
                System.out.println("Packet number sent: " + packetNum + "\n");
                packetNum++;
                socket.send(outgoing);
            }
    }
    public static byte[] intToByteArray(int data)
    {
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

    public static void main(String[] args){
        server = new UDPEchoServer();
        t = new Thread(server);
        t.start();
    }
}
