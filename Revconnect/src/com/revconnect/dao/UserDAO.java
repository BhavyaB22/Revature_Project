package com.revconnect.dao;

import com.revconnect.config.DBConnection;
import com.revconnect.model.User;
import com.revconnect.model.Profile;
import com.revconnect.exceptions.DatabaseException;
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
     * Registers a new user and creates an associated profile.
     */
    public boolean registerUser(User user) {
        String userSql = "INSERT INTO USERS (email, username, password, user_type, privacy, s_question, s_answer) VALUES (?,?,?,?,?,?,?)";
        String profileSql = "INSERT INTO PROFILES (USER_ID, USERNAME) VALUES (?, ?)";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        PreparedStatement profilePstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Transactions ensure data integrity

            pstmt = conn.prepareStatement(userSql, new String[]{"USER_ID"}); 
            pstmt.setString(1, user.getEmail());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPassword());
            pstmt.setString(4, user.getUserType());
            pstmt.setString(5, user.getPrivacy());
            pstmt.setString(6, user.getsQuestion());
            pstmt.setString(7, user.getsAnswer());

            if (pstmt.executeUpdate() > 0) {
                rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    int newUserId = rs.getInt(1);
                    profilePstmt = conn.prepareStatement(profileSql);
                    profilePstmt.setInt(1, newUserId);
                    profilePstmt.setString(2, user.getUsername());
                    profilePstmt.executeUpdate();
                }
                conn.commit();
                return true;
            }
        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            throw new DatabaseException("Registration failed", e);
        } finally {
            if (profilePstmt != null) try { profilePstmt.close(); } catch (SQLException e) {}
            closeResources(conn, pstmt, rs);
        }
        return false;
    }

    /**
     * Standard Login
     */
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
                u.setUsername(rs.getString("USERNAME"));
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

    /**
     * UPDATED: Fetches user by email for Password Recovery
     */
    public User getUserByEmail(String email) {
        String sql = "SELECT user_id, username, s_question, s_answer FROM Users WHERE LOWER(email) = LOWER(?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, email);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                User u = new User();
                u.setUserId(rs.getInt("user_id"));
                u.setUsername(rs.getString("username"));
                u.setsQuestion(rs.getString("s_question"));
                u.setsAnswer(rs.getString("s_answer"));
                return u;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving user for recovery", e);
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return null;
    }

    /**
     * UPDATED: Updates password after security verification
     */
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

    /**
     * Existing Search Functionality
     */
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
    
    public User getUserById(int id) {
        String sql = "SELECT USER_ID, USERNAME, USER_TYPE, EMAIL FROM USERS WHERE USER_ID = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                User u = new User();
                u.setUserId(rs.getInt("USER_ID"));
                u.setUsername(rs.getString("USERNAME"));
                u.setUserType(rs.getString("USER_TYPE"));
                u.setEmail(rs.getString("EMAIL"));
                return u;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return null;
    }
    
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<User>();
        String sql = "SELECT USER_ID, USERNAME, USER_TYPE, EMAIL FROM USERS";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                User u = new User();
                u.setUserId(rs.getInt("USER_ID"));
                u.setUsername(rs.getString("USERNAME"));
                u.setUserType(rs.getString("USER_TYPE"));
                u.setEmail(rs.getString("EMAIL"));
                users.add(u);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user list: " + e.getMessage());
        } finally {
            // Essential for older Java versions to prevent connection leaks
            closeResources(conn, pstmt, rs);
        }
        return users;
    }

    /**
     * Resource Cleanup Helper (Manual closing for older Java compatibility)
     */
    private void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            System.err.println("Cleanup Error: " + e.getMessage());
        }
    }
}