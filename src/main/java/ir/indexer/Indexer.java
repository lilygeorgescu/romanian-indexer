package ir.indexer;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.WordlistLoader;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.IOUtils;

public class Indexer {

	private final DocumentParser documentParser;

	private boolean create;

	private Directory dir;

	private Analyzer analyzer;

	public Indexer() {
		documentParser = new DocumentParser();
		List<String> romanianStopWords = new ArrayList<String>();
		Reader reader = null;
		CharArraySet defaultRomanianStopWords = null;
		romanianStopWords = new ArrayList<String>();
		try {
			dir = FSDirectory.open(Paths.get(Constants.INDEX_PATH));
			reader = IOUtils.getDecodingReader(RomanianAnalyzer.class.getResourceAsStream("stopwords.txt"),
					StandardCharsets.UTF_8);
			defaultRomanianStopWords = WordlistLoader.getWordSet(reader, "#", new CharArraySet(16, false));
			// delete diacritice
			String stopwords[] = defaultRomanianStopWords.toString().replace('[', ' ').replace(']', ' ').split(",");

			for (String stopword : stopwords) {
				romanianStopWords.add(RomanianChanges.change(stopword.trim()));
			}
			CharArraySet newStopwords = new CharArraySet(romanianStopWords, true);
			analyzer = new RomanianAnalyzer(newStopwords);
		} catch (IOException e) {

			e.printStackTrace();
		} finally {
			try {
				IOUtils.close(reader);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public Indexer(Analyzer analyzer) {
		documentParser = new DocumentParser();
		this.analyzer = analyzer;
	}

	public void index(final Path docDir) {
		if (!Files.isReadable(docDir)) {
			System.out.println("Document directory '" + docDir.toAbsolutePath()
					+ "' does not exist or is not readable, please check the path");
			System.exit(1);
		}

		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

		if (create) {
			iwc.setOpenMode(OpenMode.CREATE);
		} else {
			iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
		}
		IndexWriter writer;
		try {
			writer = new IndexWriter(dir, iwc);
			indexDocs(writer, docDir);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void indexDocs(final IndexWriter writer, Path path) throws IOException {
		if (Files.isDirectory(path)) {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
					return FileVisitResult.CONTINUE;
				}
			});
		} else {
			indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
		}
	}

	private void indexDoc(IndexWriter writer, Path file, long lastModified) {
		try {
			Document doc = new Document();
			Field pathField = new StringField(Constants.PATH, file.toString(), Field.Store.YES);
			doc.add(pathField);
			doc.add(new LongPoint(Constants.MODIFIED, lastModified));
			FieldType txtFieldType = new FieldType(TextField.TYPE_STORED);
			txtFieldType.setStoreTermVectors(true);
			txtFieldType.setStoreTermVectorPositions(true);
			Field field = new Field(Constants.CONTENT, RomanianChanges.change(documentParser.getContent(file)),
					txtFieldType);
			doc.add(field);
			if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
				System.out.println("adding " + file);
				writer.addDocument(doc);
			} else {
				System.out.println("updating " + file);
				writer.updateDocument(new Term("path", file.toString()), doc);
			}
		} catch (Exception e) {
			System.out.println("Error for the doc " + file);
			e.printStackTrace();
		}
	}

	public boolean isCreate() {
		return create;
	}

	public void setCreate(boolean create) {
		this.create = create;
	}
}
