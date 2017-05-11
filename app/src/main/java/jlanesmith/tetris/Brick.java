package jlanesmith.tetris;

import android.graphics.Color;
import android.graphics.Rect;

/**
 * Created by Jonathan Lane-Smith on 5/8/2017.
 */
public class Brick {

    public int color;
    public Rect rect;
    public int xCoord;
    public int yCoord;

    Brick(Rect rect, int color, int xCoord, int yCoord) {
        this.rect = rect;
        this.color = color;
        this.xCoord = xCoord;
        this.yCoord = yCoord;
    }







}
