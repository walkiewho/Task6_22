package UI;

import Task.Action;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

public class MapBenchmarkApp extends JFrame {

    enum MapKind {
        HASH_MAP("HashMap"),
        TREE_MAP("TreeMap"),
        LINKED_HASH_MAP("LinkedHashMap");

        final String title;

        MapKind(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    enum TestKind {
        PUT("PUT"),
        GET("GET"),
        REMOVE("REMOVE"),
        CONTAINS_KEY("CONTAINS_KEY"),
        ENTRY("ENTRY"),
        MIXED("MIXED");

        final String title;

        TestKind(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    static class Config {
        TestKind testKind;
        int initialSize;
        int sequenceLength;
        int keyLength;
        int warmupRuns;
        int measurementRuns;
        int hitRatioPercent;
        int putWeight;
        int getWeight;
        int removeWeight;
        int containsWeight;
        int entryWeight;
        long seed;
        EnumSet<MapKind> selectedMaps;
    }

    static class BenchmarkResult {
        final MapKind mapKind;
        final long minNs;
        final long maxNs;
        final long avgNs;
        final double avgMs;
        final double minMs;
        final double maxMs;

        BenchmarkResult(MapKind mapKind, long minNs, long maxNs, long avgNs) {
            this.mapKind = mapKind;
            this.minNs = minNs;
            this.maxNs = maxNs;
            this.avgNs = avgNs;
            this.avgMs = avgNs / 1_000_000.0;
            this.minMs = minNs / 1_000_000.0;
            this.maxMs = maxNs / 1_000_000.0;
        }
    }

    private final JComboBox<TestKind> testKindBox = new JComboBox<>(TestKind.values());

    private final JCheckBox hashCheck = new JCheckBox("HashMap", true);
    private final JCheckBox treeCheck = new JCheckBox("TreeMap", true);
    private final JCheckBox linkedCheck = new JCheckBox("LinkedHashMap", true);

    private final JSpinner initialSizeSpinner = new JSpinner(new SpinnerNumberModel(100_000, 0, Integer.MAX_VALUE, 1_000));
    private final JSpinner sequenceLengthSpinner = new JSpinner(new SpinnerNumberModel(100_000, 1, Integer.MAX_VALUE, 1_000));
    private final JSpinner keyLengthSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 256, 1));
    private final JSpinner warmupSpinner = new JSpinner(new SpinnerNumberModel(2, 0, 100, 1));
    private final JSpinner measurementSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
    private final JSpinner hitRatioSpinner = new JSpinner(new SpinnerNumberModel(100, 0, 100, 5));

    private final JSpinner putWeightSpinner = new JSpinner(new SpinnerNumberModel(25, 0, 100, 1));
    private final JSpinner getWeightSpinner = new JSpinner(new SpinnerNumberModel(25, 0, 100, 1));
    private final JSpinner removeWeightSpinner = new JSpinner(new SpinnerNumberModel(25, 0, 100, 1));
    private final JSpinner containsWeightSpinner = new JSpinner(new SpinnerNumberModel(15, 0, 100, 1));
    private final JSpinner entryWeightSpinner = new JSpinner(new SpinnerNumberModel(10, 0, 100, 1));

    private final JTextField seedField = new JTextField("42", 12);

    private final JButton runButton = new JButton("Run benchmark");
    private final JButton stopButton = new JButton("Stop");
    private final JTextArea logArea = new JTextArea(8, 80);

    private final BenchmarkTableModel tableModel = new BenchmarkTableModel();
    private final ResultsChartPanel chartPanel = new ResultsChartPanel();

    private final JPanel optionsCard = new JPanel(new CardLayout());

    public MapBenchmarkApp() {
        super("Map benchmark");

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 800));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(root);

        JPanel controls = buildControlsPanel();
        controls.setPreferredSize(new Dimension(380, 700));
        root.add(controls, BorderLayout.WEST);

        JTable table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JScrollPane tableScroll = new JScrollPane(table);

