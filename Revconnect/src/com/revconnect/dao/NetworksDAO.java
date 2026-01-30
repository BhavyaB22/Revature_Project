package com.revconnect.dao;

import com.revconnect.config.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NetworksDAO {

    // 1. View Incoming Requests
	// Fix this in NetworksDAO.java
	public List<String> getIncomingRequests(int myId) {
	    List<String> requests = new ArrayList<String>();
	    // Use 'NETWORKS' (plural) and the columns from your screenshot
	    String sql = "SELECT u.USERNAME, u.USER_ID FROM USERS u " +
	                 "JOIN NETWORKS n ON u.USER_ID = n.USER_ID " +
	                 "WHERE n.TARGET_ID = ? AND n.STATUS = 'PENDING'";
	    
	    Connection conn = null;
	    PreparedStatement pstmt = null;
	    ResultSet rs = null;

	    try {
	        conn = DBConnection.getConnection();
	        pstmt = conn.prepareStatement(sql);
	        pstmt.setInt(1, myId);
	        rs = pstmt.executeQuery();

	        while (rs.next()) {
	            requests.add(rs.getString("USERNAME") + " (ID: " + rs.getInt("USER_ID") + ")");
	        }
	    } catch (SQLException e) {
	        System.out.println("Error: " + e.getMessage());
	    } finally {
	        closeResources(conn, pstmt, rs);
	    }
	    return requests;
	}

    // 2. Accept or Reject Request
    public boolean updateRequestStatus(int myId, int senderId, String newStatus) {
        // We update the record where senderId sent a request to you (myId)
        String sql = "UPDATE NETWORKS SET STATUS = ? WHERE USER_ID = ? AND TARGET_ID = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, senderId);
            pstmt.setInt(3, myId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Update Status Error: " + e.getMessage());
            return false;
        } finally {
            closeResources(conn, pstmt, null);
        }
    }

    // 3. View Connections (Friends)
    public List<String> getConnections(int userId) {
        List<String> connections = new ArrayList<String>();
        // Using NETWORKS table and specific columns from your schema
        String sql = "SELECT u.USERNAME FROM USERS u " +
                     "JOIN NETWORKS n ON (u.USER_ID = n.USER_ID OR u.USER_ID = n.TARGET_ID) " +
                     "WHERE (n.USER_ID = ? OR n.TARGET_ID = ?) " +
                     "AND n.STATUS = 'ACCEPTED' " +
                     "AND u.USER_ID != ?";

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
            System.out.println("SQL Error: " + e.getMessage());
        } finally {
            // Use your existing helper method to clean up
            closeResources(conn, pstmt, rs);
        }
        return connections;
    }
    // 5. View Followers
    public List<String> getFollowing(int userId) {
        List<String> following = new ArrayList<String>();
        // TARGET_ID is the person YOU are following
        String sql = "SELECT u.USERNAME FROM USERS u " +
                     "JOIN NETWORKS n ON u.USER_ID = n.TARGET_ID " +
                     "WHERE n.USER_ID = ? AND n.STATUS = 'FOLLOWING'";
        // ... code to execute query
        return following;
    }

    public List<String> getFollowers(int userId) {
        List<String> followers = new ArrayList<String>();
        // USER_ID is the person following YOU (the TARGET_ID)
        String sql = "SELECT u.USERNAME FROM USERS u " +
                     "JOIN NETWORKS n ON u.USER_ID = n.USER_ID " +
                     "WHERE n.TARGET_ID = ? AND n.STATUS = 'FOLLOWING'";
        // ... code to execute query
        return followers;
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
    public boolean sendRequest(int senderId, int targetId, String status) {
        String sql = "INSERT INTO NETWORKS (USER_ID, TARGET_ID, STATUS) VALUES (?, ?, ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, senderId);
            pstmt.setInt(2, targetId);
            pstmt.setString(3, status);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error: Likely already connected/following.");
            return false;
        } finally {
            closeResources(conn, pstmt, null);
        }
    }

    public boolean removeConnectionOrFollow(int userId, int targetId) {
        String sql = "DELETE FROM NETWORKS WHERE (USER_ID = ? AND TARGET_ID = ?) OR (USER_ID = ? AND TARGET_ID = ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.setInt(2, targetId);
            pstmt.setInt(3, targetId);
            pstmt.setInt(4, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        } finally {
            closeResources(conn, pstmt, null);
        }
    }
    
    public boolean isFollowing(int userId, int targetId) {
        // Checks if a record exists with status 'FOLLOWING'
        String sql = "SELECT 1 FROM NETWORKS WHERE USER_ID = ? AND TARGET_ID = ? AND STATUS = 'FOLLOWING'";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.setInt(2, targetId);
            rs = pstmt.executeQuery();
            return rs.next(); // Returns true if a row is found
        } catch (SQLException e) {
            return false;
        } finally {
            closeResources(conn, pstmt, rs);
        }
    }
    
    public boolean isFriend(int userId, int targetId) {
        // Friends can be either (User1, User2) or (User2, User1)
        String sql = "SELECT 1 FROM NETWORKS WHERE STATUS = 'ACCEPTED' AND " +
                     "((USER_ID = ? AND TARGET_ID = ?) OR (USER_ID = ? AND TARGET_ID = ?))";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.setInt(2, targetId);
            pstmt.setInt(3, targetId);
            pstmt.setInt(4, userId);
            rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return false;
        } finally {
            closeResources(conn, pstmt, rs);
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