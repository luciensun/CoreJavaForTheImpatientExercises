package chapter01.exercises;

/**
 * Section 1.5.3, “String Comparison,” on p. 21 has an example of two strings s
 * and t so that s.equals(t) but s != t. Come up with a different example that
 * doesn’t use substring).
 * 
 * @author Lucien
 *
 */
public class Exercise09 {

    public static void main(String[] args) {
        String s = "It's just a test";
        String t = "It's just a" + " test";
        System.out.printf("'s == t' is %b, but that 's.equals(t)' is %b.%n",
                s == t, s.equals(t));

        t = "It's just a placeHolder".replace("placeHolder", "test");
        System.out.printf("'s == t' is %b, but that 's.equals(t)' is %b.%n",
                s == t, s.equals(t));

        t = new String("It's just a test");
        System.out.printf("'s == t' is %b, but that 's.equals(t)' is %b.%n",
                s == t, s.equals(t));
    }

}
