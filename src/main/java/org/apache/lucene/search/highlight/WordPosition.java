package org.apache.lucene.search.highlight;

public class WordPosition {

	public Integer start;

	public Integer finish;

	public WordPosition(Integer start, Integer finish) {
		super();
		this.start = start;
		this.finish = finish;
	}

	@Override
	public String toString() {
		return "WordPosition [start=" + start + ", finish=" + finish + "]";
	}
}
