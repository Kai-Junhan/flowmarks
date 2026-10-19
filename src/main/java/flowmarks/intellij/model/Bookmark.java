package flowmarks.intellij.model;

public record Bookmark(String title, String filePath, int line, String expectedLineText) {
    @Override
    public String toString() {
        return title + "  [" + filePath + ":" + line + "]";
    }
}
