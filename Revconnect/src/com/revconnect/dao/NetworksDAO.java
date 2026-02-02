package com.revconnect.dao;

import com.revconnect.util.DBUtil;
import com.revconnect.model.User;
import com.revconnect.exceptions.DatabaseException;
import com.revconnect.exceptions.NetworkException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NetworksDAO {

    // 1. View Incoming Requests
    public List<String> getIncomingRequests(int myId) throws DatabaseException {
        List<String> requests = new ArrayList<String>();
        String sql = "SELECT u.USERNAME, u.USER_ID FROM USERS u " +
                     "JOIN NETWORKS n ON u.USER_ID = n.USER_ID " +
                     "WHERE n.TARGET_ID = ? AND n.STATUS = 'PENDING'";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, myId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                requests.add(rs.getString("USERNAME") + " (ID: " + rs.getInt("USER_ID") + ")");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching incoming requests", e);
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return requests;
    }

    // 2. Accept or Reject Request
    public boolean updateRequestStatus(int myId, int senderId, String status) throws DatabaseException {
        String sql = "UPDATE networks SET status = ? WHERE user_id = ? AND target_id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, status);
            pstmt.setInt(2, senderId);
            pstmt.setInt(3, myId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating network status: " + e.getMessage());
        } finally {
            // Essential to close resources in older Java versions
            try { if (pstmt != null) pstmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }

    // 3. View Connections (Strings)
    public List<String> getConnections(int userId) throws DatabaseException {
        List<String> connections = new ArrayList<String>();
        String sql = "SELECT u.USERNAME FROM USERS u " +
                     "JOIN NETWORKS n ON (u.USER_ID = n.USER_ID OR u.USER_ID = n.TARGET_ID) " +
                     "WHERE (n.USER_ID = ? OR n.TARGET_ID = ?) " +
                     "AND n.STATUS = 'ACCEPTED' " + 
                     "AND u.USER_ID != ?"; 

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            pstmt.setInt(3, userId);
            
            rs = pstmt.executeQuery();
            while (rs.next()) {
                connections.add(rs.getString("USERNAME"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving connection list", e);
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return connections;
    }

    // 4. Send Request (Fixing column logic and adding Exception)
    public boolean sendRequest(int senderId, int targetId, String status) throws DatabaseException, NetworkException {
        if (senderId == targetId) {
            throw new NetworkException("Logic Error: You cannot connect with yourself.");
        }

        String sql = "INSERT INTO NETWORKS (USER_ID, TARGET_ID, STATUS) VALUES (?, ?, ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, senderId);
            pstmt.setInt(2, targetId);
            pstmt.setString(3, status);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1 || e.getSQLState().equals("23000")) {
                throw new NetworkException("You are already connected to or following this user.");
            }
            throw new DatabaseException("Error sending request", e);
        } finally {
            DBUtil.close(conn, pstmt);
        }
    }

    // 5. Remove Connection (With Pre-Check Logic)
    public boolean removeConnection(int myId, int targetUserId) throws DatabaseException, NetworkException {
        // Logic check: verify connection exists first
        if (!isFriend(myId, targetUserId)) {
            throw new NetworkException("Cannot remove: No accepted connection exists with User ID: " + targetUserId);
        }

        String sql = "DELETE FROM NETWORKS WHERE " +
                     "((USER_ID = ? AND TARGET_ID = ?) OR (USER_ID = ? AND TARGET_ID = ?)) " +
                     "AND STATUS = 'ACCEPTED'";

        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, myId);
            pstmt.setInt(2, targetUserId);
            pstmt.setInt(3, targetUserId);
            pstmt.setInt(4, myId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error removing connection", e);
        } finally {
            DBUtil.close(conn, pstmt);
        }
    }

    // 6. View Followers/Following 
    public List<String> getFollowing(int userId) throws DatabaseException {
        String sql = "SELECT u.USERNAME FROM USERS u JOIN NETWORKS n ON u.USER_ID = n.TARGET_ID WHERE n.USER_ID = ? AND n.STATUS = 'FOLLOWING'";
        return fetchUsernames(sql, userId);
    }

    public List<String> getFollowers(int userId) throws DatabaseException {
        String sql = "SELECT u.USERNAME FROM USERS u JOIN NETWORKS n ON u.USER_ID = n.USER_ID WHERE n.TARGET_ID = ? AND n.STATUS = 'FOLLOWING'";
        return fetchUsernames(sql, userId);
    }

    private List<String> fetchUsernames(String sql, int id) throws DatabaseException {
        List<String> list = new ArrayList<String>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            rs = pstmt.executeQuery();
            while (rs.next()) { list.add(rs.getString("USERNAME")); }
        } catch (SQLException e) { 
            throw new DatabaseException("Error fetching usernames", e); 
        } finally { 
            DBUtil.close(conn, pstmt, rs); 
        }
        return list;
    }

    // Checkers
    public boolean isFriend(int userId, int targetId) throws DatabaseException {
        String sql = "SELECT 1 FROM NETWORKS WHERE STATUS = 'ACCEPTED' AND " +
                     "((USER_ID = ? AND TARGET_ID = ?) OR (USER_ID = ? AND TARGET_ID = ?))";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.setInt(2, targetId);
            pstmt.setInt(3, targetId);
            pstmt.setInt(4, userId);
            rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new DatabaseException("Friend verification failed", e);
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
    }

    public List<User> getAcceptedConnections(int myId) throws DatabaseException {
        List<User> connections = new ArrayList<User>();
        String sql = "SELECT U.USER_ID, U.USERNAME FROM USERS U " +
                "JOIN NETWORKS N ON (U.USER_ID = N.USER_ID OR U.USER_ID = N.TARGET_ID) " +
                "WHERE (N.USER_ID = ? OR N.TARGET_ID = ?) " +
                "AND U.USER_ID != ? AND N.STATUS = 'ACCEPTED'";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, myId);
            pstmt.setInt(2, myId);
            pstmt.setInt(3, myId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                User user = new User();
                user.setUserId(rs.getInt("USER_ID"));
                user.setUsername(rs.getString("USERNAME"));
                connections.add(user);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching object-based connections", e);
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return connections;
    }
    
    
    public boolean isFollowing(int followerId, int creatorId) throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM networks WHERE user_id = ? AND target_id = ? AND status = 'FOLLOWING'";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, followerId);
            pstmt.setInt(2, creatorId);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error checking follow status: " + e.getMessage());
        } finally {
            // Manually closing resources since try-with-args is not used
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (pstmt != null) pstmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
        return false;
    }
    
    
}