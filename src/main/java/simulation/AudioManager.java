package simulation;

public interface AudioManager {
    AudioManager NO_OP = new AudioManager() {
        @Override public void play(AudioEvent event) {}
        @Override public void setVolume(float volume) {}
        @Override public float getVolume() { return 1f; }
        @Override public void setMuted(boolean muted) {}
        @Override public boolean isMuted() { return false; }
        @Override public void dispose() {}
    };

    void play(AudioEvent event);
    void setVolume(float volume);
    float getVolume();
    void setMuted(boolean muted);
    boolean isMuted();
    void dispose();
}
