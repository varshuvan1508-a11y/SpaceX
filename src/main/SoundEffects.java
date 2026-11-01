
import javax.sound.sampled.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class SoundEffects {

    private static final Map<String, Clip> sounds = new HashMap<>();

    private SoundEffects() {
    }

    public static void playBullet() {
        play("bullet");
    }

    public static void initialize() {
        load("kunal", "assets/kunal_death.wav");
        load("kushal", "assets/kushal_death.wav");
        load("prem", "assets/prem_death.wav");
        load("shiva", "assets/shiva_death.wav");
        load("player", "assets/player_death.wav");
        load("bullet", "assets/bullet_shoot.wav");
    }

    private static void load(String name, String path) {
        try {
            File file = new File(path);

            if (!file.isFile()) {
                System.err.println("Sound file not found: "
                        + file.getAbsolutePath());
                return;
            }

            try (AudioInputStream audio = AudioSystem.getAudioInputStream(file)) {
                Clip clip = AudioSystem.getClip();
                clip.open(audio);
                sounds.put(name, clip);
            }
        } catch (Exception e) {
            System.err.println("Could not load " + name
                    + " sound: " + e.getMessage());
        }
    }

    public static void play(String enemyName) {
        Clip clip = sounds.get(enemyName.toLowerCase());

        if (clip == null) {
            return;
        }

        if (clip.isRunning()) {
            clip.stop();
        }

        clip.setFramePosition(0);
        clip.start();
    }
}
