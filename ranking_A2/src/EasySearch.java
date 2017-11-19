import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

public class EasySearch {

	public static void main(String[] args) {
		String indexPath = "/Users/absheth/course_assingments/search/assingment2/index";
		try {

			// Get the preprocessed query terms
			String queryString = "Document will discuss government assistance to Airbus Industrie, or mention trade dispute between Airbus and a U.S. aircraft producer over the issue ofsubsidies.";
			int documentWithTermFrequency = 0;
			Map<Object, Object> queryDocRelevance = new HashMap<Object, Object>();
			
			IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
			IndexSearcher searcher = new IndexSearcher(reader);
			// Get the preprocessed query terms
			Analyzer analyzer = new StandardAnalyzer();
			QueryParser parser = new QueryParser("TEXT", analyzer);
			Query query = parser.parse(queryString);
			Set<Term> queryTerms = new LinkedHashSet<Term>();
			searcher.createNormalizedWeight(query, false).extractTerms(queryTerms);
			//for (Term t : queryTerms) {
			//	System.out.println("Query --> " + t.text());
			//}
			
			ClassicSimilarity dSimi = new ClassicSimilarity();

			List<LeafReaderContext> leafContexts = reader.getContext().reader().leaves();
			double tfSum = 0;
			
				for (Term t : queryTerms) {
				
					documentWithTermFrequency = reader.docFreq(new Term("TEXT", t.text()));
					if(documentWithTermFrequency == 0) {
						continue;
					}
					double inverseDocumentFrequency = Math.log(1 + (reader.maxDoc() / documentWithTermFrequency));
					System.out.println("IDF for " + t.text() + " --> " + inverseDocumentFrequency);

					for (int i = 0; i < leafContexts.size(); i++) {

						LeafReaderContext leafContext = leafContexts.get(i);
						PostingsEnum de = MultiFields.getTermDocsEnum(leafContext.reader(), "TEXT", new BytesRef(t.text()));
						int startDocNo = leafContext.docBase;
						//int numberOfDoc = leafContext.reader().maxDoc();
					
						int docId;
						if (de != null) {
							while ((docId = de.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {

								docId = de.docID() + startDocNo;
								String docNo = searcher.doc(docId).get("DOCNO");
								//if (!docNo.equals("AP891126-0082")) {
								//	continue;
								//}
								float normDocLeng = dSimi.decodeNormValue(leafContext.reader().getNormValues("TEXT").get(de.docID()));
							
								float docLeng = 1 / (normDocLeng * normDocLeng);
								tfSum = de.freq() / docLeng;
								System.out.println("TF Score for " + t.text() + " --> " + tfSum);
								Object ab = queryDocRelevance.get(docNo);
								double tfIdf = ab != null ? (double) ab + (tfSum * inverseDocumentFrequency):(tfSum * inverseDocumentFrequency);
								queryDocRelevance.put(docNo, tfIdf);
							}
						}
					}
				}
				int count = 0;
				// Sorting the hashmap -- Reference:
				// https://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values-java
				Map<Object, Object> sortedMap = queryDocRelevance.entrySet().stream()
						.sorted(Map.Entry.comparingByValue(Collections.reverseOrder())).collect(Collectors
								.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
				// Sorting the hashmap -- Reference:
				// https://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values-java
				queryDocRelevance.clear();
				for (Object key : sortedMap.keySet()) {
					double value = (double) queryDocRelevance.get(key);
					//if (count > 2) {
					//	break;
					//}
					System.out.println(key + " --> " + value);
					count++;
				}
			
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
}
