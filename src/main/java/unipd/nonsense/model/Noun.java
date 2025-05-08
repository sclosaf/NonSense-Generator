package unipd.nonsense.model;

public class Noun
{
	public static enum Number{SINGULAR, PLURAL};
	private String noun;
	private final Number number;

	public Noun(String newNoun, Number newNumber){
		noun = newNoun;
		number = newNumber;
	}

	public String getNoun(){
		return noun;
	}

	public void setNoun(String newNoun){
		noun = newNoun;
	}

	public Number getNumber(){
		return number;
	}

}
