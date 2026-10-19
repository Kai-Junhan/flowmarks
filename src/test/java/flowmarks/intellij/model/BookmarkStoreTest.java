package flowmarks.intellij.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookmarkStoreTest {
    @TempDir
    Path tempDir;

    private final BookmarkJsonCodec codec = new BookmarkJsonCodec();
    private final BookmarkStore store = new BookmarkStore(codec);

    @Test
    void mergesDuplicateImportsAndPersistsUtf8Json() throws IOException {
        Bookmark bookmark = new Bookmark("入口", "src/Main.java", 2, "main()");
        BookmarkDocument source = new BookmarkDocument(1, List.of(new BookmarkGroup("登录链路", List.of(bookmark))));
        Path file = tempDir.resolve(".flowmarks.json");

        store.save(file, store.merge(BookmarkStore.empty(), source));
        store.save(file, store.merge(store.load(file), source));

        BookmarkDocument loaded = store.load(file);
        assertEquals(1, loaded.groups().size());
        assertEquals(1, loaded.groups().getFirst().items().size());
        assertTrue(Files.readString(file).contains("入口"));
    }
}
