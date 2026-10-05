package antdroid.cfbcoach;

import positions.Player;

/**
 * Maps a position to one of the bundled pixel-art avatars
 * ({@code res/drawable-nodpi/sprite_avatar_*}). Quarterbacks and linebackers
 * have dedicated sprites; the rest of the offense/defense fall back to the
 * showcase variant for their side of the ball so a roster never shows the
 * same face for every player.
 */
public final class PlayerSprites {
    private PlayerSprites() {
    }

    public static int avatarFor(String position) {
        String pos = position == null ? "" : position.trim().toUpperCase();
        if (pos.startsWith("QB")) return R.drawable.sprite_avatar_qb_green;
        if (pos.startsWith("LB") || pos.endsWith("LB")) return R.drawable.sprite_avatar_lb_green;
        if (Player.defensePos.contains(pos)) return R.drawable.sprite_avatar_showcase_lb;
        if (Player.offensePos.contains(pos)) return R.drawable.sprite_avatar_showcase_qb;
        return R.drawable.sprite_avatar_qb_green;
    }
}
