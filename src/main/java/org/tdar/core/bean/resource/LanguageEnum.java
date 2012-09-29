package org.tdar.core.bean.resource;

import org.apache.commons.lang.StringUtils;


/**
 * $Id$
 * 
 * Controlled vocabulary for document types.
 * 
 * @author <a href='mailto:Allen.Lee@asu.edu'>Allen Lee</a>
 * @version $Revision$
 */
public enum LanguageEnum { 

	ENGLISH("English", "en","eng"),
	SPANISH("Spanish", "sp","spa"),
	FRENCH("French", "fr","fra"),
	GERMAN("German", "de","deu"),
	DUTCH("Dutch", "nl","nld"),
	MULTIPLE("Multiple", "-","mul"),
	CHINESE("Chinese", "cn","zho"),
	CHEROKEE("Cherokee", "","chr"),
	TURKISH("Turkish", "tr","tur");
	
    private final String label;
    private final String code;
    private final String iso639_2;
    
    
    private LanguageEnum(String label, String code,String iso) {
        this.label = label;
        this.code = code;
        this.iso639_2 = iso;
    }
    
    public String getCode() {
		return code;
	}

	public String getLabel() {
        return label;
    }
    
	public String getIso639_2() {
		return iso639_2;
	}
	
	public static LanguageEnum fromISO(String str) {
		if (!StringUtils.isEmpty(str)) {
			for (LanguageEnum val : LanguageEnum.values()) {
				if (val.getIso639_2().equalsIgnoreCase(str)) return val;
			}
		}
		return null;
	}
	
    /**
     * Returns the ResourceType corresponding to the String given or null if none exists.  Used in place of valueOf since
     * valueOf throws RuntimeExceptions.
     */
    public static LanguageEnum fromString(String string) {
        if (string == null || "".equals(string)) {
            return null;
        }
        // try to convert incoming resource type String query parameter to ResourceType enum.. unfortunately valueOf only throws RuntimeExceptions.
        try {
            return LanguageEnum.valueOf(string);
        }
        catch (Exception exception) {
            return null;
        }
    }
    


}
