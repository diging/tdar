package org.tdar.web.functional;

import static java.lang.String.format;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.tdar.web.functional.util.WebMatchers.emptySelection;
import static org.tdar.web.functional.util.WebMatchers.visible;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Keys;
import org.tdar.web.functional.util.WebElementSelection;

/**
 * Created by jimdevos on 3/12/14.
 */
public class ContextualSeachSeleniumITCase extends AbstractSeleniumWebITCase {

    public static final String PROJECT_ID = "3805";
    public static final String COLLECTION_ID = "1575";
    public static final String COLLECTION_QUERY = "sample video";
    public static final String PROJECT_QUERY = "Archaeology";

    @Before
    public void setupContextSearch() {
        force1024x768();
    }

    @Override
    public boolean testRequiresLucene() {
        return true;
    }

    @After
    public void cleanup() {
        resetSize();
    }

    @Test
    public void testProjectResults2() {
        basicTest(format("/project/%s", PROJECT_ID), PROJECT_QUERY);
    }

    @Test
    public void testCollectionResults() {
        basicTest(format("/collection/%s", COLLECTION_ID), COLLECTION_QUERY);
    }

    private void basicTest(String path, String query) {
        gotoPage(path);

        // Focus on the searchbox. The contextual search option should appear.
        WebElementSelection sb = find("input.searchbox").click();
        assertThat(sb, is(not(emptySelection())));
        assertThat(sb, is(visible()));

        // enable contextual search, enter text that matches one of the resources we want to find
        takeScreenshot("after-clicking-ctx-checkbox");
        find("#cbctxid").click();
        sb.val(query).sendKeys(Keys.RETURN);

        WebElementSelection titlebar = waitFor("#titlebar");
        assertThat(titlebar.getText(), containsString("Search Results"));
        assertThat("expecting at least one search result", find(".tdarresults"), is(not(emptySelection())));

    }

}
