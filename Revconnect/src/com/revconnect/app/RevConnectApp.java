package com.revconnect.app;

import java.util.*;
import com.revconnect.model.*;
import com.revconnect.service.*;
import com.revconnect.dao.*;

public class RevConnectApp {
    private Scanner sc = new Scanner(System.in);
    private AuthService authService = new AuthService();
    private UserDAO userDAO = new UserDAO();
    private ProfileDAO profileDAO = new ProfileDAO();
    private User loggedInUser = null;

    public void start() {
        while (true) {
            System.out.println("\n--- RevConnect ---");
            System.out.println("1. Login\n2. Register\n3. Exit");
            int choice = sc.nextInt();
            if (choice == 1) loginMenu(); // Moved Forgot PW inside here
            else if (choice == 2) registerFlow();
            else System.exit(0);
        }
    }

    private void loginMenu() {
        System.out.println("\n--- Login ---");
        System.out.println("1. Enter Credentials\n2. Forgot Password?\n3. Back");
        int choice = sc.nextInt();
        
        if (choice == 1) {
            System.out.print("Email: ");
            String e = sc.next();
            System.out.print("Password: ");
            String p = sc.next();
            loggedInUser = userDAO.login(e, p);
            if (loggedInUser != null) userDashboard();
            else System.out.println("Login Failed.");
        } else if (choice == 2) {
            forgotPasswordFlow();
        }
    }

    private void registerFlow() {
    	User user = new User();
    	// 1. Email Validation Loop
        while (true) {
            System.out.print("Email: ");
            String emailInput = sc.next();
            
            // Use the strict regex logic from AuthService
            String result = authService.validateEmail(emailInput);
            
            if (result.equals("SUCCESS")) {
                // Check if email already exists in DB
                if (userDAO.isEmailExists(emailInput)) {
                    System.out.println("Error: User already exists with this email!");
                    continue; // Re-ask for email
                }
                user.setEmail(emailInput);
                break; // Exit loop if valid and unique
            } else {
                System.out.println(result); // Show "Invalid Email!" message
            }
        }

        // 2. Password Validation Loop (Existing)
        while (true) {
            System.out.print("Password (Min 6, 1 Upper, 1 Lower, 1 Num, 1 Special): ");
            String pass = sc.next();
            String result = authService.validatePasswordRules(pass);
            if (result.equals("SUCCESS")) {
                user.setPassword(pass);
                break;
            }
            System.out.println(result);
        }
        System.out.println("Type: 1.Personal 2.Business 3.Creator");
        int t = sc.nextInt();
        String type = (t==2)?"Business":(t==3?"Creator":"Personal");
        user.setUserType(type);

        System.out.print("Privacy (1.Public 2.Private): ");
        user.setPrivacy(sc.nextInt()==2?"Private":"Public");

        sc.nextLine(); 
        System.out.print("Security Question: ");
        user.setsQuestion(sc.nextLine());
        System.out.print("Security Answer: ");
        user.setsAnswer(sc.nextLine());

        // NOW: Ask for Profile details BEFORE finishing
        System.out.println("\n--- Setup Your Profile ---");
        Profile p = new Profile();
        System.out.print("Username/Handle: ");
        p.setUsername(sc.next());
        sc.nextLine();
        System.out.print("Bio/About: ");
        p.setBio(sc.nextLine());
        System.out.print("Location: ");
        p.setLocation(sc.nextLine());
        System.out.print("Website (Optional): ");
        String websiteInput = sc.nextLine();
        p.setWebsite(websiteInput.isEmpty() ? "N/A" : websiteInput);

        if (type.equals("Business")) {
            System.out.print("Business Category: ");
            p.setCategory(sc.nextLine());
            System.out.print("Business Address: ");
            p.setAddress(sc.nextLine());
            System.out.print("Enter Phone Number: "); 
            String phone;
         // Ensure sc.nextLine() is called once before this if the previous input was sc.next()
            sc.nextLine();
            while (true) {
                System.out.print("Enter Phone Number: ");
                phone = sc.nextLine();
                
                // Capture the return string from your validation method
                String status = authService.validatePhoneNumber(phone);
                
                if (status.equals("SUCCESS")) {
                    p.setContactInfo(phone); // Matches column in image_6fe31c.png
                    break; // VALID: Exit the loop
                } else {
                    // INVALID: Print the specific error message and loop again
                    System.out.println("Invalid" + status); 
                }
            }
            p.setContactInfo(sc.nextLine());
            System.out.print("Business Hours: ");
            p.setBusinessHours(sc.nextLine());
        } else {
            // For Personal/Creator, set these to null or N/A so the DB doesn't complain
            p.setCategory("N/A");
            p.setAddress("N/A");
            p.setBusinessHours("N/A");
        }

        // Save everything to DB
        if (userDAO.registerUser(user)) {
            int newId = userDAO.getLastInsertedId();
            
            if (newId <= 0) {
                System.out.println("CRITICAL ERROR: Could not retrieve the new User ID from Database.");
                return;
            }
            
            p.setUserId(newId);
            
            if (profileDAO.saveProfile(p)) {
                System.out.println("\nREGISTRATION SUCCESSFUL! You can now login.");
            } else {
                System.out.println("ERROR: Account created, but Profile details failed to save.");
            }
        } else {
            System.out.println("ERROR: Could not save User credentials to the database.");
        }
  
    }
    
    private void forgotPasswordFlow() {
        System.out.print("Enter Registered Email: ");
        String email = sc.next();
     // 1. Fetch the question from the DAO
        String question = userDAO.getSecurityQuestion(email);
        
        if (question == null) {
            System.out.println("Error: No account found with that email.");
            return;
        }

        // 2. Display the specific question to the user
        System.out.println("Security Question: " + question);
        System.out.print("Your Answer: ");
        sc.nextLine(); // Clear the scanner buffer
        String answer = sc.nextLine();
     // 3. Verify the answer
        int uid = userDAO.verifySecurityAnswer(email, answer);
        
        if (uid != -1) {
            System.out.print("Verification Successful! Enter New Password: ");
            String newPass = sc.next();
            
            // Use your authService to ensure the new pass isn't a negative value
            String result = authService.validatePasswordRules(newPass);
            if (result.equals("SUCCESS")) {
                 System.out.println(authService.recoverPassword(uid, newPass));
            } else {
                 System.out.println(result);
            }
        } else {
            System.out.println("Incorrect security answer.");
        }
    }

    private void userDashboard() {
        while (loggedInUser != null) {
            System.out.println("\n1. My Profile\n2. Search Users\n3. Logout");
            int c = sc.nextInt();
            if (c == 1) displayProfile(loggedInUser.getUserId());
            else if (c == 2) searchFlow();
            else loggedInUser = null;
        }
    }
    
   

    private void searchFlow() {
        System.out.print("Enter search query: ");
        String query = sc.next();
        List<Profile> results = userDAO.searchUsers(query);
        if (results.isEmpty()) {
            System.out.println("No users found.");
        } else {
            for (Profile p : results) {
                System.out.println("- " + p.getUsername());
            }
        }
    }

    private void displayProfile(int id) {
        Profile p = profileDAO.getProfile(id);
        if (p != null) {
            System.out.println("\nUsername: " + p.getUsername());
            System.out.println("Bio: " + p.getBio());
        }
    }

    public static void main(String[] args) {
        new RevConnectApp().start();
    }
}