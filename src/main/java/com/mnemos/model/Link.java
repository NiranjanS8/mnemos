package com.mnemos.model;

public class Link {
    private Long id;
    private ItemType sourceType;
    private Long sourceId;
    private ItemType targetType;
    private Long targetId;
    private Long createdAt;

    public enum ItemType {
        TASK, NOTE, FILE
    }

    public Link() {
    }

    public Link(ItemType sourceType, Long sourceId, ItemType targetType, Long targetId) {
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.createdAt = System.currentTimeMillis();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ItemType getSourceType() {
        return sourceType;
    }

    public void setSourceType(ItemType sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public ItemType getTargetType() {
        return targetType;
    }

    public void setTargetType(ItemType targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
}
