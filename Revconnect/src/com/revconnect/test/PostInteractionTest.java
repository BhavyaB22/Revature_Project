package com.revconnect.test;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.revconnect.service.PostService;

public class PostInteractionTest {

    private PostService postService;

    
    @BeforeClass
    public static void globalSetup() {
        System.out.println(">>> INITIALIZING REVCONNECT LOGIC TESTS <<<");
    }

   
    @Before
    public void setup() {
        postService = new PostService();
    }

    /* ================= POST VALIDATION TESTS ================= */

    @Test //  Basic @Test
    public void testLikeValidPost() {
        String result = postService.validateLike(5, 2); 
        assertEquals("SUCCESS", result);
    }

    @Test //  Testing boundary/invalid values
    public void testLikeInvalidPostID() {
        String result = postService.validateLike(5, -1);
        assertEquals("Invalid Post ID", result);
    }

    @Test //  Testing logic for non-existent data
    public void testCommentOnNonExistentPost() {
        String result = postService.validateComment(5, 999, "Great post!");
        assertEquals("Post not found", result);
    }

    /* ================= USER AVAILABILITY TESTS ================= */

    @Test // Normal flow
    public void testInteractionWithValidUser() {
        String result = postService.validateUser(5);
        assertEquals("SUCCESS", result);
    }

    @Test // 7. Null check (simulating session timeout)
    public void testInteractionWithNullUser() {
        String result = postService.validateUser(null);
        assertEquals("User must be logged in", result);
    }

    /* ================= CONTENT LOGIC TESTS ================= */

    @Test // String validation
    public void testEmptyCommentContent() {
        String result = postService.validateComment(5, 21, "");
        assertEquals("Comment content cannot be empty", result);
    }

    @Test(timeout = 500) // 9. Performance annotation: fails if logic takes > 500ms
    public void testFeedLoadingSpeed() {
        boolean accessible = postService.isFeedAccessible();
        assertTrue(accessible);
    }

    /* ================= ADVANCED BUSINESS LOGIC ================= */

    @Test //  Self-interaction logic
    public void testSelfLikeRestriction() {
        // User 8 owns Post 6 according to your console image
        String result = postService.validateLike(8, 6);
        assertEquals("You cannot like your own post", result);
    }

    @Test(expected = IllegalArgumentException.class) // 11. Exception annotation
    public void testInvalidUserParameter() {
        // Should throw exception if ID is 0
        postService.validateUser(0);
    }

    

    /* ================= CLEANUP ANNOTATIONS ================= */

    // @After: Cleanup after every single test
    @After
    public void tearDown() {
        postService = null;
    }

   
    @AfterClass
    public static void globalTearDown() {
        System.out.println(">>> ALL  LOGIC TESTS COMPLETED <<<");
    }
}