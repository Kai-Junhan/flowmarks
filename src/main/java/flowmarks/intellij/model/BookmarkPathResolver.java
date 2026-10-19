package flowmarks.intellij.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BookmarkPathResolver {
    public Path resolve(Path projectRoot, String bookmarkPath) throws IOException {
        if (bookmarkPath == null || bookmarkPath.isBlank()) {
            throw new IllegalArgumentException("书签文件路径为空。");
        }

        Path root = projectRoot.toRealPath();
        Path candidate = Path.of(bookmarkPath);
        if (!candidate.isAbsolute()) {
            candidate = root.resolve(candidate);
        }
        Path normalized = candidate.toAbsolutePath().normalize();
        if (!normalized.startsWith(root)) {
            throw new IllegalArgumentException("书签路径越出当前项目目录：" + bookmarkPath);
        }
        if (!Files.exists(normalized)) {
            throw new IOException("书签文件不存在：" + bookmarkPath);
        }

        Path realFile = normalized.toRealPath();
        if (!realFile.startsWith(root)) {
            throw new IllegalArgumentException("书签路径指向项目目录之外：" + bookmarkPath);
        }
        if (!Files.isRegularFile(realFile)) {
            throw new IOException("书签目标不是普通文件：" + bookmarkPath);
        }
        return realFile;
    }
}
