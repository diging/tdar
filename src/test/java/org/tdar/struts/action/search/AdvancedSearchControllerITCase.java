package org.tdar.struts.action.search;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.tdar.TestConstants;
import org.tdar.core.bean.Indexable;
import org.tdar.core.bean.collection.ResourceCollection;
import org.tdar.core.bean.collection.ResourceCollection.CollectionType;
import org.tdar.core.bean.coverage.CoverageDate;
import org.tdar.core.bean.coverage.CoverageType;
import org.tdar.core.bean.coverage.LatitudeLongitudeBox;
import org.tdar.core.bean.entity.Creator;
import org.tdar.core.bean.entity.Institution;
import org.tdar.core.bean.entity.Person;
import org.tdar.core.bean.entity.ResourceCreator;
import org.tdar.core.bean.entity.ResourceCreatorRole;
import org.tdar.core.bean.entity.TdarUser;
import org.tdar.core.bean.keyword.CultureKeyword;
import org.tdar.core.bean.keyword.GeographicKeyword;
import org.tdar.core.bean.keyword.Keyword;
import org.tdar.core.bean.keyword.MaterialKeyword;
import org.tdar.core.bean.keyword.OtherKeyword;
import org.tdar.core.bean.keyword.SiteNameKeyword;
import org.tdar.core.bean.keyword.SiteTypeKeyword;
import org.tdar.core.bean.keyword.TemporalKeyword;
import org.tdar.core.bean.resource.Document;
import org.tdar.core.bean.resource.Image;
import org.tdar.core.bean.resource.InformationResource;
import org.tdar.core.bean.resource.Project;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.bean.resource.ResourceType;
import org.tdar.core.bean.resource.Status;
import org.tdar.core.dao.external.auth.InternalTdarRights;
import org.tdar.core.service.EntityService;
import org.tdar.core.service.GenericKeywordService;
import org.tdar.core.service.ResourceCreatorProxy;
import org.tdar.core.service.external.AuthorizationService;
import org.tdar.core.service.resource.ResourceService;
import org.tdar.core.service.search.Operator;
import org.tdar.core.service.search.SearchIndexService;
import org.tdar.core.service.search.SearchParameters;
import org.tdar.search.index.LookupSource;
import org.tdar.search.query.SearchResultHandler.ProjectionModel;
import org.tdar.search.query.SortOption;
import org.tdar.struts.action.AbstractControllerITCase;
import org.tdar.struts.data.DateRange;

@Transactional
public class AdvancedSearchControllerITCase extends AbstractControllerITCase {

    private static final String USAF_LOWER_CASE = "us air force archaeology and cultural resources archive";

    private static final String USAF_TITLE_CASE = "US Air Force Archaeology and Cultural Resources Archive";

    private static final String CONSTANTINOPLE = "Constantinople";

    private static final String ISTANBUL = "Istanbul";

    @Autowired
    SearchIndexService searchIndexService;

    @Autowired
    ResourceService resourceServicek;

    @Autowired
    GenericKeywordService genericKeywordService;

    @Autowired
    EntityService entityService;

    @Autowired
    private AuthorizationService authenticationAndAuthorizationService;

    private AdvancedSearchController controller;

    public void AdvancedSearchController() {
        controller = generateNewInitializedController(AdvancedSearchController.class);
    }

    private void resetController() {
        controller = generateNewInitializedController(AdvancedSearchController.class);
    }

    // we assume here that struts performs similar actions when you reference an element index that may not yet exist.
    private SearchParameters firstGroup() {
        if (controller.getG().isEmpty()) {
            controller.getG().add(new SearchParameters());
        }
        return controller.getG().get(0);
    }

    @Before
    public void reset() {
        reindex();
        resetController();
        controller.setRecordsPerPage(50);
    }

    @Test
    @Rollback
    public void testSiteNameKeywords() {
        SiteNameKeyword snk = genericKeywordService.findByLabel(SiteNameKeyword.class, "Atsinna");
        Document doc = createAndSaveNewResource(Document.class);
        doc.getSiteNameKeywords().add(snk);
        genericService.saveOrUpdate(doc);
        searchIndexService.index(doc);
        firstGroup().getSiteNames().add(snk.getLabel());
        doSearch();
        assertFalse("we should get back at least one hit", controller.getResults().isEmpty());
        for (Resource resource : controller.getResults()) {
            assertTrue("expecting site name for resource", resource.getSiteNameKeywords().contains(snk));
        }
    }

    @Test
    public void testResourceCaseSensitivity() {
        Document doc = createAndSaveNewResource(Document.class);
        ResourceCollection titleCase = new ResourceCollection(USAF_TITLE_CASE, "test", SortOption.RELEVANCE, CollectionType.SHARED, false, getAdminUser());
        titleCase.markUpdated(getAdminUser());
        ResourceCollection lowerCase = new ResourceCollection(USAF_LOWER_CASE, "test", SortOption.RELEVANCE, CollectionType.SHARED, false, getAdminUser());
        lowerCase.markUpdated(getAdminUser());
        ResourceCollection upperCase = new ResourceCollection("USAF", "test", SortOption.RELEVANCE, CollectionType.SHARED, false, getAdminUser());
        upperCase.markUpdated(getAdminUser());
        ResourceCollection usafLowerCase = new ResourceCollection("usaf", "test", SortOption.RELEVANCE, CollectionType.SHARED, false, getAdminUser());
        usafLowerCase.markUpdated(getAdminUser());
        doc.setTitle("USAF");
        updateAndIndex(doc);
        updateAndIndex(titleCase);
        updateAndIndex(lowerCase);
        updateAndIndex(upperCase);
        updateAndIndex(usafLowerCase);
        
        // search lowercase one word
        controller.setQuery("usaf");
        doSearch();
        assertTrue(controller.getResults().contains(doc));
        assertTrue(controller.getCollectionResults().contains(usafLowerCase));
        assertTrue(controller.getCollectionResults().contains(upperCase));
        doc.setTitle("usaf");
        resetController();
        updateAndIndex(doc);
        
        // search uppercase one word
        controller.setQuery("USAF");
        doSearch();
        assertTrue(controller.getCollectionResults().contains(usafLowerCase));
        assertTrue(controller.getCollectionResults().contains(upperCase));
        assertTrue(controller.getResults().contains(doc));

        resetController();
        // search lowercase phrase
        controller.setQuery("us air");
        doSearch();
        assertTrue(controller.getCollectionResults().contains(titleCase));
        assertTrue(controller.getCollectionResults().contains(lowerCase));

        resetController();
        // search titlecase phrase
        controller.setQuery("US Air");
        doSearch();
        assertTrue(controller.getCollectionResults().contains(titleCase));
        assertTrue(controller.getCollectionResults().contains(lowerCase));

        resetController();
        // search uppercase phrase
        controller.setQuery("US AIR");
        doSearch();
        assertTrue(controller.getCollectionResults().contains(titleCase));
        assertTrue(controller.getCollectionResults().contains(lowerCase));

    
        // search lowercase middle word
        controller.setQuery("force");
        doSearch();
        assertTrue(controller.getCollectionResults().contains(titleCase));
        assertTrue(controller.getCollectionResults().contains(lowerCase));

        // search uppercase middle word
        controller.setQuery("FORCE");
        doSearch();
        assertTrue(controller.getCollectionResults().contains(titleCase));
        assertTrue(controller.getCollectionResults().contains(lowerCase));
}

