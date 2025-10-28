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
        // fetch all candidates + their skills
        String sql = "SELECT c.id, c.full_name, s.name FROM candidates c " +
                "LEFT JOIN candidate_skills cs ON c.id = cs.candidate_id " +
                "LEFT JOIN skills s ON cs.skill_id = s.id " +
                "ORDER BY c.id";
        Map<Integer, CandidateScore> map = new LinkedHashMap<>();
        try (Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                int id = rs.getInt(1);
                String name = rs.getString(2);
                String skill = rs.getString(3);
                CandidateScore cs = map.computeIfAbsent(id, k -> new CandidateScore(id, name));
                if (skill != null) cs.skills.add(skill.toLowerCase());
            }
        }
        // score
        List<CandidateScore> list = new ArrayList<>(map.values());
        for (CandidateScore c : list) {
            int matches = 0;
            for (String r : requiredSkills) if (c.skills.contains(r.toLowerCase())) matches++;
            c.score = matches; // crude; could use weights
        }
        list.sort(Comparator.comparingInt((CandidateScore c) -> c.score).reversed());
        return list.subList(0, Math.min(limit, list.size()));
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
