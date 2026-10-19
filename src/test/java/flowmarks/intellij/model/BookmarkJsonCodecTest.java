package flowmarks.intellij.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookmarkJsonCodecTest {
    private final BookmarkJsonCodec codec = new BookmarkJsonCodec();

    @Test
    void parsesFlowMarksFormat() {
        BookmarkDocument document = codec.parse("""
                {"version":1,"groups":[{"name":"Login","items":[{"title":"Entry","filePath":"src/Main.java","line":4,"expectedLineText":"main()"}]}]}
                """);

        assertEquals(1, document.version());
        assertEquals("Login", document.groups().getFirst().name());
        assertEquals(4, document.groups().getFirst().items().getFirst().line());
    }

    @Test
    void parsesBookmarkAiCandidateFormat() {
        BookmarkDocument document = codec.parse("""
                {"targetGroupName":"Payment","items":[{"title":"Charge","filePath":"src/Pay.java","line":8,"expectedLineText":"charge()"}]}
                """);

        assertEquals("Payment", document.groups().getFirst().name());
        assertEquals("Charge", document.groups().getFirst().items().getFirst().title());
    }

    @Test
    void rejectsInvalidLineNumbersAndVersions() {
        assertThrows(IllegalArgumentException.class, () -> codec.parse("""
                {"targetGroupName":"g","items":[{"title":"x","filePath":"a","line":0}]}
                """));
        assertThrows(IllegalArgumentException.class, () -> codec.parse("""
                {"version":2,"groups":[]}
                """));
    }
}
