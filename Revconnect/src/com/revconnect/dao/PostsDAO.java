package com.revconnect.dao;

import com.revconnect.config.DBConnection;
import com.revconnect.exceptions.*;
import com.revconnect.model.Posts; // Ensure this is Post (singular) or matches your model
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostsDAO {

    // --- CREATE ---
    public boolean createPost(int userId, String postName, String content) {
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
            e.printStackTrace();
            return false;
        } finally { closeResources(conn, pstmt, null); }
    }

    // --- READ / FEED ---
    public List<Posts> getFeed() {
        List<Posts> list = new ArrayList<Posts>();
        String sql = "SELECT * FROM POSTS ORDER BY CREATED_TIME DESC";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Posts p = mapResultSetToPost(rs);
                list.add(p);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        finally { closeResources(conn, pstmt, rs); }
        return list;
    }
    public Posts getPostById(int pid) throws DatabaseException {
        String sql = "SELECT * FROM POSTS WHERE POST_ID = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = com.revconnect.config.DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, pid);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Posts p = new Posts();
                p.setPostId(rs.getInt("POST_ID"));
                p.setUserId(rs.getInt("USER_ID"));
                p.setTitle(rs.getString("Title"));
                p.setContent(rs.getString("Content"));
                p.setPostType(rs.getString("POST_TYPE"));
                p.setLikes(rs.getInt("LIKES"));
                p.setCommentsCount(rs.getInt("COMMENTS_COUNT"));
                p.setCreatedTime(rs.getTimestamp("CREATED_TIME"));
                return p;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching post details", e);
        } finally {
            // Use your existing closeResources helper method
            closeResources(conn, pstmt, rs);
        }
        return null;
    }

    public List<Posts> getPostsByUserId(int userId) {
        List<Posts> list = new ArrayList<Posts>();
        String sql = "SELECT * FROM POSTS WHERE USER_ID = ?";
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
        } catch (SQLException e) { e.printStackTrace(); }
        finally { closeResources(conn, pstmt, rs); }
        return list;
    }

    public List<Posts> searchPosts(String query) {
        List<Posts> list = new ArrayList<Posts>();
        String sql = "SELECT * FROM POSTS WHERE CONTENT LIKE ? OR POST_NAME LIKE ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, "%" + query + "%");
            pstmt.setString(2, "%" + query + "%");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToPost(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        finally { closeResources(conn, pstmt, rs); }
        return list;
    }

    // --- LIKE / UNLIKE LOGIC ---
    public void likePost(int userId, int postId) {
        // 1. Check if already liked (to prevent 'Already liked' errors unnecessarily)
        String insertSql = "INSERT INTO POST_LIKES (POST_ID, USER_ID) VALUES (?, ?)";
        // 2. THIS IS THE MISSING PART: Increment the LIKES column in your POSTS table
        String updateSql = "UPDATE POSTS SET LIKES = LIKES + 1 WHERE POST_ID = ?";

        Connection conn = null;
        PreparedStatement psInsert = null;
        PreparedStatement psUpdate = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Transactions are safer

            psInsert = conn.prepareStatement(insertSql);
            psInsert.setInt(1, postId);
            psInsert.setInt(2, userId);
            psInsert.executeUpdate();

            psUpdate = conn.prepareStatement(updateSql);
            psUpdate.setInt(1, postId);
            psUpdate.executeUpdate();

            conn.commit();
            System.out.println("Post Liked!");
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            System.out.println("Already liked.");
        } finally {
            closeResources(conn, psInsert, null);
            try { if(psUpdate != null) psUpdate.close(); } catch(Exception e) {}
        }
    }
    public boolean unlikePost(int postId, int userId) {
        String sql = "DELETE FROM LIKES WHERE POST_ID = ? AND USER_ID = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, postId);
            pstmt.setInt(2, userId);
            if (pstmt.executeUpdate() > 0) {
                return updatePostCount(postId, "LIKES", -1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        finally { closeResources(conn, pstmt, null); }
        return false;
    }

    public boolean hasUserLiked(int postId, int userId) {
        String sql = "SELECT 1 FROM LIKES WHERE POST_ID = ? AND USER_ID = ?";
        return checkExists(sql, postId, userId);
    }

    public boolean hasUserReposted(int originalPostId, int userId) {
        String sql = "SELECT 1 FROM POSTS WHERE ORIGINAL_POST_ID = ? AND USER_ID = ? AND POST_TYPE = 'Repost'";
        return checkExists(sql, originalPostId, userId);
    }

    private boolean checkExists(String sql, int p1, int p2) {
        Connection conn = null; PreparedStatement pstmt = null; ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, p1); pstmt.setInt(2, p2);
            rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) { return false; }
        finally { closeResources(conn, pstmt, rs); }
    }

    public boolean updatePostCount(int postId, String column, int increment) {
        String sql = "UPDATE POSTS SET " + column + " = " + column + " + ? WHERE POST_ID = ?";
        Connection conn = null; PreparedStatement pstmt = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, increment);
            pstmt.setInt(2, postId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
        finally { closeResources(conn, pstmt, null); }
    }
    
    //increase like count + store who liked it 
    public String toggleLike(int userId, int postId) {
        // SQL Definitions
        String checkSql = "SELECT COUNT(*) FROM POST_LIKES WHERE POST_ID = ? AND USER_ID = ?";
        String insertSql = "INSERT INTO POST_LIKES (POST_ID, USER_ID) VALUES (?, ?)";
        String deleteSql = "DELETE FROM POST_LIKES WHERE POST_ID = ? AND USER_ID = ?";
        String incrementSql = "UPDATE POSTS SET LIKES = LIKES + 1 WHERE POST_ID = ?";
        String decrementSql = "UPDATE POSTS SET LIKES = LIKES - 1 WHERE POST_ID = ?";
        String verifyPostSql = "SELECT COUNT(*) FROM POSTS WHERE POST_ID = ?";

        Connection conn = null;
        PreparedStatement psVerify = null;
        PreparedStatement psCheck = null;
        PreparedStatement psIns = null;
        PreparedStatement psDel = null;
        PreparedStatement psInc = null;
        PreparedStatement psDec = null;
        ResultSet rsVerify = null;
        ResultSet rs = null;
        String status = ""; 

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Enable transaction

            // --- STEP 1: Verify the Post exists ---
            psVerify = conn.prepareStatement(verifyPostSql);
            psVerify.setInt(1, postId);
            rsVerify = psVerify.executeQuery();
            if (rsVerify.next() && rsVerify.getInt(1) == 0) {
                return "Error: Post ID " + postId + " not found in database.";
            }

            // --- STEP 2: Check current status ---
            int count = 0;
            psCheck = conn.prepareStatement(checkSql);
            psCheck.setInt(1, postId); 
            psCheck.setInt(2, userId); 
            rs = psCheck.executeQuery();
            if (rs.next()) count = rs.getInt(1);

            if (count > 0) {
                // --- STEP 3: UNLIKE Logic ---
                psDel = conn.prepareStatement(deleteSql);
                psDel.setInt(1, postId);
                psDel.setInt(2, userId);
                psDel.executeUpdate();

                psDec = conn.prepareStatement(decrementSql);
                psDec.setInt(1, postId);
                psDec.executeUpdate();
                status = "Post Unliked.";
            } else {
                // --- STEP 4: LIKE Logic ---
                psIns = conn.prepareStatement(insertSql);
                psIns.setInt(1, postId); 
                psIns.setInt(2, userId); 
                psIns.executeUpdate();

                psInc = conn.prepareStatement(incrementSql);
                psInc.setInt(1, postId);
                psInc.executeUpdate();
                status = "Post Liked!";
            }

            conn.commit(); // Save transaction
            
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            status = "Error processing like: " + e.getMessage();
        } finally {
            // Clean up all resources manually to avoid memory leaks
            try { if (rsVerify != null) rsVerify.close(); } catch (Exception e) {}
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (psVerify != null) psVerify.close(); } catch (Exception e) {}
            try { if (psCheck != null) psCheck.close(); } catch (Exception e) {}
            try { if (psIns != null) psIns.close(); } catch (Exception e) {}
            try { if (psDel != null) psDel.close(); } catch (Exception e) {}
            try { if (psInc != null) psInc.close(); } catch (Exception e) {}
            try { if (psDec != null) psDec.close(); } catch (Exception e) {}
            closeResources(conn, null, null); //
        }
        return status;
    }
   
    // add a comment 
    
    

    public boolean deletePost(int postId, int userId) {
        // SQL ensures the POST_ID exists AND belongs to the logged-in USER_ID
        String sql = "DELETE FROM POSTS WHERE POST_ID = ? AND USER_ID = ?";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, postId);
            pstmt.setInt(2, userId); 
            
            int rowsDeleted = pstmt.executeUpdate();
            if (rowsDeleted > 0) {
                success = true;
            }
        } catch (SQLException e) {
            System.out.println("Delete Error: " + e.getMessage());
        } finally {
            closeResources(conn, pstmt, null);
        }
        return success;
    }

    private Posts mapResultSetToPost(ResultSet rs) throws SQLException {
        Posts p = new Posts();
        p.setPostId(rs.getInt("POST_ID"));
        p.setUserId(rs.getInt("USER_ID"));
        p.setTitle(rs.getString("TITLE"));
        p.setContent(rs.getString("CONTENT"));
        p.setLikes(rs.getInt("LIKES"));
        p.setCommentsCount(rs.getInt("COMMENTS_COUNT"));
        p.setPostType(rs.getString("POST_TYPE"));
        return p;
    }
    public boolean addComment(int postId, int userId, String text) {
        
        String sql = "INSERT INTO POST_COMMENTS (POST_ID, USER_ID, CONTENT) VALUES (?, ?, ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, postId);
            pstmt.setInt(2, userId);
            pstmt.setString(3, text);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeResources(conn, pstmt, null);
        }
    }
 
    public List<String> getLikesForPost(int postId) {
        List<String> likers = new ArrayList<String>();
        // FIX: Changed table name from LIKES to POST_LIKES
        String sql = "SELECT u.USERNAME FROM POST_LIKES l " +
                     "JOIN USERS u ON l.USER_ID = u.USER_ID " +
                     "WHERE l.POST_ID = ?";
                     
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, postId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                likers.add(rs.getString("USERNAME"));
            }
        } catch (SQLException e) {
            // This will now catch if the table name is still wrong
            System.err.println("Error fetching likers: " + e.getMessage());
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return likers;
    }

    public List<String> getCommentsForPost(int postId) {
        List<String> comments = new ArrayList<String>();
       
        String sql = "SELECT c.COMMENT_ID, u.USERNAME, c.CONTENT, " +
                     "TO_CHAR(c.CREATED_TIME, 'DD-Mon HH24:MI') as TIME_STAMP " +
                     "FROM COMMENTS c JOIN USERS u ON c.USER_ID = u.USER_ID " +
                     "WHERE c.POST_ID = ? ORDER BY c.CREATED_TIME ASC";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, postId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
              
                String entry = String.format("[%d] %s (%s): %s", 
                                rs.getInt("COMMENT_ID"), 
                                rs.getString("USERNAME"), 
                                rs.getString("TIME_STAMP"), 
                                rs.getString("CONTENT"));
                comments.add(entry);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return comments;
    }
    public boolean deleteComment(int commentId, int postId) {
        String sql = "DELETE FROM COMMENTS WHERE COMMENT_ID = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, commentId);
            
            if (pstmt.executeUpdate() > 0) {
                // After deleting the row, we must decrement the count in POSTS table
                return updatePostCount(postId, "COMMENTS_COUNT", -1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResources(conn, pstmt, null);
        }
        return false;
    }

    public boolean updatePost(int pid, String column, String value) {
        // Dynamically update based on user choice (POST_NAME or CONTENT)
        String sql = "UPDATE POSTS SET " + column + " = ? WHERE POST_ID = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, value);
            pstmt.setInt(2, pid);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeResources(conn, pstmt, null);
        }
    }
    
    public int getOwnerIdByPostId(int pid) throws DatabaseException {
        String sql = "SELECT USER_ID FROM POSTS WHERE POST_ID = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, pid);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("USER_ID");
            } else {
                return -1; 
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving post owner for Post ID: " + pid, e);
        } finally {
            // Manual cleanup of resources
            closeResources(conn, pstmt, rs);
        }
    }

    public List<Posts> getAllPosts() throws DatabaseException {
        List<Posts> postsList = new ArrayList<Posts>();
        String sql = "SELECT * FROM POSTS ORDER BY CREATED_TIME DESC";
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                Posts p = new Posts();
                p.setPostId(rs.getInt("POST_ID"));
                p.setUserId(rs.getInt("USER_ID"));
                p.setPostType(rs.getString("POST_TYPE"));
                p.setTitle(rs.getString("POST_NAME"));
                p.setContent(rs.getString("DESCRIPTION"));
                p.setLikes(rs.getInt("LIKES"));
                p.setCommentsCount(rs.getInt("COMMENTS_COUNT"));
                p.setCreatedTime(rs.getTimestamp("CREATED_TIME"));
                p.setOriginalPostId(rs.getInt("ORIGINAL_POST_ID"));
                postsList.add(p);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load posts", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
        return postsList;
    }

    
    private void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}