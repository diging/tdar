package org.tdar.core.service.processes;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.hibernate.ScrollableResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.collection.ResourceCollection;
import org.tdar.core.bean.entity.Creator;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.configuration.TdarConfiguration;
import org.tdar.core.service.GenericService;
import org.tdar.core.service.ResourceCollectionService;
import org.tdar.core.service.UrlService;
import org.tdar.core.service.resource.ResourceService;

import com.redfin.sitemapgenerator.ChangeFreq;
import com.redfin.sitemapgenerator.GoogleImageSitemapGenerator;
import com.redfin.sitemapgenerator.SitemapIndexGenerator;
import com.redfin.sitemapgenerator.WebSitemapGenerator;
import com.redfin.sitemapgenerator.WebSitemapUrl;

@Component
public class SitemapGeneratorProcess extends AbstractScheduledProcess {

    private static final long serialVersionUID = 561910508692901053L;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private transient ResourceService resourceService;
    @Autowired
    private transient GenericService genericService;
    @Autowired
    private transient ResourceCollectionService resourceCollectionService;
    @Autowired
    private transient UrlService urlService;
    private TdarConfiguration CONFIG = TdarConfiguration.getInstance();

    private boolean run = false;

    @Override
    public void execute() {
        run = true;
        File dir = new File(CONFIG.getSitemapDir());
        try {
            FileUtils.deleteDirectory(dir);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        dir.mkdirs();
        WebSitemapGenerator wsg;
        GoogleImageSitemapGenerator gisg;
        SitemapIndexGenerator sig;
        int total = 0;
        int totalImages = 0;
        boolean imageSitemapGeneratorEnabled = true;
        try {
            wsg = WebSitemapGenerator.builder(CONFIG.getBaseUrl(), dir).gzip(true).allowMultipleSitemaps(true).build();
            gisg = GoogleImageSitemapGenerator.builder(CONFIG.getBaseUrl(), dir).gzip(true).allowMultipleSitemaps(true).fileNamePrefix("image_sitemap").build();
            sig = new SitemapIndexGenerator(CONFIG.getBaseUrl(), new File(dir, "sitemap_index.xml"));
            // wsg.set
            Integer totalResource = genericService.countActive(Resource.class).intValue();
            total += totalResource;

            logger.info("({}) resources in sitemap", totalResource);
            ScrollableResults allScrollable = resourceService.findAllActiveScrollableForSitemap();
            while (allScrollable.next()) {
                Resource object = (Resource) allScrollable.get(0);
                String url = UrlService.absoluteUrl(object);
                addUrl(wsg, url);
            }

            if (imageSitemapGeneratorEnabled) {
                totalImages = resourceService.findAllResourcesWithPublicImagesForSitemap(gisg);
            }

            logger.info("({}) images in sitemap", totalImages);

            ScrollableResults activeCreators = genericService.findAllActiveScrollable(Creator.class);
            int totalCreator = 0;
            while (activeCreators.next()) {
                Creator creator = (Creator) activeCreators.get(0);
                if (!creator.isBrowsePageVisible()) {
                    continue;
                }
                String url = UrlService.absoluteUrl(creator);
                addUrl(wsg, url);
                totalCreator++;
                if (totalCreator % 500 == 0) {
                    genericService.clearCurrentSession();
                }
            }
            logger.info("({}) creators in sitemap", totalCreator);
            total += totalCreator;

            ScrollableResults activeCollections = genericService.findAllScrollable(ResourceCollection.class);
            int totalCollections = 0;
            while (activeCollections.next()) {
                ResourceCollection collection = (ResourceCollection) activeCollections.get(0);
                if (collection.isInternal() || collection.isHidden()) {
                    continue;
                }
                String url = UrlService.absoluteUrl(collection);
                addUrl(wsg, url);
                totalCollections++;
                if (totalCollections % 500 == 0) {
                    genericService.clearCurrentSession();
                }

            }
            logger.info("({}) collections in sitemap", totalCollections);
            total += totalCollections;

            if (total > 0) {
                wsg.write();
            }
            if (totalImages > 0) {
                gisg.write();
            }

            Date date = new Date();
            for (File file : dir.listFiles()) {
                if (file.getName().equals("sitemap_index.xml")) {
                    continue;
                }
                File sitemap1 = new File(dir, "sitemap1.xml.gz");
                if (file.getName().equals("sitemap.xml.gz") && sitemap1.exists()) {
                    continue;
                }
                File imageSitemap1 = new File(dir, "image_sitemap1.xml.gz");
                if (file.getName().equals("image_sitemap.xml.gz") && imageSitemap1.exists()) {
                    continue;
                }

                sig.addUrl(String.format("%s/%s/%s", CONFIG.getBaseUrl(), "hosted/sitemap", file.getName()), date);
            }

            sig.write();
            // wsg.addUrl("http://www.example.com/index.html"); // repeat multiple times
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void addUrl(WebSitemapGenerator wsg, String url) throws MalformedURLException {
        wsg.addUrl(new WebSitemapUrl.Options(url).changeFreq(ChangeFreq.WEEKLY).build());
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getDisplayName() {
        return "SiteMap generator";
    }

    @Override
    public boolean isCompleted() {
        return run;
    }

    @Override
    public boolean isSingleRunProcess() {
        return false;
    }

}
