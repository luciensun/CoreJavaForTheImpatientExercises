package chapter02.exercises;

import java.util.ArrayList;

/**
 * 3. Can you ever have a mutator method return something other than void? 
 * Can you ever have an accessor method return void? 
 * Give examples when possible.
 * @author lucienSun
 *
 */
public class Exercise03 {

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        ArrayList<String> list = new ArrayList<String>();
        list.add("zhangsan");
        /**
         * we can have a mutator method return sth other than void
         * for example, ArrayList's add method returns boolean which
         * indicates whether the operation is successful or not.
         * I think we can not have an accessor method return void because
         * it is meaningless to have an object unchanged and return nothing.
         */
    }

}
