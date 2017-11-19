import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.flexible.standard.QueryParserUtil;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

public class SearchTrecTopics {
	/**
	 * Array containing all the tags to be indexed.
	 */
	private static final String[] tagsArray = { "num", "title", "desc" };

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
			tagValues.add(matcher.group(1).trim());
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
				//System.out.println("matcher.group(1) --> " + matcher.group(1));
				String[] stringArray = matcher.group(1).trim().split(":");
				//System.out.println(Arrays.toString(stringArray));
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
		//String filePath = "/Users/absheth/course_assingments/search/assingment2/test_doc.txt";
		
		String filePath = "/Users/absheth/course_assingments/search/assingment2/topics.51-100";
		try {
			
			BufferedReader bufferedReader = null;
			int documentWithTermFrequency = 0;
			Map<Object, Object> queryDocRelevance = new HashMap<Object, Object>();
			IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
			IndexSearcher searcher = new IndexSearcher(reader);
			
			List<String> parseDocList = null;
			List<String> parseTagsList = null;
			String line = null;
			StringBuilder everything = null;

			double tfSum = 0;
			
			bufferedReader = new BufferedReader(new FileReader(filePath));
			everything = new StringBuilder();
			
			while ((line = bufferedReader.readLine()) != null) {
				everything.append(line.trim());
			}
			bufferedReader.close();
			
			parseDocList = extractDocuments(everything.toString());
			everything = null;
			Date date1 = new Date();
			System.out.println("Time --> " + date1.toString());
			for (int i = 0; i < parseDocList.size(); i++) {
				parseTagsList = getTagValues(parseDocList.get(i));
				for (int j = 1; j < parseTagsList.size(); j++) {
					//System.out.println(parseTagsList.get(j));
					
					Analyzer analyzer = new StandardAnalyzer();
					QueryParser parser = new QueryParser("TEXT", analyzer);
					Query query = parser.parse(QueryParserUtil.escape(parseTagsList.get(j)));
					Set<Term> queryTerms = new LinkedHashSet<Term>();
					searcher.createNormalizedWeight(query, false).extractTerms(queryTerms);
					//for (Term t : queryTerms) {
					//	System.out.println((j == 1 ? "Short " : "Long " + "Query --> ") + t.text());
					//}
					
					ClassicSimilarity dSimi = new ClassicSimilarity();

					List<LeafReaderContext> leafContexts = reader.getContext().reader().leaves();
					for (Term t : queryTerms) {
						documentWithTermFrequency = reader.docFreq(new Term("TEXT", t.text()));
						if(documentWithTermFrequency == 0) {
							continue;
						}
						double inverseDocumentFrequency = Math.log(1 + (reader.maxDoc() / documentWithTermFrequency));

						for (int k = 0; k < leafContexts.size(); k++) {

							LeafReaderContext leafContext = leafContexts.get(k);
							PostingsEnum de = MultiFields.getTermDocsEnum(leafContext.reader(), "TEXT", new BytesRef(t.text()));
							int startDocNo = leafContext.docBase;
							//int numberOfDoc = leafContext.reader().maxDoc();
							int docId;
							if (de != null) {
								while ((docId = de.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
									docId = de.docID() + startDocNo;
									String docNo = searcher.doc(docId).get("DOCNO");
									/* For debug*/
									//if (!docNo.equals("AP891126-0082")) {
									//	continue;
									//}
									float normDocLeng = dSimi.decodeNormValue(
											leafContext.reader().getNormValues("TEXT").get(de.docID()));
									float docLeng = 1 / (normDocLeng * normDocLeng);
									tfSum = de.freq() / docLeng;
									
									Object ab = queryDocRelevance.get(docNo);
									double tfIdf = ab != null ? (double) ab + (tfSum * inverseDocumentFrequency):(tfSum * inverseDocumentFrequency);
									queryDocRelevance.put(docNo, tfIdf);
								}
							}
						}
					}
					int count = 0;
					// Reference:
					// https://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values-java
					Map<Object, Object> sortedMap = queryDocRelevance.entrySet().stream()
							.sorted(Map.Entry.comparingByValue(Collections.reverseOrder())).collect(Collectors
									.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
					// Reference:
					// https://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values-java
					queryDocRelevance.clear();
					
					StringBuilder short_query_builder = new StringBuilder();
					StringBuilder long_query_builder = new StringBuilder();
					
					BufferedWriter short_buffer, long_buffer = null;
					FileWriter short_query_writer, long_query_writer = null;
					
					File short_query_file = new File("/Users/absheth/course_assingments/search/assingment2/EasySearch_short_query.txt");
					File long_query_file = new File ("/Users/absheth/course_assingments/search/assingment2/EasySearch_long_query.txt");
					
					if (!short_query_file.exists()) {
						short_query_file.createNewFile();
					}
					if (!long_query_file.exists()) {
						long_query_file.createNewFile();
					}

					// true = append file
					short_query_writer = new FileWriter(short_query_file.getAbsoluteFile(), true);
					long_query_writer = new FileWriter(long_query_file.getAbsoluteFile(), true);
					short_buffer = new BufferedWriter(short_query_writer);
					long_buffer = new BufferedWriter(long_query_writer);
					
					
					for (Object key : sortedMap.keySet()) {
						double value = (double) sortedMap.get(key);
						// System.out.println(key + " --> " + value );
						//querySum += value;
						count++;
						// generating 1000 vocab words for each analyzer
						if (count <= 1000) {
							if (j == 1){
								//System.out.println("SHORT QUERY --> " + value);
								short_query_builder.append(parseTagsList.get(0));
								short_query_builder.append('\t');
								short_query_builder.append(0);
								short_query_builder.append('\t');
								short_query_builder.append(key);
								short_query_builder.append('\t');
								short_query_builder.append(count);
								short_query_builder.append('\t');
								short_query_builder.append(value);
								short_query_builder.append('\t');
								short_query_builder.append("EasySearch");
								short_query_builder.append('\n');
								
							} else {
								//System.out.println("LONG QUERY --> " + value);
								long_query_builder.append(parseTagsList.get(0));
								long_query_builder.append('\t');
								long_query_builder.append(0);
								long_query_builder.append('\t');
								long_query_builder.append(key);
								long_query_builder.append('\t');
								long_query_builder.append(count);
								long_query_builder.append('\t');
								long_query_builder.append(value);
								long_query_builder.append('\t');
								long_query_builder.append("EasySearch");
								long_query_builder.append('\n');
								
							}
						}
						
					}
					short_query_writer.write(short_query_builder.toString());
					long_query_writer.write(long_query_builder.toString());
					
					//sortedMap = null;
					//queryDocRelevance.clear();
					//System.out.println("No of documents --> " + count + " || Total of all the relevances: " + querySum);
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
			
			Date date2 = new Date();
			System.out.println("Time --> " + date2.toString());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}
