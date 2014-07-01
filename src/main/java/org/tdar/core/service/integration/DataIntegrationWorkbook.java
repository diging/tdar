package org.tdar.core.service.integration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tdar.core.bean.PersonalFilestoreTicket;
import org.tdar.core.bean.entity.TdarUser;
import org.tdar.core.bean.resource.Ontology;
import org.tdar.core.bean.resource.OntologyNode;
import org.tdar.core.bean.resource.datatable.DataTable;
import org.tdar.core.bean.resource.datatable.DataTableColumn;
import org.tdar.core.configuration.TdarConfiguration;
import org.tdar.core.service.ExcelService;
import org.tdar.core.service.excel.CellFormat;
import org.tdar.core.service.excel.CellFormat.Style;
import org.tdar.core.service.excel.SheetProxy;
import org.tdar.filestore.personal.PersonalFileType;
import org.tdar.struts.data.IntegrationColumn;
import org.tdar.struts.data.IntegrationDataResult;
import org.tdar.utils.MessageHelper;
import org.tdar.utils.Pair;

import com.opensymphony.xwork2.TextProvider;

/**
 * Proxy class to handle the generation of the Excel Workbook at the end of the DataIntegration
 * 
 * @author abrin
 * 
 */
public class DataIntegrationWorkbook implements Serializable {

    private static final long serialVersionUID = -2452046179173301666L;
    
    private transient ExcelService excelService;
    private Workbook workbook;
    private List<IntegrationColumn> integrationColumns;
    private List<IntegrationDataResult> integrationDataResults;
    private StringBuilder description;
    private Pair<List<IntegrationDataResult>, Map<List<OntologyNode>, Map<DataTable, Integer>>> generatedIntegrationResults;
    private TdarUser person;
    private List<String> names;
    private PersonalFilestoreTicket ticket;
    private TextProvider provider;
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public DataIntegrationWorkbook(TextProvider provider, ExcelService excelService, TdarUser person,
            Pair<List<IntegrationDataResult>, Map<List<OntologyNode>, Map<DataTable, Integer>>> generatedIntegrationData) {
        this.setExcelService(excelService);
        this.person = person;
        this.provider = provider;
        this.setGeneratedIntegrationResults(generatedIntegrationData);
        setWorkbook(new HSSFWorkbook());
        names = new ArrayList<String>();

    }

    /**
     * Generate the Excel File
     */
    public void generate() {
        // HSSFCellStyle headerStyle = excelService.createHeaderStyle(workbook);
        CellStyle dataTableNameStyle = CellFormat.build(Style.NORMAL).setColor(new HSSFColor.GREY_25_PERCENT()).createStyle(getWorkbook());
        CellStyle summaryStyle = excelService.createSummaryStyle(getWorkbook());
        // first column is the table where the
        int rowIndex = 0;
        // int columnIndex = 0;

        setDescription(new StringBuilder(provider.getText("dataIntegrationWorkbook.descr")).append(" "));

        List<DataTable> tableList = new ArrayList<DataTable>();
        List<String> columnNames = new ArrayList<String>();
        List<String> datasetNames = new ArrayList<String>();
        createDataSheet(names, dataTableNameStyle, rowIndex, tableList, columnNames, datasetNames);

        getDescription().append(MessageHelper.getMessage("dataIntegrationWorkbook.descr_with_datasets")).append(" ")
                .append(StringUtils.join(datasetNames, ", ")).append("\n\t ").append(MessageHelper.getMessage("dataIntegrationWorkbook.descr_using_tables"))
                .append(": ").append(StringUtils.join(names, ", ")).append("\n\t ")
                .append(MessageHelper.getMessage("dataIntegrationWorkbook.descr_using_columns"))
                .append(":").append(StringUtils.join(columnNames, ", "));

        // headerRow.setRowStyle(headerStyle);

        // check that this works in excel on windows:
        // https://issues.apache.org/bugzilla/show_bug.cgi?id=50315
        // FIXME: in poi 3.7 turning this on causes a warning notice in Excel that the file is corrupted, disabling
        // sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, columnIndex - 1));

        Map<List<OntologyNode>, Map<DataTable, Integer>> pivot = getGeneratedIntegrationResults().getSecond();
        createSummarySheet(getWorkbook(), tableList, columnNames, pivot);
        createDescriptionSheet(getIntegrationColumns(), person, getWorkbook(), summaryStyle, tableList);

    }

    /**
     * Generate a @link PersonalFilestoreTicket for the excel file
     */
    private void generateTicket() {
        PersonalFilestoreTicket ticket = new PersonalFilestoreTicket();
        ticket.setDateGenerated(new Date());
        ticket.setPersonalFileType(PersonalFileType.INTEGRATION);
        ticket.setSubmitter(person);
        ticket.setDescription(getDescription().toString());
        this.setTicket(ticket);
    }

