package com.mnemos.model;

/**
 * Wrapper class for displaying linked items in the UI
 */
public class LinkedItem {
    private Link.ItemType type;
    private Long id;
    private String title;
    private String preview;
    private Long linkId;

    public LinkedItem(Link.ItemType type, Long id, String title, String preview, Long linkId) {
        this.type = type;
        this.id = id;
        this.title = title;
        this.preview = preview;
        this.linkId = linkId;
    }

    public Link.ItemType getType() {
        return type;
    }

    public void setType(Link.ItemType type) {
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPreview() {
        return preview;
    }

    public void setPreview(String preview) {
        this.preview = preview;
    }

    public Long getLinkId() {
        return linkId;
    }

    public void setLinkId(Long linkId) {
        this.linkId = linkId;
    }

    public String getIcon() {
        return switch (type) {
            case TASK -> "📋";
            case NOTE -> "📝";
            case FILE -> "📎";
        };
    }
}
