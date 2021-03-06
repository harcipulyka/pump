package com.company;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StocktwitsScraper implements Runnable {

    final static Entry WRONG_ENTRY = new Entry("NaN","NaN", "NaN", null);
    final static String BASE = "https://stocktwits.com/symbol/";

    final WebClient client;
    final List<String> links;
    List<Entry> result = new ArrayList<>();

    public StocktwitsScraper(List<String> links) {
        client = new WebClient();
        client.getOptions().setCssEnabled(false);
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setThrowExceptionOnFailingStatusCode(false);
        this.links = links;
    }

    private HtmlPage getPage (String ticker) {
        HtmlPage page;
        try {
            page = client.getPage(BASE + ticker);
            return page;
        }catch(Exception e){
            System.err.println("Small problem with this link" + e);
            return null;
        }
    }

    public List<Entry> parsePages() {
        List<Entry> entries = new ArrayList<>();

        for (String s : links) {
            HtmlPage p = getPage(s);
            if(p.getWebResponse().getStatusCode() == 404 || p == null) {
                entries.add(WRONG_ENTRY);
            } else {
                Entry newEntry = getData(p);
                if(newEntry != WRONG_ENTRY) System.err.println(s);
                entries.add(newEntry);
            }
        }

        return entries;
    }

    private Entry getData(HtmlPage p) {
        //followers
        HtmlStrong f = p.getFirstByXPath(".//strong");
        if (f == null ) return WRONG_ENTRY; //this is needed against the dynamically loaded small pages, where the page can be loaded, and still not contain bold
        String followers = f.asText();

        //sentiment
        String page = p.asXml();
        int start = page.indexOf("\"sentimentChange\":");
        int end = page.indexOf(",\"volumeChange\":");
        if(start == -1 || end == -1) return WRONG_ENTRY;
        String substring = page.substring(start, end);
        String[] temp = substring.split(":");
        String sentiment = temp[1];

        //volume
        start = page.indexOf("\"volumeChange\":");
        end = page.indexOf(",\"priceData\"");
        if(start == -1 || end == -1) return WRONG_ENTRY;
        substring = page.substring(start, end);
        String[] temp2 = substring.split(":");
        String message = temp2[1];

        //fundamentals
        List<HtmlListItem> fundamentals = p.getByXPath("//li");
        Map<String, String> keyData = new HashMap<>();
        for(HtmlListItem l : fundamentals) {
            List<HtmlSpan> span = l.getByXPath("span");
            if(span.size() == 2) {
                String attributeName = span.get(0).asText();
                String value = span.get(1).asText();
                keyData.put(attributeName, value);
            }
        }

        //exchange market
        /*
        HtmlDivision exchange = p.getFirstByXPath("//div[@class='st_3BauJpd st_3OfMfdC st_3TuKxmZ']");
        HtmlSpan companyName = exchange.getFirstByXPath("span");
        String tmp = exchange.asText().replace(companyName.asText(), "");
        String[] tmp2 = tmp.split(" ");
        */

        Entry newEntry = new Entry(followers, sentiment, message, keyData);
        return newEntry;
    }

    public void run() {
        result = parsePages();
    }

}
