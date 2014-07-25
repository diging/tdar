package org.tdar.web.functional;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jimdevos on 7/1/14.
 */
public class ByLabelText extends By {

    String labelText;

    Logger logger = LoggerFactory.getLogger(getClass());

    //todo: consider case insensitive search  (see http://stackoverflow.com/a/8474552 for clever example)
    public ByLabelText(String labelText) {
        if(StringUtils.isEmpty(labelText)) {
            throw new IllegalArgumentException("Cannot find elements when label text is blank or null");
        }
        this.labelText = labelText;


    }

    @Override
    public List<WebElement> findElements(SearchContext context) {

        //case insensitive search for label with specified text.  e.g. //label/text()[contains(translate(., 'First Name', 'first name'), 'first name')]
        //String xpath = String.format("//label/text()[contains('%s', '%s')]", labelText);
        String xpath = String.format("//label/text()[contains(translate(., '%1$s', '%2$s'), '%2$s')]/..", escape(labelText.toUpperCase()), escape(labelText.toLowerCase()));
        List<WebElement> labels = context.findElements(By.xpath(xpath));
        logger.debug("xpath is:{}   labels found:{}", xpath, labels );
        List<WebElement> results = new ArrayList<>();
        for(WebElement label : labels) {
            String elementId = label.getAttribute("for");
            //label refers to element explicitly via "for" attribute.
            if(elementId != null) {
                WebElement element = context.findElement(By.id(elementId));
                results.add(element);

            //label refers to element implicitly because element is childNode
            } else {
                List<WebElement> children = label.findElements(By.cssSelector("input,select,textarea"));
                results.addAll(children);
            }
        }
        return results;
    }

    //trivial escaping for values included in xpath query.  Not perfect, but hopefully sufficient for most labels
    private String escape(String val) {
        return val.replaceAll("'", "''").replaceAll("\"", "\\\"");
    }
}
