package flowmarks.intellij.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookmarkLineLocatorTest {
    private final BookmarkLineLocator locator = new BookmarkLineLocator();

    @Test
    void usesStoredLineWhenTextMatches() {
        assertEquals(1, locator.locate(new Bookmark("entry", "src/A.java", 2, "call()"),
                List.of("before", " call() ")));
    }

    @Test
    void relocatesWhenStoredLineMoved() {
        assertEquals(2, locator.locate(new Bookmark("entry", "src/A.java", 1, "call()"),
                List.of("before", "middle", " call() ")));
    }

    @Test
    void rejectsAmbiguousOrMissingExpectedText() {
        Bookmark movedBookmark = new Bookmark("entry", "src/A.java", 1, "call()");
        assertThrows(IllegalArgumentException.class, () -> locator.locate(movedBookmark, List.of("before", "call()", "call()")));
        assertThrows(IllegalArgumentException.class, () -> locator.locate(movedBookmark, List.of("changed")));
    }
}
