package pa1;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.jsoup.Jsoup;

import api.TaggedVertex;
import api.Util;

/**
 * Implementation of an inverted index for a web graph.
 * 
 * @author Lorenzo Zenitsky, Gabrielle Johnston
 */
public class Index {
	private List<TaggedVertex<String>> urls;
	private Map<String, Map<String, Integer>> invertedIndex;

	/**
	 * Constructs an index from the given list of urls. The tag value for each url
	 * is the indegree of the corresponding node in the graph to be indexed.
	 * 
	 * @param urls
	 *            information about graph to be indexed
	 */
	public Index(List<TaggedVertex<String>> urls) {
		this.urls = urls;
		this.invertedIndex = new HashMap<String, Map<String, Integer>>();
	}

	/**
	 * Parses the given body that was extracted from the given url
	 * using the jsoup library. A new word is only added to the inverted
	 * index if it is both not a stop word and is not already in the index.
	 * @param body
	 * @param url
	 */
	private void parseBody(String body, String url) {
		Scanner scanner = new Scanner(body);
		while (scanner.hasNext()) {
			String next = scanner.next();
			next = Util.stripPunctuation(next);
			if (!(Util.isStopWord(next))) {
				if(invertedIndex.containsKey(next)) {
					Map<String, Integer> list = invertedIndex.get(next);
					if(!list.containsKey(url)) {
						list.put(url, 1);
					}
					else {
						int rank = list.get(url);
						rank++;
						list.put(url, rank);
					}
				}
				else {
					Map<String, Integer> list = new HashMap<String, Integer>();
					list.put(url, 1);
					invertedIndex.put(next, list);
				}
			}
		}
		scanner.close();
	}

