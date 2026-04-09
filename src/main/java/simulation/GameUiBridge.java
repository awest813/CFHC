package simulation;

import java.io.File;
import java.io.IOException;

import positions.Player;

/**
 * Thin platform bridge used by the portable simulation layer to request UI work.
 * Android can implement this directly today, while future iPhone/desktop shells
 * can provide their own adapters without the core sim depending on activities.
 */
public interface GameUiBridge {
    GameUiBridge NO_OP = new GameUiBridge() {
        @Override
        public void crash() {
        }

        @Override
        public void startRecruiting(File saveFile, Team userTeam) {
        }

        @Override
        public void transferPlayer(Player player) {
        }

        @Override
        public void updateSpinners() {
        }

        @Override
        public void disciplineAction(Player player, String issue, int gamesA, int gamesB) {
        }
    };

    void crash();

    void startRecruiting(File saveFile, Team userTeam) throws InterruptedException, IOException;

    void transferPlayer(Player player);

    void updateSpinners();

    void disciplineAction(Player player, String issue, int gamesA, int gamesB);
}
