package org.tdar.core.bean.resource;

import static org.junit.Assert.assertEquals;

import java.sql.Types;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.tdar.core.bean.resource.datatable.DataTableColumnType;


public class DataTableColumnTest {

    Logger logger = Logger.getLogger(getClass());

    @Test
    public void testJDBCTypeMappings() {

        for (int type : DataTableColumnType.getAllSQLTypes()) {
            DataTableColumnType columnType = DataTableColumnType.fromJDBCType(type);
            switch (type) {
                case Types.VARCHAR:
                    assertEquals(DataTableColumnType.VARCHAR, columnType);
                    break;
                case Types.BIGINT:
                    assertEquals(DataTableColumnType.BIGINT, columnType);
                    break;
                case Types.DOUBLE:
                    assertEquals(DataTableColumnType.DOUBLE, columnType);
                    break;
                case Types.CLOB:
                    assertEquals(DataTableColumnType.TEXT, columnType);
                    break;
                case Types.BOOLEAN:
                    assertEquals(DataTableColumnType.BOOLEAN, columnType);
                    break;
                case Types.DATE:
                    assertEquals(DataTableColumnType.DATE, columnType);
                    break;
                case Types.TIME:
                case Types.TIMESTAMP:
                    assertEquals(DataTableColumnType.DATETIME, columnType);
                    break;
                default:
                    assertEquals(DataTableColumnType.TEXT, columnType);
            }
        }
    }
    
    @Test
    public void testStringParsing() {

        assertEquals(DataTableColumnType.BOOLEAN, DataTableColumnType.fromString("boolean"));
        assertEquals(DataTableColumnType.BOOLEAN, DataTableColumnType.fromString("bool"));
        assertEquals(DataTableColumnType.VARCHAR, DataTableColumnType.fromString("char"));
        assertEquals(DataTableColumnType.VARCHAR, DataTableColumnType.fromString("varchar"));
        assertEquals(DataTableColumnType.VARCHAR, DataTableColumnType.fromString("character varying"));
        assertEquals(DataTableColumnType.TEXT, DataTableColumnType.fromString("text"));
        assertEquals(DataTableColumnType.TEXT, DataTableColumnType.fromString("binary"));
        assertEquals(DataTableColumnType.TEXT, DataTableColumnType.fromString("whatever"));
        assertEquals(DataTableColumnType.DATE, DataTableColumnType.fromString("date"));
        assertEquals(DataTableColumnType.DATETIME, DataTableColumnType.fromString("datetime"));
        assertEquals(DataTableColumnType.DATETIME, DataTableColumnType.fromString("timestamp"));
        assertEquals(DataTableColumnType.DOUBLE, DataTableColumnType.fromString("double"));
        assertEquals(DataTableColumnType.DOUBLE, DataTableColumnType.fromString("float"));
        assertEquals(DataTableColumnType.BIGINT, DataTableColumnType.fromString("int"));
        assertEquals(DataTableColumnType.BIGINT, DataTableColumnType.fromString("tinyint"));
        assertEquals(DataTableColumnType.BIGINT, DataTableColumnType.fromString("smallint"));

    }
}
