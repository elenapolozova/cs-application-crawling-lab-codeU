package com.flatironschool.javacs;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import redis.clients.jedis.Jedis;


public class WikiCrawler {
	// keeps track of where we started
	private final String source;
	
	// the index where the results go
	private JedisIndex index;
	
	// queue of URLs to be indexed
	private Queue<String> queue = new LinkedList<String>();
	
	// fetcher used to get pages from Wikipedia
	final static WikiFetcher wf = new WikiFetcher();

	/**
	 * Constructor.
	 * 
	 * @param source
	 * @param index
	 */
	public WikiCrawler(String source, JedisIndex index) {
		this.source = source;
		this.index = index;
		queue.offer(source);
	}

	/**
	 * Returns the number of URLs in the queue.
	 * 
	 * @return
	 */
	public int queueSize() {
		return queue.size();	
	}

	/**
	 * Gets a URL from the queue and indexes it.
	 * @param b 
	 * 
	 * @return Number of pages indexed.
	 * @throws IOException
	 */
	public String crawl(boolean testing) throws IOException {
		String nextURL = null;
		if (testing){
			nextURL = queue.poll(); 
			Elements paragraphs = wf.readWikipedia(nextURL);
			index.indexPage(nextURL, paragraphs);
			queueInternalLinks(paragraphs);
		} // end testing if
		else{
			nextURL = queue.poll(); 
			// insert check to see if page already in DB and do nothing if it is
			if (!index.isIndexed(nextURL)){
				Elements paragraphs = wf.readWikipedia(nextURL);
				index.indexPage(nextURL, paragraphs);
				queueInternalLinks(paragraphs);
			}
		} // end else
        // FILL THIS IN!
		return nextURL;
	}
	
	/**
	 * Parses paragraphs and adds internal links to the queue.
	 * 
	 * @param paragraphs
	 */
	// NOTE: absence of access level modifier means package-level
	void queueInternalLinks(Elements paragraphs) {
        int numParagraphs = paragraphs.size();
		// iterate over every paragraph till first link is found
		//boolean validLinkFound = false;
		String addressStub = "";
		for (int p = 0; p < numParagraphs; p++){
			Element currentPara = paragraphs.get(p);
			Elements linksInParagraph = currentPara.select("a[href]");
			if (linksInParagraph.size() > 0){
					// iterate until we find a "valid" link, aka one that starts with a lowercase letter
				for (int l = 0; l < linksInParagraph.size(); l++){
					addressStub = linksInParagraph.get(l).attr("href"); // update to reflect that we found
					if (addressStub.substring(0, 5).equals("/wiki")){
						String url = "https://en.wikipedia.org" + addressStub;
						queue.add(url);
					} // only add internal links
				} // end for that finds a valid link, if one exists
			} // end if that checks if paragraph has any links in it
		} // end for that traverses each paragraph in page
	}


	public static void main(String[] args) throws IOException {
		
		// make a WikiCrawler
		Jedis jedis = JedisMaker.make();
		JedisIndex index = new JedisIndex(jedis); 
		String source = "https://en.wikipedia.org/wiki/Java_(programming_language)";
		WikiCrawler wc = new WikiCrawler(source, index);
		
		// for testing purposes, load up the queue
		Elements paragraphs = wf.fetchWikipedia(source);
		wc.queueInternalLinks(paragraphs);

		// loop until we index a new page
		String res;
		do {
			res = wc.crawl(false);

            // REMOVE THIS BREAK STATEMENT WHEN crawl() IS WORKING
            break;
		} while (res == null);
		
		Map<String, Integer> map = index.getCounts("the");
		for (Entry<String, Integer> entry: map.entrySet()) {
			System.out.println(entry);
		}
	}
}
