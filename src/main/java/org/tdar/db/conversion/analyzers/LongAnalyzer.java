package org.tdar.db.conversion.analyzers;

import org.tdar.core.bean.resource.dataTable.DataTableColumnType;

public class LongAnalyzer implements ColumnAnalyzer {

	public DataTableColumnType getType() {
		return DataTableColumnType.BIGINT;
	}

	public boolean analyze(String value) {
		if (value == null)
			return true;
		if ("".equals(value))
			return true;
		try {
			Long.parseLong(value);
		} catch (NumberFormatException nfx) {
			return false;
		}
		return true;
	}

	public int getLength() {
		return 0;
	}
}