package com.ibooks;

import com.ibooks.epub.EpubGenerator;
import com.ibooks.epub.SimpleMarkdown;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class EpubGeneratorTest {
    @Test
    public void writesZipWithMimetypeAndOpf() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String xhtml = SimpleMarkdown.toXhtml("# Hello\n\nA chapter.");
        EpubGenerator.write(
                out,
                "Test Book",
                "Jane Doe",
                "en",
                null,
                List.of(new EpubGenerator.ChapterSource("Hello", xhtml))
        );
        Set<String> names = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                names.add(entry.getName());
            }
        }
        assertTrue("mimetype", names.contains("mimetype") || names.stream().anyMatch(n -> n.endsWith("mimetype")));
        assertTrue("container", names.stream().anyMatch(n -> n.endsWith("container.xml")));
        assertTrue("chapter", names.stream().anyMatch(n -> n.contains("chapter")));
        assertTrue("not empty", out.size() > 100);
    }
}
