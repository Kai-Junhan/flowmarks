package flowmarks.intellij.model;

import java.util.List;

public record BookmarkDocument(int version, List<BookmarkGroup> groups) {
    public BookmarkDocument {
        groups = List.copyOf(groups);
    }
}
