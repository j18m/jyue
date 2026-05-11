package com.xia.yue.ui;

import burp.api.montoya.ui.UserInterface;
import com.xia.yue.burp.ResultSink;
import com.xia.yue.burp.ScanConfig;
import com.xia.yue.burp.ScanResult;
import com.xia.yue.core.DedupStore;
import com.xia.yue.core.DomainWhitelist;
import com.xia.yue.core.FindingType;
import com.xia.yue.core.HeaderRules;
import com.xia.yue.compat.MontoyaCompat;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.RowFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class XiaYuePanel extends JPanel implements ResultSink {
    private static final int EXPORT_MESSAGE_LIMIT = 12000;
    private static final Pattern CHARSET_PATTERN = Pattern.compile("(?i)charset\\s*=\\s*\"?([^;\\s\"]+)");
    private static final DateTimeFormatter EXPORT_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private final JCheckBox enabled = new JCheckBox("启动插件");
    private final JCheckBox whitelistEnabled = new JCheckBox("启动白名单");
    private final JTextField whitelist = new JTextField();
    private final JTextArea lowPrivilegeHeaders = new JTextArea("Cookie: JSESSIONID=test;UUID=1; userid=admin\nAuthorization: Bearer test");
    private final JTextArea unauthorizedRemovedHeaders = new JTextArea("Cookie\nAuthorization\nToken");
    private final ResultTableModel tableModel = new ResultTableModel();
    private final JTable table = new JTable(tableModel);
    private final TableRowSorter<ResultTableModel> sorter = new TableRowSorter<>(tableModel);
    private final JTextField filterText = new JTextField();
    private final JComboBox<String> filterType = new JComboBox<>(filterOptions());
    private final MessagePairView originalViewer;
    private final MessagePairView lowPrivilegeViewer;
    private final MessagePairView unauthorizedViewer;
    private final DedupStore dedupStore;

    public XiaYuePanel(UserInterface userInterface, DedupStore dedupStore) {
        super(new BorderLayout());
        this.dedupStore = dedupStore;
        this.originalViewer = new MessagePairView(userInterface);
        this.lowPrivilegeViewer = new MessagePairView(userInterface);
        this.unauthorizedViewer = new MessagePairView(userInterface);
        build();
    }

    public ScanConfig currentConfig() {
        return new ScanConfig(
                enabled.isSelected(),
                whitelistEnabled.isSelected(),
                DomainWhitelist.parse(whitelist.getText()),
                HeaderRules.parseAuthHeaders(lowPrivilegeHeaders.getText()),
                HeaderRules.parseRemovedHeaderNames(unauthorizedRemovedHeaders.getText())
        );
    }

    @Override
    public void accept(ScanResult result) {
        SwingUtilities.invokeLater(() -> {
            long selectedId = selectedResult() == null ? Long.MIN_VALUE : selectedResult().id();
            tableModel.upsert(result);
            if (selectedId == result.id()) {
                showSelectedResult();
            }
        });
    }

    @Override
    public void error(String message, Throwable throwable) {
        SwingUtilities.invokeLater(() -> {
            ScanResult result = new ScanResult(
                    -1,
                    com.xia.yue.core.FindingType.FAILED,
                    message + ": " + throwable.getMessage(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            tableModel.upsert(result);
        });
    }

    private void build() {
        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                showSelectedResult();
            }
        });
        table.setComponentPopupMenu(buildTableMenu());

        JSplitPane horizontal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildMainPanel(), buildSettingsPanel());
        horizontal.setResizeWeight(0.68);
        add(horizontal, BorderLayout.CENTER);
    }

    private JPanel buildMainPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(buildFilterBar(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("原始数据包", originalViewer);
        tabs.addTab("低权限数据包", lowPrivilegeViewer);
        tabs.addTab("未授权数据包", unauthorizedViewer);
        tabs.setPreferredSize(new Dimension(800, 330));

        JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(table), tabs);
        vertical.setResizeWeight(0.58);
        panel.add(vertical, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(360, 700));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.weightx = 1;

        int row = 0;
        add(panel, c, row++, new JLabel("插件名：jyue"));
        add(panel, c, row++, new JLabel("备注：xia_yue"));
        add(panel, c, row++, new JLabel("版本：jyue V1.4"));
        add(panel, c, row++, enabled);

        JButton clear = new JButton("清空列表");
        clear.addActionListener(event -> {
            tableModel.clear();
            dedupStore.clear();
            originalViewer.clear();
            lowPrivilegeViewer.clear();
            unauthorizedViewer.clear();
        });
        add(panel, c, row++, clear);

        add(panel, c, row++, new JLabel("白名单支持域名/IP/URL/*.域名，多个请用逗号、空格或换行分隔"));
        add(panel, c, row++, whitelist);
        whitelistEnabled.addItemListener(event -> {
            boolean locked = whitelistEnabled.isSelected();
            whitelist.setEnabled(!locked);
        });
        add(panel, c, row++, whitelistEnabled);

        add(panel, c, row++, new JLabel("越权：填写低权限认证信息，将会替换或新增"));
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 0.42;
        add(panel, c, row++, new JScrollPane(lowPrivilegeHeaders));

        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(panel, c, row++, new JLabel("未授权：将移除下列头部认证信息，不区分大小写"));

        c.fill = GridBagConstraints.BOTH;
        c.weighty = 0.42;
        add(panel, c, row, new JScrollPane(unauthorizedRemovedHeaders));
        return panel;
    }

    private static void add(JPanel panel, GridBagConstraints c, int row, java.awt.Component component) {
        c.gridy = row;
        panel.add(component, c);
    }

    private JPanel buildFilterBar() {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.add(new JLabel("筛选"), BorderLayout.WEST);
        panel.add(filterText, BorderLayout.CENTER);

        JPanel actions = new JPanel(new BorderLayout(6, 0));
        actions.add(filterType, BorderLayout.CENTER);
        JButton export = new JButton("导出当前表格");
        export.addActionListener(event -> exportCurrentTable());
        actions.add(export, BorderLayout.EAST);
        panel.add(actions, BorderLayout.EAST);

        filterText.getDocument().addDocumentListener((SimpleDocumentListener) this::applyFilter);
        filterType.addActionListener(event -> applyFilter());
        return panel;
    }

    private JPopupMenu buildTableMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem copyUrl = new JMenuItem("复制 URL");
        copyUrl.addActionListener(event -> copyToClipboard(selectedResult().url()));
        menu.add(copyUrl);

        JMenu requestMenu = new JMenu("复制请求包");
        requestMenu.add(copyItem("原始请求包", result -> requestText(result.originalMessage())));
        requestMenu.add(copyItem("低权限请求包", result -> requestText(result.lowPrivilegeMessage())));
        requestMenu.add(copyItem("未授权请求包", result -> requestText(result.unauthorizedMessage())));
        menu.add(requestMenu);

        JMenu responseMenu = new JMenu("复制响应包");
        responseMenu.add(copyItem("原始响应包", result -> responseText(result.originalMessage())));
        responseMenu.add(copyItem("低权限响应包", result -> responseText(result.lowPrivilegeMessage())));
        responseMenu.add(copyItem("未授权响应包", result -> responseText(result.unauthorizedMessage())));
        menu.add(responseMenu);
        return menu;
    }

    private JMenuItem copyItem(String label, ResultTextExtractor extractor) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(event -> copyToClipboard(extractor.extract(selectedResult())));
        return item;
    }

    private void applyFilter() {
        String text = filterText.getText().trim().toLowerCase(Locale.ROOT);
        String type = filterType.getSelectedItem().toString();
        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends ResultTableModel, ? extends Integer> entry) {
                ScanResult result = tableModel.get(entry.getIdentifier());
                boolean matchesType = "全部类型".equals(type) || result.findingType().label().equals(type);
                boolean matchesText = text.isEmpty()
                        || result.url().toLowerCase(Locale.ROOT).contains(text)
                        || result.findingType().label().toLowerCase(Locale.ROOT).contains(text);
                return matchesType && matchesText;
            }
        });
    }

    private void showSelectedResult() {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            return;
        }
        ScanResult result = tableModel.get(table.convertRowIndexToModel(selected));
        originalViewer.setMessage(result.originalMessage());
        lowPrivilegeViewer.setMessage(result.lowPrivilegeMessage());
        unauthorizedViewer.setMessage(result.unauthorizedMessage());
    }

    private ScanResult selectedResult() {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            return null;
        }
        return tableModel.get(table.convertRowIndexToModel(selected));
    }

    private void exportCurrentTable() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("jyue_" + LocalDateTime.now().format(EXPORT_TIMESTAMP) + ".csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        List<String> lines = new ArrayList<>();
        lines.add(String.join(",",
                csv("#"),
                csv("类型"),
                csv("URL"),
                csv("原始包长度"),
                csv("低权限包长度差值"),
                csv("未授权包长度差值"),
                csv("原始请求"),
                csv("原始响应"),
                csv("低权限请求"),
                csv("低权限响应"),
                csv("未授权请求"),
                csv("未授权响应")
        ));
        for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
            ScanResult result = tableModel.get(table.convertRowIndexToModel(viewRow));
            lines.add(String.join(",",
                    csv(String.valueOf(result.id())),
                    csv(result.findingType().label()),
                    csv(result.url()),
                    csv(length(result.originalSummary())),
                    csv(deltaLength(result.originalSummary(), result.lowPrivilegeSummary())),
                    csv(deltaLength(result.originalSummary(), result.unauthorizedSummary())),
                    csv(exportRequestText(result.originalMessage())),
                    csv(exportResponseText(result.originalMessage())),
                    csv(exportRequestText(result.lowPrivilegeMessage())),
                    csv(exportResponseText(result.lowPrivilegeMessage())),
                    csv(exportRequestText(result.unauthorizedMessage())),
                    csv(exportResponseText(result.unauthorizedMessage()))
            ));
        }

        try (Writer writer = new OutputStreamWriter(new java.io.FileOutputStream(chooser.getSelectedFile()), StandardCharsets.UTF_8)) {
            writer.write('\ufeff');
            for (String line : lines) {
                writer.write(line);
                writer.write(System.lineSeparator());
            }
        } catch (IOException e) {
            error("导出失败", e);
        }
    }

    private static String[] filterOptions() {
        String[] options = new String[FindingType.values().length + 1];
        options[0] = "全部类型";
        for (int i = 0; i < FindingType.values().length; i++) {
            options[i + 1] = FindingType.values()[i].label();
        }
        return options;
    }

    private static void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text == null ? "" : text), null);
    }

    private static String requestText(com.xia.yue.burp.CapturedExchange message) {
        return message == null || message.request() == null ? "" : decodeMessage(message.request());
    }

    private static String responseText(com.xia.yue.burp.CapturedExchange message) {
        return message == null || message.response() == null ? "" : decodeMessage(message.response());
    }

    private static String exportRequestText(com.xia.yue.burp.CapturedExchange message) {
        return truncateForExport(requestText(message));
    }

    private static String exportResponseText(com.xia.yue.burp.CapturedExchange message) {
        return truncateForExport(responseText(message));
    }

    private static String decodeMessage(burp.api.montoya.http.message.HttpMessage message) {
        byte[] bytes = message.toByteArray().getBytes();
        Charset charset = charsetFromContentType(MontoyaCompat.headerValue(message, "Content-Type"));
        return new String(bytes, charset);
    }

    private static Charset charsetFromContentType(String contentType) {
        if (contentType != null) {
            Matcher matcher = CHARSET_PATTERN.matcher(contentType);
            if (matcher.find()) {
                try {
                    return Charset.forName(matcher.group(1).trim());
                } catch (IllegalArgumentException ignored) {
                    return StandardCharsets.UTF_8;
                }
            }
        }
        return StandardCharsets.UTF_8;
    }

    private static String truncateForExport(String value) {
        if (value == null || value.length() <= EXPORT_MESSAGE_LIMIT) {
            return value;
        }
        return value.substring(0, EXPORT_MESSAGE_LIMIT)
                + "\n\n[导出已截断，原始长度 "
                + value.length()
                + " 字符；右键复制请求/响应可获取完整内容]";
    }

    private static String length(com.xia.yue.core.ResponseSummary summary) {
        return summary == null ? "" : String.valueOf(summary.length());
    }

    private static String deltaLength(
            com.xia.yue.core.ResponseSummary original,
            com.xia.yue.core.ResponseSummary compared
    ) {
        if (original == null || compared == null) {
            return "";
        }
        int delta = compared.length() - original.length();
        return delta > 0 ? "+" + delta : String.valueOf(delta);
    }

    private static String csv(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    @FunctionalInterface
    private interface ResultTextExtractor {
        String extract(ScanResult result);
    }

    @FunctionalInterface
    private interface SimpleDocumentListener extends javax.swing.event.DocumentListener {
        void update();

        @Override
        default void insertUpdate(javax.swing.event.DocumentEvent e) {
            update();
        }

        @Override
        default void removeUpdate(javax.swing.event.DocumentEvent e) {
            update();
        }

        @Override
        default void changedUpdate(javax.swing.event.DocumentEvent e) {
            update();
        }
    }

    private static final class ResultTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"#", "类型", "URL", "原始包长度", "低权限长度差", "未授权长度差"};
        private final List<ScanResult> results = new ArrayList<>();

        void upsert(ScanResult result) {
            for (int row = 0; row < results.size(); row++) {
                if (results.get(row).id() == result.id()) {
                    results.set(row, result);
                    fireTableRowsUpdated(row, row);
                    return;
                }
            }
            results.add(0, result);
            fireTableRowsInserted(0, 0);
        }

        void clear() {
            results.clear();
            fireTableDataChanged();
        }

        ScanResult get(int row) {
            return results.get(row);
        }

        @Override
        public int getRowCount() {
            return results.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ScanResult result = results.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> result.id();
                case 1 -> result.findingType().label();
                case 2 -> result.url();
                case 3 -> length(result.originalSummary());
                case 4 -> deltaLength(result.originalSummary(), result.lowPrivilegeSummary());
                case 5 -> deltaLength(result.originalSummary(), result.unauthorizedSummary());
                default -> "";
            };
        }
    }
}
