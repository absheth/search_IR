
/* Name: Akash Sheth
 * 
 * 
 * References: 
 * 1. https://stackoverflow.com/questions/6560672/java-regex-to-extract-text-between-tags
 * 
 * 
 * */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class GenerateIndex {

	/**
	 * Array containing all the tags to be indexed.
	 */
	private static final String[] tagsArray = { "DOC", "DOCNO", "HEAD", "BYLINE", "DATELINE", "TEXT" };

	/**
	 * Array containing Analyzer specific folder names.
	 */
	private static final String[] indexFolder = { "StandardAnalyzer", "SimpleAnalyzer", "StopAnalyzer",
			"KeywordAnalyzer" };

	/**
	 * Method to breakdown string as per the tag.
	 * 
	 * Reference - 1
	 */
	private static List<String> getTagValues(final String str, String tag) {

		Pattern tagRegex = Pattern.compile("<" + tag + ">(.+?)</" + tag + ">");
		final List<String> tagValues = new ArrayList<String>();
		final Matcher matcher = tagRegex.matcher(str);
		while (matcher.find()) {
			tagValues.add(matcher.group(1));
		}
		return tagValues;
	}

	/**
	 * Main method
	 */
	public static void main(String[] args) throws ParserConfigurationException {
		String indexPath = "/Users/absheth/search_indexes/";
		String folderPath = "/Users/absheth/AB_Drive/masters/course/search/assignments/corpus/";
		// String folderPath = "/Users/absheth/Desktop/";
		BufferedReader bufferedReader = null;
		Directory dir = null;
		Analyzer analyzer = null;
		IndexWriterConfig iwc = null;
		IndexWriter writer = null;
		String line = null;
		Document lDoc = null;
		StringBuilder everything = null;
		List<String> parseDocList = null;
		List<String> parseTagsList = null;
		File folder = null;
		File[] listOfFiles = null;
		try {
			// Iterating over the analyzers.
			for (int x = 0; x < indexFolder.length; x++) {
				/* Lucene Code - Provided in the assignment */

				dir = FSDirectory.open(Paths.get(indexPath + indexFolder[x]));

				// Swtich case to select the analyzer.
				switch (x) {
				case 0:
					analyzer = new StandardAnalyzer();
					break;
				case 1:
					analyzer = new SimpleAnalyzer();
					break;
				case 2:
					analyzer = new StopAnalyzer();
					break;
				case 3:
					analyzer = new KeywordAnalyzer();
					break;
				}

				iwc = new IndexWriterConfig(analyzer);
				iwc.setOpenMode(OpenMode.CREATE);
				writer = new IndexWriter(dir, iwc);
				lDoc = new Document();
				/* Lucene Code - Provided in the assignment */

				parseDocList = new ArrayList<String>(); // List for elements of the tag <DOC>
				parseTagsList = new ArrayList<String>(); // List for handling elements of individual tags in a document.

				// Opening the folder that contains all the trectext files.
				folder = new File(folderPath);
				listOfFiles = folder.listFiles(); // Getting list of all the files.

				for (File file : listOfFiles) {
					if (file.isFile()) {
						everything = new StringBuilder();

						// Opening a file
						bufferedReader = new BufferedReader(new FileReader(folderPath + file.getName()));

						// Read contents from the file line by line.
						while ((line = bufferedReader.readLine()) != null) {
							everything.append(line);
						}

						// Breaking down the document as per the tag <DOC>.
						parseDocList = getTagValues(everything.toString(), tagsArray[0]);

						Date date1 = new Date();
						System.out.println(
								indexFolder[x] + " --> " + file.getName() + " --> Time --> " + date1.toString());

						for (int i = 0; i < parseDocList.size(); i++) {

							lDoc.clear();

							for (int j = 1; j < tagsArray.length; j++) {

								// Breaking down all the tags of a particular doc.
								parseTagsList = getTagValues(parseDocList.get(i), tagsArray[j]);

								// Breaking down all the tags of a particular doc.
								for (int k = 0; k < parseTagsList.size(); k++) {

									// If DOCNO, save it as StringField else as TextField.
									if (tagsArray[j].equals("DOCNO")) {
										lDoc.add(new StringField(tagsArray[j], parseTagsList.get(k).trim(),
												Field.Store.YES));
									} else {
										lDoc.add(new TextField(tagsArray[j], parseTagsList.get(k).trim(),
												Field.Store.YES));
									}
								}
							}
							writer.addDocument(lDoc);
						}
						bufferedReader.close();
					}
				}
				writer.forceMerge(1);
				writer.commit();
				writer.close();
				System.out.println("Done for " + indexFolder[x] + ".");
				System.out.println("-----------------------------------------");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
