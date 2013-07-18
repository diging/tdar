package org.tdar.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tdar.core.configuration.ConfigurationAssistant;

/**
 * $Id$
 * 
 * Configuration file for various tDAR webapp properties.
 * 
 * FIXME: convert to a Spring managed bean (and by extension, Filestore)
 * 
 * @author <a href='mailto:Allen.Lee@asu.edu'>Allen Lee</a>
 * @version $Rev$
 */
public class TestConfiguration {
    @SuppressWarnings("unused")
    private final transient static Logger logger = LoggerFactory.getLogger(TestConfiguration.class);

    private ConfigurationAssistant assistant;
    private final static TestConfiguration INSTANCE = new TestConfiguration();

    private String configurationFile;

    private TestConfiguration() {
        this(System.getProperty("test.config", "test.properties"));
    }

    /*
     * Do not use this except for via the @MultipleTdarConfigurationRunner
     */
    @Deprecated
    public void setConfigurationFile(String configurationFile) {
        assistant = new ConfigurationAssistant();
        assistant.loadProperties(configurationFile);
        this.configurationFile = configurationFile;
    }

    public String getConfigurationFile() {
        return configurationFile;
    }

    private TestConfiguration(String configurationFile) {
        setConfigurationFile(configurationFile);
    }

    public static TestConfiguration getInstance() {
        return INSTANCE;
    }

    public String getHostName() {
        return assistant.getStringProperty("host.name", "localhost");
    }

    public boolean isHttpsEnabled() {
        return assistant.getBooleanProperty("https.enabled", true);
    }

    public int getHttpsPort() {
        return assistant.getIntProperty("https.port", 8143);
    }

    public int getPort() {
        return assistant.getIntProperty("http.port", 8180);
    }

    public String getAdminUsername() {
        return assistant.getStringProperty("admin.username", "admin");
    }

    public String getAdminPassword() {
        return assistant.getStringProperty("admin.password", "admin");
    }

    public Long getAdminUserId() {
        return assistant.getLongProperty("admin.id", 8093L);
    }

    public String getUsername() {
        return assistant.getStringProperty("user.username", "test@tdar.org");
    }

    public String getPassword() {
        return assistant.getStringProperty("user.password", "test");
    }

    public Long getUserId() {
        return assistant.getLongProperty("user.id", 8092L);
    }

    public String getEditorUsername() {
        return assistant.getStringProperty("editor.username", "editor");
    }

    public String getEditorPassword() {
        return assistant.getStringProperty("editor.password", "editor");
    }

    public Long getEditorUserId() {
        return assistant.getLongProperty("editor.id", 8093L);
    }

    public String getBaseUrl() {
        return String.format("http://%s:%s/", getHostName(), getPort());
    }

    public String getUsername(String user) {
        if (StringUtils.isBlank(user)) {
            return getUsername();
        }
        return user;
    }

    public String getPassword(String pass) {
        if (StringUtils.isBlank(pass)) {
            return getPassword();
        }
        return pass;
    }

}
