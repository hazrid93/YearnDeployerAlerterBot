package com.azad.yearn.deployer.action;

import com.azad.yearn.deployer.model.XSource_Content;
import com.azad.yearn.deployer.utils.XSource_Constants;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RecursiveTask;


public class XSource_LinkProcessor extends RecursiveTask<XSource_Content> {

    private static final Logger log = LoggerFactory.getLogger(XSource_LinkProcessor.class);
    private String url;
    private int page;

    public XSource_LinkProcessor() {
    }

    public XSource_LinkProcessor(String url, int page) {
        this.url = url;
        this.page = page;
    }

    @Override
    protected XSource_Content compute() {
        String currentUrl = url;
        currentUrl = url + "&page=" + page + "&offset=10" + "&sort=desc";
        XSource_Content XSourceContent = null;
        try {
            log.debug("current page: " + currentUrl + ", thread: " + Thread.currentThread().getName() );
            log.debug("thread: " + Thread.currentThread().getName() + ", page: " + page);
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            HttpEntity <String> entity = new HttpEntity<String>(headers);
           // log.info("Content URL: " + currentUrl);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<XSource_Content> response = restTemplate.getForEntity(currentUrl, XSource_Content.class);
            log.info("content data:" + response.getBody().getResult().size());
            return response.getBody();

        } catch (Exception e) {
            log.error("Error " + e);
        }
        return XSourceContent;
    }

}
