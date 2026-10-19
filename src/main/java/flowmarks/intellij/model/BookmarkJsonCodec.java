package flowmarks.intellij.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

public final class BookmarkJsonCodec {
    public BookmarkDocument parse(String json) {
        JsonElement rootElement;
        try {
            rootElement = JsonParser.parseString(json);
        } catch (JsonParseException exception) {
            throw new IllegalArgumentException("JSON 格式错误：" + exception.getMessage(), exception);
        }
        if (!rootElement.isJsonObject()) {
            throw new IllegalArgumentException("JSON 顶层必须是对象。");
        }

        JsonObject root = rootElement.getAsJsonObject();
        if (root.has("groups")) {
            int version = integer(root, "version", 1);
            if (version != 1) {
                throw new IllegalArgumentException("不支持的书签数据版本：" + version);
            }
            return new BookmarkDocument(version, parseGroups(requiredArray(root, "groups")));
        }

        String groupName = string(root, "targetGroupName", "候选书签缺少分组名称 targetGroupName。");
        List<Bookmark> items = parseItems(requiredArray(root, "items"));
        return new BookmarkDocument(1, List.of(new BookmarkGroup(groupName, items)));
    }

    public String serialize(BookmarkDocument document) {
        JsonObject root = new JsonObject();
        root.addProperty("version", document.version());
        JsonArray groups = new JsonArray();
        for (BookmarkGroup sourceGroup : document.groups()) {
            JsonObject group = new JsonObject();
            group.addProperty("name", sourceGroup.name());
            JsonArray items = new JsonArray();
            for (Bookmark bookmark : sourceGroup.items()) {
                JsonObject item = new JsonObject();
                item.addProperty("title", bookmark.title());
                item.addProperty("filePath", bookmark.filePath());
                item.addProperty("line", bookmark.line());
                item.addProperty("expectedLineText", bookmark.expectedLineText());
                if (!bookmark.step().isBlank()) {
                    item.addProperty("step", bookmark.step());
                }
                if (bookmark.level() > 0) {
                    item.addProperty("level", bookmark.level());
                }
                items.add(item);
            }
            group.add("items", items);
            groups.add(group);
        }
        root.add("groups", groups);
        return new GsonBuilder().setPrettyPrinting().create().toJson(root) + System.lineSeparator();
    }

    private static List<BookmarkGroup> parseGroups(JsonArray groups) {
        List<BookmarkGroup> result = new ArrayList<>();
        for (int index = 0; index < groups.size(); index++) {
            JsonElement element = groups.get(index);
            if (!element.isJsonObject()) {
                throw new IllegalArgumentException("分组第 " + (index + 1) + " 项必须是对象。");
            }
            JsonObject group = element.getAsJsonObject();
            String name = string(group, "name", "分组缺少名称 name。");
            result.add(new BookmarkGroup(name, parseItems(requiredArray(group, "items"))));
        }
        return result;
    }

    private static List<Bookmark> parseItems(JsonArray items) {
        List<Bookmark> result = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            JsonElement element = items.get(index);
            if (!element.isJsonObject()) {
                throw new IllegalArgumentException("书签第 " + (index + 1) + " 项必须是对象。");
            }
            JsonObject item = element.getAsJsonObject();
            String title = string(item, "title", "书签缺少标题 title。");
            String filePath = string(item, "filePath", "书签缺少文件路径 filePath。");
            int line = integer(item, "line", -1);
            if (line < 1) {
                throw new IllegalArgumentException("书签行号必须从 1 开始：" + title);
            }
            String expectedLineText = item.has("expectedLineText") && !item.get("expectedLineText").isJsonNull()
                    ? item.get("expectedLineText").getAsString()
                    : "";
            String step = optionalString(item, "step");
            int level = integer(item, "level", 0);
            if (level < 0) {
                throw new IllegalArgumentException("书签层级 level 不能为负数：" + title);
            }
            result.add(new Bookmark(title, filePath, line, expectedLineText, step, level));
        }
        return result;
    }

    private static JsonArray requiredArray(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonArray()) {
            throw new IllegalArgumentException("字段 " + key + " 必须是数组。");
        }
        return value.getAsJsonArray();
    }

    private static String string(JsonObject object, String key, String error) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(error);
        }
        String result = value.getAsString().trim();
        if (result.isEmpty()) {
            throw new IllegalArgumentException(error);
        }
        return result;
    }

    private static int integer(JsonObject object, String key, int defaultValue) {
        JsonElement value = object.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return value.getAsInt();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("字段 " + key + " 必须是整数。", exception);
        }
    }

    private static String optionalString(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) {
            return "";
        }
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("字段 " + key + " 必须是字符串。");
        }
        return value.getAsString().trim();
    }
}
