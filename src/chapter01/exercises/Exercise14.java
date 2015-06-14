package chapter01.exercises;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Write a program that reads a two-dimensional array of integers and determines
 * whether it is a magic square (that is, whether the sum of all rows, all
 * columns, and the diagonals is the same). Accept lines of input that you break
 * up into individual integers, and stop when the user enters a blank line. 
 * For example, 
 * 
16 3 2 13
3 10 11 8
9 6 7 12
4 15 14 3
(Blank line)

2 7 6
9 5 1
4 3 8

 * with the input your program should respond affirmatively.
 * 
 * @author Lucien
 *
 */

public class Exercise14 {

    public static void main(String[] args) {
        List<String> str2DimensionalArray = new ArrayList<String>();
        Scanner in = new Scanner(System.in);
        boolean isMagicSquare = true;

        String strLine = null;
        while (!"".equals(strLine = in.nextLine().trim())) {
            str2DimensionalArray.add(strLine);
        }

        int[][] int2DimensionalArray = new int[str2DimensionalArray.size()][];
        for (int row = 0; row < int2DimensionalArray.length; row++) {
            strLine = str2DimensionalArray.get(row);
            // split by blank words
            String[] strElement = strLine.split("\\s");
            // allocate memory for int[] variable
            int2DimensionalArray[row] = new int[strElement.length];
            for (int col = 0; col < strElement.length; col++) {
                int2DimensionalArray[row][col] = Integer
                        .parseInt(strElement[col]);
            }
        }

        // System.out.println(Arrays.deepToString(int2DimensionalArray));

        int sum = 0;
        int currentSum = 0;
        for (int col = 0; col < int2DimensionalArray[0].length; col++) {
            sum = sum + int2DimensionalArray[0][col];
        }

        for (int row = 0; row < int2DimensionalArray.length && isMagicSquare; row++) {
            currentSum = 0;
            for (int col = 0; col < int2DimensionalArray[row].length; col++) {
                currentSum = currentSum + int2DimensionalArray[row][col];
            }
            System.out.println("The currentSum is " + currentSum);
            if (currentSum != sum) {
                isMagicSquare = false;
            }
        }

        for (int col = 0; col < int2DimensionalArray[0].length && isMagicSquare; col++) {
            currentSum = 0;
            for (int row = 0; row < int2DimensionalArray.length; row++) {
                currentSum = currentSum + int2DimensionalArray[row][col];
            }
            System.out.println("The currentSum is " + currentSum);
            if (currentSum != sum) {
                isMagicSquare = false;
            }
        }

        currentSum = 0;
        for (int index = 0; index < int2DimensionalArray.length; index++) {
            currentSum = currentSum + int2DimensionalArray[index][index];
        }
        System.out.println("The currentSum is " + currentSum);
        if (currentSum != sum) {
            isMagicSquare = false;
        }

        currentSum = 0;
        for (int index = 0; index < int2DimensionalArray.length; index++) {
            currentSum = currentSum
                    + int2DimensionalArray[index][int2DimensionalArray.length
                            - 1 - index];
        }
        System.out.println("The currentSum is " + currentSum);
        if (currentSum != sum) {
            isMagicSquare = false;
        }

        in.close();
    }

}
