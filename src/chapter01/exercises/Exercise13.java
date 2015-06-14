package chapter01.exercises;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Write a program that prints a lottery combination, picking six distinct
 * numbers between 1 and 49. To pick six distinct numbers, start with an array
 * list filled with 1 . . . 49. Pick a random index and remove the element.
 * Repeat six times. Print the result in sorted order.
 * 
 * @author Lucien
 *
 */
public class Exercise13 {

    // Pick a random index and remove the element
    public static int getARandomVal(List<Integer> intList) {
        Random generator = new Random();
        int index = generator.nextInt(intList.size());
        int retVal = intList.remove(index);
        return retVal;
    }

    public static void main(String[] args) {
        int[] lotteryNums = new int[6];
        // fill an array use Collections.fill(friends, "") for ArrayList<?>
        Arrays.fill(lotteryNums, 0);
        List<Integer> intList = new ArrayList<Integer>();
        for (int i = 1; i < 50; i++) {
            intList.add(i);
        }
        getARandomVal(intList);
    }

}
