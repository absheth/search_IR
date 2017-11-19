/* Name: Akash Sheth
 * 
 * 
 * References: 
 * 1. https://stackoverflow.com/questions/30073980/java-writing-strings-to-a-csv-file
 * 
 * */

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Paths;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

public class IndexComparison {
	private static final String[] indexFolder = { "StandardAnalyzer", "SimpleAnalyzer", "StopAnalyzer",
			"KeywordAnalyzer" };

	public static void main(String[] args) {
		String indexPath = "/Users/absheth/search_indexes/";
		String vocabPath = "/Users/absheth/Z534/Assignment1/search_vocabs/";
		try {
			for (int x = 0; x < indexFolder.length; x++) {
				System.out.println("For " + indexFolder[x] + ": ");
				IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath + indexFolder[x])));

				// Print the total number of documents in the corpus
				System.out.println("Total number of documents in the corpus: " + reader.maxDoc());

				// Print the number of documents containing the term "new" in
				// <field>TEXT</field>.
				System.out.println("Number of documents containing the term \"new\" for field \"TEXT\": "
						+ reader.docFreq(new Term("TEXT", "new")));

				// Print the total number of occurrences of the term "new" across all documents
				// for <field>TEXT</field>.
				System.out.println("Number of occurrences of \"new\" in the field \"TEXT\": "
						+ reader.totalTermFreq(new Term("TEXT", "new")));

				// Print the number of documents containing the term "the" in
				// <field>TEXT</field>.
				System.out.println("Number of documents containing the term \"that\" for field \"TEXT\": "
						+ reader.docFreq(new Term("TEXT", "that")));

				// Print the total number of occurrences of the term "the" across all documents
				// for <field>TEXT</field>.
				System.out.println("Number of occurrences of \"that\" in the field \"TEXT\": "
						+ reader.totalTermFreq(new Term("TEXT", "that")));

				Terms vocabulary = MultiFields.getTerms(reader, "TEXT");

				// Print the size of the vocabulary for <field>TEXT</field>, applicable when the
				// index has only one segment.
				System.out.println("Size of the vocabulary for this field: " + vocabulary.size());

				// Print the total number of documents that have at least one term for
				// <field>TEXT</field>
				System.out.println(
						"Number of documents that have at least one term for this field: " + vocabulary.getDocCount());

				// Print the total number of tokens for <field>TEXT</field>
				System.out.println("Number of tokens for this field: " + vocabulary.getSumTotalTermFreq());

				// Print the total number of postings for <field>TEXT</field>
				System.out.println("Number of postings for this field: " + vocabulary.getSumDocFreq());

				// Print the vocabulary for <field>TEXT</field>
				TermsEnum iterator = vocabulary.iterator();
				BytesRef byteRef = null;
				
				/*
				 * Writing vocab words to a .csv file.
				 * 
				 * Reference - 1
				 * */ 
				PrintWriter pw = new PrintWriter(new File(vocabPath + indexFolder[x] + "_vocab.csv"));
				StringBuilder sb = new StringBuilder();

				int counter = 0;

				int word_count = 0;
				while ((byteRef = iterator.next()) != null) {

					counter++;
					// generating 1000 vocab words for each analyzer
					if (word_count < 1000) {
						sb.append(byteRef.utf8ToString());
						sb.append('\t');

						// 50 words per line
						if (counter == 50) {
							sb.append('\n');
							counter = 0;
						}

					}

					word_count++;

				}

				pw.write(sb.toString());
				pw.close();
				System.out.println("Total words/TERMS in the dictionary: " + word_count);
				System.out.println("done!");
				reader.close();
				System.out.println("------------------------------------");
				System.out.println();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
