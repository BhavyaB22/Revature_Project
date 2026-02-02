package com.revconnect.app;

import java.util.*;
import com.revconnect.exceptions.*;
import com.revconnect.model.*;
import com.revconnect.service.*;
import com.revconnect.dao.*;

public class RevConnectApp {
    private Scanner sc = new Scanner(System.in);
    private UserDAO userDAO = new UserDAO();
    private ProfileDAO profileDAO = new ProfileDAO();
    private NotificationDAO notificationDAO = new NotificationDAO();
    private PostsDAO postsDAO = new PostsDAO(); 
    private NetworksDAO networksDAO = new NetworksDAO();
    private AuthService authService = new AuthService();
    private ProfileService profileService = new ProfileService();
    
    private User loggedInUser = null;

    public static void main(String[] args) {
        new RevConnectApp().start();
    }

    // --- STARTUP WITH GLOBAL ERROR HANDLING ---
    public void start() {
        while (true) {
            try {
                System.out.println("\n===== REVCONNECT =====");
                System.out.println("1. Register\n2. Login\n3. Exit");
                System.out.print("Choice: ");
                int choice = sc.nextInt();
                switch (choice) {
                    case 1: registrationMenu(); break;
                    case 2: loginFlow(); break;
                    case 3: System.out.println("Goodbye!"); System.exit(0);
                    default: System.out.println("Invalid choice!");
                }
            } catch (InputMismatchException e) {
                System.out.println("Error: Please enter a number.");
                sc.next(); 
            } catch (DatabaseException e) {
                System.out.println("\n[SYSTEM ERROR] " + e.getMessage());
            }
        }
    }

    // --- AUTHENTICATION MODULE ---
    private void loginFlow() throws DatabaseException {
        System.out.print("Email: "); String e = sc.next();
        System.out.print("Pass: "); String p = sc.next();
        
        loggedInUser = userDAO.login(e, p);
        
        if (loggedInUser != null) {
            System.out.println("\nLogin Successful! Welcome, " + loggedInUser.getUsername());
            mainMenu();
        } else {
            System.out.println("\n[!] Invalid credentials.");
            System.out.println("1. Try Again | 2. Forgot Password?");
            int choice = sc.nextInt();
            if (choice == 2) forgotPasswordFlow(e);
        }
    }

    private void forgotPasswordFlow(String email) throws DatabaseException {
        User user = userDAO.getUserByEmail(email);
        if (user == null) {
            System.out.println("Error: No account found.");
            return;
        }
        System.out.println("\n--- SECURITY CHECK ---");
        System.out.println("Question: " + user.getsQuestion());
        System.out.print("Your Answer: ");
        sc.nextLine(); 
        String inputAnswer = sc.nextLine();

        if (inputAnswer.equalsIgnoreCase(user.getsAnswer())) {
            System.out.print("Enter New Password: ");
            String newPass = sc.next();
            if (userDAO.updatePassword(user.getUserId(), newPass)) {
                System.out.println("Success! Password updated.");
            }
        } else {
            System.out.println("Incorrect answer.");
        }
    }

    private void registrationMenu() throws DatabaseException {
        System.out.println("\n--- NEW ACCOUNT ---");
        System.out.println("1. Personal | 2. Creator | 3. Business | 4. Back");
        int choice = sc.nextInt();
        if (choice == 4) return;
        
        String type = (choice == 2) ? "ContentCreator" : (choice == 3 ? "Business" : "Personal");
        User u = new User();
        u.setUserType(type);
        
        System.out.print("Email: "); String email = sc.next();
        if (userDAO.isEmailExists(email)) { System.out.println("Error: Email exists."); return; }
        u.setEmail(email);

        System.out.print("Username: "); String uname = sc.next();
        if (userDAO.isUsernameExists(uname)) { System.out.println("Error: Taken."); return; }
        u.setUsername(uname);
        
        System.out.print("Password: "); u.setPassword(sc.next());
        sc.nextLine();
        System.out.print("Security Question: "); u.setsQuestion(sc.nextLine());
        System.out.print("Answer: "); u.setsAnswer(sc.nextLine());
        u.setPrivacy("Public");

        if (userDAO.registerUser(u)) System.out.println("Account Created!");
    }