    @Test
    public void testTitleCaseSensitivity() {
        Document doc = createAndSaveNewResource(Document.class);
        doc.setTitle("usaf");
        updateAndIndex(doc);
        firstGroup().setTitles(Arrays.asList("USAF"));
        doSearch();
        assertTrue(controller.getResults().contains(doc));
        doc.setTitle("USAF");
        updateAndIndex(doc);
        resetController();
        firstGroup().setTitles(Arrays.asList("usaf"));
        doSearch();
        assertTrue(controller.getResults().contains(doc));
    }

    private void updateAndIndex(Indexable doc) {
        genericService.saveOrUpdate(doc);
        searchIndexService.index(doc);
    }

    @Test
    @Rollback
    public void testComplexGeographicKeywords() {
        GeographicKeyword snk = genericKeywordService.findOrCreateByLabel(GeographicKeyword.class, "propylon, Athens, Greece, Mnesicles");
        Document doc = createAndSaveNewResource(Document.class);
        doc.getGeographicKeywords().add(snk);
        genericService.saveOrUpdate(doc);
        searchIndexService.index(doc);
        firstGroup().getGeographicKeywords().add("Greece");
        doSearch();
        assertFalse("we should get back at least one hit", controller.getResults().isEmpty());
        for (Resource resource : controller.getResults()) {
            assertTrue("expecting site name for resource", resource.getGeographicKeywords().contains(snk));
        }
    }

    @Test
    @Rollback
    public void testPersonSearchWithoutAutocomplete() {
        String lastName = "Watts";
        Person person = new Person(null, lastName, null);
        lookForCreatorNameInResult(lastName, person);
    }

    @Test
    @Rollback
    public void testMultiplePersonSearch() {
        Long peopleIds[] = { 8044L, 8344L, 8393L, 8608L, 8009L };
        List<Person> people = genericService.findAll(Person.class, Arrays.asList(peopleIds));
        assertEquals(4, people.size());
        logger.info("{}", people);
        List<String> names = new ArrayList<String>();
        for (Person person : people) {
            names.add(person.getProperName());
            Person p = new Person();
            // this will likely fail because skeleton people are being put into a set further down the chain...
            p.setId(person.getId());
            ResourceCreator rc = new ResourceCreator(p, null);
            firstGroup().getResourceCreatorProxies().add(new ResourceCreatorProxy(rc));
        }
        doSearch();
        logger.info(controller.getSearchPhrase());
        for (String name : names) {
            assertTrue(controller.getSearchPhrase().contains(name));
        }
        // lookForCreatorNameInResult(lastName, person);
    }

    @Test
    @Rollback
    public void testInstitutionSearchWithoutAutocomplete() {
        String name = "Digital Antiquity";
        Institution institution = new Institution(name);
        lookForCreatorNameInResult(name, institution);
    }

    private void lookForCreatorNameInResult(String namePart, Creator creator_) {
        firstGroup().getResourceCreatorProxies().add(new ResourceCreatorProxy(new ResourceCreator(creator_, null)));
        doSearch();
        assertFalse("we should get back at least one hit", controller.getResults().isEmpty());
        for (Resource resource : controller.getResults()) {
            logger.info("{}", resource);
            boolean seen = checkResourceForValue(namePart, resource);
            if (resource instanceof Project) {
                for (Resource r : projectService.findAllResourcesInProject((Project) resource, Status.values())) {
                    if (seen) {
                        break;
                    }
                    seen = checkResourceForValue(namePart, r);
                }

            }
            assertTrue("should have seen term somwehere", seen);
        }
    }

    private boolean checkResourceForValue(String namePart, Resource resource) {
        boolean seen = false;
        if (resource.getSubmitter().getProperName().contains(namePart) || resource.getUpdatedBy().getProperName().contains(namePart)) {
            logger.debug("seen submitter or updater");
            seen = true;
        }
        if (resource instanceof InformationResource) {
            Institution institution = ((InformationResource) resource).getResourceProviderInstitution();
            if ((institution != null) && institution.getName().contains(namePart)) {
                logger.debug("seen in institution");
                seen = true;
            }
        }
        for (ResourceCreator creator : resource.getActiveResourceCreators()) {
            if (creator.getCreator().getProperName().contains(namePart)) {
                logger.debug("seen in resource creator");
                seen = true;
            }
        }
        return seen;
    }

    @Test
    @Rollback
    public void testSearchDecade() {
        Document doc = createAndSaveNewResource(Document.class);
        doc.setDate(4000);
        genericService.saveOrUpdate(doc);
        searchIndexService.index(doc);
        firstGroup().getCreationDecades().add(4000);
        doSearch();
        assertFalse("we should get back at least one hit", controller.getResults().isEmpty());
        for (Resource resource : controller.getResults()) {
            assertEquals("expecting resource", 4000, ((InformationResource) resource).getDateNormalized().intValue());
        }

    }

    
    @Test
    @Rollback
    public void testApprovedSiteTypeKeywords() {
        final Long keywordId = 256L;
        Keyword keyword = genericService.find(SiteTypeKeyword.class, keywordId);
        List<String> keywordIds = new ArrayList<String>();
        keywordIds.add(keywordId.toString());
        firstGroup().getApprovedSiteTypeIdLists().add(keywordIds);
        doSearch();
        assertFalse("we should get back at least one hit", controller.getResults().isEmpty());
        assertTrue(resultsContainId(262L));
        controller.getIncludedStatuses().add(Status.ACTIVE);
        for (Resource resource : controller.getResults()) {
            // if it's a project, the keyword should be found in either it's own keyword list, or the keyword list
            // of one of the projects informationResources
            if (resource instanceof Project) {
                // put all of the keywords in a superset
                Project project = (Project) resource;
                Set<Keyword> keywords = new HashSet<Keyword>(project.getActiveSiteTypeKeywords());
                Set<InformationResource> projectInformationResources = projectService.findAllResourcesInProject(project, Status.ACTIVE);
                for (InformationResource informationResource : projectInformationResources) {
                    keywords.addAll(informationResource.getActiveSiteTypeKeywords());
                }
                assertTrue("keyword should be found in project, or project's informationResources",
                        keywords.contains(keyword));

            } else {
                logger.debug("resourceid:{} contents of resource:", resource.getId(), resource.getActiveSiteTypeKeywords());
                assertTrue("expecting site type for resource:", resource.getActiveSiteTypeKeywords().contains(keyword));
            }
        }
    }

    @Test
    @Rollback
    public void testMaterialKeywords() {
        // FIXME: magic numbers
        Keyword keyword = genericKeywordService.find(MaterialKeyword.class, 2L);
        firstGroup().getMaterialKeywordIdLists().add(Arrays.asList(keyword.getId().toString()));
        doSearch();
        assertTrue("we should get back at least one hit", !controller.getResults().isEmpty());
        // every resource in results should have that material keyword (or should have at least one informationResource that has that keyword)
        for (Resource resource : controller.getResults()) {
            Set<Keyword> keywords = new HashSet<Keyword>(resource.getActiveMaterialKeywords());
            if (resource instanceof Project) {
                // check that at least one child uses this keyword
                Project project = (Project) resource;
                for (InformationResource informationResource : projectService.findAllResourcesInProject(project)) {
                    keywords.addAll(informationResource.getMaterialKeywords());
                }
            }
            assertTrue(String.format("Expected to find material keyword %s in %s", keyword, resource), keywords.contains(keyword));
        }
    }

