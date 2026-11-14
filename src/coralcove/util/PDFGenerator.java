package coralcove.util;

import java.awt.Font;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes a simple text PDF and sends the same lines to the Java print dialog.
 * Helvetica has no rupee glyph, so PDF lines should already use {@code Rs.}.
 */
public final class PDFGenerator {

    private PDFGenerator() {
    }

    public static void write(Path file, String title, List<String> lines) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.write(file, build(title, lines));
        } catch (IOException ex) {
            throw new AppException("Could not write PDF: " + ex.getMessage(), ex);
        }
    }

    public static void print(String title, List<String> lines) {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName(title);
        List<String> body = new ArrayList<>();
        body.add(title);
        body.add("");
        body.addAll(lines);
        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            int perPage = 42;
            int from = pageIndex * perPage;
            if (from >= body.size()) {
                return Printable.NO_SUCH_PAGE;
            }
            graphics.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
            int y = (int) pageFormat.getImageableY() + 20;
            int x = (int) pageFormat.getImageableX() + 12;
            int end = Math.min(body.size(), from + perPage);
            for (int i = from; i < end; i++) {
                graphics.drawString(body.get(i), x, y);
                y += 14;
            }
            return Printable.PAGE_EXISTS;
        });
        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException ex) {
                throw new AppException("Printing failed: " + ex.getMessage(), ex);
            }
        }
    }

    static byte[] build(String title, List<String> lines) {
        List<String> all = new ArrayList<>();
        all.add(title);
        all.add("");
        all.addAll(lines);
        int perPage = 46;
        int pages = Math.max(1, (all.size() + perPage - 1) / perPage);
        StringBuilder objects = new StringBuilder();
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);
        StringBuilder content = new StringBuilder();
        content.append("%PDF-1.4\n");
        int next = 1;
        int catalogId = next++;
        int pagesId = next++;
        int fontId = next++;
        int[] pageIds = new int[pages];
        int[] contentIds = new int[pages];
        for (int p = 0; p < pages; p++) {
            pageIds[p] = next++;
            contentIds[p] = next++;
        }
        addObject(content, offsets, catalogId, "<< /Type /Catalog /Pages " + pagesId + " 0 R >>");
        StringBuilder kids = new StringBuilder("<< /Type /Pages /Count " + pages + " /Kids [");
        for (int id : pageIds) {
            kids.append(id).append(" 0 R ");
        }
        kids.append("] >>");
        addObject(content, offsets, pagesId, kids.toString());
        addObject(content, offsets, fontId, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");
        for (int p = 0; p < pages; p++) {
            String stream = pageStream(all, p * perPage, Math.min(all.size(), (p + 1) * perPage));
            addObject(content, offsets, pageIds[p],
                    "<< /Type /Page /Parent " + pagesId + " 0 R /MediaBox [0 0 595 842] /Contents "
                            + contentIds[p] + " 0 R /Resources << /Font << /F1 " + fontId + " 0 R >> >> >>");
            addObject(content, offsets, contentIds[p],
                    "<< /Length " + stream.getBytes(StandardCharsets.US_ASCII).length + " >>\nstream\n"
                            + stream + "endstream");
        }
        int xref = content.length();
        content.append("xref\n0 ").append(next).append('\n');
        content.append("0000000000 65535 f \n");
        for (int i = 1; i < next; i++) {
            content.append(String.format("%010d 00000 n \n", offsets.get(i)));
        }
        content.append("trailer << /Size ").append(next).append(" /Root ").append(catalogId).append(" 0 R >>\n");
        content.append("startxref\n").append(xref).append("\n%%EOF\n");
        objects.append(content);
        return objects.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private static void addObject(StringBuilder content, List<Integer> offsets, int id, String body) {
        while (offsets.size() <= id) {
            offsets.add(0);
        }
        offsets.set(id, content.length());
        content.append(id).append(" 0 obj\n").append(body).append("\nendobj\n");
    }

    private static String pageStream(List<String> lines, int from, int to) {
        StringBuilder stream = new StringBuilder();
        stream.append("BT\n/F1 11 Tf\n50 800 Td\n");
        for (int i = from; i < to; i++) {
            stream.append('(').append(escape(lines.get(i))).append(") Tj\n");
            if (i + 1 < to) {
                stream.append("0 -16 Td\n");
            }
        }
        stream.append("ET\n");
        return stream.toString();
    }

    private static String escape(String value) {
        StringBuilder out = new StringBuilder();
        String text = value == null ? "" : value;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' || c == '(' || c == ')') {
                out.append('\\').append(c);
            } else if (c >= 32 && c <= 126) {
                out.append(c);
            } else {
                out.append(' ');
            }
        }
        return out.toString();
    }
}
