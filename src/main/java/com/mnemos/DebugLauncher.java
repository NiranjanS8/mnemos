package com.mnemos;

import com.mnemos.model.Note;
import com.mnemos.repository.NoteRepository;
import com.mnemos.util.DatabaseManager;
import java.util.List;
import java.io.FileWriter;
import java.io.IOException;

public class DebugLauncher {
    public static void main(String[] args) {
        try (FileWriter fw = new FileWriter("debug_result.txt")) {
            fw.write("Initializing Database...\n");
            try {
                DatabaseManager.initialize();
                fw.write("Database Initialized.\n");
            } catch (Throwable t) {
                fw.write("DB Init Failed: " + t.getMessage() + "\n");
                for (StackTraceElement s : t.getStackTrace())
                    fw.write(s.toString() + "\n");
                return;
            }

            NoteRepository repo = new NoteRepository();

            fw.write("Creating Note...\n");
            try {
                Note note = new Note("Debug Note", "Content");
                repo.save(note);
                fw.write("Note Saved. ID: " + note.getId() + "\n");
            } catch (Throwable t) {
                fw.write("Save Failed: " + t.getMessage() + "\n");
                for (StackTraceElement s : t.getStackTrace())
                    fw.write(s.toString() + "\n");
            }

            fw.write("Reading Notes...\n");
            try {
                List<Note> notes = repo.findAll();
                fw.write("Found " + notes.size() + " notes.\n");
                for (Note n : notes) {
                    fw.write(" - " + n.getTitle() + ": " + n.getContent() + "\n");
                }
            } catch (Throwable t) {
                fw.write("FindAll Failed: " + t.getMessage() + "\n");
            }

            fw.write("Done.\n");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
