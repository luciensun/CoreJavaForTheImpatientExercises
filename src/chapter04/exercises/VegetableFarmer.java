package chapter04.exercises;

public class VegetableFarmer implements BaseFarmer<Vegetable>{

	@Override
	public void blossom(Vegetable vegetable) {
		System.out.println("VegetableFarmer" + " makes " + vegetable + " blossom.");
	}

	@Override
	public void seed(Vegetable vegetable) {
		System.out.println("VegetableFarmer" + " makes " + vegetable + " seed.");
	}

}
