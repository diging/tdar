/**
 * 
 */
package org.tdar.web;

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.tdar.core.bean.entity.Person;
import org.tdar.core.bean.entity.TdarUser;
import org.tdar.core.bean.resource.Document;
import org.tdar.core.bean.resource.Resource;

/**
 * @author Adam Brin
 * 
 */
public abstract class AbstractAdminAuthenticatedWebTestCase extends AbstractAuthenticatedWebTestCase {

    @Before
    @Override
    public void setUp() {
        loginAdmin();
    }

    public void createTestCollection(String name, String desc, List<? extends Resource> someResources) {
        assertNotNull(genericService);
        gotoPage("/collection/add");
        setInput("resourceCollection.name", name);
        setInput("resourceCollection.description", desc);

        for (int i = 0; i < someResources.size(); i++) {
            Resource resource = someResources.get(i);
            // FIXME: we don't set id's in the form this way but setInput() doesn't understand 'resources.id' syntax. fix it so that it can.
            String fieldName = "toAdd[" + i + "]";
            String fieldValue = "" + resource.getId();
            logger.debug("setting  fieldName:{}\t value:{}", fieldName, fieldValue);
            createInput("hidden", fieldName, fieldValue);
        }
        submitForm();
    }

    protected List<? extends Resource> getSomeResources() {
        List<? extends Resource> alldocs = genericService.findAll(Document.class);
        List<? extends Resource> somedocs = alldocs.subList(0, Math.min(10, alldocs.size())); // get no more than 10 docs, pls
        return somedocs;
    }

    protected List<TdarUser> getSomeUsers() {
        // let's only get authorized users
        List<TdarUser> allRegisteredUsers = entityService.findAllRegisteredUsers();
        List<TdarUser> someRegisteredUsers = allRegisteredUsers.subList(0, Math.min(10, allRegisteredUsers.size()));
        return someRegisteredUsers;
    }

    protected List<Person> getSomePeople() {
        List<Person> allNonUsers = entityService.findAll();
        allNonUsers.removeAll(entityService.findAllRegisteredUsers());
        List<Person> someNonUsers = allNonUsers.subList(0, Math.min(10, allNonUsers.size()));
        logger.debug("non-users: {}", someNonUsers);
        if (CollectionUtils.isEmpty(someNonUsers)) {
            Assert.fail("expecting users");
        }
        return someNonUsers;
    }

}
