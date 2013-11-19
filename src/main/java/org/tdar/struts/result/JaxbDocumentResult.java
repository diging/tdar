package org.tdar.struts.result;

import java.io.Writer;

import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.ServletActionContext;
import org.slf4j.Logger;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.Result;

import edu.asu.lib.jaxb.JaxbDocument;
import edu.asu.lib.jaxb.JaxbDocumentWriter;

@SuppressWarnings("unused")
public class JaxbDocumentResult implements Result {

    private static final long serialVersionUID = -8983164414828858380L;

    public static final String UTF_8 = "UTF-8";
    public static final String CONTENT_TYPE_XML = "application/xml;charset=UTF-8";
    public static final String FMT_ERROR_DOCUMENT_NOT_FOUND = ("Can not find a edu.asu.lib.jaxb.JaxbDocument " +
            "with the name [%s] in the invocation stack. " +
            "Check the <param name=\"documentName\"> tag specified for this action.");
    public static final String DEFAULT_PARAM = "documentName";

    private Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());
    private String documentName;
    private Boolean formatOutput;

    public JaxbDocumentResult() {
        super();
    }

    public JaxbDocumentResult(String documentName) {
        this();
        this.documentName = documentName;
        formatOutput = false;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public Boolean getFormatOutput() {
        return formatOutput;
    }

    public void setFormatOutput(Boolean formatOutput) {
        this.formatOutput = formatOutput;
    }

    @Override
    public void execute(ActionInvocation invocation) throws Exception {
        JaxbDocument jaxbDocument = (JaxbDocument) invocation.getStack().findValue(documentName);
        if (jaxbDocument == null) {
            String msg = String.format(FMT_ERROR_DOCUMENT_NOT_FOUND, documentName);
            logger.error(msg);
            throw new IllegalArgumentException(msg);
        }
        HttpServletResponse resp = ServletActionContext.getResponse();
        resp.setCharacterEncoding(UTF_8);
        resp.setContentType(CONTENT_TYPE_XML);
        Writer writer = null;
        try {
            writer = resp.getWriter();
            JaxbDocumentWriter.write(jaxbDocument, writer, formatOutput);
            logger.trace("Serving Jaxb result [{}]", documentName);
        } finally {
            if (writer != null)
                writer.close();
        }
    }
}
