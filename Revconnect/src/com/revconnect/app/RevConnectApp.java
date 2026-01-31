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
    private ProfileService profileService = new ProfileService(); // Updated service
    
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
            System.out.println("\nLogin Successful! Welcome, " + loggedInUser.getUsername());
            mainMenu();
        } else {
            System.out.println("\n[!] Invalid credentials.");
            System.out.println("1. Try Again");
            System.out.println("2. Forgot Password? (Verify via Security Question)");
            System.out.print("Choice: ");
            
            int choice = sc.nextInt();
            if (choice == 2) {
                forgotPasswordFlow(e); // Passing the email they just typed
            }
        }
    }

    private void forgotPasswordFlow(String email) {
        // 1. Fetch user data
        User user = userDAO.getUserByEmail(email);
        
        if (user == null) {
            System.out.println("Error: No account found with that email.");
            return;
        }

        // 2. Security Verification
        System.out.println("\n--- SECURITY CHECK ---");
        System.out.println("Question: " + user.getsQuestion());
        System.out.print("Your Answer: ");
        sc.nextLine(); // Consume leftover newline
        String inputAnswer = sc.nextLine();

        if (inputAnswer.equalsIgnoreCase(user.getsAnswer())) {
            System.out.println("Identity Verified!");
            
            // 3. Update Password
            System.out.print("Enter New Password: ");
            String newPass = sc.next();
            
            if (userDAO.updatePassword(user.getUserId(), newPass)) {
                System.out.println("Success! Your password has been updated.");
            } else {
                System.out.println("Error: Could not update password in database.");
            }
        } else {
            System.out.println("Incorrect answer. Recovery aborted.");
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
            String result = authService.validateEmail(email);
            if (!result.equals("SUCCESS")) {
                System.out.println("Error: " + result);
            } else if (userDAO.isEmailExists(email)) {
                System.out.println("Error: Email already registered.");
            } else {
                break;
            }
        }
        u.setEmail(email);

        System.out.print("Username: "); String uname = sc.next();
        if (userDAO.isUsernameExists(uname)) { System.out.println("Error: Username taken."); return; }
        u.setUsername(uname);
        
        String password;
        while (true) {
            System.out.print("Password: ");
            password = sc.next();
            String passResult = authService.validatePassword(password);
            if (!passResult.equals("SUCCESS")) {
                System.out.println(" Error: " + passResult);
            } else {
                u.setPassword(password);
                break;
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
                case 5: showNotificationsFlow(); break;
                case 6: loggedInUser = null; break;
            }
        }
    }

    // --- PROFILE MODULE (UPDATED WITH ROLE LOGIC) ---
   

    // --- POSTS MODULE ---
    private void postMenu() {
        while (true) {
            System.out.println("\n--- MY POSTS ---");
            System.out.println("1. Create Post\n2. View My Posts (Likes/Comments)\n3. Delete Post\n4. Back");
            System.out.print("Choice: ");
            int choice = sc.nextInt();

            if (choice == 1) {
                sc.nextLine(); 
                System.out.print("Title: "); String title = sc.nextLine();
                System.out.print("Content: "); String content = sc.nextLine();
                postsDAO.createPost(loggedInUser.getUserId(), title, content);
                System.out.println("Post created successfully!");
            } else if (choice == 2) {
                List<Posts> myPosts = postsDAO.getPostsByUserId(loggedInUser.getUserId());
                if (myPosts.isEmpty()) {
                    System.out.println(">> No posts yet!");
                } else {
                    for (Posts p : myPosts) {
                        System.out.println("[" + p.getPostId() + "] " + p.getTitle() + " | Likes: " + p.getLikes());
                    }
                    System.out.print("Enter Post ID to view details (0 to back): ");
                    int pid = sc.nextInt();
                    if(pid != 0) viewPostDetails(pid, loggedInUser.getUserId());
                }
            } else if (choice == 3) {
                deletePostFlow();
            } else break;
        }
    }

    // --- FEED & INTERACTION ---
    private void showFeedFlow() {
        System.out.println("\n--- FEED ---");
        System.out.println("1. All Posts\n2. Users List\n3. Back");
        int feedChoice = sc.nextInt();

        if (feedChoice == 1) {
            List<Posts> feed = postsDAO.getFeed();
            for (Posts p : feed) {
                System.out.println("[" + p.getPostId() + "] " + p.getTitle() + " (Likes: " + p.getLikes() + ")");
                System.out.println("   > " + p.getContent());
            }
            System.out.print("\nEnter Post ID to interact (0 to exit): ");
            int pid = sc.nextInt();
            if (pid != 0) interactWithPost(pid);
        } else if (feedChoice == 2) {
            List<User> users = userDAO.getAllUsers(); 
            for (User u : users) {
                if (u.getUserId() != loggedInUser.getUserId()) {
                    System.out.println("ID: " + u.getUserId() + " | Username: " + u.getUsername() + " [" + u.getUserType() + "]");
                }
            }
            System.out.print("\nEnter User ID to view profile (0 to back): ");
            int targetId = sc.nextInt();
            if (targetId != 0 && targetId != loggedInUser.getUserId()) viewUserDetailFlow(targetId);
        }
    }

    private void viewUserDetailFlow(int targetId) {
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
    }

    private void interactWithPost(int pid) {
        System.out.println("1. Like\n2. Comment\n3. Back");
        int action = sc.nextInt();
        int ownerId = postsDAO.getOwnerIdByPostId(pid);

        if (action == 1) {
            String result = postsDAO.toggleLike(pid, loggedInUser.getUserId());
            System.out.println(result);
            if ("Post Liked!".equals(result)) {
                notificationDAO.createNotification(ownerId, loggedInUser.getUserId(), "liked your post", "LIKE");
            }
        } else if (action == 2) {
            sc.nextLine();
            System.out.print("Comment: ");
            String msg = sc.nextLine();
            if (postsDAO.addComment(pid, loggedInUser.getUserId(), msg)) {
                notificationDAO.createNotification(ownerId, loggedInUser.getUserId(), "commented on your post", "COMMENT");
                System.out.println("Commented!");
            }
        }
    }

    // --- NETWORK MODULE ---
    private void showNetworkFlow() {
        while (true) {
            System.out.println("\n--- NETWORK ---");
            System.out.println("1. Incoming Requests\n2. Connections\n3. Remove Connection\n4. Followers\n5. Following\n6. Back");
            int choice = sc.nextInt();
            int myId = loggedInUser.getUserId();

            switch (choice) {
                case 1: handleIncomingRequests(myId); break;
                case 2: displayList(networksDAO.getConnections(myId), "Connections"); break;
                case 3: removeConnectionFlow(); break;
                case 4: displayList(networksDAO.getFollowers(myId), "Followers"); break;
                case 5: displayList(networksDAO.getFollowing(myId), "Following"); break;
                case 6: return;
            }
        }
    }

    private void handleIncomingRequests(int myId) {
        List<String> requests = networksDAO.getIncomingRequests(myId);
        if (requests.isEmpty()) { System.out.println("No requests."); return; }
        
        for (String req : requests) System.out.println("- " + req);
        System.out.print("Enter Sender ID to Accept (0 to skip): ");
        int sid = sc.nextInt();
        if (sid != 0) {
            System.out.print("1. Accept | 2. Reject: ");
            int res = sc.nextInt();
            String status = (res == 1) ? "ACCEPTED" : "REJECTED";
            if (networksDAO.updateRequestStatus(myId, sid, status)) {
                if(res == 1) notificationDAO.createNotification(sid, myId, "accepted your request!", "ACCEPT");
                System.out.println("Done!");
            }
        }
    }

    private void showNotificationsFlow() {
        int myId = loggedInUser.getUserId();
        List<Notification> list = notificationDAO.getNotificationsForUser(myId);
        System.out.println("\n--- NOTIFICATIONS ---");
        if (list.isEmpty()) { System.out.println("Clear!"); return; }

        for (int i = 0; i < list.size(); i++) System.out.println("[" + (i + 1) + "] " + list.get(i).getMessage());
        System.out.println("[" + (list.size() + 1) + "] Clear All | [0] Back");
        
        int choice = sc.nextInt();
        if (choice == list.size() + 1) notificationDAO.deleteAllNotifications(myId);
        else if (choice > 0 && choice <= list.size()) notificationDAO.deleteNotification(list.get(choice-1).getNotifId());
    }

    private void displayList(List<String> list, String title) {
        System.out.println("\n--- " + title + " ---");
        if (list.isEmpty()) System.out.println("Empty.");
        else for (String s : list) System.out.println("- " + s);
    }

    private void viewPostDetails(int postId, int viewerId) {
        Posts post = postsDAO.getPostById(postId);
        if (post != null) {
            System.out.println("\n--- POST DETAILS ---");
            System.out.println("Title: " + post.getTitle());
            System.out.println("Content: " + post.getContent());
            System.out.println("Likes: " + post.getLikes());
            System.out.println("Date: " + post.getCreatedTime());
        }
    }

    private void deletePostFlow() {
        List<Posts> myPosts = postsDAO.getPostsByUserId(loggedInUser.getUserId());
        if (myPosts.isEmpty()) return;
        for (int i = 0; i < myPosts.size(); i++) System.out.println("[" + (i + 1) + "] " + myPosts.get(i).getTitle());
        System.out.print("Select number to delete: ");
        int choice = sc.nextInt();
        if (choice > 0 && choice <= myPosts.size()) {
            postsDAO.deletePost(myPosts.get(choice - 1).getPostId(), loggedInUser.getUserId());
            System.out.println("Deleted.");
        }
    }

    private void removeConnectionFlow() {
        List<User> connections = networksDAO.getAcceptedConnections(loggedInUser.getUserId());
        if (connections.isEmpty()) return;
        for (int i = 0; i < connections.size(); i++) System.out.println("[" + (i + 1) + "] " + connections.get(i).getUsername());
        System.out.print("Select to remove: ");
        int choice = sc.nextInt();
        if (choice > 0 && choice <= connections.size()) {
            networksDAO.removeConnection(loggedInUser.getUserId(), connections.get(choice - 1).getUserId());
            System.out.println("Removed.");
        }
    }

 // --- UPDATED VIEW PROFILE LOGIC ---
    private void viewProfile(int userId) {
        Profile p = profileDAO.getProfileByUserId(userId);
        User u = userDAO.getUserById(userId); // Needed to check user type

        if (u == null || p == null) {
            System.out.println("Profile record missing.");
            return;
        }

        System.out.println("\n--- PROFILE DETAILS ---");
        System.out.println("User ID  : " + userId);
        System.out.println("Username : " + p.getUsername());
        System.out.println("Type     : " + u.getUserType());

        // Relaxed check: Only show "No details" if both bio and location are empty or default
        boolean hasNoData = (p.getBio() == null || p.getBio().equals("-") || p.getBio().trim().isEmpty()) && 
                            (p.getLocation() == null || p.getLocation().equals("-") || p.getLocation().trim().isEmpty());

        if (hasNoData) {
            System.out.println("No details found. Please edit profile.");
        } else {
            // Check if user is Business/Creator to display "Packed" details
            if (!u.getUserType().equalsIgnoreCase("Personal")) {
                System.out.println("Details  : " + p.getBio()); // Shows Category | Bio | Contact
                System.out.println("Address  : " + p.getLocation()); // Shows Address (Hours)
            } else {
                // Standard display for Personal accounts
                System.out.println("Bio      : " + p.getBio());
                System.out.println("Location : " + p.getLocation());
            }
            System.out.println("Website  : " + (p.getWebsite() != null ? p.getWebsite() : "N/A"));
        }
    }

    // --- UPDATED PROFILE MENU ---
    private void profileMenu() {
        // 1. Ensure the row exists so UPDATE won't fail
        profileDAO.ensureProfileExists(loggedInUser.getUserId(), loggedInUser.getUsername());
        
        String role = loggedInUser.getUserType();
        while (true) {
            System.out.println("\n--- PROFILE MENU (" + role + ") ---");
            System.out.println("1. View My Profile\n2. Edit My Profile\n3. Back");
            System.out.print("Choice: ");
            int choice = sc.nextInt();
            sc.nextLine(); // Clear buffer

            if (choice == 1) {
                viewProfile(loggedInUser.getUserId());
            } else if (choice == 2) {
                // Fetch current data to show as "Existing"
                Profile current = profileDAO.getProfileByUserId(loggedInUser.getUserId());
                
                System.out.println("\n--- EDITING " + role.toUpperCase() + " PROFILE ---");
                System.out.println("(Leave blank or enter new values)");

                if (role.equalsIgnoreCase("Personal")) {
                    // Personal Prompts with existing data shown
                    System.out.print("Name [" + current.getUsername() + "]: "); 
                    String n = sc.nextLine();
                    if(n.isEmpty()) n = current.getUsername();

                    System.out.print("Bio [" + current.getBio() + "]: "); 
                    String b = sc.nextLine();
                    if(b.isEmpty()) b = current.getBio();

                    System.out.print("Location [" + current.getLocation() + "]: "); 
                    String l = sc.nextLine();
                    if(l.isEmpty()) l = current.getLocation();

                    System.out.print("Website [" + current.getWebsite() + "]: "); 
                    String w = sc.nextLine();
                    if(w.isEmpty()) w = current.getWebsite();

                    if (profileDAO.updateProfile(loggedInUser.getUserId(), n, b, l, w)) {
                        System.out.println(">> Personal Profile Updated!");
                    }
                } else {
                    // Business/Creator Prompts with current data shown
                    System.out.println("Current Details: " + current.getBio());
                    
                    System.out.print("Business Name [" + current.getUsername() + "]: "); 
                    String name = sc.nextLine();
                    if(name.isEmpty()) name = current.getUsername();

                    System.out.print("Category/Industry: "); String cat = sc.nextLine();
                    System.out.print("Detailed Bio: "); String bio = sc.nextLine();
                    
                    String contact;
                    while (true) {
                        System.out.print("Contact Info (10-digits): ");
                        contact = sc.nextLine();
                        if (profileService.isValidIndianMobile(contact)) break;
                        System.out.println("Invalid number!");
                    }

                    System.out.print("Address: "); String addr = sc.nextLine();
                    System.out.print("Business Hours: "); String hours = sc.nextLine();
                    System.out.print("Website/Social Media [" + current.getWebsite() + "]: "); 
                    String web = sc.nextLine();
                    if(web.isEmpty()) web = current.getWebsite();

                    // Packing Business Data
                    String packedBio = "Category: " + cat + " | " + bio + " | Contact: " + contact;
                    String packedLoc = addr + " (Hours: " + hours + ")";

                    if (profileDAO.updateProfile(loggedInUser.getUserId(), name, packedBio, packedLoc, web)) {
                        System.out.println(">> Business Profile Updated!");
                    }
                }
            } else break;
        }
    }

    // Helper to keep profileMenu clean
    private void handleEditProfile(String role) {
        System.out.println("\n--- Edit Profile Details ---");
        if (role.equalsIgnoreCase("Personal")) {
            System.out.print("Name/Username: "); String n = sc.nextLine();
            System.out.print("Bio: "); String b = sc.nextLine();
            System.out.print("Location: "); String l = sc.nextLine();
            System.out.print("Website: "); String w = sc.nextLine();
            
            if (profileDAO.updateProfile(loggedInUser.getUserId(), n, b, l, w)) {
                System.out.println("Success: Personal Profile Updated!");
            }
        } else {
            // Reuse the existing enhanced logic for Business/Creator
            updateBusinessFromSearch(loggedInUser.getUserId()); 
        }
    }
    private void viewSearchedProfile(int targetId) {
        User targetUser = userDAO.getUserById(targetId);
        Profile targetProfile = profileDAO.getProfileByUserId(targetId);

        if (targetUser == null || targetProfile == null) {
            System.out.println("Profile not found.");
            return;
        }

        // Display Profile Details
        System.out.println("\n--- " + targetUser.getUsername() + "'s Profile ---");
        System.out.println("Type: " + targetUser.getUserType());
        System.out.println("Bio: " + targetProfile.getBio());
        System.out.println("Location: " + targetProfile.getLocation());

        // --- NEW BUSINESS OPTIONS BLOCK ---
        if (targetUser.getUserType().equalsIgnoreCase("Business")) {
            System.out.println("\n[Business Tools]");
            System.out.println("1. Change Business Address/Hours");
            System.out.println("2. Update Business Category");
            System.out.println("3. Back to Search");
            System.out.print("Choice: ");
            int choice = sc.nextInt();
            sc.nextLine(); // clear buffer

            if (choice == 1 || choice == 2) {
                updateBusinessFromSearch(targetUser.getUserId());
            }
        } else {
            System.out.println("\n1. Back to Search");
            sc.nextInt();
        }
    }
    
    private void updateBusinessFromSearch(int businessId) {
        System.out.println("\n--- Update Business Details ---");
        
        // We use sc.nextLine() to ensure we capture spaces in names/addresses
        System.out.print("New Entity Name (or enter to skip): "); 
        String name = sc.nextLine();
        
        System.out.print("New Category: "); 
        String cat = sc.nextLine();
        
        System.out.print("New Description/Bio: "); 
        String bio = sc.nextLine();
        
        System.out.print("New Business Address: "); 
        String addr = sc.nextLine();
        
        // Validate the phone number using your existing ProfileService logic
        String contact;
        while (true) {
            System.out.print("New Contact Info (10-digit mobile): ");
            contact = sc.nextLine();
            if (profileService.isValidIndianMobile(contact)) {
                break;
            } else {
                System.out.println("Error: Please enter a valid 10-digit Indian mobile number.");
            }
        }
        
        System.out.print("New Website: "); 
        String web = sc.nextLine();
        
        System.out.print("New Business Hours: "); 
        String hours = sc.nextLine();

        // Call your service to pack and save these details
        boolean success = profileService.updateEnhancedProfile(
            businessId, name, cat, bio, addr, contact, web, hours
        );

        if (success) {
            System.out.println("\n[SUCCESS] Business details updated!");
        } else {
            System.out.println("\n[ERROR] Failed to update business details.");
        }
    }
}