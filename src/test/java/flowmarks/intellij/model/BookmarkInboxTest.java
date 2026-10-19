package flowmarks.intellij.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookmarkInboxTest {
    @TempDir
    Path tempDir;

    private final BookmarkJsonCodec codec = new BookmarkJsonCodec();
    private final BookmarkInbox inbox = new BookmarkInbox(codec);

    @Test
    void scansOnlyTheCurrentProjectInbox() throws IOException {
        Path first = Files.createDirectory(tempDir.resolve("first"));
        Path second = Files.createDirectory(tempDir.resolve("second"));
        writeCandidate(first, "first.json", "First", "src/First.java");
        writeCandidate(second, "second.json", "Second", "src/Second.java");

        BookmarkInbox.ScanResult firstResult = inbox.scan(first);
        BookmarkInbox.ScanResult secondResult = inbox.scan(second);

        assertEquals(1, firstResult.candidates().size());
        assertEquals("First", firstResult.candidates().getFirst().document().groups().getFirst().name());
        assertEquals(1, secondResult.candidates().size());
        assertEquals("Second", secondResult.candidates().getFirst().document().groups().getFirst().name());
    }

    @Test
    void rejectsAbsoluteAndTraversalPaths() throws IOException {
        Path root = Files.createDirectory(tempDir.resolve("project"));
        String absolute = Path.of("outside.java").toAbsolutePath().toString().replace("\\", "\\\\");
        writeRaw(root, "absolute.json", """
                {"version":1,"groups":[{"name":"Bad","items":[{"title":"Absolute","filePath":"%s","line":1,"expectedLineText":"x"}]}]}
                """.formatted(absolute));
        writeRaw(root, "traversal.json", """
                {"version":1,"groups":[{"name":"Bad","items":[{"title":"Traversal","filePath":"../outside.java","line":1,"expectedLineText":"x"}]}]}
                """);

        BookmarkInbox.ScanResult result = inbox.scan(root);

        assertTrue(result.candidates().isEmpty());
        assertEquals(2, result.failures().size());
    }

    @Test
    void reportsMalformedCandidatesWithoutFailingTheScan() throws IOException {
        Path root = Files.createDirectory(tempDir.resolve("project"));
        writeRaw(root, "broken.json", "{not-json");

        BookmarkInbox.ScanResult result = inbox.scan(root);

        assertTrue(result.candidates().isEmpty());
        assertEquals(1, result.failures().size());
        assertEquals("broken.json", result.failures().getFirst().fileName());
    }

    @Test
    void archivesCandidateInsideTheSameProject() throws IOException {
        Path root = Files.createDirectory(tempDir.resolve("project"));
        Path candidate = writeCandidate(root, "login.json", "Login", "src/Login.java");

        Path archived = inbox.archive(root, candidate);

        assertFalse(Files.exists(candidate));
        assertTrue(Files.exists(archived));
        assertTrue(archived.startsWith(root.resolve(".flowmarks").resolve("archive")));
        assertTrue(Files.readString(archived, StandardCharsets.UTF_8).contains("Login"));
    }

    @Test
    void ignoresNonJsonFiles() throws IOException {
        Path root = Files.createDirectory(tempDir.resolve("project"));
        writeRaw(root, "notes.txt", "not a bookmark");

        BookmarkInbox.ScanResult result = inbox.scan(root);

        assertTrue(result.candidates().isEmpty());
        assertTrue(result.failures().isEmpty());
    }

    @Test
    void refusesToArchiveFilesOutsideTheInbox() throws IOException {
        Path root = Files.createDirectory(tempDir.resolve("project"));
        Path outside = Files.writeString(root.resolve("outside.json"), "{}", StandardCharsets.UTF_8);

        assertThrows(IOException.class, () -> inbox.archive(root, outside));
        assertTrue(Files.exists(outside));
    }

    private Path writeCandidate(Path root, String name, String group, String filePath) throws IOException {
        return writeRaw(root, name, """
                {"version":1,"groups":[{"name":"%s","items":[{"title":"Entry","filePath":"%s","line":1,"expectedLineText":"class Main {"}]}]}
                """.formatted(group, filePath));
    }

    private Path writeRaw(Path root, String name, String content) throws IOException {
        Path directory = root.resolve(".flowmarks").resolve("inbox");
        Files.createDirectories(directory);
        return Files.writeString(directory.resolve(name), content, StandardCharsets.UTF_8);
    }
}
