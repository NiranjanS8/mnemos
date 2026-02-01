package com.mnemos.service;

import com.mnemos.model.FileReference;
import com.mnemos.repository.FileRepository;
import java.io.File;
import java.awt.Desktop;
import java.time.Instant;
import java.util.List;

public class FileService {
    private final FileRepository repository;

    public FileService() {
        this.repository = new FileRepository();
    }

    public FileReference addFile(File file) {
        String type = getFileExtension(file);
        FileReference ref = new FileReference(
                null,
                file.getName(),
                file.getAbsolutePath(),
                type,
                Instant.now());
        return repository.save(ref);
    }

    public List<FileReference> getAllFiles() {
        return repository.findAll();
    }

    public void openFile(FileReference ref) {
        try {
            File file = new File(ref.getPath());
            if (file.exists()) {
                Desktop.getDesktop().open(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteFile(Long id) {
        repository.deleteById(id);
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return name.substring(lastIndexOf + 1);
    }
}
