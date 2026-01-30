package com.revconnect.app;

import java.util.*;
import com.revconnect.exception.*;
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
    
    private User loggedInUser = null;

    public static void main(String[] args) {
        new RevConnectApp().start();
    }

    // --- STARTUP ---
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
            }
        }
    }

    // --- AUTHENTICATION ---
    private void loginFlow() {
        System.out.print("Email: "); String e = sc.next();
        System.out.print("Pass: "); String p = sc.next();
        
        loggedInUser = userDAO.login(e, p);
        if (loggedInUser != null) {
            int count = notificationDAO.getUnreadCount(loggedInUser.getUserId());
            System.out.println("\nLogin Successful! Welcome, " + loggedInUser.getUsername());
            if (count > 0) {
                System.out.println(">>> You have " + count + " new notifications! <<<");
            }
            mainMenu();
        } else {
            System.out.println("Invalid credentials.");
        }
    }

    private void registrationMenu() {
        System.out.println("\n--- NEW ACCOUNT ---");
        System.out.println("1. Personal | 2. Creator | 3. Business | 4. Back");
        int choice = sc.nextInt();
        if (choice == 4) return;
        
        String type = (choice == 2) ? "ContentCreator" : (choice == 3 ? "Business" : "Personal");
        User u = new User();
        u.setUserType(type);
        
        String email;
        while (true) {
            System.out.print("Email: ");
            email = sc.next();
            
            // Call your AuthService
            String result = authService.validateEmail(email);
            
            if (!result.equals("SUCCESS")) {
                System.out.println("Error: " + result); // This prints "Invalid email..."
            } else if (userDAO.isEmailExists(email)) {
                System.out.println("Error: Email already registered.");
            } else {
                break; // Email is valid and unique, exit loop
            }
        }
        u.setEmail(email);

        System.out.print("Username: "); String uname = sc.next();
        if (userDAO.isUsernameExists(uname)) { System.out.println("Error: Username taken."); return; }
        u.setUsername(uname);
        
     // --- PASSWORD VALIDATION LOOP ---
        String password;
        while (true) {
            System.out.print("Password: ");
            password = sc.next();
            
            String passResult = authService.validatePassword(password);
            
            if (!passResult.equals("SUCCESS")) {
                System.out.println(" Error: " + passResult);
            } else {
                u.setPassword(password); // Save to the user object
                break; // Valid password, move to security question
            }
        }
        sc.nextLine();
        
        System.out.print("Security Question: "); u.setsQuestion(sc.nextLine());
        System.out.print("Answer: "); u.setsAnswer(sc.nextLine());
        u.setPrivacy("Public");

        if (userDAO.registerUser(u)) System.out.println("Account Created!");
    }

    // --- MAIN NAVIGATION ---
    private void mainMenu() {
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
                case 5: showNotificationFlow(); break;
                case 6: loggedInUser = null; break;
            }
        }
    }

    // --- PROFILE MODULE ---
    private void profileMenu() {
        while (true) {
            System.out.println("\n--- PROFILE ---");
            System.out.println("1. View My Profile\n2. Edit my profile \n3. Back");
            int choice = sc.nextInt();
            if (choice == 1) {
                viewProfile(loggedInUser.getUserId());
                
            } else if (choice == 2) {
            	sc.nextLine(); 
                
                System.out.println("--- Edit Profile Details ---");
                System.out.print("username: ");
                String name = sc.nextLine();
                
                System.out.print("Bio: ");
                String bio = sc.nextLine();
                
                System.out.print("Location: ");
                String loc = sc.nextLine();
                
                System.out.print("Website: ");
                String web = sc.nextLine();

                if (profileDAO.updateProfile(loggedInUser.getUserId(), name, bio, loc, web)) {
                    System.out.println("Success: Profile updated!");
                } else {
                    System.out.println("Error: Update failed.");
                }
              break;
            }
        }
    }

    // --- POSTS MODULE ---
    private void postMenu() {
        while (true) {
            System.out.println("\n--- MY POSTS ---");
            System.out.println("1. Create Post\n2. View My Posts\n3. Delete Post\n4. Back");
            System.out.print("Choice: ");
            int choice = sc.nextInt();

            if (choice == 1) {
                sc.nextLine(); // Clear buffer
                System.out.print("Title: "); 
                String title = sc.nextLine();
                System.out.print("Content: "); 
                String content = sc.nextLine();
                postsDAO.createPost(loggedInUser.getUserId(), title, content);
                System.out.println("Post created successfully!");

            } else if (choice == 2) {
                // 1. Fetch the list first
                List<Posts> myPosts = postsDAO.getPostsByUserId(loggedInUser.getUserId());

                // 2. Check if the list is empty
                if (myPosts == null || myPosts.isEmpty()) {
                    System.out.println(">> No posts yet! Start by creating one (Option 1).");
                } else {
                    System.out.println("\n--- YOUR POST ENTRIES ---");
                    for (Posts p : myPosts) {
                        System.out.println("[" + p.getPostId() + "] " + p.getPostName());
                    }
                }

            } else if (choice == 3) {
                System.out.print("Enter Post ID to Delete: ");
                int postId = sc.nextInt();
                boolean deleted = postsDAO.deletePost(postId, loggedInUser.getUserId());
                
                if (deleted) {
                    System.out.println("Post deleted successfully.");
                } else {
                    System.out.println("Error: Post ID not found or doesn't belong to you.");
                }

            } else if (choice == 4) {
                break;
            } else {
                System.out.println("Invalid choice. Try again.");
            }
        }
    }

    // --- FEED & INTERACTION ---
    private void showFeedFlow() {
        System.out.println("\n--- FEED ---");
        System.out.println("1. All Posts\n2. Users List\n3. Back");
        System.out.print("Choice: ");
        int feedChoice = sc.nextInt();

        if (feedChoice == 1) {
            List<Posts> feed = postsDAO.getFeed();
            for (Posts p : feed) {
                System.out.println("[" + p.getPostId() + "] " + p.getPostName() + " (Likes: " + p.getLikes() + ")");
                System.out.println("   > " + p.getDescription());
            }
            System.out.print("\nEnter Post ID to interact (0 to exit): ");
            int pid = sc.nextInt();
            if (pid != 0) interactWithPost(pid);

        } else if (feedChoice == 2) {
            List<User> users = userDAO.getAllUsers(); 
            System.out.println("\n--- REGISTERED USERS ---");
            for (User u : users) {
                // Self-exclusion check
                if (u.getUserId() != loggedInUser.getUserId()) {
                    System.out.println("ID: " + u.getUserId() + " | Username: " + u.getUsername() + " [" + u.getUserType() + "]");
                }
            }
            System.out.print("\nEnter User ID to view profile (0 to back): ");
            int targetId = sc.nextInt();
            
            // Validation: Cannot view/interact with yourself in the list
            if (targetId == loggedInUser.getUserId()) {
                System.out.println("You cannot view your own profile through the user list.");
            } else if (targetId != 0) {
                viewUserDetailFlow(targetId);
            }
        }
    }
    
    //viewuserslist
    private void viewUserDetailFlow(int targetId) {
        User targetUser = userDAO.getUserById(targetId);
        Profile p = profileDAO.getProfileByUserId(targetId);
        
        if (targetUser == null) return;

        System.out.println("\n--- " + targetUser.getUsername().toUpperCase() + "'S PROFILE ---");
        System.out.println("Type:     " + targetUser.getUserType());
        System.out.println("Bio:      " + (p.getBio() != null ? p.getBio() : "No bio set."));
        System.out.println("Location: " + (p.getLocation() != null ? p.getLocation() : "Not set."));
        System.out.println("Website:  " + (p.getWebsite() != null ? p.getWebsite() : "Not set."));

        // Logic for Friend vs Follow
        boolean isCreator = targetUser.getUserType().equalsIgnoreCase("ContentCreator") || 
                            targetUser.getUserType().equalsIgnoreCase("Business");
        
        // Check current state from NETWORKS table
        boolean isFollowing = networksDAO.isFollowing(loggedInUser.getUserId(), targetId);
        boolean isFriend = networksDAO.isFriend(loggedInUser.getUserId(), targetId);

        System.out.println("\n--- OPTIONS ---");
        if (!isFriend) {
            System.out.println("1. Add Friend");
        } else {
            System.out.println("1. Remove Friend");
        }

        if (isCreator) {
            System.out.println(isFollowing ? "2. Unfollow" : "2. Follow");
        }
        System.out.println("3. Back");
        System.out.print("Action: ");
        
        int act = sc.nextInt();
        if (act == 1) {
            if (!isFriend) {
                // STATUS will be 'PENDING'
                if (networksDAO.sendRequest(loggedInUser.getUserId(), targetId, "PENDING")) {
                    notificationDAO.createNotification(targetId, loggedInUser.getUserId(), "sent you a friend request", "CONNECT");
                    System.out.println("Friend request sent to " + targetUser.getUsername());
                }
            } else {
                networksDAO.removeConnection(loggedInUser.getUserId(), targetId);
                System.out.println("Friend removed.");
            }
        } else if (act == 2 && isCreator) {
            if (isFollowing) {
                networksDAO.removeConnection(loggedInUser.getUserId(), targetId); // Deletes 'FOLLOWING' row
                System.out.println("Unfollowed " + targetUser.getUsername());
            } else {
                // STATUS will be 'FOLLOWING'
                if (networksDAO.sendRequest(loggedInUser.getUserId(), targetId, "FOLLOWING")) {
                    notificationDAO.createNotification(targetId, loggedInUser.getUserId(), "started following you", "FOLLOW");
                    System.out.println("Now following " + targetUser.getUsername());
                }
            }
        }
    }

    private void interactWithPost(int pid) {
        System.out.println("1. Like\n2. Comment\n3. Back");
        int action = sc.nextInt();
        int ownerId = postsDAO.getOwnerIdByPostId(pid);

        if (action == 1) {
            // FIX: Use toggleLike instead of likePost
            String result = postsDAO.toggleLike(pid, loggedInUser.getUserId());
            System.out.println(result); // This will print "Post Liked!" or "Already liked."

            // Only notify the owner if it's a new like
            if ("Post Liked!".equals(result)) {
                notificationDAO.createNotification(ownerId, loggedInUser.getUserId(), "liked your post", "LIKE");
            }
        } else if (action == 2) {
        	sc.nextLine(); // Clear buffer
            System.out.print("Comment: ");
            String msg = sc.nextLine();
            
            // Call the DAO method
            if (postsDAO.addComment(pid, loggedInUser.getUserId(), msg)) {
                // Notify the post owner
                notificationDAO.createNotification(ownerId, loggedInUser.getUserId(), "commented on your post", "COMMENT");
                System.out.println("Commented successfully!");
            } else {
                System.out.println("Failed to add comment.");
            }
        }
    }

    // --- NETWORK MODULE ---
    private void showNetworkFlow() {
        while (true) {
            System.out.println("\n--- NETWORK ---");
            System.out.println("1. View Incoming Requests\n2. View My Connections\n3. Remove Connection\n4. View Followers\n5. View Following\n6. Back");
            System.out.print("Action: ");
            int choice = sc.nextInt();
            int myId = loggedInUser.getUserId();

            switch (choice) {
                case 1:
                    handleIncomingRequests(myId);
                    break;
                case 2:
                    displayList(networksDAO.getConnections(myId), "My Connections");
                    break;
                case 3:
                    // Logic to remove a connection
                    break;
                case 4:
                    displayList(networksDAO.getFollowers(myId), "Followers");
                    break;
                case 5:
                    displayList(networksDAO.getFollowing(myId), "Following");
                    break;
                case 6:
                    return;
            }
        }
    }

    private void handleIncomingRequests(int myId) {
        // 1. Fetch and display the list of pending requests
        List<String> requests = networksDAO.getIncomingRequests(myId);
        
        System.out.println("\n--- INCOMING FRIEND REQUESTS ---");
        if (requests.isEmpty()) {
            System.out.println("No pending requests.");
            return; // Exit the method early if nothing to show
        }

        for (String req : requests) {
            System.out.println("- " + req);
        }

        // 2. Ask user which ID they want to interact with
        System.out.print("\nEnter Sender ID to Accept/Reject (0 to skip): ");
        int sid = sc.nextInt();
        
        if (sid != 0) {
            System.out.print("1. Accept | 2. Reject: ");
            int res = sc.nextInt();
            
            if (res == 1) { 
                // Update status in NETWORKS table
                if (networksDAO.updateRequestStatus(myId, sid, "ACCEPTED")) {
                    // Notify the sender that you accepted
                    notificationDAO.createNotification(sid, myId, "accepted your friend request!", "ACCEPT");
                    System.out.println("Success! You are now connected.");
                } else {
                    System.out.println("Error: Could not update request.");
                }
            } else if (res == 2) {
                // Remove the pending request from NETWORKS table
                if (networksDAO.updateRequestStatus(myId, sid, "REJECTED")) {
                    System.out.println("Request rejected.");
                }
            } else {
                System.out.println("Invalid choice.");
            }
        }
    }

    private void searchAndConnect() {
        System.out.print("Search: ");
        String q = sc.next();
        List<Profile> results = profileDAO.searchByUsername(q);
        for (Profile p : results) System.out.println("ID: " + p.getUserId() + " | Name: " + p.getUsername());
        
        System.out.print("Select User ID (0 to cancel): ");
        int tid = sc.nextInt();
        if (tid == 0 || tid == loggedInUser.getUserId()) return;

        System.out.println("1. Send Request\n2. Follow\n3. Unfollow");
        int act = sc.nextInt();
        if (act == 1) {
            notificationDAO.createNotification(tid, loggedInUser.getUserId(), "sent a request", "CONNECT");
            System.out.println("Request sent!");
        } else if (act == 2) {
            if (networksDAO.followUser(loggedInUser.getUserId(), tid)) {
                notificationDAO.createNotification(tid, loggedInUser.getUserId(), "followed you", "FOLLOW");
                System.out.println("Followed!");
            }
        } else if (act == 3) {
            networksDAO.unfollowUser(loggedInUser.getUserId(), tid);
            System.out.println("Unfollowed.");
        }
    }

    // --- NOTIFICATIONS MODULE ---
    private void showNotificationFlow() {
        while (true) {
            System.out.println("\n--- NOTIFICATIONS ---");
            List<Notification> notifs = notificationDAO.getNotificationsForUser(loggedInUser.getUserId());
            
            if (notifs.isEmpty()) {
                System.out.println("Inbox empty.");
                break;
            }

            for (int i = 0; i < notifs.size(); i++) {
                Notification n = notifs.get(i);
                String msg = n.getSenderUsername() + " " + n.getMessage();
                System.out.println("[" + (i + 1) + "] " + msg);
            }

            notificationDAO.markAllAsRead(loggedInUser.getUserId());
            System.out.print("\nSelect number to view details (0 to exit): ");
            int choice = sc.nextInt();
            if (choice > 0 && choice <= notifs.size()) {
                Notification selected = notifs.get(choice - 1);
                if (selected.getType().contains("LIKE") || selected.getType().contains("COMMENT")) {
                    viewPostDetails(selected.getPostId(), loggedInUser.getUserId());
                } else {
                    viewProfile(selected.getSenderId());
                }
            } else break;
        }
    }

    
    private void displayList(List<String> list, String title) {
        System.out.println("\n--- " + title + " ---");
        if (list.isEmpty()) System.out.println("Nothing to show.");
        else for (String s : list) System.out.println("- " + s);
    }

 // Update the signature to accept TWO parameters: postId and viewerId
    private void viewPostDetails(int postId, int viewerId) {
        try {
            // 1. Fetch the post from the DAO
            Posts post = postsDAO.getPostById(postId);

            if (post != null) {
                
                System.out.println("   POST: " + post.getPostName());
                System.out.println("==============================");
                System.out.println("Description: " + post.getDescription());
                System.out.println("Type: " + post.getPostType());
                System.out.println("Likes: " + post.getLikes());
                System.out.println("Posted on: " + post.getCreatedTime());
                System.out.println("==============================");
                
                
                System.out.println("Logged in as User ID: " + viewerId);
            } else {
                System.out.println("Error: Could not find post details.");
            }
        } catch (DatabaseException e) {
            System.out.println("Database Error: " + e.getMessage());
        }
    }

    private void viewProfile(int userId) {
        Profile p = profileDAO.getProfileByUserId(userId);
        if (p == null || (p.getBio() == null || p.getBio().equals("-"))) {
            System.out.println("\n--- PROFILE ---");
            System.out.println("Status: No profile details found. Please edit your profile to add info!");
        } else {
            System.out.println("\n--- " + loggedInUser.getUsername().toUpperCase() + " ---");
            System.out.println("Bio: " + p.getBio());
            System.out.println("Location: " + (p.getLocation() != null ? p.getLocation() : "Not set"));
            System.out.println("Website: " + (p.getWebsite() != null ? p.getWebsite() : "Not set"));
        
        }
    }
}