    @Test
    @Rollback
    public void testCulture() {
        // FIXME: this test is brittle/incomplete
        String label = "Sinagua";
        firstGroup().getUncontrolledCultureKeywords().add(label);
        Keyword keyword = genericKeywordService.findByLabel(CultureKeyword.class, label);

        doSearch();
        assertTrue("we should get back at least one hit", !controller.getResults().isEmpty());

        for (Resource resource : controller.getResults()) {
            // if it's a project, the keyword should be found in either it's own keyword list, or the keyword list
            // of one of the projects informationResources
            if (resource instanceof Project) {
                // put all of the keywords in a superset
                Project project = (Project) resource;
                Set<Keyword> keywords = new HashSet<Keyword>(project.getActiveCultureKeywords());
                Set<InformationResource> projectInformationResources = projectService.findAllResourcesInProject(project, Status.ACTIVE);
                for (InformationResource informationResource : projectInformationResources) {
                    keywords.addAll(informationResource.getActiveCultureKeywords());
                }
                assertTrue("keyword should be found in project, or project's informationResources",
                        keywords.contains(keyword));

            } else {
                logger.debug("resourceid:{} contents of resource:", resource.getId(), resource.getActiveCultureKeywords());
                assertTrue("expecting site type for resource:", resource.getActiveCultureKeywords().contains(keyword));
            }
        }

    }

    @Test
    @Rollback
    public void testApprovedCulture() {
        // FIXME: pull this ID from db or generate/save new keyword+resource that uses it
        Long keywordId = 19L;
        Keyword keyword = genericService.find(CultureKeyword.class, keywordId);
        firstGroup().getApprovedCultureKeywordIdLists().add(Arrays.asList(keywordId.toString()));
        doSearch();
        assertTrue("we should get back at least one hit", !controller.getResults().isEmpty());
        for (Resource resource : controller.getResults()) {
            // if it's a project, the keyword should be found in either it's own keyword list, or the keyword list
            // of one of the projects informationResources
            if (resource instanceof Project) {
                // put all of the keywords in a superset
                Project project = (Project) resource;
                Set<Keyword> keywords = new HashSet<Keyword>(project.getActiveCultureKeywords());
                Set<InformationResource> projectInformationResources = projectService.findAllResourcesInProject(project, Status.ACTIVE);
                for (InformationResource informationResource : projectInformationResources) {
                    keywords.addAll(informationResource.getActiveCultureKeywords());
                }
                assertTrue("keyword should be found in project, or project's informationResources",
                        keywords.contains(keyword));

            } else {
                logger.debug("resourceid:{} contents of resource:", resource.getId(), resource.getActiveCultureKeywords());
                assertTrue("expecting site type for resource:", resource.getActiveCultureKeywords().contains(keyword));
            }
        }
    }

    @Test
    @Rollback
    public void testLatLong() throws Exception {
        // create a document that we expect to find w/ geo search, and a bounding box big enough to find it
        Document doc = createAndSaveNewInformationResource(Document.class);
        LatitudeLongitudeBox region = new LatitudeLongitudeBox(-100d, 30d, -90d, 40d);
        LatitudeLongitudeBox selectionRegion = new LatitudeLongitudeBox(-101d, 29d, -89d, 41d);
        LatitudeLongitudeBox elsewhere = new LatitudeLongitudeBox(100d, 10d, 110d, 20d);

        doc.getLatitudeLongitudeBoxes().add(region);

        genericService.save(doc);
        reindex();

        controller = generateNewInitializedController(AdvancedSearchController.class, getAdminUser());
        controller.setRecordsPerPage(50);
        controller.setMap(selectionRegion);
        doSearch();
        assertTrue("expected to find document within selection region", controller.getResults().contains(doc));

        // now do another search with bounding boxes outside of doc's region
        controller = generateNewInitializedController(AdvancedSearchController.class, getAdminUser());
        controller.setRecordsPerPage(50);
        controller.setMap(elsewhere);
        assertFalse("document shouldn't not be found within provided bounding box.", controller.getResults().contains(doc));
        doSearch();

    }

    @Test
    @Rollback
    public void testProjectIds() {
        // FIXME: magic numbers
        Long projectId = 3805L;
        firstGroup().getProjects().add(sparseProject(projectId));
        controller.getResourceTypes().clear(); // select all resource types
        doSearch();
        int resourceCount = 0;
        for (Resource resource : controller.getResults()) {
            if (resource instanceof InformationResource) {
                resourceCount++;
                InformationResource informationResource = (InformationResource) resource;
                assertEquals("informationResource should belong to project we just searched for", projectId, informationResource.getProjectId());
            }
        }
        assertTrue("search should have at least 1 result", resourceCount > 0);
    }

    @SuppressWarnings("deprecation")
    private Project sparseProject(Long id) {
        Project project = new Project(id, "sparse");
        return project;
    }

    private ResourceCollection sparseCollection(Long id) {
        ResourceCollection collection = new ResourceCollection();
        collection.setId(id);
        return collection;
    }

    @Test
    @Rollback
    public void testTitle() {
        // FIXME: magic numbers
        Long projectId = 139L;
        Project project = genericService.find(Project.class, projectId);
        String projectTitle = project.getTitle();
        firstGroup().getTitles().add(projectTitle);
        doSearch();
        controller.getResults().contains(project);
    }

    @Test
    @Rollback
    public void testSearchSubmitterIds() {
        // FIXME: magic numbers
        Person person = genericService.find(Person.class, 6L);
        firstGroup().getResourceCreatorProxies().add(new ResourceCreatorProxy(person, ResourceCreatorRole.SUBMITTER));
        doSearch();

        // make sure every resource has that submitter
        for (Resource resource : controller.getResults()) {
            assertEquals("Expecting same submitterId", person.getId(), resource.getSubmitter().getId());
        }
    }

    @Test
    @Rollback(true)
    public void testResultCountsAsUnauthenticatedUser() {
        evictCache();

        setIgnoreActionErrors(true);
        testResourceCounts(null);
    }

    @Test
    @Rollback(true)
    public void testResultCountsAsBasicUser() {
        evictCache();

        // testing as a user who did not create their own stuff
        setIgnoreActionErrors(true);
        TdarUser p = new TdarUser("a", "test", "anoter@test.user.com");
        p.setContributor(true);
        genericService.saveOrUpdate(p);
        testResourceCounts(p);
    }

    @Test
    @Rollback(true)
    public void testResultCountsAsBasicContributor() {
        // testing as a user who did create their own stuff
        evictCache();
        setIgnoreActionErrors(true);
        testResourceCounts(getBasicUser());
    }

