package org.tdar.core.service;

import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tdar.core.bean.Obfuscatable;
import org.tdar.core.bean.entity.Person;
import org.tdar.core.dao.GenericDao;
import org.tdar.core.service.external.AuthenticationAndAuthorizationService;

@Service
@Transactional(readOnly = true)
public class ObfuscationService {

    protected static final transient Logger logger = LoggerFactory.getLogger(ObfuscationService.class);

    @Autowired
    private GenericDao genericDao;

    @Autowired
    private AuthenticationAndAuthorizationService authService;
    
    @Transactional(readOnly = true)
    public void obfuscate(Collection<? extends Obfuscatable> targets, Person user) {
        for (Obfuscatable target : targets) {
            obfuscate(target, user);
        }
    }

    @Transactional(readOnly = true)
    public void obfuscate(Obfuscatable target, Person user) {
        /*
         * we're going to manipulate the record, so, we detach the items from the session before
         * we muck with them... then we'll pass it on. If we don't detach, then hibernate may try
         * to persist the changes.
         * Before we detach from the session, though, we have to make sure any lazily-initialized
         * properties and collections are initialized, because without a session, these properties
         * can't be initialized. So first we'll serialize the object (and discard the serialization),
         * purely as a means of fully loading the properties for the final serialization later.
         */

        if (target == null || target.isObfuscated()) {
            logger.debug("target is already obfuscated or null: {} ({}}", target, user);
            return;
        }

//        if (target instanceof Resource && authService.canViewConfidentialInformation(user, (Resource)target)) {
//            return;
//        }

        if (authService.isEditor(user)) {
            logger.debug("user is editor: {} ({}}", target, user);
            return;
        }
        
        // don't obfuscate someone for themself
        if (target instanceof Person && ObjectUtils.equals(user, (Person)target)) {
            logger.info("not obfuscating person: {}", user);
            return;
        }

        genericDao.markReadOnly(target);
        List<Obfuscatable> obfuscateList = handleObfuscation(target);
        if (CollectionUtils.isNotEmpty(obfuscateList)) {
            for (Obfuscatable subTarget : obfuscateList) {
                obfuscate(subTarget, user);
            }
        }
    }

    private List<Obfuscatable> handleObfuscation(Obfuscatable target) {
        logger.trace("obfuscating: {} [{}]", target.getClass(), target.getId());
        target.setObfuscated(true);
        return target.obfuscate();
    }
    

}
