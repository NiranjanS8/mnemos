package com.mnemos.service;

import com.mnemos.model.*;
import com.mnemos.model.Link.ItemType;
import com.mnemos.repository.LinkRepository;
import com.mnemos.repository.NoteRepository;
import com.mnemos.repository.TaskRepository;
import com.mnemos.repository.FileRepository;

import java.util.ArrayList;
import java.util.List;

public class LinkService {
    private final LinkRepository linkRepository = new LinkRepository();
    private final TaskRepository taskRepository = new TaskRepository();
    private final NoteRepository noteRepository = new NoteRepository();
    private final FileRepository fileRepository = new FileRepository();

    public void linkItems(ItemType sourceType, Long sourceId, ItemType targetType, Long targetId) {
        if (!linkRepository.isLinked(sourceType, sourceId, targetType, targetId)) {
            Link link = new Link(sourceType, sourceId, targetType, targetId);
            linkRepository.save(link);
        }
    }

    public void unlinkItems(Long linkId) {
        linkRepository.delete(linkId);
    }

    public List<LinkedItem> getLinkedItems(ItemType type, Long id) {
        List<LinkedItem> linkedItems = new ArrayList<>();
        List<Link> links = linkRepository.getLinksForItem(type, id);

        for (Link link : links) {
            ItemType otherType;
            Long otherId;

            if (link.getSourceType() == type && link.getSourceId().equals(id)) {
                otherType = link.getTargetType();
                otherId = link.getTargetId();
            } else {
                otherType = link.getSourceType();
                otherId = link.getSourceId();
            }

            LinkedItem item = getItemDetails(otherType, otherId, link.getId());
            if (item != null) {
                linkedItems.add(item);
            }
        }

        return linkedItems;
    }

    private LinkedItem getItemDetails(ItemType type, Long id, Long linkId) {
        return switch (type) {
            case TASK -> {
                var taskOpt = taskRepository.findById(id);
                if (taskOpt.isPresent()) {
                    Task task = taskOpt.get();
                    yield new LinkedItem(type, id, task.getTitle(),
                            task.getStatus().toString(), linkId);
                }
                yield null;
            }
            case NOTE -> {
                var noteOpt = noteRepository.findById(id);
                if (noteOpt.isPresent()) {
                    Note note = noteOpt.get();
                    String preview = note.getContent() != null && note.getContent().length() > 50
                            ? note.getContent().substring(0, 50) + "..."
                            : note.getContent();
                    yield new LinkedItem(type, id, note.getTitle(), preview, linkId);
                }
                yield null;
            }
            case FILE -> {
                var fileOpt = fileRepository.findById(id);
                if (fileOpt.isPresent()) {
                    FileReference file = fileOpt.get();
                    yield new LinkedItem(type, id, file.getName(), file.getType(), linkId);
                }
                yield null;
            }
        };
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public List<Note> getAllNotes() {
        return noteRepository.findAll();
    }

    public List<FileReference> getAllFiles() {
        return fileRepository.findAll();
    }
}
