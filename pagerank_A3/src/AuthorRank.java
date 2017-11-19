
/* Author: Akash B. Sheth
 * 
 * 
 * **/
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import edu.uci.ics.jung.algorithms.scoring.PageRank;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class AuthorRank {

	private static double tolerance = 0.00000001;
	private static double alpha = 0.15;
	public static void main(String[] args) {
		String graphFile = "/Users/absheth/course_assingments/search/assignment3/author.net";
		try {

			DirectedSparseGraph<String, Integer> graph = new DirectedSparseGraph<String, Integer>();
			populateGraph(graphFile, graph);
			
			PageRank<String, Integer> rank = new PageRank<String, Integer>(graph, AuthorRank.alpha);
			rank.setTolerance(AuthorRank.tolerance); 
			rank.setMaxIterations(30);
			rank.evaluate();
			Map<String, Double> result = new HashMap<String, Double>();
			for (String v : graph.getVertices()) { 
				   result.put(v, rank.getVertexScore(v)); 
			}
			
			Map<Object, Object> sortedMap = result.entrySet().stream()
					.sorted(Map.Entry.comparingByValue(Collections.reverseOrder())).collect(Collectors
							.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
			
			System.out.println("Alpha = " + AuthorRank.alpha);
			System.out.println("--------------------");
			int count = 1;
			for (Object key : sortedMap.keySet()) {
				double value = (double) sortedMap.get(key);
				if (count > 10) {
					break;
				}
				System.out.println("Rank: " + count + " | " + "Score: " + value + " | AuthorId: " + key);
				count++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private static void populateGraph(String p_graphFile, DirectedSparseGraph<String, Integer> p_graph) {
		BufferedReader bufferedReader;
		String line = "";
		String splitArray[];
		String sourceNode = null;
		String destinationNode = null;
		try {
			int edge_count = 0;
			bufferedReader = new BufferedReader(new FileReader(p_graphFile));
			HashSet<String> added = new HashSet<String>();
			Map<String, String> authorMappings = new HashMap<String, String>();
			while ((line = bufferedReader.readLine()) != null) {
				if (line.indexOf("*") != -1) {
					continue;
				}
				if((splitArray = line.split(" ")).length == 2) { 
					authorMappings.put(splitArray[0], splitArray[1].substring(1, splitArray[1].length()-1));
					if(!p_graph.addVertex(authorMappings.get(splitArray[0]))) {
						System.out.println("Vertex Failed --> " + sourceNode);
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
