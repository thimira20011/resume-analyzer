package com.example.cvanalyzer;

import java.sql.*;
import java.util.*;

public class Analyzer {
    private final Connection conn;

    public Analyzer(Connection conn) {
        this.conn = conn;
    }

    // Top N skills by count
    public List<Map.Entry<String, Integer>> topNSkills(int n) throws SQLException {
        String sql = "SELECT s.name, COUNT(*) AS cnt " +
                "FROM candidate_skills cs JOIN skills s ON cs.skill_id = s.id " +
                "GROUP BY s.name ORDER BY cnt DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, n);
            ResultSet rs = ps.executeQuery();
            List<Map.Entry<String,Integer>> res = new ArrayList<>();
            while (rs.next()) {
                res.add(new AbstractMap.SimpleEntry<>(rs.getString(1), rs.getInt(2)));
            }
            return res;
        }
    }

    // Average experience across candidates
    public double averageExperience() throws SQLException {
        String sql = "SELECT AVG(total_experience_years) FROM candidates";
        try (Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            if (rs.next()) return rs.getDouble(1);
            return 0.0;
        }
    }

    // Simple match scoring: count overlapping skills between job required skill set and candidate
    public List<CandidateScore> rankCandidatesByJobSkills(Set<String> requiredSkills, int limit) throws SQLException {
        if (requiredSkills == null || requiredSkills.isEmpty()) {
            return new ArrayList<>();
        }

        String placeholders = String.join(",", Collections.nCopies(requiredSkills.size(), "?"));
        String sql = "SELECT c.id, c.full_name, COUNT(s.id) AS skill_matches " +
                "FROM candidates c " +
                "JOIN candidate_skills cs ON c.id = cs.candidate_id " +
                "JOIN skills s ON cs.skill_id = s.id " +
                "WHERE s.name IN (" + placeholders + ") " +
                "GROUP BY c.id, c.full_name " +
                "ORDER BY skill_matches DESC " +
                "LIMIT ?";

        List<CandidateScore> rankedCandidates = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            for (String skill : requiredSkills) {
                ps.setString(i++, skill.toLowerCase());
            }
            ps.setInt(i, limit);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("full_name");
                int score = rs.getInt("skill_matches");
                CandidateScore candidateScore = new CandidateScore(id, name);
                candidateScore.score = score;
                // Note: individual skills are not loaded in this efficient version
                rankedCandidates.add(candidateScore);
            }
        }
        return rankedCandidates;
    }

    public static class CandidateScore {
        public int id;
        public String name;
        public Set<String> skills = new HashSet<>();
        public int score;

        public CandidateScore(int id, String name) { this.id = id; this.name = name; }

        @Override
        public String toString() {
            return "CandidateScore{" + "id=" + id + ", name='" + name + '\'' + ", score=" + score + ", skills=" + skills + '}';
        }
    }
}