    /**
     * Create the workbook for the actual data
     * 
     * @param integrationColumns
     * @param integrationDataResults
     * @param workbook
     * @param names
     * @param dataTableNameStyle
     * @param rowIndex
     * @param tableList
     * @param columnNames
     * @param datasetNames
     */
    @SuppressWarnings("unchecked")
    public void createDataSheet(List<String> names, CellStyle dataTableNameStyle, int rowIndex, List<DataTable> tableList,
            List<String> columnNames, List<String> datasetNames) {
        // Create header
        List<String> headerLabels = new ArrayList<String>();
        headerLabels.add(MessageHelper.getMessage("dataIntegrationWorkbook.data_table"));
        for (IntegrationColumn integrationColumn : integrationColumns) {
            columnNames.add(integrationColumn.getName());
            headerLabels.add(integrationColumn.getName());

            if (integrationColumn.isIntegrationColumn()) {
                headerLabels.add(provider.getText("dataIntegrationWorkbook.data_mapped_value", Arrays.asList(integrationColumn.getName())));
            }
        }

        List<Iterator<String[]>> iterators = new ArrayList<Iterator<String[]>>();
        // compile the rowdata
        for (IntegrationDataResult integrationDataResult : integrationDataResults) {
            DataTable table = integrationDataResult.getDataTable();
            names.add(table.getName());
            tableList.add(table);
            iterators.add(integrationDataResult.getRowData().iterator());
        }

        // FIXME: support for cell style data table name (C1)
        SheetProxy sheetProxy = new SheetProxy(workbook, MessageHelper.getMessage("dataIntegrationWorkbook.data_worksheet"));

        sheetProxy.setData(IteratorUtils.chainedIterator(iterators));
        sheetProxy.setHeaderLabels(headerLabels);
        sheetProxy.setFreezeRow(1);
        sheetProxy.setStartRow(rowIndex);
        getExcelService().addSheets(sheetProxy);

    }

    /**
     * Create the Description worksheet
     * 
     * @param integrationColumns
     * @param person
     * @param workbook
     * @param summaryStyle
     * @param tableList
     */
    private void createDescriptionSheet(List<IntegrationColumn> integrationColumns, TdarUser person, Workbook workbook,
            CellStyle summaryStyle,
            List<DataTable> tableList) {
        Sheet summarySheet = workbook.createSheet(MessageHelper.getMessage("dataIntegrationWorkbook.description_worksheet"));
        Row summaryRow = summarySheet.createRow(0);
        // FIXME: Should I have the ontology mappings too??
        excelService
                .createHeaderCell(
                        summaryStyle,
                        summaryRow,
                        0,
                        provider.getText("dataIntegrationWorkbook.description_header",
                                Arrays.asList(person.getProperName(), new SimpleDateFormat().format(new Date()))));
        summarySheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

        int currentRow = 3;

        List<String> summaryLabels = new ArrayList<String>();
        summaryLabels.add("Table:");
        for (int i = 0; i < tableList.size(); i++) {
            DataTable table = tableList.get(i);
            StringBuilder builder = new StringBuilder(table.getDisplayName());
            builder.append(table.getDataset().getTitle());
            builder.append(" (").append(table.getDataset().getId()).append(")");
            summaryLabels.add(builder.toString());
        }
        excelService.addHeaderRow(summarySheet, 1, 0, summaryLabels);

        for (IntegrationColumn integrationColumn : integrationColumns) {
            List<String> labels = new ArrayList<String>();
            List<String> descriptions = new ArrayList<String>();
            List<String> mappings = new ArrayList<String>();
            descriptions.add(provider.getText("dataIntegrationWorkbook.description_description_column", Arrays.asList("    ")));
            if (integrationColumn.isIntegrationColumn()) {
                labels.add(provider.getText("dataIntegrationWorkbook.description_integration_column", Arrays.asList("    ")));
                mappings.add(provider.getText("dataIntegrationWorkbook.description_mapped_column", Arrays.asList("    ")));
            } else {
                labels.add(" Display Column:");
            }

            for (int i = 0; i < tableList.size(); i++) {
                DataTable table = tableList.get(i);
                DataTableColumn column = integrationColumn.getColumnForTable(table);
                if (column == null) {
                    continue;
                }
                labels.add(column.getDisplayName());
                descriptions.add(column.getDescription());
                if (integrationColumn.isIntegrationColumn()) {
                    Ontology ontology = column.getDefaultOntology();
                    StringBuilder builder = new StringBuilder(ontology.getTitle()).append(" (").append(ontology.getId()).append(")");
                    mappings.add(builder.toString());
                }
            }
            excelService.addDataRow(summarySheet, currentRow++, 0, labels);
            excelService.addDataRow(summarySheet, currentRow++, 0, descriptions);
            if (!mappings.isEmpty()) {
                excelService.addDataRow(summarySheet, currentRow++, 0, mappings);
            }
        }

        // auto-sizing columns
        for (int i = 0; i < summaryLabels.size(); i++) {
            summarySheet.autoSizeColumn(i);
        }
    }

