package org.apache.lucene.search.highlight;

public class SearchResponse {

	private Integer docId;

	private String highlightedText;

	private String pathDoc;

	private Float score;

	public SearchResponse() {

	}

	public SearchResponse(Integer docId, String pathDoc) {
		super();
		this.docId = docId;
		this.pathDoc = pathDoc;
	}

	public SearchResponse(Integer docId, String highlightedText, String pathDoc) {
		super();
		this.docId = docId;
		this.highlightedText = highlightedText;
		this.pathDoc = pathDoc;
	}

	public SearchResponse(Integer docId, String highlightedText, String pathDoc, Float score) {
		super();
		this.docId = docId;
		this.highlightedText = highlightedText;
		this.pathDoc = pathDoc;
		this.score = score;
	}

 

	@Override
	public String toString() {
		return "SearchResponse [docId=" + docId + ", highlightedText=" + highlightedText + ", pathDoc=" + pathDoc
				+ ", score=" + score + "]";
	}

	public Integer getDocId() {
		return docId;
	}

	public void setDocId(Integer docId) {
		this.docId = docId;
	}

	public String getHighlightedText() {
		return highlightedText;
	}

	public void setHighlightedText(String highlightedText) {
		this.highlightedText = highlightedText;
	}

	public String getPathDoc() {
		return pathDoc;
	}

	public void setPathDoc(String pathDoc) {
		this.pathDoc = pathDoc;
	}

	public Float getScore() {
		return score;
	}

	public void setScore(Float score) {
		this.score = score;
	}

}
