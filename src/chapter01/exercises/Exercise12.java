package chapter01.exercises;

/**
 * The Java Development Kit includes a file src.zip with the source code of the
 * Java library. Unzip and, with your favorite text search tool, find usages of
 * the labeled break and continue sequences. Take one and rewrite it without a
 * labeled statement.
 * 
 * @author Lucien
 *
 */
public class Exercise12 {

    public static void main(String[] args) {
        int i = 0, j = 0;
        exit:
        while (true) {
            System.out.println("in outer loop the " + i + "count");
            while (true) {
                System.out.println("in inner loop the " + j + "count");
                if (j > 2) {
                    j = 0;
                    break exit;
                }
                j++;
            }
//            if (i > 5)
//                break;
//            i++;
        }

    }

}
