package chapter04.exercises;

public class FruitFarmer implements BaseFarmer<Fruit>{

	@Override
	public void blossom(Fruit fruit) {
		System.out.println("FruitFarmer" + " makes " + fruit + " blossom.");
	}

	@Override
	public void seed(Fruit fruit) {
		System.out.println("FruitFarmer" + " makes " + fruit + " seed.");
	}

}
