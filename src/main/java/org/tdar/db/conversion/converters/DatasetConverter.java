package org.tdar.db.conversion.converters;

import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tdar.core.bean.resource.InformationResourceFileVersion;
import org.tdar.core.bean.resource.dataTable.DataTable;
import org.tdar.core.bean.resource.dataTable.DataTableColumn;
import org.tdar.core.bean.resource.dataTable.DataTableColumnType;
import org.tdar.core.bean.resource.dataTable.DataTableRelationship;
import org.tdar.core.exception.TdarRecoverableRuntimeException;
import org.tdar.db.conversion.analyzers.ColumnAnalyzer;
import org.tdar.db.model.abstracts.TargetDatabase;

import com.healthmarketscience.jackcess.Database;

public interface DatasetConverter {

    /**
     * Get names of all tables for the converted data file.
     * 
     * @return
     */
    public List<String> getTableNames();

    public void setTargetDatabase(TargetDatabase targetDatabase);

    /**
     * Imports the given sourceDatabase into the target database.
     * 
     * @param sourceFile
     * @param targetDatabase
     * @return
     */
    public Set<DataTable> execute();

    public String getInternalTableName(String originalTableName);

    public Set<DataTable> getDataTables();

    public Set<DataTableRelationship> getKeys();

    public DataTable getDataTableByName(String name);

    public Set<DataTableRelationship> getRelationships();

    public void setRelationships(Set<DataTableRelationship> relationships);

    public List<DataTableRelationship> getRelationshipsWithTable(String tableName);

    /**
     * Abstract base class for DatasetConverterS, uses template pattern to ease implementation of execute().
     */
    public abstract static class Base implements DatasetConverter {

        private String filename = "";
        private Long irFileId;
        private Database database = null;
        private String prefix = "";
        protected final Logger logger = LoggerFactory.getLogger(getClass());
        protected InformationResourceFileVersion informationResourceFileVersion;
        protected TargetDatabase targetDatabase;
        protected Connection connection;
        protected Set<DataTable> dataTables = new HashSet<DataTable>();
        protected Set<String> dataTableNames = new HashSet<String>();
        protected Set<DataTableRelationship> dataTableRelationships = new HashSet<DataTableRelationship>();

        protected abstract void openInputDatabase() throws IOException;

        protected abstract void dumpData() throws IOException, Exception;

        public void setRelationships(Set<DataTableRelationship> relationships) {
            this.dataTableRelationships = relationships;
        }

        public Set<DataTableRelationship> getRelationships() {
            return dataTableRelationships;
        }

        public List<DataTableRelationship> getRelationshipsWithTable(String tableName) {
            List<DataTableRelationship> rels = new ArrayList<DataTableRelationship>();
            for (DataTableRelationship rel : dataTableRelationships) {
                if (rel.getForeignTable().getName().equals(tableName) || rel.getLocalTable().getName().equals(tableName)) {
                    rels.add(rel);
                }
            }
            return rels;
        }

        public Set<DataTable> getDataTables() {
            return dataTables;
        }

        public DataTable createDataTable(String name) {
            DataTable dataTable = new DataTable();
            dataTable.setDisplayName(name);
            String name_ = generateDataTableName(name);
            if (dataTableNames.contains(name_)) {
                int add = 1;
                while (dataTableNames.contains(name_ + add)) {
                    add++;
                }
                String tmpName = name_ + add;
                logger.debug("renaming table from " + name_ + " to " + tmpName);
                name_ = tmpName;
            }
            dataTableNames.add(name_);
            dataTable.setName(name_);
            dataTables.add(dataTable);

            return dataTable;
        }

        public DataTableColumn createDataTableColumn(String name, DataTableColumnType type, DataTable dataTable) {
            DataTableColumn dataTableColumn = new DataTableColumn();
            dataTableColumn.setDisplayName(name);
            dataTableColumn.setName(targetDatabase.normalizeTableOrColumnNames(name));
            dataTableColumn.setColumnDataType(type);
            dataTableColumn.setColumnEncodingType(type.getDefaultEncodingType());
            dataTableColumn.setDataTable(dataTable);
            dataTable.getDataTableColumns().add(dataTableColumn);
            return dataTableColumn;
        }

