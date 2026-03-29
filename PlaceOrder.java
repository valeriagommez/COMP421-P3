import java.sql.SQLException;
import java.util.ArrayList;

public class PlaceOrder {

    public static String searchEmail(String emailInput) throws SQLException {
        String email = "";
        try
        {
            String querySQL = "SELECT email FROM Users WHERE email = '" + emailInput + "'";
//            System.out.println (querySQL) ;     // debugging
            java.sql.ResultSet rs = Main.statement.executeQuery ( querySQL ) ;

            while ( rs.next ( ) ) {
                email = rs.getString (1);
//                System.out.println ("email:  " + email);    // debugging
            }
//            System.out.println ("DONE");    // debugging
        }
        catch (SQLException e)
        {
            Main.sqlCode = e.getErrorCode(); // Get SQLCODE
            Main.sqlState = e.getSQLState(); // Get SQLSTATE

//            System.out.println("Code: " + Main.sqlCode + "  sqlState: " + Main.sqlState);
//            System.out.println(e);
        }
        return email;   // will be "" if we found no match in the Users table
    }

    public static int chooseStore(String storeIDinput) throws SQLException {
        int storeID = -1;

        try {
            String querySQL = "SELECT storeID FROM Stores WHERE storeID = " + storeIDinput ;
            java.sql.ResultSet rs = Main.statement.executeQuery ( querySQL ) ;
            while ( rs.next ( ) ) {
                storeID = rs.getInt (1);
            }
        }

        catch (SQLException e)  {
            Main.sqlCode = e.getErrorCode(); // Get SQLCODE
            Main.sqlState = e.getSQLState(); // Get SQLSTATE

//            System.out.println("Code: " + Main.sqlCode + "  sqlState: " + Main.sqlState);
//            System.out.println(e);
        }
        return storeID;   // will be -1 if we found no match in the Stores table
    }

    public static Float[] getFoodInfo(String foodInput) throws SQLException {
        float foodID = -1;
        float price = -1;
        try {
            String querySQL = "SELECT foodID, price FROM Food WHERE LOWER(name) = '" + foodInput.toLowerCase() + "'";
            java.sql.ResultSet rs = Main.statement.executeQuery ( querySQL ) ;
            while ( rs.next ( ) ) {
                foodID = rs.getInt (1);
                price = rs.getFloat (2);
            }
        }
        catch (SQLException e)  {
            Main.sqlCode = e.getErrorCode(); // Get SQLCODE
            Main.sqlState = e.getSQLState(); // Get SQLSTATE
        }
        return new Float[] {foodID, price};   // will be {-1, -1} if we found no match in the Food table
    }

    public static int getNumLeft(int foodID, int storeID) throws SQLException {
        int numLeft = -1;
        try {
            String querySQL = "SELECT numLeft FROM Inventory WHERE foodID=" + foodID + " AND storeID =" + storeID;
            java.sql.ResultSet rs = Main.statement.executeQuery ( querySQL ) ;
            while ( rs.next ( ) ) {
                numLeft = rs.getInt (1);
            }
        }
        catch (SQLException e)  {
            Main.sqlCode = e.getErrorCode(); // Get SQLCODE
            Main.sqlState = e.getSQLState(); // Get SQLSTATE
        }
        return numLeft; // should never be -1
    }

    public static void updateStock(ArrayList<Object[]> order, int storeID) throws SQLException {
        for (Object[] objects : order) {
            int curFoodID = (int) objects[0];
            int newNumLeft = (int) objects[3];

            try {
                String updateSQL = "UPDATE Inventory SET numLeft = " + newNumLeft + " WHERE storeID = " + storeID + " AND foodID = " + curFoodID;
                Main.statement.executeUpdate(updateSQL);
            }
            catch (SQLException e) {
                Main.sqlCode = e.getErrorCode(); // Get SQLCODE
                Main.sqlState = e.getSQLState(); // Get SQLSTATE
            }
        }
    }

    public static float calculateTotal (ArrayList<Object[]> order) {
        float total = 0;
        int curQty;
        float curPrice;

        for (Object[] objects : order) {
            curPrice = (float) objects[1];
            curQty = (int) objects[2];

            total = total + curPrice * curQty;
        }

        return total;
    }

    public static int appendOrder(float total, String email, int storeID) throws SQLException {
        int newOrderID = -1;
        try {
            String selectSQL = "SELECT COALESCE(MAX(orderID), 0) + 1 FROM Orders";
            java.sql.ResultSet rs = Main.statement.executeQuery(selectSQL);
            if (rs.next()) {
                newOrderID = rs.getInt(1);
            }

            String insertSQL = "INSERT INTO Orders VALUES (" + newOrderID + ", NOW(), "
                    + total + ", '" + email + "', " + storeID + ")";

            Main.statement.executeUpdate ( insertSQL ) ;
        }

        catch (SQLException e) {
            Main.sqlCode = e.getErrorCode(); // Get SQLCODE
            Main.sqlState = e.getSQLState(); // Get SQLSTATE

            System.out.println("Code: " + Main.sqlCode + "  sqlState: " + Main.sqlState);
            System.out.println(e);
        }

        return newOrderID;
    }

    public static void appendOrderItems(int newOrderID, ArrayList<Object[]> order) throws SQLException {
        int curFoodID;
        int curQty;
        float curPrice;

        for (Object[] objects : order) {
            curFoodID = (int) objects[0];
            curPrice = (float) objects[1];
            curQty = (int) objects[2];

            try {
                String insertSQL = "INSERT INTO OrderItems VALUES (" + newOrderID + ", "
                        + curQty + ", " + curPrice + ", " + curFoodID + ")";
//            System.out.println ( insertSQL ) ;
                Main.statement.executeUpdate ( insertSQL ) ;
//            System.out.println ( "DONE" ) ;
            }

            catch (SQLException e) {
                Main.sqlCode = e.getErrorCode(); // Get SQLCODE
                Main.sqlState = e.getSQLState(); // Get SQLSTATE

                System.out.println("Code: " + Main.sqlCode + "  sqlState: " + Main.sqlState);
                System.out.println(e);
            }
        }
    }

    public static void appendPayment(int newOrderID, String paymentInput, float total, String email) throws SQLException {
        String insertSQL;
        if (paymentInput.equals("1")) {
            insertSQL = "INSERT INTO Payments VALUES (" + newOrderID + ", "
                    + "'debit card'" + ", " + total + ", NOW(), '" + email + "')";
        }

        else if (paymentInput.equals("2")) {
            insertSQL = "INSERT INTO Payments VALUES (" + newOrderID + ", "
                    + "'credit card'" + ", " + total + ", NOW(), '" + email + "')";
        }

        else {
            insertSQL = "INSERT INTO Payments VALUES (" + newOrderID + ", "
                    + "'digital wallet'" + ", " + total + ", NOW(), '" + email + "')";
        }

        try {
            Main.statement.executeUpdate ( insertSQL ) ;
        }

        catch (SQLException e) {
            Main.sqlCode = e.getErrorCode(); // Get SQLCODE
            Main.sqlState = e.getSQLState(); // Get SQLSTATE

            System.out.println("Code: " + Main.sqlCode + "  sqlState: " + Main.sqlState);
            System.out.println(e);
        }
    }



}
