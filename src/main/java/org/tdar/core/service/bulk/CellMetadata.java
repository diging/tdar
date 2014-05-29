/**
 * $Id$
 * 
 * @author $Author$
 * @version $Revision$
 */
package org.tdar.core.service.bulk;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tdar.core.bean.BulkImportField;

/**
 * This class is a proxy class that enables the BulkUpload process to map and manage the import process. The BulkUploadService
 * scans the resource tree and then creates the CellMetadata classes to describe each field.
 * 
 * @author Adam Brin
 * 
 */
public class CellMetadata implements Serializable {

    private static final String REQUIRED = "*";

    private static final long serialVersionUID = 3392288145007832396L;

    /**
     * A special instance that repsents the Filename Field
     */
    public static final CellMetadata FILENAME = new CellMetadata() {

        private static final long serialVersionUID = 5182864266494862305L;

        @Override
        public boolean isRequired() {
            return true;
        };

        @Override
        public String getName() {
            return BulkUploadTemplate.FILENAME;
        };

        @Override
        public String getComment() {
            return BulkImportField.FILENAME_DESCRIPTION;
        };

        @Override
        public int getOrder() {
            return -1000;
        };
    };

    private String name;
    private String displayName;
    private String comment;
    private Class<?> mappedClass;
    private boolean required = false;
    private int order = 0;

    public CellMetadata() {
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" display:").append(displayName).append(" rqrd:").append(required).append(" -- ").append(mappedClass);
        return sb.toString();
    }

    @SuppressWarnings("unused")
    private final transient Logger logger = LoggerFactory.getLogger(getClass());
    private List<Enum<?>> enumList = new ArrayList<>();
    private boolean floatNumber = false;
    private boolean numeric = false;

    /**
     * Initialize with specified values
     * 
     * @param field
     * @param annotation
     * @param class2
     * @param stack
     * @param prefix
     */
    public CellMetadata(Field field, BulkImportField annotation, Class<?> class2, Stack<List<Class<?>>> stack, String prefix) {
        this.mappedClass = class2;
        if (field.getType().isEnum()) {
            for (Object enumConstant : field.getType().getEnumConstants()) {
                getEnumList().add((Enum<?>) enumConstant);
            }
        }

        this.required = annotation.required();

        String fieldPrefix = "";
        if (stack.size() > 1) {
            for (int i = 1; i < stack.size(); i++) {
                List<Class<?>> list = stack.get(i);
                String prefix_ = "";
                for (Class<?> cls : list) {
                    prefix_ += cls.getSimpleName();
                }
                fieldPrefix += prefix_ + ".";
            }
        }
        this.name = fieldPrefix + field.getName();
        this.displayName = StringUtils.trim(prefix + " " + annotation.label());
        this.comment = annotation.comment();
        this.order = annotation.order();

        if (Integer.class.isAssignableFrom(field.getType()) || Long.class.isAssignableFrom(field.getType())) {
            setNumeric(true);
        }

        if (Float.class.isAssignableFrom(field.getType())) {
            setNumeric(true);
            setFloatNumber(true);
        }

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Equality defined by cell name
     */
    @Override
    public boolean equals(Object cm) {
        if (cm == null) { // all equals() should return false if passed a null value...
            return false;
        } else if (cm.getClass().isAssignableFrom(CellMetadata.class)) {
            return getName().equals(((CellMetadata) cm).getName());
        }
        return getName().equals(cm.toString()); // if a string, then 'this object (which is already a string!) is itself returned'
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    /**
     * @return the mappedClass
     */
    public Class<?> getMappedClass() {
        return mappedClass;
    }

    /**
     * @param mappedClass
     *            the mappedClass to set
     */
    public void setMappedClass(Class<?> mappedClass) {
        this.mappedClass = mappedClass;
    }

    /**
     * @return the required
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * @param required
     *            the required to set
     */
    public void setRequired(boolean required) {
        this.required = required;
    }

    /**
     * Get the actual name that should be used in Excel (add a * for required)
     * 
     * @return
     */
    public String getOutputName() {
        StringBuilder sb = new StringBuilder();
        if (!StringUtils.isBlank(getDisplayName())) {
            sb.append(getDisplayName());
        } else {
            sb.append(getName());
        }
        if (isRequired()) {
            sb.append(REQUIRED);
        }
        return sb.toString();
    }

    /**
     * @return the order
     */
    public int getOrder() {
        return order;
    }

    /**
     * @param order
     *            the order to set
     */
    public void setOrder(int order) {
        this.order = order;
    }

    public String getPropertyName() {
        if ((name != null) && (name.indexOf(".") != -1)) {
            return name.substring(name.lastIndexOf(".") + 1);
        }
        return name;
    }

    public List<Enum<?>> getEnumList() {
        return enumList;
    }

    public void setEnumList(List<Enum<?>> enumList) {
        this.enumList = enumList;
    }

    public boolean isFloatNumber() {
        return floatNumber;
    }

    public void setFloatNumber(boolean floatNumber) {
        this.floatNumber = floatNumber;
    }

    public boolean isNumeric() {
        return numeric;
    }

    public void setNumeric(boolean numeric) {
        this.numeric = numeric;
    }
}
