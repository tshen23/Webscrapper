package net.neological.gui;

import net.neological.webscraping.FileDownloader;
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
    private JButton assistedModeButton;
    private JButton completeButton;
    private JTextArea logArea;

    // Assisted mode components
    private AssistedModeManager assistedModeManager;
    private JPanel downloadFolderPanel; // Panel containing download folder components

    // Map to store scraper name to class mapping
    private final Map<String, Class<? extends WebScraper>> scraperClasses = new HashMap<>();

    public WebScraperGUI() {
        // Register available scrapers
        registerScraper("FRED", FredWebScraper.class);

        // Set up the frame
        setTitle("Web Scraper GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 400); // Slightly wider to accommodate new button
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

        // Add default option to scraper combo box
        String[] scraperOptions = new String[scraperClasses.size() + 1];
        scraperOptions[0] = "-- Select --";
        System.arraycopy(scraperClasses.keySet().toArray(new String[0]), 0, scraperOptions, 1, scraperClasses.size());

        scraperComboBox = new JComboBox<>(scraperOptions);
        scraperComboBox.addActionListener(this::scraperSelectionChanged);
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
        gbc.gridwidth = 1;
        inputPanel.add(urlField, gbc);

        // Assisted mode button
        assistedModeButton = new JButton("Assisted Mode");
        assistedModeButton.addActionListener(this::assistedModeButtonClicked);
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        inputPanel.add(assistedModeButton, gbc);

        // Download folder panel (will be shown/hidden based on scraper selection)
        downloadFolderPanel = new JPanel(new GridBagLayout());
        GridBagConstraints dfGbc = new GridBagConstraints();
        dfGbc.fill = GridBagConstraints.HORIZONTAL;
        dfGbc.insets = new Insets(0, 0, 0, 0);

        dfGbc.gridx = 0;
        dfGbc.gridy = 0;
        downloadFolderPanel.add(new JLabel("Download Folder:"), dfGbc);

        downloadFolderField = new JTextField(System.getProperty("user.home") + File.separator + "Downloads");
        dfGbc.gridx = 1;
        dfGbc.gridy = 0;
        dfGbc.weightx = 1.0;
        downloadFolderPanel.add(downloadFolderField, dfGbc);

        browseButton = new JButton("Browse...");
        browseButton.addActionListener(this::browseButtonClicked);
        dfGbc.gridx = 2;
        dfGbc.gridy = 0;
        dfGbc.weightx = 0.0;
        downloadFolderPanel.add(browseButton, dfGbc);

        // Add download folder panel to main input panel
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        inputPanel.add(downloadFolderPanel, gbc);

        // Complete button (initially hidden)
        completeButton = new JButton("Complete");
        completeButton.addActionListener(this::completeButtonClicked);
        completeButton.setVisible(false);
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        inputPanel.add(completeButton, gbc);

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

        // Initialize assisted mode manager
        assistedModeManager = new AssistedModeManager();

        // Initially hide download folder panel
        updateDownloadFolderVisibility();
    }

    private void registerScraper(String name, Class<? extends WebScraper> scraperClass) {
        scraperClasses.put(name, scraperClass);
    }

    private void scraperSelectionChanged(ActionEvent e) {
        updateDownloadFolderVisibility();
    }

    private void updateDownloadFolderVisibility() {
        String selectedScraper = (String) scraperComboBox.getSelectedItem();
        boolean showDownloadFolder = false;

        if (selectedScraper != null && !selectedScraper.equals("-- Select --")) {
            Class<? extends WebScraper> scraperClass = scraperClasses.get(selectedScraper);

            if (scraperClass != null) {
                showDownloadFolder = FileDownloader.class.isAssignableFrom(scraperClass);
            }
        }

        downloadFolderPanel.setVisible(showDownloadFolder);
        revalidate();
        repaint();
    }

    private void assistedModeButtonClicked(ActionEvent e) {
        try {
            assistedModeManager.startAssistedMode();
            assistedModeButton.setVisible(false);
            completeButton.setVisible(true);
            logMessage("Assisted mode started. Navigate to your desired URL and click 'Complete' when ready.");
        } catch (Exception ex) {
            logMessage("Error starting assisted mode: " + ex.getMessage());
        }
    }

    private void completeButtonClicked(ActionEvent e) {
        try {
            String currentUrl = assistedModeManager.getCurrentUrl();
            if (currentUrl != null && !currentUrl.isEmpty()) {
                urlField.setText(currentUrl);
                logMessage("URL captured: " + currentUrl);
            } else {
                logMessage("No URL could be captured from the browser.");
            }
        } catch (Exception ex) {
            logMessage("Error capturing URL: " + ex.getMessage());
        } finally {
            assistedModeManager.stopAssistedMode();
            assistedModeButton.setVisible(true);
            completeButton.setVisible(false);
            logMessage("Assisted mode completed.");
        }
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

        if (scraperName == null || scraperName.equals("-- Select --")) {
            logMessage("Please select a scraper type");
            return;
        }

        if (url.isEmpty()) {
            logMessage("Please enter a URL");
            return;
        }

        // Check if download folder is required and provided
        Class<? extends WebScraper> scraperClass = scraperClasses.get(scraperName);
        if (FileDownloader.class.isAssignableFrom(scraperClass) && downloadFolder.isEmpty()) {
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
                    Constructor<? extends WebScraper> constructor = scraperClass.getConstructor(
                            String.class, int.class);

                    WebScraper scraper = constructor.newInstance(
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
                            15_000);

                    // Set download folder if it's a FileDownloader
                    if (scraper instanceof FileDownloader && scraper instanceof FredWebScraper) {
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
        assistedModeButton.setEnabled(enabled);
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