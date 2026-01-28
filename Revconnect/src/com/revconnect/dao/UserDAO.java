package com.revconnect.dao;

import com.revconnect.config.DBConnection;
import com.revconnect.model.User;
import com.revconnect.model.Profile;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

	public boolean isEmailExists(String email) {
	    // LOWER(?) ensures the input is normalized, 
	    // and LOWER(email) ensures the database record is normalized
	    String sql = "SELECT 1 FROM Users WHERE LOWER(email) = LOWER(?)";
	    Connection conn = null;
	    PreparedStatement pstmt = null;
	    ResultSet rs = null;
	    try {
	        conn = DBConnection.getConnection();
	        pstmt = conn.prepareStatement(sql);
	        pstmt.setString(1, email);
	        rs = pstmt.executeQuery();
	        
	        // If rs.next() is true, a matching email already exists
	        return rs.next();
	    } catch (SQLException e) {
	        System.out.println("Email Check Error: " + e.getMessage());
	        return false;
	    } finally {
	        closeResources(conn, pstmt, null);
	    }
	}

    public boolean registerUser(User user) {
        // USER_ID is handled by your Oracle Trigger/Sequence
        // Ensure column names match what we added to the DB: PRIVACY, S_QUESTION, S_ANSWER
        String sql = "INSERT INTO USERS (email, password, user_type, privacy, s_question, s_answer) VALUES (?,?,?,?,?,?)";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            
            pstmt.setString(1, user.getEmail());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getUserType());
            pstmt.setString(4, user.getPrivacy());
            pstmt.setString(5, user.getsQuestion());
            pstmt.setString(6, user.getsAnswer());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { 
            // Print the error so you can see if a column name is still mismatched
            System.out.println("User Registration Error: " + e.getMessage());
            return false; 
        } finally {
            // Standard cleanup (using your closeResources method)
            closeResources(conn, pstmt, null);
        }
    }

    public User login(String email, String pass) {
        // LOWER() on both sides makes the email check case-insensitive
        String sql = "SELECT * FROM Users WHERE LOWER(email) = LOWER(?) AND password = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, email);
            pstmt.setString(2, pass);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                User u = new User();
                // user_id is the primary key from your table
                u.setUserId(rs.getInt("user_id"));
                u.setEmail(rs.getString("email"));
                u.setUserType(rs.getString("user_type"));
                return u;
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Helpful for debugging connection issues
        } finally {
            // Use your existing closeResources helper
            closeResources(conn, pstmt, null);
        }
        return null;
    }

    public int verifySecurityAnswer(String email, String answer) {
        String sql = "SELECT user_id FROM Users WHERE email=? AND s_answer=?";
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, email);
            pstmt.setString(2, answer);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt("user_id");
        } catch (SQLException e) {}
        return -1;
    }
 // Add this to UserDAO.java

 // Resolves error: userDAO.getOldPassword(userId)
 public String getOldPassword(int userId) {
     String sql = "SELECT password FROM Users WHERE user_id = ?";
     Connection conn = null;
     PreparedStatement pstmt = null;
     ResultSet rs = null;
     try {
         conn = DBConnection.getConnection();
         pstmt = conn.prepareStatement(sql);
         pstmt.setInt(1, userId);
         rs = pstmt.executeQuery();
         if (rs.next()) {
             return rs.getString("password");
         }
     } catch (SQLException e) {
         e.printStackTrace();
     } finally {
         closeResources(conn, pstmt, null);
     }
     return null;
 }

 // Resolves error: userDAO.updatePassword(userId, newPass)
 public boolean updatePassword(int userId, String newPass) {
     String sql = "UPDATE Users SET password = ? WHERE user_id = ?";
     Connection conn = null;
     PreparedStatement pstmt = null;
     try {
         conn = DBConnection.getConnection();
         pstmt = conn.prepareStatement(sql);
         pstmt.setString(1, newPass);
         pstmt.setInt(2, userId);
         return pstmt.executeUpdate() > 0;
     } catch (SQLException e) {
         e.printStackTrace();
         return false;
     } finally {
         closeResources(conn, pstmt, null);
     }
 }

 	public int getLastInsertedId() {
	    String sql = "SELECT MAX(user_id) FROM Users"; 
	    Connection conn = null;
	    Statement stmt = null;
	    ResultSet rs = null;
	    try {
	        conn = DBConnection.getConnection();
	        stmt = conn.createStatement();
	        rs = stmt.executeQuery(sql);
	        if (rs.next()) return rs.getInt(1);
	    } catch (SQLException e) { 
	        e.printStackTrace(); 
	    } finally {
	        closeResources(conn, null, stmt); // Pass stmt here!
	    }
	    return 0;
	}

    public List<Profile> searchUsers(String query) {
        List<Profile> results = new ArrayList<Profile>();
        String sql = "SELECT * FROM Profiles WHERE username LIKE ?";
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, "%" + query + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Profile p = new Profile();
                p.setUserId(rs.getInt("user_id"));
                p.setUsername(rs.getString("username"));
                p.setBio(rs.getString("bio"));
                results.add(p);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return results;
    }
    public String getSecurityQuestion(String email) {
        String question = null;
        String sql = "SELECT s_question FROM USERS WHERE LOWER(email) = LOWER(?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, email);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                question = rs.getString("s_question");
            }
        } catch (SQLException e) {
            System.out.println("Error fetching question: " + e.getMessage());
        } finally {
            // Use your existing helper method to clean up
            closeResources(conn, pstmt, null);
        }
        return question;
    }
    private void closeResources(Connection conn, PreparedStatement pstmt, Statement stmt) {
        try {
            if (pstmt != null) pstmt.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}