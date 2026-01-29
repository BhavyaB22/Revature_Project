package com.revconnect.dao;

import com.revconnect.config.DBConnection;
import com.revconnect.model.User;
import com.revconnect.model.Profile;
import com.revconnect.exception.DatabaseException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    /**
     * Checks if an email already exists in the database.
     */
    public boolean isEmailExists(String email) {
        String sql = "SELECT 1 FROM Users WHERE LOWER(email) = LOWER(?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, email);
            rs = pstmt.executeQuery();
            return rs.next(); 
        } catch (SQLException e) {
            return false;
        } finally {
            closeResources(conn, pstmt, rs);
        }
    }
    
    /**
     * Checks if a username already exists.
     */
    public boolean isUsernameExists(String username) {
        String sql = "SELECT 1 FROM Users WHERE LOWER(username) = LOWER(?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return false;
        } finally {
            closeResources(conn, pstmt, rs);
        }
    }

    /**
     * Registers a new user including the username.
     */
    public boolean registerUser(User user) {
        String userSql = "INSERT INTO USERS (email, username, password, user_type, privacy, s_question, s_answer) VALUES (?,?,?,?,?,?,?)";
        String profileSql = "INSERT INTO PROFILES (USER_ID, USERNAME) VALUES (?, ?)";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBConnection.getConnection();
            // Step 1: Tell JDBC we want the generated ID back
            pstmt = conn.prepareStatement(userSql, new String[]{"USER_ID"}); 
            pstmt.setString(1, user.getEmail());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPassword());
            pstmt.setString(4, user.getUserType());
            pstmt.setString(5, user.getPrivacy());
            pstmt.setString(6, user.getsQuestion());
            pstmt.setString(7, user.getsAnswer());

            if (pstmt.executeUpdate() > 0) {
                // Step 2: Get that auto-generated ID (like 13 or 14)
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    int newUserId = rs.getInt(1);
                    
                    // Step 3: Automatically create the Profile row
                    PreparedStatement profilePstmt = conn.prepareStatement(profileSql);
                    profilePstmt.setInt(1, newUserId);
                    profilePstmt.setString(2, user.getUsername());
                    profilePstmt.executeUpdate();
                    profilePstmt.close();
                }
                return true;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Registration failed", e);
        } finally {
            closeResources(conn, pstmt, null);
        }
        return false;
    }
    public User login(String email, String pass) {
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
                u.setUserId(rs.getInt("user_id"));
                u.setEmail(rs.getString("email"));
                u.setUserType(rs.getString("user_type"));
                return u;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Login database error", e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return null;
    }

    public String getSecurityQuestion(String email) {
        String sql = "SELECT s_question FROM USERS WHERE LOWER(email) = LOWER(?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, email);
            rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("s_question");
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching security question", e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return null;
    }

    public int verifySecurityAnswer(String email, String answer) {
        String sql = "SELECT user_id FROM Users WHERE LOWER(email)=LOWER(?) AND s_answer=?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, email);
            pstmt.setString(2, answer);
            rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt("user_id");
        } catch (SQLException e) {
            throw new DatabaseException("Verification error", e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return -1;
    }

    public int getNotificationCount(int userId) {
    	String sql = "SELECT COUNT(*) FROM Notifications WHERE USER_ID = ? AND READ_STATUS = 0";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error fetching notification count: " + e.getMessage());
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return 0;
    }

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
            throw new DatabaseException("Password update failed", e);
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
            throw new DatabaseException("ID retrieval error", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
        return 0;
    }

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
            if (rs.next()) return rs.getString("password");
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching old password", e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return null;
    }
    public String getUserTypeById(int userId) {
        String type = "Personal"; // Default
        String sql = "SELECT USER_TYPE FROM USERS WHERE USER_ID = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                type = rs.getString("USER_TYPE");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return type;
    }
    public List<Profile> searchUsers(String query) {
        List<Profile> results = new ArrayList<Profile>();
        String sql = "SELECT * FROM Profiles WHERE username LIKE ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, "%" + query + "%");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Profile p = new Profile();
                p.setUserId(rs.getInt("user_id"));
                p.setUsername(rs.getString("username"));
                p.setBio(rs.getString("bio"));
                results.add(p);
            }
        } catch (SQLException e) { 
            throw new DatabaseException("Search error", e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return results;
    }

    /**
     * Fixed helper method using Statement instead of PreparedStatement
     * to allow both normal and Prepared statements to be closed.
     */
    private void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            System.err.println("Resource cleanup error: " + e.getMessage());
        }
    }
}