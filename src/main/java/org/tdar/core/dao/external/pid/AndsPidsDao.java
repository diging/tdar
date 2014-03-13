package org.tdar.core.dao.external.pid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.bean.resource.Status;
import org.tdar.core.configuration.ConfigurationAssistant;
import org.tdar.core.exception.TdarRecoverableRuntimeException;
import org.tdar.core.service.processes.DoiProcess;
import org.xml.sax.SAXException;

import au.csiro.pidclient.AndsPidClient;
import au.csiro.pidclient.AndsPidClient.HandleType;
import au.csiro.pidclient.AndsPidResponse;
import au.csiro.pidclient.business.AndsPidIdentity;
import au.csiro.pidclient.business.AndsPidResponseProperty;

/**
 * @author Nuwan Goonasekera
 */
@Service
public class AndsPidsDao implements ExternalIDProvider {

    // DOCUMENTATION: http://andspidclient.sourceforge.net/ and http://services.ands.org.au/documentation/pids/pids_text.pdf
    private static final String DOI_PROVIDER_HOSTNAME = "doi.provider.hostname";
    private static final String DOI_PROVIDER_PORT = "doi.provider.port";
    private static final String DOI_PROVIDER_SERVICE_PATH = "doi.provider.path";
    private static final String DOI_PROVIDER_APPID = "doi.provider.appid";
    private static final String DOI_PROVIDER_IDENTIFIER = "doi.provider.identifier";
    private static final String DOI_PROVIDER_AUTHDOMAIN = "doi.provider.authdomain";

    public static final String DATACITE_PROFILE_NAME = "datacite";
    public static final String DATACITE_TITLE = DATACITE_PROFILE_NAME + ".title";
    public static final int DATACITE_TITLE_INDEX = 1;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    AndsPidClient pidsClient = new AndsPidClient();
    private ConfigurationAssistant assistant = new ConfigurationAssistant();
    private String configIssue = "";
    private boolean isEnabled = true; // the happy case

    public AndsPidsDao() {
        try {
            assistant.loadProperties("andspids.properties");
        } catch (Throwable t) {
            isEnabled = false;
            configIssue = t.getMessage();
        }
    }

    @Override
    public boolean isConfigured() {
        if (isEnabled && StringUtils.isNotBlank(getDOIProviderHostname()) && (getDOIProviderPort() > 0) &&
                StringUtils.isNotBlank(getDOIProviderServicePath()) && StringUtils.isNotBlank(getDOIProviderAppId()) &&
                StringUtils.isNotBlank(getDOIProviderIdentifier()) && StringUtils.isNotBlank(getDOIProviderAuthDomain())) {
            return true;
        }
        logger.debug("a required parameter for the AndsPidsDao was not provided. " + configIssue);
        return false;
    }

    @Override
    public boolean connect() {
        logger.debug("Connecting to ANDS Pids Service: {}:{}{}", new Object[] { getDOIProviderHostname(), getDOIProviderPort(), getDOIProviderServicePath() });
        pidsClient.setPidServiceHost(getDOIProviderHostname());
        pidsClient.setPidServicePort(getDOIProviderPort());
        pidsClient.setPidServicePath(getDOIProviderServicePath());
        AndsPidIdentity identity = new AndsPidIdentity(getDOIProviderAppId(), getDOIProviderIdentifier(), getDOIProviderAuthDomain());
        pidsClient.setRequestorIdentity(identity);
        return true;
    }

    @Override
    public boolean logout() {
        logger.info("ANDS Pids logout: {} ", getDOIProviderHostname());
        // do nothing
        return true;
    }

