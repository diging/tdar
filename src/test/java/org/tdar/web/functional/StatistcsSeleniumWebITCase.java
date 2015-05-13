package org.tdar.web.functional;

import junit.framework.Assert;

import org.junit.Test;
import org.openqa.selenium.By;

public class StatistcsSeleniumWebITCase extends AbstractEditorSeleniumWebITCase {

    @Test
    public void testColletionStatistics() {
        gotoPage("/collection/1575");
        testStatsSection();
    }

    private void testStatsSection() {
        find(By.partialLinkText("USAGE")).click();
        waitForPageload();
        Assert.assertTrue(getCurrentUrl().contains("usage"));
        find(By.partialLinkText("Last Week")).click();
        waitForPageload();
        find(By.partialLinkText("Last Month")).click();
        waitForPageload();
        find(By.partialLinkText("Overall")).click();
    }

    @Test
    public void testAccountStatistics() {
        logout();
        loginAdmin();
        gotoPage("/billing/1");
        testStatsSection();
    }
    
    @Test
    public void testResourceStats() {
        gotoPage("/dataset/3088");
        find(By.partialLinkText("USAGE")).click();
        waitForPageload();
        logger.debug(getText());
        Assert.assertTrue(getText().contains("2013"));
    }
}
