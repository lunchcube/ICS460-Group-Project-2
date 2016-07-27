package com.Echo;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;

@SuppressWarnings("serial")
public class UIClient extends JFrame  {

    // Output panel variables
        private JPanel contentPane;
        private JPanel inputPane;
        private JTextField packSizeTF;
        private JTextField timeIntervalTF;
        private JTextField windowSizeTF;
        private JTextField seqNumOneTF;
        private JTextField seqNumTwoTF;
        private JButton sendFileButton;
        private static JTextArea textArea;


        public UIClient() {

    // Sets title to window and sets characteristics to JFrame.
            super("UDP Client Control Panel");
            setResizable(false);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setBounds(100, 100, 610, 400);

    // Sets the content pane to a JPanel.
            contentPane = new JPanel();
            contentPane.setBorder(new LineBorder(new Color(0, 0, 0), 4, true));
            setContentPane(contentPane);
            contentPane.setLayout(null);
            Font defaultFont = new Font("Arial", Font.PLAIN, 12);

    // Adds the content to the content panel.
            inputPane = new JPanel();
            inputPane.setBounds(10, 32, 580, 310);
            contentPane.add(inputPane);
            inputPane.setLayout(null);

    // labels for input fields
            JLabel lblSizePacket = new JLabel("Size of Packet");
            lblSizePacket.setFont(defaultFont);
            lblSizePacket.setHorizontalAlignment(SwingConstants.RIGHT);
            lblSizePacket.setBounds(40,24,100,14);
            inputPane.add(lblSizePacket);

            JLabel lblTimeOut = new JLabel("Timeout Interval");
            lblTimeOut.setFont(defaultFont);
            lblTimeOut.setHorizontalAlignment(SwingConstants.RIGHT);
            lblTimeOut.setBounds(40,54,100, 14);
            inputPane.add(lblTimeOut);

            JLabel lblWindowSize = new JLabel("Window Size");
            lblWindowSize.setFont(defaultFont);
            lblWindowSize.setHorizontalAlignment(SwingConstants.RIGHT);
            lblWindowSize.setBounds(40, 84,100,14);
            inputPane.add(lblWindowSize);

            JLabel lblseqNumRangeTF = new JLabel("Seq Num Range:");
            lblseqNumRangeTF.setFont(defaultFont);
            lblseqNumRangeTF.setHorizontalAlignment(SwingConstants.RIGHT);
            lblseqNumRangeTF.setBounds(40, 114,100,14);
            inputPane.add(lblseqNumRangeTF);

    // Input text areas.
             packSizeTF = new JTextField();
             packSizeTF.setFont(defaultFont);
             packSizeTF.setBounds(175, 20 , 100, 20);
             inputPane.add(packSizeTF);
             packSizeTF.setColumns(25);

             timeIntervalTF =  new JTextField();
             timeIntervalTF.setFont(defaultFont);
             timeIntervalTF.setBounds(175, 52 , 100, 20);
             inputPane.add(timeIntervalTF );
             timeIntervalTF.setColumns(25);

             windowSizeTF =  new JTextField();
             windowSizeTF.setFont(defaultFont);
             windowSizeTF.setBounds(175, 84 , 100, 20);
             inputPane.add(windowSizeTF);
             windowSizeTF.setColumns(25);

             seqNumOneTF =  new JTextField();
             seqNumOneTF.setFont(defaultFont);
             seqNumOneTF.setBounds(175, 114 , 100, 20);
             inputPane.add(seqNumOneTF);
             seqNumOneTF.setColumns(25);

             seqNumTwoTF = new JTextField();
             seqNumTwoTF.setFont(defaultFont);
             seqNumTwoTF.setBounds(300, 114 , 100, 20);
             inputPane.add(seqNumTwoTF);
             seqNumTwoTF.setColumns(25);


    // Output text field for the results.
            textArea = new JTextArea();
            textArea.setFont(defaultFont);
            textArea.setBounds(10, 200, 600, 240);
            inputPane.add(textArea);
            textArea.setColumns(25);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setEditable(true);



    //Creates a button to calculate the user input.
            sendFileButton = new JButton("Send");
            sendFileButton.setFont(defaultFont);
            sendFileButton.setBounds(480, 150, 90, 28);
            inputPane.add(sendFileButton);

    // Mouse button listener
            sendFileButton.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {

                // *** call client class with variables taken from control panel
                }
            });

            }
    // Updates the text area with message from client and server classes
            public static void updateTextArea(String message){
                textArea.append(message + "\n");
            }

            public static void main(String[] s) {

                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        try {
                            UIClient frame = new UIClient();
                            frame.setVisible(true);
                            String message = "This is where the text from the client and server goes.";
                            updateTextArea(message);
                            String message2 = "You can add multiple messages";
                            updateTextArea(message2);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
}



