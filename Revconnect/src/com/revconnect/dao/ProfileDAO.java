package com.revconnect.dao;

import com.revconnect.config.DBConnection;
import com.revconnect.util.DBUtil;
import com.revconnect.model.Profile;
import com.revconnect.exceptions.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProfileDAO {

    
    public void ensureProfileExists(int userId, String username) {
        String checkSql = "SELECT COUNT(*) FROM Profiles WHERE user_id = ?";
        String insertSql = "INSERT INTO Profiles (user_id, username, bio, location, website) VALUES (?, ?, '-', '-', '-')";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(checkSql);
            pstmt.setInt(1, userId);
            rs = pstmt.executeQuery();
            
            if (rs.next() && rs.getInt(1) == 0) {
                // Clear statement before reuse using DBUtil
                DBUtil.close(null, pstmt, null); 
                
                pstmt = conn.prepareStatement(insertSql);
                pstmt.setInt(1, userId);
                pstmt.setString(2, username);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
    }

    
    public Profile getProfileByUserId(int userId) throws DatabaseException {
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
                p.setUsername(rs.getString("username"));
                p.setBio(rs.getString("bio"));
                p.setLocation(rs.getString("location"));
                p.setWebsite(rs.getString("website"));
                return p;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching profile for user: " + userId, e);
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return null;
    }

    /**
     * UPDATE FUNCTIONALITY: Updates existing profile information.
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
            pstmt.setString(4, (web != null && !web.isEmpty()) ? web : "-");
            pstmt.setInt(5, userId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtil.close(conn, pstmt, null);
        }
    }

   
    public List<Profile> getAllProfiles() throws DatabaseException {
        List<Profile> list = new ArrayList<Profile>();
        String sql = "SELECT * FROM Profiles";
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                Profile p = new Profile();
                p.setUserId(rs.getInt("user_id"));
                p.setUsername(rs.getString("username"));
                p.setBio(rs.getString("bio"));
                p.setLocation(rs.getString("location"));
                p.setWebsite(rs.getString("website"));
                list.add(p);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not retrieve profiles", e);
        } finally {
            DBUtil.close(conn, stmt, rs);
        }
        return list;
    }
}