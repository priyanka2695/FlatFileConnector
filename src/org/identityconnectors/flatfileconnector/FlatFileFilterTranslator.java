
package org.identityconnectors.flatfileconnector;

import java.util.HashMap;
import java.util.Map;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.filter.AbstractFilterTranslator;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;

public class FlatFileFilterTranslator extends AbstractFilterTranslator<Map<String, String>> {

	@Override
	protected Map<String, String> createStartsWithExpression(StartsWithFilter startFilter, boolean not) {

		Map<String, String> startsMap = new HashMap<String, String>();
		System.out.println("#### Inside FlatFileFilterTranslator #### ");
		String queryString = new String();
		Attribute attr = startFilter.getAttribute();
		System.out.println(" ## Attribute Name " + attr.getName() + " Value" + attr.getValue().get(0).toString());
		startsMap.put(attr.getName(), attr.getValue().get(0).toString());
		return startsMap;

	}

}
