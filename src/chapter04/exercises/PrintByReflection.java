package chapter04.exercises;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

abstract class PrintClzInfoByReflect {
	private int value;
	private String name;
	private int[] childs;
	public int printI(int value, String testArg) {
		System.out.println(value + testArg);
		return value;
	}
	public String printS(String value, Integer intVal) {
		System.out.println(value + intVal);
		return value;
	};
	// print the variable's type info
	void printVariableTypeInfo(Class<?> varType) {
		if (varType.isPrimitive()) {
			// if the field's type is primitive
			System.out.print(varType.getSimpleName());
		} else if (varType.isArray()) {
			System.out.print(varType.getSimpleName());
		} else {
			System.out.print(varType.getSimpleName());
		}
	}
	void test() {
		Type type = this.getClass().getGenericSuperclass();
		// print class mqTest.PrintClzInfoByReflect
		//System.out.println(type);
		if (type instanceof ParameterizedType) {
			// class is a generic type
			ParameterizedType parameterizedType = (ParameterizedType) type;
			Type[] actualTypes = parameterizedType.getActualTypeArguments();
			if (actualTypes != null && actualTypes.length > 0) {
				System.out.println(actualTypes[0]);
			}
		} else {
			// class is not a generic type
			Class clzType = (Class) type;
			// print class description
			System.out.print(Modifier.toString(clzType.getModifiers()));
			if (clzType.isEnum()) {
				System.out.print(" enum ");
			} else if (clzType.isInterface()) {
				System.out.print(" interface ");
			} else{
				System.out.print(" class ");
			}			
			System.out.println(clzType.getSimpleName() + " {");
			Field[] fields = clzType.getDeclaredFields();
			for (Field field:fields) {
				// print field description
				System.out.print(Modifier.toString(field.getModifiers()) + " ");
				Class<?> fieldType = field.getType();
				printVariableTypeInfo(fieldType);
				
				System.out.println(" " + field.getName() + ";");
			}
			
			Method[] methods = clzType.getDeclaredMethods();
			for (Method method : methods) {
				// print method description
				System.out.print(Modifier.toString(method.getModifiers()) + " ");
				Class<?> methodReturnType = method.getReturnType();
				printVariableTypeInfo(methodReturnType);
				System.out.print(" " + method.getName() + "(");
				Parameter[] methodParams = method.getParameters();
				for (int ind = 0; ind < methodParams.length; ind++) {
					Parameter methodParam = methodParams[ind];
					printVariableTypeInfo(methodParam.getType());
					if (ind < methodParams.length-1) {
						System.out.print(" " + methodParam.getName() + ", ");
					} else {
						System.out.print(" " + methodParam.getName());
					}
				}
				System.out.println(");");
			}
			System.out.println("}");
		}
	}
}

public class PrintByReflection extends PrintClzInfoByReflect {

	public static void main(String[] args) {
		PrintByReflection clzTest = new PrintByReflection();
		clzTest.test();
	}

}
