/**
 * $Id$
 * 
 * @author $Author$
 * @version $Revision$
 */
package org.tdar.core.dao;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.type.StandardBasicTypes;
import org.tdar.core.bean.entity.Person;
import org.tdar.core.bean.entity.permissions.GeneralPermissions;
import org.tdar.core.bean.resource.InformationResourceFileVersion;
import org.tdar.core.bean.resource.Project;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.bean.resource.dataTable.DataTableColumn;

/**
 * @author Adam Brin
 *         This class should not exist once Hibernate properly supports the @NamedNativeQueries
 *         annotations per JPA2
 */
public final class NamedNativeQueries {

    public static String incrementAccessCount(Resource r) {
        return "update resource set access_counter=access_counter+1 where id=" + r.getId();
    }

    public static String updateDatasetMappings(Project project, DataTableColumn column, String value, List<String> filenames,
            List<InformationResourceFileVersion.VersionType> types) {
        // SQL escape all incoming filenames
        for (int i = 0; i < filenames.size(); i++) {
            filenames.set(i, StringEscapeUtils.escapeSql(filenames.get(i)));
        }
        String filenameCheck = "filename";
        if (column.isIgnoreFileExtension()) {
            filenameCheck = "substring(lower(irfv.filename), 0, length(irfv.filename) - length(irfv.extension) + 1)";
        }
        String filenameList = StringUtils.join(filenames, "','");
        String versionList = StringUtils.join(types, "','");

        String sql = "update information_resource ir_ set mappeddatakeycolumn_id=%s, mappedDataKeyValue='%s' from information_resource ir inner join " +
                "information_resource_file irf on ir.id=irf.information_resource_id " +
                "inner join information_resource_file_version irfv on irf.id=irfv.information_resource_file_id " +
                "WHERE ir.project_id=%s and %s in ('%s') and irfv.internal_type in ('%s') and ir.id=ir_.id";

        return String.format(sql, column.getId(), StringEscapeUtils.escapeSql(value), project.getId(),
                filenameCheck, filenameList, versionList);
    }

    public static String removeDatasetMappings(Project project, Collection<DataTableColumn> columns) {
        StringBuilder sb = new StringBuilder();
        for (DataTableColumn column : columns) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(column.getId());
        }

        String sql = "update information_resource ir_ set mappeddatakeycolumn_id=NULL, mappedDataKeyValue=NULL " +
                "WHERE ir_.project_id=%s and mappeddatakeycolumn_id in (%s)";

        return String.format(sql, project.getId(), sb.toString());
    }

    public static String generateDashboardGraphQuery(Person user, GeneralPermissions permission) {
        return String
                .format("select id, status, resource_type from resource where id in  (select resource_id from collection_resource,collection, authorized_user where collection.id=collection_resource.collection_id and collection.id=user_id and user_id=%s and general_permission_int > %s union select id from resource where updater_id=%s or submitter_id=%s)",
                        user.getId(), (permission.getEffectivePermissions() -1), user.getId(), user.getId());
    }

    public static SQLQuery generateDashboardGraphQuery(Session currentSession) {
        return currentSession
            .createSQLQuery(TdarNamedQueries.QUERY_SQL_DASHBOARD)
            .addScalar("id", StandardBasicTypes.LONG)
            .addScalar("status", StandardBasicTypes.STRING)
            .addScalar("resource_type", StandardBasicTypes.STRING);
    }
}
