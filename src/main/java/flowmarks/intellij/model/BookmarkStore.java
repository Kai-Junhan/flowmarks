package flowmarks.intellij.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BookmarkStore {
    private final BookmarkJsonCodec codec;

    public BookmarkStore(BookmarkJsonCodec codec) {
        this.codec = codec;
    }

    public BookmarkDocument load(Path path) throws IOException {
        if (!Files.exists(path)) {
            return empty();
        }
        return codec.parse(Files.readString(path, StandardCharsets.UTF_8));
    }

    public void save(Path path, BookmarkDocument document) throws IOException {
        Path parent = path.toAbsolutePath().normalize().getParent();
        if (parent == null) {
            throw new IOException("无法确定书签文件所在目录。");
        }
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, ".flowmarks-", ".tmp");
        try {
            Files.writeString(temporary, codec.serialize(document), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public BookmarkDocument merge(BookmarkDocument first, BookmarkDocument second) {
        Map<String, LinkedHashMap<String, Bookmark>> groups = new LinkedHashMap<>();
        addToIndex(groups, first);
        addToIndex(groups, second);

        List<BookmarkGroup> mergedGroups = new ArrayList<>();
        for (Map.Entry<String, LinkedHashMap<String, Bookmark>> entry : groups.entrySet()) {
            mergedGroups.add(new BookmarkGroup(entry.getKey(), new ArrayList<>(entry.getValue().values())));
        }
        return new BookmarkDocument(1, mergedGroups);
    }

    public BookmarkDocument removeBookmark(BookmarkDocument document, String groupName, Bookmark target) {
        List<BookmarkGroup> groups = new ArrayList<>();
        for (BookmarkGroup group : document.groups()) {
            if (!group.name().equals(groupName)) {
                groups.add(group);
                continue;
            }
            List<Bookmark> remaining = group.items().stream()
                    .filter(bookmark -> !bookmark.equals(target))
                    .toList();
            if (!remaining.isEmpty()) {
                groups.add(new BookmarkGroup(group.name(), remaining));
            }
        }
        return new BookmarkDocument(1, groups);
    }

    public BookmarkDocument removeGroup(BookmarkDocument document, String groupName) {
        return new BookmarkDocument(1, document.groups().stream()
                .filter(group -> !group.name().equals(groupName))
                .toList());
    }

    public static BookmarkDocument empty() {
        return new BookmarkDocument(1, List.of());
    }

    private static void addToIndex(Map<String, LinkedHashMap<String, Bookmark>> groups, BookmarkDocument document) {
        for (BookmarkGroup group : document.groups()) {
            LinkedHashMap<String, Bookmark> items = groups.computeIfAbsent(group.name(), ignored -> new LinkedHashMap<>());
            for (Bookmark bookmark : group.items()) {
                items.putIfAbsent(deduplicationKey(bookmark), bookmark);
            }
        }
    }

    private static String deduplicationKey(Bookmark bookmark) {
        return bookmark.title() + "\u0000" + bookmark.filePath() + "\u0000" + bookmark.expectedLineText();
    }
}
