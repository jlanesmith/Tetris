package jlanesmith.tetris;

import android.graphics.Color;
import android.graphics.Rect;

/**
 * Created by Jonathan Lane-Smith on 5/8/2017.
 */
public class Brick {

    private int color = Color.argb(255, 255, 255, 255);
    private Rect rect;

    Brick(Rect rect) {
        this.rect = rect;
    }

    public Rect getRect() {
        return rect;
    }

    public int getColor() {
        return color;
    }





}
