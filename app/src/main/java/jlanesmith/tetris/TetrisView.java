package jlanesmith.tetris;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static jlanesmith.tetris.Constants.*;
import static jlanesmith.tetris.Shapes.*;

/**
 * Created by Jonathan Lane-Smith on 5/8/2017.
 */
public class TetrisView extends SurfaceView implements Runnable {

    Context context;
    private Thread gameThread = null;
    private SurfaceHolder ourHolder;
    private volatile boolean playing = true;
    private boolean paused = false;
    private Canvas canvas;
    private Paint paint;

    // The size of the screen in pixels
    private int screenX, screenY, totalX, totalY, shapeChangeX, shapeChangeY;
    private int sideDistance, gameHeight, brickSize;
    private int[][] shapeType, filledSquares;
    private Rect bottomLine, leftSideLine, rightSideLine;
    private RectF playAgain, menu;
    private int score = 0;
    private List<Brick> bricks = new ArrayList<Brick>();

    private int normalGameSpeed = initialGameSpeed;
    private int currentGameSpeed = normalGameSpeed;

    // When the we initialize (call new()) on gameView
    public TetrisView(Context context, int x, int y) {

        super(context);
        // Make a globally available copy of the context so we can use it in another method
        this.context = context;
        ourHolder = getHolder();
        paint = new Paint();
        sideDistance = x/gameSide;
        totalX = x;
        totalY = y;
        screenX = x-2*sideDistance;
        screenY = y*(gameBottom-1)/gameBottom;
        prepareLevel();
    }

    private Rect updateBrick(Brick brick) {

        int left = brickSize * (brick.xCoord) + padding + sideDistance;
        int top = brickSize * (brick.yCoord) + padding;
        int right = brickSize * (brick.xCoord + 1) - padding + sideDistance;
        int bottom = brickSize * (brick.yCoord + 1) - padding;
        return new Rect(left, top, right, bottom);
    }

    private Brick makeBrick(int i, int j, int color) {

        int left = sideDistance + brickSize * (gameLength/2-2 + j + shapeChangeX) 
                + padding;
        int top = brickSize * (-4 + i + shapeChangeY) + padding;
        int right = sideDistance + brickSize * (gameLength/2-1 + j + shapeChangeX) - padding;
        int bottom = brickSize * (-3 + i + shapeChangeY) - padding;
        int xCoord = gameLength/2-2 + j + shapeChangeX;
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
        brickSize = screenX / gameLength;
        int gameBottomPixels = (int) getY()/gameBottom;
        gameHeight = (screenY - gameBottomPixels -
                (screenY - gameBottomPixels) % brickSize) / brickSize;
        filledSquares = new int[gameHeight][gameLength];

        int bottomLineY = gameHeight * brickSize;
        bottomLine = new Rect(sideDistance - padding, bottomLineY + padding,
                screenX + sideDistance + padding, bottomLineY + padding * 3);
        leftSideLine = new Rect(sideDistance - padding * 3, 0, sideDistance - padding,
                bottomLineY + padding * 3);
        rightSideLine = new Rect(screenX + sideDistance - padding, 0,
                screenX + sideDistance + padding, bottomLineY + padding * 3);

        bricks = createBricks();
        draw();
    }

    @Override
    public void run() {

        long timeSinceMove = System.currentTimeMillis();

        while (playing) {

            // Capture the current time in milliseconds in startFrameTime
            long startFrameTime = System.currentTimeMillis();

            if (System.currentTimeMillis() - timeSinceMove >= currentGameSpeed) {

                timeSinceMove = System.currentTimeMillis();

                if (!paused) {
                    update();
                }
                if (playing) {
                    draw();
                }
            }
        }
    }

