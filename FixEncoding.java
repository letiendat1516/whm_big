import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class FixEncoding {
    public static void main(String[] args) throws Exception {
        Path path = Paths.get("src/main/resources/public/dashboard.html");
        System.out.println("Reading: " + path.toAbsolutePath());

        byte[] raw = Files.readAllBytes(path);
        System.out.println("File size: " + raw.length);

        // Check for BOM
        int offset = 0;
        if (raw.length >= 3 && (raw[0] & 0xFF) == 0xEF && (raw[1] & 0xFF) == 0xBB && (raw[2] & 0xFF) == 0xBF) {
            offset = 3;
            System.out.println("BOM detected, skipping");
        }

        // Read as UTF-8
        String text = new String(raw, offset, raw.length - offset, StandardCharsets.UTF_8);
        System.out.println("Text length: " + text.length());

        // Show title line
        int ti = text.indexOf("<title>");
        int te = text.indexOf("</title>");
        if (ti >= 0 && te >= 0) {
            String titleContent = text.substring(ti + 7, te);
            System.out.println("Title content: " + titleContent);

            // Try round-trip through ISO-8859-1
            try {
                byte[] latin1Bytes = titleContent.getBytes("ISO-8859-1");
                String recovered = new String(latin1Bytes, StandardCharsets.UTF_8);
                System.out.println("Recovered title: " + recovered);
            } catch (Exception e) {
                System.out.println("Latin1 round-trip failed: " + e.getMessage());
            }
        }

        // Now fix the whole file: process line by line
        String[] lines = text.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        int fixedCount = 0;
        int failCount = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            try {
                byte[] b = line.getBytes("ISO-8859-1");
                String fixed = new String(b, StandardCharsets.UTF_8);
                sb.append(fixed);
                fixedCount++;
            } catch (Exception e) {
                // Line has chars outside latin-1, keep as-is
                sb.append(line);
                failCount++;
            }
            if (i < lines.length - 1) sb.append('\n');
        }

        System.out.println("Fixed lines: " + fixedCount + ", Kept as-is: " + failCount);

        // Write back as UTF-8 without BOM
        Files.write(path, sb.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println("File written successfully");

        // Verify
        String verify = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        int vti = verify.indexOf("<title>");
        int vte = verify.indexOf("</title>");
        if (vti >= 0 && vte >= 0) {
            System.out.println("Verified title: " + verify.substring(vti, vte + 8));
        }
    }
}