    // --- NAVIGATION ---
    private void mainMenu() throws DatabaseException {
        while (loggedInUser != null) {
            int count = notificationDAO.getUnreadCount(loggedInUser.getUserId());
            String notifLabel = (count > 0) ? "Notifications (" + count + ")" : "Notifications";

            System.out.println("\n--- MAIN MENU ---");
            System.out.println("1. Profile\n2. My Posts\n3. Global Feed\n4. Network\n5. " + notifLabel + "\n6. Logout");
            System.out.print("Action: ");
            int choice = sc.nextInt();
            switch (choice) {
                case 1: profileMenu(); break;
                case 2: postMenu(); break;
                case 3: showFeedFlow(); break;
                case 4: showNetworkFlow(); break;
                case 5: showNotificationsFlow(); break;
                case 6: loggedInUser = null; break;
            }
        }
    }

    // --- PROFILE MODULE ---
    private void profileMenu() throws DatabaseException {
        profileDAO.ensureProfileExists(loggedInUser.getUserId(), loggedInUser.getUsername());
        String role = loggedInUser.getUserType();
        while (true) {
            System.out.println("\n--- PROFILE MENU (" + role + ") ---");
            System.out.println("1. View Profile\n2. Edit Profile\n3. Back");
            int choice = sc.nextInt();
            sc.nextLine(); 

            if (choice == 1) viewProfile(loggedInUser.getUserId());
            else if (choice == 2) handleEditProfile(role);
            else break;
        }
    }

    private void viewProfile(int userId) throws DatabaseException {
        Profile p = profileDAO.getProfileByUserId(userId);
        User u = userDAO.getUserById(userId);
        if (u == null || p == null) return;

        System.out.println("\n--- PROFILE DETAILS ---");
        System.out.println("Username : " + p.getUsername());
        System.out.println("Type     : " + u.getUserType());
        System.out.println("Bio      : " + p.getBio());
        System.out.println("Location : " + p.getLocation());
        System.out.println("Website  : " + (p.getWebsite() != null ? p.getWebsite() : "N/A"));
    }

    private void handleEditProfile(String role) throws DatabaseException {
        if (role.equalsIgnoreCase("Personal")) {
            System.out.print("Name: "); String n = sc.nextLine();
            System.out.print("Bio: "); String b = sc.nextLine();
            System.out.print("Location: "); String l = sc.nextLine();
            System.out.print("Website: "); String w = sc.nextLine();
            if (profileDAO.updateProfile(loggedInUser.getUserId(), n, b, l, w)) 
                System.out.println("Profile Updated!");
        } else {
            updateBusinessFromSearch(loggedInUser.getUserId());
        }
    }

    private void updateBusinessFromSearch(int bId) throws DatabaseException {
        System.out.println("\n--- Update Business Details ---");
        System.out.print("Entity Name: "); String n = sc.nextLine();
        System.out.print("Category: "); String cat = sc.nextLine();
        System.out.print("Detailed Bio: "); String bio = sc.nextLine();
        System.out.print("Address: "); String add = sc.nextLine();
        System.out.print("Contact: "); String con = sc.nextLine();
        System.out.print("Website: "); String web = sc.nextLine();
        System.out.print("Hours: "); String hr = sc.nextLine();

        if (profileService.updateEnhancedProfile(bId, n, cat, bio, add, con, web, hr))
            System.out.println("[SUCCESS] Details updated!");
    }

