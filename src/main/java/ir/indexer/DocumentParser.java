package ir.indexer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Nullable;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;

public class DocumentParser {

	private boolean pdf;

	private boolean txt;

	private boolean doc;

	public String getContent(Path path) throws Exception {
		String result = null;
		String extension = getExtension(path.getFileName().toString());
		if (extension == null || extension.length() == 0) {
			throw new Exception("We could not find the file extension!");

		}
		result = getContent(path, extension);
		if (result == null) {
			throw new Exception("We could not extract the content for the file " + path.getFileName());
		}
		return result;
	}

	public boolean isPdf() {
		return pdf;
	}

	public void setPdf(boolean pdf) {
		this.pdf = pdf;
	}

	public boolean isTxt() {
		return txt;
	}

	public void setTxt(boolean txt) {
		this.txt = txt;
	}

	public boolean isDoc() {
		return doc;
	}

	public void setDoc(boolean doc) {
		this.doc = doc;
	} 
	private String getExtension(String filename) {
		if (filename == null) {
			return null;
		}
		if (filename.lastIndexOf('.') == -1) {
			return null;

		} else {
			return filename.substring(filename.lastIndexOf('.')).toLowerCase();
		}
	}

	private String getContent(Path file,@Nullable String extension) {
		String result = null;
		switch (extension) {
		case Constants.PDF:
			pdf = true;
			result = getPdfContent(file);
			break;
		case Constants.DOC:
			doc = true;
			result = getDocContent(file);
			break;
		case Constants.DOCX:
			doc = true;
			result = getDocxContent(file);
			break;
		case Constants.TXT:
			txt = true;
			result = getTxtContent(file);
			break;
		default:
			return null;
		}
		return result;
	}

	private String getPdfContent(Path pdfFile) {
		try {
			InputStream in = Files.newInputStream(pdfFile);
			PdfReader reader = new PdfReader(in);
			StringBuffer sb = new StringBuffer();
			PdfReaderContentParser parser = new PdfReaderContentParser(reader);
			TextExtractionStrategy strategy;
			for (int i = 1; i <= reader.getNumberOfPages(); i++) {
				strategy = parser.processContent(i, new SimpleTextExtractionStrategy());
				sb.append(strategy.getResultantText());
			}
			reader.close();
			in.close();
			return sb.toString();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private String getTxtContent(Path txtFile) {
		try {
			return new String(Files.readAllBytes(txtFile));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private String getDocContent(Path docFile) {
		try {
			InputStream in = Files.newInputStream(docFile);
			HWPFDocument doc = new HWPFDocument(in);
			WordExtractor we = new WordExtractor(doc);
			String[] paragraphs = we.getParagraphText();
			StringBuffer sb = new StringBuffer();
			for (String para : paragraphs) {
				sb.append(para);
				sb.append(" ");
			}
			we.close();
			in.close();
			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private String getDocxContent(Path docFile) {
		try {
			InputStream in = Files.newInputStream(docFile);
			XWPFDocument docx = new XWPFDocument(in);
			XWPFWordExtractor we = new XWPFWordExtractor(docx);
			in.close();
			String result = we.getText();
			we.close();
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
