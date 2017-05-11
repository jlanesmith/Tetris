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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static android.media.CamcorderProfile.get;
import static jlanesmith.tetris.Constants.colors;
import static jlanesmith.tetris.Constants.gameLength;
import static jlanesmith.tetris.Constants.padding;
import static jlanesmith.tetris.Shapes.shapeTypes;

/**
 * Created by Jonathan Lane-Smith on 5/8/2017.
 */
public class TetrisView extends SurfaceView implements Runnable {

    Context context;
    private Thread gameThread = null;
    private SurfaceHolder ourHolder;
    private volatile boolean playing;
    private boolean paused = false;
    private Canvas canvas;
    private Paint paint;
    private long fps;
    private long timeThisFrame;

    int score = 0;
    private int lives = 3;

    // The size of the screen in pixels
    public int screenX;
    private int screenY;
    private int gameHeight;
    private int brickSize;
    private int[][] shapeType;
    private int[][] filledSquares;
    List<Brick> bricks = new ArrayList<Brick>();

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
        prepareLevel();
    }

    private void printInfo() {
        for (int i = 0; i < gameHeight; i++) {
            for (int j = 0; j < gameLength; j++) {
                System.out.print(filledSquares[i][j]);
            }
            System.out.println();
        }
    }

    private Rect updateBrick(Brick brick) {

        int left = brickSize * (brick.xCoord) + Constants.padding;
        int top = brickSize * (brick.yCoord) + Constants.padding;
        int right = brickSize * (brick.xCoord + 1) - Constants.padding;
        int bottom = brickSize * (brick.yCoord + 1) - Constants.padding;
        return new Rect(left, top, right, bottom);
    }

    private Brick makeBrick(int i, int j, int color) {

        int left = brickSize * (3 + j + shapeChangeX) + Constants.padding;
        int top = brickSize * (-4 + i + shapeChangeY) + Constants.padding;
        int right = brickSize * (4 + j + shapeChangeX) - Constants.padding;
        int bottom = brickSize * (-3 + i + shapeChangeY) - Constants.padding;
        int xCoord = 3 + j + shapeChangeX;
        int yCoord = -4 + i + shapeChangeY;
        return new Brick(new Rect(left, top, right, bottom), color, xCoord, yCoord);
    }

    private List<Brick> createBricks() {

        Random generator = new Random();
        int color = generator.nextInt(colors.length);
        shapeType = shapeTypes[generator.nextInt(shapeTypes.length)];
        int numBricksSoFar = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (shapeType[i][j] == 1) {
                    bricks.add(numBricksSoFar, makeBrick(i, j, colors[color]));
                    numBricksSoFar++;
                }
            }
        }
        return bricks;
    }

    private void prepareLevel() {

        while (screenX == 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        brickSize = screenX / Constants.gameLength;
        gameHeight = (screenY - Constants.gameBottom -
                (screenY - Constants.gameBottom) % brickSize) / brickSize;
        filledSquares = new int[gameHeight][gameLength];

        int bottomLineY = gameHeight * brickSize;
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

                if (!paused) {
                    update();
                }
                draw();
            }
            timeThisFrame = System.currentTimeMillis() - startFrameTime;
            if (timeThisFrame >= 1) {
                fps = 1000 / timeThisFrame;
            }
        }
    }

    private void update() {

        boolean isStopped = false;

        checkStopped:
        for (int i = 0; i < 4; i++) {
            try {
                if ((bricks.get(i).yCoord == gameHeight - 1) ||
                        (filledSquares[bricks.get(i).yCoord + 1][bricks.get(i).xCoord] == 1)) {
                    isStopped = true;
                    break checkStopped;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
            }
        }

        if (!isStopped) {
            for (int i = 0; i < 4; i++) {
                Brick newBrick = bricks.get(i);
                newBrick.yCoord++;
                bricks.set(i, newBrick);
            }
            shapeChangeY++;
        } else {
            for (int i = 0; i < 4; i++) {
                filledSquares[bricks.get(i).yCoord][bricks.get(i).xCoord] = 1;
            }
            for (int i = 0; i < gameHeight; i++) {
                boolean isOne = true;
                for (int j = 0; j < gameLength; j++) {
                    isOne &= filledSquares[i][j] == 1;
                }
                if (isOne) {
                    int totalBricks = bricks.size();
                    for (int j = 0; j < totalBricks; j++) {
                        if (bricks.get(j).yCoord == i) {
                            bricks.remove(j);
                            totalBricks--;
                            j--;
                        } else if (bricks.get(j).yCoord < i) {
                            Brick newBrick = bricks.get(j);
                            newBrick.yCoord++;
                            bricks.set(j, newBrick);
                        }
                    }
                    for (int j = i; j > 0; j--) {
                        for (int k = 0; k < gameLength; k++) {
                            filledSquares[j][k] = filledSquares[j - 1][k];
                        }
                    }
                }
            }
            shapeChangeX = 0;
            shapeChangeY = 0;
            bricks = createBricks();
        }
    }

    private void draw() {
        // Make sure our drawing surface is valid or we crash
        if (ourHolder.getSurface().isValid()) {
            // Lock the canvas ready to draw
            canvas = ourHolder.lockCanvas();

            // Draw the background color
            canvas.drawColor(Color.argb(255, 26, 128, 182));

            paint.setColor(Color.argb(255, 255, 255, 255));
            canvas.drawRect(bottomLine, paint);

            for (int i = 0; i < bricks.size(); i++) {
                paint.setColor(bricks.get(i).color);
                canvas.drawRect(updateBrick(bricks.get(i)), paint);
            }

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
                        Brick newBrick = bricks.get(i);
                        newBrick.xCoord += movementChange / brickSize;
                        bricks.set(i, newBrick);
                    }
                    shapeChangeX += movementChange / brickSize;

                } else {
                    boolean clockwise = motionEvent.getX() > screenX / 2;
                    shapeType = Shapes.rotateShape(clockwise, shapeType);
                    int numShapesSoFar = 0;
                    Brick oldBrick = bricks.get(0);
                    for (int i = 0; i < 4; i++) {
                        for (int j = 0; j < 4; j++) {
                            if (shapeType[i][j] == 1) {
                                bricks.set(numShapesSoFar, makeBrick(i, j, oldBrick.color));
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

