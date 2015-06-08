package chapter01.exercises;

public class Exercise02 {

    public static void main(String[] args) {
        // angle in [600, -600, 180, -180, 90, -90] etc
        int angle = -600;
        // The % operator yields the remainder
        // always be careful using % with potentially negative operands. 
        System.out.printf("The integer angle %d° is normalized to a value "
                + "between 0 and 359 degrees %d°%n", angle,
                (angle % 360 + 360) % 360);
        
        // Math.floorMod (base, divisor) yields a value between 0 and divisor-1. 
        // floorMod gives negative results for negative divisors
        System.out.printf("The integer angle %d° is normalized to a value "
                + "between 0 and 359 degrees %d°%n", angle,
                Math.floorMod(angle, 360));
    }

}
