package com.example.cvanalyzer;

import java.sql.*;
import java.util.*;

class DatabaseManager implements AutoCloseable {
    private final Connection conn;

    public DatabaseManager(String jdbcUrl, String user, String pass) throws SQLException {
        conn = DriverManager.getConnection(jdbcUrl, user, pass);
        conn.setAutoCommit(false);
    }

    // Save candidate and return generated id
    public int insertCandidate(String fullName, String email, String phone, double totalExp, String summary) throws SQLException {
        String sql = "INSERT INTO candidates(full_name, email, phone, total_experience_years, summary) VALUES (?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setDouble(4, totalExp);
            ps.setString(5, summary);
            ResultSet rs = ps.executeQuery();
            rs.next();
            int id = rs.getInt(1);
            return id;
        }
    }

    public int findOrCreateSkill(String skill) throws SQLException {
        String sel = "SELECT id FROM skills WHERE name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sel)) {
            ps.setString(1, skill);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        String ins = "INSERT INTO skills(name) VALUES(?) RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(ins)) {
            ps.setString(1, skill);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }

    public void addCandidateSkill(int candidateId, int skillId, Integer proficiency) throws SQLException {
        String sql = "INSERT INTO candidate_skills(candidate_id, skill_id, proficiency) VALUES (?, ?, ?) ON CONFLICT (candidate_id, skill_id) DO NOTHING";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, candidateId);
            ps.setInt(2, skillId);
            if (proficiency == null) ps.setNull(3, Types.INTEGER);
            else ps.setInt(3, proficiency);
            ps.executeUpdate();
        }
    }

    public void insertExperience(int candidateId, String title, String company, Integer startYear, Integer endYear, String desc) throws SQLException {
        String sql = "INSERT INTO experience(candidate_id, title, company, start_year, end_year, description) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, candidateId);
            ps.setString(2, title);
            ps.setString(3, company);
            if (startYear == null) ps.setNull(4, Types.INTEGER); else ps.setInt(4, startYear);
            if (endYear == null) ps.setNull(5, Types.INTEGER); else ps.setInt(5, endYear);
            ps.setString(6, desc);
            ps.executeUpdate();
        }
    }

    public void insertEducation(int candidateId, String degree, String institution, Integer startYear, Integer endYear) throws SQLException {
        String sql = "INSERT INTO education(candidate_id, degree, institution, start_year, end_year) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, candidateId);
            ps.setString(2, degree);
            ps.setString(3, institution);
            if (startYear == null) ps.setNull(4, Types.INTEGER); else ps.setInt(4, startYear);
            if (endYear == null) ps.setNull(5, Types.INTEGER); else ps.setInt(5, endYear);
            ps.executeUpdate();
        }
    }

    public void commit() throws SQLException {
        conn.commit();
    }

    public void rollback() {
        try { conn.rollback(); } catch (SQLException ignored) {}
    }

    public Connection getConnection() { return conn; }

    @Override
    public void close() throws Exception {
        conn.close();
    }
}
