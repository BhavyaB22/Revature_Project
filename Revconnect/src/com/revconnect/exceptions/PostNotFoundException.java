package com.revconnect.exceptions;

// Custom exception for when a Post ID is typed but doesn't exist in the DB
public class PostNotFoundException extends Exception {
    private int postId;

    public PostNotFoundException(int postId) {
        super("Post with ID " + postId + " was not found in the database.");
        this.postId = postId;
    }

    public int getPostId() {
        return postId;
    }
}