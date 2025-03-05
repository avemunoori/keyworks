package com.example.keyworks.midi;

import javax.sound.midi.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class VirtualMidiKeyboard extends JFrame {
    private MidiDevice outputDevice;
    private Receiver receiver;
    private final JComboBox<String> deviceSelector;
    private final JPanel keyboardPanel;
    private final String[] noteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private boolean isConnected = false;

    public VirtualMidiKeyboard() {
        super("Virtual MIDI Keyboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 300);
        
        // Create device selector
        deviceSelector = new JComboBox<>();
        refreshDeviceList();
        
        JButton refreshButton = new JButton("Refresh Devices");
        refreshButton.addActionListener(this::refreshDevices);
        
        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(this::connectToDevice);
        
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("MIDI Output Device:"));
        topPanel.add(deviceSelector);
        topPanel.add(refreshButton);
        topPanel.add(connectButton);
        
        // Create keyboard panel
        keyboardPanel = new JPanel();
        keyboardPanel.setLayout(new GridLayout(1, 24));
        createKeyboard();
        
        // Add panels to frame
        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(keyboardPanel), BorderLayout.CENTER);
        
        // Status bar
        JPanel statusPanel = new JPanel(new BorderLayout());
        JLabel statusLabel = new JLabel("Not connected to any MIDI device");
        statusPanel.add(statusLabel, BorderLayout.WEST);
        add(statusPanel, BorderLayout.SOUTH);
    }
    
    private void refreshDevices(ActionEvent e) {
        refreshDeviceList();
    }
    
    private void refreshDeviceList() {
        deviceSelector.removeAllItems();
        
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (MidiDevice.Info info : infos) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                // Only include devices that can receive MIDI messages
                if (device.getMaxReceivers() != 0) {
                    deviceSelector.addItem(info.getName() + " - " + info.getDescription());
                }
            } catch (MidiUnavailableException ex) {
                System.err.println("Error accessing MIDI device: " + ex.getMessage());
            }
        }
    }
    
    private void connectToDevice(ActionEvent e) {
        if (isConnected) {
            // Disconnect first
            if (receiver != null) {
                receiver.close();
            }
            if (outputDevice != null) {
                outputDevice.close();
            }
            isConnected = false;
            ((JButton)e.getSource()).setText("Connect");
            return;
        }
        
        String selectedItem = (String) deviceSelector.getSelectedItem();
        if (selectedItem == null) {
            JOptionPane.showMessageDialog(this, "Please select a MIDI device");
            return;
        }
        
        String deviceName = selectedItem.split(" - ")[0];
        
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (MidiDevice.Info info : infos) {
            if (info.getName().equals(deviceName)) {
                try {
                    outputDevice = MidiSystem.getMidiDevice(info);
                    outputDevice.open();
                    receiver = outputDevice.getReceiver();
                    isConnected = true;
                    ((JButton)e.getSource()).setText("Disconnect");
                    JOptionPane.showMessageDialog(this, "Connected to " + deviceName);
                    return;
                } catch (MidiUnavailableException ex) {
                    JOptionPane.showMessageDialog(this, 
                        "Error connecting to MIDI device: " + ex.getMessage(), 
                        "Connection Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    private void createKeyboard() {
        // Create 2 octaves of keys starting from C3 (MIDI note 48)
        for (int i = 0; i < 24; i++) {
            final int noteNumber = 48 + i;
            final int octave = (noteNumber / 12) - 1;
            final int noteIndex = noteNumber % 12;
            final String noteName = noteNames[noteIndex] + octave;
            
            JButton keyButton = new JButton(noteName);
            keyButton.setPreferredSize(new Dimension(50, 120));
            
            // Make black keys look different
            if (noteName.contains("#")) {
                keyButton.setBackground(Color.BLACK);
                keyButton.setForeground(Color.WHITE);
            }
            
            // Add action listeners for mouse press and release
            keyButton.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent evt) {
                    if (isConnected) {
                        sendNoteOn(noteNumber, 100);
                    }
                }
                
                @Override
                public void mouseReleased(java.awt.event.MouseEvent evt) {
                    if (isConnected) {
                        sendNoteOff(noteNumber);
                    }
                }
            });
            
            keyboardPanel.add(keyButton);
        }
    }
    
    private void sendNoteOn(int noteNumber, int velocity) {
        try {
            ShortMessage message = new ShortMessage();
            message.setMessage(ShortMessage.NOTE_ON, 0, noteNumber, velocity);
            receiver.send(message, -1);
            System.out.println("Note On: " + noteNumber + " velocity: " + velocity);
        } catch (InvalidMidiDataException e) {
            System.err.println("Error sending MIDI message: " + e.getMessage());
        }
    }
    
    private void sendNoteOff(int noteNumber) {
        try {
            ShortMessage message = new ShortMessage();
            message.setMessage(ShortMessage.NOTE_OFF, 0, noteNumber, 0);
            receiver.send(message, -1);
            System.out.println("Note Off: " + noteNumber);
        } catch (InvalidMidiDataException e) {
            System.err.println("Error sending MIDI message: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new VirtualMidiKeyboard().setVisible(true);
        });
    }
}