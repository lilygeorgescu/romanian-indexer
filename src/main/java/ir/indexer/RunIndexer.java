package ir.indexer;

import java.nio.file.Path;
import java.nio.file.Paths;

public class RunIndexer {
	public static void main(String[] args) throws Exception {
		Path path = Paths.get("D:\\master\\an1\\sem 2\\docs");
		Indexer indexer = new Indexer();
		indexer.setCreate(true);
		indexer.index(path);

	}
}
