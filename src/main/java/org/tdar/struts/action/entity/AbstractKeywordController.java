package org.tdar.struts.action.entity;

import org.springframework.beans.factory.annotation.Autowired;
import org.tdar.core.bean.Persistable;
import org.tdar.core.bean.keyword.Keyword;
import org.tdar.core.bean.keyword.KeywordType;
import org.tdar.core.service.GenericKeywordService;
import org.tdar.struts.action.AuthenticationAware;

import com.opensymphony.xwork2.Preparable;

public abstract class AbstractKeywordController extends AuthenticationAware.Base implements Preparable {

    private static final long serialVersionUID = -7469398370759336245L;

    @Autowired
    private transient GenericKeywordService genericKeywordService;

    private Long id;
    private KeywordType keywordType;
    private Keyword keyword;

    public Keyword getKeyword() {
        return keyword;
    }

    public void setKeyword(Keyword keyword) {
        this.keyword = keyword;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public KeywordType getKeywordType() {
        return keywordType;
    }

    public void setKeywordType(KeywordType keywordType) {
        this.keywordType = keywordType;
    }

    @Override
    public void prepare() throws Exception {
        if (Persistable.Base.isNullOrTransient(getId())) {
            addActionError(getText("simpleKeywordAction.id_required"));
        }
        if (getKeywordType() == null) {
            addActionError(getText("simpleKeywordAction.type_required"));
        }

        setKeyword(genericKeywordService.find(getKeywordType().getKeywordClass(), getId()));
    }

}