    @Test
    @Rollback(true)
    public void testTitleSiteCodeMatching() {
        List<String> titles = Arrays
                .asList("Pueblo Grande (AZ U:9:1(ASM)): Unit 12, Gateway and 44th Streets: SSI Kitchell Testing, Photography Log (PHOTO) Data (1997)",
                        "Archaeological Testing at Pueblo Grande (AZ U:9:1(ASM)): Unit 15, The Former Maricopa County Sheriff's Substation, Washington and 48th Streets, Phoenix, Arizona -- DRAFT REPORT (1999)",
                        "Phase 2 Archaeological Testing at Pueblo Grande (AZ U:9:1(ASM)): Unit 15, the Former Maricopa County Sheriff’s Substation, Washington and 48th Streets, Phoenix, Arizona -- DRAFT REPORT (1999)",
                        "Final Data Recovery And Burial Removal At Pueblo Grande (AZ U:9:1(ASM)): Unit 15, The Former Maricopa Counry Sheriff's Substation, Washington And 48th Streets, Phoenix, Arizona (2008)",
                        "Pueblo Grande (AZ U:9:1(ASM)): Unit 15, Washington and 48th Streets: Soil Systems, Inc. Kitchell Development Testing and Data Recovery (The Former Maricopa County Sheriff's Substation) ",
                        "Archaeological Testing of Unit 13 at Pueblo Grande, AZ U:9:1(ASM), Arizona Federal Credit Union Property, 44th and Van Buren Streets, Phoenix, Maricopa County, Arizona (1998)",
                        "Archaeological Testing And Burial Removal Of Unit 11 At Pueblo Grande, AZ U:9:1(ASM), DMB Property, 44th And Van Buren Streets, Phoenix, Maricopa County, Arizona -- DRAFT REPORT (1998)",
                        "Pueblo Grande (AZ U:9:1(ASM)): Unit 13, Northeast Corner of Van Buren and 44th Streets: Soil Systems, Inc. AZ Federal Credit Union Testing and Data Recovery Project ",
                        "POLLEN AND MACROFLORAL ANAYSIS AT THE WATER USERS SITE, AZ U:6:23(ASM), ARIZONA (1990)",
                        "Partial Data Recovery and Burial Removal at Pueblo Grande (AZ U:9:1(ASM)): Unit 15, The Former Maricopa County Sheriff's Substation, Washington and 48th Streets, Phoenix, Arizona -- DRAFT REPORT (2002)",
                        "MACROFLORAL AND PROTEIN RESIDUE ANALYSIS AT SITE AZ U:15:18(ASM), CENTRAL ARIZONA (1996)",
                        "Pueblo Grande (AZ U:9:1(ASM)) Soil Systems, Inc. Master Provenience Table: Projects, Unit Numbers, and Feature Numbers (2008)");

        List<Document> docs = new ArrayList<>();
        List<Document> badMatches = new ArrayList<>();
        for (String title : titles) {
            Document doc = new Document();
            doc.setTitle(title);
            doc.setDescription(title);
            doc.markUpdated(getBasicUser());
            genericService.saveOrUpdate(doc);
            if (title.contains("MACROFLORAL")) {
                badMatches.add(doc);
            }
        }
        genericService.synchronize();
        searchIndexService.indexCollection(docs);
        searchIndexService.flushToIndexes();
        controller.setQuery("AZ U:9:1(ASM)");
        controller.setRecordsPerPage(1000);
        doSearch();
        List<Resource> results = controller.getResults();
        logger.debug("results: {}", results);
        assertTrue("controller should not contain titles with MACROFLORAL", CollectionUtils.containsAny(results, badMatches));
        assertTrue("controller should not contain titles with MACROFLORAL",
                CollectionUtils.containsAll(results.subList(results.size() - 3, results.size()), badMatches));

    }

    @Test
    @Rollback(true)
    public void testResultCountsAdmin() {
        evictCache();
        testResourceCounts(getAdminUser());
    }

    @Test
    @Rollback(true)
    public void testGeographicKeywordIndexedAndFound() {
        Document doc = createAndSaveNewResource(Document.class, getBasicUser(), "testing doc");
        GeographicKeyword kwd = new GeographicKeyword();
        kwd.setLabel("Casa NonGrande");
        genericService.save(kwd);
        doc.getGeographicKeywords().add(kwd);
        genericService.saveOrUpdate(doc);
        searchIndexService.index(doc);
        firstGroup().getGeographicKeywords().add("Casa NonGrande");
        doSearch();
        boolean seen = false;
        for (Resource res : controller.getResults()) {
            logger.info("{}", res);
            if (res.getGeographicKeywords().contains(kwd)) {
                seen = true;
            } else {
                fail("found resource without keyword");
            }
        }
        assertTrue(seen);
    }

    @Test
    @Rollback(true)
    public void testFilenameFound() throws InstantiationException, IllegalAccessException {
        Document doc = generateDocumentWithFileAndUseDefaultUser();
        searchIndexService.index(doc);
        firstGroup().getFilenames().add(TestConstants.TEST_DOCUMENT_NAME);
        doSearch();
        boolean seen = false;
        for (Resource res : controller.getResults()) {
            if (res.getId().equals(doc.getId())) {
                seen = true;
            }
        }
        assertTrue(seen);
    }

    private void testResourceCounts(TdarUser user) {
        for (ResourceType type : ResourceType.values()) {
            Resource resource = createAndSaveNewResource(type.getResourceClass());
            for (Status status : Status.values()) {
                if ((Status.DUPLICATE == status) || (Status.FLAGGED_ACCOUNT_BALANCE == status)) {
                    continue;
                }
                resource.setStatus(status);
                genericService.saveOrUpdate(resource);
                searchIndexService.index(resource);
                assertResultCount(type, status, user);
            }
        }
    }

    // compare the counts returned from searchController against the counts we get from the database
    private void assertResultCount(ResourceType resourceType, Status status, TdarUser user) {
        String stat = String.format("testing %s , %s for %s", resourceType, status, user);
        logger.info(stat);
        long expectedCount = resourceService.getResourceCount(resourceType, status);
        controller = generateNewController(AdvancedSearchController.class);
        init(controller, user);
        controller.setRecordsPerPage(Integer.MAX_VALUE);
        controller.getResourceTypes().add(resourceType);
        controller.getIncludedStatuses().add(status);
        if (((status == Status.DELETED) && authenticationAndAuthorizationService.cannot(InternalTdarRights.SEARCH_FOR_DELETED_RECORDS, user)) ||
                ((status == Status.FLAGGED) && authenticationAndAuthorizationService.cannot(InternalTdarRights.SEARCH_FOR_FLAGGED_RECORDS, user))) {
            logger.debug("expecting exception");
            doSearch(true);
            assertTrue(String.format("expected action errors %s", stat), controller.getActionErrors().size() > 0);
        } else if ((status == Status.DRAFT) && authenticationAndAuthorizationService.cannot(InternalTdarRights.SEARCH_FOR_DRAFT_RECORDS, user)) {
            // this was in the test, but with the new status search I think this is more accurate to be commented out as
            doSearch(null);
            for (Resource res : controller.getResults()) {
                if (res.isDraft() && !res.getSubmitter().equals(user)) {
                    fail("we should only see our own drafts here");
                }

            }
            // assertEquals(String.format("expecting results to be empty %s",stat),0, controller.getTotalRecords());
        } else {
            doSearch();
            Object[] msg_ = { user, resourceType, status, expectedCount, controller.getTotalRecords() };
            String msg = String.format("User: %s ResourceType:%s  Status:%s  expected:%s actual: %s", msg_);
            logger.info(msg);
            Assert.assertEquals(msg, expectedCount, controller.getTotalRecords());
        }
    }

    private void setSortThenCheckFirstResult(String message, SortOption sortField, Long projectId, Long expectedId) {
        resetController();
        controller.setSortField(sortField);
        firstGroup().getProjects().add(sparseProject(projectId));
        doSearch();
        logger.info("{}", controller.getResults());
        Resource found = controller.getResults().iterator().next();
        logger.info("{}", found);
        Assert.assertEquals(message, expectedId, found.getId());
    }

