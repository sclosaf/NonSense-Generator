package unipd.nonsense.model;

public class Noun{
	public Noun(String newNoun){
		noun = newNoun;
	}

	public String getNoun(){
		return noun;
	}

	public void setNoun(String newNoun){
		noun = newNoun;
	}

	private String noun;
}