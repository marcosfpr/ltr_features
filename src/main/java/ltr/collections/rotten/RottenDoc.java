package ltr.collections.rotten;

import java.util.ArrayList;
import java.util.List;

import ltr.features.QueryDocument;

/**
 * Um documento da rotten em memória
 */
public class RottenDoc implements QueryDocument{
	private String movieId;
    private String movieTitle;
    private List<String> labels;
    private String info;
    private List<RottenCritic> critics;

    
    public RottenDoc(String movieId, String movieTitle, String info, List<String> labels) {
    	this.setMovieId(movieId);
    	this.setTitle(movieTitle);
    	this.setInfo(info);
    	this.setLabels(labels);
    	this.critics = new ArrayList<RottenCritic>();
    }

    @Override
    public String getText() {
    	return this.info + "\n" + this.critics;
		// return this.info;
    }
    
    public void addCritic(RottenCritic critic) {
    	this.critics.add(critic);
    }

    @Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RottenDoc other = (RottenDoc) obj;
		if (movieId == null) {
			if (other.movieId != null)
				return false;
		} else if (!movieId.equals(other.movieId))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((movieId == null) ? 0 : movieId.hashCode());
		return result;
	}

	@Override
	public String getId() {
		return movieId;
	}


	public void setMovieId(String movieId) {
		this.movieId = movieId;
	}

	@Override
	public String getTitle() {
		return movieTitle;
	}


	public void setTitle(String movieTitle) {
		this.movieTitle = movieTitle;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}
	
	@Override
	public List<String> getLabels() {
		return labels;
	}

	public void setLabels(List<String> labels) {
		if(this.labels == null) this.labels = new ArrayList<String>();
		
		for(String label : labels)
			this.labels.add(label.trim().toLowerCase());
	}

	@Override
	public String toString() {
		return "RottenDoc [movieId=" + movieId + ", movieTitle=" + movieTitle + ", labels=" + labels + "]";
	}

	
}
