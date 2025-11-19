package com.example.cvanalyzer;

import org.apache.log4j.Logger;

import java.io.InputStream;
import java.nio.file.*;
import java.sql.Connection;
import java.util.*;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        // configure DB connection
        Properties props = new Properties();
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                logger.error("Sorry, unable to find config.properties");
                return;
            }
            props.load(input);
        }

        String jdbcUrl = props.getProperty("db.url");
        String dbUser = props.getProperty("db.user");
        String dbPass = props.getProperty("db.password");

        // minimal skills lexicon - expand this file in production
        List<String> skills = Arrays.asList("java","python","sql","javascript","spring","hibernate","html","css","react","angular","aws","docker","kubernetes","machine learning","data analysis");

        ResumeParser parser = new ResumeParser(skills);

        try (DatabaseManager db = new DatabaseManager(jdbcUrl, dbUser, dbPass)) {
            Path dir = Paths.get("resumes"); // put .txt/.pdf resumes here
            if (!Files.exists(dir)) {
                logger.info("Create a 'resumes' folder and drop sample resumes (.txt or .pdf) there.");
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
                        logger.info("Inserted candidate " + cid + " from " + path.getFileName());
                    } catch (Exception e) {
                        logger.error("Error processing file: " + path, e);
                        try { db.rollback(); } catch (Exception ex) {
                            logger.error("Error rolling back transaction", ex);
                        }
                    }
                });
            }

            // Run analysis
            Connection conn = db.getConnection();
            Analyzer analyzer = new Analyzer(conn);
            logger.info("Top skills: " + analyzer.topNSkills(10));
            logger.info("Average experience (years): " + analyzer.averageExperience());

            // Example: rank candidates by job requirements
            Set<String> jobReq = new HashSet<>(Arrays.asList("java","spring","sql"));
            var ranked = analyzer.rankCandidatesByJobSkills(jobReq, 10);
            logger.info("Ranked candidates for job req " + jobReq + ":");
            for (var r : ranked) logger.info(r);

        }
    }
}
