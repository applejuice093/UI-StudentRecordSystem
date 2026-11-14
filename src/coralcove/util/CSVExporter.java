package coralcove.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class CSVExporter {

    private CSVExporter() {
    }

    public static void write(Path file, List<String> columns, List<List<String>> rows) {
        StringBuilder out = new StringBuilder();
        out.append(line(columns)).append('\n');
        for (List<String> row : rows) {
            out.append(line(row)).append('\n');
        }
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new AppException("Could not write " + file.getFileName() + ": " + ex.getMessage(), ex);
        }
    }

    private static String line(List<String> cells) {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                row.append(',');
            }
            row.append(escape(cells.get(i)));
        }
        return row.toString();
    }

    private static String escape(String value) {
        String text = value == null ? "" : value;
        boolean quote = text.contains(",") || text.contains("\"") || text.contains("\n");
        text = text.replace("\"", "\"\"");
        return quote ? "\"" + text + "\"" : text;
    }
}
