package chapter02.exercises;

/**
 * 9. Implement a class Car that models a car traveling along the x-axis,
 * consuming gas as it moves. Provide methods to drive by a given number of
 * miles, to add a given number of gallons to the gas tank, and to get the
 * current distance from the origin and fuel level. Specify the fuel efficiency
 * (in miles/gallons) in the constructor. Should this be an immutable class? Why
 * or why not?
 * 
 * @author lucienSun
 *
 * answer:
 * This should not be an immutable class because the car's situation 
 * will change, it consumes gas as it moves.
 */
public class Exercise09 {
    class Car {
        private double miles;
        private double gallons;
        private double fuelEfficiency;

        Car(double miles, double gallons, double fuelEfficiency) {
            this.miles = miles;
            this.gallons = gallons;
            this.fuelEfficiency = fuelEfficiency;
        }

        Car(double fuelEfficiency) {
            this(0, 0, fuelEfficiency);
        }

        void setMiles(double miles) {
            this.miles = miles;
        }

        double getMiles() {
            return miles;
        }

        void setGallons(double gallons) {
            this.gallons = gallons;
        }

        double getGallons() {
            return gallons;
        }

        void setFuelEfficiency(double fuelEfficiency) {
            this.fuelEfficiency = fuelEfficiency;
        }

        double getFuelEfficiency() {
            return fuelEfficiency;
        }

        /**
         * drive by a given number of miles
         * 
         * @Title: driveMiles
         * @Description: TODO(这里用一句话描述这个方法的作用)
         * @param @param dmiles 设定文件
         * @return void 返回类型
         * @throws
         */
        void driveMiles(double dmiles) {
            setMiles(getMiles() + dmiles);
            setGallons(getGallons() - dmiles / getFuelEfficiency());
        }

        /**
         * add a given number of gallons to the gas tank
         * 
         * @Title: gasUp
         * @Description: TODO(这里用一句话描述这个方法的作用)
         * @param @param dGallons 设定文件
         * @return void 返回类型
         * @throws
         */
        void gasUp(double dGallons) {
            setGallons(getGallons() + dGallons);
        }
    }

    public static void main(String[] args) {
        Car myCar = new Exercise09().new Car(0, 2, 12);
        myCar.driveMiles(12);
        System.out
                .printf("MyCar's current situation is (%.1f, %.1f, %.1f).%n",
                        myCar.getMiles(), myCar.getGallons(),
                        myCar.getFuelEfficiency());
    }

}
