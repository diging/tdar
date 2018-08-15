package org.tdar.core.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.Date;
import java.util.List;

import org.tdar.core.bean.PersonalFilestoreTicket;
import org.tdar.core.bean.billing.BillingAccount;
import org.tdar.core.bean.collection.ResourceCollection;
import org.tdar.core.bean.entity.TdarUser;
import org.tdar.core.bean.file.AbstractFile;
import org.tdar.core.bean.file.CurationState;
import org.tdar.core.bean.file.FileComment;
import org.tdar.core.bean.file.Mark;
import org.tdar.core.bean.file.TdarDir;
import org.tdar.core.bean.file.TdarFile;
import org.tdar.core.bean.resource.ResourceType;
import org.tdar.core.dao.DirSummary;
import org.tdar.core.dao.FileOrder;
import org.tdar.core.dao.RecentFileSummary;
import org.tdar.core.exception.FileUploadException;
import org.tdar.filestore.personal.PersonalFileType;
import org.tdar.filestore.personal.PersonalFilestore;
import org.tdar.filestore.personal.PersonalFilestoreFile;

public interface PersonalFilestoreService {

    /**
     * Creates a new PersonalFilestoreTicket that the personal filestore can use to create a filestore in a unique location.
     * 
     * @return a new PersonalFilestoreTicket used to keep track of files uploaded by the given Person
     */
    PersonalFilestoreTicket createPersonalFilestoreTicket(TdarUser person);

    /**
     * Creates a new PersonalFilestoreTicket for the given Person with the given PersonalFileType (either UPLOAD or INTEGRATION)
     * 
     * @param person
     * @param fileType
     *            whether or not the ticket is being created for an upload or data integration process.
     * @return a new PersonalFilestoreTicket used to keep track of files generated by the given Person (either uploaded or via data integration)
     */
    PersonalFilestoreTicket createPersonalFilestoreTicket(TdarUser person, PersonalFileType fileType);

    /**
     * Returns a personal filestore for the given user.
     * FIXME: should this be based on the PersonalFilestoreTicket instead?
     * 
     * @param submitter
     * @return a properly synchronized filestore for the given user.
     */
    PersonalFilestore getPersonalFilestore(TdarUser submitter);

    /**
     * Find a @link PersonalFilestoreTicket based on the ID
     * 
     * @param ticketId
     * @return
     */
    PersonalFilestoreTicket findPersonalFilestoreTicket(Long ticketId);

    /**
     * Return all files in the @link PersonalFilestore based on the ticketId
     * 
     * @param ticketId
     * @return
     */
    List<PersonalFilestoreFile> retrieveAllPersonalFilestoreFiles(Long ticketId);

    /**
     * Get the @link PersonalFilestore
     * 
     * @param ticket
     * @return
     */
    PersonalFilestore getPersonalFilestore(PersonalFilestoreTicket ticket);

    /**
     * Get a filestore given a ticket
     * 
     * @param ticketId
     * @return
     */
    PersonalFilestore getPersonalFilestore(Long ticketId);

    /**
     * Store a file in the Personal Filestore
     * 
     * @param ticket
     * @param file
     * @param filename
     * @return
     * @throws FileUploadException
     * @throws IOException
     */
    TdarFile store(PersonalFilestoreTicket ticket, File file, String fileName, BillingAccount account, TdarUser user, TdarDir dir) throws FileUploadException;

    TdarDir createDirectory(TdarDir parent, String name, BillingAccount account, TdarUser authenticatedUser) throws FileAlreadyExistsException;

    List<AbstractFile> listFiles(TdarDir parent, BillingAccount account, String term, FileOrder sortBy, TdarUser authenticatedUser);

    void deleteFile(AbstractFile file, TdarUser authenticatedUser) throws FileUploadException;

    void moveFiles(List<AbstractFile> files, TdarDir dir, TdarUser authenticatedUser);

    TdarDir findUnfileDir(TdarUser authenticatedUser);

    void editMetadata(TdarFile file, String note, boolean needsOcr, CurationState curate, TdarUser authenticatedUser);

    void mark(List<TdarFile> files, Mark mark, TdarUser authenticatedUser);

    void unMark(List<TdarFile> files, Mark role, TdarUser authenticatedUser);

    FileComment addComment(AbstractFile file, String comment, TdarUser authenticatedUser);

    FileComment resolveComment(AbstractFile file, FileComment comment, TdarUser authenticatedUser);

    ResourceType getResourceTypeForFiles(TdarFile files);

    List<TdarDir> listDirectories(TdarDir parent, BillingAccount account, TdarUser authenticatedUser);

    List<AbstractFile> moveFilesBetweenAccounts(List<AbstractFile> files, BillingAccount account, TdarUser authenticatedUser);

    void renameDirectory(TdarDir file, BillingAccount account, String name, TdarUser authenticatedUser) throws FileAlreadyExistsException;

    DirSummary summarizeAccountBy(BillingAccount account, Date date, TdarUser authenticatedUser);

    RecentFileSummary recentByAccount(BillingAccount account, Date dateStart, Date dateEnd,  TdarDir dir, TdarUser actor, TdarUser authenticatedUser);

    void linkCollection(TdarDir file, ResourceCollection collection, TdarUser user);

    void updateLinkedCollection(TdarDir file, TdarUser user);

    void unlinkLinkedCollection(TdarDir file, TdarUser authenticatedUser);

}