package net.neological;

import net.neological.webscraping.specific.FredWebScraper;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        FredWebScraper fredWebscraper = new FredWebScraper(
                "FRED",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64",
                15_000);

        fredWebscraper.scrape("https://fred.stlouisfed.org/searchresults/?st=Wayne%20County");
    }
}