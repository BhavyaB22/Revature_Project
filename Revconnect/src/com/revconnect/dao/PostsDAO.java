package com.revconnect.dao;

import com.revconnect.config.DBConnection;
import com.revconnect.exception.*;
import com.revconnect.model.Posts; // Ensure this is Post (singular) or matches your model
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostsDAO {

    // --- CREATE ---
    public boolean createPost(int userId, String postName, String content) {
        String sql = "INSERT INTO POSTS (USER_ID, POST_NAME, DESCRIPTION, POST_TYPE) VALUES (?, ?, ?, 'Standard')";
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
                p.setPostName(rs.getString("POST_NAME"));
                p.setDescription(rs.getString("DESCRIPTION"));
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
    public boolean likePost(int postId, int userId) {
        String sql = "INSERT INTO LIKES (POST_ID, USER_ID) VALUES (?, ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, postId);
            pstmt.setInt(2, userId);
            if (pstmt.executeUpdate() > 0) {
                return updatePostCount(postId, "LIKES", 1);
            }
        } catch (SQLException e) { System.out.println("Already liked."); }
        finally { closeResources(conn, pstmt, null); }
        return false;
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

    // --- REPOST LOGIC ---
    public boolean repost(int originalPostId, int userId, String content) {
        String sql = "INSERT INTO POSTS (USER_ID, CONTENT, POST_TYPE, ORIGINAL_POST_ID, POST_NAME) VALUES (?, ?, 'Repost', ?, ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.setString(2, content);
            pstmt.setInt(3, originalPostId);
            pstmt.setString(4, "Shared Post");
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
        finally { closeResources(conn, pstmt, null); }
    }

    public boolean removeRepost(int originalPostId, int userId) {
        String sql = "DELETE FROM POSTS WHERE ORIGINAL_POST_ID = ? AND USER_ID = ? AND POST_TYPE = 'Repost'";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, originalPostId);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
        finally { closeResources(conn, pstmt, null); }
    }

    // --- HELPER METHODS ---
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
    
    //increase like count
    
    public String toggleLike(int postId, int userId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            
            // 1. Check if the user has already liked this specific post
            // We use 'POST_LIKES' to match your trigger setup
            String checkSql = "SELECT 1 FROM POST_LIKES WHERE POST_ID = ? AND USER_ID = ?";
            pstmt = conn.prepareStatement(checkSql);
            pstmt.setInt(1, postId);
            pstmt.setInt(2, userId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                return "Already liked.";
            } else {
                pstmt.close(); // Close the check statement

                // 2. Simple Insert. 
                // Trigger POST_LIKES_BIR will automatically:
                // - Generate the LIKE_ID
                // - Increment LIKES in the POSTS table
                String insertSql = "INSERT INTO POST_LIKES (POST_ID, USER_ID) VALUES (?, ?)";
                pstmt = conn.prepareStatement(insertSql);
                pstmt.setInt(1, postId);
                pstmt.setInt(2, userId);
                
                pstmt.executeUpdate();
                return "Post Liked!";
            }
        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        } finally {
            closeResources(conn, pstmt, rs);
        }
    }
    // add a comment 
    
    

    public boolean deletePost(int postId, int userId) {
        String sql = "DELETE FROM POSTS WHERE POST_ID = ? AND USER_ID = ?";
        Connection conn = null; PreparedStatement pstmt = null;
        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, postId); pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
        finally { closeResources(conn, pstmt, null); }
    }

    private Posts mapResultSetToPost(ResultSet rs) throws SQLException {
        Posts p = new Posts();
        p.setPostId(rs.getInt("POST_ID"));
        p.setUserId(rs.getInt("USER_ID"));
        p.setPostName(rs.getString("TITLE"));
        p.setDescription(rs.getString("CONTENT"));
        p.setLikes(rs.getInt("LIKES"));
        p.setCommentsCount(rs.getInt("COMMENTS_COUNT"));
        p.setPostType(rs.getString("POST_TYPE"));
        return p;
    }
    public boolean addComment(int postId, int userId, String text) {
        // We only insert. The Trigger handles the auto-increment and the counter update!
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
 // Add this inside PostsDAO.java
    public List<String> getLikesForPost(int postId) {
        List<String> likers = new ArrayList<String>();
        // Joins USERS and LIKES to get the usernames of people who liked the post
        String sql = "SELECT u.USERNAME FROM LIKES l JOIN USERS u ON l.USER_ID = u.USER_ID WHERE l.POST_ID = ?";
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
            e.printStackTrace();
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return likers;
    }

    public List<String> getCommentsForPost(int postId) {
        List<String> comments = new ArrayList<String>();
        // Added TO_CHAR to format the timestamp for better readability
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
                // New Format: [ID] Username (Time): Content
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
                p.setPostName(rs.getString("POST_NAME"));
                p.setDescription(rs.getString("DESCRIPTION"));
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

    /**
     * Example of how your Fetch method should look using your Posts.java fields
     */
   
    private void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}