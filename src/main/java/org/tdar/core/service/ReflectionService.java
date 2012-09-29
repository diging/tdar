/**
 * $Id$
 * 
 * @author $Author$
 * @version $Revision$
 */
package org.tdar.core.service;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.apache.struts2.convention.ReflectionTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

/**
 * @author Adam Brin
 * 
 */
@Service
public class ReflectionService {

    public transient Logger logger = LoggerFactory.getLogger(getClass());

    /*
     * This method looks at a class like "Resource" and finds fields that contain the "classToFind",
     * e.g. GeographicKeyword. This would return [geographicKeywords,managedGeographicKeywords]
     */
    public Set<Field> findFieldsReferencingClass(Class<?> targetClass, Class<?> classToFind) {
        Set<Field> matchingFields = new HashSet<Field>();
        for (Field field : targetClass.getDeclaredFields()) {
            if (getFieldReturnType(field).equals(classToFind)) {
                matchingFields.add(field);
            }
        }
        logger.debug("Found Fields:{} on {}", matchingFields, targetClass.getSimpleName());
        return matchingFields;
    }

    @SuppressWarnings("rawtypes")
    public Set<Field> findFieldsWithAnnotation(Class<?> targetClass, List<Class<? extends Annotation>> list, boolean recursive) {
        Set<Field> set = new HashSet<Field>();
        for (Field field : targetClass.getDeclaredFields()) {
            for (Class<? extends Annotation> ann : list) {
                if (field.isAnnotationPresent(ann)) {
                    set.add(field);
                }
            }
        }
        if (recursive) {
            for (Class<?> parent : ReflectionTools.getClassHierarchy(targetClass)) {
                set.addAll(findFieldsWithAnnotation(parent, list, false));
            }
        }
        return set;
    }

    @SuppressWarnings("unchecked")
    public void warmUp(Object obj, int i) {
        logger.debug("warming up: {} ", obj);
        Set<Field> fields = findFieldsWithAnnotation(obj.getClass(), Arrays.asList(ManyToMany.class, ManyToOne.class, OneToMany.class, OneToOne.class), true);
        for (Field field : fields) {
            Object result = callFieldGetter(obj, field);
            logger.trace("{}", result);
            if (result == null)
                continue;
            if (result instanceof Collection<?> && i > 0) {
                for (Object child : (Collection<?>) result) {
                    warmUp(child, i - 1);
                }
            } else if (field.getAnnotation(OneToOne.class) != null) {
                warmUp(result, 0);
            }
        }
    }

    /**
     * Take the method name and try and replace it with the same
     * logic that Hibernate uses
     * 
     * @param name
     * @return
     */
    public static String cleanupMethodName(String name) {
        name = name.replaceAll("^(get|set)", "");
        name = name.substring(0, 1).toLowerCase() + name.substring(1);
        return name;
    }

    public static String cleanupMethodName(Method method) {
        return cleanupMethodName(method.getName());
    }

    public static String generateGetterName(Field field) {
        return generateGetterName(field.getName());
    }

    public static String generateGetterName(String name) {
        return generateName("get", name);
    }

    public static String generateSetterName(Field field) {
        return generateGetterName(field.getName());
    }

    /*
     * Based on the field and the object passed in, call the getter and return the result
     */
    public <T> T callFieldGetter(Object obj, Field field) {
        logger.debug("calling getter on: {} {} ", obj, field.getName());
        Method method = ReflectionUtils.findMethod(field.getDeclaringClass(), generateGetterName(field));
        if (method.getReturnType() != Void.TYPE)
            try {
                return (T) method.invoke(obj);
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        return null;
    }

    public static String generateSetterName(String name) {
        return generateName("set", name);
    }

    private static String generateName(String prefix, String name) {
        return prefix + name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public static Class<?> getFieldReturnType(AccessibleObject accessibleObject) {
        final Logger log = LoggerFactory.getLogger(ReflectionService.class);

        if (accessibleObject instanceof Field) {
            Field field = (Field) accessibleObject;
            log.trace("generic type: {}", field.getGenericType());
            return getType(field.getGenericType());
        }
        if (accessibleObject instanceof Method) {
            Method method = (Method) accessibleObject;
            log.trace("generic type: {}", method.getGenericReturnType());
            return getType(method.getGenericReturnType());
        }
        return null;
    }

    private static Class<?> getType(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType collectionType = (ParameterizedType) type;
            return (Class<?>) collectionType.getActualTypeArguments()[0];
        }

        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        return null;
    }
}
