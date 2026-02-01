package com.mnemos.service;

import com.mnemos.model.Note;
import com.mnemos.repository.NoteRepository;
import java.util.List;
import java.util.Optional;

public class NoteService {
    private final NoteRepository repository;

    public NoteService() {
        this.repository = new NoteRepository();
    }

    public Note saveNote(Note note) {
        if (note.getTitle() == null || note.getTitle().isBlank()) {
            note.setTitle("Untitled Note");
        }
        return repository.save(note);
    }

    public List<Note> getAllNotes() {
        return repository.findAll();
    }

    public Optional<Note> getNoteById(Long id) {
        return repository.findById(id);
    }

    public void deleteNote(Long id) {
        repository.deleteById(id);
    }
}
