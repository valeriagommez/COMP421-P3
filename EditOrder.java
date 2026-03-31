import java.sql.ResultSet;
import java.sql.SQLException;

public class EditOrder {

    // Finds the latest order made a by user
    public static int[] getLatestOrder(String email) throws SQLException {
        try {
            String querySQL =
                "SELECT orderID, storeID, total FROM Orders " +
                "WHERE email = '" + email + "' " +
                "AND timeStamp >= CURRENT_TIMESTAMP - 24 HOURS " +
                "ORDER BY timeStamp DESC FETCH FIRST 1 ROW ONLY";
            ResultSet rs = Main.statement.executeQuery(querySQL);
            if (rs.next()) {
                return new int[]{rs.getInt("orderID"), rs.getInt("storeID"), (int) rs.getFloat("total")};
            }
        } catch (SQLException e) {
            Main.sqlCode = e.getErrorCode();
            Main.sqlState = e.getSQLState();
        }
        return null;
    }

    // Prints current items in the order
    public static boolean printOrderItems(int orderID) throws SQLException {
        boolean hasItems = false;
        try {
            String querySQL =
                "SELECT f.name, oi.quantity, oi.price " +
                "FROM OrderItems oi JOIN Food f ON oi.foodID = f.foodID " +
                "WHERE oi.orderID = " + orderID;
            ResultSet rs = Main.statement.executeQuery(querySQL);
            System.out.println("\nCurrent items in order #" + orderID + ":");
            System.out.printf("  %-25s %-10s %-10s%n", "Item", "Qty", "Unit Price");
            System.out.println("  " + "-".repeat(45));
            while (rs.next()) {
                hasItems = true;
                System.out.printf("  %-25s %-10d $%-10.2f%n",
                    rs.getString("name"),
                    rs.getInt("quantity"),
                    rs.getFloat("price"));
            }
        } catch (SQLException e) {
            Main.sqlCode = e.getErrorCode();
            Main.sqlState = e.getSQLState();
        }
        return hasItems;
    }

    // Returns quantity of a food item currently in the order
    public static int getItemQtyInOrder(int orderID, int foodID) throws SQLException {
        try {
            String querySQL =
                "SELECT quantity FROM OrderItems " +
                "WHERE orderID = " + orderID + " AND foodID = " + foodID;
            ResultSet rs = Main.statement.executeQuery(querySQL);
            if (rs.next()) return rs.getInt("quantity");
        } catch (SQLException e) {
            Main.sqlCode = e.getErrorCode();
            Main.sqlState = e.getSQLState();
        }
        return -1; //return -1 if food item is not in order
    }

    // Increases quantity of an item in OrderItems, updates Inventory
    public static void addItem(int orderID, int foodID, float price, int qtyToAdd, int storeID) throws SQLException {
        int existing = getItemQtyInOrder(orderID, foodID);
        try {
            if (existing == -1) {
                // Item not yet in order, we insert it
                String insertSQL = "INSERT INTO OrderItems VALUES (" +
                    orderID + ", " + qtyToAdd + ", " + price + ", " + foodID + ")";
                Main.statement.executeUpdate(insertSQL);
            } else {
                // Item already in order, we update the quantity
                String updateSQL = "UPDATE OrderItems SET quantity = " + (existing + qtyToAdd) +
                    " WHERE orderID = " + orderID + " AND foodID = " + foodID;
                Main.statement.executeUpdate(updateSQL);
            }
            // Update Inventory
            String invSQL = "UPDATE Inventory SET numLeft = numLeft - " + qtyToAdd +
                " WHERE foodID = " + foodID + " AND storeID = " + storeID;
            Main.statement.executeUpdate(invSQL);
        } catch (SQLException e) {
            Main.sqlCode = e.getErrorCode();
            Main.sqlState = e.getSQLState();
            System.out.println("Code: " + Main.sqlCode + "  sqlState: " + Main.sqlState);
        }
    }

    // Decreases quantity of an item in OrderItems, adds it back to Inventory
    public static void removeItem(int orderID, int foodID, int qtyToRemove, int storeID) throws SQLException {
        int existing = getItemQtyInOrder(orderID, foodID);
        try {
            if (existing == qtyToRemove) {
                // Remove the row entirely if we are removing it completely
                String deleteSQL = "DELETE FROM OrderItems WHERE orderID = " + orderID +
                    " AND foodID = " + foodID;
                Main.statement.executeUpdate(deleteSQL);
            } else {
                // Decrease the quantity by the amount requested
                String updateSQL = "UPDATE OrderItems SET quantity = " + (existing - qtyToRemove) +
                    " WHERE orderID = " + orderID + " AND foodID = " + foodID;
                Main.statement.executeUpdate(updateSQL);
            }
            // Update Inventory
            String invSQL = "UPDATE Inventory SET numLeft = numLeft + " + qtyToRemove +
                " WHERE foodID = " + foodID + " AND storeID = " + storeID;
            Main.statement.executeUpdate(invSQL);

        } catch (SQLException e) {
            Main.sqlCode = e.getErrorCode();
            Main.sqlState = e.getSQLState();
            System.out.println("Code: " + Main.sqlCode + "  sqlState: " + Main.sqlState);
        }
    }

    // Recalculates total from OrderItems and updates Orders and Payments table
    public static void recalculateTotal(int orderID) throws SQLException {
        try {
            String sumSQL = "SELECT SUM(quantity * price) FROM OrderItems WHERE orderID = " + orderID;
            ResultSet rs = Main.statement.executeQuery(sumSQL);
            float newTotal = 0;
            if (rs.next()) newTotal = rs.getFloat(1);

            String updateOrderSQL = "UPDATE Orders SET total = " + newTotal +
                " WHERE orderID = " + orderID;
            Main.statement.executeUpdate(updateOrderSQL);

            String updatePaySQL = "UPDATE Payments SET total = " + newTotal +
                " WHERE orderID = " + orderID;
            Main.statement.executeUpdate(updatePaySQL);

            System.out.println("Updated order total: $" + String.format("%.2f", newTotal));
        } catch (SQLException e) {
            Main.sqlCode = e.getErrorCode();
            Main.sqlState = e.getSQLState();
            System.out.println("Code: " + Main.sqlCode + "  sqlState: " + Main.sqlState);
        }
    }
}