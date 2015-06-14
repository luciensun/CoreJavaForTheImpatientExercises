package chapter01.exercises;

import java.util.Random;

/**
 * Write a program that produces a random string of letters and digits by
 * generating a random long value and printing it in base 36.
 * 
 * @author Lucien
 *
 */
public class Exercise10 {

    public static void main(String[] args) {
        Random generator = new Random();
        // generate a random long value
        long randomVal = generator.nextLong();

        // base 36 becaue 0-9 + A-Z
        String str = Long.toString(randomVal, 36);
        System.out.printf("a random string of letters and digits is '%s'%n",
                str);
    }

}