        JPanel right = new JPanel(new BorderLayout(10, 10));
        right.add(tableScroll, BorderLayout.CENTER);
        right.add(chartPanel, BorderLayout.SOUTH);

        root.add(right, BorderLayout.CENTER);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setPreferredSize(new Dimension(900, 160));
        root.add(logScroll, BorderLayout.SOUTH);

        runButton.addActionListener(e -> startBenchmark());
        stopButton.addActionListener(e -> stopCurrentBenchmark());
        stopButton.setEnabled(false);

        testKindBox.addActionListener(e -> updateVisibleOptions());
        updateVisibleOptions();
    }

    private JPanel buildControlsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(section("General"));
        panel.add(row("Test type", testKindBox));
        panel.add(row("Initial size", initialSizeSpinner));
        panel.add(row("Sequence length", sequenceLengthSpinner));
        panel.add(row("Key length", keyLengthSpinner));
        panel.add(row("Warm-up runs", warmupSpinner));
        panel.add(row("Measured runs", measurementSpinner));
        panel.add(row("Seed", seedField));

        JPanel mapsPanel = new JPanel();
        mapsPanel.setLayout(new BoxLayout(mapsPanel, BoxLayout.Y_AXIS));
        mapsPanel.add(hashCheck);
        mapsPanel.add(treeCheck);
        mapsPanel.add(linkedCheck);
        panel.add(section("Maps"));
        panel.add(mapsPanel);

        panel.add(section("GET / REMOVE / CONTAINS"));
        panel.add(row("Hit ratio %", hitRatioSpinner));

        JPanel mixed = new JPanel(new java.awt.GridLayout(5, 2, 6, 6));
        mixed.add(new JLabel("PUT weight"));
        mixed.add(putWeightSpinner);
        mixed.add(new JLabel("GET weight"));
        mixed.add(getWeightSpinner);
        mixed.add(new JLabel("REMOVE weight"));
        mixed.add(removeWeightSpinner);
        mixed.add(new JLabel("CONTAINS weight"));
        mixed.add(containsWeightSpinner);
        mixed.add(new JLabel("ENTRY weight"));
        mixed.add(entryWeightSpinner);

        optionsCard.add(mixed, "MIXED");
        optionsCard.add(new JPanel(), "SIMPLE");
        panel.add(section("Mixed weights"));
        panel.add(optionsCard);

        JPanel buttons = new JPanel(new java.awt.GridLayout(1, 2, 8, 8));
        buttons.add(runButton);
        buttons.add(stopButton);
        panel.add(Box.createVerticalStrut(8));
        panel.add(buttons);

