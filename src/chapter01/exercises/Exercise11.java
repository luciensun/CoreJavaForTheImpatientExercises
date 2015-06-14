package chapter01.exercises;

/**
 * Write a program that reads a line of text and prints all characters that are not ASCII, 
 * together with their Unicode values.
 * @author Lucien
 *
 */
public class Exercise11 {

    public static void main(String[] args) {
        String text = "abc  中文测试 　　ｆａｄｆａｅ end here";
             
        int[] codePoints = text.codePoints().toArray();
        for (int codePoint:codePoints) {
            // the range of ascii is 0 to 127
            if (codePoint > 127) {
                // 04X means hexadecimal is in uppercase and 4 digits with leading 0
                System.out.printf("the code point  is '%c' and '%04X'%n", codePoint, 
                        codePoint);
            }

        }

    }

}
