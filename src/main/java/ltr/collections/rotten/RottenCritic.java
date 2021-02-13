package ltr.collections.rotten;

/**
 * Uma critica de um conteúdo da rotten
 */
public class RottenCritic {
    private String criticName;
    private Boolean topCritic;
    private String publisher;
    private String criticDate;
    private String critic;
    
    
	public RottenCritic(String criticName, Boolean topCritic, String publisher, String criticDate, String critic) {
		super();
		this.criticName = criticName;
		this.topCritic = topCritic;
		this.publisher = publisher;
		this.criticDate = criticDate;
		this.critic = critic;
	}
	
	
	@Override
	public String toString() {
		return criticName + "\n" +  topCritic  + "\n" + publisher
				+ "\n" + criticDate + "\n" + critic;
	}


	public String getCriticName() {
		return criticName;
	}
	public void setCriticName(String criticName) {
		this.criticName = criticName;
	}
	public Boolean getTopCritic() {
		return topCritic;
	}
	public void setTopCritic(Boolean topCritic) {
		this.topCritic = topCritic;
	}
	public String getPublisher() {
		return publisher;
	}
	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}
	public String getCriticDate() {
		return criticDate;
	}
	public void setCriticDate(String criticDate) {
		this.criticDate = criticDate;
	}
	public String getCritic() {
		return critic;
	}
	public void setCritic(String critic) {
		this.critic = critic;
	}
    
    
}
