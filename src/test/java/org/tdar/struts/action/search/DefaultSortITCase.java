package org.tdar.struts.action.search;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.tdar.search.query.SortOption;
import org.tdar.struts.action.TdarActionException;

public class DefaultSortITCase extends AbstractSearchControllerITCase {

    @Test
    public void testAdvancedSearchDefaultSort() throws TdarActionException {
        controller = generateNewInitializedController(AdvancedSearchController.class);
        controller.search();
        logger.info("sorting by: {} ", controller.getSortField());
        assertEquals(SortOption.getDefaultSortOption(), controller.getSortField());
    }

    @Test
    public void testRSSDefaultSort() throws TdarActionException {
        RSSSearchAction controller = generateNewInitializedController(RSSSearchAction.class);
        controller.viewRss();
        logger.info("sorting by: {} ", controller.getSortField());
        assertEquals(SortOption.ID_REVERSE, controller.getSortField());
    }
}