        return panel;
    }

    private JPanel section(String title) {
        JPanel p = new JPanel(new BorderLayout());
        JLabel label = new JLabel(title);
        label.setBorder(new EmptyBorder(8, 0, 4, 0));
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        p.add(label, BorderLayout.NORTH);
        return p;
    }

    private JPanel row(String label, java.awt.Component component) {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.add(new JLabel(label), BorderLayout.WEST);
        p.add(component, BorderLayout.CENTER);
        return p;
    }

    private void updateVisibleOptions() {
        TestKind kind = (TestKind) testKindBox.getSelectedItem();
        if (kind == TestKind.MIXED) {
            ((CardLayout) optionsCard.getLayout()).show(optionsCard, "MIXED");
        } else {
            ((CardLayout) optionsCard.getLayout()).show(optionsCard, "SIMPLE");
        }

        boolean keyBased = kind == TestKind.GET || kind == TestKind.REMOVE || kind == TestKind.CONTAINS_KEY || kind == TestKind.MIXED;
        hitRatioSpinner.setEnabled(keyBased);

        repaint();
    }

    private void startBenchmark() {
        Config config;
        try {
            config = readConfig();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid parameters", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (config.selectedMaps.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select at least one map.", "Invalid parameters", JOptionPane.ERROR_MESSAGE);
            return;
        }

        runButton.setEnabled(false);
        stopButton.setEnabled(true);
        tableModel.setResults(List.of());
        chartPanel.setResults(List.of());
        logArea.setText("");

        BenchmarkWorker worker = new BenchmarkWorker(config);
        worker.execute();
        currentWorker = worker;
    }

    private void stopCurrentBenchmark() {
        if (currentWorker != null) {
            currentWorker.cancel(true);
            appendLog("Benchmark cancelled.");
        }
    }

    private Config readConfig() {
        Config c = new Config();
        c.testKind = (TestKind) testKindBox.getSelectedItem();
        c.initialSize = ((Number) initialSizeSpinner.getValue()).intValue();
        c.sequenceLength = ((Number) sequenceLengthSpinner.getValue()).intValue();
        c.keyLength = ((Number) keyLengthSpinner.getValue()).intValue();
        c.warmupRuns = ((Number) warmupSpinner.getValue()).intValue();
        c.measurementRuns = ((Number) measurementSpinner.getValue()).intValue();
        c.hitRatioPercent = ((Number) hitRatioSpinner.getValue()).intValue();
        c.putWeight = ((Number) putWeightSpinner.getValue()).intValue();
        c.getWeight = ((Number) getWeightSpinner.getValue()).intValue();
        c.removeWeight = ((Number) removeWeightSpinner.getValue()).intValue();
        c.containsWeight = ((Number) containsWeightSpinner.getValue()).intValue();
        c.entryWeight = ((Number) entryWeightSpinner.getValue()).intValue();

        String seedText = seedField.getText().trim();
        try {
            c.seed = Long.parseLong(seedText);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Seed must be a valid long integer.");
        }

        EnumSet<MapKind> selected = EnumSet.noneOf(MapKind.class);
        if (hashCheck.isSelected()) selected.add(MapKind.HASH_MAP);
        if (treeCheck.isSelected()) selected.add(MapKind.TREE_MAP);
        if (linkedCheck.isSelected()) selected.add(MapKind.LINKED_HASH_MAP);
        c.selectedMaps = selected;

        if (c.sequenceLength <= 0) throw new IllegalArgumentException("Sequence length must be > 0.");
        if (c.keyLength <= 0) throw new IllegalArgumentException("Key length must be > 0.");
        if (c.warmupRuns < 0) throw new IllegalArgumentException("Warm-up runs cannot be negative.");
        if (c.measurementRuns <= 0) throw new IllegalArgumentException("Measured runs must be > 0.");

        if (c.testKind == TestKind.MIXED) {
            int sum = c.putWeight + c.getWeight + c.removeWeight + c.containsWeight + c.entryWeight;
            if (sum <= 0) throw new IllegalArgumentException("At least one mixed weight must be > 0.");
        }

        return c;
    }

    private BenchmarkResult benchmarkOne(MapKind kind, Config c) {
        SplittableRandom rng = new SplittableRandom(c.seed ^ (kind.ordinal() * 0x9E3779B97F4A7C15L));

        List<String> baseKeys = generateBaseKeys(c.initialSize, c.keyLength, rng);
        List<Task.Action> sequence = buildSequence(c, baseKeys, rng);

        // Warm-up
        for (int i = 0; i < c.warmupRuns; i++) {
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
            Map<String, Integer> map = createAndFillMap(kind, baseKeys, rng);
            executeSequence(map, sequence);
        }

        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        long sum = 0;

        for (int i = 0; i < c.measurementRuns; i++) {
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
            Map<String, Integer> map = createAndFillMap(kind, baseKeys, rng);
            long start = System.nanoTime();
            executeSequence(map, sequence);
            long elapsed = System.nanoTime() - start;

            min = Math.min(min, elapsed);
            max = Math.max(max, elapsed);
            sum += elapsed;
        }

        long avg = sum / c.measurementRuns;
        return new BenchmarkResult(kind, min, max, avg);
    }

    private Map<String, Integer> createAndFillMap(MapKind kind, List<String> baseKeys, SplittableRandom rng) {
        Map<String, Integer> map;
        switch (kind) {
            case HASH_MAP -> map = new java.util.HashMap<>();
            case TREE_MAP -> map = new java.util.TreeMap<>();
            case LINKED_HASH_MAP -> map = new LinkedHashMap<>();
            default -> throw new IllegalStateException("Unexpected value: " + kind);
        }

        for (String key : baseKeys) {
            map.put(key, rng.nextInt(-100_000, 100_001));
        }
        return map;
    }

    private void executeSequence(Map<String, Integer> map, List<Task.Action> sequence) {
        for (Task.Action action : sequence) {
            action.doAction(map);
        }
    }

    private List<Task.Action> buildSequence(Config c, List<String> existingKeys, SplittableRandom rng) {
        ArrayList<Task.Action> result = new ArrayList<>(c.sequenceLength);

        for (int i = 0; i < c.sequenceLength; i++) {
            switch (c.testKind) {
                case PUT -> result.add(new Task.Action(Task.Action.actionTypes.PUT,
                        makeKey("put", i, c.keyLength, rng), rng.nextInt(-100_000, 100_001)));

                case GET -> result.add(new Task.Action(Task.Action.actionTypes.GET,
                        chooseKey(existingKeys, c.hitRatioPercent, "get", i, c.keyLength, rng), null));

                case REMOVE -> result.add(new Task.Action(Task.Action.actionTypes.REMOVE,
                        chooseKey(existingKeys, c.hitRatioPercent, "rem", i, c.keyLength, rng), null));

                case CONTAINS_KEY -> result.add(new Task.Action(Task.Action.actionTypes.CONTAINS_KEY,
                        chooseKey(existingKeys, c.hitRatioPercent, "chk", i, c.keyLength, rng), null));

                case ENTRY -> result.add(new Task.Action(Task.Action.actionTypes.ENTRY, null, null));

                case MIXED -> {
                    Task.Action.actionTypes type = chooseMixedType(c, rng);
                    switch (type) {
                        case PUT -> result.add(new Task.Action(type,
                                makeKey("mixput", i, c.keyLength, rng), rng.nextInt(-100_000, 100_001)));
                        case GET, REMOVE, CONTAINS_KEY -> result.add(new Task.Action(type,
                                chooseKey(existingKeys, c.hitRatioPercent, "mix", i, c.keyLength, rng), null));
                        case ENTRY -> result.add(new Task.Action(type, null, null));
                    }
                }
            }
        }

        return result;
    }

    private Task.Action.actionTypes chooseMixedType(Config c, SplittableRandom rng) {
        int total = c.putWeight + c.getWeight + c.removeWeight + c.containsWeight + c.entryWeight;
        int roll = rng.nextInt(total);

        if (roll < c.putWeight) return Task.Action.actionTypes.PUT;
        roll -= c.putWeight;

        if (roll < c.getWeight) return Task.Action.actionTypes.GET;
        roll -= c.getWeight;

        if (roll < c.removeWeight) return Task.Action.actionTypes.REMOVE;
        roll -= c.removeWeight;

        if (roll < c.containsWeight) return Task.Action.actionTypes.CONTAINS_KEY;

        return Action.actionTypes.ENTRY;
    }

    private String chooseKey(List<String> existingKeys, int hitRatioPercent, String prefix, int index, int keyLength, SplittableRandom rng) {
        if (!existingKeys.isEmpty() && rng.nextInt(100) < hitRatioPercent) {
            return existingKeys.get(rng.nextInt(existingKeys.size()));
        }
        return makeKey(prefix, index, keyLength, rng);
    }

    private List<String> generateBaseKeys(int count, int keyLength, SplittableRandom rng) {
        ArrayList<String> keys = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            keys.add(makeKey("base", i, keyLength, rng));
        }
        return keys;
    }

    private String makeKey(String prefix, int index, int keyLength, SplittableRandom rng) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(Math.max(keyLength + 12, prefix.length() + 12));
        sb.append(prefix).append('_').append(index).append('_');
        while (sb.length() < keyLength) {
            sb.append(alphabet.charAt(rng.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    private void appendLog(String text) {
        logArea.append(text + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private BenchmarkWorker currentWorker;

    private class BenchmarkWorker extends SwingWorker<List<BenchmarkResult>, String> {
        private final Config config;

        BenchmarkWorker(Config config) {
            this.config = config;
        }

        @Override
        protected List<BenchmarkResult> doInBackground() {
            ArrayList<BenchmarkResult> results = new ArrayList<>();

            for (MapKind kind : config.selectedMaps) {
                if (isCancelled()) break;
                publish("Running " + kind + "...");
                BenchmarkResult r = benchmarkOne(kind, config);
                if (r != null) {
                    results.add(r);
                }
            }

            return results;
        }

        @Override
        protected void process(List<String> chunks) {
            for (String s : chunks) {
                appendLog(s);
            }
        }

        @Override
        protected void done() {
            runButton.setEnabled(true);
            stopButton.setEnabled(false);

            try {
                List<BenchmarkResult> results = get();
                tableModel.setResults(results);
                chartPanel.setResults(results);

                if (!isCancelled()) {
                    appendLog("Done.");
                    for (BenchmarkResult r : results) {
                        appendLog(r.mapKind + ": avg=" + String.format("%.3f", r.avgMs) + " ms, "
                                + "min=" + String.format("%.3f", r.minMs) + " ms, "
                                + "max=" + String.format("%.3f", r.maxMs) + " ms");
                    }
                }
            } catch (Exception e) {
                appendLog("Error: " + e.getMessage());
                JOptionPane.showMessageDialog(MapBenchmarkApp.this,
                        "Benchmark failed: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    static class BenchmarkTableModel extends AbstractTableModel {
        private final String[] columns = {"Map", "Average ms", "Min ms", "Max ms"};
        private List<BenchmarkResult> results = List.of();

        public void setResults(List<BenchmarkResult> results) {
            this.results = results == null ? List.of() : List.copyOf(results);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return results.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            BenchmarkResult r = results.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> r.mapKind.toString();
                case 1 -> String.format("%.3f", r.avgMs);
                case 2 -> String.format("%.3f", r.minMs);
                case 3 -> String.format("%.3f", r.maxMs);
                default -> "";
            };
        }
    }

    static class ResultsChartPanel extends JPanel {
        private List<BenchmarkResult> results = List.of();

        ResultsChartPanel() {
            setPreferredSize(new Dimension(1000, 260));
            setBorder(BorderFactory.createTitledBorder("Average time (ms)"));
        }

        void setResults(List<BenchmarkResult> results) {
            this.results = results == null ? List.of() : List.copyOf(results);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (results.isEmpty()) {
                g.drawString("No results yet.", 20, 30);
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            int left = 50;
            int top = 30;
            int bottom = 45;
            int right = 20;

            int chartW = w - left - right;
            int chartH = h - top - bottom;

            double max = 0.0;
            for (BenchmarkResult r : results) {
                max = Math.max(max, r.avgMs);
            }
            if (max <= 0.0) max = 1.0;

            int barGap = 16;
            int barWidth = Math.max(40, (chartW - (results.size() - 1) * barGap) / results.size());

            int x = left;
            for (BenchmarkResult r : results) {
                int barH = (int) Math.round((r.avgMs / max) * chartH);
                int y = top + chartH - barH;

                g2.drawRect(x, top, barWidth, chartH);
                g2.fillRect(x + 1, y, barWidth - 1, barH);

                String label = r.mapKind.toString();
                String value = String.format("%.2f", r.avgMs);
                int labelWidth = g2.getFontMetrics().stringWidth(label);
                int valueWidth = g2.getFontMetrics().stringWidth(value);

                g2.drawString(label, x + (barWidth - labelWidth) / 2, h - 20);
                g2.drawString(value, x + (barWidth - valueWidth) / 2, y - 6);

                x += barWidth + barGap;
            }

            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MapBenchmarkApp().setVisible(true));
    }
}