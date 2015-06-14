package chapter01.exercises;

/**
 * Improve the average method so that it is called with at least one parameter.
 * @author Lucien
 *
 */
public class Exercise16 {

    public static double average(double defaultValue, double... values) {
        double sum = 0;
        for (double value : values)
            sum = sum + value;
        return values.length == 0 ? defaultValue : sum / values.length;
    }

    public static void main(String[] args) {
        double[] values = { 1, 2, 3, 4, 5, 6 };
        double averageVal = average(3, values);
        //double averageVal = average(3);
        System.out.printf("The average value of values is %s.%n", averageVal);
    }

}
