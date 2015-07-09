package chapter02.exercises;

import java.util.ArrayList;
import java.util.Random;

/**
 * 10. In the RandomNumbers class, 
 * provide two static methods randomElement that get a random element 
 * from an array or array list of integers. 
 * (Return zero if the array or array list is empty.) 
 * Why couldn’t you make these methods into instance methods of int[] or ArrayList<Integer>?
 * @author lucienSun
 *
 * answer
 * I think I can make this method into instance method of ArrayList<Integer> but
 * can't do it for int[] because int[] is not a class.
 */
public class Exercise10 {
    static class RandomNumbers {
        static int randomElement(int[] intArray) {
            if (intArray == null || intArray.length == 0) {
                // return zero if the array is empty
                return 0;
            } else {
                Random random = new Random();
                int ind = random.nextInt(intArray.length);
                return intArray[ind];
            }
        }
        static int randomElement(ArrayList<Integer> intList) {
            if (intList == null || intList.size() == 0) {
                return 0;
            } else {
                Random random = new Random();
                int ind = random.nextInt(intList.size());
                return intList.get(ind); 
            }
        }
    }
    public static void main(String[] args) {
        int[] intArray = new int[] { 1, 3, 5, 7, 9};
        int val = Exercise10.RandomNumbers.randomElement(intArray);
        System.out.println("The random value is " + val);
    }

}
