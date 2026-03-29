import com.ibm.db2.jcc.DB2Driver;

import java.sql.* ;
import java.util.ArrayList;
import java.util.Scanner;


class Main
{
    static int sqlCode=0;      // Variable to hold SQLCODE
    static String sqlState="00000";  // Variable to hold SQLSTATE
    static Statement statement ;
    static Connection con;

    public static Statement setupDatabase() throws SQLException {
        // Register the driver.  You must register the driver before you can use it.
        try { DriverManager.registerDriver ( new DB2Driver() ) ; }
        catch (Exception cnfe){ System.out.println("Class not found"); }

        // This is the url you must use for DB2.
        String url = "jdbc:db2://winter2026-comp421.cs.mcgill.ca:50000/comp421";

        //REMEMBER to remove your user id and password before submitting your code!!
        String your_userid = "cs421g09";
        String your_password = "Group-09!!!";

        if(your_userid == null && (your_userid = System.getenv("SOCSUSER")) == null)
        {
            System.err.println("Error!! do not have a password to connect to the database!");
            System.exit(1);
        }
        if(your_password == null && (your_password = System.getenv("SOCSPASSWD")) == null)
        {
            System.err.println("Error!! do not have a password to connect to the database!");
            System.exit(1);
        }
        con = DriverManager.getConnection (url,your_userid,your_password) ;
        statement = con.createStatement ( ) ;

        return statement;
    }

    public static void noMatchFood(String foodInput) {
        System.out.println("ERROR : There's no food with name '" + foodInput +
                "'.\nTo restock the inventory, you can choose option (6), but for now, you can choose another item.");
    }

    public static void selectStoreText() throws SQLException {
        System.out.println("Select your store from the options below : ");
        System.out.println(" (1) Downtown Market");
        System.out.println(" (2) Laval Central Market");
        System.out.println(" (3) Sherbrooke Fresh Market");
        System.out.println(" (4) Longueuil Market");
        System.out.println(" (5) Brossard Market");
        System.out.print("Enter the ID of your preferred store : ");
    }

    public static void placeOrder(Scanner scanner)  throws SQLException {
        // important attributes
        String email;
        int storeID = 0;
        ArrayList<Object[]> newOrder = new ArrayList<Object[]>();

        System.out.println("\n(1) PLACING AN ORDER\n");
        System.out.print("Enter your email : ");
        String emailInput = scanner.nextLine();

        email = PlaceOrder.searchEmail(emailInput);

        // if there's no account tied to that email, then go back to the menu
        if (email.isEmpty()) {
            System.out.println("ERROR : There's no user with email address '" + emailInput + "'. To create an account, you can choose option (4).");
            return;
        }

        // getting the store the user wants to order from
        selectStoreText();
        String storeIDinput = scanner.nextLine();
        storeID = PlaceOrder.chooseStore(storeIDinput);

        // if there's no store with the ID entered
        if (storeID == -1) {
            System.out.println("ERROR : There's no store with ID '" + storeIDinput + "'. Choose an ID between 1 and 5");
            return;
        }

        // choosing the food the user wants to order
        System.out.println("Great! Let's start building your order.");
        System.out.print("\nEnter the name of the food item you'd like to order : ");
        String foodInput = "NULL";

        int curQty;
        Float[] curFoodInfo = new Float[2]; // curFoodInfo[0] = foodID, curFoodInfo[1] = price
        float curFoodID ;
        float curFoodPrice;
        String paymentInput;

        int iterations = 0;
        while (!foodInput.isEmpty()) {
            Object[] currentOrder = new Object[4];

            if (iterations != 0) {  // give the user an option to stop ordering
                System.out.print("\nEnter the name of the food item you'd like to order (or press ENTER to stop) : ");
            }

            foodInput = scanner.nextLine();
            curFoodInfo = PlaceOrder.getFoodInfo(foodInput);
            curFoodID = curFoodInfo[0];
            curFoodPrice = curFoodInfo[1];

            // prevent the printing of the messages in the last iteration
            if (foodInput.isEmpty()) {
                continue;
            }

            // if there's no food with the name entered
            if (curFoodInfo[0] == -1) {
                noMatchFood(foodInput);
                System.out.println("\nEnter the name of the food item you'd like to order (or press ENTER to stop) : ");
                continue;
            }

            // if the food exists, then fetch ask the quantity
            System.out.print("Enter how many you would like to order : ");
            if (scanner.hasNextInt()) { // Check if the next input is an integer
                curQty = scanner.nextInt();
                scanner.nextLine();
//                System.out.println("You entered: " + curQty);
            } else {
                System.out.println("Invalid input. Please enter a valid integer.");
                scanner.nextLine();
                continue;
            }

            // if numLeft < curQty for that specific food, then display an error message
            int numLeft = PlaceOrder.getNumLeft((int) curFoodID, storeID);
            if (numLeft <= 0) {
                System.out.println("This item is currently out of stock in your store.\n" +
                        "To restock the inventory, you can choose option (6), but for now, you can choose another item.");
                continue;
            }
            if (numLeft < curQty) {
                System.out.println("There are currently only " + numLeft + " of such items left in your store. Choose a different quantity.");
                continue;
            }

            currentOrder[0] =  (int) curFoodID;
            currentOrder[1] =  curFoodPrice;
            currentOrder[2] =  (int) curQty;
            currentOrder[3] =  (int) numLeft - curQty;

            newOrder.add(currentOrder);
            iterations++;
        }

        if (newOrder.isEmpty()) {
            System.out.println("No items were ordered. Returning to menu.");
            return;
        }

        // updating Inventory table
        PlaceOrder.updateStock(newOrder, storeID);

        // updating Orders table
        float total = PlaceOrder.calculateTotal(newOrder);
        int newOrderID = PlaceOrder.appendOrder(total, email, storeID);

        // updating the OrderItems table
        PlaceOrder.appendOrderItems(newOrderID, newOrder);

        // updating the Payment table
        System.out.println("The total comes down to " + total + "$");
        System.out.println("Select your payment type from the options below : ");
        System.out.println(" (1) Debit card");
        System.out.println(" (2) Credit card");
        System.out.println(" (3) Digital wallet");
        System.out.print("Desired payment type : ");
        paymentInput = scanner.nextLine();

        while (!(paymentInput.equals("1") || paymentInput.equals("2") || paymentInput.equals("3"))) {
            System.out.println("Invalid input. Please enter a valid payment type.");
            paymentInput = scanner.nextLine();
        }
        PlaceOrder.appendPayment(newOrderID, paymentInput, total, email);

        System.out.println("Your order has been processed, thank you for shopping with us!\n" +
                "Order ID : " + newOrderID);
    } // ending of placeOrder

