package unipd.nonsense.model;

public class Adjective{
	public Adjective(String newAdj){
		adj = newAdj;
	}

	public String getAdjective(){
		return adj;
	}

	public void setAdjective(String newAdj){
		adj = newAdj;
	}

	private String adj;
}
