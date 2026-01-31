package com.revconnect.dao;

import com.revconnect.config.DBConnection;
import com.revconnect.model.Profile;
import com.revconnect.exceptions.*;
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
    public boolean updateProfile(int userId, String name, String bio, String loc, String web) {
        String sql = "UPDATE PROFILES SET username = ?, BIO = ?, LOCATION = ?, WEBSITE = ? WHERE USER_ID = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            
            pstmt.setString(1, name);
            pstmt.setString(2, bio);
            pstmt.setString(3, loc);
            pstmt.setString(4, (web != null && !web.isEmpty()) ? web : null);
            pstmt.setInt(5, userId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            // Manually closing resources as your Java version requires
            try {
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
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