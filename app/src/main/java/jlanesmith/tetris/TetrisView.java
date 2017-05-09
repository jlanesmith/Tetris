package jlanesmith.tetris;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import static jlanesmith.tetris.Constants.gameBottom;
import static jlanesmith.tetris.Constants.padding;

/**
 * Created by Jonathan Lane-Smith on 5/8/2017.
 */
public class TetrisView extends SurfaceView implements Runnable {

    private static int gameHeight = 20;
    private static int gameLength = 10;

    Context context;

    // This is our thread
    private Thread gameThread = null;

    // Our SurfaceHolder to lock the surface before we draw our graphics
    private SurfaceHolder ourHolder;

    // A boolean which we will set and unset
    // when the game is running- or not.
    private volatile boolean playing;

    // Game is paused at the start
    private boolean paused = false;

    // A Canvas and a Paint object
    private Canvas canvas;
    private Paint paint;

    // This variable tracks the game frame rate
    private long fps;

    // This is used to help calculate the fps
    private long timeThisFrame;

    // The size of the screen in pixels
    public int screenX;
    private int screenY;
    private int brickSize;

    // The score
    int score = 0;

    // Lives
    private int lives = 3;

    private Brick[] bricks;

    private Rect bottomLine;

    // When the we initialize (call new()) on gameView
    public TetrisView(Context context, int x, int y) {

        super(context);

        // Make a globally available copy of the context so we can use it in another method
        this.context = context;

        // Initialize ourHolder and paint objects
        ourHolder = getHolder();
        paint = new Paint();

        screenX = x;
        screenY = y;
        bricks = new Brick[4];

        prepareLevel();
    }

    private void prepareLevel() {

        while (screenX == 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
        }
        brickSize = screenX/gameLength;

        int bottomLineY = screenY - Constants.gameBottom -
                (screenY - Constants.gameBottom)%brickSize;
        bottomLine = new Rect(0, bottomLineY + padding, screenX, bottomLineY + padding*3);

        bricks[0] = new Brick( new Rect(brickSize*4+Constants.padding, brickSize*-2+
                Constants.padding, brickSize*5-Constants.padding, brickSize*-1-Constants.padding));
        bricks[1] = new Brick( new Rect(brickSize*5+Constants.padding, brickSize*-2+
                Constants.padding, brickSize*6-Constants.padding, brickSize*-1-Constants.padding));
        bricks[2] = new Brick( new Rect(brickSize*6+Constants.padding, brickSize*-2+
                Constants.padding, brickSize*7-Constants.padding, brickSize*-1-Constants.padding));
        bricks[3] = new Brick( new Rect(brickSize*7+Constants.padding, brickSize*-2+
                Constants.padding, brickSize*8-Constants.padding, brickSize*-1-Constants.padding));
    }

    @Override
    public void run() {

        long timeSinceMove = System.currentTimeMillis();

        while (playing) {

            // Capture the current time in milliseconds in startFrameTime
            long startFrameTime = System.currentTimeMillis();

            if (System.currentTimeMillis() - timeSinceMove >= 500) {
                timeSinceMove = System.currentTimeMillis();
                // Update the frame
                if (!paused) {
                    update();
                }

                // Draw the frame
                draw();
            }

            // Calculate the fps this frame
            // We can then use the result to
            // time animations and more.
            timeThisFrame = System.currentTimeMillis() - startFrameTime;
            if (timeThisFrame >= 1) {
                fps = 1000 / timeThisFrame;
            }
        }
    }

    private void update() {

        // Has the player lost
        boolean lost = false;

        if (lost) {
            prepareLevel();
        }

        boolean isStopped = false;

        checkStopped: for (int i = 0; i < bricks.length; i++) {
            if (bricks[1].rect.bottom >= (bottomLine.top - Constants.padding*2)) {
                isStopped = true;
                break checkStopped;
            }
        }

        if (!isStopped) {
            for (int i = 0; i < bricks.length; i++) {
                bricks[i].rect.top += brickSize;
                bricks[i].rect.bottom += brickSize;
            }
        }
    }

    private void draw() {
        // Make sure our drawing surface is valid or we crash
        if (ourHolder.getSurface().isValid()) {
            // Lock the canvas ready to draw
            canvas = ourHolder.lockCanvas();

            // Draw the background color
            canvas.drawColor(Color.argb(255, 26, 128, 182));

            // Choose the brush color for drawing
            paint.setColor(bricks[0].color);

            for (int i = 0; i < bricks.length; i++) {
                canvas.drawRect(bricks[i].rect, paint);
            }

            canvas.drawRect(bottomLine , paint);

            // Draw the score and remaining lives
            // Change the brush color
            paint.setColor(Color.argb(255, 249, 129, 0));
            paint.setTextSize(40);
            canvas.drawText("Score: " + score + "   Lives: " + lives, 10, 50, paint);

            // Draw everything to the screen
            ourHolder.unlockCanvasAndPost(canvas);
        }
    }

    public void pause() {
        playing = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            Log.e("Error:", "joining thread");
        }
    }

    public void resume() {
        playing = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    // The SurfaceView class implements onTouchListener
    // So we can override this method and detect screen touches.
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {

        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {

            // Player has touched the screen
            case MotionEvent.ACTION_DOWN:

                break;

            // Player has removed finger from screen
            case MotionEvent.ACTION_UP:

                break;

        }

        return true;
    }
}

