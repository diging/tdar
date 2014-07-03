package org.tdar.core.service;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.lang.ObjectUtils;
import org.atteo.evo.inflector.English;
import org.hibernate.ScrollableResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tdar.core.bean.HasLabel;
import org.tdar.core.bean.Localizable;
import org.tdar.core.bean.Persistable;
import org.tdar.core.bean.collection.ResourceCollection;
import org.tdar.core.bean.entity.AuthorizedUser;
import org.tdar.core.bean.entity.Dedupable;
import org.tdar.core.bean.entity.Institution;
import org.tdar.core.bean.entity.Person;
import org.tdar.core.bean.entity.ResourceCreator;
import org.tdar.core.bean.keyword.Keyword;
import org.tdar.core.bean.resource.InformationResource;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.bean.resource.Status;
import org.tdar.core.bean.util.Email;
import org.tdar.core.configuration.TdarConfiguration;
import org.tdar.core.dao.GenericDao;
import org.tdar.core.dao.ReflectionDao;
import org.tdar.core.exception.TdarRecoverableRuntimeException;
import org.tdar.core.service.external.EmailService;
import org.tdar.filestore.Filestore;
import org.tdar.filestore.Filestore.LogType;
import org.tdar.utils.MessageHelper;
import org.tdar.utils.activity.Activity;
import org.tdar.utils.jaxb.IdList;
import org.tdar.utils.jaxb.converters.JaxbMapConverter;
import org.tdar.utils.jaxb.converters.JaxbPersistableMapConverter;

@Service
public class AuthorityManagementService {

    public enum DupeMode implements HasLabel, Localizable {
        /*
         * Authority Management really needs multiple modes:
         * 1. Typo cleanup mode -- remove the dups and pretend they never existed
         * 2. Synonym mode -- mark the dups as dups; keep them, but change all references from the current thing to the authority (5 versions of the same person
         * at the same time)
         * 3. User consolidation mode -- mark the "dups" as dups, but keep the references set on the "dup" instead of the authority. If I have 2 versions of a
         * person from different jobs, this is useful for consolidating the people, but keeping the context of that person at that time.
         */
        DELETE_DUPLICATES("Delete Duplicates (irreversable)"),
        MARK_DUPS_AND_CONSOLDIATE("Mark duplicates and update references (somewhat reversable)"),
        MARK_DUPS_ONLY("Mark As Dup (reversable)");

        private String label;

        private DupeMode(String label) {
            this.label = label;
        }

        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public String getLocaleKey() {
            return MessageHelper.formatLocalizableKey(this);
        }

    }

    @Autowired
    private ReflectionDao reflectionDao;

    @Autowired
    private ReflectionService reflectionService;

    private transient Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private GenericDao genericDao;

    @Autowired
    private XmlService xmlService;

    @Autowired
    private EmailService emailService;

    // List of classes we will evaluate when looking for references.
    private static List<Class<?>> hostClasses = Arrays.<Class<?>> asList(Resource.class, InformationResource.class, ResourceCreator.class,
            Person.class, Institution.class, AuthorizedUser.class, ResourceCollection.class);

    /**
     * Search through all of the defined classes in {@link #hostClasses} and find Fields that refer to the specified class.
     * 
     * @param referredClass
     * @param dupeIds
     * @return
     */
    @Transactional(readOnly = true)
    public Map<Field, ScrollableResults> getReferrers(Class<?> referredClass, Collection<Long> dupeIds) {
        Map<Field, ScrollableResults> referrers = new HashMap<Field, ScrollableResults>();
        for (Class<?> targetClass : hostClasses) {
            Set<Field> fields = reflectionService.findAssignableFieldsRefererencingClass(targetClass, referredClass);
            for (Field field : fields) {
                // get the list of referrer to the duplicates via this specific class and field, add it to the pile
                ScrollableResults fieldReferrers = reflectionDao.findReferrers(field, dupeIds);
                logger.trace("The following objects refer to dupeid list via {}:{}", field.getName(), fieldReferrers);
                referrers.put(field, fieldReferrers);
            }
        }
        return referrers;
    }

