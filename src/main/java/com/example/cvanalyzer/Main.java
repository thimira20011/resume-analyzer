package com.example.cvanalyzer;

import java.nio.file.*;
import java.sql.Connection;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        // configure DB connection
        String jdbcUrl = "jdbc:postgresql://localhost:5432/cvdb";
        String dbUser = "postgres";
        String dbPass = "80204885";

        // minimal skills lexicon - expand this file in production
        List<String> skills = Arrays.asList("java","python","sql","javascript","spring","hibernate","html","css","react","angular","aws","docker","kubernetes","machine learning","data analysis");

        ResumeParser parser = new ResumeParser(skills);

        try (DatabaseManager db = new DatabaseManager(jdbcUrl, dbUser, dbPass)) {
            Path dir = Paths.get("resumes"); // put .txt/.pdf resumes here
            if (!Files.exists(dir)) {
                System.out.println("Create a 'resumes' folder and drop sample resumes (.txt or .pdf) there.");
                return;
            }

            try (var stream = Files.list(dir)) {
                stream.filter(Files::isRegularFile).forEach(path -> {
                    try {
                        String text = parser.extractText(path);
                        ResumeParser.ParsedResume pr = parser.parse(text);
                        // insert
                        int cid = db.insertCandidate(
                                pr.fullName != null ? pr.fullName : path.getFileName().toString(),
                                pr.email, pr.phone, pr.totalExperienceYears, pr.summary
                        );
                        // skills
                        for (String sk : pr.skills) {
                            int sid = db.findOrCreateSkill(sk);
                            db.addCandidateSkill(cid, sid, null);
                        }
                        // education entries - saved as raw degree strings
                        for (String deg : pr.educationEntries) {
                            db.insertEducation(cid, deg, null, null, null);
                        }
                        db.commit();
                        System.out.println("Inserted candidate " + cid + " from " + path.getFileName());
                    } catch (Exception e) {
                        e.printStackTrace();
                        try { db.rollback(); } catch (Exception ignored) {}
                    }
                });
            }

            // Run analysis
            Connection conn = db.getConnection();
            Analyzer analyzer = new Analyzer(conn);
            System.out.println("Top skills: " + analyzer.topNSkills(10));
            System.out.println("Average experience (years): " + analyzer.averageExperience());

            // Example: rank candidates by job requirements
            Set<String> jobReq = new HashSet<>(Arrays.asList("java","spring","sql"));
            var ranked = analyzer.rankCandidatesByJobSkills(jobReq, 10);
            System.out.println("Ranked candidates for job req " + jobReq + ":");
            for (var r : ranked) System.out.println(r);

        }
    }
}
