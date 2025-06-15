// src/main/java/net/neological/webscraping/FileDownloader.java
package net.neological.webscraping;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Interface for downloading files from URLs to specified file paths.
 */
public interface FileDownloader {

    /**
     * Downloads a file from the given URL to the specified file path.
     *
     * @param fileUrl      the URL of the file to download
     * @param filePath the destination path where the file should be saved
     * @throws IOException if an error occurs during download
     */
    default void downloadFile(String fileUrl, String filePath) throws IOException {
        try {
            URI uri = URI.create(fileUrl);
            URL url = uri.toURL();

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10_000); // 10 seconds
            connection.setReadTimeout(10_000);

            int statusCode = connection.getResponseCode();
            if (statusCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Failed to download file: HTTP status code " + statusCode);
            }

            Path dir = Paths.get(filePath);
            if (Files.exists(dir) && !Files.isDirectory(dir)) {
                connection.disconnect();
                throw new IOException("The specified path exists and is not a directory: " + filePath);
            }
            Files.createDirectories(dir);

            // Destination file within the folder
            Path destFile = dir.resolve(filePath);

            // Stream data from the URL to the destination file
            try (InputStream in = connection.getInputStream()) {
                Files.copy(in, destFile, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                connection.disconnect();
            }

        } catch (Exception e) {
            System.err.println("Error downloading CSV: " + e.getMessage());
        }
    }
}