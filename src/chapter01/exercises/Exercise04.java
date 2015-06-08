package chapter01.exercises;

public class Exercise04 {

    public static void main(String[] args) {
        System.out.printf("The largest postive double value is %g%n"
                + ", and the smallest positive double value is %g%n",
                Double.MAX_VALUE, Math.nextUp(0.0));
    }

}