    /**
     * Count the total number of Objects that refer to the set of Ids specified (to report to the user)
     * 
     * @param referredClass
     * @param idlist
     * @return
     */
    @Transactional(readOnly = true)
    public Map<Class<?>, Long> getReferrerCounts(Class<?> referredClass, List<Long> idlist) {
        Map<Class<?>, Long> countMap = new HashMap<Class<?>, Long>();
        for (Class<?> targetClass : hostClasses) {
            Set<Field> fields = reflectionService.findAssignableFieldsRefererencingClass(targetClass, referredClass);
            long classTotal = 0;
            for (Field field : fields) {
                Long count = reflectionDao.getReferrerCount(field, idlist);
                classTotal += count;
            }
            countMap.put(targetClass, classTotal);
        }
        return countMap;
    }

    /**
     * Aggregate all referrer counts
     * 
     * @param referredClass
     * @param idlist
     * @return
     */
    @Transactional(readOnly = true)
    public long getTotalReferrerCount(Class<?> referredClass, List<Long> idlist) {
        Map<Class<?>, Long> map = getReferrerCounts(referredClass, idlist);
        long total = 0;
        for (long count : map.values()) {
            total += count;
        }
        return total;
    }

    /**
     * Create an aggregated map of Ids and counts to report to the user what's going to be changed or adjusted
     * 
     * @param referredClass
     * @param idlist
     * @return
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> getReferrerCountMaps(Class<?> referredClass, List<Long> idlist) {
        Map<Long, Long> countmap = new HashMap<Long, Long>();
        for (Long id : idlist) {
            countmap.put(id, 0L);
        }
        // FIXME: this loop is kludgey and confusing and will surely be fodder for my next code review
        for (Class<?> targetClass : hostClasses) {
            Set<Field> fields = reflectionService.findAssignableFieldsRefererencingClass(targetClass, referredClass);
            for (Field field : fields) {
                List<Map<String, Long>> list = reflectionDao.getReffererCountMap(field, idlist);
                for (Map<String, Long> item : list) {
                    Map<String, Long> map = item;
                    Long id = map.get("id");
                    Long updatedCount = countmap.get(id) + map.get("referenceCount");
                    countmap.put(id, updatedCount);
                }
            }
        }
        return countmap;
    }

    // TODO: jim you (probably) aren't handling one-to-many correctly yet.
    /**
     * Find objects that refer to the specified duplicates and replace the references with the specified authority,
     * saving the referrers in the process. This method handles objects that may refer to another object via scalar
     * fields as well as via collection fields. A few assumptions, restricions:
     * 
     * - this method assumes that, for collection fields, it is not necessary to perform a piecewise replacement
     * of each duplicate record with an authority record. In other words, a collection that contains multiple
     * duplicates will be replaced by one (and only one) authority record (if the authority record is not already
     * in the collection)
     * 
     * - all of the potential referring classes must refer to duplicate objects via fields that have public getters
     * and setters.
     * 
     * Based on DupeMode, this method will do different things:
     * - MARK_DUPS_ONLY -- only marks the dups, does not do anything else
     * - MARK_DUPS_AND_CONSOLDIATE -- mark the items as dups, but also transfer their references to the declared master
     * - DELETE_DUPLICATES -- completely delete the duplicate
     * 
     * @param user
     * @param class1
     * @param dupeIds
     * @param authorityId
     * @param dupeMode
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Transactional(readOnly = false)
    public <T extends Dedupable> void updateReferrers(Person user, Class<? extends Dedupable> class1, Collection<Long> dupeIds, Long authorityId,
            DupeMode dupeMode, boolean sendEmail) {
        Activity activity = new Activity();
        activity.setName(String.format("update-referrers:: referredClass:%s\tauthorityId:%s", class1.getSimpleName(), authorityId));
        ActivityManager.getInstance().addActivityToQueue(activity);
        activity.start();
        logger.info("deduping {} [{}: auth: {}]", class1, dupeMode, authorityId);
        int maxAffectedRecordsCount = TdarConfiguration.getInstance().getAuthorityManagementMaxAffectedRecords();
        int affectedRecordCount = 0;
        // get a list of all the referrer objects and the Fields that contain the reference.
        Map<Field, ScrollableResults> referrers = getReferrers(class1, dupeIds);

        // instantiate the duplicates and the authority record
        Set<T> dupes = new HashSet<T>((Collection<? extends T>) genericDao.findAll(class1, dupeIds));
        T authority = (T) genericDao.find(class1, authorityId);

        // prevent 'protected' records from being deleted
        if (countProtectedRecords(dupes) > 0) {
            activity.end();
            throw new TdarRecoverableRuntimeException("authorityManagementService.dedup_not_allowed_protected");
        }

        /*
         * -if many-to-many or one-to-many,
         * + get collection getter and remove dupes
         * + add authority records to collection
         * + (this may be unnecessary if many-to-many target deletion implicitly deletes rows in jointable... test this out!)
         * - if many-to-one
         * + get scalar setter and set to authority record
         * -hibsession.save() each reference
         */
        AuthorityManagementLog<T> authorityManagementLog = new AuthorityManagementLog<T>(authority, dupes, user, dupeMode);
        for (Map.Entry<Field, ScrollableResults> entry : referrers.entrySet()) {
            Field field = entry.getKey();
            ScrollableResults scrollableResults = entry.getValue();
            logger.trace("field:{}", field);
            while (scrollableResults.next()) {
                affectedRecordCount++;

                Persistable referrer = (Persistable) scrollableResults.get(0);
                genericDao.markWritableOnExistingSession(referrer);
                if (dupeMode != DupeMode.MARK_DUPS_ONLY) {
                    if (Collection.class.isAssignableFrom(field.getType())) {
                        // remove all dupes from the Collection and add in the authoritative entity (unless it's there already)
                        Collection<T> collection = reflectionService.callFieldGetter(referrer, field);
                        for (T dupe : dupes) {
                            if (collection.remove(dupe)) {
                                authorityManagementLog.add(referrer, field, dupe);
                            }
                        }
                        if (!collection.contains(authority)) {
                            collection.add(authority);
                        }
                    } else {
                        T dupe = reflectionService.callFieldGetter(referrer, field);
                        authorityManagementLog.add(referrer, field, dupe);
                        reflectionService.callFieldSetter(referrer, field, authority);
                    }
                    genericDao.saveOrUpdate(referrer);
                }

            }
            scrollableResults.close();
        }

