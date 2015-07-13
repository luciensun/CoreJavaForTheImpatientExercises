package chapter02.exercises;

/**
 * 12. Make a file HelloWorld.java that declares a class HelloWorld in a package ch01.sec01. 
 * Put it into some directory, but not in a ch01/sec01 subdirectory. 
 * From that directory, run javac HelloWorld.java. 
 * Do you get a class file? Where? Then run java HelloWorld. 
 * What happens? Why? (Hint: Run javap HelloWorld and study the warning message.) 
 * Finally, try javac -d . HelloWorld.java. Why is that better?
 * @author lucienSun
 *
 * answer:
 * yes, I get a class file in current directory(not in a ch01/sec01 subdirectory.)
 * But if I run java HelloWorld, I get an error message 错误: 找不到或无法加载主类 HelloWorld
 * The fully qualified name is ch01.sec01.HelloWorld
 * javap HelloWorld will get following info
 * 警告: 二进制文件ch01\sec01\HelloWorld包含ch01.sec01.HelloWorld
Compiled from "HelloWorld.java"
public class ch01.sec01.HelloWorld {
    public ch01.sec01.HelloWorld();
 */
public class Exercise12 implements A, B{
    @Override
    public void m() {
        B.super.m();
        System.out.println("Exercise12's m() will print Exercise12");
    }
    public static void main(String[] args) {
        Exercise12 exercise = new Exercise12();
        exercise.m();
    }

}

interface A {
    default void m() {
        System.out.println("A's m() will print A");
    }
}

interface B {
    default void m() {
        System.out.println("B's m() will print B");
    }
}
