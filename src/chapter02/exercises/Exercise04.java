package chapter02.exercises;

import org.omg.CORBA.IntHolder;

/**
 * 4. Why can’t you implement a Java method that swaps the contents of two int variables? 
 * Instead, write a method that swaps the contents of two IntHolder objects. 
 * (Look up this rather obscure class in the API documentation.) 
 * Can you swap the contents of two Integer objects?
 * 
 * answer:
 * 1)object references When you pass an object to a method, 
 * the method obtains a copy of the object reference. 
 * Through this reference, it can access or mutate the parameter object.
 * 
 * 2)primitive type values In Java, 
 * you can never write a method that updates primitive type parameters.
 * 
 * 3)In Java, all parameters
 * —object references as well as primitive type values—
 * are passed by value.
 * 
 * I can't swap the contents of two Integer objects, 
 * because I can't change the contents of an Integer.
 * If you assign an Integer variable a new value, 
 * It will just refer to a new Integer object.
 * @author lucienSun
 *
 */
public class Exercise04 {
    
    /**
     * this swap the references to an IntHolder in the swap1 method
    * @Title: swap 
    * @Description: TODO(这里用一句话描述这个方法的作用) 
    * @param @param intHolder1
    * @param @param intHolder2    设定文件 
    * @return void    返回类型 
    * @throws
     */
    public static void swap1(IntHolder intHolder1, IntHolder intHolder2) {
        IntHolder temp = new IntHolder();
        temp = intHolder1;
        intHolder1 = intHolder2;
        intHolder2 = temp;
    }
    
    /**
     * this swap the contents of two IntHolder objects
    * @Title: swap2 
    * @Description: TODO(这里用一句话描述这个方法的作用) 
    * @param @param intHolder1
    * @param @param intHolder2    设定文件 
    * @return void    返回类型 
    * @throws
     */
    public static void swap2(IntHolder intHolder1, IntHolder intHolder2) {
        int temp = 0;
        temp = intHolder1.value;
        intHolder1.value = intHolder2.value;
        intHolder2.value = temp;
    }

    public static void swap(Integer a, Integer b) {
        int temp = 0;
        temp = a.intValue();
        a = b.intValue(); // equals to a = new Integer(b.intValue());
        b = temp; // equals to b = new Integer(temp);
    }
    
    public static void main(String[] args) {
        IntHolder intHolder1 = new IntHolder(1);
        IntHolder intHolder2 = new IntHolder(2);
        //swap1(intHolder1, intHolder2);
        swap2(intHolder1, intHolder2);
        System.out.println("intHolder1's value is " + intHolder1.value + " and intHolder2's value is " + intHolder2.value);
        
        Integer intA = new Integer(1111);
        Integer intB = new Integer(2222);
        swap(intA, intB);
        System.out.println("intA's value is " + intA + " and intB's value is " + intB);
        
    }

}
