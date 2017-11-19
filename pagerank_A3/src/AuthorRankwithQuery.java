/* Author: Akash B. Sheth
 * 
 * 
 * **/
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections15.Transformer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
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
import org.apache.lucene.store.FSDirectory;

import edu.uci.ics.jung.algorithms.scoring.PageRankWithPriors;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class AuthorRankwithQuery {
	
	private static String [] queryArray = {"Data Mining", "Information Retrieval"};
	private static double tolerance = 0.0000001;
	private static double alpha = 0.15;
	
	public static void main(String[] args) {
		Analyzer analyzer = null;
		QueryParser parser = null;
		Query query = null;
		Set<Term> queryTerms = null;
		IndexReader reader =  null;
		DirectedSparseGraph<String, Integer> author_graph;
		TopDocs topDocs = null;
		ScoreDoc[] scoreDocs= null;
		String graphFile = "/Users/absheth/course_assingments/search/assignment3/author.net";
		String indexPath = "/Users/absheth/course_assingments/search/assignment3/author_index";
		Map<String, Double> authorPrior = new HashMap<String, Double>();
		Map<Object, Object> authorInfo = new HashMap<Object, Object>();
		
		try {
			reader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
			author_graph = new DirectedSparseGraph<String, Integer>();
			
			populateGraph(graphFile, author_graph);
			IndexSearcher searcher = new IndexSearcher(reader);
			for (int i = 0; i < queryArray.length; i++) {
				analyzer = new StandardAnalyzer();
				parser = new QueryParser("content", analyzer);
				query = parser.parse(QueryParserUtil.escape(queryArray[i]));
				queryTerms = new LinkedHashSet<Term>();
				query.extractTerms(queryTerms);
				searcher.createNormalizedWeight(query);
				searcher.setSimilarity(new BM25Similarity());
				topDocs = searcher.search(query, 300);
				scoreDocs = topDocs.scoreDocs;
				double totalScore = 0.0;
				
				for (int k = 0; k < scoreDocs.length; k++) {
					String authorId = searcher.doc(scoreDocs[k].doc).get("authorid");
					authorInfo.put(authorId, authorInfo.containsKey(authorId) ? authorInfo.get(authorId) : searcher.doc(scoreDocs[k].doc).get("authorName"));
					authorPrior.put(authorId,  authorPrior.containsKey(authorId) ? (double)authorPrior.get(authorId) + scoreDocs[k].score : scoreDocs[k].score);
					totalScore += scoreDocs[k].score;
				}

				for(String id : authorPrior.keySet()) {
					authorPrior.put(id, authorPrior.get(id)/totalScore);
				}

				Transformer<String, Double> vertexPriors = new Transformer<String, Double>() {
					@Override
					public Double transform(String authorId) {
						if (authorPrior.containsKey(authorId)) {
							return (double) authorPrior.get(authorId);
						}
						return 0.0;
					}
				};
				PageRankWithPriors<String, Integer>  rank = new PageRankWithPriors<String, Integer>(author_graph, vertexPriors, AuthorRankwithQuery.alpha);
				rank.setMaxIterations(30);
			    rank.setTolerance(AuthorRankwithQuery.tolerance);
			    rank.evaluate();
			    
			    Map<String, Double> result = new HashMap<String, Double>();
				for (String v : author_graph.getVertices()) { 
					   result.put(v, rank.getVertexScore(v)); 
				}
				Map<Object, Object> sortedMap = result.entrySet().stream()
						.sorted(Map.Entry.comparingByValue(Collections.reverseOrder())).collect(Collectors
								.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
				
				
				int count = 1;
				System.out.println("For query --> " + queryArray[i] + " | Alpha = " + AuthorRankwithQuery.alpha);
				System.out.println("-----------------------------");
				for (Object key : sortedMap.keySet()) {
					if (count > 10) {
						break;
					}
					double value = (double) sortedMap.get(key);
					// System.out.println("Rank: " + count + " | " + "Score: " + value + " | AuthorId: " + key + " | Name: "+ authorInfo.get(key));
					System.out.println("Rank: " + count + " | " + "Score: " + value + " | AuthorId: " + key);
					count++;
				}
				System.out.println("*****************************");
				System.out.println();
				
				authorInfo.clear();
				authorPrior.clear();
			}
		} catch (Exception  e) {
			e.printStackTrace();
		}
		
	}
	
	private static void populateGraph(String p_graphFile, DirectedSparseGraph<String, Integer> p_graph) {
		BufferedReader bufferedReader;
		String line = "";
		String splitArray[];
		String sourceNode = null;
		String destinationNode = null;
		HashSet<String> added = new HashSet<String>() ;
		Map<String, String> authorMappings = new HashMap<String, String>();
		try {
			int edge_count = 0;
			bufferedReader = new BufferedReader(new FileReader(p_graphFile));
			
			while ((line = bufferedReader.readLine()) != null) {
				if (line.indexOf("*") != -1) {
					continue;
				}
				if((splitArray = line.split(" ")).length == 2) { 
					authorMappings.put(splitArray[0], splitArray[1].substring(1, splitArray[1].length()-1));
					if(! p_graph.addVertex(authorMappings.get(splitArray[0]))) {
						System.out.println("FAIL");
					}
				} else {
					sourceNode = splitArray[0];
					destinationNode = splitArray[1];
					if (!sourceNode.equals(destinationNode)) {
						if (!added.contains(sourceNode+"~"+destinationNode)) {
							p_graph.addEdge(new Integer(++edge_count), authorMappings.get(sourceNode), authorMappings.get(destinationNode));
							added.add(sourceNode+"~"+destinationNode);
						}
					}
				}
			}
			bufferedReader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
