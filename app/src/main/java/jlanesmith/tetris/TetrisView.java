package jlanesmith.tetris;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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

    // The size of the screen in pixels
    public int screenX;
    private int screenY;
    private int gameHeight;
    private int brickSize;
    private int[][] shapeType;
    private int[][] filledSquares;
    private Rect bottomLine, sideLine;
    private Rect[] dividingLines;
    private int shapeChangeX = 0;
    private int shapeChangeY = 0;
    private int score = 0;
    private List<Brick> bricks = new ArrayList<Brick>();

    private int normalGameSpeed = Constants.initialGameSpeed;
    private int currentGameSpeed = normalGameSpeed;

    // When the we initialize (call new()) on gameView
    public TetrisView(Context context, int x, int y) {

        super(context);
        // Make a globally available copy of the context so we can use it in another method
        this.context = context;
        ourHolder = getHolder();
        paint = new Paint();
        screenX = x*3/4;
        screenY = y;
        prepareLevel();
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
        bottomLine = new Rect(0, bottomLineY + padding, screenX + padding,
                bottomLineY + padding * 3);
        sideLine = new Rect(screenX + padding, 0, screenX + padding * 3, bottomLineY + padding * 3);

        dividingLines = new Rect[4];
        dividingLines[0] = new Rect(0, screenY/2 + 1, screenX, screenY/2 - 1);
        dividingLines[1] = new Rect(screenX/2-1, 0, screenX/2+1, screenY/2);
        dividingLines[2] = new Rect(screenX*3/10-1, screenY/2, screenX*3/10+1, screenY);
        dividingLines[3] = new Rect(screenX*7/10-1, screenY/2, screenX*7/10+1, screenY);

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
                draw();
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
            canvas.drawRect(sideLine, paint);

            for (int i = 0; i < bricks.size(); i++) {
                paint.setColor(bricks.get(i).color);
                canvas.drawRect(updateBrick(bricks.get(i)), paint);
            }

            paint.setColor(Color.argb(255, 0, 0, 0));
            paint.setTextSize(200);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("← ↶ ↓ ↷ →" , screenX/2, screenY-100, paint);

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

                    if (motionEvent.getX() > screenX *3 / 10
                            && motionEvent.getX() < screenX * 7 / 10) {

                        currentGameSpeed = Constants.fastGameSpeed;

                    } else {

                        int movementChange = motionEvent.getX() < screenX / 2 ? -brickSize : brickSize;

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

                    boolean clockwise = motionEvent.getX() > screenX / 2;
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
}
