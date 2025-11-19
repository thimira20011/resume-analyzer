package com.example.cvanalyzer;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class ResumeParser {
    private final Set<String> skillsLexicon;

    public ResumeParser(Collection<String> skillsLexicon) {
        this.skillsLexicon = new HashSet<>();
        for (String s : skillsLexicon) this.skillsLexicon.add(s.trim().toLowerCase());
    }

    public String extractText(Path file) throws IOException {
        String name = file.toString().toLowerCase();
        if (name.endsWith(".pdf")) {
            try (PDDocument doc = PDDocument.load(Files.newInputStream(file))) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(doc);
            }
        } else {
            return Files.readString(file);
        }
    }

    public ParsedResume parse(String text) {
        ParsedResume p = new ParsedResume();
        if (text == null) return p;
        // Normalize
        String norm = text.replaceAll("\r\n", "\n");

        // Name heuristic: first line up to 4 words that contains letters and capitalized words
        String[] lines = norm.split("\n");
        for (String l : lines) {
            String trimmed = l.trim();
            if (trimmed.length() > 2 && trimmed.length() < 120 && trimmed.split("\\s+").length <= 4) {
                // avoid lines that look like 'Resume' or 'Curriculum Vitae'
                String low = trimmed.toLowerCase();
                if (!low.contains("resume") && !low.contains("curriculum") && !low.contains("cv")) {
                    p.fullName = trimmed;
                    break;
                }
            }
        }

        // email
        Matcher m = Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}").matcher(text);
        if (m.find()) p.email = m.group();

        // phone (very simple)
        m = Pattern.compile("(\\+?\\d{2,3}[\\s-]?)?(\\(?\\d{2,4}\\)?[\\s-]?)?\\d{3,4}[\\s-]?\\d{3,4}").matcher(text);
        if (m.find()) p.phone = m.group().trim();

        // skills: use word boundaries to avoid matching substrings
        String lowered = text.toLowerCase();
        for (String sk : skillsLexicon) {
            Pattern skillPattern = Pattern.compile("\\b" + Pattern.quote(sk) + "\\b");
            if (skillPattern.matcher(lowered).find()) {
                p.skills.add(sk);
            }
        }

        // experience estimation: find patterns like "X years", "X+ years"
        m = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*\\+?\\s*(years|yrs)\\b", Pattern.CASE_INSENSITIVE).matcher(text);
        double maxYears = 0.0;
        while (m.find()) {
            try {
                double val = Double.parseDouble(m.group(1));
                if (val > maxYears) maxYears = val;
            } catch (NumberFormatException ignored) {}
        }
        p.totalExperienceYears = maxYears;

        // Education heuristics (look for Degree keywords)
        Pattern degPat = Pattern.compile("(Bachelor|B\\.Sc|BSc|Master|M\\.Sc|MSc|PhD|Doctor)\\b[\\w\\s,.-]{0,80}", Pattern.CASE_INSENSITIVE);
        m = degPat.matcher(text);
        while (m.find()) {
            p.educationEntries.add(m.group().trim());
        }

        // summary: first 400 chars
        p.summary = trimmedSafe(norm).substring(0, Math.min(400, norm.length()));

        return p;
    }

    private String trimmedSafe(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }

    public static class ParsedResume {
        public String fullName = null;
        public String email = null;
        public String phone = null;
        public Set<String> skills = new HashSet<>();
        public double totalExperienceYears = 0.0;
        public String summary = "";
        public List<String> educationEntries = new ArrayList<>();

        @Override
        public String toString() {
            return "ParsedResume{" +
                    "fullName='" + fullName + '\'' +
                    ", email='" + email + '\'' +
                    ", phone='" + phone + '\'' +
                    ", skills=" + skills +
                    ", totalExperienceYears=" + totalExperienceYears +
                    ", education=" + educationEntries +
                    '}';
        }
    }
}
