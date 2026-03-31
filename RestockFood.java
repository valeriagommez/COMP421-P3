import java.sql.ResultSet;
import java.sql.SQLException;

public class RestockFood {

    public static void updateExistingStock(int foodID, int storeID, int qtyToAdd) throws SQLException {
        try {
            String updateSQL = "UPDATE Inventory SET numLeft = numLeft + " + qtyToAdd +
                " WHERE foodID = " + foodID + " AND storeID = " + storeID;
            Main.statement.executeUpdate(updateSQL);
        } catch (SQLException e) {
            Main.sqlCode = e.getErrorCode();
            Main.sqlState = e.getSQLState();
            System.out.println("Code: " + Main.sqlCode + "  sqlState: " + Main.sqlState);
        }
    }

    // Returns new foodID
    public static int insertNewFood(String name, float price, String availability,
                                    String localProd, String fruitOrVeg, String organic,
                                    String meatTypeSQL, String bestBeforeSQL,
                                    String categorySQL, String refrigeratedSQL) throws SQLException {
        int newFoodID = -1;
        try {
            String idSQL = "SELECT COALESCE(MAX(foodID), 0) + 1 FROM Food";
            ResultSet rs = Main.statement.executeQuery(idSQL);
            if (rs.next()) newFoodID = rs.getInt(1);

            String insertFoodSQL = "INSERT INTO Food VALUES (" +
                newFoodID     + ", '" +
                name          + "', " +
                price         + ", '" +
                availability  + "', " +
                localProd     + ", " +
                fruitOrVeg    + ", " +
                organic       + ", " +
                meatTypeSQL   + ", " +
                bestBeforeSQL + ", " +
                categorySQL   + ", " +
                refrigeratedSQL + ")";
            Main.statement.executeUpdate(insertFoodSQL);
        } catch (SQLException e) {
            Main.sqlCode = e.getErrorCode();
            Main.sqlState = e.getSQLState();
            System.out.println("Code: " + Main.sqlCode + "  sqlState: " + Main.sqlState);
            System.out.println(e);
            return -1;
        }
        return newFoodID;
    }

    public static void insertInventoryRow(int storeID, int foodID, int qty) throws SQLException {
        try {
            String insertInvSQL = "INSERT INTO Inventory VALUES (" + storeID + ", " + foodID + ", " + qty + ")";
            Main.statement.executeUpdate(insertInvSQL);
        } catch (SQLException e) {
            Main.sqlCode = e.getErrorCode();
            Main.sqlState = e.getSQLState();
            System.out.println("Code: " + Main.sqlCode + "  sqlState: " + Main.sqlState);
        }
    }
}