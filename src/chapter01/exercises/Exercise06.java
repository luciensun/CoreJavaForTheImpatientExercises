package chapter01.exercises;

import java.math.BigInteger;

public class Exercise06 {

    public static void main(String[] args) {

        BigInteger sum = BigInteger.ONE;
        long n = 1000;
        for (long i = 1; i <= n; i++) {
            sum = sum.multiply(BigInteger.valueOf(i));
        }
        System.out.println("The factorial of 1000 is " + sum);
    }

}
