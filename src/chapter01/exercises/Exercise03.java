package chapter01.exercises;

import java.util.Scanner;

public class Exercise03 {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int maxVal = Integer.MIN_VALUE;
        for (int i = 0; i < 3; i++) {
            int inputVal = in.nextInt();
            maxVal = Math.max(maxVal, inputVal);
        }
        System.out.printf("The largest integer is %d%n", maxVal);
    }
}
