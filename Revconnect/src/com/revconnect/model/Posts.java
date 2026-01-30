package com.revconnect.model;
import java.sql.Timestamp;

public class Posts {
	private int postId;
    private int userId;
    private String postType; // Maps to POST_TYPE
    private int originalPostId;
    private Timestamp createdTime; // Maps to CREATED_TIME
    private String title; // Maps to POST_NAME
    private String content; // Maps to DESCRIPTION
    private int likes; // Maps to LIKES
    private int commentsCount; // Maps to COMMENTS_COUNT

    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }
    public int getCommentsCount() { return commentsCount; }
    public void setCommentsCount(int commentsCount) { this.commentsCount = commentsCount; }
    public void setPostType(String postType) {this.postType = postType;}
    public String getPostType() {return postType;}
    public int getOriginalPostId() { return originalPostId; }
    public void setOriginalPostId(int originalPostId) { this.originalPostId = originalPostId;}
    public Timestamp getCreatedTime() {return createdTime;}
    public void setCreatedTime(Timestamp createdTime) {this.createdTime = createdTime;}
}