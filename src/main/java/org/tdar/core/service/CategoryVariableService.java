package org.tdar.core.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tdar.core.bean.resource.CategoryVariable;
import org.tdar.core.dao.resource.CategoryVariableDao;

/**
 * $Id$
 * 
 * Provides access to the category variables that can be associated with a given column in a CodingSheet.
 * 
 * @author <a href='mailto:Allen.Lee@asu.edu'>Allen Lee</a>
 * @version $Revision$
 */
@Service
public class CategoryVariableService extends ServiceInterface.TypedDaoBase<CategoryVariable, CategoryVariableDao> {

    @Autowired
    public void setDao(CategoryVariableDao dao) {
        super.setDao(dao);
    }

    @Transactional(readOnly = true)
    public List<CategoryVariable> findAllCategories() {
        return getDao().findAllCategories();
    }

    @Transactional(readOnly = true)
    public List<CategoryVariable> findAllCategoriesSorted() {
        return getDao().findAllCategoriesSorted();
    }
    
    @Transactional(readOnly = true)
    public List<CategoryVariable> findAllSubcategories(Long id) {
        return getDao().findAllSubcategories(id);
    }

}
