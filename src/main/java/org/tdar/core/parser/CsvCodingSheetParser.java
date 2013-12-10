package org.tdar.core.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.tdar.core.bean.resource.CodingRule;
import org.tdar.core.bean.resource.CodingSheet;
import org.tdar.core.exception.TdarRecoverableRuntimeException;
import org.tdar.utils.MessageHelper;

import au.com.bytecode.opencsv.CSVReader;

/**
 * $Id$
 * <p>
 * Parses CSV coding sheets using <a href='http://opencsv.sourceforge.net'>OpenCSV</a>.
 * 
 * Should we switch to <a href='http://supercsv.sourceforge.net'>Super CSV</a> instead.
 * 
 * @author <a href='mailto:Allen.Lee@asu.edu'>Allen Lee</a>
 * @version $Revision$
 */
public class CsvCodingSheetParser implements CodingSheetParser {

    private final static Logger logger = Logger.getLogger(CsvCodingSheetParser.class);

    public CSVReader getReader(InputStream stream) {
        return new CSVReader(new BufferedReader(new InputStreamReader(stream)));
    }

    @Override
    public List<CodingRule> parse(CodingSheet codingSheet, InputStream stream) throws CodingSheetParserException {
        List<CodingRule> codingRules = new ArrayList<CodingRule>();
        CSVReader reader = getReader(stream);
        boolean emptyBecauseOfParseIssues = true;
        try {
            for (String[] ruleArray : reader.readAll()) {
                if (ruleArray.length < 2) {
                    logger.warn(String.format("Each line needs to be at least 2 elements but '%s' only had %d elements.  Skipping.", Arrays.asList(ruleArray),
                            ruleArray.length));
                    continue;
                }
                emptyBecauseOfParseIssues = false;
                String code = ruleArray[CODE_INDEX];
                String term = ruleArray[TERM_INDEX];
                if (StringUtils.isBlank(code) && StringUtils.isBlank(term)) {
                    logger.warn("Null code and term, skipping");
                    continue;
                }
                if (StringUtils.isBlank(code) || StringUtils.isBlank(term)) {
                    throw new TdarRecoverableRuntimeException(MessageHelper.getMessage("csvCodingSheetParser.null_code_or_term", code, term));
                }

                CodingRule codingRule = new CodingRule();
                codingRule.setCode(code);
                codingRule.setTerm(term);
                codingRule.setCodingSheet(codingSheet);
                if (ruleArray.length > 2) {
                    codingRule.setDescription(ruleArray[DESCRIPTION_INDEX]);
                }
                codingRules.add(codingRule);
            }
        } catch (IOException e) {
            logger.error(e);
            throw new CodingSheetParserException(e);
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.error("Invalid CSV format for coding sheets.", e);
            throw new CodingSheetParserException(MessageHelper.getMessage("csvCodingSheetParser.could_not_parse_columns"), e);
        }
        if (emptyBecauseOfParseIssues) {
            throw new CodingSheetParserException(MessageHelper.getMessage("csvCodingSheetParser.could_not_parse_comma"));
        }
        return codingRules;
    }

}