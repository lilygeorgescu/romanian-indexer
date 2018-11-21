package ir.indexer;

public class RomanianChanges {

	public static String change(String inputText) {
		if (inputText != null) {
			return inputText.replace('ă', 'a').replace('â', 'a').replace('î', 'i').replace('ș', 's').replace('ț', 't')
					.replace('Ă', 'A').replace('Â', 'A').replace('Î', 'I').replace('Ș', 'S').replace('Ț', 'T')
					.replace('ş', 's').replace('ţ', 't').replace('Ș', 'S').replace('Ț', 'T');
		} else {
			return null;
		}
	}

}
