package net.neological;

import net.neological.gui.WebScraperGUI;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Set look and feel to system default
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Launch the GUI
        SwingUtilities.invokeLater(() -> {
            WebScraperGUI gui = new WebScraperGUI();
            gui.setVisible(true);
        });
    }
}