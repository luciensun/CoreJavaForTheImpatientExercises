package chapter02.exercises;

/**
 *  5. Implement an immutable class Point 
 *  that describes a point in the plane. 
 *  Provide a constructor to set it to a specific point, 
 *  a no-arg constructor to set it to the origin, 
 *  and methods getX, getY, translate, and scale. 
 *  The translate method moves the point by a given amount in x- and y-direction. 
 *  The scale method scales both coordinates by a given factor. 
 *  Implement these methods so that they return new points with the results. 
 *  For example, Point p = new Point(3, 4).translate(1, 3).scale(0.5);
 *  should set p to a point with coordinates (2, 3.5).
 * @author lucienSun
 *
 */
public class Exercise05 {
    class Point {
        private double x;
        private double y;
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
        
        Point translate(double dx, double dy) {
            Point newPoint = new Point(getX() + dx, getY() + dy);
            return newPoint;
        }
        
        Point scale(double factor) {
            Point newPoint = new Point(getX()*factor, getY() * factor);
            return newPoint;
        }
    }
    public static void main(String[] args) {
        Point p = new Exercise05().new Point(3, 4).translate(1, 3).scale(0.5);
        System.out.printf("The coordinates of p is (%.1f, %.1f).%n", p.getX(), p.getY());

    }

}