    // --- POSTS MODULE ---
 // --- POSTS MODULE ---
    private void postMenu() throws DatabaseException {
        while (true) {
            System.out.println("\n--- MY POSTS ---");
            System.out.println("1. Create\n2. View My Posts\n3. Delete\n4. Back");
            System.out.print("Choice: ");
            int choice = sc.nextInt();
            sc.nextLine(); // Clear buffer after nextInt()

            if (choice == 1) {
                System.out.print("Title: "); String t = sc.nextLine();
                System.out.print("Content: "); String c = sc.nextLine();
                postsDAO.createPost(loggedInUser.getUserId(), t, c);
                System.out.println("Created!");
            } else if (choice == 2) {
                List<Posts> myPosts = postsDAO.getPostsByUserId(loggedInUser.getUserId());
                
                // NEW: Check if list is empty before showing the prompt
                if (myPosts.isEmpty()) {
                    System.out.println("\n[!] You have no posts yet.");
                } else {
                    System.out.println("\n--- YOUR POST ENTRIES ---");
                    for (Posts p : myPosts) {
                        System.out.println("[" + p.getPostId() + "] " + p.getTitle());
                    }
                    
                    System.out.print("\nView Post ID (0 to back): ");
                    int pid = sc.nextInt();
                    sc.nextLine(); // Clear buffer
                    
                    if (pid != 0) {
                        viewPostDetails(pid, loggedInUser.getUserId());
                    }
                }
            } else if (choice == 3) {
                deletePostFlow();
            } else if (choice == 4) {
                break;
            }
        }
    }
    private void viewPostDetails(int postId, int viewerId) throws DatabaseException {
        Posts post = postsDAO.getPostById(postId);
        if (post != null) {
            System.out.println("\n----------------------------");
            System.out.println("TITLE   : " + post.getTitle());
            System.out.println("CONTENT : " + post.getContent());
            System.out.println("LIKES   : 🧡 " + post.getLikes());
            System.out.println("COMMENTS: 💬 " + post.getCommentsCount());
            System.out.println("----------------------------");
            
            // Fetch and display the actual comment list
            List<String> commentList = postsDAO.getCommentsByPostId(postId);
            if (commentList.isEmpty()) {
                System.out.println("(No comments yet)");
            } else {
                for (String comment : commentList) {
                    System.out.println("  > " + comment);
                }
            }
            System.out.println("----------------------------");
        } else {
            System.out.println("\n[!] Error: Post not found.");
        }
    }

    private void deletePostFlow() throws DatabaseException {
        List<Posts> myPosts = postsDAO.getPostsByUserId(loggedInUser.getUserId());
        
        if (myPosts.isEmpty()) {
            System.out.println("\n[!] You have no posts to delete.");
            return;
        }
        
        System.out.println("\n--- SELECT POST TO DELETE ---");
        for (int i = 0; i < myPosts.size(); i++) {
            // We show 1, 2, 3... for easier user selection
            System.out.println("[" + (i + 1) + "] " + myPosts.get(i).getTitle());
        }
        
        System.out.print("\nDelete selection (0 to cancel): ");
        int choice = sc.nextInt();
        sc.nextLine(); // CRITICAL: Clear the newline buffer
        
        if (choice > 0 && choice <= myPosts.size()) {
            // Map the user's list choice (1, 2, 3) back to the Database Post ID
            int actualPostId = myPosts.get(choice - 1).getPostId();
            
            System.out.print("Are you sure? (Y/N): ");
            String confirm = sc.nextLine();
            
            if (confirm.equalsIgnoreCase("Y")) {
                if (postsDAO.deletePost(actualPostId, loggedInUser.getUserId())) {
                    System.out.println("[SUCCESS] Post removed.");
                } else {
                    System.out.println("[!] Failed to delete. Database error.");
                }
            } else {
                System.out.println("Deletion cancelled.");
            }
        } else if (choice != 0) {
            System.out.println("[!] Invalid selection.");
        }
    }
  
   
 // --- VIEW USER DETAIL FLOW ---
    private void viewUserDetailFlow(int targetId) {
        try {
            User targetUser = userDAO.getUserById(targetId);
            Profile p = profileDAO.getProfileByUserId(targetId);
            if (targetUser == null) return;

            System.out.println("\n--- " + targetUser.getUsername().toUpperCase() + " ---");
            System.out.println("Type: " + targetUser.getUserType());
            System.out.println("Bio: " + (p.getBio() != null ? p.getBio() : "No bio."));
            
            boolean isCreator = !targetUser.getUserType().equalsIgnoreCase("Personal");
            boolean isFollowing = networksDAO.isFollowing(loggedInUser.getUserId(), targetId);
            boolean isFriend = networksDAO.isFriend(loggedInUser.getUserId(), targetId);

            System.out.println("\n1. " + (isFriend ? "Remove Friend" : "Add Friend"));
            if (isCreator) System.out.println("2. " + (isFollowing ? "Unfollow" : "Follow"));
            System.out.println("3. Back");
            
            int act = sc.nextInt();
            if (act == 1) {
                if (!isFriend) {
                    // Logic: Handle the custom NetworkException
                    if (networksDAO.sendRequest(loggedInUser.getUserId(), targetId, "PENDING")) {
                        notificationDAO.createNotification(targetId, loggedInUser.getUserId(), "sent a friend request", "CONNECT");
                        System.out.println("Request sent!");
                    }
                } else {
                    networksDAO.removeConnection(loggedInUser.getUserId(), targetId);
                    System.out.println("Connection removed.");
                }
            } else if (act == 2 && isCreator) {
                if (isFollowing) {
                    networksDAO.removeConnection(loggedInUser.getUserId(), targetId);
                    System.out.println("Unfollowed.");
                } else {
                    networksDAO.sendRequest(loggedInUser.getUserId(), targetId, "FOLLOWING");
                    notificationDAO.createNotification(targetId, loggedInUser.getUserId(), "followed you", "FOLLOW");
                    System.out.println("Following!");
                }
            }
        } catch (NetworkException e) {
            // This catches "You cannot connect with yourself" or "Already connected"
            System.out.println("\n[!] Network Error: " + e.getMessage());
        } catch (DatabaseException e) {
            System.out.println("\n[!] Database Error: " + e.getMessage());
        }
    }

