package ir.indexer;

import java.util.List;
import java.util.Scanner;

public class RunSearcherKeyboard {
	public static void main(String[] args) throws Exception {

		Searcher searcher = new Searcher();
		Scanner sc = new Scanner(System.in);
		System.out.println("Search for the words  : ");
		String words = sc.nextLine();
		List<String> paths = searcher.search(words, true);
		if (paths.size() == 0) {
			System.out.printf("We could not find any docs to match your query:  %s.", words);
		} else {
			System.out.printf("We've found %d docs that match your query:  %s.\n", paths.size(), words);
			paths.forEach(a -> System.out.println(a));
		}
		sc.close();
	}
}
