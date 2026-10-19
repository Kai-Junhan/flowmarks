package flowmarks.intellij.model;

public record Bookmark(String title, String filePath, int line, String expectedLineText, String step, int level) {
    public Bookmark {
        step = step == null ? "" : step.trim();
        level = Math.max(0, Math.min(level, 9));
    }

    public Bookmark(String title, String filePath, int line, String expectedLineText) {
        this(title, filePath, line, expectedLineText, "", 0);
    }

    @Override
    public String toString() {
        String prefix = step.isBlank() ? "" : step + " ";
        return prefix + title + "  [" + filePath + ":" + line + "]";
    }
}
