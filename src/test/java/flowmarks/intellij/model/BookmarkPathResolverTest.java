package flowmarks.intellij.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookmarkPathResolverTest {
    @TempDir
    Path projectRoot;

    private final BookmarkPathResolver resolver = new BookmarkPathResolver();

    @Test
    void resolvesRelativePathInsideProject() throws IOException {
        Path target = projectRoot.resolve("src/Main.java");
        Files.createDirectories(target.getParent());
        Files.writeString(target, "class Main {}");

        assertEquals(target.toRealPath(), resolver.resolve(projectRoot, "src/Main.java"));
    }

    @Test
    void rejectsTraversalAndAbsolutePathsOutsideProject() throws IOException {
        Path outside = Files.createTempFile("flowmarks-outside", ".java");
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(projectRoot, "../outside.java"));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(projectRoot, outside.toString()));
    }

    @Test
    void reportsMissingFile() {
        assertThrows(IOException.class, () -> resolver.resolve(projectRoot, "src/Missing.java"));
    }
}
