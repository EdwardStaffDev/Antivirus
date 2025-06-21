import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Timer;

public class AdvancedAntivirusScanner extends JFrame {
    private JTextArea logArea;
    private JProgressBar progressBar;
    private JButton scanButton;
    private JButton updateButton;
    private JButton cleanButton;
    private JComboBox<String> scanTypeCombo;
    private JComboBox<String> antivirusBaseCombo;
    private JLabel statusLabel;
    private JLabel lastUpdateLabel;
    private JCheckBox[] baseCheckboxes;

    private Map<String, List<String>> antivirusBases = new HashMap<>();
    private List<String> detectedThreats = new ArrayList<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    private String lastUpdateDate = "Никогда";

    public AdvancedAntivirusScanner() {
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("Antivirus");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initVirusBases();
        initUI();
    }

    private void initUI() {
        // Main panel with border layout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top panel with controls
        JPanel topPanel = new JPanel(new GridLayout(2, 1, 5, 5));

        // Scan controls panel
        JPanel scanControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        scanControlPanel.setBorder(new TitledBorder("Управление сканированием"));

        scanTypeCombo = new JComboBox<>(new String[]{"Быстрая проверка", "Полная проверка", "Выборочная проверка"});
        antivirusBaseCombo = new JComboBox<>(new String[]{"Kaspersky", "Dr.Web", "ESET NOD32", "Avast", "Bitdefender", "Norton"});
        scanButton = new JButton("Сканировать");
        cleanButton = new JButton("Очистить угрозы");
        cleanButton.setEnabled(false);

        scanControlPanel.add(new JLabel("Тип сканирования:"));
        scanControlPanel.add(scanTypeCombo);
        scanControlPanel.add(new JLabel("База для сканирования:"));
        scanControlPanel.add(antivirusBaseCombo);
        scanControlPanel.add(scanButton);
        scanControlPanel.add(cleanButton);

        // Update controls panel
        JPanel updateControlPanel = new JPanel(new GridLayout(1, 3, 10, 5));
        updateControlPanel.setBorder(new TitledBorder("Управление базами"));

        JPanel baseSelectionPanel = new JPanel(new GridLayout(2, 3, 5, 5));
        baseSelectionPanel.setBorder(new TitledBorder("Выбор баз для обновления"));
        String[] baseNames = {"Kaspersky", "Dr.Web", "ESET NOD32", "Avast", "Bitdefender", "Norton"};
        baseCheckboxes = new JCheckBox[baseNames.length];
        for (int i = 0; i < baseNames.length; i++) {
            baseCheckboxes[i] = new JCheckBox(baseNames[i], true);
            baseSelectionPanel.add(baseCheckboxes[i]);
        }

        updateButton = new JButton("Обновить выбранные базы");
        JPanel updateStatusPanel = new JPanel(new BorderLayout());
        lastUpdateLabel = new JLabel("Последнее обновление: " + lastUpdateDate);
        statusLabel = new JLabel("Готов к работе");
        statusLabel.setForeground(new Color(0, 100, 0));

        updateStatusPanel.add(lastUpdateLabel, BorderLayout.NORTH);
        updateStatusPanel.add(statusLabel, BorderLayout.CENTER);

        updateControlPanel.add(baseSelectionPanel);
        updateControlPanel.add(updateButton);
        updateControlPanel.add(updateStatusPanel);

        topPanel.add(scanControlPanel);
        topPanel.add(updateControlPanel);

        // Log area with scroll
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(new TitledBorder("Журнал сканирования"));

        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Add components to main panel
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(logScroll, BorderLayout.CENTER);
        mainPanel.add(progressBar, BorderLayout.SOUTH);

        // Event handlers
        scanButton.addActionListener(e -> startScan());
        cleanButton.addActionListener(e -> cleanThreats());
        updateButton.addActionListener(e -> updateSelectedBases());

        add(mainPanel);
    }

    private void initVirusBases() {
        // Initialize with some demo signatures for each antivirus base
        antivirusBases.put("Kaspersky", Arrays.asList(
                "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*",
                "evil_code_kaspersky",
                "trojan_horse_kaspersky_db",
                "kaspersky_ransomware_pattern"
        ));

        antivirusBases.put("Dr.Web", Arrays.asList(
                "drweb_malware_signature",
                "drweb_virus_pattern_2023",
                "drweb_trojan_signature",
                "drweb_spyware_code"
        ));

        antivirusBases.put("ESET NOD32", Arrays.asList(
                "eset_virus_signature",
                "eset_malware_pattern",
                "eset_threat_detection",
                "eset_heuristic_match"
        ));

        antivirusBases.put("Avast", Arrays.asList(
                "avast_virus_db_entry",
                "avast_malware_signature",
                "avast_threat_detection",
                "avast_suspicious_pattern"
        ));

        antivirusBases.put("Bitdefender", Arrays.asList(
                "bitdefender_virus_sig",
                "bitdefender_malware_code",
                "bitdefender_threat_pattern",
                "bitdefender_ransomware_sig"
        ));

        antivirusBases.put("Norton", Arrays.asList(
                "norton_antivirus_sig",
                "norton_malware_pattern",
                "norton_threat_detection",
                "norton_virus_db_entry"
        ));
    }

