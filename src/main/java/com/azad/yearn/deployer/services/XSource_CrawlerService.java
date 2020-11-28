package com.azad.yearn.deployer.services;

import com.azad.yearn.deployer.action.EmailSender;
import com.azad.yearn.deployer.action.WebCrawler;
import com.azad.yearn.deployer.utils.AppProperties;
import com.azad.yearn.deployer.utils.AsyncConfiguration;
import com.azad.yearn.deployer.model.XSource_Content;
import com.azad.yearn.deployer.utils.XSource_Constants;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Component
public class XSource_CrawlerService {

    private Queue<String> fifo = new CircularFifoQueue<String>(100);
    private static final Logger log = LoggerFactory.getLogger(XSource_CrawlerService.class);

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Autowired
    private AppProperties applicationProperties;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    @Qualifier(AsyncConfiguration.TASK_EXECUTOR_FORK)
    private Executor forkExecutor;

    @Autowired
    @Qualifier(AsyncConfiguration.TASK_EXECUTOR_DEFAULT)
    private Executor defaultExecutor;

    @Scheduled(fixedDelay = XSource_Constants.XSource_MAX_SCRAP_DELAY )
    public void fetchData() {
        log.info("The time is now {}", dateFormat.format(new Date()) + ", thread : " + Thread.currentThread().getName());

        CompletableFuture<Void> result = CompletableFuture.runAsync(() -> {
           startCrawler();
        }, forkExecutor).exceptionally((e) -> {
            log.error("Error " + e);
            return null;
        });

    }

    private void startCrawler(){
        int pageIdx = 0;

        // parse <XSource_Constants.XSource_MAX_SCRAP_PAGE> pages (throttled to avoid ip banned this application.
       // while (pageIdx != XSource_Constants.XSource_MAX_SCRAP_PAGE) {
            try {
                pageIdx = pageIdx + 1; // e.x: pageIdx = pageIdx + 5 -> 5 page at a time by forkjoin pool
                //TODO if change webcrawler to @component how to always get a new instance? need research
                String url = XSource_Constants.XSource_SEARCH_URI + "&address=" + applicationProperties.getEtherscan().getDeployer() + "&apikey=" + applicationProperties.getEtherscan().getApikey();
                log.info("Search URL: " + url);
                Collection<XSource_Content> data = new WebCrawler(url, applicationProperties.getAsync().getForkThreadSize(), pageIdx).startScrapping();
                defaultExecutor.execute(new EmailSender(data,fifo,applicationProperties));
            } catch (Exception e){
                log.error("Error " + e);
            }
      //  }
    }

    /**
    private void save(Collection<XSource_Content> object){
        try {
            for (Object item : object) {
                if(item instanceof XSource_Content) {

                }
            }
        } catch (Exception e){
            log.error(e.getMessage());
        }
    }
     */
}
