package chapter02.exercises;

/**
 * 6. Repeat the preceding exercise, 
 * but now make translate and scale into mutators.
 * @author lucienSun
 *
 */
public class Exercise06 {
    /**
     * a mutable Point object
     * @author lucienSun
     * @version 0.1
     */
    class Point {
        private double x;
        private double y;
        /**
         * a constructor with args
         * @param x the x coordinates
         * @param y the y coordinates
         */
        Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
        
        Point() {
            this(0, 0);
        }
        
        double getX() {
            return this.x;
        }
        
        double getY() {
            return this.y;
        }
        
        void setX(double x) {
            this.x = x;
        }
        
        void setY(double y) {
            this.y = y;
        }
        
        void translate(double dx, double dy) {
            setX(getX() + dx);
            setY(getY() + dy);
        }
        
        void scale(double factor) {
            setX(getX() * factor);
            setY(getY() * factor);
        }
    }
    public static void main(String[] args) {
        Point p = new Exercise06().new Point(3, 4);
        p.translate(1, 3);
        p.scale(0.5);
        System.out.printf("The coordinates of p is (%.1f, %.1f).%n", p.getX(), p.getY());
    }

}