    /**
     * Create the "pivot" table worksheet ("summmary")
     * 
     * @param workbook
     * @param tableList
     * @param columnNames
     * @param pivot
     */
    private void createSummarySheet(Workbook workbook, List<DataTable> tableList, List<String> columnNames,
            Map<List<OntologyNode>, Map<DataTable, Integer>> pivot) {
        int rowIndex;
        Sheet pivotSheet = workbook.createSheet(MessageHelper.getMessage("dataIntegrationWorkbook.summary_worksheet"));

        rowIndex = 2;
        List<String> rowHeaders = new ArrayList<String>(columnNames);
        for (DataTable table : tableList) {
            rowHeaders.add(table.getDataset().getName() + " (" + table.getDisplayName() + ")");
        }

        excelService.addHeaderRow(pivotSheet, ExcelService.FIRST_ROW, ExcelService.FIRST_COLUMN, rowHeaders);

        for (List<OntologyNode> key : pivot.keySet()) {
            List<String> rowData = new ArrayList<String>();
            for (OntologyNode col : key) {
                if (col != null) {
                    rowData.add(col.getDisplayName());
                    // rowData.add(col.getIndex());
                }
            }
            Map<DataTable, Integer> vals = pivot.get(key);
            for (DataTable table : tableList) {
                Integer integer = vals.get(table);
                if (integer == null) {
                    rowData.add("0");
                } else {
                    rowData.add(integer.toString());
                }
            }
            excelService.addDataRow(pivotSheet, rowIndex++, 0, rowData);
        }
    }

    public ExcelService getExcelService() {
        return excelService;
    }

    public void setExcelService(ExcelService excelService) {
        this.excelService = excelService;
    }

    public Workbook getWorkbook() {
        return workbook;
    }

    public void setWorkbook(Workbook workbook) {
        this.workbook = workbook;
    }

    public List<IntegrationColumn> getIntegrationColumns() {
        return integrationColumns;
    }

    public void setIntegrationColumns(List<IntegrationColumn> integrationColumns) {
        this.integrationColumns = integrationColumns;
    }

    public List<IntegrationDataResult> getIntegrationDataResults() {
        return integrationDataResults;
    }

    public void setIntegrationDataResults(List<IntegrationDataResult> integrationDataResults) {
        this.integrationDataResults = integrationDataResults;
    }

    public StringBuilder getDescription() {
        return description;
    }

    public void setDescription(StringBuilder description) {
        this.description = description;
    }

    public Pair<List<IntegrationDataResult>, Map<List<OntologyNode>, Map<DataTable, Integer>>> getGeneratedIntegrationResults() {
        return generatedIntegrationResults;
    }

    public void setGeneratedIntegrationResults(Pair<List<IntegrationDataResult>, Map<List<OntologyNode>, Map<DataTable, Integer>>> generatedIntegrationResults) {
        this.generatedIntegrationResults = generatedIntegrationResults;
    }

    public TdarUser getPerson() {
        return person;
    }

    public void setPerson(TdarUser person) {
        this.person = person;
    }

    public PersonalFilestoreTicket getTicket() {
        if (ticket == null) {
            generateTicket();
        }
        return ticket;
    }

    public void setTicket(PersonalFilestoreTicket ticket) {
        this.ticket = ticket;
    }

    public String getFileName() {
        // MD5 is probably overkill, but we want a filename that is unique based on the included result sheets while avoiding any OS filename restrictions (e.g.
        // maxlength)
        String basename = StringUtils.join(names, "");
        String basenameMd5 = DigestUtils.md5Hex(basename);
        String fileName = provider.getText("dataIntegrationWorkbook.file_name", Arrays.asList(basenameMd5));
        return fileName;
    }

    /**
     * write to temp file
     * 
     * @return
     * @throws IOException
     */
    public File writeToTempFile() throws IOException {
        File resultFile = File.createTempFile(getFileName(), ".xls", TdarConfiguration.getInstance().getTempDirectory());
        logger.trace("writing temp file:{}", resultFile);
        resultFile.deleteOnExit();
        getWorkbook().write(new FileOutputStream(resultFile));
        return resultFile;
    }
}
