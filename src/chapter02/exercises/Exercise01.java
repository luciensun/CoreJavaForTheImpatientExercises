package chapter02.exercises;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;

/**
 * Change the calendar printing program so it starts the week on a Sunday. Also
 * make it print a newline at the end (but only one).
 * 
 * @author Lucien
 *
 */
public class Exercise01 {

    public static void main(String[] args) {
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        Month month = today.getMonth();
        LocalDate beginDay = LocalDate.of(today.getYear(), month, 1);
        LocalDate currentDay = beginDay;
        while (currentDay.getMonth() == month) {

            if (currentDay.getDayOfMonth() == 1) {
                DayOfWeek weekday = currentDay.getDayOfWeek();
                int value = weekday.getValue();
                // 如果每周 以 周日开始 则 需要 value % 7
                for (int i = 0; i < value % 7; i++) {
                    System.out.print("    ");
                }
            }
            System.out.printf("%4d", currentDay.getDayOfMonth());
            currentDay = currentDay.plusDays(1);
        }
    }

}