        public void completePreparedStatements() throws Exception {
            targetDatabase.closePreparedStatements(getDataTables());
            logger.debug("completed prepared statements...");
        }

        /**
         * Uses the template method pattern to read the input database and convert its
         * contents into a Set<DataTable>.
         * FIXME: should probably add an abstract closeInputDatabase() for proper cleanup
         * and add it to the finally clause
         */
        @Override
        public Set<DataTable> execute() {
            try {
                openInputDatabase();
                dumpData();
                return getDataTables();
            } catch (IOException e) {
                logger.error("I/O error while opening input database or dumping data", e);
                throw new TdarRecoverableRuntimeException("I/O error while opening input database or dumping data", e);
            } catch (Exception e) {
                logger.error("Unexpected expection while opening input dataset or dumping data", e);
                throw new TdarRecoverableRuntimeException("Unexpected expection while opening input dataset or dumping data", e);
            }
        }

        public List<String> getTableNames() {
            ArrayList<String> tables = new ArrayList<String>();
            for (DataTable table : dataTables) {
                tables.add(table.getName());
            }
            return tables;
        }

        public DataTable getDataTableByName(String name) {
            for (DataTable table : dataTables) {
                if (name.equals(table.getName()))
                    return table;
            }
            return null;
        }

        protected void alterTableColumnTypes(DataTable dataTable, Map<DataTableColumn, List<ColumnAnalyzer>> statistics) {
            logger.debug("altering table column types for " + dataTable.getDisplayName());
            for (Map.Entry<DataTableColumn, List<ColumnAnalyzer>> entry : statistics
                    .entrySet()) {
                DataTableColumn column = entry.getKey();
                // the first item in the list is our "most desired" conversion choice
                ColumnAnalyzer best = entry.getValue().get(0);
                targetDatabase.alterTableColumnType(dataTable.getName(),
                        column, best.getType(), best.getLength());
                column.setColumnDataType(best.getType());
                column.setColumnEncodingType(best.getType().getDefaultEncodingType());
            }
        }

        public void setTargetDatabase(TargetDatabase targetDatabase) {
            this.targetDatabase = targetDatabase;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getFilename() {
            return filename;
        }

        public void setIrFileId(Long irFileId) {
            this.irFileId = irFileId;
        }

        public Long getIrFileId() {
            return irFileId;
        }

        public void setDatabase(Database database) {
            this.database = database;
        }

        public Database getDatabase() {
            return database;
        }

        public void setDatabasePrefix(String prefix) {
            this.prefix = prefix;
        }

        public String getDatabasePrefix() {
            return prefix;
        }

        protected String generateDataTableName(String tableName) {
            StringBuilder sb = new StringBuilder(getDatabasePrefix());
            sb.append('_').append(getIrFileId()).append('_');
            if (!StringUtils.isBlank(getFilename()))
                sb.append(getFilename()).append('_');
            sb.append(targetDatabase.normalizeTableOrColumnNames(tableName));
            return targetDatabase.normalizeTableOrColumnNames(sb.toString());
        }

        public String getInternalTableName(String originalTableName) {
            return originalTableName.replaceAll("^(" + getDatabasePrefix() + "_)(\\d+)(_?)", "");
        }

        public Set<DataTableRelationship> getKeys() {
            return dataTableRelationships;
        }

        /**
         * @param informationResourceFileVersion
         *            the informationResourceFileVersion to set
         */
        public void setInformationResourceFileVersion(InformationResourceFileVersion informationResourceFileVersion) {
            this.informationResourceFileVersion = informationResourceFileVersion;
        }

        /**
         * @return the informationResourceFileVersion
         */
        public InformationResourceFileVersion getInformationResourceFileVersion() {
            return informationResourceFileVersion;
        }

    }

}