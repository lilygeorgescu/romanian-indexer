package ir.indexer;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WordlistLoader;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.highlight.MyHighlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SearchResponse;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.IOUtils;

import com.facebook.infer.annotation.ThreadSafe;

import ir.custom.score.CustomSimilarity;

@ThreadSafe
public class Searcher {
	private DirectoryReader ireader;
	private IndexSearcher isearcher;
	private QueryParser parser;
	private Analyzer analyzer;
	private ArrayList<String> romanianStopWords;
	private CharArraySet newStopwords;

	public Searcher() throws IOException {
		ireader = DirectoryReader.open(FSDirectory.open(Paths.get(Constants.INDEX_PATH)));
		isearcher = new IndexSearcher(ireader);

		Reader reader = null;
		CharArraySet defaultRomanianStopWords = null;
		romanianStopWords = new ArrayList<String>();
		try {
			reader = IOUtils.getDecodingReader(RomanianAnalyzer.class.getResourceAsStream("stopwords.txt"),
					StandardCharsets.UTF_8);
			defaultRomanianStopWords = WordlistLoader.getWordSet(reader, "#", new CharArraySet(16, false));
			// delete diacritice
			String stopwords[] = defaultRomanianStopWords.toString().replace('[', ' ').replace(']', ' ').split(",");

			for (String stopword : stopwords) {
				romanianStopWords.add(RomanianChanges.change(stopword.trim()));
			}
			newStopwords = new CharArraySet(romanianStopWords, true);
			analyzer = new RomanianAnalyzer(newStopwords);
		} finally {
			IOUtils.close(reader);
		}
		parser = new QueryParser(Constants.CONTENT, analyzer);
		parser.setDefaultOperator(QueryParser.Operator.OR);
	}

	public Searcher(Analyzer analyzer) throws IOException {
		isearcher = new IndexSearcher(ireader);
		this.analyzer = analyzer;
		parser = new QueryParser(Constants.CONTENT, analyzer);
	}

	@SuppressWarnings("unused")
	private String deleteStopWords(String words) {
		StringBuffer buffer = new StringBuffer(words);
		for (String word : words.split(" ")) {
			if (romanianStopWords.contains(word)) {
				int inc = buffer.lastIndexOf(word);
				buffer.replace(inc - 1, inc + word.length(), "");
			}
		}
		return new String(buffer);
	}

	public ArrayList<String> search(String words) throws Exception {
		return search(words, false);
	}

	public ArrayList<String> search(String words, boolean useCustomSimilarity) throws Exception {
		return search(words, useCustomSimilarity, CustomSimilarity.DEFAULT_DISTANCE);
	}

	public ArrayList<String> search(String words, boolean useCustomSimilarity, int distance) throws Exception {
		ArrayList<String> result = new ArrayList<String>();
		Query query = parser.parse((RomanianChanges.change(words)));
		if (useCustomSimilarity) {
			isearcher.setSimilarity(new CustomSimilarity(ireader, distance));
		}
		ScoreDoc[] hits = isearcher.search(query, 1000).scoreDocs;
		for (int i = 0; i < hits.length; i++) {
			Document hitDoc = isearcher.doc(hits[i].doc);
			System.out.println(getPath(hitDoc.getField(Constants.PATH).toString()));
			System.out.println(hits[i].score);
			result.add(getPath(hitDoc.getField(Constants.PATH).toString()));
		}
		return result;

	}

	public ArrayList<SearchResponse> searchAndHighlight(String words) throws Exception {
		return searchAndHighlight(words, false, CustomSimilarity.DEFAULT_DISTANCE);
	}

	public ArrayList<SearchResponse> searchAndHighlight(String words, boolean useCustomSimilarity) throws Exception {
		return searchAndHighlight(words, useCustomSimilarity, CustomSimilarity.DEFAULT_DISTANCE);
	}

	public ArrayList<SearchResponse> searchAndHighlight(String words, boolean useCustomSimilarity, int distance)
			throws Exception {
		ArrayList<SearchResponse> result = new ArrayList<SearchResponse>();
		Query query = parser.parse((RomanianChanges.change(words)));
		if (useCustomSimilarity) {
			isearcher.setSimilarity(new CustomSimilarity(ireader, distance));
		}
		ScoreDoc[] hits = isearcher.search(query, 1000).scoreDocs;
		MyHighlighter highlighter = new MyHighlighter(new QueryScorer(query));
		for (int i = 0; i < hits.length; i++) {
			Document doc = isearcher.doc(hits[i].doc);
			String text = doc.get(Constants.CONTENT);
			@SuppressWarnings("deprecation")
			TokenStream tokenStream = TokenSources.getTokenStream(doc, Constants.CONTENT, analyzer);
			SearchResponse response = new SearchResponse(hits[i].doc,
					highlighter.getBestTextFragments(tokenStream, text),
					getPath(doc.getField(Constants.PATH).toString()),hits[i].score);
			result.add(response);

		}
		return result;

	}

	public void close() throws IOException {
		ireader.close();
	}

	private String getPath(String path) {
		if (path.lastIndexOf("path") != -1) {
			return path.substring(path.lastIndexOf("path") + 5, path.length() - 1);
		}
		return null;
	}

	public void changeDefaultOperator(QueryParser.Operator operator) {
		parser.setDefaultOperator(operator);
	}
}
