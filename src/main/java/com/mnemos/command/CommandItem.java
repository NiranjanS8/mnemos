package com.mnemos.command;

public class CommandItem {
    private final String name;
    private final String description;
    private final String category;
    private final Command command;
    private final String icon;

    public CommandItem(String name, String description, String category, String icon, Command command) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.icon = icon;
        this.command = command;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public Command getCommand() {
        return command;
    }

    public String getIcon() {
        return icon;
    }

    public String getDisplayText() {
        return icon + "  " + name;
    }

    public boolean matches(String query) {
        if (query == null || query.isEmpty())
            return true;
        String lowerQuery = query.toLowerCase();
        return name.toLowerCase().contains(lowerQuery) ||
                description.toLowerCase().contains(lowerQuery) ||
                category.toLowerCase().contains(lowerQuery);
    }
}
