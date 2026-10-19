package flowmarks.intellij.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

public final class BookmarkInbox {
    private static final Path INBOX = Path.of(".flowmarks", "inbox");
    private static final Path ARCHIVE = Path.of(".flowmarks", "archive");
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final BookmarkJsonCodec codec;

    public BookmarkInbox(BookmarkJsonCodec codec) {
        this.codec = codec;
    }

    public ScanResult scan(Path projectRoot) throws IOException {
        Path root = projectRoot.toAbsolutePath().normalize();
        Path inbox = root.resolve(INBOX);
        if (!Files.isDirectory(inbox)) {
            return ScanResult.empty();
        }

        Path realRoot = root.toRealPath();
        List<Candidate> candidates = new ArrayList<>();
        List<Failure> failures = new ArrayList<>();
        try (Stream<Path> files = Files.list(inbox)) {
            List<Path> jsonFiles = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
            for (Path file : jsonFiles) {
                try {
                    Path realFile = file.toRealPath();
                    if (!realFile.startsWith(realRoot)) {
                        throw new IllegalArgumentException("候选文件不在当前项目目录内。");
                    }
                    BookmarkDocument document = codec.parse(Files.readString(realFile, StandardCharsets.UTF_8));
                    validatePaths(realRoot, document);
                    candidates.add(new Candidate(realFile, document));
                } catch (Exception exception) {
                    failures.add(new Failure(file, message(exception)));
                }
            }
        }
        return new ScanResult(candidates, failures);
    }

    public Path archive(Path projectRoot, Path candidate) throws IOException {
        Path root = projectRoot.toAbsolutePath().normalize();
        Path inbox = root.resolve(INBOX);
        Path archiveDirectory = root.resolve(ARCHIVE);
        Path normalized = candidate.toAbsolutePath().normalize();
        if (!normalized.startsWith(inbox)) {
            throw new IOException("只能归档当前项目候选目录中的文件。");
        }

        Path realRoot = root.toRealPath();
        Path realCandidate = normalized.toRealPath();
        Path realInbox = inbox.toRealPath();
        if (!realCandidate.startsWith(realRoot) || !realInbox.equals(realCandidate.getParent())) {
            throw new IOException("候选文件位置无效。");
        }

        Files.createDirectories(archiveDirectory);
        String stamp = TIMESTAMP.format(LocalDateTime.now());
        Path target = uniqueTarget(archiveDirectory, stamp + "-" + realCandidate.getFileName());
        try {
            Files.move(realCandidate, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(realCandidate, target);
        }
        return target;
    }

    private static void validatePaths(Path root, BookmarkDocument document) {
        for (BookmarkGroup group : document.groups()) {
            for (Bookmark bookmark : group.items()) {
                Path path;
                try {
                    path = Path.of(bookmark.filePath());
                } catch (InvalidPathException exception) {
                    throw new IllegalArgumentException("书签路径无效：" + bookmark.filePath(), exception);
                }
                if (path.isAbsolute()) {
                    throw new IllegalArgumentException("书签必须使用项目相对路径：" + bookmark.filePath());
                }
                if (!root.resolve(path).normalize().startsWith(root)) {
                    throw new IllegalArgumentException("书签路径越出当前项目目录：" + bookmark.filePath());
                }
            }
        }
    }

    private static Path uniqueTarget(Path directory, String fileName) {
        Path target = directory.resolve(fileName);
        if (!Files.exists(target)) {
            return target;
        }

        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        String extension = dot > 0 ? fileName.substring(dot) : "";
        int counter = 1;
        while (Files.exists(target)) {
            target = directory.resolve(base + "-" + counter++ + extension);
        }
        return target;
    }

    private static String message(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    public record Candidate(Path file, BookmarkDocument document) {
        public Candidate {
            Objects.requireNonNull(file, "file");
            Objects.requireNonNull(document, "document");
        }

        public String fileName() {
            return file.getFileName().toString();
        }

        public int bookmarkCount() {
            return document.groups().stream().mapToInt(group -> group.items().size()).sum();
        }
    }

    public record Failure(Path file, String message) {
        public Failure {
            Objects.requireNonNull(file, "file");
            Objects.requireNonNull(message, "message");
        }

        public String fileName() {
            return file.getFileName().toString();
        }
    }

    public record ScanResult(List<Candidate> candidates, List<Failure> failures) {
        public ScanResult {
            candidates = List.copyOf(candidates);
            failures = List.copyOf(failures);
        }

        public static ScanResult empty() {
            return new ScanResult(List.of(), List.of());
        }

        public boolean hasCandidates() {
            return !candidates.isEmpty();
        }

        public int bookmarkCount() {
            return candidates.stream().mapToInt(Candidate::bookmarkCount).sum();
        }
    }
}
