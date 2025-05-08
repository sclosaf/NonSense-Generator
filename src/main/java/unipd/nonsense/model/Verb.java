package unipd.nonsense.model;

public class Verb{

	private static enum Tense{PAST, PRESENT, FUTURE};
	private String verb;
	private final Tense tense;

	public Verb(String newVerb, Tense newTense){
		verb = newVerb;
		tense = newTense;
	}

	public String getVerb(){
		return verb;
	}

	public void setVerb(String newVerb){
		verb = newVerb;
	}

	public Tense getTense(){
		return tense;
	}

}
