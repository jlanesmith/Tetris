package jlanesmith.tetris;

import android.graphics.Color;

/**
 * Created by Jonathan Lane-Smith on 5/9/2017.
 */
public class Constants {

    /** Distance from bottom of screen to bottom of game */
    public final static int gameBottom = 400;

    /** Padding between each brick (Doubled because both bricks have padding). */
    public final static int padding = 4;

    /** Speed of the game, in milliseconds. */
    public final static int initialGameSpeed = 500;

    /** Speed of the game, in milliseconds. */
    public final static int fastGameSpeed = 50;

    /** Number of bricks across */
    public final static int gameLength = 10;

    public final static int[] colors = {Color.RED, Color.BLUE, Color.CYAN, Color.MAGENTA,
            Color.YELLOW, Color.GREEN, Color.WHITE};

}