    private void updateSelectedBases() {
        new Thread(() -> {
            statusLabel.setText("Обновление баз...");
            statusLabel.setForeground(Color.BLUE);

            boolean atLeastOneSelected = false;
            for (int i = 0; i < baseCheckboxes.length; i++) {
                if (baseCheckboxes[i].isSelected()) {
                    atLeastOneSelected = true;
                    String baseName = baseCheckboxes[i].getText();
                    updateAntivirusBase(baseName);
                }
            }

            if (!atLeastOneSelected) {
                logArea.append("Не выбрано ни одной базы для обновления!\n");
                statusLabel.setText("Готов к работе");
                statusLabel.setForeground(new Color(0, 100, 0));
                return;
            }

            lastUpdateDate = dateFormat.format(new Date());
            lastUpdateLabel.setText("Последнее обновление: " + lastUpdateDate);
            statusLabel.setText("Готов к работе");
            statusLabel.setForeground(new Color(0, 100, 0));

            JOptionPane.showMessageDialog(this,
                    "Базы успешно обновлены!",
                    "Обновление завершено",
                    JOptionPane.INFORMATION_MESSAGE);
        }).start();
    }

    private void updateAntivirusBase(String baseName) {
        logArea.append("Начато обновление базы: " + baseName + "\n");

        // Simulate connecting to update server
        simulateWork(1000, "Подключение к серверу " + baseName + "...", 10);

        // Simulate downloading updates
        simulateWork(1500, "Загрузка обновлений для " + baseName + "...", 30);

        // Simulate installing updates
        simulateWork(1000, "Установка обновлений для " + baseName + "...", 60);

        // Simulate updating signatures
        simulateWork(800, "Обновление сигнатур для " + baseName + "...", 90);

        // Add some new signatures
        List<String> base = antivirusBases.get(baseName);
        base.add(baseName.toLowerCase() + "_new_virus_" + new Random().nextInt(1000));
        base.add(baseName.toLowerCase() + "_emergency_update_" + System.currentTimeMillis());

        logArea.append("База " + baseName + " успешно обновлена!\n");
        progressBar.setValue(0);
    }

