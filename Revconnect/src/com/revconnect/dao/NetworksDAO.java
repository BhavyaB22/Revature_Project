package com.revconnect.dao;

import com.revconnect.config.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NetworksDAO {

    // 1. View Incoming Requests
    public List<String> getIncomingRequests(int userId) {
        List<String> requests = new ArrayList<String>();
        String sql = "SELECT u.USERNAME FROM CONNECTION_REQUESTS r " +
                     "JOIN USERS u ON r.SENDER_ID = u.USER_ID " +
                     "WHERE r.RECEIVER_ID = ? AND r.STATUS = 'PENDING'";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                requests.add(rs.getString("USERNAME"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return requests;
    }

    // 2. Accept or Reject Request
    public boolean updateRequestStatus(int receiverId, int senderId, String status) {
        String updateSql = "UPDATE CONNECTION_REQUESTS SET STATUS = ? WHERE RECEIVER_ID = ? AND SENDER_ID = ?";
        String connectionSql = "INSERT INTO CONNECTIONS (USER1_ID, USER2_ID) VALUES (?, ?)";
        
        Connection conn = null;
        PreparedStatement pstmtUpdate = null;
        PreparedStatement pstmtInsert = null;
        
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); 

            pstmtUpdate = conn.prepareStatement(updateSql);
            pstmtUpdate.setString(1, status);
            pstmtUpdate.setInt(2, receiverId);
            pstmtUpdate.setInt(3, senderId);
            pstmtUpdate.executeUpdate();

            if ("ACCEPTED".equalsIgnoreCase(status)) {
                pstmtInsert = conn.prepareStatement(connectionSql);
                pstmtInsert.setInt(1, receiverId);
                pstmtInsert.setInt(2, senderId);
                pstmtInsert.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return false;
        } finally {
            if (pstmtUpdate != null) try { pstmtUpdate.close(); } catch (SQLException e) {}
            if (pstmtInsert != null) try { pstmtInsert.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    // 3. View Connections (Friends)
    public List<String> getConnections(int userId) {
        List<String> connections = new ArrayList<String>();
        String sql = "SELECT u.USERNAME FROM CONNECTIONS c " +
                     "JOIN USERS u ON (CASE WHEN c.USER1_ID = ? THEN c.USER2_ID ELSE c.USER1_ID END) = u.USER_ID " +
                     "WHERE c.USER1_ID = ? OR c.USER2_ID = ?";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            pstmt.setInt(3, userId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                connections.add(rs.getString("USERNAME"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return connections;
    }

    // 5. View Followers
    public List<String> getFollowers(int userId) {
        String sql = "SELECT u.USERNAME FROM FOLLOWS f JOIN USERS u ON f.FOLLOWER_ID = u.USER_ID WHERE f.FOLLOWING_ID = ?";
        return fetchUsernames(sql, userId);
    }

    // 6. View Following
    public List<String> getFollowing(int userId) {
        String sql = "SELECT u.USERNAME FROM FOLLOWS f JOIN USERS u ON f.FOLLOWING_ID = u.USER_ID WHERE f.FOLLOWER_ID = ?";
        return fetchUsernames(sql, userId);
    }

    // Helper for Followers/Following
    private List<String> fetchUsernames(String sql, int id) {
        List<String> list = new ArrayList<String>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            rs = pstmt.executeQuery();
            while (rs.next()) { list.add(rs.getString("USERNAME")); }
        } catch (SQLException e) { e.printStackTrace(); }
        finally { closeResources(conn, pstmt, rs); }
        return list;
    }
 // 4. Remove Connection
    public boolean removeConnection(int user1, int user2) {
        // This deletes the relationship regardless of who is USER1 or USER2
        String sql = "DELETE FROM CONNECTIONS WHERE (USER1_ID = ? AND USER2_ID = ?) OR (USER1_ID = ? AND USER2_ID = ?)";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, user1);
            pstmt.setInt(2, user2);
            pstmt.setInt(3, user2);
            pstmt.setInt(4, user1);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            // Using your helper method to close resources safely
            closeResources(conn, pstmt, null);
        }
    }
 // Add these to NetworksDAO.java
    
    public boolean followUser(int followerId, int followingId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        String sql = "INSERT INTO FOLLOWERS (FOLLOWER_ID, FOLLOWING_ID) VALUES (?, ?)";
        
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, followerId);
            pstmt.setInt(2, followingId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            // Log error or handle duplicate entry
            return false;
        } finally {
            // Manually closing to avoid "Try with Arguments" error
            try { if (pstmt != null) pstmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }

    public boolean unfollowUser(int followerId, int followingId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        String sql = "DELETE FROM FOLLOWERS WHERE FOLLOWER_ID = ? AND FOLLOWING_ID = ?";
        
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, followerId);
            pstmt.setInt(2, followingId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            try { if (pstmt != null) pstmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
    public int getUnreadCount(int userId) {
        String sql = "SELECT COUNT(*) FROM NOTIFICATIONS WHERE USER_ID = ? AND READ_STATUS = 0";
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
            e.printStackTrace();
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return 0;
    }

    // Add this to clear notifications (Mark as read)
    public void markAllAsRead(int userId) {
        String sql = "UPDATE NOTIFICATIONS SET READ_STATUS = 1 WHERE USER_ID = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResources(conn, pstmt, null);
        }
    }

    // Helper to close resources (Crucial for older Java)
    private void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}