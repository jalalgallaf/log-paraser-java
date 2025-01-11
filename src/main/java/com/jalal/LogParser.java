package com.jalal;
import java.io.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.JTextArea;

public class LogParser {
    public static class SearchGroup {
        private final String text;
        private final String operation;
        private final boolean caseSensitive;
        private final boolean useRegex;
        private Pattern pattern;

        public SearchGroup(String text, String operation, boolean caseSensitive, boolean useRegex) throws PatternSyntaxException {
            this.text = text;
            this.operation = operation;
            this.caseSensitive = caseSensitive;
            this.useRegex = useRegex;
            if (useRegex) {
                int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
                this.pattern = Pattern.compile(text, flags);
            }
        }

        public String getText() {
            return text;
        }

        public String getOperation() {
            return operation;
        }

        public boolean isCaseSensitive() {
            return caseSensitive;
        }

        public boolean isUseRegex() {
            return useRegex;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(operation).append(" '").append(text).append("'");
            if (useRegex) sb.append(" (regex)");
            if (caseSensitive) sb.append(" (case sensitive)");
            return sb.toString();
        }
    }

    public void searchInLogFile(String filePath, List<SearchGroup> searchGroups, JTextArea resultArea) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 0;
            int matchCount = 0;
            
            // Write header to result area
            StringBuilder results = new StringBuilder();
            results.append("Search Results for expression:\n");
            for (int i = 0; i < searchGroups.size(); i++) {
                if (i > 0) {
                    results.append(" ");
                }
                results.append(searchGroups.get(i).toString());
            }
            results.append("\n\n");
            results.append("Source Log File: ").append(filePath).append("\n");
            results.append("Search Time: ").append(LocalDateTime.now()).append("\n\n");
            results.append("Matches Found:\n");
            results.append("-------------------\n");
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (matchesCriteria(line, searchGroups)) {
                    matchCount++;
                    results.append("Line ").append(lineNumber).append(": ").append(line).append("\n");
                }
            }
            
            // Write summary
            results.append("\n-------------------\n");
            results.append("Total Matches Found: ").append(matchCount);
            
            resultArea.setText(results.toString());
            
        } catch (FileNotFoundException e) {
            resultArea.setText("Error: File not found - " + filePath);
        } catch (IOException e) {
            resultArea.setText("Error reading the file: " + e.getMessage());
        }
    }

    private boolean matchesCriteria(String line, List<SearchGroup> searchGroups) {
        if (searchGroups.isEmpty()) {
            return true;
        }

        SearchGroup firstGroup = searchGroups.get(0);
        boolean result = containsText(line, firstGroup);

        for (int i = 1; i < searchGroups.size(); i++) {
            SearchGroup group = searchGroups.get(i);
            boolean matches = containsText(line, group);
            
            if (group.getOperation().equals("AND")) {
                result = result && matches;
            } else { // OR
                result = result || matches;
            }
        }

        return result;
    }

    private boolean containsText(String line, SearchGroup group) {
        if (group.isUseRegex()) {
            return group.pattern.matcher(line).find();
        } else if (group.isCaseSensitive()) {
            return line.contains(group.getText());
        } else {
            return line.toLowerCase().contains(group.getText().toLowerCase());
        }
    }

    public void exportResults(String outputPath, String content) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            writer.print(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