    private void startScan() {
        detectedThreats.clear();
        cleanButton.setEnabled(false);

        new Thread(() -> {
            statusLabel.setText("Сканирование...");
            statusLabel.setForeground(Color.BLUE);

            String scanType = (String) scanTypeCombo.getSelectedItem();
            String antivirusBase = (String) antivirusBaseCombo.getSelectedItem();

            logArea.append("\n=== НАЧАЛО СКАНИРОВАНИЯ ===\n");
            logArea.append("Тип: " + scanType + "\n");
            logArea.append("Антивирусная база: " + antivirusBase + "\n");
            logArea.append("Время начала: " + dateFormat.format(new Date()) + "\n\n");

            List<String> signatures = antivirusBases.get(antivirusBase);

            if (scanType.equals("Быстрая проверка")) {
                scanCriticalLocations(signatures);
            } else if (scanType.equals("Полная проверка")) {
                scanFullSystem(signatures);
            } else {
                // Custom scan - let user select directories
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setMultiSelectionEnabled(true);

                if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    File[] selectedDirs = chooser.getSelectedFiles();
                    for (File dir : selectedDirs) {
                        scanDirectory(dir, signatures, 0);
                    }
                }
            }

            logArea.append("\n=== СКАНИРОВАНИЕ ЗАВЕРШЕНО ===\n");
            logArea.append("Найдено угроз: " + detectedThreats.size() + "\n");
            logArea.append("Время окончания: " + dateFormat.format(new Date()) + "\n\n");

            if (!detectedThreats.isEmpty()) {
                cleanButton.setEnabled(true);
                JOptionPane.showMessageDialog(this,
                        "Найдено угроз: " + detectedThreats.size() + "\n" +
                                "Нажмите кнопку 'Очистить угрозы' для удаления.",
                        "Результаты сканирования",
                        JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Угроз не обнаружено!",
                        "Результаты сканирования",
                        JOptionPane.INFORMATION_MESSAGE);
            }

            statusLabel.setText("Готов к работе");
            statusLabel.setForeground(new Color(0, 100, 0));
        }).start();
    }

    private void scanCriticalLocations(List<String> signatures) {
        String[] locations = {
                "C:\\Windows\\System32",
                "C:\\Program Files",
                "C:\\Users\\Public",
                "C:\\Temp",
                "C:\\Documents and Settings",
                "C:\\ProgramData"
        };

        for (int i = 0; i < locations.length; i++) {
            progressBar.setValue((i + 1) * 100 / locations.length);
            simulateFileScan(locations[i], signatures);
        }
    }

    private void scanFullSystem(List<String> signatures) {
        File[] roots = File.listRoots();
        int totalRoots = roots.length;

        for (int i = 0; i < totalRoots; i++) {
            File root = roots[i];
            logArea.append("Сканирование диска: " + root.getPath() + "\n");
            progressBar.setValue((i + 1) * 100 / totalRoots);

            // For demo purposes, limit depth
            scanDirectory(root, signatures, 0);
        }
    }

    private void scanDirectory(File dir, List<String> signatures, int depth) {
        if (depth > 3) return; // Limit depth for demo

        simulateWork(50, "Сканирование папки: " + dir.getPath(), -1);

        // Randomly detect threats based on signatures
        for (String signature : signatures) {
            if (new Random().nextInt(20) == 0) {
                String threatPath = dir.getPath() + "\\infected_file_" +
                        System.currentTimeMillis() + ".exe";
                String threatName = "Virus." + signature.substring(0, 3) +
                        new Random().nextInt(1000);

                logArea.append("[УГРОЗА] Обнаружено: " + threatName +
                        " в " + threatPath + "\n");
                detectedThreats.add(threatPath);
            }
        }

        // Simulate recursive scanning
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    scanDirectory(file, signatures, depth + 1);
                } else {
                    simulateFileScan(file.getPath(), signatures);
                }
            }
        }
    }

    private void simulateFileScan(String filePath, List<String> signatures) {
        simulateWork(20, "Проверка файла: " + filePath, -1);

        // Check for virus signatures in filename (for demo)
        for (String signature : signatures) {
            if (filePath.contains(signature)) {
                String threatName = "Virus." + signature.substring(0, 3) +
                        new Random().nextInt(1000);
                logArea.append("[УГРОЗА] Обнаружено: " + threatName +
                        " в " + filePath + "\n");
                detectedThreats.add(filePath);
                return;
            }
        }

        // Random threat detection
        if (new Random().nextInt(100) == 0) {
            String threatName = "Virus." + (char)('A' + new Random().nextInt(26)) +
                    new Random().nextInt(1000);
            logArea.append("[УГРОЗА] Обнаружено: " + threatName +
                    " в " + filePath + "\n");
            detectedThreats.add(filePath);
        }
    }

    private void cleanThreats() {
        new Thread(() -> {
            statusLabel.setText("Удаление угроз...");
            statusLabel.setForeground(Color.RED);

            int cleaned = 0;
            int failed = 0;

            logArea.append("\n=== НАЧАЛО ОЧИСТКИ ===\n");

            for (String threat : detectedThreats) {
                simulateWork(100, "Обработка угрозы: " + threat, -1);

                // Simulate 90% success rate for cleaning
                if (new Random().nextInt(10) < 9) {
                    logArea.append("[УСПЕХ] Угроза удалена: " + threat + "\n");
                    cleaned++;
                } else {
                    logArea.append("[ОШИБКА] Не удалось удалить: " + threat + "\n");
                    failed++;
                }
            }

            logArea.append("\n=== ОЧИСТКА ЗАВЕРШЕНА ===\n");
            logArea.append("Успешно удалено: " + cleaned + "\n");
            logArea.append("Не удалось удалить: " + failed + "\n\n");

            if (failed > 0) {
                JOptionPane.showMessageDialog(this,
                        "Некоторые угрозы не были удалены!\n" +
                                "Рекомендуется выполнить сканирование еще раз.",
                        "Результаты очистки",
                        JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Все угрозы успешно удалены!",
                        "Результаты очистки",
                        JOptionPane.INFORMATION_MESSAGE);
            }

            detectedThreats.clear();
            cleanButton.setEnabled(false);
            statusLabel.setText("Готов к работе");
            statusLabel.setForeground(new Color(0, 100, 0));
        }).start();
    }

    private void simulateWork(int millis, String message, int progress) {
        if (message != null) {
            logArea.append(message + "\n");
        }

        if (progress >= 0) {
            progressBar.setValue(progress);
        }

        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AdvancedAntivirusScanner scanner = new AdvancedAntivirusScanner();
            scanner.setVisible(true);
        });
    }
}