    public static void editOrder(Scanner scanner)  throws SQLException {
        System.out.println("\n(2) EDITING AN ORDER\n");
    }

    public static void foodLookup(Scanner scanner)  throws SQLException {
        System.out.println("\n(3) LOOK UP FOOD AVAILABILITY\n");
        int storeID = -1;
        String[] storeNames = new String[6];
        storeNames[0] = "dummy value";
        storeNames[1] = "Downtown Market";
        storeNames[2] = "Laval Central Market";
        storeNames[3] = "Sherbrooke Fresh Market";
        storeNames[4] = "Longueuil Market";
        storeNames[5] = "Brossard Market";

        // if there's no store with the ID entered
        while (storeID == -1) {
            selectStoreText();
            String storeIDinput = scanner.nextLine();
            storeID = PlaceOrder.chooseStore(storeIDinput);

            // if there's no store with the ID entered
            if (storeID == -1) {
                System.out.println("ERROR : There's no store with ID '" + storeIDinput + "'. Choose an ID between 1 and 5\n");
            }
        }

        System.out.print("Enter the name of the food item you'd like to look up : ");
        String foodInput = scanner.nextLine();

        int foodID = FoodLookup.getFoodID(foodInput);
        while (foodID == -1) {
            noMatchFood(foodInput);
            System.out.print("Enter the name of the food item you'd like to look up : ");
            foodInput = scanner.nextLine();
            foodID = FoodLookup.getFoodID(foodInput);
        }

        int numLeft = PlaceOrder.getNumLeft(foodID, storeID);
        if (numLeft <= 0) {
            // suggest store that has the most items of foodID in stock
            int otherStoreID = FoodLookup.getAlternateStore(foodID);
            String otherStoreName = "";

            if  (otherStoreID != -1) {
                otherStoreName = storeNames[otherStoreID];
            }

            if ((otherStoreID == -1) || (otherStoreID == storeID)){
                System.out.println("OUT OF STOCK : There are no " + foodInput + " items left in stock.\n" +
                        "We suggest you restock the stores by selecting option (6).");
            }
            else {
                System.out.println("OUT OF STOCK : There are no " + foodInput + " items left in stock in our "  + storeNames[storeID] + ".\n"
                        + "Our store with the highest stock for that product is : " + otherStoreName + " (storeID : " + otherStoreID + ")");
            }
        }
        else if (numLeft <= 5) {
            System.out.println("LOW STOCK : There are currently only " + numLeft + " " + foodInput + " items left in our " + storeNames[storeID] + ".");
        }
        else {
            System.out.println("There are currently " + numLeft + " " + foodInput + " items left in our " + storeNames[storeID] + ".");
        }
    }


