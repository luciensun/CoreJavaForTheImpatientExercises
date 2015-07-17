package chapter04.exercises;

public abstract class PlantImpl<T> implements Plant<T> {
	private BaseFarmer<T> baseFarmer;
	@Override
	public void setBaseFarmer(BaseFarmer<T> baseFarmer) {
		this.baseFarmer = baseFarmer;
	}
	
	@Override
	public void blossom(T t) {
		baseFarmer.blossom(t);
	}
	
	@Override
	public void seed(T t) {
		baseFarmer.seed(t);
	}
}
