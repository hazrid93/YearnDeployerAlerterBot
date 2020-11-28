package com.azad.yearn.deployer.action;

import com.azad.yearn.deployer.model.XSource_Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;


/**
 * This will be the starting class to start the scrapper processes.
 */

public class WebCrawler {
    private static final Logger log = LoggerFactory.getLogger(WebCrawler.class);

    //private final Collection<String> visitedLinks = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private String url;
    private ForkJoinPool mainPool;
    private int page;

    public WebCrawler() {
    }

    public WebCrawler(String startURL, int threadCount, int page) {
        this.url = startURL;
        this.page = page;
        mainPool = new ForkJoinPool(threadCount);
    }

    public Collection<XSource_Content> startScrapping(){
        Collection<XSource_Content> object = new CopyOnWriteArrayList<XSource_Content>();
      //  for(int i=1; i<=page; i++){
            object.add(mainPool.invoke(new XSource_LinkProcessor(this.url, this.page)));
      //  }
        return object;
    }

}

