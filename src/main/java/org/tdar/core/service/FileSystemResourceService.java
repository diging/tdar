package org.tdar.core.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tdar.core.dao.FileSystemResourceDao;
import org.tdar.struts.action.TdarActionException;

/**
 * This Service provides support for getting resources off of the filesystem and
 * helps to abstract knowledge of the war file, and other placement of resources
 * on and off the classpath.
 * 
 * @author abrin
 * 
 */
@Service
public class FileSystemResourceService {

    @Autowired
    FileSystemResourceDao fileSystemResourceDao;

    public boolean testWRO() {
        return fileSystemResourceDao.testWRO();
    }

    // helper to load the PDF Template for the cover page
    public File loadTemplate(String path) throws IOException, FileNotFoundException {
        return fileSystemResourceDao.loadTemplate(path);
    }

    public List<String> parseWroXML(String prefix) throws TdarActionException {
        return fileSystemResourceDao.parseWroXML(prefix);
    }

}
