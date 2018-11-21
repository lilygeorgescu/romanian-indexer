package org.apache.lucene.search.highlight;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.util.PriorityQueue;

import ir.indexer.Constants;

/**
 * Marks up highlighted terms found in the best sections of text, using
 * configurable {@link Fragmenter}, {@link Scorer}, {@link Formatter},
 * {@link Encoder} and tokenizers.
 *
 * This is Lucene's original Highlighter; there are others.
 */
public class MyHighlighter {
	public static final int DEFAULT_MAX_CHARS_TO_ANALYZE = 50 * 1024;

	private Encoder encoder;
	private Scorer fragmentScorer;
	private int maxDocCharsToAnalyze = DEFAULT_MAX_CHARS_TO_ANALYZE;
	private Fragmenter textFragmenter = new SimpleFragmenter();

	public MyHighlighter(Scorer fragmentScorer) {
		this.encoder = new DefaultEncoder();
		this.fragmentScorer = fragmentScorer;
	}

	public String getBestTextFragments(TokenStream tokenStream, String text)
			throws IOException, InvalidTokenOffsetsException {
		ArrayList<TextFragment> docFrags = new ArrayList<>();
		StringBuilder newText = new StringBuilder();

		CharTermAttribute termAtt = tokenStream.addAttribute(CharTermAttribute.class);
		OffsetAttribute offsetAtt = tokenStream.addAttribute(OffsetAttribute.class);
		TextFragment currentFrag = new TextFragment(newText, newText.length(), docFrags.size());

		if (fragmentScorer instanceof QueryScorer) {
			((QueryScorer) fragmentScorer).setMaxDocCharsToAnalyze(maxDocCharsToAnalyze);
		}

		TokenStream newStream = fragmentScorer.init(tokenStream);
		if (newStream != null) {
			tokenStream = newStream;
		}
		fragmentScorer.startFragment(currentFrag);
		docFrags.add(currentFrag);
		HashMap<String, ArrayList<WordPosition>> positions = new HashMap<>();
		try {

			String tokenText;
			int startOffset;
			int endOffset;
			int lastEndOffset = 0;
			textFragmenter.start(text, tokenStream);

			TokenGroup tokenGroup = new TokenGroup(tokenStream);

			tokenStream.reset();
			for (boolean next = tokenStream.incrementToken(); next
					&& (offsetAtt.startOffset() < maxDocCharsToAnalyze); next = tokenStream.incrementToken()) {
				if ((offsetAtt.endOffset() > text.length()) || (offsetAtt.startOffset() > text.length())) {
					throw new InvalidTokenOffsetsException(
							"Token " + termAtt.toString() + " exceeds length of provided text sized " + text.length());
				}
				if ((tokenGroup.getNumTokens() > 0) && (tokenGroup.isDistinct())) {
					// the current token is distinct from previous tokens -
					// markup the cached token group info
					startOffset = tokenGroup.getStartOffset();
					endOffset = tokenGroup.getEndOffset();
					tokenText = text.substring(startOffset, endOffset);
					if (tokenGroup.getTotalScore() > 0) {
						String word = getTerm(tokenText);
						WordPosition newPos = new WordPosition(startOffset, endOffset);
						if (positions.containsKey(word)) {
							ArrayList<WordPosition> wordPositions = positions.get(word);
							wordPositions.add(newPos);
							positions.put(word, wordPositions);
						} else {
							ArrayList<WordPosition> wordPositions = new ArrayList<>();
							wordPositions.add(newPos);
							positions.put(word, wordPositions);
						}

					}

					// store any whitespace etc from between this and last group
					if (startOffset > lastEndOffset)
						newText.append(encoder.encodeText(text.substring(lastEndOffset, startOffset)));
					// newText.append(markedUpText);
					lastEndOffset = Math.max(endOffset, lastEndOffset);
					tokenGroup.clear();

				}

				tokenGroup.addToken(fragmentScorer.getTokenScore());

			}

			// the current token is distinct from previous tokens -
			// markup the cached token group info
			startOffset = tokenGroup.getStartOffset();
			endOffset = tokenGroup.getEndOffset();
			tokenText = text.substring(startOffset, endOffset);
			if (tokenGroup.getTotalScore() > 0) {
				String word = getTerm(tokenText);
				WordPosition newPos = new WordPosition(startOffset, endOffset);
				if (positions.containsKey(word)) {
					ArrayList<WordPosition> wordPositions = positions.get(word);
					wordPositions.add(newPos);
					positions.put(word, wordPositions);
				} else {
					ArrayList<WordPosition> wordPositions = new ArrayList<>();
					wordPositions.add(newPos);
					positions.put(word, wordPositions);
				}

			}
//			positions.forEach((k, v) -> System.out.println(k + " " + v));
			HashMap<String, WordPosition> bestPositions = getBestPositions(positions);
			return getFragment(bestPositions, text);

		} finally {
			if (tokenStream != null) {
				try {
					tokenStream.end();
					tokenStream.close();
				} catch (Exception e) {
				}
			}
		}
	}

