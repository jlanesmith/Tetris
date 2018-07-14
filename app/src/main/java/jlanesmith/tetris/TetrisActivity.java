package jlanesmith.tetris;

import android.content.Intent;
import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.View;

public class TetrisActivity extends AppCompatActivity {

    TetrisView tetrisView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get a Display object to access screen details
        Display display = getWindowManager().getDefaultDisplay();
        // Load the resolution into a Point object
        Point size = new Point();
        display.getSize(size);

        // Initialize gameView and set it as the view
        tetrisView = new TetrisView(this, size.x, size.y);
        setContentView(tetrisView);

        tetrisView.setEventListener(new TetrisView.MyEventListener() {

            @Override
            public void onEventOccurred() {
                // TODO Auto-generated method stub
                startActivity(new Intent(TetrisActivity.this, GameOverActivity.class));
            }
        });

    }

    // This method executes when the player starts the game
    @Override
    protected void onResume() {
        super.onResume();

        // Tell the gameView resume method to execute
        tetrisView.resume();
    }

    // This method executes when the player quits the game
    @Override
    protected void onPause() {
        super.onPause();

        // Tell the gameView pause method to execute
        tetrisView.pause();
    }
}
