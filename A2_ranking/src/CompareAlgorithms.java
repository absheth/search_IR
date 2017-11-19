import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.flexible.standard.QueryParserUtil;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;

public class CompareAlgorithms {
	/**
	 * Array containing all the tags to be indexed.
	 */
	private static final String[] tagsArray = { "num", "title", "desc" };
	
	/**
	 * Array containing all the similarity classes.
	 */
	private static final String[] similarityClasses = { "ClassicSimilarity", "BM25Similarity", "LMDirichletSimilarity", "LMJelinekMercerSimilarity" };
	/**
	 * Method to breakdown string as per the tag.
	 * 
	 * 
	 */
	private static List<String> extractDocuments(final String str) {
		Pattern tagRegex = null;
		final List<String> tagValues = new ArrayList<String>();
		tagRegex = Pattern.compile("<top>(.+?)</top>");
		final Matcher matcher = tagRegex.matcher(str);

		while (matcher.find()) {
			tagValues.add(matcher.group(1));
		}
		return tagValues;
	}

	private static List<String> getTagValues(final String str) {
		Pattern tagRegex = null;
		final List<String> tagValues = new ArrayList<String>();
		for (int i = 0; i < tagsArray.length; i++) {
			tagRegex = Pattern.compile("<" + tagsArray[i] + ">"+(tagsArray[i] == "num"?" Number: ":"")+"(.+?)<");
			final Matcher matcher = tagRegex.matcher(str);
			while (matcher.find()) {
				String[] stringArray = matcher.group(1).split(":");
				if (matcher.group(1).indexOf(":") != -1) {
					tagValues.add(stringArray[1].trim());
				} else {
					tagValues.add(matcher.group(1).trim());
				}
			}
		}
		//System.out.println("tagValues --> " + tagValues);
		return tagValues;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String indexPath = "/Users/absheth/course_assingments/search/assingment2/index";
		String filePath = "/Users/absheth/course_assingments/search/assingment2/topics.51-100";
		//String filePath = "/Users/absheth/course_assingments/search/assingment2/test_doc.txt";
		try {
			
			BufferedReader bufferedReader = null;
			IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
			IndexSearcher searcher = new IndexSearcher(reader);
			
			List<String> parseDocList = null;
			List<String> parseTagsList = null;
			String line = null;
			StringBuilder everything = null;

			bufferedReader = new BufferedReader(new FileReader(filePath));
			everything = new StringBuilder();
			
			while ((line = bufferedReader.readLine()) != null) {
				everything.append(line);
			}
			bufferedReader.close();
			
			
			for ( int x = 0; x < similarityClasses.length; x++) {
				
				parseDocList = extractDocuments(everything.toString());
				Date date1 = new Date();
				System.out.println("Time --> " + similarityClasses[x] + " START --> " + date1.toString());
				for (int i = 0; i < parseDocList.size(); i++) {
					parseTagsList = getTagValues(parseDocList.get(i));
					for (int j = 1; j < parseTagsList.size(); j++) {
						//System.out.println((j == 1 ? "Short Query --> " : "Long Query --> ") + parseTagsList.get(j));
						
						Analyzer analyzer = new StandardAnalyzer();
						QueryParser parser = new QueryParser("TEXT", analyzer);
						Query query = parser.parse(QueryParserUtil.escape(parseTagsList.get(j)));
						Set<Term> queryTerms = new LinkedHashSet<Term>();
						searcher.createNormalizedWeight(query, false).extractTerms(queryTerms);
						/*for (Term t : queryTerms) {
							System.out.println((j == 1 ? "Short " : "Long " + "Query --> ") + t.text());
						}*/
						switch (x) {
						case 0:
							searcher.setSimilarity(new ClassicSimilarity());
							break;
						case 1:
							searcher.setSimilarity(new BM25Similarity());
							break;
						case 2:
							searcher.setSimilarity(new LMDirichletSimilarity());
							break;
						case 3:
							searcher.setSimilarity(new LMJelinekMercerSimilarity(0.7f));
							break;
						}
						
						TopDocs topDocs = searcher.search(query, 1000);
						ScoreDoc[] scoreDocs = topDocs.scoreDocs;
						for (int k = 0; k < scoreDocs.length; k++) {
							Document doc = searcher.doc(scoreDocs[k].doc);
							
							StringBuilder short_query_builder = new StringBuilder();
							StringBuilder long_query_builder = new StringBuilder();
							
							BufferedWriter short_buffer, long_buffer = null;
							FileWriter short_query_writer, long_query_writer = null;
							
							File short_query_file = new File("/Users/absheth/course_assingments/search/assingment2/"+similarityClasses[x]+"_short_query.txt");
							File long_query_file = new File ("/Users/absheth/course_assingments/search/assingment2/"+similarityClasses[x]+"_long_query.txt");
							
							if (!short_query_file.exists()) {
								short_query_file.createNewFile();
							}
							if (!long_query_file.exists()) {
								long_query_file.createNewFile();
							}
	
							short_query_writer = new FileWriter(short_query_file.getAbsoluteFile(), true);
							long_query_writer = new FileWriter(long_query_file.getAbsoluteFile(), true);
							short_buffer = new BufferedWriter(short_query_writer);
							long_buffer = new BufferedWriter(long_query_writer);
							if (j == 1){
								short_query_builder.append(parseTagsList.get(0));
								short_query_builder.append('\t');
								short_query_builder.append(0);
								short_query_builder.append('\t');
								short_query_builder.append(doc.get("DOCNO"));
								short_query_builder.append('\t');
								short_query_builder.append(k+1);
								short_query_builder.append('\t');
								short_query_builder.append((double)scoreDocs[k].score);
								short_query_builder.append('\t');
								short_query_builder.append(similarityClasses[x]);
								short_query_builder.append('\n');
								
							} else {
								long_query_builder.append(parseTagsList.get(0));
								long_query_builder.append('\t');
								long_query_builder.append(0);
								long_query_builder.append('\t');
								long_query_builder.append(doc.get("DOCNO"));
								long_query_builder.append('\t');
								long_query_builder.append(k+1);
								long_query_builder.append('\t');
								long_query_builder.append((double)scoreDocs[k].score);
								long_query_builder.append('\t');
								long_query_builder.append(similarityClasses[x]);
								long_query_builder.append('\n');
							}
						
					
							short_query_writer.write(short_query_builder.toString());
							long_query_writer.write(long_query_builder.toString());
							try {
	
								if (short_buffer != null)
									short_buffer.close();
								if (short_query_writer != null)
									short_query_writer.close();
								
								
								if (long_buffer != null)
									long_buffer.close();
								if (long_query_writer != null)
									long_query_writer.close();
	
							} catch (IOException ex) {
	
								ex.printStackTrace();
	
							}
						}
						
					}
				}
			}
			reader.close();
			Date date2 = new Date();
			System.out.println("Time --> Program END --> " + date2.toString());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