    private void update() {

        boolean isStopped = false;

        for (int i = 0; i < 4; i++) {
            try {
                if ((bricks.get(i).yCoord == gameHeight - 1) ||
                        (filledSquares[bricks.get(i).yCoord + 1][bricks.get(i).xCoord] == 1)) {
                    isStopped = true;
                    break;
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
                try {
                    filledSquares[bricks.get(i).yCoord][bricks.get(i).xCoord] = 1;
                } catch (ArrayIndexOutOfBoundsException e) {
                    playing = false;
                    Button button = new Button(context);
                    button.setHeight(500);
                    button.setWidth(500);
                    button.bringToFront();
                    drawGameOver();
                }
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

    private void drawGameOver() {

        if (ourHolder.getSurface().isValid()) {
            // Lock the canvas ready to draw
            canvas = ourHolder.lockCanvas();

            paint.setColor(Color.WHITE);
            canvas.drawRoundRect(totalX/8, totalY/2-180, totalX*7/8, totalY/2+80, 20, 20, paint);
            paint.setColor(Color.BLACK);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(150);
            canvas.drawText("Game Over", totalX/2, totalY/2, paint);

            playAgain = new RectF(totalX/4, totalY*15/24, totalX*3/4, totalY*9/12);
            menu = new RectF(totalX/4, totalY*19/24, totalX*3/4, totalY*11/12);
            canvas.drawRoundRect(playAgain, 20, 20, paint);
            canvas.drawRoundRect(menu, 20, 20, paint);
            paint.setTextSize(100);
            paint.setColor(Color.WHITE);
            canvas.drawText("Play Again", totalX/2, totalY*17/24, paint);
            canvas.drawText("Menu", totalX/2, totalY*21/24, paint);


            ourHolder.unlockCanvasAndPost(canvas);
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
            canvas.drawRect(leftSideLine, paint);
            canvas.drawRect(rightSideLine, paint);

            for (int i = 0; i < bricks.size(); i++) {
                paint.setColor(bricks.get(i).color);
                canvas.drawRect(updateBrick(bricks.get(i)), paint);
            }

            paint.setColor(Color.argb(255, 0, 0, 0));
            paint.setTextSize(200);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("← ↶ ↓ ↷ →" , screenX/2, getY()-100, paint);

            // Draw everything to the screen
            ourHolder.unlockCanvasAndPost(canvas);
        }
    }

    public void pause() {
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            Log.e("Error:", "joining thread");
        }
    }

    public void resume() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    private boolean playGameTouchEvent(MotionEvent motionEvent) {

        int screenWidth = screenX + 2*sideDistance;
        int screenHeight = screenY + gameBottom;

        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {

            // Player has touched the screen
            case MotionEvent.ACTION_DOWN:

                if (motionEvent.getY() > screenHeight / 2) {

                    if (motionEvent.getX() > screenWidth *3 / 10
                            && motionEvent.getX() < screenWidth * 7 / 10) {

                        currentGameSpeed = fastGameSpeed;

                    } else {

                        int movementChange = motionEvent.getX() < screenWidth / 2 ? -brickSize : brickSize;

                        boolean isOkayToMove = true;
                        for (int i = 0; i < 4; i++) {
                            int testXCoord = bricks.get(i).xCoord + movementChange / brickSize;
                            isOkayToMove &= testXCoord >= 0 && testXCoord < gameLength;
                            try {
                                isOkayToMove &= filledSquares[bricks.get(i).yCoord][testXCoord] == 0;
                            } catch (ArrayIndexOutOfBoundsException e) {
                            }
                        }
                        if (isOkayToMove) {
                            for (int i = 0; i < 4; i++) {
                                Brick newBrick = bricks.get(i);
                                newBrick.xCoord += movementChange / brickSize;
                                bricks.set(i, newBrick);
                            }
                            shapeChangeX += movementChange / brickSize;
                        }
                    }

                } else {

                    boolean clockwise = motionEvent.getX() > screenWidth / 2;
                    shapeType = Shapes.rotateShape(clockwise, shapeType);
                    int numShapesSoFar = 0;
                    Brick oldBrick = bricks.get(0);
                    Brick[] testBricks = new Brick[4];
                    for (int i = 0; i < 4; i++) {
                        for (int j = 0; j < 4; j++) {
                            if (shapeType[i][j] == 1) {
                                testBricks[numShapesSoFar] = makeBrick(i, j, oldBrick.color);
                                numShapesSoFar++;
                            }
                        }
                    }
                    boolean isOkayToRotate = true;
                    for (int i = 0; i < 4; i++) {
                        try {
                            isOkayToRotate &= filledSquares[testBricks[i].yCoord]
                                    [testBricks[i].xCoord] == 0;
                        } catch (ArrayIndexOutOfBoundsException e) {
                            isOkayToRotate = false;
                        }
                    }
                    if (isOkayToRotate) {
                        for (int i = 0; i < 4; i++) {
                            bricks.set(i, testBricks[i]);
                        }
                    } else {
                        shapeType = Shapes.rotateShape(!clockwise, shapeType);
                    }
                }
                draw();

                break;

            // Player has removed finger from screen
            case MotionEvent.ACTION_UP:

                currentGameSpeed = normalGameSpeed;

                break;
        }
        return true;
    }

    private boolean gameOverTouchEvent(MotionEvent motionEvent) {

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


    // The SurfaceView class implements onTouchListener
    // So we can override this method and detect screen touches.
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {

        if (playing) {
            return playGameTouchEvent(motionEvent);
        } else {
            return gameOverTouchEvent(motionEvent);
        }
    }

}