    // note: relevance sort broken out into SearchRelevancyITCase
    @Test
    @Rollback
    public void testSortFieldTitle() {
        Long alphaId = -1L;
        Long omegaId = -1L;
        Project p = new Project();
        p.setTitle("test project");
        p.setDescription("test descr");
        p.setStatus(Status.ACTIVE);
        p.markUpdated(getUser());
        List<String> titleList = Arrays.asList(new String[] { "a", "b", "c", "d" });
        genericService.save(p);
        for (String title : titleList) {
            Document doc = new Document();
            doc.markUpdated(getUser());
            doc.setTitle(title);
            doc.setDescription(title);
            doc.setDate(2341);
            doc.setProject(p);
            doc.setStatus(Status.ACTIVE);
            genericService.save(doc);
            if (alphaId == -1) {
                alphaId = doc.getId();
            }
            omegaId = doc.getId();
        }
        reindex();
        setSortThenCheckFirstResult("sorting by title asc", SortOption.TITLE, p.getId(), alphaId);
        setSortThenCheckFirstResult("sorting by title desc", SortOption.TITLE_REVERSE, p.getId(), omegaId);
    }

    @Test
    @Rollback
    public void testSortFieldProject() throws InstantiationException, IllegalAccessException {

        Project project = createAndSaveNewProject("my project");
        Project project2 = createAndSaveNewProject("my project 2");
        Image a = createAndSaveNewInformationResource(Image.class, project, getBasicUser(), "a");
        Image b = createAndSaveNewInformationResource(Image.class, project, getBasicUser(), "b");
        Image c = createAndSaveNewInformationResource(Image.class, project, getBasicUser(), "c");

        Image d = createAndSaveNewInformationResource(Image.class, project2, getBasicUser(), "d");
        Image e = createAndSaveNewInformationResource(Image.class, project2, getBasicUser(), "e");
        Image aa = createAndSaveNewInformationResource(Image.class, project2, getBasicUser(), "a");
        List<Resource> res = Arrays.asList(project, project2, a, b, c, d, e, aa);

        reindex();

        controller.setQuery("");
        controller.setSortField(SortOption.PROJECT);
        controller.setRecordsPerPage(1000);
        doSearch();
        List<Resource> results = controller.getResults();
        // assertTrue(CollectionUtils.isProperSubCollection(results, res));
        int i = results.indexOf(project);
        assertEquals(i + 1, results.indexOf(a));
        assertEquals(i + 2, results.indexOf(b));
        assertEquals(i + 3, results.indexOf(c));
        assertEquals(i + 4, results.indexOf(project2));
        assertEquals(i + 5, results.indexOf(aa));
        assertEquals(i + 6, results.indexOf(d));
        assertEquals(i + 7, results.indexOf(e));
    }

    @Test
    @Rollback
    public void testSortFieldDateCreated() {
        Long alphaId = -1L;
        Long omegaId = -1L;
        Project p = new Project();
        p.setTitle("test project");
        p.setDescription("test description");
        p.markUpdated(getUser());
        List<Integer> dateList = Arrays.asList(new Integer[] { 1, 2, 3, 4, 5, 19, 39 });
        genericService.save(p);
        for (Integer date : dateList) {
            Document doc = new Document();
            doc.markUpdated(getUser());
            doc.setDate(date);
            doc.setTitle("hello" + date);
            doc.setDescription(doc.getTitle());
            doc.setProject(p);
            genericService.save(doc);
            if (alphaId == -1) {
                logger.debug("setting id for doc:{}", doc.getId());
                alphaId = doc.getId();
            }
            omegaId = doc.getId();
        }
        reindex();

        setSortThenCheckFirstResult("sorting by datecreated asc", SortOption.DATE, p.getId(), alphaId);
        setSortThenCheckFirstResult("sorting by datecreated desc", SortOption.DATE_REVERSE, p.getId(), omegaId);
    }

    @Test
    @Rollback
    public void testResourceCount() {
        // fixme: remove this query. it's only temporary to ensure that my named query is working
        long count = resourceService.getResourceCount(ResourceType.PROJECT, Status.ACTIVE);
        assertTrue(count > 0);
    }

