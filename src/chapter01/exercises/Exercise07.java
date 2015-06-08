package chapter01.exercises;

public class Exercise07 {

    public static void main(String[] args) {
        Short num1 = (short) 65530;
        Short num2 = (short) 6000;
        System.out.println("The unsigned sum of num1 and num2 is "
                + ((short)(Short.toUnsignedInt(num1) + Short.toUnsignedInt(num2))));
        
        System.out.println("The difference of num1 and num2 is "
                + (num1 - num2));
        
        System.out.println("The product of num1 and num2 is "
                + (num1 * num2));
        
        System.out.println("The quotient of num1 and num2 is "
                + (num1 / num2));
        
        System.out.println("The remainder of num1 and num2 is "
                + (num1 % num2));
    }

}
