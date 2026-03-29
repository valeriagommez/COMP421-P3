import java.sql.ResultSet;
import java.sql.SQLException;

public class FoodLookup {

    public static int getFoodID (String foodInput) throws SQLException {
        int foodID = -1;
        try
        {
            String querySQL = "SELECT foodID FROM Food WHERE LOWER(name) = '" + foodInput.toLowerCase() + "'";
            ResultSet rs = Main.statement.executeQuery ( querySQL ) ;

            while ( rs.next ( ) ) {
                foodID = rs.getInt (1);
            }
        }
        catch (SQLException e)
        {
            Main.sqlCode = e.getErrorCode(); // Get SQLCODE
            Main.sqlState = e.getSQLState(); // Get SQLSTATE
//            System.out.println("Code: " + Main.sqlCode + "  sqlState: " + Main.sqlState);
//            System.out.println(e);
        }
        return foodID;   // will be -1 if we found no match in the Food table
    }

    public static int getAlternateStore(int foodID) throws SQLException {
        int otherStoreID = -1;

        try {
            String querySQL = "SELECT storeID FROM Inventory WHERE foodID = " + foodID +
                    "ORDER BY numLeft DESC LIMIT 1";
            ResultSet rs = Main.statement.executeQuery ( querySQL ) ;

            if (rs.next()) {
                otherStoreID = rs.getInt(1);
            }
        }
        catch (SQLException e) {
            Main.sqlCode = e.getErrorCode(); // Get SQLCODE
            Main.sqlState = e.getSQLState(); // Get SQLSTATE
//            System.out.println("Code: " + Main.sqlCode + "  sqlState: " + Main.sqlState);
//            System.out.println(e);
        }
        return otherStoreID;
    }

}

