## Install Maven
Download Maven:
- Go to https://maven.apache.org/download.cgi
- Unzip it somewhere like C:\Program Files.

Add mvn to your PATH:
- Open Windows Settings → “Environment Variables.”
- Under “System variables,” find and select Path → Edit → New.
- Paste the Maven bin folder (for example):

```
C:\Program Files\maven-mvnd-1.0.2-windows-amd64\mvn\bin
```
- Click OK on all dialogs to save.

## Compile

Open a terminal in the project root directory and run:
mvn clean package

This will produce `target/Webscrapper-1.0-SNAPSHOT.jar`.

## Run

After building, execute:
java -jar target/Webscrapper-1.0-SNAPSHOT.jar