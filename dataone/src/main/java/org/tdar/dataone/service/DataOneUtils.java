package org.tdar.dataone.service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Subject;
import org.tdar.dataone.bean.EntryType;

/**
 * helper class
 * 
 * @author abrin
 *
 */
public class DataOneUtils {

    public static Subject createSubject(String name) {
        Subject subject = new Subject();
        subject.setValue(name);
        return subject;
    }

    public static Identifier createIdentifier(String formattedIdentifier) {
        Identifier id = new Identifier();
        id.setValue(formattedIdentifier);
        return id;
    }

    public static Checksum createChecksum(String checksum) {
        Checksum cs = new Checksum();
        cs.setAlgorithm("MD5");
        cs.setValue(checksum);
        return cs;
    }

    public static AccessRule createAccessRule(Permission permission, String name) {
        AccessRule rule = new AccessRule();
        rule.getPermissionList().add(permission);
        if (StringUtils.isNotBlank(name)) {
            rule.getSubjectList().add(DataOneUtils.createSubject(name));
        }
        return rule;
    }

    public static ObjectFormatIdentifier contentTypeToD1Format(EntryType type, String contentType) {
        ObjectFormatIdentifier identifier = new ObjectFormatIdentifier();
        switch (type) {
            case D1:
                identifier.setValue(DataOneConstants.D1_RESOURCE_MAP_FORMAT);
                break;
            case FILE:
                break;
            case TDAR:
                identifier.setValue(DataOneConstants.D1_DC_FORMAT);
                break;
            default:
                identifier.setValue("BAD-FORMAT");
                break;
        }
        return identifier;
    }

    public static String checksumString(String string) throws NoSuchAlgorithmException, UnsupportedEncodingException {
//        final MessageDigest messageDigest = MessageDigest.getInstance(DataOneConstants.MD5);

        final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        // NOTE NO SLASH -- important for charset here
        messageDigest.update(string.getBytes("UTF8"));
        byte[] raw = messageDigest.digest();
        final String result = new String(Hex.encodeHex(raw));
        return result;
    }

}
