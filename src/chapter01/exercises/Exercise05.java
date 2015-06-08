package chapter01.exercises;

public class Exercise05 {

    public static void main(String[] args) {
        
        /**
         * If we cast a double to an int that is larger than the largest possible int value, 
         * the int value we get will be the largest possible int value.
         */
        System.out.printf("The double value %f that is larger than %n"
                + "the largest possible int value %d %n"
                + "is casted to an int value %d%n",
                Math.nextUp(Integer.MAX_VALUE), Integer.MAX_VALUE,
                (int)Math.nextUp(Integer.MAX_VALUE));
        
        System.out.printf("The double value %f that is larger than %n"
                + "the largest possible int value %d %n"
                + "is casted to an int value %d%n",
                (Integer.MAX_VALUE + 2.0), Integer.MAX_VALUE,
                (int)(Integer.MAX_VALUE + 2.0));
    }

}