        // FIXME: this seems dodgy to me. replace with the slower/safer way.
        // Throw an exception if this operation touched on too many records. Here we rely upon the assumption that throwing an exception will rollback the
        // underlying transaction and all will be set back to normal. A much slower, but safer, way to go about it would be to pre-count the affected records.
        if ((dupeMode != DupeMode.MARK_DUPS_ONLY) && (affectedRecordCount > maxAffectedRecordsCount)) {
            throw new TdarRecoverableRuntimeException("authorityManagementService.dedup_not_allowed_too_many", Arrays.asList(maxAffectedRecordsCount));
        }

        // add the dupes to the authority as synonyms
        processSynonyms(authority, dupes, dupeMode);
        logAndNotify(authorityManagementLog, sendEmail);
        genericDao.saveOrUpdate(authority);
        // finally, delete each dupe
        genericDao.saveOrUpdate(dupes);
        activity.end();
    }

    /**
     * For People, we have "protected" resources, those that have User accounts, we have to count them to ensure that we don't try and dedup two into one (which
     * is unsupported, and bad).
     * 
     * @param dupes
     * @return
     */
    @SuppressWarnings("rawtypes")
    public <T extends Dedupable> int countProtectedRecords(Collection<T> dupes) {
        int count = 0;
        for (Dedupable d : dupes) {
            if (!d.isDedupable()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Old names become synonyms... this makrs items as synonyms as needed
     * 
     * @param authority
     * @param dupes
     * @param markAndConsoldiateDups
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <T extends Dedupable> void processSynonyms(T authority, Set<T> dupes, DupeMode markAndConsoldiateDups) {
        for (T dup : dupes) {
            authority.getSynonyms().addAll(dup.getSynonyms());
            dup.getSynonyms().clear();
            switch (markAndConsoldiateDups) {
                case DELETE_DUPLICATES:
                    dup.setStatus(Status.DELETED);
                    break;
                case MARK_DUPS_ONLY:
                case MARK_DUPS_AND_CONSOLDIATE:
                    dup.setStatus(Status.DUPLICATE);
                    authority.getSynonyms().addAll(dupes);
                    break;
            }
        }
    }

    /**
     * Log the results of the deduplication (XML/text) and notify the admins (email)
     * 
     * @param logData
     */
    private <T extends Dedupable<?>> void logAndNotify(AuthorityManagementLog<T> logData, boolean sendEmail) {
        logger.debug("{}", logData);

        // log the xml to filestore/logs
        Filestore filestore = TdarConfiguration.getInstance().getFilestore();
        String xml = "";
        String className = logData.getAuthority().getClass().getSimpleName();
        int numUpdated = logData.getUpdatedReferrers().keySet().size(); // number of records affected, not total reference count
        try {
            xml = xmlService.convertToXML(logData);
        } catch (Exception e) {
            xml = MessageHelper.getMessage("authorityManagementService.xml_conversion_error");
            logger.warn("could not completely log authmgmt operation", e);
        }
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-H-m-s");
        String datePart = dateFormat.format(new Date());
        String filename = className.toLowerCase() + "-" + datePart + ".txt";
        filestore.storeLog(LogType.AUTHORITY_MANAGEMENT, filename, xml);
        if (!sendEmail) {
            return;
        }

        // now send a summary email
        String subject = MessageHelper.getMessage("authorityManagementService.email_subject",
                Arrays.asList(TdarConfiguration.getInstance().getSiteAcronym(),
                        MessageHelper.getMessage("authorityManagementService.service_name"),
                        logData.getUserDisplayName(), numUpdated, className, logData.getAuthority().toString()));
        Email email = new Email();
        email.setSubject(subject);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("log", logData);
        map.put("className", className);
        map.put("numUpdated", numUpdated);

        map.put("referrers", logData.getUpdatedReferrers().entrySet());
        try {
            emailService.queueWithFreemarkerTemplate("auth-report.ftl", map, email);
        } catch (Exception e) {
            logger.warn("could not send email: {} ", e);
        }
    }

    /**
     * Static entry for the XML / bean representation to an entire Log
     * 
     * @author abrin
     * 
     * @param <R>
     */
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.PROPERTY)
    @XmlType(name = "log")
    public static class AuthorityManagementLog<R> {
        private String userDisplayName = "n/a";
        private R authority;
        private Set<R> dupes;
        // map of referrer -> ( [field containing the dupe, id of the dupe] ... )
        private Map<Persistable, AuthorityManagementLogPart> updatedReferrers = new HashMap<Persistable, AuthorityManagementLogPart>();
        private DupeMode dupeMode;

        public AuthorityManagementLog() {
        }

        public AuthorityManagementLog(R authority, Set<R> dupes, Person user, DupeMode dupeMode) {
            this.authority = authority;
            this.dupes = dupes;
            this.setDupeMode(dupeMode);
            this.userDisplayName = String.format("%s (%s)", user.getProperName(), user.getId());
        }

        public void add(Persistable referrer, Field field, Persistable dupe) {
            AuthorityManagementLogPart dupeList = updatedReferrers.get(referrer);
            if (dupeList == null) {
                dupeList = new AuthorityManagementLogPart();
                updatedReferrers.put(referrer, dupeList);
            }
            dupeList.add(field.getName(), dupe.getId());
        }

        /**
         * @return the authority
         */
        public R getAuthority() {
            return authority;
        }

        /**
         * @param authority
         *            the authority to set
         */
        public void setAuthority(R authority) {
            this.authority = authority;
        }

        /**
         * @return the dupes
         */
        public Set<R> getDupes() {
            return dupes;
        }

        /**
         * @param dupes
         *            the dupes to set
         */
        public void setDupes(Set<R> dupes) {
            this.dupes = dupes;
        }

        /**
         * @return the updatedReferrers
         */
        @XmlJavaTypeAdapter(JaxbPersistableMapConverter.class)
        public Map<Persistable, AuthorityManagementLogPart> getUpdatedReferrers() {
            return updatedReferrers;
        }

        @Override
        public String toString() {
            return String.format("Authority: %s, dupes: %s, referrers: %s", authority, dupes, updatedReferrers.values());
        }

        public String getUserDisplayName() {
            return userDisplayName;
        }

        public void setUserDisplayName(String userDisplayName) {
            this.userDisplayName = userDisplayName;
        }

        public DupeMode getDupeMode() {
            return dupeMode;
        }

        public void setDupeMode(DupeMode dupeMode) {
            this.dupeMode = dupeMode;
        }

    }

    /**
     * Static class for the Log Entry Part used to log to XML via JAXB
     * 
     * @author abrin
     * 
     */
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.PROPERTY)
    @XmlType(name = "logPart")
    public static class AuthorityManagementLogPart {

        private HashMap<String, IdList> fieldToDupeIds = new HashMap<String, IdList>();

        public void add(String fieldName, Long dupeId) {
            IdList dupeIds = fieldToDupeIds.get(fieldName);
            if (dupeIds == null) {
                dupeIds = new IdList();
                fieldToDupeIds.put(fieldName, dupeIds);
            }
            dupeIds.add(dupeId);
        }

        /**
         * @return the fieldToDupeIds
         */
        @XmlElement
        // @XmlAnyElement(lax=true)
        @XmlJavaTypeAdapter(JaxbMapConverter.class)
        public HashMap<String, IdList> getFieldToDupeIds() {
            return fieldToDupeIds;
        }

        @Override
        public String toString() {
            return fieldToDupeIds.toString();
        }

    }

    public void findPluralDups(Class<? extends Keyword> cls, Person user, boolean listOnly) {
        Map<String, Keyword> map = new HashMap<>();
        Map<Keyword, Set<Keyword>> dups = new HashMap<>();
        for (Keyword kwd : genericDao.findAll(cls)) {
            if (kwd.getLabel().matches("\\d+s")) {
                continue;
            }
            map.put(kwd.getLabel(), kwd);
        }
        for (String label : map.keySet()) {
            String plural = English.plural(label);
            Keyword dup = map.get(plural);
            Keyword key = map.get(label);
            if (dup != null && ObjectUtils.notEqual(dup, key)) {
                if (dup.isDuplicate()) {
                    continue;
                }
                logger.debug("should set plural: {} to singular: {}", plural, label);
                Set<Keyword> list = dups.get(key);
                if (list == null) {
                    list = new HashSet<>();
                }
                list.add(dup);
                dups.put(key, list);
            }
        }
        if (listOnly) {
            return;
        }
        for (Entry<Keyword, Set<Keyword>> entry : dups.entrySet()) {
            processSynonyms(entry.getKey(), entry.getValue(), DupeMode.MARK_DUPS_ONLY);
            updateReferrers(user, (Class<? extends Dedupable>) cls, Persistable.Base.extractIds(entry.getValue()), entry.getKey().getId(),
                    DupeMode.MARK_DUPS_ONLY, false);
        }
    }

}
