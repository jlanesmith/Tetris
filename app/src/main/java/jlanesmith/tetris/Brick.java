package jlanesmith.tetris;

import android.graphics.Color;
import android.graphics.Rect;

/**
 * Created by Jonathan Lane-Smith on 5/8/2017.
 */
public class Brick {

    public int color;
    public Rect rect;

    Brick() {

    }

    Brick(Rect rect, int color) {
        this.rect = rect;
        this.color = color;
    }





}
