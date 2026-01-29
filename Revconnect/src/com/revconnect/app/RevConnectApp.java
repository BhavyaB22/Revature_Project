package com.revconnect.app;

import java.util.*;
import com.revconnect.model.*;
import com.revconnect.dao.*;

public class RevConnectApp {
    private Scanner sc = new Scanner(System.in);
    private UserDAO userDAO = new UserDAO();
    private ProfileDAO profileDAO = new ProfileDAO();
    private NotificationDAO notificationDAO = new NotificationDAO();
    private PostsDAO postDAO = new PostsDAO(); 
    private NetworksDAO networksDAO = new NetworksDAO();
    
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

        System.out.print("Email: "); String email = sc.next();
        if (userDAO.isEmailExists(email)) { System.out.println("Error: Email registered."); return; }
        u.setEmail(email);

        System.out.print("Username: "); String uname = sc.next();
        if (userDAO.isUsernameExists(uname)) { System.out.println("Error: Username taken."); return; }
        u.setUsername(uname);

        System.out.print("Password: "); u.setPassword(sc.next());
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
            System.out.println("1. View My Profile\n2. Edit Bio/Location\n3. Back");
            int choice = sc.nextInt();
            if (choice == 1) {
                viewProfile(loggedInUser.getUserId());
            } else if (choice == 2) {
                sc.nextLine();
                System.out.print("New Bio: "); String bio = sc.nextLine();
                profileDAO.updateProfileField(loggedInUser.getUserId(), "bio", bio);
                System.out.println("Updated!");
            } else break;
        }
    }

    // --- POSTS MODULE ---
    private void postMenu() {
        while (true) {
            System.out.println("\n--- MY POSTS ---");
            System.out.println("1. Create Post\n2. View My Posts\n3. Delete Post\n4. Back");
            int choice = sc.nextInt();
            if (choice == 1) {
                sc.nextLine();
                System.out.print("Title: "); String title = sc.nextLine();
                System.out.print("Content: "); String content = sc.nextLine();
                postDAO.createPost(loggedInUser.getUserId(), title, content);
            } else if (choice == 2) {
                List<Posts> myPosts = postDAO.getPostsByUserId(loggedInUser.getUserId());
                for (Posts p : myPosts) System.out.println("[" + p.getPostId() + "] " + p.getPostName());
            } else if (choice == 3) {
                System.out.print("Enter Post ID to Delete: ");
                postDAO.deletePost(sc.nextInt(), loggedInUser.getUserId());
            } else break;
        }
    }

    // --- FEED & INTERACTION ---
    private void showFeedFlow() {
        List<Posts> feed = postDAO.getFeed();
        System.out.println("\n--- GLOBAL FEED ---");
        for (Posts p : feed) {
            System.out.println("[" + p.getPostId() + "] " + p.getPostName() + " (Likes: " + p.getLikes() + ")");
            System.out.println("   > " + p.getDescription());
        }
        System.out.print("\nEnter ID to interact (0 to exit): ");
        int pid = sc.nextInt();
        if (pid != 0) interactWithPost(pid);
    }

    private void interactWithPost(int pid) {
        System.out.println("1. Like\n2. Comment\n3. Back");
        int action = sc.nextInt();
        int ownerId = postDAO.getOwnerIdByPostId(pid); // You need this in PostsDAO

        if (action == 1) {
            if (postDAO.likePost(pid, loggedInUser.getUserId())) {
                notificationDAO.createNotification(ownerId, loggedInUser.getUserId(), "liked your post", "LIKE");
                System.out.println("Liked!");
            }
        } else if (action == 2) {
            sc.nextLine();
            System.out.print("Comment: ");
            String msg = sc.nextLine();
            if (postDAO.addComment(pid, loggedInUser.getUserId(), msg)) {
                notificationDAO.createNotification(ownerId, loggedInUser.getUserId(), "commented on your post", "COMMENT");
                System.out.println("Commented!");
            }
        }
    }

    // --- NETWORK MODULE ---
    private void showNetworkFlow() {
        while (true) {
            System.out.println("\n--- NETWORK ---");
            System.out.println("1. Connections\n2. Requests\n3. Search Users\n4. Followers\n5. Following\n6. Back");
            int choice = sc.nextInt();
            int myId = loggedInUser.getUserId();

            if (choice == 1) displayList(networksDAO.getConnections(myId), "Connections");
            else if (choice == 2) {
                displayList(networksDAO.getIncomingRequests(myId), "Incoming Requests");
                System.out.print("Enter Sender ID to process (0 to skip): ");
                int sid = sc.nextInt();
                if (sid != 0) {
                    System.out.print("1. Accept | 2. Reject: ");
                    int res = sc.nextInt();
                    String status = (res == 1) ? "ACCEPTED" : "REJECTED";
                    if (networksDAO.updateRequestStatus(myId, sid, status)) {
                        if (res == 1) notificationDAO.createNotification(sid, myId, "accepted your request", "ACCEPT");
                        System.out.println("Request " + status);
                    }
                }
            } else if (choice == 3) searchAndConnect();
            else if (choice == 4) displayList(networksDAO.getFollowers(myId), "Followers");
            else if (choice == 5) displayList(networksDAO.getFollowing(myId), "Following");
            else break;
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

    // --- HELPER METHODS ---
    private void displayList(List<String> list, String title) {
        System.out.println("\n--- " + title + " ---");
        if (list.isEmpty()) System.out.println("Nothing to show.");
        else for (String s : list) System.out.println("- " + s);
    }

    private void viewPostDetails(int postId, int viewerId) {
        // Logic to fetch post from DAO and print it
        System.out.println("Displaying Post ID: " + postId);
    }

    private void viewProfile(int userId) {
        Profile p = profileDAO.getProfileByUserId(userId);
        if (p != null) {
            System.out.println("\n--- " + p.getUsername().toUpperCase() + " ---");
            System.out.println("Bio: " + p.getBio());
            System.out.println("Location: " + p.getLocation());
        }
    }
}