 // --- FEED & INTERACTION MODULE ---
    private void showFeedFlow() throws DatabaseException {
        System.out.println("\n--- FEED ---");
        System.out.println("1. View Posts");
        System.out.println("2. Search Users");
        System.out.println("3. Back");
        System.out.print("Choice: ");
        
        int choice = sc.nextInt();
        sc.nextLine(); // Clear buffer

        if (choice == 1) {
            // This now pulls the post with aggregated counts
            List<Posts> feed = postsDAO.getFeed();
            
            if (feed.isEmpty()) {
                System.out.println("\n[!] The feed is currently empty.");
                return;
            }

            System.out.println("\n--- GLOBAL POSTS ---");
            for (Posts p : feed) {
                // FORMAT: [ID] Title (By ID: X) | ❤️ Count | 💬 Count
                System.out.printf("[%d] %s (By ID: %d) | ❤️ %d | 💬 %d\n", 
                    p.getPostId(), 
                    p.getTitle(), 
                    p.getUserId(), 
                    p.getLikes(), 
                    p.getCommentsCount());
            }

            System.out.print("\nEnter Post ID to interact (0 to back): ");
            int pid = sc.nextInt();
            sc.nextLine(); // Clear buffer
            
            if (pid != 0) {
                interactWithPost(pid);
            }

        } else if (choice == 2) {
            List<User> users = userDAO.getAllUsers();
            
            System.out.println("\n--- REGISTERED USERS ---");
            for (User u : users) {
                if (u.getUserId() != loggedInUser.getUserId()) {
                    System.out.println("ID: " + u.getUserId() + " | Username: " + u.getUsername() + " [" + u.getUserType() + "]");
                }
            }

            System.out.print("\nEnter User ID to view profile (0 to back): ");
            int tid = sc.nextInt();
            sc.nextLine(); // Clear buffer
            
            if (tid != 0) {
                viewUserDetailFlow(tid);
            }
        }
    }

