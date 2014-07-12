/**
 * $Id$
 * 
 * @author $Author$
 * @version $Revision$
 */
package org.tdar;

/**
 * @author Adam Brin
 * 
 */
public interface URLConstants {

    String HOME = "/";
    String DASHBOARD = "/dashboard";
    String WORKSPACE = "/workspace/list";
    String ADMIN = "/admin/internal";
    String PAGE_NOT_FOUND = "/page-not-found";
    String BOOKMARKS = DASHBOARD + "#bookmarks";
    String ENTITY_NAMESPACE = "browse/creators";
    String CART_ADD = "/cart/new";
    String COLUMNS_RESOURCE_ID = "columns?id=${resource.id}&startRecord=${startRecord}&recordsPerPage=${recordsPerPage}";
    String VIEW_RESOURCE_ID = "view?id=${resource.id}";
	String MY_PROFILE = "/entity/person/myprofile";
	String CHOOSE_BILLING_ACCOUNT = "/cart/choose-billing-account";
}
