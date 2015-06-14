package chapter01.exercises;

import java.util.ArrayList;
import java.util.List;

/**
 * Write a program that stores Pascal’s triangle up to a given n in an
 * ArrayList<ArrayList<Integer>>.
 * 
 * @author Lucien
 *
 */
public class Exercise15 {

    public static void main(String[] args) {
        int n = 6;
        ArrayList<ArrayList<Integer>> triangle = new ArrayList<ArrayList<Integer>>();

        for (int i = 0; i < n; i++) {
            ArrayList<Integer> row = new ArrayList<Integer>();
            row.add(1);

            for (int j = 1; j < i; j++) {
                row.add(triangle.get(i - 1).get(j - 1)
                        + triangle.get(i - 1).get(j));
            }
            if (i > 0) {
                row.add(1);
            }
            triangle.add(row);
        }
        
        for (ArrayList<Integer> row : triangle) {
            for (Integer col:row) {
                System.out.printf("%4d", col);
            }
            System.out.println();
        }

    }

}
