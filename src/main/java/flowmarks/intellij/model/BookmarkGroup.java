package flowmarks.intellij.model;

import java.util.List;

public record BookmarkGroup(String name, List<Bookmark> items) {
    public BookmarkGroup {
        items = List.copyOf(items);
    }

    @Override
    public String toString() {
        return name;
    }
}