    // helper function
    public static void addUser(String email, String name, String address, String phoneNum) {
        try {
            String insertSQL = "INSERT INTO Users VALUES ('" + email + "', '" + name + "','" + address + "','" + phoneNum + "')";
            Main.statement.executeUpdate ( insertSQL ) ;
        }

        catch (SQLException e) {
            Main.sqlCode = e.getErrorCode(); // Get SQLCODE
            Main.sqlState = e.getSQLState(); // Get SQLSTATE
            System.out.println("Code: " + Main.sqlCode + "  sqlState: " + Main.sqlState);
            System.out.println(e);
        }
    }
    public static String getNumericInput(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine();
        while (!input.matches("\\d+")) {
            System.out.println("ERROR : Please enter numbers only.");
            System.out.print(prompt);
            input = scanner.nextLine();
        }
        return input;
    }

    public static String capitalizeFirst(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    public static void createAccount(Scanner scanner) throws SQLException {
        System.out.println("\n(4) CREATING AN ACCOUNT\n");

        System.out.print("Enter your email : ");
        String emailInput = scanner.nextLine();
        String email = PlaceOrder.searchEmail(emailInput);

        while (!(email.isEmpty())) {
            System.out.println("ERROR : There's already a user with email address '" + emailInput + "'. Choose another email address.");
            System.out.print("Enter your email : ");
            emailInput = scanner.nextLine();
            email = PlaceOrder.searchEmail(emailInput);
        }

        System.out.print("Enter your first name : ");
        String firstNameInput = capitalizeFirst(scanner.nextLine());

        System.out.print("Enter your last name : ");
        String lastNameInput = capitalizeFirst(scanner.nextLine());
        String name = firstNameInput + " " + lastNameInput;

        String streetNum = getNumericInput(scanner, "Enter your street number : ");
        System.out.print("Enter the name of your street : ");
        String streetName = capitalizeFirst(scanner.nextLine());
        System.out.print("Enter the name of the city : ");
        String cityName = capitalizeFirst(scanner.nextLine());
        System.out.print("Enter your postal code in the format 'A1B 2C3': ");
        String postalCode = scanner.nextLine().toUpperCase();
        String address = streetNum + " Rue " + streetName + " " + cityName + " QC " + postalCode;
        String phoneNum = getNumericInput(scanner, "Enter your phone number : ");

        addUser(emailInput, name, address, phoneNum);
        System.out.println("Your account has been created successfully!");
    }

    public static void editUser(Scanner scanner)  throws SQLException {
        System.out.println("\n(5) UPDATING USER\n");

    }

    public static void restockFood(Scanner scanner)  throws SQLException {
        System.out.println("\n(6) RESTOCKING FOOD\n");

    }

    public static void mainLoop() throws SQLException {
        // setting up the user input scanner :
        Scanner scanner = new Scanner(System.in);
        String userChoice = "0";

        System.out.println("Welcome to your online grocery store!");
        System.out.println("-------------------------------------");
        System.out.println("What would you like to do?");
        System.out.println(" (1) Place an order");
        System.out.println(" (2) Edit an order");
        System.out.println(" (3) Look up food availability");
        System.out.println(" (4) Create an account");
        System.out.println(" (5) Update user information");
        System.out.println(" (6) Restock food item");
        System.out.println(" (7) Quit");

        // main loop stopping only when the user chooses to quit (option 6)
        while( !userChoice.equals("7") ) {
            System.out.println("-------------------------------------");
            System.out.print("Please enter your choice: ");
            userChoice = scanner.nextLine();

            if (userChoice.equals("1")) {
                placeOrder(scanner);
            }
            else if (userChoice.equals("2")) {
                editOrder(scanner);
            }
            else if (userChoice.equals("3")) {
                foodLookup(scanner);
            }
            else if (userChoice.equals("4")) {
                createAccount(scanner);
            }
            else if (userChoice.equals("5")) {
                editUser(scanner);
            }
            else if (userChoice.equals("6")) {
                restockFood(scanner);
            }
            else if (userChoice.equals("7")) {
                System.out.println("Have a great day!");
            }
            else {
                System.out.println("Invalid choice. Try again!");
            }
        }
    }

    public static void main(String[] args) throws SQLException {
        statement = setupDatabase();
        mainLoop();

        statement.close ( ) ;
        con.close ( ) ;
    }
}