    @Test
    @Rollback
    public void testResourceUpdated() throws java.text.ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Document document = new Document();
        document.setTitle("just before");
        document.setDescription("just before");
        document.markUpdated(getAdminUser());
        document.setDateUpdated(format.parse("2010-03-04"));
        genericService.saveOrUpdate(document);
        Document documentAfter = new Document();
        documentAfter.setTitle("just after");
        documentAfter.setDescription("just after");
        documentAfter.markUpdated(getAdminUser());
        documentAfter.setDateUpdated(format.parse("2010-07-23"));
        genericService.saveOrUpdate(documentAfter);
        genericService.synchronize();
        searchIndexService.flushToIndexes();
        controller.setSortField(SortOption.DATE_UPDATED);
        SearchParameters params = new SearchParameters();
        params.getUpdatedDates().add(new DateRange(format.parse("2010-03-05"), format.parse("2010-07-22")));
        controller.getG().add(params);
        AbstractSearchControllerITCase.doSearch(controller, LookupSource.RESOURCE);
        for (Resource r : controller.getResults()) {
            logger.debug("{} - {} - {}", r.getId(), r.getDateUpdated(), r.getTitle());
        }
        assertFalse(controller.getResults().contains(documentAfter));
        assertFalse(controller.getResults().contains(document));
    }

    /**
     * lucene translates dates to utc prior to indexing. When performing a search the system must similarly transform the begin/end
     * dates in a daterange
     */
    @Test
    @Rollback
    public void testTimezoneEdgeCase() {
        Resource doc = createAndSaveNewInformationResource(Document.class);
        DateTime createDateTime = new DateTime(2005, 3, 26, 23, 0, 0, 0);
        DateTime searchDateTime = new DateTime(2005, 3, 26, 0, 0, 0, 0);
        doc.setDateCreated(createDateTime.toDate());
        doc.setDateUpdated(createDateTime.toDate());
        genericService.saveOrUpdate(doc);
        genericService.synchronize();

        // converstion from MST to UTC date advances registration date by one day.
        searchIndexService.flushToIndexes();
        DateRange dateRange = new DateRange();
        dateRange.setStart(searchDateTime.toDate());
        dateRange.setEnd(searchDateTime.plusDays(1).toDate());

        firstGroup().getRegisteredDates().add(dateRange);
        doSearch();
        assertThat(controller.getResults(), contains(doc));

        // if we advance the search begin/end by one day, we should not see it in search results
        dateRange.setStart(searchDateTime.plusDays(1).toDate());
        dateRange.setEnd(searchDateTime.plusDays(2).toDate());
        resetController();
        firstGroup().getRegisteredDates().add(dateRange);
        assertThat(controller.getResults(), not(contains(doc)));

        // if we decrement the search begin/end by one day, we should not see it in search results
        dateRange.setStart(searchDateTime.minusDays(1).toDate());
        dateRange.setEnd(searchDateTime.toDate());
        resetController();
        firstGroup().getRegisteredDates().add(dateRange);
        assertThat(controller.getResults(), not(contains(doc)));
    }

    @Test
    @Rollback
    public void testOtherKeywords() throws InstantiationException, IllegalAccessException, ParseException {
        // Create a document w/ some other keywords, then try to find that document in a search
        OtherKeyword ok = new OtherKeyword();
        ok.setLabel("testotherkeyword");
        assertNull("this label already taken. need a unique label", genericKeywordService.findByLabel(OtherKeyword.class, ok.getLabel()));
        Document document = createAndSaveNewInformationResource(Document.class);
        document.setTitle("otherkeywordtest");
        document.getOtherKeywords().add(ok);
        genericService.save(ok);
        genericService.save(document);
        searchIndexService.index(document);
        searchIndexService.index(ok);
        Long documentId = document.getId();
        assertNotNull(documentId);
        firstGroup().getOtherKeywords().add(ok.getLabel());
        controller.getResourceTypes().add(ResourceType.DOCUMENT);
        AbstractSearchControllerITCase.doSearch(controller, LookupSource.RESOURCE);
        Set<Indexable> results = new HashSet<Indexable>();
        results.addAll(controller.getResults());
        assertEquals("only expecting one result", 1L, controller.getResults().size());
        assertTrue("document containig our test keyword should be in results", results.contains(document));
        assertSearchPhrase(ok.getLabel());
    }

    @Test
    @Rollback
    public void testTemporalKeywords() throws ParseException, InstantiationException, IllegalAccessException {
        // Create a document w/ some temporal keywords, then try to find that document in a search
        TemporalKeyword tk = new TemporalKeyword();
        tk.setLabel("testtemporalkeyword");
        assertNull("this label already taken. need a unique label", genericKeywordService.findByLabel(TemporalKeyword.class, tk.getLabel()));
        Document document = createAndSaveNewInformationResource(Document.class);
        document.setTitle("temporal keyword test");
        document.getTemporalKeywords().add(tk);
        genericService.save(tk);
        genericService.save(document);
        searchIndexService.index(tk);
        searchIndexService.index(document);
        Long documentId = document.getId();
        assertNotNull(documentId);
        firstGroup().getTemporalKeywords().add(tk.getLabel());
        controller.getResourceTypes().add(ResourceType.DOCUMENT);
        AbstractSearchControllerITCase.doSearch(controller, LookupSource.RESOURCE);
        Set<Indexable> results = new HashSet<Indexable>();
        results.addAll(controller.getResults());
        assertEquals("only expecting one result", 1L, controller.getResults().size());
        assertTrue("document containig our test keyword should be in results", results.contains(document));
        assertSearchPhrase(tk.getLabel());
    }

    @Test
    @Rollback
    public void testGeoKeywords() throws InstantiationException, IllegalAccessException, ParseException {
        // Create a document w/ some temporal keywords, then try to find that document in a search
        GeographicKeyword gk = new GeographicKeyword();
        gk.setLabel("testgeographickeyword");
        assertNull("this label already taken. need a unique label", genericKeywordService.findByLabel(GeographicKeyword.class, gk.getLabel()));
        Document document = createAndSaveNewInformationResource(Document.class);
        document.setTitle("geographic keyword test");
        document.getGeographicKeywords().add(gk);
        genericService.save(gk);
        genericService.save(document);
        searchIndexService.index(gk);
        searchIndexService.index(document);
        Long documentId = document.getId();
        assertNotNull(documentId);
        firstGroup().getGeographicKeywords().add(gk.getLabel());
        controller.getResourceTypes().add(ResourceType.DOCUMENT);
        AbstractSearchControllerITCase.doSearch(controller, LookupSource.RESOURCE);
        Set<Indexable> results = new HashSet<Indexable>();
        results.addAll(controller.getResults());
        assertEquals("only expecting one result", 1L, controller.getResults().size());
        assertTrue("document containig our test keyword should be in results", results.contains(document));
        assertSearchPhrase(gk.getLabel());
    }

    private Document createDocumentWithContributorAndSubmitter() throws InstantiationException, IllegalAccessException {
        TdarUser submitter = new TdarUser("E", "deVos", "ecd@tdar.net");
        genericService.save(submitter);
        Document doc = createAndSaveNewInformationResource(Document.class, submitter);
        ResourceCreator rc = new ResourceCreator(new Person("K", "deVos", "kellyd@tdar.net"), ResourceCreatorRole.AUTHOR);
        genericService.save(rc.getCreator());
        // genericService.save(rc);
        doc.getResourceCreators().add(rc);
        genericService.saveOrUpdate(doc);
        searchIndexService.index(doc);
        return doc;
    }

    @Test
    @Rollback
    public void testSearchBySubmitterIds() throws InstantiationException, IllegalAccessException, ParseException {
        Document doc = createDocumentWithContributorAndSubmitter();
        Long submitterId = doc.getSubmitter().getId();
        assertFalse(submitterId == -1);
        firstGroup().getResourceCreatorProxies().add(new ResourceCreatorProxy(doc.getSubmitter(), ResourceCreatorRole.SUBMITTER));
        AbstractSearchControllerITCase.doSearch(controller, LookupSource.RESOURCE);
        assertTrue("only one result expected", 1 <= controller.getResults().size());
        assertTrue(controller.getResults().contains(doc));
    }

    @Test
    @Rollback
    public void testSearchContributorIds2() throws InstantiationException, IllegalAccessException, ParseException {
        Document doc = createDocumentWithContributorAndSubmitter();
        ResourceCreator contributor = doc.getResourceCreators().iterator().next();
        firstGroup().getResourceCreatorProxies().add(new ResourceCreatorProxy(contributor.getCreator(), contributor.getRole()));
        AbstractSearchControllerITCase.doSearch(controller, LookupSource.RESOURCE);
        assertEquals("only one result expected", 1L, controller.getResults().size());
        assertEquals(doc, controller.getResults().iterator().next());
    }

    public void assertSearchPhrase(String term) {
        logger.debug("term:{}\t search phrase:{}", term, controller.getSearchPhrase());
        assertTrue(String.format("looking for string '%s' in search phrase '%s'", term, controller.getSearchPhrase()),
                controller.getSearchPhrase().toLowerCase().contains(term.toLowerCase()));
    }

    @Test
    @Rollback
    public void testTitleSearch() throws InstantiationException, IllegalAccessException, ParseException {
        Document doc = createDocumentWithContributorAndSubmitter();
        String title = "the archaeology of class and war";
        doc.setTitle(title);
        genericService.saveOrUpdate(doc);
        searchIndexService.index(doc);
        firstGroup().getTitles().add(title);
        AbstractSearchControllerITCase.doSearch(controller, LookupSource.RESOURCE);
        logger.info("{}", controller.getResults());
        assertEquals("only one result expected", 1L, controller.getResults().size());
        assertEquals(doc, controller.getResults().iterator().next());
    }

    @Test
    @Rollback
    public void testLuceneOperatorInSearch() throws InstantiationException, IllegalAccessException, ParseException {
        Document doc = createDocumentWithContributorAndSubmitter();
        String title = "the archaeology of class ( AND ) war";
        doc.setTitle(title);
        genericService.saveOrUpdate(doc);
        searchIndexService.index(doc);
        firstGroup().getAllFields().add(title);
        AbstractSearchControllerITCase.doSearch(controller, LookupSource.RESOURCE);
        logger.info("{}", controller.getResults());
        assertEquals("only one result expected", 1L, controller.getResults().size());
        assertEquals(doc, controller.getResults().iterator().next());
    }

    @Test
    @Rollback
    public void testResourceCreatorPerson() {
        Person person = new Person("Bob", "Loblaw", null);
        genericService.save(person);
        Resource resource = constructActiveResourceWithCreator(person, ResourceCreatorRole.AUTHOR);
        logger.info("resource: {}", resource);
        reindex();
        logger.debug("user:{}   id:{}", person, person.getId());
        assertTrue("person id should be set - id:" + person.getId(), person.getId() != 1L);

        firstGroup().getResourceCreatorProxies().add(new ResourceCreatorProxy(person, ResourceCreatorRole.AUTHOR));

        doSearch();
        logger.info("{}", controller.getResults());
        assertTrue(String.format("expecting %s in results", resource), controller.getResults().contains(resource));
        assertEquals("should be one and only one result", 1, controller.getResults().size());
    }

    @Test
    @Rollback
    public void testResourceCreatorWithAnyRole() {
        Person person = new Person("Bob", "Loblaw", null);
        genericService.save(person);
        Resource resource = constructActiveResourceWithCreator(person, ResourceCreatorRole.AUTHOR);
        reindex();
        logger.debug("user:{}   id:{}", person, person.getId());
        assertTrue("person id should be set - id:" + person.getId(), person.getId() != 1L);
        firstGroup().getResourceCreatorProxies().add(new ResourceCreatorProxy(person, null));

        doSearch();
        assertTrue(String.format("expecting %s in results", resource), controller.getResults().contains(resource));
    }

    @Test
    @Rollback
    public void testBooleanSearch() throws InstantiationException, IllegalAccessException {
        Document doc1 = generateDocumentWithUser();
        Document doc2 = generateDocumentWithUser();
        GeographicKeyword istanbul = new GeographicKeyword();
        istanbul.setLabel(ISTANBUL);
        GeographicKeyword constantinople = new GeographicKeyword();
        constantinople.setLabel(CONSTANTINOPLE);
        genericKeywordService.save(istanbul);
        genericKeywordService.save(constantinople);
        doc1.getGeographicKeywords().add(istanbul);
        doc2.getGeographicKeywords().add(constantinople);
        genericService.saveOrUpdate(doc1);
        genericService.saveOrUpdate(doc2);
        evictCache();
        searchIndexService.index(doc1, doc2);
        searchIndexService.flushToIndexes();
        SearchParameters params = new SearchParameters();
        controller.getG().add(params);
        params.setAllFields(Arrays.asList(ISTANBUL, CONSTANTINOPLE));
        params.setOperator(Operator.OR);
        doSearch();
        assertTrue(controller.getResults().contains(doc1));
        assertTrue(controller.getResults().contains(doc2));
        logger.debug("results:{}", controller.getResults());
        resetController();
        controller.getG().add(params);
        params.setOperator(Operator.AND);
        doSearch();
        logger.debug("results:{}", controller.getResults());
        assertFalse(controller.getResults().contains(doc1));
        assertFalse(controller.getResults().contains(doc2));
    }

    @Test
    @Rollback(true)
    public void testCalDateSearch() throws InstantiationException, IllegalAccessException {
        Document exact = createDocumentWithDates(-1000, 1200);
        Document interior = createDocumentWithDates(-500, 1000);
        Document start = createDocumentWithDates(-1500, 1000);
        Document end = createDocumentWithDates(-500, 2000);
        Document before = createDocumentWithDates(-1300, -1100);
        Document after = createDocumentWithDates(1300, 2000);
        genericService.saveOrUpdate(start, end, interior, exact, after, before);
        searchIndexService.index(exact, interior, start, end, after, before);

        CoverageDate cd = new CoverageDate(CoverageType.CALENDAR_DATE, -1000, 1200);
        firstGroup().getCoverageDates().add(cd);
        doSearch();

        assertFalse("expecting multiple results", controller.getResults().isEmpty());
        assertTrue(controller.getResults().contains(start));
        assertTrue(controller.getResults().contains(end));
        assertTrue(controller.getResults().contains(interior));
        assertTrue(controller.getResults().contains(exact));
        assertFalse(controller.getResults().contains(before));
        assertFalse(controller.getResults().contains(after));
    }

    private Document createDocumentWithDates(int i, int j) throws InstantiationException, IllegalAccessException {
        Document document = createAndSaveNewInformationResource(Document.class);
        CoverageDate date = new CoverageDate(CoverageType.CALENDAR_DATE, i, j);
        document.getCoverageDates().add(date);
        genericService.saveOrUpdate(date);
        return document;
    }

    @Test
    @Rollback(true)
    public void testLegacyKeywordSearch() throws Exception {
        Document doc = createAndSaveNewInformationResource(Document.class);
        Project proj = createAndSaveNewProject("parent");
        doc.setProject(proj);
        Set<CultureKeyword> cultureKeywords = genericKeywordService.findOrCreateByLabels(CultureKeyword.class, Arrays.asList("iamaculturekeyword"));
        Set<SiteNameKeyword> siteNames = genericKeywordService.findOrCreateByLabels(SiteNameKeyword.class, Arrays.asList("thisisasitename"));
        Set<SiteTypeKeyword> siteTypes = genericKeywordService.findOrCreateByLabels(SiteTypeKeyword.class, Arrays.asList("asitetypekeyword"));

        doc.setCultureKeywords(cultureKeywords);
        doc.setSiteNameKeywords(siteNames);
        doc.setSiteTypeKeywords(siteTypes);
        genericService.saveOrUpdate(doc);
        reindex();

        controller.getUncontrolledCultureKeywords().add(cultureKeywords.iterator().next().getLabel());
        controller.setProjectionModel(ProjectionModel.HIBERNATE_DEFAULT);
        evictCache();
        searchIndexService.flushToIndexes();
        assertOnlyResultAndProject(doc);
        resetController();

        controller.setProjectionModel(ProjectionModel.HIBERNATE_DEFAULT);
        controller.getUncontrolledSiteTypeKeywords().add(siteTypes.iterator().next().getLabel());
        assertOnlyResultAndProject(doc);
        resetController();

        controller.setProjectionModel(ProjectionModel.HIBERNATE_DEFAULT);
        controller.getSiteNameKeywords().add(siteNames.iterator().next().getLabel());
        assertOnlyResultAndProject(doc);
    }

    @Test
    @Rollback(true)
    // TODO: modify this test to do additional checks on what we define as "good grammar", right now it only tests for a one-off bug (repetition)
    public void testAllFieldsSearchDescriptionGrammar() {
        String TEST_VALUE = "spam"; // damn vikings!
        controller.setQuery(TEST_VALUE);
        AbstractSearchControllerITCase.doSearch(controller, LookupSource.RESOURCE);
        for (int i = 0; i < 10; i++) {
            logger.debug("search phrase:{}", controller.getSearchPhrase());
        }
        int occurances = controller.getSearchPhrase().split(TEST_VALUE).length;
        assertTrue("search description should have gooder english than it currently does", occurances <= 2);
    }

    @Test
    @Rollback()
    // sparse collections like projects and collections should get partially hydrated when rendering the "refine" page
    public void testSparseObjectLoading() {
        String colname = "my fancy collection";
        Project proj = createAndSaveNewResource(Project.class);
        ResourceCollection coll = createAndSaveNewResourceCollection(colname);
        searchIndexService.index(coll);
        searchIndexService.index(proj);

        // simulate searchParamerters that represents a project at [0] and collection at [1]
        firstGroup().getProjects().add(sparseProject(proj.getId()));
        firstGroup().getCollections().add(null); // [0]
        firstGroup().getCollections().add(sparseCollection(coll.getId())); // [1]
        AbstractSearchControllerITCase.doSearch(controller, LookupSource.RESOURCE);

        // skeleton lists should have been loaded w/ sparse records...
        assertEquals(proj.getTitle(), firstGroup().getProjects().get(0).getTitle());
        assertEquals(colname, firstGroup().getCollections().get(1).getName());
    }

    @Test
    @Rollback()
    // sparse collections like projects and collections should get partially hydrated when rendering the "refine" page
    public void testSparseObjectNameLoading() {
        String colname = "my fancy collection";
        Project proj = createAndSaveNewResource(Project.class);
        ResourceCollection coll = createAndSaveNewResourceCollection(colname);
        searchIndexService.index(coll);
        proj.getResourceCollections().add(coll);
        searchIndexService.index(proj);

        // simulate searchParamerters that represents a project at [0] and collection at [1]
        // firstGroup().getProjects().add(new Project(null,proj.getName()));
        // firstGroup().getCollections().add(null); // [0]
        firstGroup().getCollections().add(new ResourceCollection(colname, null, null, null, true, null)); // [1]
        AbstractSearchControllerITCase.doSearch(controller, LookupSource.RESOURCE);

        // skeleton lists should have been loaded w/ sparse records...
        // assertEquals(proj.getTitle(), firstGroup().getProjects().get(0).getTitle());
        assertEquals(colname, firstGroup().getCollections().get(0).getName());
        assertTrue(controller.getResults().contains(proj));
        // assertEquals(proj.getId(), firstGroup().getProjects().get(0).getId());
        // assertEquals(coll.getId(), firstGroup().getCollections().get(1).getId());
    }

    @SuppressWarnings("deprecation")
    @Test
    @Rollback()
    // sparse collections like projects and collections should get partially hydrated when rendering the "refine" page
    public void testLookupObjectLoading() {
        String colname = "my fancy collection";
        Project proj = createAndSaveNewResource(Project.class);
        proj.setTitle(colname);
        Document doc1 = createAndSaveNewResource(Document.class);
        doc1.setProject(proj);
        genericService.saveOrUpdate(doc1);
        genericService.saveOrUpdate(proj);
        ResourceCollection coll = createAndSaveNewResourceCollection(colname);
        searchIndexService.index(doc1, proj);

        // simulate searchParamerters that represents a project at [0] and collection at [1]
        firstGroup().getProjects().add(new Project(-1L, colname));
        AbstractSearchControllerITCase.doSearch(controller, LookupSource.RESOURCE);

        // skeleton lists should have been loaded w/ sparse records...
        assertEquals(proj.getTitle(), firstGroup().getProjects().get(0).getTitle());
        assertTrue(controller.getResults().contains(doc1));
    }

    @Test
    @Rollback
    public void testPersonRoles() {
        assertTrue(controller.getRelevantPersonRoles().contains(ResourceCreatorRole.SUBMITTER));
        assertTrue(controller.getRelevantPersonRoles().contains(ResourceCreatorRole.UPDATER));
        assertFalse(controller.getRelevantPersonRoles().contains(ResourceCreatorRole.RESOURCE_PROVIDER));
    }

    @Test
    @Rollback
    public void testInstitutionRoles() {
        assertFalse(controller.getRelevantInstitutionRoles().contains(ResourceCreatorRole.SUBMITTER));
        assertFalse(controller.getRelevantInstitutionRoles().contains(ResourceCreatorRole.UPDATER));
        assertTrue(controller.getRelevantInstitutionRoles().contains(ResourceCreatorRole.RESOURCE_PROVIDER));
    }

    @Test
    public void testRefineSimpleSearch() {
        // simulate /search?query=this is a test. We expect the form to pre-populate with this search term
        String query = "this is a test";
        controller.setQuery(query);
        controller.advanced();
        assertTrue("first group should have one term", firstGroup().getAllFields().size() > 0);
        assertEquals("query should appear on first term", query, firstGroup().getAllFields().get(0));
    }

    @Test
    // if user gets to the results page via clicking on persons name from resource view page, querystring only contains person.id field. So before
    // rendering the 'refine your search' version of the search form the controller must inflate query components.
    public void testRefineSearchWithSparseProject() {
        Project persisted = createAndSaveNewProject("PROJECT TEST TITLE");
        Project sparse = new Project();
        // ensure the project is in
        evictCache();
        sparse.setId(persisted.getId());
        firstGroup().getProjects().add(sparse);
        controller.advanced();

        assertEquals("sparse project should have been inflated", persisted.getTitle(), firstGroup().getProjects().get(0).getTitle());
    }

    @Test
    @Rollback
    public void testRefineSearchWithSparseCollection() {
        ResourceCollection rc = createAndSaveNewResourceCollection("Mega Collection");
        ResourceCollection sparseCollection = new ResourceCollection();
        evictCache();
        long collectionId = rc.getId();
        assertThat(collectionId, greaterThan(0L));
        sparseCollection.setId(collectionId);
        firstGroup().getCollections().add(sparseCollection);

        assertThat(sparseCollection.getTitle(), equalTo(firstGroup().getCollections().get(0).getTitle()));
    }

    private void assertOnlyResultAndProject(InformationResource informationResource) {
        doSearch();
        assertEquals("expecting two results: doc and project", 2, controller.getResults().size());
        assertTrue("expecting resource in results", controller.getResults().contains(informationResource));
        assertTrue("expecting resource's project in results", controller.getResults().contains(informationResource.getProject()));
    }

    private Resource constructActiveResourceWithCreator(Creator creator, ResourceCreatorRole role) {
        try {
            Document doc = createAndSaveNewInformationResource(Document.class);
            ResourceCreator resourceCreator = new ResourceCreator(creator, role);
            doc.getResourceCreators().add(resourceCreator);
            return doc;
        } catch (Exception ignored) {
        }
        fail();
        return null;
    }

    protected boolean resultsContainId(Long id) {
        boolean found = false;
        for (Resource r_ : controller.getResults()) {
            Resource r = r_;
            logger.trace(r.getId() + " " + r.getResourceType());
            if (id.equals(r.getId())) {
                found = true;
            }
        }
        return found;
    }

    @Override
    protected void reindex() {
        evictCache();
        searchIndexService.purgeAll();
        searchIndexService.indexAll(getAdminUser(), Resource.class, Person.class, Institution.class, ResourceCollection.class);
    }

    protected void doSearch() {
        doSearch(false);
    }

    protected void doSearch(Boolean b) {
        controller.setProjectionModel(ProjectionModel.HIBERNATE_DEFAULT);
        AbstractSearchControllerITCase.doSearch(controller, LookupSource.RESOURCE, b);
        logger.info("search found: " + controller.getTotalRecords());
    }

    protected Long setupImage() {
        return setupImage(getUser());
    }

    protected Long setupImage(TdarUser user) {
        Image img = new Image();
        img.setTitle("precambrian Test");
        img.setDescription("image description");
        img.markUpdated(user);
        CultureKeyword label = genericKeywordService.findByLabel(CultureKeyword.class, "Folsom");
        CultureKeyword label2 = genericKeywordService.findByLabel(CultureKeyword.class, "Early Archaic");
        LatitudeLongitudeBox latLong = new LatitudeLongitudeBox(-117.101, 33.354, -117.124, 35.791);
        img.setLatitudeLongitudeBox(latLong);
        assertNotNull(label.getId());
        img.getCultureKeywords().add(label);
        img.getCultureKeywords().add(label2);
        img.setStatus(Status.DRAFT);
        genericService.save(img);
        genericService.save(latLong);
        Long imgId = img.getId();
        return imgId;
    }

}
