package chapter04.exercises;

public interface Plant<T> {
	public void setBaseFarmer(BaseFarmer<T> baseFarmer);
	public void blossom(T t);
	public void seed(T t);
}
