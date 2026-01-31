package com.revconnect.service;

import com.revconnect.dao.PostsDAO;
import com.revconnect.model.Posts;
import java.util.List;

public class PostService {
    private PostsDAO postsDAO = new PostsDAO();

    public void createPost(int userId, String title, String content) {
        postsDAO.createPost(userId, title, content);
    }

    public List<Posts> getMyPosts(int userId) {
        return postsDAO.getPostsByUserId(userId);
    }

    public boolean deletePost(int postId, int userId) {
        return postsDAO.deletePost(postId, userId);
    }

    // This fulfills "view likes and comments for my posts"
    public void viewPostStats(int postId) {
        Posts post = postsDAO.getPostById(postId);
        if (post != null) {
            System.out.println("\n--- POST DETAILS ---");
            System.out.println("Title: " + post.getTitle());
            System.out.println("Likes: " + post.getLikes());
            // Here you can call a method to fetch comments if you have a Comments table
            System.out.println("Comments: (Check notifications for latest activity)");
        }
    }
}