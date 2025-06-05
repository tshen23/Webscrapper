package net.neological.gui;

import net.neological.webscraping.WebScraper;
import net.neological.webscraping.specific.FredWebScraper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class WebScraperGUI extends JFrame {
    private JTextField urlField;
    private JTextField downloadFolderField;
    private JComboBox<String> scraperComboBox;
    private JButton browseButton;
    private JButton scrapeButton;
    private JTextArea logArea;

    // Map to store scraper name to class mapping
    private final Map<String, Class<? extends WebScraper>> scraperClasses = new HashMap<>();

    public WebScraperGUI() {
        // Register available
        registerScraper("FRED", FredWebScraper.class);

        // Set up the frame
        setTitle("Web Scraper GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);

        // Create components
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Input panel
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Scraper selection
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("Scraper Type:"), gbc);

        scraperComboBox = new JComboBox<>(scraperClasses.keySet().toArray(new String[0]));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        inputPanel.add(scraperComboBox, gbc);

        // URL input
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        inputPanel.add(new JLabel("URL:"), gbc);

        urlField = new JTextField(30);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        inputPanel.add(urlField, gbc);

        // Download folder
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        inputPanel.add(new JLabel("Download Folder:"), gbc);

        downloadFolderField = new JTextField(System.getProperty("user.home") + File.separator + "Downloads");
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        inputPanel.add(downloadFolderField, gbc);

        browseButton = new JButton("Browse...");
        browseButton.addActionListener(this::browseButtonClicked);
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        inputPanel.add(browseButton, gbc);

        // Scrape button
        scrapeButton = new JButton("Scrape");
        scrapeButton.addActionListener(this::scrapeButtonClicked);
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        inputPanel.add(scrapeButton, gbc);

        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        // Add components to main panel
        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Add main panel to frame
        add(mainPanel);
    }

    private void registerScraper(String name, Class<? extends WebScraper> scraperClass) {
        scraperClasses.put(name, scraperClass);
    }

    private void browseButtonClicked(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setCurrentDirectory(new File(downloadFolderField.getText()));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            downloadFolderField.setText(selectedFile.getAbsolutePath());
        }
    }

    private void scrapeButtonClicked(ActionEvent e) {
        String url = urlField.getText().trim();
        String downloadFolder = downloadFolderField.getText().trim();
        String scraperName = (String) scraperComboBox.getSelectedItem();

        if (url.isEmpty()) {
            logMessage("Please enter a URL");
            return;
        }

        if (downloadFolder.isEmpty()) {
            logMessage("Please specify a download folder");
            return;
        }

        // Disable UI during scraping
        setUIEnabled(false);
        logMessage("Starting scraping with " + scraperName + "...");

        // Run scraping in background thread to keep UI responsive
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    // Create scraper instance using reflection
                    Class<? extends WebScraper> scraperClass = scraperClasses.get(scraperName);
                    Constructor<? extends WebScraper> constructor = scraperClass.getConstructor(
                            String.class, String.class, int.class);

                    WebScraper scraper = constructor.newInstance(
                            scraperName,
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
                            15_000);

                    // Set download folder if it's a FredWebScraper
                    if (scraper instanceof FredWebScraper) {
                        ((FredWebScraper) scraper).setDownloadFolder(downloadFolder);
                    }

                    // Redirect System.out and System.err to the log area
                    PrintStreamRedirector.redirectSystemOut(message -> SwingUtilities.invokeLater(() -> logMessage(message)));

                    // Perform scraping
                    scraper.scrape(url);

                    return null;
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> logMessage("Error: " + ex.getMessage()));
                    ex.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void done() {
                // Re-enable UI after scraping is done
                setUIEnabled(true);
                logMessage("Scraping completed.");
            }
        };

        worker.execute();
    }

    private void setUIEnabled(boolean enabled) {
        urlField.setEnabled(enabled);
        downloadFolderField.setEnabled(enabled);
        scraperComboBox.setEnabled(enabled);
        browseButton.setEnabled(enabled);
        scrapeButton.setEnabled(enabled);
    }

    private void logMessage(String message) {
        logArea.append(message + "\n");
        // Scroll to the bottom
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    // Helper class to redirect System.out and System.err
    private static class PrintStreamRedirector {
        public interface MessageConsumer {
            void consume(String message);
        }

        public static void redirectSystemOut(MessageConsumer consumer) {
            System.setOut(new java.io.PrintStream(System.out) {
                @Override
                public void println(String x) {
                    super.println(x);
                    consumer.consume(x);
                }
            });

            System.setErr(new java.io.PrintStream(System.err) {
                @Override
                public void println(String x) {
                    super.println(x);
                    consumer.consume("ERROR: " + x);
                }
            });
        }
    }
}