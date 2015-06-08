package chapter01.exercises;

import java.util.Scanner;

public class Exercise01 {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int value = in.nextInt();
        System.out.printf("The binary value of %d is %s%n",value, Integer.toBinaryString(value));
        System.out.printf("The octal value of %d is %o%n",value, value);
        System.out.printf("The hexadecimal value of %d is %x%n",value, value);
        
        // The reciprocal value of 16 in hexadecimal floating-point format is 0x1.0p-4
        // In hexadecimal notation, we use a p, not an e, to denote the exponent.
        // (An e is a hexadecimal digit.) 
        // Note that, even though the digits are written in hexadecimal, 
        // the exponent(that is, the power of 2)is written in decimal.
        System.out.printf("The reciprocal value of %d in "
                + "hexadecimal floating-point format is %a%n",value, 1.0/value);
    }
    
    public static void formatDemo() {
        // d decimal integer
        System.out.printf("%4d\n", 16);
        // x or X hexadecimal integer
        System.out.printf("%4x\n", 16);
        // o octal integer
        System.out.printf("%4o\n", 16);
        
        // f fixed floating-point
        System.out.printf("%8.3f\n", 16.212);
        // e or E exponential floating-point
        System.out.printf("%e\n", 16.212);
        // g or G general floating-point: the shorter of e/E and f/F
        System.out.printf("%g\n", 16.212);
        
        // a or A hexadecimal floating-point
        System.out.printf("%a\n", 0.4);
        
        // h or H Hash code
        System.out.printf("%h\n", "ab");
    }
}
