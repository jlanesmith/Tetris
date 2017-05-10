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

import java.util.Random;

import static jlanesmith.tetris.Constants.gameBottom;
import static jlanesmith.tetris.Constants.gameSpeed;
import static jlanesmith.tetris.Constants.padding;
import static jlanesmith.tetris.Shapes.shapeTypes;

/**
 * Created by Jonathan Lane-Smith on 5/8/2017.
 */
public class TetrisView extends SurfaceView implements Runnable {

    Context context;
    // This is our thread
    private Thread gameThread = null;
    // Our SurfaceHolder to lock the surface before we draw our graphics
    private SurfaceHolder ourHolder;
    // A boolean which we will set and unset when the game is running- or not.
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

    int score = 0;
    private int lives = 3;

    // The size of the screen in pixels
    public int screenX;
    private int screenY;
    private int brickSize;
    private int[][] shapeType;
    private Brick[] bricks;
    private Rect bottomLine;
    private int shapeChangeX = 0;
    private int shapeChangeY = 0;

    // When the we initialize (call new()) on gameView
    public TetrisView(Context context, int x, int y) {

        super(context);
        // Make a globally available copy of the context so we can use it in another method
        this.context = context;
        ourHolder = getHolder();
        paint = new Paint();
        screenX = x;
        screenY = y;
        bricks = new Brick[4];

        prepareLevel();
    }

    private Brick[] createBricks() {

        int numShapesSoFar = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (shapeType[i][j] == 1) {
                    int left = brickSize * (3 + j + shapeChangeX) + Constants.padding;
                    int top = brickSize * (-4 + i + shapeChangeY) + Constants.padding;
                    int right = brickSize * (4 + j + shapeChangeX) - Constants.padding;
                    int bottom = brickSize * (-3 + i + shapeChangeY) - Constants.padding;
                    bricks[numShapesSoFar] = new Brick(new Rect(left, top, right,
                            bottom));
                    numShapesSoFar++;
                }
            }
        }
      return bricks;
    }

    private void prepareLevel() {

        Random generator = new Random();
        shapeType = shapeTypes[generator.nextInt(shapeTypes.length)];

        while (screenX == 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        brickSize = screenX / Constants.gameLength;

        int bottomLineY = screenY - Constants.gameBottom -
                (screenY - Constants.gameBottom) % brickSize;
        bottomLine = new Rect(0, bottomLineY + padding, screenX, bottomLineY + padding * 3);
        bricks = createBricks();
        draw();
    }

    @Override
    public void run() {

        long timeSinceMove = System.currentTimeMillis();

        while (playing) {

            // Capture the current time in milliseconds in startFrameTime
            long startFrameTime = System.currentTimeMillis();

            if (System.currentTimeMillis() - timeSinceMove >= Constants.gameSpeed) {
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

        boolean isStopped = false;

        checkStopped:
        for (int i = 0; i < bricks.length; i++) {
            if (bricks[i].rect.bottom >= (bottomLine.top - Constants.padding * 2)) {
                isStopped = true;
                break checkStopped;
            }
        }

        if (!isStopped) {
            for (int i = 0; i < bricks.length; i++) {
                bricks[i].rect.top += brickSize;
                bricks[i].rect.bottom += brickSize;
            }
            shapeChangeY++;
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

            canvas.drawRect(bottomLine, paint);

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

                if (motionEvent.getY() > screenY / 2) {

                    int movementChange = motionEvent.getX() < screenX / 2 ? -brickSize : brickSize;

                    for (int i = 0; i < 4; i++) {
                        bricks[i].rect.left += movementChange;
                        bricks[i].rect.right += movementChange;
                    }
                    shapeChangeX += movementChange / brickSize;

                } else {
                    boolean clockwise = motionEvent.getX() > screenX / 2;
                    shapeType = Shapes.rotateShape(clockwise, shapeType);
                    int numShapesSoFar = 0;
                    for (int i = 0; i < 4; i++) {
                        for (int j = 0; j < 4; j++) {
                            if (shapeType[i][j] == 1) {
                                int left = brickSize * (3 + j + shapeChangeX) + Constants.padding;
                                int top = brickSize * (-4 + i + shapeChangeY) + Constants.padding;
                                int right = brickSize * (4 + j + shapeChangeX) - Constants.padding;
                                int bottom = brickSize * (-3 + i + shapeChangeY) - Constants.padding;
                                bricks[numShapesSoFar] = new Brick(new Rect(left, top, right,
                                        bottom));
                                numShapesSoFar++;
                            }
                        }
                    }
                }
                draw();

                break;

            // Player has removed finger from screen
            case MotionEvent.ACTION_UP:
                break;
        }

        return true;
    }
}

