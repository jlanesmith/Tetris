package jlanesmith.tetris;

/**
 * Created by Jonathan Lane-Smith on 5/9/2017.
 */
public class Shapes {

    private final static int[][] straight = { {0,0,0,0},{0,0,0,0},{1,1,1,1},{0,0,0,0}};
    private final static int[][] square = { {0,0,0,0},{0,1,1,0},{0,1,1,0},{0,0,0,0}};
    private final static int[][] threeProng = { {0,0,0,0},{0,0,1,0},{0,1,1,1},{0,0,0,0}};
    private final static int[][] leftThreeOne = { {0,0,0,0},{0,0,1,0},{1,1,1,0},{0,0,0,0}};
    private final static int[][] rightThreeOne = { {0,0,0,0},{0,1,0,0},{0,1,1,1},{0,0,0,0}};
    private final static int[][] leftTwoTwo = { {0,0,0,0},{0,1,1,0},{1,1,0,0},{0,0,0,0}};
    private final static int[][] rightTwoTwo = { {0,0,0,0},{0,1,1,0},{0,0,1,1},{0,0,0,0}};

    public final static int[][][] shapeTypes =
            {straight, square, leftThreeOne, rightThreeOne, leftTwoTwo, rightTwoTwo};

    public static int[][] rotateShape(boolean clockwise, int[][] origArray) {

        int[][] newArray = new int[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (clockwise) {
                    newArray[i][j] = origArray[3 - j][i];
                } else {
                    newArray[i][j] = origArray[j][3 - i];
                }
            }
        }
        return newArray;
    }

}
