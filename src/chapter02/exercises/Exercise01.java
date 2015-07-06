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
        LocalDate beginDay = LocalDate.of(year, month, 1);
        LocalDate currentDay = beginDay;
        // 打印日历的表头 print the header of the calendar
        System.out.printf(
                "     %4s     %4s     %4s     %4s     %4s     %4s     %4s%n",
                "日", "一", "二", "三", "四", "五", "六");

        while (currentDay.getMonth() == month) {

            // 获得 当前日期是周几的信息 get the day of week
            DayOfWeek weekday = currentDay.getDayOfWeek();
            int value = weekday.getValue();

            if (currentDay.getDayOfMonth() == 1) {
                // 如果每周 以 周日开始 则 需要 value % 7
                for (int i = 0; i < value % 7; i++) {
                    System.out.print("    ");
                }
            }

            System.out.printf("%4d", currentDay.getDayOfMonth());

            if (value == 6) {
                // 如果是周六，那么需要换行 if currentDay is Saturday print new line
                System.out.println();
            }
            currentDay = currentDay.plusDays(1);
        }
    }

}
