package chapter05.exercises;

public class ProxyDemo {
	public static void main(String[] args) {
		LogHandler logHandler = new LogHandler();
		IHello helloProxy = (IHello)logHandler.bind(new HelloSpeaker());
		helloProxy.hello("Lucien");
	}
}
