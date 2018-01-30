package org.tdar.core.bean.notification.aws;

import java.util.Arrays;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.tdar.core.bean.entity.TdarUser;
import org.tdar.core.bean.notification.Email;
import org.tdar.core.configuration.TdarConfiguration;
import org.tdar.utils.MessageHelper;

@Entity
@DiscriminatorValue("INVITE_MESSAGE")
public class InviteMessage extends Email {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4517475490706454849L;

	@Override
	public String createSubjectLine() {
		String properName = ((TdarUser) getMap().get("from")).getProperName();
		String tdar = TdarConfiguration.getInstance().getSiteAcronym();
		return MessageHelper.getMessage("EmailType.INVITE",Arrays.asList(properName,tdar));
	}

}
