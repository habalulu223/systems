package main;

import config.dbConnect;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class main {
    public static void main(String[] args) {
        // Initialize the database connection manager
        dbConnect cf = new dbConnect();

        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n--- Welcome! Choose an option ---");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Enter choice: ");

            int choice = -1;
            try {
                choice = sc.nextInt();
                sc.nextLine();
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                sc.nextLine();
                continue;
            }

            switch (choice) {
                case 1:
                    // Register new user
                    System.out.print("Enter desired username: ");
                    String username = sc.nextLine();
                    System.out.print("Enter Email: ");
                    String Gmail = sc.nextLine();
                    System.out.print("Enter password: ");
                    String password = sc.nextLine();

                    // ⭐️ CHANGED: Call static method in this class
                    boolean registered = registerUser(cf, username, password, Gmail);
                    if (registered) {
                        System.out.println("Registration successful! Awaiting admin approval.");
                    } else {
                        System.out.println("Registration failed. Username or Email may already exist.");
                    }
                    break;


                case 2:
                
                    System.out.print("Enter Email: ");
                    String loginEmail = sc.nextLine(); 
                    System.out.print("Enter password: ");
                    String loginPass = sc.nextLine();
                    
                    // ⭐️ CHANGED: Call static method in this class
                    String loggedInUsername = loginUser(cf, loginEmail, loginPass);

                    
                    if (loggedInUsername != null) {
                        System.out.println("Login successful! Welcome, " + loggedInUsername + "!");
                        
                        // ⭐️ CHANGED: Call static method in this class
                        if (isAdmin(cf, loginEmail)) { 
                            adminMenu(sc, cf, loggedInUsername, loginEmail);
                        } else {
                            userMenu(sc, cf, loggedInUsername, loginEmail);
                        }
                    } else {
                        System.out.println("Login failed. Invalid credentials or account pending approval.");
                    }
                    break;

                    

                case 3:
                    System.out.println("Exiting program. Goodbye! 👋");
                    sc.close();
                    System.exit(0);
                    break;

                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    // --------------------------------------------------------------------------------------------------
    // Regular user menu
    // --------------------------------------------------------------------------------------------------
    private static void userMenu(Scanner sc, dbConnect cf, String username, String email) {
        while (true) {
            System.out.println("\n--- User Menu for " + username + " ---"); 
            System.out.println("1. View Products");
            System.out.println("2. Make Transaction");
            System.out.println("3. Make Payment");
            System.out.println("4. View Transactions");
            System.out.println("5. Update Transaction Quantity");
            System.out.println("6. Delete Transaction");
            System.out.println("7. Logout");
            System.out.print("Enter choice: ");

            int option = -1;
            try {
                option = sc.nextInt();
                sc.nextLine(); // consume newline
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                sc.nextLine(); // Clear the invalid input
                continue;
            }

            switch (option) {
                case 1:
                    // ⭐️ CHANGED: Calls the single logic method directly
                    viewProducts(cf);
                    break;
                case 2:
                    System.out.println("Available Products:");
                    // ⭐️ CHANGED: Calls the single logic method directly
                    viewProducts(cf); 

                    // ⭐️ CHANGED: Call static method
                    int user_id = getUserId(cf, email); 
                    if (user_id <= 0) {
                        System.out.println("Error: User ID not found. Cannot proceed with transaction.");
                        break;
                    }

                    try {
                        System.out.print("Enter product id to buy: ");
                        int productId = sc.nextInt();
                        System.out.print("Enter quantity: ");
                        int quantity = sc.nextInt();
                        sc.nextLine(); // consume newline

                        // ⭐️ CHANGED: Call static method
                        makeTransaction(cf, email, user_id, productId, quantity);
                    } catch (InputMismatchException e) {
                        System.out.println("Invalid input for ID or Quantity. Please enter numbers.");
                        sc.nextLine();
                    }
                    break;
                case 3:
                    // ⭐️⭐️⭐️ RE-IMPLEMENTED CASH/CHANGE LOGIC ⭐️⭐️⭐️
                    viewTransactions(cf, email); // Show pending transactions
                    try {
                        System.out.print("Enter transaction ID to pay: ");
                        int transactionId = sc.nextInt();
                        sc.nextLine(); // consume newline

                        // 1. Get transaction details
                        java.util.Map<String, Object> txnDetails = getTransactionForPayment(cf, email, transactionId);

                        // 2. Check if the transaction was found
                        if (txnDetails != null) {
                            String status = (String) txnDetails.get("status");
                            
                            // 3. Check if it's 'pending_payment'
                            if (!"pending_payment".equalsIgnoreCase(status)) {
                                System.out.println("Error: This transaction is already '" + status + "' and cannot be paid.");
                                break; // Exit case 3
                            }
                            
                            // 4. Get price
                            double amountDue = ((Number) txnDetails.get("total_price")).doubleValue();
                            
                            System.out.println("---------------------------------");
                            System.out.printf("Amount Due: %.2f PHP\n", amountDue);
                            System.out.print("Enter cash payment: ");
                            
                            // 5. Get cash payment
                            double cashPaid = sc.nextDouble();
                            sc.nextLine(); // consume newline

                            // 6. Check if cash is sufficient
                            if (cashPaid < amountDue) {
                                System.out.println("Error: Insufficient payment. Payment of " + amountDue + " is required.");
                            } else {
                                // 7. Cash is sufficient, proceed with payment
                                boolean success = makePayment(cf, email, transactionId);
                                
                                if (success) {
                                    double change = cashPaid - amountDue;
                                    System.out.println("\nPayment Successful!");
                                    System.out.println("--- Receipt ---");
                                    System.out.printf("Total Due: %.2f\n", amountDue);
                                    System.out.printf("Cash Paid: %.2f\n", cashPaid);
                                    System.out.printf("Change:    %.2f\n", change);
                                    System.out.println("---------------------------------");
                                } else {
                                    System.out.println("Error: Payment failed. Database error.");
                                }
                            }
                        } else {
                             System.out.println("Payment failed. Transaction not found, already paid, or does not belong to you.");
                        }
                        
                    } catch (InputMismatchException e) {
                        System.out.println("Invalid input. Please enter numbers for ID or cash.");
                        sc.nextLine();
                    }
                    break;
                case 4:
                    // ⭐️ CHANGED: Call static method
                    viewTransactions(cf, email);
                    break;
                case 5:
                    // ⭐️ CHANGED: Call static method
                    viewTransactions(cf, email);
                    try {
                        System.out.print("Enter transaction id to update: ");
                        int transactionid = sc.nextInt();
                        System.out.print("Enter new quantity: ");
                        int newquantity = sc.nextInt();
                        sc.nextLine();
                        // ⭐️ CHANGED: Call static method
                        updateTransaction(cf, transactionid, newquantity, email);
                    } catch (InputMismatchException e) {
                        System.out.println("Invalid input for ID or Quantity. Please enter numbers.");
                        sc.nextLine();
                    }
                    break;
                case 6:
                    // ⭐️ CHANGED: Call static method
                    viewTransactions(cf, email);
                    try {
                        System.out.print("Enter transaction id to delete: ");
                        int deletetransactionid = sc.nextInt();
                        sc.nextLine();
                        // ⭐️ CHANGED: Call static method
                        deleteTransaction(cf, deletetransactionid, email);
                    } catch (InputMismatchException e) {
                        System.out.println("Invalid input for Transaction ID. Please enter a number.");
                        sc.nextLine();
                    }
                    break;
                case 7:
                    System.out.println("Logging out " + username + "...");
                    return; // Exit user menu loop
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    // --------------------------------------------------------------------------------------------------
    // Product Management Methods (for Admin Menu)
    // --------------------------------------------------------------------------------------------------

    private static void addProduct(Scanner sc, dbConnect cf, String adminUsername) {
        System.out.println("\n--- Add Product ---");
        System.out.print("Enter product name: ");
        String name = sc.nextLine();
        try {
            System.out.print("Enter product price: ");
            double price = sc.nextDouble();
            System.out.print("Enter product stock quantity: ");
            int stock = sc.nextInt();
            sc.nextLine(); // consume newline
            // ⭐️ CHANGED: Call static method
            addProduct(cf, name, price, stock, adminUsername);
        } catch (InputMismatchException e) {
            System.out.println("Invalid input for Price or Stock. Please enter numbers.");
            sc.nextLine(); // Clear the invalid input
        }
        viewProducts(cf);
    }

    private static void updateProduct(Scanner sc, dbConnect cf, String adminUsername) {
        viewProducts(cf);
        try {
            System.out.println("\n--- Update Product ---");
            System.out.print("Enter ID of product to update: ");
            int updateId = sc.nextInt();
            sc.nextLine(); // consume newline
            System.out.print("Enter new product name: ");
            String newName = sc.nextLine();
            System.out.print("Enter new product price: ");
            double newPrice = sc.nextDouble();
            System.out.print("Enter new product stock quantity: ");
            int newStock = sc.nextInt();
            sc.nextLine(); // consume newline
            // ⭐️ CHANGED: Call static method
            updateProduct(cf, updateId, newName, newPrice, newStock, adminUsername);
        } catch (InputMismatchException e) {
            System.out.println("Invalid input for ID, Price, or Stock. Please enter numbers.");
            sc.nextLine(); // Clear the invalid input
        }
        viewProducts(cf);
    }

    private static void deleteProduct(Scanner sc, dbConnect cf, String adminUsername) {
        viewProducts(cf);
        try {
            System.out.println("\n--- Delete Product ---");
            System.out.print("Enter product id to delete: ");
            int deleteId = sc.nextInt();
            sc.nextLine(); // consume newline
            // ⭐️ CHANGED: Call static method
            deleteProduct(cf, deleteId, adminUsername);
        } catch (InputMismatchException e) {
            System.out.println("Invalid input for ID. Please enter a number.");
            sc.nextLine(); // Clear the invalid input
        }
        viewProducts(cf);
    }

    // ⭐️⭐️⭐️ DELETED 'viewProducts' METHOD FROM HERE ⭐️⭐️⭐️

    // --------------------------------------------------------------------------------------------------
    // User Account Management Methods (for Admin Menu)
    // --------------------------------------------------------------------------------------------------

    // ⭐️⭐️⭐️ DELETED 'viewPendingUsers' METHOD FROM HERE ⭐️⭐️⭐️

    private static void approveUser(Scanner sc, dbConnect cf) {
        viewPendingUsers(cf); // This now correctly calls the single logic method
        System.out.println("\n--- Approve User ---");
        System.out.print("Enter username to approve: ");
        String userToApprove = sc.nextLine();
        
        // ⭐️ CHANGED: Call static method (using the improved logic from before)
        approveUser(cf, userToApprove);
        // Note: The success/fail messages are now handled inside the approveUser logic method
    }

    private static void promoteUserToAdmin(Scanner sc, dbConnect cf, String adminUsername, String adminEmail) {
        viewUsers(cf); // This now correctly calls the single logic method
        System.out.println("\n--- Promote User to Admin ---");
        System.out.print("Enter account name to promote as admin: ");
        String userToPromote = sc.nextLine();
        
        // ⭐️ CHANGED: Call static method
        boolean success = makeUserAdmin(cf, userToPromote, adminEmail);
        if (success) {
            System.out.println("User '" + userToPromote + "' has been successfully promoted to Admin. 🎉");
        } else {
            // Error message is handled inside makeUserAdmin
        }
    }

    // ⭐️⭐️⭐️ DELETED 'viewUsers' METHOD FROM HERE ⭐️⭐️⭐️

    // --------------------------------------------------------------------------------------------------
    // Transaction Management Methods (for Admin Menu)
    // --------------------------------------------------------------------------------------------------
    
    private static void deleteAnyTransaction(Scanner sc, dbConnect cf, String adminUsername) {
        System.out.println("\n--- Force Delete ANY Transaction ---");
        try {
            System.out.print("Enter transaction ID to forcefully delete: ");
            int deleteTxnId = sc.nextInt();
            sc.nextLine(); // consume newline
            // ⭐️ CHANGED: Call static method
            adminDeleteTransaction(cf, deleteTxnId, adminUsername);
        } catch (InputMismatchException e) {
            System.out.println("Invalid input. Please enter a number for the Transaction ID.");
            sc.nextLine(); // Clear the invalid input
        }
    }


    // --------------------------------------------------------------------------------------------------
    // Admin menu
    // --------------------------------------------------------------------------------------------------
    private static void adminMenu(Scanner sc, dbConnect cf, String username, String email) {
        while (true) {
            System.out.println("\n--- Admin Menu for " + username + " (ADMIN) ---"); // Uses username for display
            System.out.println("1. Add Product");
            System.out.println("2. Update Product");
            System.out.println("3. Delete Product");
            System.out.println("--- User Account Management ---");
            System.out.println("4. View Pending Users");
            System.out.println("5. Approve User");
            System.out.println("6. Promote User to Admin");
            System.out.println("7. View All Accounts");
            System.out.println("--- Transaction Management ---");
            System.out.println("8. Delete ANY Transaction (No Stock Restore!)");
            System.out.println("9. Logout");
            System.out.print("Enter choice: ");

            int option = -1;
            try {
                option = sc.nextInt();
                sc.nextLine(); // consume newline
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                sc.nextLine(); // Clear the invalid input
                continue;
            }

            switch (option) {
                case 1:
                    addProduct(sc, cf, username);
                    break;
                case 2:
                    updateProduct(sc, cf, username);
                    break;
                case 3:
                    deleteProduct(sc, cf, username);
                    break;
                case 4:
                    viewPendingUsers(cf); // This now correctly calls the single logic method
                    break;
                case 5:
                    approveUser(sc, cf);
                    break;
                case 6:
                    promoteUserToAdmin(sc, cf, username, email);
                    break;
                case 7:
                    viewUsers(cf); // This now correctly calls the single logic method
                    break;
                case 8: 
                    // ⭐️ CHANGED: Call static method
                    viewTransactions(cf, email);
                    deleteAnyTransaction(sc, cf, username);
                    break;
                case 9:
                    System.out.println("Logging out " + username + "...");
                    return; 
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }


    // --------------------------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------------------------
    //
    //  ⭐️⭐️⭐️ BUSINESS LOGIC METHODS (Moved from dbConnect) ⭐️⭐️⭐️
    //  All methods are now private static and take 'dbConnect cf' as a parameter.
    //
    // --------------------------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------------------------


    //-----------------------------------------------
    // ⭐️ USER MANAGEMENT METHODS ⭐️
    //-----------------------------------------------

    private static boolean registerUser(dbConnect cf, String username, String password, String Gmail) {
        String checkSql = "SELECT user_id FROM tbl_user WHERE username = ? OR email = ?";
        // ⭐️ CHANGED: Call with 'cf.'
        if (!cf.fetchRecords(checkSql, username, Gmail).isEmpty()) {
            return false; 
        }
        
        // ⭐️ CHANGED: Call static method from dbConnect class
        String hashedPass = dbConnect.hashPassword(password);
        if (hashedPass == null) return false;

        String sql = "INSERT INTO tbl_user (username, email, password, role, status) VALUES (?, ?, ?, 'user', 'pending')";
        // ⭐️ CHANGED: Call with 'cf.'
        return cf.addRecord(sql, username, Gmail, hashedPass);
    }

    
    private static String loginUser(dbConnect cf, String email, String password) {
        
        String sql = "SELECT username, password FROM tbl_user WHERE email = ? AND status = 'approved'";
        // ⭐️ CHANGED: Call with 'cf.'
        List<Map<String, Object>> users = cf.fetchRecords(sql, email);
        
        if (users.isEmpty()) {
            return null; // User not found or not approved
        }
        
        String storedHash = users.get(0).get("password").toString();
        // ⭐️ CHANGED: Call static method from dbConnect class
        String hashedPass = dbConnect.hashPassword(password);
        
        // Check if passwords match
        if (storedHash.equals(hashedPass)) {
            return users.get(0).get("username").toString();
        } else {
            // Password mismatch
            return null;
        }
    }


    private static boolean isAdmin(dbConnect cf, String email) {
        String sql = "SELECT role FROM tbl_user WHERE email = ?";
        // ⭐️ CHANGED: Call with 'cf.'
        List<Map<String, Object>> users = cf.fetchRecords(sql, email);
        
        if (users.isEmpty()) {
            return false;
        }
        
        String role = users.get(0).get("role").toString();
        return "admin".equalsIgnoreCase(role);
    }

    private static int getUserId(dbConnect cf, String email) {
        String sql = "SELECT user_id FROM tbl_user WHERE email = ?";
        // ⭐️ CHANGED: Call with 'cf.'
        List<Map<String, Object>> users = cf.fetchRecords(sql, email);
        
        if (users.isEmpty()) {
            return -1; 
        }
        
        return (Integer) users.get(0).get("user_id");
    }

    private static void viewPendingUsers(dbConnect cf) {
        System.out.println("\n--- View Pending Users ---"); // ⭐️ HEADER MOVED HERE
        String sql = "SELECT user_id, username, email, status FROM tbl_user WHERE status = 'pending'";
        String[] headers = {"User ID", "Username", "Email", "Status"};
        String[] columns = {"user_id", "username", "email", "status"};
        // ⭐️ CHANGED: Call with 'cf.'
        cf.viewRecords(sql, headers, columns);
    }

    // ⭐️ MOVED: This is the improved approveUser method
    private static boolean approveUser(dbConnect cf, String username) {
        // 1. First, check the user's current status
        String checkSql = "SELECT status FROM tbl_user WHERE username = ?";
        List<Map<String, Object>> users = cf.fetchRecords(checkSql, username);

        if (users.isEmpty()) {
            // CASE 1: User does not exist
            System.out.println("Error: User '" + username + "' not found.");
            return false;
        }
        
        String currentStatus = users.get(0).get("status").toString();
        
        if ("approved".equalsIgnoreCase(currentStatus)) {
            // CASE 2: User is already approved
            System.out.println("Info: User '" + username + "' is already approved.");
            return false; 
        }
        
        if (!"pending".equalsIgnoreCase(currentStatus)) {
            // CASE 3: User is 'banned', 'rejected', etc.
            System.out.println("Error: User '" + username + "' has status '" + currentStatus + "' and cannot be approved.");
            return false;
        }

        // CASE 4: User exists and is 'pending'. Proceed with update.
        String updateSql = "UPDATE tbl_user SET status = 'approved' WHERE username = ?";
        
        boolean success = cf.updateRecord(updateSql, username); 
        
        if (success) {
            System.out.println("Success: User '" + username + "' has been approved.");
        } else {
            System.out.println("Error: Failed to update user status in the database.");
        }
        
        return success;
    }

    private static boolean makeUserAdmin(dbConnect cf, String userToPromote, String adminEmail) { 
        String checkSelfSql = "SELECT user_id FROM tbl_user WHERE username = ? AND email = ?";
        // ⭐️ CHANGED: Call with 'cf.'
        if (!cf.fetchRecords(checkSelfSql, userToPromote, adminEmail).isEmpty()) {
             System.out.println("Error: Admin cannot change their own role.");
             return false;
        }

        String sql = "UPDATE tbl_user SET role = 'admin' WHERE username = ? AND role = 'user'";
        // ⭐️ CHANGED: Call with 'cf.'
        return cf.updateRecord(sql, userToPromote);
    }

    private static void viewUsers(dbConnect cf) {
        System.out.println("\n--- View All Accounts ---"); // ⭐️ HEADER MOVED HERE
        String sql = "SELECT user_id, username, email, role, status FROM tbl_user";
        String[] headers = {"ID", "Username", "Email", "Role", "Status"};
        String[] columns = {"user_id", "username", "email", "role", "status"};
        // ⭐️ CHANGED: Call with 'cf.'
        cf.viewRecords(sql, headers, columns);
    }

    //-----------------------------------------------
    // ⭐️ PRODUCT MANAGEMENT METHODS ⭐️
    //-----------------------------------------------

    private static void viewProducts(dbConnect cf) {
        System.out.println("\n--- View All Products ---"); // ⭐️ HEADER MOVED HERE
        String sql = "SELECT product_id, name, price, stock FROM tbl_products WHERE stock > 0";
        String[] headers = {"ID", "Product Name", "Price (PHP)", "Stock"};
        String[] columns = {"product_id", "name", "price", "stock"};
        // ⭐️ CHANGED: Call with 'cf.'
        cf.viewRecords(sql, headers, columns);
    }

    private static void addProduct(dbConnect cf, String name, double price, int stock, String adminUsername) {
        String sql = "INSERT INTO tbl_products (name, price, stock) VALUES (?, ?, ?)";
        // ⭐️ CHANGED: Call with 'cf.'
        if (cf.addRecord(sql, name, price, stock)) {
            System.out.println("Product '" + name + "' added successfully by " + adminUsername);
        }
    }

    private static void updateProduct(dbConnect cf, int id, String name, double price, int stock, String adminUsername) {
        String sql = "UPDATE tbl_products SET name = ?, price = ?, stock = ? WHERE product_id = ?";
        // ⭐️ CHANGED: Call with 'cf.'
        if (cf.updateRecord(sql, name, price, stock, id)) {
            System.out.println("Product ID " + id + " updated successfully by " + adminUsername);
        } else {
            System.out.println("Failed to update product. Check if ID " + id + " exists.");
        }
    }

    private static void deleteProduct(dbConnect cf, int id, String adminUsername) {
        String sql = "DELETE FROM tbl_products WHERE product_id = ?";
        // ⭐️ CHANGED: Call with 'cf.'
        if (cf.deleteRecord(sql, id)) {
            System.out.println("Product ID " + id + " deleted successfully by " + adminUsername);
        } else {
            System.out.println("Failed to delete product. Check if ID " + id + " exists.");
        }
    }

    //-----------------------------------------------
    // ⭐️ TRANSACTION MANAGEMENT METHODS ⭐️
    //-----------------------------------------------

    private static void makeTransaction(dbConnect cf, String email, int userId, int productId, int quantity) {
        String productSql = "SELECT price, stock FROM tbl_products WHERE product_id = ?";
        // ⭐️ CHANGED: Call with 'cf.'
        List<Map<String, Object>> products = cf.fetchRecords(productSql, productId);
        
        if (products.isEmpty()) {
            System.out.println("Error: Product ID " + productId + " not found.");
            return;
        }
        
        
        double price = ((Number) products.get(0).get("price")).doubleValue();
        
        
        int stock = ((Number) products.get(0).get("stock")).intValue();

        if (quantity <= 0) {
             System.out.println("Error: Quantity must be greater than 0.");
             return;
        }
        if (quantity > stock) {
            System.out.println("Error: Not enough stock. Available: " + stock);
            return;
        }

        double totalPrice = price * quantity;
        int newStock = stock - quantity;

        String updateStockSql = "UPDATE tbl_products SET stock = ? WHERE product_id = ?";
        // ⭐️ CHANGED: Call with 'cf.'
        if (!cf.updateRecord(updateStockSql, newStock, productId)) {
            System.out.println("Error: Could not update stock. Transaction rolled back.");
            return;
        }

        String insertTxnSql = "INSERT INTO tbl_transactions (user_id, product_id, quantity, total_price, status, transaction_date) " +
                                 "VALUES (?, ?, ?, ?, 'pending_payment', CURRENT_TIMESTAMP)";
        
        // ⭐️ CHANGED: Call with 'cf.'
        if (cf.addRecord(insertTxnSql, userId, productId, quantity, totalPrice)) {
            System.out.println("Transaction created successfully! Total: " + totalPrice);
            System.out.println("Please go to 'Make Payment' to complete your order.");
        } else {
            System.out.println("Error: Failed to create transaction record. Rolling back stock...");
            // ⭐️ CHANGED: Call with 'cf.'
            cf.updateRecord(updateStockSql, stock, productId);
            System.out.println("Stock has been restored.");
        }
    }

    private static void viewTransactions(dbConnect cf, String email) {
        // ⭐️ CHANGED: Call static method
        int userId = getUserId(cf, email);
        if (userId == -1) {
            System.out.println("Error: Could not find user.");
            return;
        }
        
        String sql = "SELECT t.transaction_id, p.name, t.quantity, t.total_price, t.status, t.transaction_date " +
                       "FROM tbl_transactions t JOIN tbl_products p ON t.product_id = p.product_id " +
                       "WHERE t.user_id = ? ORDER BY t.transaction_id DESC";
        
        String[] headers = {"Txn ID", "Product", "Qty", "Total", "Status", "Date"};
        String[] columns = {"transaction_id", "name", "quantity", "total_price", "status", "transaction_date"};
        
        // ⭐️ CHANGED: Call with 'cf.'
        cf.viewRecords(sql, headers, columns, userId);
    }

    // ⭐️ MOVED: This is the helper for the cash/change logic
    private static java.util.Map<String, Object> getTransactionForPayment(dbConnect cf, String email, int transactionId) {
        int userId = getUserId(cf, email); // Call our static helper
        if (userId == -1) {
            System.out.println("Error: Could not find user account.");
            return null;
        }
        
        String sql = "SELECT total_price, status FROM tbl_transactions WHERE transaction_id = ? AND user_id = ?";
        java.util.List<java.util.Map<String, Object>> txns = cf.fetchRecords(sql, transactionId, userId);
        
        if (txns.isEmpty()) {
            return null; // The menu will handle the "not found" message
        }
        
        return txns.get(0); 
    }
    
    // ⭐️ MOVED: This is the "silent" makePayment method for the cash/change logic
    private static boolean makePayment(dbConnect cf, String email, int transactionId) {
        int userId = getUserId(cf, email); // Call our static helper
        String sql = "UPDATE tbl_transactions SET status = 'paid' " +
                     "WHERE transaction_id = ? AND user_id = ? AND status = 'pending_payment'";
        
        return cf.updateRecord(sql, transactionId, userId);
    }


    private static void updateTransaction(dbConnect cf, int transactionId, int newQuantity, String email) {
        // ⭐️ CHANGED: Call static method
        int userId = getUserId(cf, email);
        String oldTxnSql = "SELECT product_id, quantity, status FROM tbl_transactions WHERE transaction_id = ? AND user_id = ?";
        // ⭐️ CHANGED: Call with 'cf.'
        List<Map<String, Object>> txns = cf.fetchRecords(oldTxnSql, transactionId, userId);
        
        if (txns.isEmpty()) {
            System.out.println("Error: Transaction " + transactionId + " not found or does not belong to you.");
            return;
        }
        
        int productId = (Integer) txns.get(0).get("product_id");
        int oldQuantity = (Integer) txns.get(0).get("quantity");
        String status = (String) txns.get(0).get("status");

        if (!"pending_payment".equals(status)) {
            System.out.println("Error: Cannot update a transaction that is already paid or cancelled.");
            return;
        }

        String productSql = "SELECT price, stock FROM tbl_products WHERE product_id = ?";
        // ⭐️ CHANGED: Call with 'cf.'
        List<Map<String, Object>> products = cf.fetchRecords(productSql, productId);
        if (products.isEmpty()) {
            System.out.println("Error: Associated product not found. Aborting.");
            return;
        }
        
        double price = ((Number) products.get(0).get("price")).doubleValue(); // Safe cast
        int currentStock = ((Number) products.get(0).get("stock")).intValue(); // Safe cast
        
        int quantityChange = newQuantity - oldQuantity;
        int newStock = currentStock - quantityChange; 
        
        if (newStock < 0) {
            System.out.println("Error: Not enough stock for new quantity. Only " + (currentStock + oldQuantity) + " total available.");
            return;
        }
        
        String updateStockSql = "UPDATE tbl_products SET stock = ? WHERE product_id = ?";
        // ⭐️ CHANGED: Call with 'cf.'
        if (!cf.updateRecord(updateStockSql, newStock, productId)) {
            System.out.println("Error: Failed to update product stock. Aborting.");
            return;
        }

        double newTotalPrice = price * newQuantity;
        String updateTxnSql = "UPDATE tbl_transactions SET quantity = ?, total_price = ? WHERE transaction_id = ?";
        // Example:
        if (cf.updateRecord(updateTxnSql, newQuantity, newTotalPrice, transactionId)) {
            System.out.println("Transaction " + transactionId + " updated successfully. New total: " + newTotalPrice);
        } else {
            System.out.println("Error: Failed to update transaction. Rolling back stock.");
            // ⭐️ CHANGED: Call with 'cf.'
            cf.updateRecord(updateStockSql, currentStock, productId); // Rollback stock
        }
    }

    private static void deleteTransaction(dbConnect cf, int transactionId, String email) {
        // ⭐️ CHANGED: Call static method
        int userId = getUserId(cf, email);
        String oldTxnSql = "SELECT product_id, quantity, status FROM tbl_transactions WHERE transaction_id = ? AND user_id = ?";
        // ⭐️ CHANGED: Call with 'cf.'
        List<Map<String, Object>> txns = cf.fetchRecords(oldTxnSql, transactionId, userId);
        
        if (txns.isEmpty()) {
            System.out.println("Error: Transaction " + transactionId + " not found or does not belong to you.");
            return;
        }
        
        int productId = (Integer) txns.get(0).get("product_id");
        int oldQuantity = (Integer) txns.get(0).get("quantity");
        String status = (String) txns.get(0).get("status");
        
        if (!"pending_payment".equals(status)) {
            System.out.println("Error: Cannot delete a transaction that is already paid. Contact an admin.");
            return;
        }

        String productSql = "SELECT stock FROM tbl_products WHERE product_id = ?";
        // ⭐️ CHANGED: Call with 'cf.'
        List<Map<String, Object>> products = cf.fetchRecords(productSql, productId);
        int currentStock = 0;
        if (!products.isEmpty()) {
            currentStock = ((Number) products.get(0).get("stock")).intValue(); // Safe cast
        }
        
        int newStock = currentStock + oldQuantity;
        String updateStockSql = "UPDATE tbl_products SET stock = ? WHERE product_id = ?";
        // Example:
        if (!cf.updateRecord(updateStockSql, newStock, productId)) {
            System.out.println("Warning: Failed to restore stock, but will proceed with transaction deletion.");
        }

        String deleteTxnSql = "DELETE FROM tbl_transactions WHERE transaction_id = ?";
        // ⭐️ CHANGED: Call with 'cf.'
        if (cf.deleteRecord(deleteTxnSql, transactionId)) {
            System.out.println("Transaction " + transactionId + " deleted successfully. Stock restored.");
        } else {
            System.out.println("Error: Failed to delete transaction. Rolling back stock update.");
            // ⭐️ CHANGED: Call with 'cf.'
            cf.updateRecord(updateStockSql, currentStock, productId); // Rollback stock restore
        }
    }

    private static void adminDeleteTransaction(dbConnect cf, int transactionId, String adminUsername) {
        String sql = "DELETE FROM tbl_transactions WHERE transaction_id = ?";
        // ⭐️ CHANGED: Call with 'cf.'
        if (cf.deleteRecord(sql, transactionId)) {
            System.out.println("Transaction " + transactionId + " forcefully deleted by " + adminUsername + ". No stock was restored.");
        } else {
            System.out.println("Failed to delete transaction. It may not exist.");
        }
    }
}