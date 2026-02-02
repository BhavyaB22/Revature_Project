package com.revconnect.dao;

import com.revconnect.config.DBConnection;
import com.revconnect.util.DBUtil; // Added this import
import com.revconnect.exceptions.*;
import com.revconnect.model.Posts;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostsDAO {

    // --- CREATE ---
    public boolean createPost(int userId, String postName, String content) throws DatabaseException {
        String sql = "INSERT INTO POSTS (USER_ID, TITLE, CONTENT, POST_TYPE) VALUES (?, ?, ?, 'Standard')";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.setString(2, postName);
            pstmt.setString(3, content);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Could not create post", e);
        } finally {
            DBUtil.close(conn, pstmt, null); // Using DBUtil
        }
    }

    // --- READ / FEED ---
    public List<Posts> getFeed() throws DatabaseException {
        List<Posts> feed = new ArrayList<Posts>();
        // Updated SQL to match your exact column names from the image
        String sql = "SELECT POST_ID, USER_ID, TITLE, CONTENT, LIKES, COMMENTS_COUNT FROM POSTS ORDER BY POST_ID DESC";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                Posts p = new Posts();
                p.setPostId(rs.getInt("POST_ID"));
                p.setUserId(rs.getInt("USER_ID"));
                p.setTitle(rs.getString("TITLE"));
                p.setContent(rs.getString("CONTENT"));
                // Mapping the exact column names from your screenshot
                p.setLikes(rs.getInt("LIKES")); 
                p.setCommentsCount(rs.getInt("COMMENTS_COUNT"));
                feed.add(p);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Database Error: " + e.getMessage());
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return feed;
    }
    public Posts getPostById(int pid) throws DatabaseException {
        String sql = "SELECT * FROM POSTS WHERE POST_ID = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, pid);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToPost(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching post", e);
        } finally {
            DBUtil.close(conn, pstmt, rs); // Using DBUtil
        }
        return null;
    }
    
    // --- Get Posts By User ID ---
    public List<Posts> getPostsByUserId(int userId) throws DatabaseException {
        List<Posts> list = new ArrayList<Posts>();
        String sql = "SELECT * FROM POSTS WHERE USER_ID = ? ORDER BY CREATED_TIME DESC";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToPost(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching user posts", e);
        } finally {
            DBUtil.close(conn, pstmt, rs); // Using DBUtil
        }
        return list;
    }

 // --- FIXED TOGGLE LIKE ---
    public String toggleLike(int userId, int postId) throws DatabaseException {
        String checkSql = "SELECT COUNT(*) FROM POST_LIKES WHERE POST_ID = ? AND USER_ID = ?";
        String insertSql = "INSERT INTO POST_LIKES (POST_ID, USER_ID) VALUES (?, ?)";
        String deleteSql = "DELETE FROM POST_LIKES WHERE POST_ID = ? AND USER_ID = ?";
        String incrementSql = "UPDATE POSTS SET LIKES = LIKES + 1 WHERE POST_ID = ?";
        String decrementSql = "UPDATE POSTS SET LIKES = LIKES - 1 WHERE POST_ID = ?";

        Connection conn = null;
        PreparedStatement psCheck = null, psAction = null, psUpdate = null;
        ResultSet rs = null;
        String status = "";

        try {
            conn = DBUtil.getConnection(); // Use your DBUtil here
            conn.setAutoCommit(false); // Start transaction

            psCheck = conn.prepareStatement(checkSql);
            psCheck.setInt(1, postId);
            psCheck.setInt(2, userId);
            rs = psCheck.executeQuery();
            
            boolean alreadyLiked = false;
            if (rs.next() && rs.getInt(1) > 0) alreadyLiked = true;

            if (alreadyLiked) {
                psAction = conn.prepareStatement(deleteSql);
                psUpdate = conn.prepareStatement(decrementSql);
                status = "Post Unliked.";
            } else {
                psAction = conn.prepareStatement(insertSql);
                psUpdate = conn.prepareStatement(incrementSql);
                status = "Post Liked!";
            }

            // Execute interaction (POST_LIKES table)
            psAction.setInt(1, postId);
            psAction.setInt(2, userId); 
            psAction.executeUpdate();

            // Execute count update (POSTS table)
            psUpdate.setInt(1, postId);
            psUpdate.executeUpdate();

            conn.commit(); // Save changes
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* ignored */ }
            }
            throw new DatabaseException("Toggle Like failed: " + e.getMessage());
        } finally {
            // Closing all statements and resultsets manually
            DBUtil.close(null, psCheck, null);
            DBUtil.close(null, psAction, null);
            DBUtil.close(null, psUpdate, null);
            DBUtil.close(conn, null, rs);
        }
        return status;
    }

    // --- DELETE ---

    public boolean deletePost(int postId, int userId) throws DatabaseException {

        String sql = "DELETE FROM POSTS WHERE POST_ID = ? AND USER_ID = ?";

        Connection conn = null;

        PreparedStatement pstmt = null;

        try {

            conn = DBConnection.getConnection();

            pstmt = conn.prepareStatement(sql);

            pstmt.setInt(1, postId);

            pstmt.setInt(2, userId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {

            throw new DatabaseException("Delete failed", e);

        } finally {

            DBUtil.close(conn, pstmt, null); // Using DBUtil

        }

    }
    
    public int getOwnerIdByPostId(int postId) throws DatabaseException {
        String sql = "SELECT USER_ID FROM POSTS WHERE POST_ID = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            // Use DBConnection for consistency with your other methods
            conn = DBConnection.getConnection(); 
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, postId);
            rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt("USER_ID");
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching post owner", e);
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return -1;
    }
    public boolean addComment(int postId, int userId, String content) throws DatabaseException {
        String insertSql = "INSERT INTO POST_COMMENTS (POST_ID, USER_ID, CONTENT) VALUES (?, ?, ?)";
        String updateCountSql = "UPDATE POSTS SET COMMENTS_COUNT = COMMENTS_COUNT + 1 WHERE POST_ID = ?";
        
        Connection conn = null;
        PreparedStatement psInsert = null;
        PreparedStatement psUpdate = null;
        
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false); // Start transaction

            psInsert = conn.prepareStatement(insertSql);
            psInsert.setInt(1, postId);
            psInsert.setInt(2, userId);
            psInsert.setString(3, content);
            psInsert.executeUpdate();

            psUpdate = conn.prepareStatement(updateCountSql);
            psUpdate.setInt(1, postId);
            psUpdate.executeUpdate();

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) {}
            }
            throw new DatabaseException("Comment failed: " + e.getMessage());
        } finally {
            DBUtil.close(null, psInsert, null);
            DBUtil.close(null, psUpdate, null);
            DBUtil.close(conn, null, null);
        }
    }
    // --- MAPPING HELPER ---
    private Posts mapResultSetToPost(ResultSet rs) throws SQLException {
        Posts p = new Posts();
        p.setPostId(rs.getInt("POST_ID"));
        p.setUserId(rs.getInt("USER_ID"));
        p.setTitle(rs.getString("TITLE"));
        p.setContent(rs.getString("CONTENT"));
        p.setLikes(rs.getInt("LIKES"));
        // Ensure this matches your Oracle column name exactly
        p.setCommentsCount(rs.getInt("COMMENTS_COUNT")); 
        return p;
    }
    
    public List<String> getCommentsByPostId(int pid) throws DatabaseException {
        List<String> comments = new ArrayList<String>();
        // We join with USERS to show who wrote the comment
        String sql = "SELECT u.USERNAME, c.CONTENT FROM POST_COMMENTS c " +
                     "JOIN USERS u ON c.USER_ID = u.USER_ID " +
                     "WHERE c.POST_ID = ? ORDER BY c.CREATED_AT ASC";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, pid);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                comments.add(rs.getString("USERNAME") + ": " + rs.getString("CONTENT"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching comments: " + e.getMessage());
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return comments;
    }
    
    public boolean hasUserLikedPost(int userId, int postId) throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM POST_LIKES WHERE POST_ID = ? AND USER_ID = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, postId);
            pstmt.setInt(2, userId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error checking like status: " + e.getMessage());
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return false;
    }
    
    public boolean hasUserCommented(int userId, int postId) throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM POST_COMMENTS WHERE POST_ID = ? AND USER_ID = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, postId);
            pstmt.setInt(2, userId);
            rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error checking comment status: " + e.getMessage());
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return false;
    }
    
    public boolean deleteUserComment(int userId, int postId) throws DatabaseException {
        String deleteSql = "DELETE FROM POST_COMMENTS WHERE POST_ID = ? AND USER_ID = ?";
        String updateCountSql = "UPDATE POSTS SET COMMENTS_COUNT = COMMENTS_COUNT - 1 WHERE POST_ID = ?";
        
        Connection conn = null;
        PreparedStatement psDelete = null;
        PreparedStatement psUpdate = null;
        
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            psDelete = conn.prepareStatement(deleteSql);
            psDelete.setInt(1, postId);
            psDelete.setInt(2, userId);
            int deletedRows = psDelete.executeUpdate();

            if (deletedRows > 0) {
                psUpdate = conn.prepareStatement(updateCountSql);
                psUpdate.setInt(1, postId);
                psUpdate.executeUpdate();
                conn.commit();
                return true;
            }
            return false;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) {}
            }
            throw new DatabaseException("Delete comment failed: " + e.getMessage());
        } finally {
            DBUtil.close(null, psDelete, null);
            DBUtil.close(null, psUpdate, null);
            DBUtil.close(conn, null, null);
        }
    }
}

