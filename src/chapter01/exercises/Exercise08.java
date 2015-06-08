package chapter01.exercises;

import java.util.Scanner;

public class Exercise08 {

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        String input = in.nextLine();
        String[] subStrs = input.split("\\s+");
        System.out.printf("The nonempty substrings of '%s' is %s", input,
                String.join("-", subStrs));

        in.close();
    }

}
