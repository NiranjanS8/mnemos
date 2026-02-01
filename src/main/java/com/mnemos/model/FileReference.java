package com.mnemos.model;

import java.time.Instant;

public class FileReference {
    private Long id;
    private String name;
    private String path;
    private String type;
    private Instant addedAt;

    public FileReference(Long id, String name, String path, String type, Instant addedAt) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.type = type;
        this.addedAt = addedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Instant getAddedAt() { return addedAt; }
    public void setAddedAt(Instant addedAt) { this.addedAt = addedAt; }
}
