package com.revconnect.dao;

import com.revconnect.util.DBUtil;
import com.revconnect.exceptions.DatabaseException;
import com.revconnect.exceptions.NetworkException;
import com.revconnect.model.Notification;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    // 1. Create - Triggered by Likes, Comments, Requests, etc.
    public void createNotification(int receiverId, int senderId, String message, String type) throws DatabaseException {
        String sql = "INSERT INTO NOTIFICATIONS (USER_ID, SENDER_ID, MESSAGE, NOTIF_TYPE, READ_STATUS) VALUES (?, ?, ?, ?, 0)";
        
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, receiverId);
            pstmt.setInt(2, senderId);
            pstmt.setString(3, message);
            pstmt.setString(4, type);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not create notification for user: " + receiverId, e);
        } finally {
            DBUtil.close(conn, pstmt);
        }
    }

    // 2. Fetch - Used to display the interactive list
    public List<Notification> getNotificationsForUser(int userId) throws DatabaseException {
        List<Notification> list = new ArrayList<Notification>();
        String sql = "SELECT NOTIF_ID, USER_ID, SENDER_ID, POST_ID, MESSAGE, NOTIF_TYPE, READ_STATUS " +
                     "FROM NOTIFICATIONS WHERE USER_ID = ? ORDER BY NOTIF_ID DESC";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                Notification n = new Notification();
                n.setNotifId(rs.getInt("NOTIF_ID")); 
                n.setReceiverId(rs.getInt("USER_ID")); 
                n.setSenderId(rs.getInt("SENDER_ID")); 
                
                // Safety fix: Check if POST_ID is null in DB before setting
                int postId = rs.getInt("POST_ID");
                if (!rs.wasNull()) {
                    n.setPostId(postId);
                }

                n.setMessage(rs.getString("MESSAGE")); 
                n.setType(rs.getString("NOTIF_TYPE")); 
                n.setReadStatus(rs.getInt("READ_STATUS"));
                list.add(n);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving notifications for user ID: " + userId, e);
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return list;
    }

    // 3. Count - For the login screen "You have X notifications"
    public int getUnreadCount(int userId) throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM NOTIFICATIONS WHERE USER_ID = ? AND READ_STATUS = 0";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new DatabaseException("Error counting unread notifications", e);
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return 0;
    }

    // 4. Update - Marks them read
    public void markAllAsRead(int userId) throws DatabaseException {
        String sql = "UPDATE NOTIFICATIONS SET READ_STATUS = 1 WHERE USER_ID = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to mark notifications as read", e);
        } finally {
            DBUtil.close(conn, pstmt);
        }
    }

    // 5. Single deletion
    public void deleteNotification(int notifId) throws DatabaseException, NetworkException {
        String sql = "DELETE FROM NOTIFICATIONS WHERE NOTIF_ID = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, notifId);
            int rows = pstmt.executeUpdate();
            
            if (rows == 0) {
                throw new NetworkException("Notification ID " + notifId + " does not exist.");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting notification", e);
        } finally {
            DBUtil.close(conn, pstmt);
        }
    }
    
    // 6. Delete all notifications
    public void deleteAllNotifications(int userId) throws DatabaseException {
        String sql = "DELETE FROM NOTIFICATIONS WHERE USER_ID = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to clear notification history", e);
        } finally {
            DBUtil.close(conn, pstmt);
        }
    }
}