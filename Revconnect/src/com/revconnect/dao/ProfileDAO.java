package com.revconnect.dao;

import com.revconnect.config.DBConnection;
import com.revconnect.model.Profile;
import com.revconnect.exception.DatabaseException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProfileDAO {

    /**
     * Fetches profile details using USER_ID.
     */
    public Profile getProfileByUserId(int userId) {
        String sql = "SELECT * FROM Profiles WHERE user_id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                Profile p = new Profile();
                p.setUserId(rs.getInt("user_id"));
                p.setUsername(rs.getString("username")); // Matches your column
                p.setBio(rs.getString("bio"));
                p.setLocation(rs.getString("location"));
                p.setWebsite(rs.getString("website"));
                p.setPicPath(rs.getString("pic_path"));
                return p;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching profile", e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return null;
    }

    /**
     * Updates a specific profile column (Name, Bio, etc.)
     */
    public boolean updateProfileField(int userId, String column, String value) {
        // Warning: Direct string concatenation for column names is used here for flexibility.
        // Ensure 'column' variable comes from a safe internal switch-case.
        String sql = "UPDATE Profiles SET " + column + " = ? WHERE user_id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, value);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Update failed for " + column, e);
        } finally {
            closeResources(conn, pstmt, null);
        }
    }

    /**
     * Searches profiles by username by joining with the USERS table.
     */
    public List<Profile> searchByUsername(String username) {
        List<Profile> list = new ArrayList<Profile>(); // Old style diamond for 1.7
        String sql = "SELECT p.* FROM Profiles p JOIN Users u ON p.user_id = u.user_id " +
                     "WHERE LOWER(u.username) LIKE LOWER(?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, "%" + username + "%");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Profile p = new Profile();
                p.setUserId(rs.getInt("user_id"));
                p.setUsername(rs.getString("username"));
                list.add(p);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Search failed", e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return list;
    }

    /**
     * Helper to close resources using Statement for compatibility.
     */
    private void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            System.err.println("Cleanup error: " + e.getMessage());
        }
    }
}