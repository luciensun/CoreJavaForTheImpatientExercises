package chapter04.exercises;

public class PlantImplTest {
	public static void main(String[] args) {
		BaseFarmer<Vegetable> vegetablefarmer = new VegetableFarmer();
		BaseFarmer<Fruit> fruitFarmer = new FruitFarmer();
		Plant<Vegetable> vegetable = new VegetableImpl();
		vegetable.setBaseFarmer(vegetablefarmer);
		vegetable.blossom((Vegetable) vegetable);
		/**
		 * @See VegetableImpl 
		 * It confirms that the Farmer type must corresponds to the plant type
		 * so this statement will be wrong
		 */
		//plant.setBaseFarmer(fruitFarmer);
		Plant<Fruit> fruit = new FruitImpl();
		fruit.setBaseFarmer(fruitFarmer);
		fruit.blossom((Fruit)fruit);
	}
}
