package org.example;

import org.example.client.ui.MainApp;

/**
 * Plain-main launcher – bypasses JavaFX module restrictions on some JDKs.
 * Run the CLIENT : mvn javafx:run
 * Run the SERVER : java -jar target/AppMessagerie-server-jar-with-dependencies.jar
 */
public class Main {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
