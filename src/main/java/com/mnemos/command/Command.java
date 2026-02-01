package com.mnemos.command;

@FunctionalInterface
public interface Command {
    void execute();

    default String getName() {
        return this.getClass().getSimpleName();
    }

    default String getDescription() {
        return "";
    }

    default String getCategory() {
        return "General";
    }
}