	/**
	 * Creates the index.
	 */
	public void makeIndex() {
		int requests = 0;
		for (TaggedVertex<String> tv : urls) {
			String url = tv.getVertexData();
			try {
				if(requests == 50) {
					try {
						Thread.sleep(3000);
						requests = 0;
					} catch (InterruptedException ignore) {
						ignore.printStackTrace();
					}
				}
				String body = Jsoup.connect(url).get().body().text();
				requests++;
				parseBody(body, url);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Returns the rank of the specified url stored in the 
	 * urls list.
	 * @param url
	 * @return rank of url, -1 if not found.
	 */
	private int getRankFromURLs(String url) {
		for(int i = 0; i < urls.size(); i++) {
			if(urls.get(i).getVertexData().equals(url)) {
				return urls.get(i).getTagValue();
			}
		}
		
		return -1;
	}

	/**
	 * Searches the index for pages containing keyword w. Returns a list of urls
	 * ordered by ranking (largest to smallest). The tag value associated with each
	 * url is its ranking. The ranking for a given page is the number of occurrences
	 * of the keyword multiplied by the indegree of its url in the associated graph.
	 * No pages with rank zero are included.
	 * 
	 * @param w
	 *            keyword to search for
	 * @return ranked list of urls
	 */
	public List<TaggedVertex<String>> search(String w) {
		List<TaggedVertex<String>> ranked = new ArrayList<TaggedVertex<String>>();
		if(!invertedIndex.containsKey(w)) {
			return ranked;
		}
		else {
			Map<String, Integer> list = invertedIndex.get(w);
			for(Map.Entry<String, Integer> entry : list.entrySet()) {
				String urlSource = entry.getKey();
				int freq = list.get(urlSource);
				int rank = freq * getRankFromURLs(urlSource);
				if(rank > 0) {
					TaggedVertex<String> tv = new TaggedVertex<String>(urlSource, rank);
					ranked.add(tv);
				}
			}
		}
		
		ranked.sort(new RankComparator());
		return ranked;
	}
	
	/**
	 * Returns an array list of data from given vertex list.
	 * @param searchList
	 * @return array of vertex data
	 */
	private ArrayList<String> getData(List<TaggedVertex<String>> searchList) {
		ArrayList<String> arr = new ArrayList<String>();
		for(TaggedVertex<String> tv : searchList) {
			arr.add(tv.getVertexData());
		}
		return arr;
	}
	
	/**
	 * Returns rank of given url stored in given list.
	 * @param url
	 * @param list
	 * @return rank of give url from list.
	 */
	private int getRankFromList(String url, List<TaggedVertex<String>> list) {
		int tag = 0;
		for(TaggedVertex<String> tv : list) {
			if(tv.getVertexData().equals(url)) {
				tag = tv.getTagValue();
				break;
			}
		}
		
		return tag;
	}

	/**
	 * Searches the index for pages containing both of the keywords w1 and w2.
	 * Returns a list of qualifying urls ordered by ranking (largest to smallest).
	 * The tag value associated with each url is its ranking. The ranking for a
	 * given page is the number of occurrences of w1 plus number of occurrences of
	 * w2, all multiplied by the indegree of its url in the associated graph. No
	 * pages with rank zero are included.
	 * 
	 * @param w1
	 *            first keyword to search for
	 * @param w2
	 *            second keyword to search for
	 * @return ranked list of urls
	 */
	public List<TaggedVertex<String>> searchWithAnd(String w1, String w2) {
		List<TaggedVertex<String>> rankedAnd = new ArrayList<TaggedVertex<String>>();
		
		List<TaggedVertex<String>> search1 = search(w1);
		List<TaggedVertex<String>> search2 = search(w2);
		if(search1.size() <= search2.size()) {
			ArrayList<String> search2Urls = getData(search2);
			for(TaggedVertex<String> url : search1) {
				if(search2Urls.contains(url.getVertexData())) {
					int rank1 = url.getTagValue();
					int rank2 = getRankFromList(url.getVertexData(), search2);
					if(rank1 + rank2 > 0) {
						TaggedVertex<String> tv = new TaggedVertex<String>(url.getVertexData(), rank1 + rank2);
						rankedAnd.add(tv);	
					}
				}
			}
		}
		else {
			ArrayList<String> search1Urls = getData(search1);
			for(TaggedVertex<String> url : search2) {
				if(search1Urls.contains(url.getVertexData())) {
					int rank2 = url.getTagValue();
					int rank1 = getRankFromList(url.getVertexData(), search1);
					if(rank1 + rank2 > 0) {
						TaggedVertex<String> tv = new TaggedVertex<String>(url.getVertexData(), rank1 + rank2);
						rankedAnd.add(tv);	
					}
				}
			}
		}
		
		rankedAnd.sort(new RankComparator());
		return rankedAnd;
	}
	
	/**
	 * Returns index of given url in list.
	 * @param url
	 * @param list
	 * @return index of given url in list.
	 */
	private int getIndex(String url, List<TaggedVertex<String>> list) {
		for(int i = 0; i < list.size(); i++) {
			if(list.get(i).getVertexData().equals(url)){
				return i;
			}
		}
		
		return -1;
	}

	/**
	 * Searches the index for pages containing at least one of the keywords w1 and
	 * w2. Returns a list of qualifying urls ordered by ranking (largest to
	 * smallest). The tag value associated with each url is its ranking. The ranking
	 * for a given page is the number of occurrences of w1 plus number of
	 * occurrences of w2, all multiplied by the indegree of its url in the
	 * associated graph. No pages with rank zero are included.
	 * 
	 * @param w1
	 *            first keyword to search for
	 * @param w2
	 *            second keyword to search for
	 * @return ranked list of urls
	 */
	public List<TaggedVertex<String>> searchWithOr(String w1, String w2) {
		List<TaggedVertex<String>> rankedOr = new ArrayList<TaggedVertex<String>>();
		
		List<TaggedVertex<String>> search1 = search(w1);
		List<TaggedVertex<String>> search2 = search(w2);
		
		if(search1.size() <= search2.size()) {
			rankedOr.addAll(0, search2);
			ArrayList<String> search2Urls = getData(search2);
			for(TaggedVertex<String> url : search1) {
				if(search2Urls.contains(url.getVertexData())) {
					int rank1 = url.getTagValue();
					int rank2 = getRankFromList(url.getVertexData(), search2);
					if(rank1 + rank2 > 0) {
						TaggedVertex<String> tv = new TaggedVertex<String>(url.getVertexData(), rank1 + rank2);
						rankedOr.remove(getIndex(url.getVertexData(), rankedOr));
						rankedOr.add(tv);	
					}
				}
				else {
					int rank = url.getTagValue();
					if(rank > 0) {
						TaggedVertex<String> tv = new TaggedVertex<String>(url.getVertexData(), rank);
						rankedOr.add(tv);	
					}
				}
			}
		}
		else {
			rankedOr.addAll(0, search1);
			ArrayList<String> search1Urls = getData(search1);
			for(TaggedVertex<String> url : search2) {
				if(search1Urls.contains(url.getVertexData())) {
					int rank2 = url.getTagValue();
					int rank1 = getRankFromList(url.getVertexData(), search1);
					if(rank2 + rank1 > 0) {
						TaggedVertex<String> tv = new TaggedVertex<String>(url.getVertexData(), rank2 + rank1);
						rankedOr.remove(getIndex(url.getVertexData(), rankedOr));
						rankedOr.add(tv);	
					}
				}
				else {
					int rank = url.getTagValue();
					if(rank > 0) {
						TaggedVertex<String> tv = new TaggedVertex<String>(url.getVertexData(), rank);
						rankedOr.add(tv);	
					}
				}
			}
		}
		
		rankedOr.sort(new RankComparator());
		return rankedOr;
	}

	/**
	 * Searches the index for pages containing keyword w1 but NOT w2. Returns a list
	 * of qualifying urls ordered by ranking (largest to smallest). The tag value
	 * associated with each url is its ranking. The ranking for a given page is the
	 * number of occurrences of w1, multiplied by the indegree of its url in the
	 * associated graph. No pages with rank zero are included.
	 * 
	 * @param w1
	 *            first keyword to search for
	 * @param w2
	 *            second keyword to search for
	 * @return ranked list of urls
	 */
	public List<TaggedVertex<String>> searchAndNot(String w1, String w2) {
		List<TaggedVertex<String>> rankedNot = new ArrayList<TaggedVertex<String>>();
		
		List<TaggedVertex<String>> search1 = search(w1);
		List<TaggedVertex<String>> search2 = search(w2);
		
		ArrayList<String> search2Urls = getData(search2);
		for(TaggedVertex<String> url : search1) {
			if(!search2Urls.contains(url.getVertexData())) {
				if(url.getTagValue() > 0) {
					rankedNot.add(url);	
				}
			}
		}
		
		rankedNot.sort(new RankComparator());
		return rankedNot;
	}	

}
