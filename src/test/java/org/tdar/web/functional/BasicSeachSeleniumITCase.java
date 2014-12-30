package org.tdar.web.functional;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.tdar.utils.TestConfiguration;
import org.tdar.web.functional.util.WebElementSelection;

/**
 * Created by jimdevos on 3/12/14.
 */
public class BasicSeachSeleniumITCase extends AbstractSeleniumWebITCase {

    private static final String SEARCH_RESULTS = "/search/results";

    @Before
    public void setup() {
        reindexOnce();
    }

    @Test
    public void testBrowse() {
        gotoPage("/browse/explore");
        Set<String> urls = new HashSet<>();
        for (WebElement el_ : find("article ul")) {
            // only add one per selection group
            for (WebElement el : new WebElementSelection(el_, getDriver()).find("a")) {
                String url = el.getAttribute("href");
                if (url.contains(TestConfiguration.getInstance().getHostName())) {
                    urls.add(url);
                    break;
                }
            }
        }

        for (String url : urls) {
            gotoPage("/browse/explore");
            gotoPage(url);
        }
    }

    @Test
    public void testResults() {
        gotoPage(SEARCH_RESULTS);
        Set<String> urls = new HashSet<>();
        for (WebElement el : find("article ul a")) {
            String url = el.getAttribute("href");
            if (url.contains(TestConfiguration.getInstance().getHostName())) {
                urls.add(url);
                break;
            }
        }
        for (String url : urls) {
            gotoPage(SEARCH_RESULTS);
            gotoPage(url);
        }

        gotoPage(SEARCH_RESULTS);
        Select sel = new Select(getDriver().findElement(By.id("recordsPerPage")));
        int size = sel.getOptions().size();
        for (int i = 0; i < size; i++) {
            sel = new Select(getDriver().findElement(By.id("recordsPerPage")));
            sel.selectByIndex(i);
            waitForPageload();
        }
        sel = new Select(getDriver().findElement(By.id("sortField")));
        size = sel.getOptions().size();
        for (int i = 0; i < size; i++) {
            sel = new Select(getDriver().findElement(By.id("sortField")));
            sel.selectByIndex(i);
            waitForPageload();
        }
    }

}
