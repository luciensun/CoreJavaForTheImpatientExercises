package chapter02.exercises;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;

import static java.time.LocalDate.*;
import static java.lang.System.*;

/**
 * 11. Rewrite the Cal class to use static imports for the System and LocalDate
 * classes.
 * 
 * @author lucienSun
 *
 */
public class Exercise11 {
    class Cal {
        Cal() {
            this(now());
        }
        Cal(LocalDate theDay) {

            int year = theDay.getYear();
            Month month = theDay.getMonth();
            LocalDate beginDay = of(year, month, 1);
            LocalDate currentDay = beginDay;
            // 打印日历的表头 print the header of the calendar
            out.printf(
                    "     %4s     %4s     %4s     %4s     %4s     %4s     %4s%n",
                    "一", "二", "三", "四", "五", "六", "日");

            while (currentDay.getMonth() == month) {

                // 获得 当前日期是周几的信息 get the day of week
                DayOfWeek weekday = currentDay.getDayOfWeek();
                int value = weekday.getValue();

                if (currentDay.getDayOfMonth() == 1) {
                    // 如果每周 以 周一开始 则 需要 value % 7
                    for (int i = 0; i < (value - 1) % 7; i++) {
                        out.print("    ");
                    }
                }

                out.printf("%4d", currentDay.getDayOfMonth());

                if (value == 7) {
                    // 如果是周日，那么需要换行 if currentDay is Sunday print new line
                    out.println();
                }
                currentDay = currentDay.plusDays(1);
            }
        }
    }

    public static void main(String[] args) {
        CharSequence text = "2008-8-8";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-M-d");
        LocalDate theDay = LocalDate.parse(text, formatter);
        new Exercise11().new Cal(theDay);
        
        new Exercise11().new Cal();
    }

}
