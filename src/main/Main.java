import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createWindow);
    }

    private static void createWindow() {
        SoundEffects.initialize();
        try {
            DatabaseManager.initialize();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                    null, "Database initialization failed:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }

        JFrame window = new JFrame("SpaceX");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        CardLayout cardLayout = new CardLayout();
        JPanel screens = new JPanel(cardLayout);

        JPanel menuPanel = new JPanel(new GridLayout(5, 1, 10, 10));
        menuPanel.setBorder(BorderFactory.createEmptyBorder(35, 100, 35, 100));
        menuPanel.setBackground(new Color(10, 15, 35));

        JLabel title = new JLabel("SpaceX", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 48));
        title.setForeground(Color.CYAN);

        JLabel tagline = new JLabel(
                "Do you dare to defeat Shiva...", SwingConstants.CENTER);
        tagline.setFont(new Font("Arial", Font.ITALIC, 20));
        tagline.setForeground(Color.WHITE);

        JTextField playerNameField = new JTextField();
        playerNameField.setFont(new Font("Arial", Font.PLAIN, 18));
        playerNameField.setHorizontalAlignment(SwingConstants.CENTER);
        playerNameField.setToolTipText("Enter your player name");

        JButton startButton = new JButton("START GAME");
        JButton leaderboardButton = new JButton("LEADERBOARD");

        menuPanel.add(title);
        menuPanel.add(tagline);
        menuPanel.add(playerNameField);
        menuPanel.add(startButton);
        menuPanel.add(leaderboardButton);

        GamePanel gamePanel = new GamePanel();

        screens.add(menuPanel, "MENU");
        screens.add(gamePanel, "GAME");

        startButton.addActionListener(event -> {
            String name = playerNameField.getText().trim();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(
                        window, "Please enter your player name.");
                return;
            }

            if (name.length() > 30) {
                JOptionPane.showMessageDialog(
                        window, "Player names must be 30 characters or fewer.");
                return;
            }

            gamePanel.setPlayerName(name);
            cardLayout.show(screens, "GAME");
            gamePanel.requestFocusInWindow();
        });

        leaderboardButton.addActionListener(event -> {
            try {
                List<String> rows = DatabaseManager.getLeaderboard();
                String message = rows.isEmpty()
                        ? "No scores saved yet. Play a game first!"
                        : String.join("\n", rows);

                JOptionPane.showMessageDialog(
                        window, message, "SpaceX Leaderboard",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(
                        window, "Could not load leaderboard:\n" + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        window.add(screens);
        window.setSize(800, 500);
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }
}
