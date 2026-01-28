package com.revconnect.dao;

import com.revconnect.config.DBConnection;
import com.revconnect.model.Profile;
import java.sql.*;

public class ProfileDAO {

	public boolean saveProfile(Profile profile) {
	    // PROFILE_ID is excluded because your Trigger/Sequence handles it automatically.
	    String sql = "INSERT INTO Profiles (user_id, username, bio, location, website, " +
	                 "category, address, contact_info, business_hours) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
	    
	    Connection conn = null;
	    PreparedStatement pstmt = null;
	    try {
	        conn = DBConnection.getConnection();
	        pstmt = conn.prepareStatement(sql);
	        
	        pstmt.setInt(1, profile.getUserId());
	        pstmt.setString(2, profile.getUsername());   // Maps to FULL_NAME in DB
	        pstmt.setString(3, profile.getBio());
	        pstmt.setString(4, profile.getLocation());
	        pstmt.setString(5, profile.getWebsite());    // Maps to WEBSITE_LINK in DB
	        pstmt.setString(6, profile.getCategory());
	        pstmt.setString(7, profile.getAddress());    // Maps to BUSINESS_ADDRESS in DB
	        pstmt.setString(8, profile.getContactInfo());
	        pstmt.setString(9, profile.getBusinessHours());
	        
	        return pstmt.executeUpdate() > 0;
	    } catch (SQLException e) { 
	        System.out.println("SQL Error: " + e.getMessage());
	        e.printStackTrace(); 
	        return false; 
	    } finally { 
	        closeResources(conn, pstmt, null); 
	    }
	}

    public Profile getProfile(int userId) {
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
                p.setUsername(rs.getString("FULL_NAME"));
                p.setBio(rs.getString("bio"));
                p.setLocation(rs.getString("location"));
                p.setWebsite(rs.getString("website"));
                p.setCategory(rs.getString("category"));
                p.setAddress(rs.getString("address"));
                p.setContactInfo(rs.getString("contact_info"));
                p.setBusinessHours(rs.getString("business_hours"));
                return p;
            }
        } catch (SQLException e) { 
            e.printStackTrace(); 
        } finally { 
            closeResources(conn, pstmt, rs); 
        }
        return null;
    }

    public boolean updateProfile(Profile profile) {
        String sql = "UPDATE Profiles SET bio = ?, location = ?, website = ? WHERE user_id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, profile.getBio());
            pstmt.setString(2, profile.getLocation());
            pstmt.setString(3, profile.getWebsite());
            pstmt.setInt(4, profile.getUserId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { 
            e.printStackTrace(); 
            return false; 
        } finally { 
            closeResources(conn, pstmt, null); 
        }
    }

    // Updated helper to handle ResultSet closing safely
    private void closeResources(Connection conn, PreparedStatement pstmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
    }
}