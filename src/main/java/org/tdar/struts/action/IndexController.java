package org.tdar.struts.action;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Actions;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.cache.HomepageFeaturedItemCache;
import org.tdar.core.bean.cache.HomepageGeographicKeywordCache;
import org.tdar.core.bean.cache.HomepageResourceCountCache;
import org.tdar.core.bean.resource.InformationResource;
import org.tdar.core.bean.resource.Project;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.service.ObfuscationService;
import org.tdar.core.service.RssService;
import org.tdar.core.service.external.AuthorizationService;
import org.tdar.core.service.resource.ResourceService;
import org.tdar.struts.interceptor.annotation.HttpOnlyIfUnauthenticated;

import com.sun.syndication.feed.synd.SyndEntry;

/**
 * $Id$
 * 
 * <p>
 * Action for the root namespace.
 * 
 * 
 * @author <a href='mailto:Allen.Lee@asu.edu'>Allen Lee</a>
 * @version $Rev$
 */
@Namespace("/")
@ParentPackage("default")
@Component
@Scope("prototype")
@Results({
        @Result(name = "authenticated", type = "redirect", location = "/")
})
public class IndexController extends AuthenticationAware.Base {
    private static final long serialVersionUID = -9216882130992021384L;

    private Project featuredProject;

    private List<HomepageResourceCountCache> homepageResourceCountCache = new ArrayList<HomepageResourceCountCache>();
    private List<Resource> featuredResources = new ArrayList<Resource>();
    private HashMap<String, HomepageGeographicKeywordCache> worldMapData = new HashMap<>();

    private String sitemapFile = "sitemap_index.xml";

    
    @Autowired
    private ResourceService resourceService;

    @Autowired
    private ObfuscationService obfuscationService;
    @Autowired
    private transient AuthorizationService authorizationService;

    @Autowired
    private RssService rssService;

    private List<SyndEntry> rssEntries;

    @HttpOnlyIfUnauthenticated
    @Actions({
            @Action(value = "page-not-found", results = { @Result(name = ERROR, type = "freemarkerhttp",
                    location = "/WEB-INF/content/errors/page-not-found.ftl", params = { "status", "404" }) }),
            @Action(value = "not-found", results = { @Result(name = ERROR, type = "freemarkerhttp", location = "/WEB-INF/content/errors/page-not-found.ftl",
                    params = { "status", "404" }) }),
            @Action(value = "gone", results = { @Result(name = ERROR, type = "freemarkerhttp", location = "/WEB-INF/content/errors/resource-deleted.ftl",
                    params = { "status", "410" }) }),
            @Action(value = "unauthorized", results = { @Result(name = ERROR, type = "freemarkerhttp", location = "/WEB-INF/content/errors/unauthorized.ftl",
                    params = { "status", "401" }) }),
            @Action(value = "access-denied", results = { @Result(name = ERROR, type = "freemarkerhttp", location = "/WEB-INF/content/errors/access-denied.ftl",
                    params = { "status", "403" }) }),
            @Action(value = "invalid-token", results = { @Result(name = ERROR, type = "freemarkerhttp", location = "/WEB-INF/content/errors/double-submit.ftl",
                    params = { "status", "500" }) })
    })
    public String error() {
        return ERROR;
    }

    @HttpOnlyIfUnauthenticated
    @Override
    @Actions({
            @Action("contact"),
            @Action("credit"),
            @Action(value = "opensearch", results = {
                    @Result(name = SUCCESS, location = "opensearch.ftl", type = FREEMARKER, params = { "contentType", "application/xml" })
            }),
            @Action(value = "robots", results = {
                    @Result(name = SUCCESS, location = "robots.ftl", type = FREEMARKER, params = { "contentType", "text/plain" })
            })
    })
    public String execute() {
        File file = new File(getTdarConfiguration().getSitemapDir(), sitemapFile);
        if (!file.exists()) {
            setSitemapFile("sitemap1.xml.gz");
        }

        return SUCCESS;
    }

    @Action(value = "about", results = { @Result(name = SUCCESS, location = "about.ftl") })
    @HttpOnlyIfUnauthenticated
    public String about() {
        worldMap();

        try {
            setRssEntries(rssService.parseFeed(new URL(getTdarConfiguration().getNewsRssFeed())));
        } catch (Exception e) {
            getLogger().warn("RssParsingException happened", e);
        }
        featuredItems();
        resourceStats();
        return SUCCESS;
    }

    @Action(value = "map", results = { @Result(name = SUCCESS, location = "map.ftl", type = FREEMARKER, params = { "contentType", "text/html" }) })
    public String worldMap() {
        resourceService.setupWorldMap(worldMapData);
        return SUCCESS;
    }

    @Action(value = "featured", results = { @Result(name = SUCCESS, location = "featured.ftl", type = FREEMARKER,
            params = { "contentType", "text/html" }) })
    public String featuredItems() {
        try {
            for (HomepageFeaturedItemCache cache : getGenericService().findAllWithL2Cache(HomepageFeaturedItemCache.class)) {
                Resource key = cache.getKey();
                if (key instanceof InformationResource) {
                    authorizationService.applyTransientViewableFlag(key, null);
                }
                if (getTdarConfiguration().obfuscationInterceptorDisabled()) {
                    obfuscationService.obfuscate(key, getAuthenticatedUser());
                }
                getFeaturedResources().add(key);
            }
        } catch (IndexOutOfBoundsException ioe) {
            getLogger().debug("no featured resources found");
        }
        return SUCCESS;
    }

    @Action(value = "resourceGraph", results = { @Result(name = SUCCESS, location = "resourceGraph.ftl", type = FREEMARKER,
            params = { "contentType", "text/html" }) })
    public String resourceStats() {
        setHomepageResourceCountCache(getGenericService().findAllWithL2Cache(HomepageResourceCountCache.class));
        Iterator<HomepageResourceCountCache> iterator = homepageResourceCountCache.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getResourceType().isSupporting()) {
                iterator.remove();
            }
        }
        return SUCCESS;
    }

    public Project getFeaturedProject() {
        return featuredProject;
    }

    public void setFeaturedProject(Project featuredProject) {
        this.featuredProject = featuredProject;
    }

    public List<HomepageResourceCountCache> getHomepageResourceCountCache() {
        return homepageResourceCountCache;
    }

    public void setHomepageResourceCountCache(List<HomepageResourceCountCache> homepageResourceCountCache) {
        this.homepageResourceCountCache = homepageResourceCountCache;
    }

    public List<Resource> getFeaturedResources() {
        return featuredResources;
    }

    public void setFeaturedResources(List<Resource> featuredResources) {
        this.featuredResources = featuredResources;
    }

    public List<SyndEntry> getRssEntries() {
        return rssEntries;
    }

    public void setRssEntries(List<SyndEntry> rssEntries) {
        this.rssEntries = rssEntries;
    }

    public HashMap<String, HomepageGeographicKeywordCache> getWorldMapData() {
        return worldMapData;
    }

    public void setWorldMapData(HashMap<String, HomepageGeographicKeywordCache> worldMapData) {
        this.worldMapData = worldMapData;
    }


    public String getSitemapFile() {
        return sitemapFile;
    }

    public void setSitemapFile(String sitemapFile) {
        this.sitemapFile = sitemapFile;
    }

}