	private HashMap<String, WordPosition> getBestPositions(HashMap<String, ArrayList<WordPosition>> positions) {
		HashMap<String, WordPosition> result = new HashMap<>();
		int[][] m = new int[positions.keySet().size()][];
		int i = -1;
		for (String key : positions.keySet()) {
			ArrayList<WordPosition> pos = positions.get(key);
			int n = pos.size();
			m[++i] = new int[n];
			for (int j = 0; j < n; j++) {
				m[i][j] = pos.get(j).start;
			}
		}
		int pos[] = new int[m.length];
		int best[] = new int[m.length];
		int actual[] = new int[m.length];
		for (i = 0; i < best.length; i++) {
			best[i] = m[i][0];
			actual[i] = m[i][0];
		}

		int bestDistance = calculateDistance(best);
		while (hasNext(pos, m)) {
			{
				i = minPos(actual);
				pos[i] = pos[i] + 1;
				if (pos[i] < m[i].length) {
					int newValue = m[i][pos[i]];
					actual[i] = newValue;
					int dist = calculateDistance(actual);
					if (dist < bestDistance) {
						bestDistance = dist;
						best = actual.clone();
					}
//					System.out.println(Arrays.toString(actual));
//					System.out.println(Arrays.toString(pos));
				}
			}
		}
		i = 0;
		for (String key : positions.keySet()) {
			ArrayList<WordPosition> posistionList = positions.get(key);
			result.put(key, find(posistionList, best[i++]));
		}
		return result;
	}

	private int minPos(int[] v) {
		int minPos = 0;
		for (int i = 0; i < v.length; i++) {
			if (v[i] < v[minPos]) {
				minPos = i;
			}
		}
		return minPos;
	}

	private WordPosition find(ArrayList<WordPosition> posistionList, Integer start) {
		for (WordPosition e : posistionList) {
			if (e.start.equals(start)) {
				return e;
			}
		}
		return null;
	}

	private boolean hasNext(int pos[], int m[][]) {
		for (int i = 0; i < m.length; i++) {
			if (pos[i] == m[i].length) {
				return false;
			}
		}
		return true;
	}

	private Integer calculateDistance(int[] best) {
		int[] v = best.clone();
		Arrays.sort(v);
		Integer distance = 0;
		for (int i = 0; i < v.length - 1; i++)
			distance += Math.abs(v[i] - v[i + 1]);
		return distance;
	}

	private String getTerm(String word) throws IOException {
		RomanianAnalyzer romAnalyzer = new RomanianAnalyzer();
		TokenStream stream = romAnalyzer.tokenStream(Constants.CONTENT, word);
		stream.reset();
		stream.incrementToken();
		romAnalyzer.close();
		return new String(stream.getAttribute(TermToBytesRefAttribute.class).getBytesRef().bytes);

	}

	public String getFragment(HashMap<String, WordPosition> positions, String text) {
		String result = "";
		positions = sortByValues(positions);
		String[] keys = new String[positions.values().size()];
		positions.keySet().toArray(keys);
		int N = 30;
		int lastPos = -1;
		for (int i = 0; i < keys.length; i++) {
			String key = keys[i];
			int start = Math.max(0, positions.get(key).start - N);
			start = Math.max(start, lastPos);
			int finish = Math.min(positions.get(key).start + N, text.length());

			if (i < keys.length - 1) {
				finish = Math.min(finish, positions.get(keys[i + 1]).start);
			}

			lastPos = finish;
			int startWord = positions.get(key).start;
			int endWord = positions.get(key).finish;
			int beforeStartWord = startWord;
			int beforeEndWord = endWord;
			if (startWord != 0 && start != beforeStartWord) {
				beforeStartWord--;
			}
			if (beforeEndWord != text.length()) {
				beforeEndWord++;
			}
			result += text.substring(start, beforeStartWord) + " <b>" + text.substring(startWord, endWord) + "</b> "
					+ text.substring(beforeEndWord, finish);

		}
		return result;
	}

	private HashMap<String, WordPosition> sortByValues(HashMap<String, WordPosition> map) {
		List<String> mapKeys = new ArrayList<>(map.keySet());
		List<WordPosition> mapValues = new ArrayList<>(map.values());
		Collections.sort(mapValues, new Comparator<WordPosition>() {
			@Override
			public int compare(WordPosition o1, WordPosition o2) {
				return o1.start.compareTo(o2.start);
			}

		});
		Collections.sort(mapKeys);

		LinkedHashMap<String, WordPosition> sortedMap = new LinkedHashMap<>();

		Iterator<WordPosition> valueIt = mapValues.iterator();
		while (valueIt.hasNext()) {
			WordPosition val = valueIt.next();
			Iterator<String> keyIt = mapKeys.iterator();

			while (keyIt.hasNext()) {
				String key = keyIt.next();
				WordPosition comp1 = map.get(key);
				WordPosition comp2 = val;

				if (comp1.equals(comp2)) {
					keyIt.remove();
					sortedMap.put(key, val);
					break;
				}
			}
		}
		return sortedMap;
	}

	public int getMaxDocCharsToAnalyze() {
		return maxDocCharsToAnalyze;
	}

	public void setMaxDocCharsToAnalyze(int maxDocCharsToAnalyze) {
		this.maxDocCharsToAnalyze = maxDocCharsToAnalyze;
	}

	public Fragmenter getTextFragmenter() {
		return textFragmenter;
	}

	public void setTextFragmenter(Fragmenter fragmenter) {
		textFragmenter = Objects.requireNonNull(fragmenter);
	}

	/**
	 * @return Object used to score each text fragment
	 */
	public Scorer getFragmentScorer() {
		return fragmentScorer;
	}

	public void setFragmentScorer(Scorer scorer) {
		fragmentScorer = Objects.requireNonNull(scorer);
	}

	public Encoder getEncoder() {
		return encoder;
	}

	public void setEncoder(Encoder encoder) {
		this.encoder = Objects.requireNonNull(encoder);
	}

	static class FragmentQueue extends PriorityQueue<TextFragment> {
		FragmentQueue(int size) {
			super(size);
		}

		@Override
		public final boolean lessThan(TextFragment fragA, TextFragment fragB) {
			if (fragA.getScore() == fragB.getScore())
				return fragA.fragNum > fragB.fragNum;
			else
				return fragA.getScore() < fragB.getScore();
		}
	}
}
