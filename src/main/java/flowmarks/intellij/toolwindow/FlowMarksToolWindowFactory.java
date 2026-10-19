package flowmarks.intellij.toolwindow;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;
import flowmarks.intellij.model.Bookmark;
import flowmarks.intellij.model.BookmarkDocument;
import flowmarks.intellij.model.BookmarkGroup;
import flowmarks.intellij.model.BookmarkInbox;
import flowmarks.intellij.model.BookmarkJsonCodec;
import flowmarks.intellij.model.BookmarkLineLocator;
import flowmarks.intellij.model.BookmarkPathResolver;
import flowmarks.intellij.model.BookmarkStore;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class FlowMarksToolWindowFactory implements ToolWindowFactory, DumbAware {
    private static final PluginId PLUGIN_ID = PluginId.getId("flowmarks.intellij");

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        toolWindow.getComponent().add(JBUI.Panels.simplePanel(new FlowMarksPanel(project)));
    }

    private static String pluginVersion() {
        IdeaPluginDescriptor descriptor = PluginManagerCore.getPlugin(PLUGIN_ID);
        return descriptor == null ? "development" : descriptor.getVersion();
    }

    private static final class FlowMarksPanel extends JPanel {
        private final Project project;
        private final Path projectRoot;
        private final Path storePath;
        private final BookmarkJsonCodec codec = new BookmarkJsonCodec();
        private final BookmarkStore store = new BookmarkStore(codec);
        private final BookmarkInbox inbox = new BookmarkInbox(codec);
        private final BookmarkPathResolver pathResolver = new BookmarkPathResolver();
        private final BookmarkLineLocator lineLocator = new BookmarkLineLocator();
        private final JBLabel status = new JBLabel();
        private final JBTextField searchField = new JBTextField();
        private final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("代码链路书签");
        private final DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        private final JTree tree = new JTree(treeModel);
        private final JBLabel candidateLabel = new JBLabel();
        private final JPanel candidatePanel = new JPanel();
        private final JButton importCandidatesButton;
        private final JButton ignoreCandidatesButton;
        private BookmarkDocument document = BookmarkStore.empty();
        private BookmarkInbox.ScanResult scanResult = BookmarkInbox.ScanResult.empty();

        private FlowMarksPanel(Project project) {
            super(new BorderLayout(JBUI.scale(6), JBUI.scale(6)));
            this.project = project;
            String basePath = project.getBasePath();
            projectRoot = basePath == null ? null : Path.of(basePath).toAbsolutePath().normalize();
            storePath = projectRoot == null ? null : projectRoot.resolve(".flowmarks.json");
            setBorder(JBUI.Borders.empty(10));

            JPanel header = new JPanel(new BorderLayout(JBUI.scale(6), JBUI.scale(6)));
            JPanel titlePanel = new JPanel(new BorderLayout());
            JBLabel title = new JBLabel("代码链路书签");
            title.setFont(JBUI.Fonts.label(18.0f));
            titlePanel.add(title, BorderLayout.NORTH);
            titlePanel.add(new JBLabel("FlowMarks " + pluginVersion()), BorderLayout.SOUTH);
            header.add(titlePanel, BorderLayout.NORTH);

            searchField.getEmptyText().setText("搜索标题、分组或文件路径");
            header.add(searchField, BorderLayout.CENTER);

            JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(4), 0));
            toolbar.add(button("新增", "收藏当前编辑器行", this::addCurrentLine));
            toolbar.add(button("删除", "删除选中的书签或分组", this::deleteSelection));
            toolbar.add(button("刷新", "重新读取当前项目的书签和候选目录", this::refresh));
            toolbar.add(button("导入文件", "手动选择 agent 生成的 JSON 文件", this::chooseImport));
            toolbar.add(button("导出", "导出全部书签为 JSON 文件", this::export));
            header.add(toolbar, BorderLayout.SOUTH);

            candidatePanel.setLayout(new BorderLayout(JBUI.scale(4), 0));
            importCandidatesButton = button("导入候选", "导入当前项目 .flowmarks/inbox 中等待处理的候选", this::importCandidates);
            ignoreCandidatesButton = button("忽略", "将候选文件归档但不导入书签", this::ignoreCandidates);
            JPanel candidateActions = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(4), 0));
            candidateActions.add(importCandidatesButton);
            candidateActions.add(ignoreCandidatesButton);
            candidatePanel.add(candidateLabel, BorderLayout.CENTER);
            candidatePanel.add(candidateActions, BorderLayout.EAST);
            candidatePanel.setVisible(false);

            tree.setRootVisible(false);
            tree.setShowsRootHandles(true);
            tree.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    if (event.getClickCount() == 2) {
                        openSelectedBookmark();
                    }
                }
            });
            searchField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent event) { populateTree(); }
                @Override
                public void removeUpdate(DocumentEvent event) { populateTree(); }
                @Override
                public void changedUpdate(DocumentEvent event) { populateTree(); }
            });

            JPanel top = new JPanel(new BorderLayout(0, JBUI.scale(6)));
            top.add(header, BorderLayout.NORTH);
            top.add(candidatePanel, BorderLayout.SOUTH);

            add(top, BorderLayout.NORTH);
            add(new JBScrollPane(tree), BorderLayout.CENTER);
            add(status, BorderLayout.SOUTH);
            refresh();
        }

        private JButton button(String text, String tooltip, Runnable action) {
            JButton button = new JButton(text);
            button.setToolTipText(tooltip);
            button.addActionListener(event -> action.run());
            return button;
        }

        private void chooseImport() {
            if (storePath == null) {
                showStatus("当前项目没有可用的项目根目录。");
                return;
            }
            FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("json")
                    .withTitle("导入代码链路书签")
                    .withDescription("选择 agent 生成的 FlowMarks 或 BookmarkAI JSON 候选");
            VirtualFile selected = FileChooser.chooseFile(descriptor, project, null);
            if (selected == null) {
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this,
                    "书签将合并并保存在：\n" + storePath + "\n\n原始候选文件不会被修改。是否继续？",
                    "确认本地保存位置", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (confirm != JOptionPane.OK_OPTION) {
                return;
            }
            try {
                BookmarkDocument incoming = codec.parse(Files.readString(Path.of(selected.getPath()), StandardCharsets.UTF_8));
                BookmarkDocument current = store.load(storePath);
                BookmarkDocument merged = store.merge(current, incoming);
                store.save(storePath, merged);
                document = merged;
                populateTree();
                showStatus("导入完成，已保存到 " + storePath + "；重复书签已跳过。" );
            } catch (Exception exception) {
                showStatus("导入失败：" + safeMessage(exception));
            }
        }

        private void refresh() {
            refresh("");
        }

        private void refresh(String statusPrefix) {
            if (storePath == null) {
                showStatus("当前项目没有可用的项目根目录。");
                return;
            }
            showStatus("正在读取当前项目书签...");
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    BookmarkDocument loaded = store.load(storePath);
                    BookmarkInbox.ScanResult scanned = inbox.scan(projectRoot);
                    boolean storeExists = Files.exists(storePath);
                    ApplicationManager.getApplication().invokeLater(
                            () -> applyRefresh(loaded, scanned, storeExists, statusPrefix));
                } catch (Exception exception) {
                    ApplicationManager.getApplication().invokeLater(
                            () -> showStatus("读取失败，原文件未修改：" + safeMessage(exception)));
                }
            });
        }

        private void applyRefresh(BookmarkDocument loaded, BookmarkInbox.ScanResult scanned,
                                  boolean storeExists, String statusPrefix) {
            document = loaded;
            scanResult = scanned;
            populateTree();
            updateCandidatePanel();
            StringBuilder message = new StringBuilder(storeExists
                    ? "已从 " + storePath + " 读取 " + countBookmarks(document) + " 条书签。"
                    : "尚无本地书签；导入后保存到 " + storePath + "。");
            if (scanResult.hasCandidates()) {
                message.append(" 发现 ").append(scanResult.candidates().size())
                        .append(" 个候选文件，共 ").append(scanResult.bookmarkCount()).append(" 条书签。");
            }
            if (!scanResult.failures().isEmpty()) {
                message.append(" 有 ").append(scanResult.failures().size()).append(" 个候选文件无法解析。");
            }
            if (!statusPrefix.isBlank()) {
                message.insert(0, statusPrefix + " ");
            }
            showStatus(message.toString());
        }

        private void updateCandidatePanel() {
            int fileCount = scanResult.candidates().size();
            boolean hasCandidates = fileCount > 0;
            if (hasCandidates) {
                candidateLabel.setText("待导入候选：" + fileCount + " 个文件 / " + scanResult.bookmarkCount() + " 条书签");
            } else if (!scanResult.failures().isEmpty()) {
                candidateLabel.setText("候选目录：" + scanResult.failures().size() + " 个文件无法解析");
            } else {
                candidateLabel.setText("");
            }

            String tooltip = scanResult.failures().stream()
                    .map(failure -> failure.fileName() + "：" + failure.message())
                    .collect(Collectors.joining("\n"));
            candidateLabel.setToolTipText(tooltip.isEmpty() ? null : tooltip);
            importCandidatesButton.setVisible(hasCandidates);
            ignoreCandidatesButton.setVisible(hasCandidates);
            candidatePanel.setVisible(hasCandidates || !scanResult.failures().isEmpty());
        }

        private void importCandidates() {
            if (storePath == null) {
                showStatus("当前项目没有可用的项目根目录。");
                return;
            }
            if (scanResult.candidates().isEmpty()) {
                showStatus("没有待导入的候选文件。");
                return;
            }

            StringBuilder preview = new StringBuilder("将导入以下候选书签：\n\n");
            for (BookmarkInbox.Candidate candidate : scanResult.candidates()) {
                preview.append("• ").append(candidate.fileName())
                        .append("（").append(candidate.bookmarkCount()).append(" 条）");
                if (!candidate.document().groups().isEmpty()) {
                    preview.append("：").append(candidate.document().groups().stream()
                            .map(BookmarkGroup::name)
                            .collect(Collectors.joining("、")));
                }
                preview.append('\n');
            }
            preview.append("\n正式书签保存到：\n").append(storePath)
                    .append("\n\n导入后候选文件会移动到 .flowmarks/archive/。是否继续？");
            int confirm = JOptionPane.showConfirmDialog(this, preview.toString(), "导入候选书签",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (confirm != JOptionPane.OK_OPTION) {
                return;
            }

            int fileCount = scanResult.candidates().size();
            int bookmarkCount = scanResult.bookmarkCount();
            BookmarkDocument merged = document;
            for (BookmarkInbox.Candidate candidate : scanResult.candidates()) {
                merged = store.merge(merged, candidate.document());
            }
            try {
                store.save(storePath, merged);
            } catch (Exception exception) {
                showStatus("导入失败，正式书签未修改：" + safeMessage(exception));
                return;
            }

            List<String> archiveErrors = new ArrayList<>();
            for (BookmarkInbox.Candidate candidate : scanResult.candidates()) {
                try {
                    inbox.archive(projectRoot, candidate.file());
                } catch (Exception exception) {
                    archiveErrors.add(candidate.fileName() + "：" + safeMessage(exception));
                }
            }

            document = merged;
            String result = "已导入 " + fileCount + " 个候选文件，" + bookmarkCount + " 条书签";
            if (archiveErrors.isEmpty()) {
                result += "，候选文件已归档";
            } else {
                result += "；部分候选归档失败：" + String.join("；", archiveErrors);
            }
            refresh(result + "。数据保存到 " + storePath + "。");
        }

        private void ignoreCandidates() {
            if (storePath == null) {
                showStatus("当前项目没有可用的项目根目录。");
                return;
            }
            if (scanResult.candidates().isEmpty()) {
                showStatus("没有待忽略的候选文件。");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this,
                    "确定忽略并归档 " + scanResult.candidates().size() + " 个候选文件吗？\n"
                            + "不会导入书签，也不会删除候选文件。",
                    "忽略候选书签", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.OK_OPTION) {
                return;
            }

            int archived = 0;
            List<String> errors = new ArrayList<>();
            for (BookmarkInbox.Candidate candidate : scanResult.candidates()) {
                try {
                    inbox.archive(projectRoot, candidate.file());
                    archived++;
                } catch (Exception exception) {
                    errors.add(candidate.fileName() + "：" + safeMessage(exception));
                }
            }
            String result = "已归档 " + archived + " 个候选文件";
            if (!errors.isEmpty()) {
                result += "；失败：" + String.join("；", errors);
            }
            refresh(result + "。");
        }

        private void addCurrentLine() {
            if (projectRoot == null) {
                showStatus("当前项目没有可用的项目根目录。");
                return;
            }
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (editor == null) {
                showStatus("请先打开一个代码文件并将光标放到目标行。");
                return;
            }
            VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
            if (file == null) {
                showStatus("无法确定当前编辑器文件。");
                return;
            }
            Path target;
            try {
                target = pathResolver.resolve(projectRoot, file.getPath());
            } catch (Exception exception) {
                showStatus("只能收藏当前项目目录内的文件：" + safeMessage(exception));
                return;
            }
            Document editorDocument = editor.getDocument();
            int lineIndex = editor.getCaretModel().getLogicalPosition().line;
            int start = editorDocument.getLineStartOffset(lineIndex);
            int end = editorDocument.getLineEndOffset(lineIndex);
            String expectedText = editorDocument.getText(new com.intellij.openapi.util.TextRange(start, end)).trim();
            String title = JOptionPane.showInputDialog(this, "书签标题：", "新增书签", JOptionPane.PLAIN_MESSAGE);
            if (title == null || title.isBlank()) {
                return;
            }
            String groupName = JOptionPane.showInputDialog(this, "分组名称：", "新增书签", JOptionPane.PLAIN_MESSAGE);
            if (groupName == null || groupName.isBlank()) {
                return;
            }
            String relativePath = projectRoot.relativize(target).toString().replace('\\', '/');
            Bookmark bookmark = new Bookmark(title.trim(), relativePath, lineIndex + 1, expectedText);
            List<BookmarkGroup> addition = List.of(new BookmarkGroup(groupName.trim(), List.of(bookmark)));
            persist(store.merge(document, new BookmarkDocument(1, addition)), "书签已添加");
        }

        private void deleteSelection() {
            TreePath selection = tree.getSelectionPath();
            if (selection == null || !(selection.getLastPathComponent() instanceof DefaultMutableTreeNode node)) {
                showStatus("请先选择要删除的书签或分组。");
                return;
            }
            Object selected = node.getUserObject();
            if (selected instanceof Bookmark bookmark) {
                DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
                BookmarkGroup group = (BookmarkGroup) parentNode.getUserObject();
                int confirm = JOptionPane.showConfirmDialog(this, "确定删除书签“" + bookmark.title() + "”吗？",
                        "删除书签", JOptionPane.OK_CANCEL_OPTION);
                if (confirm == JOptionPane.OK_OPTION) {
                    persist(store.removeBookmark(document, group.name(), bookmark), "书签已删除");
                }
            } else if (selected instanceof BookmarkGroup group) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "确定删除分组“" + group.name() + "”及其中 " + group.items().size() + " 条书签吗？",
                        "删除整个分组", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.OK_OPTION) {
                    persist(store.removeGroup(document, group.name()), "分组及其中书签已删除");
                }
            }
        }

        private void export() {
            FileSaverDescriptor descriptor = new FileSaverDescriptor("导出代码链路书签", "选择导出位置", "json");
            FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project);
            VirtualFileWrapper selected = dialog.save("flowmarks.json");
            if (selected == null) {
                return;
            }
            try {
                Path target = selected.getFile().toPath();
                store.save(target, document);
                showStatus("导出完成：" + target);
            } catch (Exception exception) {
                showStatus("导出失败：" + safeMessage(exception));
            }
        }

        private void persist(BookmarkDocument updated, String successMessage) {
            if (storePath == null) {
                showStatus("当前项目没有可用的项目根目录。");
                return;
            }
            try {
                store.save(storePath, updated);
                document = updated;
                populateTree();
                showStatus(successMessage + "，已保存到 " + storePath);
            } catch (Exception exception) {
                showStatus("保存失败，当前数据未应用：" + safeMessage(exception));
            }
        }

        private void populateTree() {
            rootNode.removeAllChildren();
            String query = searchField.getText().trim().toLowerCase(java.util.Locale.ROOT);
            for (BookmarkGroup group : document.groups()) {
                boolean groupMatches = group.name().toLowerCase(java.util.Locale.ROOT).contains(query);
                List<Bookmark> matching = new ArrayList<>();
                for (Bookmark bookmark : group.items()) {
                    if (query.isEmpty() || groupMatches || bookmark.title().toLowerCase(java.util.Locale.ROOT).contains(query)
                            || bookmark.filePath().toLowerCase(java.util.Locale.ROOT).contains(query)) {
                        matching.add(bookmark);
                    }
                }
                if (!matching.isEmpty() || (group.items().isEmpty() && groupMatches)) {
                    DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(group);
                    for (Bookmark bookmark : matching) {
                        groupNode.add(new DefaultMutableTreeNode(bookmark));
                    }
                    rootNode.add(groupNode);
                }
            }
            treeModel.reload();
            for (int index = 0; index < tree.getRowCount(); index++) {
                tree.expandRow(index);
            }
        }

        private void openSelectedBookmark() {
            TreePath selection = tree.getSelectionPath();
            if (selection == null || !(selection.getLastPathComponent() instanceof DefaultMutableTreeNode node)
                    || !(node.getUserObject() instanceof Bookmark bookmark)) {
                return;
            }
            if (projectRoot == null) {
                showStatus("当前项目没有可用的项目根目录。");
                return;
            }
            showStatus("正在打开：" + bookmark.title());
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    Path target = pathResolver.resolve(projectRoot, bookmark.filePath());
                    VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(target);
                    if (virtualFile == null) {
                        throw new IOException("IDEA 无法打开文件：" + bookmark.filePath());
                    }
                    List<String> lines = readLines(virtualFile, target);
                    int lineIndex = lineLocator.locate(bookmark, lines);
                    OpenFileDescriptor descriptor = new OpenFileDescriptor(project, virtualFile, lineIndex, 0);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        descriptor.navigate(true);
                        showStatus("已跳转：" + bookmark.title() + "（第 " + (lineIndex + 1) + " 行）");
                    });
                } catch (Exception exception) {
                    ApplicationManager.getApplication().invokeLater(
                            () -> showStatus("无法打开书签“" + bookmark.title() + "”：" + safeMessage(exception)));
                }
            });
        }

        private static List<String> readLines(VirtualFile virtualFile, Path target) throws IOException {
            List<String> editorLines = ApplicationManager.getApplication().runReadAction(
                    (Computable<List<String>>) () -> {
                Document targetDocument = FileDocumentManager.getInstance().getDocument(virtualFile);
                return targetDocument == null ? null : documentLines(targetDocument);
            });
            return editorLines == null ? Files.readAllLines(target, StandardCharsets.UTF_8) : editorLines;
        }

        private static List<String> documentLines(Document document) {
            List<String> lines = new ArrayList<>(document.getLineCount());
            for (int index = 0; index < document.getLineCount(); index++) {
                int start = document.getLineStartOffset(index);
                int end = document.getLineEndOffset(index);
                lines.add(document.getText(new com.intellij.openapi.util.TextRange(start, end)));
            }
            return lines;
        }

        private void showStatus(String text) {
            status.setText(text);
            status.setToolTipText(text);
        }

        private static int countBookmarks(BookmarkDocument document) {
            return document.groups().stream().mapToInt(group -> group.items().size()).sum();
        }

        private static String safeMessage(Exception exception) {
            String message = exception.getMessage();
            return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
        }
    }
}
