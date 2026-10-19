package flowmarks.intellij.model;

import java.util.List;

public final class BookmarkLineLocator {
    public int locate(Bookmark bookmark, List<String> lines) {
        int expectedIndex = bookmark.line() - 1;
        String expectedText = bookmark.expectedLineText().trim();
        if (expectedIndex < lines.size() && expectedIndex >= 0) {
            if (expectedText.isEmpty() || lines.get(expectedIndex).trim().equals(expectedText)) {
                return expectedIndex;
            }
        }

        if (expectedText.isEmpty()) {
            throw new IllegalArgumentException("保存的行号已失效，且没有可用于重新定位的原始代码文本。");
        }

        int foundIndex = -1;
        for (int index = 0; index < lines.size(); index++) {
            if (lines.get(index).trim().equals(expectedText)) {
                if (foundIndex >= 0) {
                    throw new IllegalArgumentException("原始代码文本出现多次，无法确定书签位置。");
                }
                foundIndex = index;
            }
        }
        if (foundIndex < 0) {
            throw new IllegalArgumentException("找不到保存的原始代码行，无法重新定位。");
        }
        return foundIndex;
    }
}
