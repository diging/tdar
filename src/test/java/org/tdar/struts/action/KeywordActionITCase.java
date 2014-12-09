package org.tdar.struts.action;

import static org.junit.Assert.assertEquals;
import junit.framework.Assert;

import org.junit.Test;
import org.springframework.test.annotation.Rollback;
import org.tdar.core.bean.keyword.InvestigationType;
import org.tdar.core.bean.keyword.KeywordType;
import org.tdar.core.bean.resource.Status;
import org.tdar.struts.action.browse.BrowseKeywordController;

public class KeywordActionITCase extends AbstractDataIntegrationTestCase {

    @Test
    public void testBasicKeywordAction() {
        BrowseKeywordController bkc = setupController(3L, KeywordType.CULTURE_KEYWORD, null, false);
        bkc.view();
    }

    private BrowseKeywordController setupController(long l, KeywordType cultureKeyword, String slug,boolean expectNotFound) {
        BrowseKeywordController bkc = generateNewController(BrowseKeywordController.class);
        init(bkc, null);
        bkc.setId(l);
        bkc.setKeywordPath(cultureKeyword.getUrlSuffix());
        bkc.setKeywordType(cultureKeyword);
        bkc.setSlug(slug);
        Exception e = null;
        try {
            bkc.prepare();
        } catch (Exception ex) {
            e = ex;
            logger.error("ex",e);
        }
        if (expectNotFound) {
            Assert.assertTrue(e != null);
            Assert.assertTrue(e instanceof TdarActionException);
            Assert.assertTrue(e.getMessage().equals("not found"));
        } else {
            assertEquals(null, e);
        }
        return bkc;
    }

    @Test
    public void testKeywordActionInvalidId() {
        BrowseKeywordController bkc = setupController(1000L, KeywordType.CULTURE_KEYWORD, null, true);
//        String result = bkc.view();
//        assertEquals(TdarActionSupport.NOT_FOUND, result);
    }

    @Test
    @Rollback(true)
    public void testKeywordActionStatus() {
        InvestigationType it = setupTestInvestigationType();
        String slug = "test-type";
        BrowseKeywordController bkc = setupController(it.getId(), KeywordType.INVESTIGATION_TYPE, slug, true);
//        String result = bkc.view();
//        assertEquals(TdarActionSupport.NOT_FOUND, result);
    }

    @Test
    @Rollback(true)
    public void testKeywordActionStatusDraft() {
        InvestigationType it = setupTestInvestigationType();
        String slug = "test-type";

        // change to draft
        it.setStatus(Status.DRAFT);
        genericService.saveOrUpdate(it);
        BrowseKeywordController bkc = setupController(it.getId(), KeywordType.INVESTIGATION_TYPE, slug, true);
//        String result = bkc.view();
//        assertEquals(TdarActionSupport.NOT_FOUND, result);
    }

    @Test
    @Rollback(true)
    public void testKeywordActionStatusActive() {
        InvestigationType it = setupTestInvestigationType();
        String slug = "test-type";

        // change to draft
        it.setStatus(Status.ACTIVE);
        genericService.saveOrUpdate(it);
        BrowseKeywordController bkc = setupController(it.getId(), KeywordType.INVESTIGATION_TYPE, slug,false);
        String result = bkc.view();
        assertEquals(TdarActionSupport.SUCCESS, result);
    }

    @Test
    @Rollback(true)
    public void testKeywordActionBadSlug() {
        InvestigationType it = setupTestInvestigationType();
        String slug = "test-type";

        it.setStatus(Status.ACTIVE);
        genericService.saveOrUpdate(it);
        BrowseKeywordController bkc = setupController(it.getId(), KeywordType.INVESTIGATION_TYPE, slug, false);
        bkc = setupController(it.getId(), KeywordType.INVESTIGATION_TYPE, "1234", false);
        String result = bkc.view();
        assertEquals(BrowseKeywordController.BAD_SLUG, result);
    }

    private InvestigationType setupTestInvestigationType() {
        InvestigationType it = new InvestigationType();
        it.setDefinition("this is a test");
        it.setStatus(Status.DELETED);
        it.setLabel("test type");
        genericService.save(it);
        return it;
    }

}
