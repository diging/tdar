package org.tdar.core.dao.external.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.tdar.core.bean.entity.TdarUser;
import org.tdar.core.configuration.TdarConfiguration;
import org.tdar.core.dao.external.auth.AuthenticationResult.AuthenticationResultType;
import org.tdar.core.service.EntityService;

/*
 * This provider is designed specifically for testing where no valid connection to an external service
 * is available.  Great for testing* and bascially nothing else. DO NOT USE IN PRODUCTION**
 *
 * To authenticate: use a valid username, but for the password field, use the name of a tdar membership group name
 * (e.g.  'tdar-admins', 'tdar-editors', 'tdar-users').  The provider will authenticate the user and endow that user
 * with the permissions indicated by the groupname.
 *
 * *As you might expect,  authentication-tests will probably fail
 * ** This class has failsafes to prevent use in production.  Don't rely on them.
 */
public class MockAuthenticationProvider extends BaseAuthenticationProvider {
    private Logger logger = LoggerFactory.getLogger(getClass());

    private Map<String, String> users = new ConcurrentHashMap<String, String>();

    public MockAuthenticationProvider() {
        logger.debug("is prod?: {}", TdarConfiguration.getInstance().isProductionEnvironment());
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public AuthenticationResult authenticate(HttpServletRequest request, HttpServletResponse response, String name,
            String password) {
        if (!isEnabled() || !isConfigured()) {
            return new AuthenticationResult(AuthenticationResultType.REMOTE_EXCEPTION);
        }
        if (TdarConfiguration.getInstance().isProductionEnvironment()) {
            logger.error("Mock Authentication is not allowed in production.");
            return new AuthenticationResult(AuthenticationResultType.REMOTE_EXCEPTION);
        }

        AuthenticationResult result = new AuthenticationResult(AuthenticationResultType.ACCOUNT_DOES_NOT_EXIST);

        logger.debug("trying to authenticate:: user: {}  groupname:{}", name, password);
        if (users.containsKey(name)) {
            TdarGroup group = TdarGroup.fromString(password);
            logger.debug("user found:{}  group:{}", name, group);
            users.put(name, group.getGroupName());
            result.setType(AuthenticationResultType.VALID);
            if (group == TdarGroup.UNAUTHORIZED) {
                result.setType(AuthenticationResultType.INVALID_PASSWORD);
            }
        } else {
            logger.debug("user not found: {}", name);
        }
        return result;
    }

    @Override
    public boolean isAuthenticated(HttpServletRequest request, HttpServletResponse response) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public AuthenticationResult addUser(TdarUser person, String password, TdarGroup... groups) {
        if (users.containsKey(person.getEmail())) {
            return new AuthenticationResult(AuthenticationResultType.REMOTE_EXCEPTION);
        } else {
            users.put(person.getEmail(), password);
            return new AuthenticationResult(AuthenticationResultType.VALID);
        }
    }

    @Override
    public boolean deleteUser(TdarUser person) {
        try {
            users.remove(person.getEmail());
            return true;
        } catch (Exception e) {
            logger.debug("{}", e);
        }
        return false;
    }

    @Override
    public void resetUserPassword(TdarUser person) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateUserPassword(TdarUser person, String password) {
        users.put(person.getEmail(), password);
    }

    @Override
    public String[] findGroupMemberships(TdarUser person) {
        TdarGroup group = TdarGroup.fromString(users.get(person.getUsername()));
        logger.debug("group membership request: name:{}   groupname:{},    group:{}", new Object[] { person.getUsername(), users.get(person.getUsername()),
                group });
        List<String> toReturn = new ArrayList<String>();
        switch (group) {
            case TDAR_ADMIN:
                toReturn.add(TdarGroup.TDAR_ADMIN.getGroupName());
            case TDAR_EDITOR:
                toReturn.add(TdarGroup.TDAR_EDITOR.getGroupName());
            case TDAR_USERS:
                toReturn.add(TdarGroup.TDAR_USERS.getGroupName());
                break;
            default:
                break;
        }
        String[] result = toReturn.toArray(new String[0]);
        logger.debug("group membership request: name:{}   groupname:{},  group:{},  membership:[{}]",
                new Object[] { person.getUsername(), users.get(person.getUsername()), group, result });
        return result;
    }

    @Override
    public String getPasswordResetURL() {
        // TODO Auto-generated method stub
        return null;
    }

    @Autowired
    public void setEntityService(EntityService entityService) {
        if (TdarConfiguration.getInstance().isProductionEnvironment()) {
            logger.info("Mock Authentication is not allowed in production. System will not load mock user db");
            return;
        }
        List<TdarUser> registeredUsers = entityService.findAllRegisteredUsers();
        for (TdarUser user : registeredUsers) {
            users.put(user.getUsername(), user.getUsername());
        }
    }
}
