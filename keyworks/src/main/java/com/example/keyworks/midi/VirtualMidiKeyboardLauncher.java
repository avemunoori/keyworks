package com.example.keyworks.midi;

import javax.swing.SwingUtilities;

public class VirtualMidiKeyboardLauncher {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new VirtualMidiKeyboard().setVisible(true);
        });
    }
}