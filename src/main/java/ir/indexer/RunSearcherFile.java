package ir.indexer;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.apache.lucene.search.highlight.SearchResponse;

import ir.custom.score.CustomSimilarity;

public class RunSearcherFile {
	public static void main(String[] args) throws Exception {

		Searcher searcher = new Searcher();
		String words = new String(Files.readAllBytes(Paths.get("input.txt")));
		List<SearchResponse> response = searcher.searchAndHighlight(words, false, CustomSimilarity.DEFAULT_DISTANCE);
		response.forEach(e-> System.out.println(e));
	 
		// List<String> paths =searcher.search(words, true);
		// if (paths.size() == 0) {
		// System.out.printf("We could not find any docs to match your query: %s.",
		// words);
		// } else {
		// System.out.printf("We've found %d docs that match your query: %s.\n",
		// paths.size(), words);
		// paths.forEach(a -> System.out.println(a));
		// }
	}
}
