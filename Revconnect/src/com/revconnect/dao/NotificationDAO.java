package com.revconnect.dao;

import com.revconnect.config.DBConnection;
import com.revconnect.exception.DatabaseException;
import com.revconnect.model.Notification; // Ensure this is imported
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    // 1. Create - Triggered by Likes, Comments, Requests, etc.
    public boolean createNotification(int receiverId, int senderId, String message, String type) {
        String sql = "INSERT INTO NOTIFICATIONS (USER_ID, SENDER_ID, MESSAGE, NOTIF_TYPE, READ_STATUS) VALUES (?, ?, ?, ?, 0)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, receiverId);
            pstmt.setInt(2, senderId);
            pstmt.setString(3, message);
            pstmt.setString(4, type);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to send notification", e);
        } finally {
            closeResources(conn, pstmt, null);
        }
    }

    // 2. Fetch - Used to display the interactive list
    public List<Notification> getNotificationsForUser(int userId) {
        List<Notification> list = new ArrayList<Notification>();
        // Using JOIN to get the username of the person who triggered the notification
        String sql = "SELECT n.*, u.USERNAME as sender_name FROM NOTIFICATIONS n " +
                     "JOIN USERS u ON n.SENDER_ID = u.USER_ID " +
                     "WHERE n.USER_ID = ? ORDER BY n.NOTIF_ID DESC";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                Notification n = new Notification();
                n.setNotifId(rs.getInt("NOTIF_ID"));
                n.setPostId(rs.getInt("POST_ID"));
                n.setReceiverId(rs.getInt("USER_ID"));
                n.setSenderId(rs.getInt("SENDER_ID"));
                n.setMessage(rs.getString("MESSAGE"));
                n.setType(rs.getString("NOTIF_TYPE"));
                n.setSenderUsername(rs.getString("sender_name")); 
                n.setReadStatus(rs.getInt("READ_STATUS"));
                list.add(n);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching notifications: " + e.getMessage());
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return list;
    }

    // 3. Count - For the login screen "You have X notifications"
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

    // 4. Update - Marks them read so the count resets
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