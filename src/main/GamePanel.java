
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class GamePanel extends JPanel {

    private final int[] starX = new int[100];
    private final int[] starY = new int[100];
    private final Random random = new Random();

    private int baseHealth = 100;
    private final int maxBaseHealth = 100;
    private final int baseDamage = 20;

    private int playerHealth = 100;
    private int spawnCounter = 0;

    private int shipX = 400;
    private final int shipSpeed = 13;

    private boolean gameOver = false;
    private boolean paused = false;

    private final Set<Integer> pressedKeys = new HashSet<>();

    private final ArrayList<Rectangle> bullets = new ArrayList<>();
    private final ArrayList<Projectile> enemyBullets = new ArrayList<>();
    private final ArrayList<Enemy> enemies = new ArrayList<>();

    private BufferedImage scoutImage;
    private BufferedImage playerShipImage;
    private BufferedImage premImage;
    private BufferedImage kunalImage;
    private BufferedImage shivaImage;

    private int score = 0;
    private String playerName = "Player";
    private boolean scoreSaved = false;

    public void setPlayerName(String playerName) {
        if (playerName != null && !playerName.trim().isEmpty()) {
            this.playerName = playerName.trim();
        }
    }

    // Shiva progression
    private int nextShivaScore = 500;
    private int pendingShivaEncounters = 0;
    private int shivaEncounters = 0;
    private boolean shivaActive = false;

    private int shivaAttackPhase = 0;
    private int shivaSpiralAngle = 0;

    // Weapon level: 1 = single shot, 2 = triple shot, 3 = five shots
    private int weaponLevel = 1;

    private Timer gameTimer;

    private enum EnemyType {
        KUSHAL, PREM, KUNAL, SHIVA
    }

    private class Projectile {
        Rectangle bounds;
        int vx;
        int vy;

        Projectile(int x, int y, int vx, int vy) {
            bounds = new Rectangle(x, y, 8, 14);
            this.vx = vx;
            this.vy = vy;
        }
    }

    private class Enemy {
        Rectangle bounds;
        EnemyType type;

        int health;
        int maxHealth;
        int speed;
        int direction;

        int moveCounter = 0;
        int attackCounter = 0;
        int age = 0;

        Enemy(int x, EnemyType type) {
            this.type = type;

            if (type == EnemyType.SHIVA) {
                bounds = new Rectangle(x, 40, 110, 80);
                maxHealth = 30 + shivaEncounters * 20;
                health = maxHealth;
                speed = Math.min(2 + shivaEncounters, 7);

            } else if (type == EnemyType.PREM) {
                bounds = new Rectangle(x, 20, 65, 75);
                maxHealth = 6 + shivaEncounters;
                health = maxHealth;
                speed = 1 + Math.min(shivaEncounters / 3, 2);

            } else if (type == EnemyType.KUNAL) {
                bounds = new Rectangle(x, 20, 48, 52);
                maxHealth = 1 + shivaEncounters / 2;
                health = maxHealth;
                speed = 2 + Math.min(shivaEncounters / 2, 3);

            } else {
                bounds = new Rectangle(x, 20, 60, 65);
                maxHealth = 2 + shivaEncounters / 2;
                health = maxHealth;

                // Kushal is slower at the start.
                speed = 1 + Math.min(shivaEncounters / 4, 1);
            }

            direction = random.nextBoolean() ? 1 : -1;
        }
    }

    public GamePanel() {
        setBackground(Color.BLACK);

        loadImages();
        initializeStars();
        setupControls();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                initializeStars();
                repaint();
            }
        });

        gameTimer = new Timer(16, e -> updateGame());
        gameTimer.start();
    }

    private BufferedImage readImage(String path) {
        try {
            BufferedImage image = ImageIO.read(new File(path));
            if (image == null) {
                System.err.println("Invalid image: " + path);
            }
            return image;
        } catch (IOException e) {
            System.err.println("Could not load " + path);
            return null;
        }
    }

    private void loadImages() {
        scoutImage = readImage("assets/kushal_scout.png");
        playerShipImage = readImage("assets/player_ship.png");
        premImage = readImage("assets/prem.png");
        kunalImage = readImage("assets/kunal.png");
        shivaImage = readImage("assets/shiva.png");
    }

    private void initializeStars() {
        int w = Math.max(getWidth(), 800);
        int h = Math.max(getHeight(), 500);

        for (int i = 0; i < starX.length; i++) {
            starX[i] = random.nextInt(w);
            starY[i] = random.nextInt(h);
        }
    }

    private void setupControls() {
        bindKey("LEFT", "leftPressed", () -> {
            if (!paused && !gameOver) {
                pressedKeys.add(KeyEvent.VK_LEFT);
            }
        });

        bindKey("released LEFT", "leftReleased",
                () -> pressedKeys.remove(KeyEvent.VK_LEFT));

        bindKey("RIGHT", "rightPressed", () -> {
            if (!paused && !gameOver) {
                pressedKeys.add(KeyEvent.VK_RIGHT);
            }
        });

        bindKey("released RIGHT", "rightReleased",
                () -> pressedKeys.remove(KeyEvent.VK_RIGHT));

        bindKey("pressed SPACE", "fire", () -> {
            if (!gameOver && !paused) {
                firePlayerWeapon();
            }
        });

        bindKey("P", "togglePause", () -> {
            if (!gameOver) {
                paused = !paused;
                pressedKeys.clear();
                repaint();
            }
        });

        bindKey("R", "restart", () -> {
            if (gameOver) {
                restartGame();
            }
        });
    }

    private void bindKey(String key, String name, Runnable action) {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(key), name);

        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                action.run();
            }
        });
    }

    private void firePlayerWeapon() {
        int shipY = getHeight() - 130;
        int[] offsets;

        if (weaponLevel == 1) {
            offsets = new int[] { 0 };
        } else if (weaponLevel == 2) {
            offsets = new int[] { -18, 0, 18 };
        } else {
            offsets = new int[] { -30, -15, 0, 15, 30 };
        }

        for (int offset : offsets) {
            bullets.add(new Rectangle(
                    shipX + offset - 3, shipY, 6, 14));
                    SoundEffects.playBullet();
        }
    }

    private void spawnEnemy() {
        int roll = random.nextInt(100);

        EnemyType type;
        if (roll < 35) {
            type = EnemyType.PREM;
        } else if (roll < 70) {
            type = EnemyType.KUNAL;
        } else {
            type = EnemyType.KUSHAL;
        }

        int width = type == EnemyType.PREM ? 65
                : type == EnemyType.KUNAL ? 48 : 60;

        int x = random.nextInt(
                Math.max(1, getWidth() - width));

        enemies.add(new Enemy(x, type));
    }

    // Queue every 500-point milestone, even if Shiva is still alive.
    private void checkShivaMilestones() {
        while (score >= nextShivaScore) {
            pendingShivaEncounters++;
            nextShivaScore += 500;
        }

        if (!shivaActive && pendingShivaEncounters > 0) {
            spawnShiva();
        }
    }

    private void spawnShiva() {
        // Clear ordinary enemies and all existing projectiles.
        enemies.clear();
        bullets.clear();
        enemyBullets.clear();

        int x = Math.max(0, (getWidth() - 110) / 2);

        enemies.add(new Enemy(x, EnemyType.SHIVA));
        shivaActive = true;
        pendingShivaEncounters--;

        System.out.println(
                "SHIVA ENCOUNTER " + (shivaEncounters + 1)
                        + " — Health: "
                        + (30 + shivaEncounters * 20));

        shivaAttackPhase = 0;
        shivaSpiralAngle = 0;
    }

    private void updateGame() {
        if (gameOver || paused) {
            return;
        }

        if (pressedKeys.contains(KeyEvent.VK_LEFT)) {
            shipX -= shipSpeed;
        }

        if (pressedKeys.contains(KeyEvent.VK_RIGHT)) {
            shipX += shipSpeed;
        }

        shipX = Math.max(35, Math.min(getWidth() - 35, shipX));

        checkShivaMilestones();

        if (!shivaActive) {
            spawnCounter++;

            if (spawnCounter >= 60) {
                spawnEnemy();
                spawnCounter = 0;
            }
        } else {
            spawnCounter = 0;
        }

        // Update enemy movement and attacks.
        for (Enemy enemy : enemies) {
            enemy.age++;

            if (enemy.type == EnemyType.SHIVA) {
                updateShiva(enemy);
            } else if (enemy.type == EnemyType.KUNAL) {
                updateKunal(enemy);
            } else {
                enemy.bounds.y += enemy.speed;
            }

            if (enemy.type == EnemyType.PREM) {
                enemy.attackCounter++;

                if (enemy.attackCounter >= 100) {
                    fireAimedShot(enemy, 0);
                    enemy.attackCounter = 0;
                }
            }
        }

        // Move player bullets.
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Rectangle bullet = bullets.get(i);
            bullet.y -= 10;

            if (bullet.y + bullet.height < 0) {
                bullets.remove(i);
            }
        }

        // Move enemy projectiles along their individual trajectories.
        for (int i = enemyBullets.size() - 1; i >= 0; i--) {
            Projectile projectile = enemyBullets.get(i);

            projectile.bounds.x += projectile.vx;
            projectile.bounds.y += projectile.vy;

            if (projectile.bounds.y > getHeight()
                    || projectile.bounds.y + projectile.bounds.height < 0
                    || projectile.bounds.x > getWidth()
                    || projectile.bounds.x + projectile.bounds.width < 0) {
                enemyBullets.remove(i);
            }
        }

        // Player bullets hit enemies and Shiva.
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Rectangle bullet = bullets.get(i);
            boolean hit = false;

            for (int j = enemies.size() - 1; j >= 0; j--) {
                Enemy enemy = enemies.get(j);

                if (bullet.intersects(enemy.bounds)) {
                    enemy.health--;
                    hit = true;

                    if (enemy.health <= 0) {
                        if (enemy.type == EnemyType.SHIVA) {
                            defeatShiva(enemy);
                        } else {
                            if (enemy.type == EnemyType.PREM) {
                                score += 50;
                            } else if (enemy.type == EnemyType.KUNAL) {
                                score += 25;
                            } else {
                                score += 15;
                            }

                            // Play the matching ordinary enemy's defeat sound.
                            SoundEffects.play(enemy.type.name().toLowerCase());

                            enemies.remove(j);
                        }
                    }

                    break;
                }
            }

            if (hit) {
                bullets.remove(i);
            }
        }

        // Check projectiles against the player's ship.
        Rectangle playerBounds = new Rectangle(
                shipX - 30, getHeight() - 150, 60, 100);

        for (int i = enemyBullets.size() - 1; i >= 0; i--) {
            if (enemyBullets.get(i).bounds.intersects(playerBounds)) {
                enemyBullets.remove(i);
                playerHealth -= 10;

                if (playerHealth <= 0) {
                    playerHealth = 0;
                    endGame();
                    return;
                }
            }
        }

        // Ordinary enemies damage the base.
        // Shiva is excluded: his objective is the player's ship.
        int baseY = getHeight() - 40;

        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);

            if (enemy.type != EnemyType.SHIVA
                    && enemy.bounds.y + enemy.bounds.height >= baseY) {

                enemies.remove(i);
                baseHealth -= baseDamage;

                if (baseHealth <= 0) {
                    baseHealth = 0;
                    endGame();
                    return;
                }
            }
        }

        checkShivaMilestones();
        repaint();
    }

    private void updateShiva(Enemy shiva) {
        shiva.bounds.x += shiva.speed * shiva.direction;
        shiva.bounds.y = 40
                + (int) (Math.sin(shiva.age / 12.0) * 24);

        if (shiva.bounds.x <= 0) {
            shiva.bounds.x = 0;
            shiva.direction = 1;
        }

        if (shiva.bounds.x + shiva.bounds.width >= getWidth()) {
            shiva.bounds.x = Math.max(
                    0, getWidth() - shiva.bounds.width);
            shiva.direction = -1;
        }

        shiva.attackCounter++;

        boolean enraged = shiva.health <= shiva.maxHealth / 3;

        int attackInterval = enraged ? 20
                : Math.max(35, 75 - shivaEncounters * 5);

        if (shiva.attackCounter < attackInterval) {
            return;
        }

        shiva.attackCounter = 0;
        shivaAttackPhase++;

        int centerX = shiva.bounds.x + shiva.bounds.width / 2;
        int startY = shiva.bounds.y + shiva.bounds.height;

        // Rotate between three main attack patterns.
        int pattern = shivaAttackPhase % 3;

        if (pattern == 0) {
            // COSMIC BARRAGE: wide fan of aimed projectiles.
            for (int offset = -100; offset <= 100; offset += 25) {
                fireAimedShot(shiva, offset);
            }

        } else if (pattern == 1) {
            // SPIRAL ATTACK: rotating projectiles.
            int count = enraged ? 12 : 8;
            int radiusSpeed = enraged ? 7 : 5;

            for (int i = 0; i < count; i++) {
                double angle = Math.toRadians(
                        shivaSpiralAngle + i * (360.0 / count));

                int vx = (int) Math.round(
                        Math.cos(angle) * radiusSpeed);
                int vy = Math.max(2, (int) Math.round(
                        Math.sin(angle) * radiusSpeed));

                enemyBullets.add(new Projectile(
                        centerX - 4, startY, vx, vy));
            }

            shivaSpiralAngle += enraged ? 37 : 23;

        } else {
            // TARGETED BURST: several shots at the player's position.
            int shots = enraged ? 5 : 3;

            for (int i = 0; i < shots; i++) {
                fireAimedShot(shiva, (i - shots / 2) * 22);
            }
        }

        // Rage mode adds a second aimed attack.
        if (enraged) {
            fireAimedShot(shiva, 0);
        }
    }

    // Fire a projectile from the enemy toward the player's position.
    private void fireAimedShot(Enemy enemy, int horizontalOffset) {
        int startX = enemy.bounds.x + enemy.bounds.width / 2 - 4;
        int startY = enemy.bounds.y + enemy.bounds.height;

        int targetX = shipX + horizontalOffset;
        int targetY = getHeight() - 100;

        double dx = targetX - startX;
        double dy = targetY - startY;
        double distance = Math.max(1, Math.sqrt(dx * dx + dy * dy));

        int speed = enemy.type == EnemyType.SHIVA ? 7 : 5;
        int vx = (int) Math.round(dx / distance * speed);
        int vy = Math.max(2, (int) Math.round(dy / distance * speed));

        enemyBullets.add(new Projectile(startX, startY, vx, vy));
    }

    // Kunal roams horizontally and vertically before descending.
    private void updateKunal(Enemy kunal) {
        kunal.moveCounter++;

        if (kunal.moveCounter >= 14) {
            int turn = random.nextInt(5);

            if (turn == 0) {
                kunal.direction = -1;
            } else if (turn == 1) {
                kunal.direction = 1;
            } else if (turn == 2) {
                kunal.direction = 0;
            }

            kunal.moveCounter = 0;
        }

        kunal.bounds.x += kunal.direction * (kunal.speed + 2);

        if (kunal.bounds.x <= 0) {
            kunal.bounds.x = 0;
            kunal.direction = 1;
        }

        if (kunal.bounds.x + kunal.bounds.width >= getWidth()) {
            kunal.bounds.x = Math.max(0, getWidth() - kunal.bounds.width);
            kunal.direction = -1;
        }

        if (kunal.age <= 180) {
            int roamTop = 35;
            int roamBottom = Math.max(
                    roamTop, getHeight() / 2 - kunal.bounds.height);

            kunal.bounds.y = roamTop + (int) ((roamBottom - roamTop)
                    * (0.5 + 0.5 * Math.sin(kunal.age / 22.0)));
        } else {
            kunal.bounds.y += 1;
        }
    }

    private void defeatShiva(Enemy shiva) {
        SoundEffects.play("shiva");
        enemies.remove(shiva);
        shivaActive = false;

        // Award bonus points.
        int bonusPoints = 100 + shivaEncounters * 50;
        score += bonusPoints;

        // Fully restore spaceship and base health.
        playerHealth = 100;
        baseHealth = maxBaseHealth;

        // Upgrade the weapon up to level 3.
        weaponLevel = Math.min(3, weaponLevel + 1);

        shivaEncounters++;

        System.out.println("SHIVA DEFEATED!");
        System.out.println("Bonus points: " + bonusPoints);
        System.out.println("Spaceship health restored to 100!");
        System.out.println("Base health restored to 100!");
        System.out.println("Weapon level: " + weaponLevel);

        // Spawn the next queued Shiva if a milestone was reached.
        checkShivaMilestones();
    }

    private void endGame() {
        if (gameOver) {
            return;
        }

        gameOver = true;
        SoundEffects.play("player");
        paused = false;
        pressedKeys.clear();
        gameTimer.stop();

        if (!scoreSaved) {
            scoreSaved = true;
            try {
                DatabaseManager.saveScore(playerName, score);
                System.out.println("Saved score for " + playerName + ": " + score);
            } catch (java.sql.SQLException ex) {
                scoreSaved = false;
                System.err.println("Could not save score: " + ex.getMessage());
            }
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(Color.WHITE);
        for (int i = 0; i < starX.length; i++) {
            g.fillOval(starX[i], starY[i], 2, 2);
        }

        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        g.drawString("Score: " + score, 20, 30);

        drawHealthBar(g, 20, 55, 220, 18,
                baseHealth, maxBaseHealth, "BASE HP");

        drawHealthBar(g, 20, 100, 220, 18,
                playerHealth, 100, "PLAYER HP");

        g.setColor(Color.CYAN);
        for (Rectangle bullet : bullets) {
            g.fillRoundRect(
                    bullet.x, bullet.y,
                    bullet.width, bullet.height, 5, 5);
        }

        g.setColor(Color.ORANGE);
        for (Projectile projectile : enemyBullets) {
            Rectangle bullet = projectile.bounds;
            g.fillRoundRect(
                    bullet.x, bullet.y,
                    bullet.width, bullet.height, 5, 5);
        }

        // Player ship.
        int shipY = getHeight() - 100;

        if (playerShipImage != null) {
            g.drawImage(
                    playerShipImage,
                    shipX - 30, shipY - 42,
                    60, 85, this);
        } else {
            g.setColor(Color.CYAN);
            int[] xs = { shipX, shipX - 25, shipX + 25 };
            int[] ys = { shipY - 30, shipY + 20, shipY + 20 };
            g.fillPolygon(xs, ys, 3);
        }

        // Enemies and individual health indicators.
        for (Enemy enemy : enemies) {
            if (enemy.type == EnemyType.SHIVA) {
                drawShiva(g, enemy);
                continue;
            }

            BufferedImage image;
            Color fallbackColor;

            if (enemy.type == EnemyType.PREM) {
                image = premImage;
                fallbackColor = Color.MAGENTA;
            } else if (enemy.type == EnemyType.KUNAL) {
                image = kunalImage;
                fallbackColor = Color.GREEN;
            } else {
                image = scoutImage;
                fallbackColor = Color.RED;
            }

            if (image != null) {
                g.drawImage(
                        image,
                        enemy.bounds.x, enemy.bounds.y,
                        enemy.bounds.width, enemy.bounds.height,
                        this);
            } else {
                g.setColor(fallbackColor);
                g.fillOval(
                        enemy.bounds.x, enemy.bounds.y,
                        enemy.bounds.width, enemy.bounds.height);
            }

            int healthWidth = Math.max(0,
                    enemy.bounds.width * enemy.health / enemy.maxHealth);

            g.setColor(Color.DARK_GRAY);
            g.fillRect(enemy.bounds.x, enemy.bounds.y - 7,
                    enemy.bounds.width, 4);

            g.setColor(Color.GREEN);
            g.fillRect(enemy.bounds.x, enemy.bounds.y - 7,
                    healthWidth, 4);
        }

        if (gameOver) {
            g.setColor(new Color(0, 0, 0, 190));
            g.fillRect(0, 0, getWidth(), getHeight());

            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 42));
            drawCentered(g, "GAME OVER", getHeight() / 2 - 30);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 22));
            drawCentered(g, "Final Score: " + score,
                    getHeight() / 2 + 15);

            g.setFont(new Font("Arial", Font.PLAIN, 18));
            drawCentered(g, "Press R to Restart",
                    getHeight() / 2 + 55);

        } else if (paused) {
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(0, 0, getWidth(), getHeight());

            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 40));
            drawCentered(g, "PAUSED", getHeight() / 2);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            drawCentered(g, "Press P to Resume",
                    getHeight() / 2 + 40);
        }
    }

    private void drawShiva(Graphics g, Enemy shiva) {
        int x = shiva.bounds.x;
        int y = shiva.bounds.y;
        int w = shiva.bounds.width;
        int h = shiva.bounds.height;

        // Draw Shiva's picture, with a fallback if the image cannot load.
        if (shivaImage != null) {
            g.drawImage(shivaImage, x, y, w, h, this);
        } else {
            g.setColor(new Color(70, 0, 120));
            g.fillRoundRect(x, y, w, h, 24, 24);
            g.setColor(new Color(190, 60, 255));
            g.drawRoundRect(x, y, w, h, 24, 24);
            g.setColor(Color.MAGENTA);
            g.fillOval(x + 18, y + 20, 20, 15);
            g.fillOval(x + w - 38, y + 20, 20, 15);
            g.setColor(Color.WHITE);
            g.fillOval(x + 25, y + 24, 7, 7);
            g.fillOval(x + w - 31, y + 24, 7, 7);
        }
        // Boss health bar.
        int barWidth = Math.min(360, getWidth() - 40);
        int barX = (getWidth() - barWidth) / 2;
        int barY = 12;

        g.setFont(new Font("Arial", Font.BOLD, 15));
        g.setColor(Color.WHITE);
        g.drawString("SHIVA — ENCOUNTER " + (shivaEncounters + 1),
                barX, barY + 15);

        g.setColor(Color.DARK_GRAY);
        g.fillRect(barX, barY + 22, barWidth, 12);

        int healthWidth = Math.max(0,
                barWidth * shiva.health / shiva.maxHealth);

        g.setColor(new Color(190, 0, 255));
        g.fillRect(barX, barY + 22, healthWidth, 12);

        g.setColor(Color.WHITE);
        g.drawRect(barX, barY + 22, barWidth, 12);
    }

    private void drawHealthBar(Graphics g, int x, int y,
            int width, int height, int health,
            int maxHealth, String label) {

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString(label + ": " + health + "%", x, y - 5);

        g.setColor(Color.DARK_GRAY);
        g.fillRoundRect(x, y, width, height, 8, 8);

        int fill = width * health / maxHealth;

        if (health > 60) {
            g.setColor(Color.GREEN);
        } else if (health > 30) {
            g.setColor(Color.ORANGE);
        } else {
            g.setColor(Color.RED);
        }

        g.fillRoundRect(x, y, fill, height, 8, 8);

        g.setColor(Color.WHITE);
        g.drawRoundRect(x, y, width, height, 8, 8);
    }

    private void drawCentered(Graphics g, String text, int y) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text,
                (getWidth() - metrics.stringWidth(text)) / 2, y);
    }

    private void restartGame() {
        bullets.clear();
        enemyBullets.clear();
        enemies.clear();

        score = 0;
        scoreSaved = false;
        spawnCounter = 0;
        shipX = getWidth() / 2;

        nextShivaScore = 500;
        pendingShivaEncounters = 0;
        shivaEncounters = 0;
        shivaActive = false;
        shivaAttackPhase = 0;
        shivaSpiralAngle = 0;

        weaponLevel = 1;
        pressedKeys.clear();

        baseHealth = maxBaseHealth;
        playerHealth = 100;

        gameOver = false;
        paused = false;

        gameTimer.start();
        repaint();
    }
}
