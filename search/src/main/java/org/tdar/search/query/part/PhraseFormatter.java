package org.tdar.search.query.part;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Formatting text in a lucene query to help handle escaping and other issues like quoting.
 * These phrases can be stacked and order of operations is managed.
 * 
 * @author abrin
 *
 */
public enum PhraseFormatter {

    WILDCARD,
    ESCAPED,
    EMBEDDED,
    QUOTED,
    ESCAPE_QUOTED,
    ESCAPED_EMBEDDED;

    private static final String[] FIND = Arrays.asList(" ").toArray(new String[0]);
    private static final String[] REPLACE = Arrays.asList("\\ ").toArray(new String[0]);
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public String format(String value) {
        if (StringUtils.isEmpty(value)) {
            return "";
        }
        switch (this) {
            case ESCAPED:
                return StringUtils.replaceEach(QueryParser.escape(value.trim()), FIND, REPLACE);
            case WILDCARD:
                if (StringUtils.startsWith(value, "\"") && StringUtils.endsWith(value, "\"")) {
                    return value;
                }
                if (StringUtils.endsWith(value, "\"") && !StringUtils.endsWith(value, "\\\"")) {
                    logger.error("trying to wildcard a quoted element {}", value);
                }
                return String.format("%s*", value);
            case QUOTED:
                return String.format("\"%s\"", value);
            case ESCAPE_QUOTED:
                return QUOTED.format(ESCAPED.format(value));
            case EMBEDDED:
                return StringUtils.replace(value, "'", "\\'");
            case ESCAPED_EMBEDDED:
                return EMBEDDED.format(ESCAPED.format(value));

            default:
                return value;
        }
    }
}