package com.mnemos;

/**
 * A simple launcher class that does NOT extend Application.
 * This is required for fat/shaded JARs because the Java launcher
 * checks if the main class extends javafx.application.Application
 * and enforces module-path loading of JavaFX — which fails in a fat JAR.
 */
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