    @Override
    public Map<String, String> create(Resource r, String resourceUrl) throws IOException {
        Map<String, String> typeMap = new HashMap<>();
        AndsPidResponse response;
       try {
            response = pidsClient.mintHandleFormattedResponse(HandleType.URL, 0, resourceUrl);
            String handle = response.getHandle();
            if (response.isSuccess()) {
                typeMap.put(DoiProcess.DOI_KEY, handle);
                if (r.getStatus() == Status.ACTIVE) {
                    pidsClient.addValueByIndex(handle, DATACITE_TITLE_INDEX, HandleType.DESC, r.getTitle());
                }
            } else {
                String message = "The PID Creation was not successful for: " + resourceUrl + " [ " + response.getMessage() + " ]";
                logger.debug(message);
                throw new TdarRecoverableRuntimeException(message);
            }
        } catch (IllegalStateException | IllegalArgumentException | XPathExpressionException | ParserConfigurationException | SAXException e) {
            List<String> vals = new ArrayList<>();
            vals.add(resourceUrl);
            throw new TdarRecoverableRuntimeException("andsPidDao.creation_fail", e,vals);
        }
        return typeMap;
    }

    @Override
    public Map<String, String> getMetadata(String identifier) throws IOException {
        Map<String, String> typeMap = new HashMap<>();
        try {
            typeMap.put(DoiProcess.DOI_KEY, identifier);
            AndsPidResponse response = pidsClient.getHandleFormattedResponse(identifier);
            List<AndsPidResponseProperty> properties = response.getProperties();
            typeMap.put(DATACITE_TITLE, properties.get(DATACITE_TITLE_INDEX - 1).getValue());
        } catch (Exception e) {
            logger.debug("could not get properties for handle: {}", identifier);
            throw new ClientProtocolException(e);
        }
        logger.trace("result: {}", typeMap);
        return typeMap;
    }

    @Override
    public Map<String, String> modify(Resource r, String resourceUrl, String identifier) throws IOException {
        Map<String, String> typeMap = new HashMap<>();
        try {
            String handle = identifier;
            typeMap.put(DoiProcess.DOI_KEY, handle);
            if (r.getStatus() == Status.ACTIVE) {
                pidsClient.modifyValueByIndex(handle, DATACITE_TITLE_INDEX, r.getTitle());
            }
        } catch (Exception e) {
            logger.debug("could not modify handle for resource: {}", resourceUrl);
            List<String> vals = new ArrayList<>();
            vals.add(resourceUrl);
            throw new TdarRecoverableRuntimeException("andsPidDao.update_fail", e,vals);
        }
        return typeMap;
    }

    @Override
    public Map<String, String> delete(Resource r, String resourceUrl, String identifier) throws IOException {
        Map<String, String> typeMap = new HashMap<>();
        try {
            String handle = identifier;
            typeMap.put(DoiProcess.DOI_KEY, handle);
            if (r.getStatus() != Status.ACTIVE)
                pidsClient.deleteValueByIndex(handle, 1);
        } catch (Exception e) {
            logger.debug("could not delete handle for resource: {}", resourceUrl);
            List<String> vals = new ArrayList<>();
            vals.add(resourceUrl);
            throw new TdarRecoverableRuntimeException("andsPidDao.delete_fail", e,vals);
        }
        return typeMap;
    }

    /**
     * @return
     */
    public String getDOIProviderAuthDomain() {
        return assistant.getStringProperty(DOI_PROVIDER_AUTHDOMAIN);
    }

    /**
     * @return
     */
    public String getDOIProviderIdentifier() {
        return assistant.getStringProperty(DOI_PROVIDER_IDENTIFIER);
    }

    /**
     * @return
     */
    public String getDOIProviderAppId() {
        return assistant.getStringProperty(DOI_PROVIDER_APPID);
    }

    /**
     * @return
     */
    public String getDOIProviderServicePath() {
        return assistant.getStringProperty(DOI_PROVIDER_SERVICE_PATH);
    }

    /**
     * @return
     */
    public int getDOIProviderPort() {
        return assistant.getIntProperty(DOI_PROVIDER_PORT);
    }

    /**
     * @return
     */
    public String getDOIProviderHostname() {
        return assistant.getStringProperty(DOI_PROVIDER_HOSTNAME);
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

}
