package com.jalal;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LogParserUI extends JFrame {
    private JTextField filePathField;
    private JTextArea resultArea;
    private JButton browseButton;
    private JButton searchButton;
    private JButton exportButton;
    private JButton addGroupButton;
    private JPanel searchGroupsPanel;
    private List<LogParser.SearchGroup> searchGroups;
    private LogParser logParser;

    public LogParserUI() {
        logParser = new LogParser();
        searchGroups = new ArrayList<>();
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Log Parser");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new BorderLayout(5, 5));

        searchGroupsPanel = new JPanel();
        searchGroupsPanel.setLayout(new BoxLayout(searchGroupsPanel, BoxLayout.Y_AXIS));
        searchGroupsPanel.setBorder(BorderFactory.createTitledBorder("Search Criteria"));

        addGroupButton = new JButton("Add Search Group");
        JPanel addGroupPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addGroupPanel.add(addGroupButton);

        JPanel searchPanel = new JPanel(new BorderLayout());
        JScrollPane searchScroll = new JScrollPane(searchGroupsPanel);
        searchScroll.setPreferredSize(new Dimension(0, 150));
        searchPanel.add(searchScroll, BorderLayout.CENTER);
        searchPanel.add(addGroupPanel, BorderLayout.SOUTH);

        JPanel filePanel = new JPanel(new BorderLayout(5, 5));
        filePanel.setBorder(new EmptyBorder(5, 0, 5, 0));
        
        filePathField = new JTextField();
        browseButton = new JButton("Browse");
        
        JPanel fileLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fileLabelPanel.add(new JLabel("Log File:"));
        
        filePanel.add(fileLabelPanel, BorderLayout.WEST);
        filePanel.add(filePathField, BorderLayout.CENTER);
        filePanel.add(browseButton, BorderLayout.EAST);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchButton = new JButton("Search");
        exportButton = new JButton("Export Results");
        exportButton.setEnabled(false);
        buttonPanel.add(searchButton);
        buttonPanel.add(exportButton);

        topPanel.add(searchPanel, BorderLayout.CENTER);
        topPanel.add(filePanel, BorderLayout.SOUTH);
        topPanel.add(buttonPanel, BorderLayout.NORTH);

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel);

        // Add event listeners
        setupEventListeners();
    }

    private void addSearchGroupToPanel(LogParser.SearchGroup group, int index) {
        JPanel groupPanel = new JPanel(new BorderLayout(5, 5));
        groupPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(2, 2, 2, 2),
            BorderFactory.createEtchedBorder()
        ));

        JLabel groupLabel = new JLabel(group.toString());
        groupLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        
        JButton editButton = new JButton("Edit");
        editButton.addActionListener(e -> {
            SearchGroupDialog dialog = new SearchGroupDialog(this, group);
            dialog.setVisible(true);

            if (dialog.isApproved()) {
                LogParser.SearchGroup editedGroup = new LogParser.SearchGroup(
                    dialog.getSearchText(),
                    dialog.getOperation(),
                    dialog.isCaseSensitive(),
                    dialog.isUseRegex()
                );
                searchGroups.set(index, editedGroup);
                refreshSearchGroups();
            }
        });
        
        JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(e -> {
            searchGroups.remove(index);
            refreshSearchGroups();
            updateSearchButtonState();
        });

        buttonPanel.add(editButton);
        buttonPanel.add(removeButton);

        groupPanel.add(groupLabel, BorderLayout.CENTER);
        groupPanel.add(buttonPanel, BorderLayout.EAST);

        searchGroupsPanel.add(groupPanel);
    }

    private void refreshSearchGroups() {
        searchGroupsPanel.removeAll();
        for (int i = 0; i < searchGroups.size(); i++) {
            addSearchGroupToPanel(searchGroups.get(i), i);
        }
        searchGroupsPanel.revalidate();
        searchGroupsPanel.repaint();
    }

    private void updateSearchButtonState() {
        searchButton.setEnabled(!searchGroups.isEmpty());
    }

    private void setupEventListeners() {
        addGroupButton.addActionListener(e -> {
            SearchGroupDialog dialog = new SearchGroupDialog(this);
            dialog.setVisible(true);

            if (dialog.isApproved()) {
                LogParser.SearchGroup group = new LogParser.SearchGroup(
                    dialog.getSearchText(),
                    dialog.getOperation(),
                    dialog.isCaseSensitive(),
                    dialog.isUseRegex()
                );
                searchGroups.add(group);
                refreshSearchGroups();
                updateSearchButtonState();
            }
        });

        browseButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                filePathField.setText(selectedFile.getAbsolutePath());
            }
        });

        searchButton.addActionListener(e -> {
            String filePath = filePathField.getText().trim();

            if (filePath.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select a log file.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (searchGroups.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please add at least one search group.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Perform search in a background thread
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    searchButton.setEnabled(false);
                    exportButton.setEnabled(false);
                    resultArea.setText("Searching...\n");
                    logParser.searchInLogFile(filePath, searchGroups, resultArea);
                    return null;
                }

                @Override
                protected void done() {
                    searchButton.setEnabled(true);
                    exportButton.setEnabled(true);
                }
            }.execute();
        });

        exportButton.addActionListener(e -> {
            if (resultArea.getText().isEmpty() || resultArea.getText().equals("Searching...\n")) {
                JOptionPane.showMessageDialog(this, "No results to export.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            JFileChooser fileChooser = new JFileChooser();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String defaultFileName = "search_results_" + timestamp + ".txt";
            fileChooser.setSelectedFile(new File(defaultFileName));
            
            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                logParser.exportResults(selectedFile.getAbsolutePath(), resultArea.getText());
            }
        });

        // Initial button state
        updateSearchButtonState();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            LogParserUI ui = new LogParserUI();
            ui.setVisible(true);
        });
    }
}