    private void interactWithPost(int pid) throws DatabaseException {
        Posts post = postsDAO.getPostById(pid);
        if (post == null) {
            System.out.println("[!] Post not found.");
            return;
        }

        int ownerId = postsDAO.getOwnerIdByPostId(pid);

        System.out.println("\n--- INTERACTING WITH:"+pid+"---");
        System.out.println("1. Like/Unlike | 2. Comment | 3. Back");
        System.out.print("Action: ");
        int action = sc.nextInt();
        sc.nextLine(); // Clear buffer

        if (action == 1) {
            // STEP 1: Check if already liked
            boolean alreadyLiked = postsDAO.hasUserLikedPost(loggedInUser.getUserId(), pid);

            if (alreadyLiked) {
                System.out.print("You have already liked this post. Unlike it? (Y/N): ");
                String confirm = sc.nextLine();
                if (!confirm.equalsIgnoreCase("Y")) {
                    System.out.println("Unliked.");
                    return;
                }
            }

            // STEP 2: Perform the toggle
            String result = postsDAO.toggleLike(loggedInUser.getUserId(), pid);
            System.out.println(result);

            // STEP 3: Notify owner only if it was a "Like" and not "Unlike"
            if (result.contains("Liked") && ownerId != loggedInUser.getUserId()) {
                notificationDAO.createNotification(ownerId, loggedInUser.getUserId(), 
                    "liked your post: " + post.getTitle(), "LIKE");
            }

        } else if (action == 2) {
            // STEP 1: Check if already commented
            boolean alreadyCommented = postsDAO.hasUserCommented(loggedInUser.getUserId(), pid);

            if (alreadyCommented) {
                System.out.println("You have already commented on this post.");
                System.out.print("Do you want to add another or remove existing? (1: Add, 2: Remove, 3: Cancel): ");
                int subChoice = sc.nextInt();
                sc.nextLine(); // Clear buffer

                if (subChoice == 2) {
                    if (postsDAO.deleteUserComment(loggedInUser.getUserId(), pid)) {
                        System.out.println("Comment removed.");
                    }
                    return;
                } else if (subChoice == 3) {
                    return;
                }
            }

            // STEP 2: Add comment
            System.out.print("Write your comment: ");
            String msg = sc.nextLine();
            
            if (postsDAO.addComment(pid, loggedInUser.getUserId(), msg)) {
                System.out.println("Comment added!");
                if (ownerId != loggedInUser.getUserId()) {
                    notificationDAO.createNotification(ownerId, loggedInUser.getUserId(), 
                        "commented on your post: " + post.getTitle(), "COMMENT");
                }
            }
        }
    }
    // --- NETWORK & NOTIFICATIONS ---
    private void showNetworkFlow() throws DatabaseException {
        System.out.println("\n1. Requests\n2. Connections\n3. Back");
        System.out.print("Choice: ");
        int c = sc.nextInt();
        sc.nextLine(); // FIX: Clear buffer

        if (c == 1) handleIncomingRequests(loggedInUser.getUserId());
        else if (c == 2) {
            List<String> connections = networksDAO.getConnections(loggedInUser.getUserId());
            if(connections.isEmpty()) {
                System.out.println("No connections found.");
            } else {
                for (String s : connections) System.out.println("- " + s);
            }
        }
    }

    private void handleIncomingRequests(int myId) throws DatabaseException {
        // 1. Fetch the requests
        List<String> requests = networksDAO.getIncomingRequests(myId);

        // 2. Check if empty immediately
        if (requests.isEmpty()) {
            System.out.println("\n[!] You have no pending connection requests.");
            return; // Exit early
        }

        System.out.println("\n--- PENDING REQUESTS ---");
        for (String req : requests) {    
            System.out.println(req);
        }

        System.out.print("\nEnter Sender ID to Accept (0 to skip): ");
        int sid = sc.nextInt();
        sc.nextLine();

        if (sid != 0) {
            
            if (networksDAO.updateRequestStatus(myId, sid, "ACCEPTED")) {
                notificationDAO.createNotification(sid, myId, "accepted your request", "ACCEPT");
                System.out.println("[SUCCESS] Connection accepted!");
            } else {
                System.out.println("[!] Failed to accept. Invalid ID or request expired.");
            }
        }
    }

    private void showNotificationsFlow() throws DatabaseException {
        List<Notification> list = notificationDAO.getNotificationsForUser(loggedInUser.getUserId());
        
        if (list.isEmpty()) {
            System.out.println("\nNo new notifications.");
        } else {
            for (Notification n : list) System.out.println("> " + n.getMessage());
        }
        
        System.out.println("\n1. Clear All | 0. Back");
        System.out.print("Choice: ");
        int choice = sc.nextInt();
        sc.nextLine(); // FIX: Clear buffer

        if (choice == 1) {
            notificationDAO.deleteAllNotifications(loggedInUser.getUserId());
            System.out.println("Notifications cleared.");
        }
    }
}