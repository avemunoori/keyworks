package com.example.keyworks.midi;

import javax.sound.midi.*;
import java.util.Scanner;

public class MidiTestSender {
    public static void main(String[] args) {
        Scanner scanner = null;
        try {
            // List all MIDI devices
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            System.out.println("Available MIDI devices:");
            for (int i = 0; i < infos.length; i++) {
                MidiDevice device = MidiSystem.getMidiDevice(infos[i]);
                String deviceType = "";
                
                if (device.getMaxTransmitters() != 0) {
                    deviceType += "Input";
                }
                
                if (device.getMaxReceivers() != 0) {
                    if (!deviceType.isEmpty()) {
                        deviceType += "/";
                    }
                    deviceType += "Output";
                }
                
                System.out.println(i + ": " + infos[i].getName() + " - " + infos[i].getDescription() + " (" + deviceType + ")");
            }
            
            // Ask user to select a device
            System.out.print("Enter the number of the MIDI device to use: ");
            scanner = new Scanner(System.in);
            int deviceIndex = scanner.nextInt();
            
            if (deviceIndex < 0 || deviceIndex >= infos.length) {
                System.out.println("Invalid device number");
                return;
            }
            
            // Open the selected device
            MidiDevice outputDevice = MidiSystem.getMidiDevice(infos[deviceIndex]);
            outputDevice.open();
            
            // Get a receiver from the device
            Receiver receiver = outputDevice.getReceiver();
            System.out.println("Connected to " + infos[deviceIndex].getName());
            
            // Play a C major scale
            int[] notes = {60, 62, 64, 65, 67, 69, 71, 72};
            
            System.out.println("Playing C major scale...");
            for (int note : notes) {
                // Note on
                ShortMessage noteOn = new ShortMessage();
                noteOn.setMessage(ShortMessage.NOTE_ON, 0, note, 100);
                receiver.send(noteOn, -1);
                System.out.println("Note On: " + note);
                
                // Wait
                Thread.sleep(500);
                
                // Note off
                ShortMessage noteOff = new ShortMessage();
                noteOff.setMessage(ShortMessage.NOTE_OFF, 0, note, 0);
                receiver.send(noteOff, -1);
                System.out.println("Note Off: " + note);
                
                Thread.sleep(100);
            }
            
            // Clean up
            receiver.close();
            outputDevice.close();
            System.out.println("Test complete!");
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close the scanner in the finally block to ensure it's always closed
            if (scanner != null) {
                scanner.close();
            }
        }
    }
}