import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class DatabaseManager {
    private static final String URL = "jdbc:sqlite:spacex.db";

    private DatabaseManager() {}

    public static void initialize() throws SQLException {
        try (Connection con = DriverManager.getConnection(URL);
             Statement stmt = con.createStatement()) {
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS player_scores (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "player_name TEXT NOT NULL COLLATE NOCASE, " +
                "score INTEGER NOT NULL CHECK(score >= 0), " +
                "played_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)"
            );
        }
    }

    public static void saveScore(String name, int score)
            throws SQLException {
        String sql =
            "INSERT INTO player_scores(player_name, score) VALUES (?, ?)";

        try (Connection con = DriverManager.getConnection(URL);
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, Math.max(0, score));
            ps.executeUpdate();
        }
    }

    public static List<String> getLeaderboard() throws SQLException {
        List<String> rows = new ArrayList<>();
        String sql =
            "SELECT player_name, MAX(score) AS best_score " +
            "FROM player_scores GROUP BY player_name " +
            "ORDER BY best_score DESC, player_name COLLATE NOCASE LIMIT 20";

        try (Connection con = DriverManager.getConnection(URL);
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int rank = 1;
            while (rs.next()) {
                rows.add(rank++ + ". " + rs.getString("player_name")
                    + " - " + rs.getInt("best_score") + " points");
            }
        }
        return rows;
    }
}
