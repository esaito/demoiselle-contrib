package br.gov.frameworkdemoiselle.ldap.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.ietf.ldap.LDAPAttribute;
import org.ietf.ldap.LDAPConnection;
import org.ietf.ldap.LDAPEntry;
import org.ietf.ldap.LDAPException;
import org.ietf.ldap.LDAPReferralException;
import org.ietf.ldap.LDAPSearchConstraints;
import org.ietf.ldap.LDAPSearchResults;
import org.slf4j.Logger;

import br.gov.frameworkdemoiselle.internal.producer.LoggerProducer;
import br.gov.frameworkdemoiselle.ldap.configuration.EntryManagerConfig;
import br.gov.frameworkdemoiselle.ldap.internal.ConnectionManager;

public class EntryQuery {

	private Logger logger = LoggerProducer.create(EntryQuery.class);
	@Inject
	private EntryManagerConfig entryManagerConfig;
	@Inject
	private ConnectionManager conn;
	private String ldapFilter;
	private String[] resultAttributes;
	private Integer sizeLimit;
	private String basedn;
	private int scope;
	private LDAPSearchConstraints ldapConstraints = new LDAPSearchConstraints();

	@PostConstruct
	public void init() {
		ldapFilter = "(objectClass=*)";
		scope = LDAPConnection.SCOPE_SUB;
		ldapConstraints.setMaxResults(sizeLimit);
		basedn = entryManagerConfig.getBasedn();
		sizeLimit = entryManagerConfig.getSearchSizelimit();
	}

	public void setMaxResults(int maxResult) {
		this.ldapConstraints.setMaxResults(maxResult);
	}

	public void setBaseDn(String basedn) {
		this.basedn = basedn;
	}

	public void setSearchFilter(String searchFilter) {
		this.ldapFilter = searchFilter;
	}

	public void setResultAttributes(String... resultAttributes) {
		this.resultAttributes = resultAttributes;
	}

	private LDAPConnection getConnection() {
		return this.conn.initialized();
	}

	/**
	 * 
	 * @param searchFilter
	 * @param resultAttributes
	 * @return
	 */
	private Map<String, LDAPEntry> find() {
		Map<String, LDAPEntry> resultMap = new HashMap<String, LDAPEntry>();
		try {
			LDAPSearchResults searchResults = getConnection().search(basedn, scope, ldapFilter, resultAttributes,
					false, ldapConstraints);
			while (searchResults != null && searchResults.hasMore()) {
				try {
					LDAPEntry entry = searchResults.next();
					resultMap.put(entry.getDN(), entry);
				} catch (LDAPReferralException e) {
					// Ignore referrals;
				} catch (LDAPException e) {
					// Catch size limit exceeded;
					if (e.getResultCode() != LDAPException.SIZE_LIMIT_EXCEEDED) {
						throw e;
					}
				}
			}
		} catch (LDAPException e) {
			e.printStackTrace();
		}
		return resultMap;
	}

	/**
	 * 
	 * @return
	 */
	public Map<String, Map<String, String[]>> getResult() {
		Map<String, LDAPEntry> searchResult = new HashMap<String, LDAPEntry>();
		Map<String, Map<String, String[]>> resultMap = new HashMap<String, Map<String, String[]>>();

		Iterator<String> itrDn = find().keySet().iterator();
		while (itrDn.hasNext()) {
			String dn = itrDn.next();
			Map<String, String[]> entryMap = new HashMap<String, String[]>();
			@SuppressWarnings("unchecked")
			Iterator<LDAPAttribute> itrAttr = searchResult.get(dn).getAttributeSet().iterator();
			while (itrAttr.hasNext()) {
				LDAPAttribute attr = itrAttr.next();
				entryMap.put(attr.getName(), attr.getStringValueArray());
			}
			resultMap.put(dn, entryMap);
		}
		return resultMap;
	}

	/**
	 * 
	 * @return
	 */
	public Map<String, String[]> getSingleResult() {
		Map<String, LDAPEntry> searchResult = new HashMap<String, LDAPEntry>();
		Map<String, String[]> returnResult = new HashMap<String, String[]>();

		searchResult = find();
		if (searchResult.size() == 1) {
			Iterator<LDAPEntry> itr = searchResult.values().iterator();
			while (itr.hasNext() == true) {
				@SuppressWarnings("unchecked")
				Iterator<LDAPAttribute> itrAttr = itr.next().getAttributeSet().iterator();
				while (itrAttr.hasNext()) {
					LDAPAttribute attr = itrAttr.next();
					returnResult.put(attr.getName(), attr.getStringValueArray());
				}
			}
		}
		return returnResult;
	}

	/**
	 * 
	 * @return
	 */
	public Map<String, String> getSingleAttributesResult() {
		Map<String, String[]> searchResult = getSingleResult();
		Map<String, String> returnResult = new HashMap<String, String>();
		Iterator<String> iter = searchResult.keySet().iterator();
		while (iter.hasNext()) {
			String attr = iter.next();
			String[] valuesList = searchResult.get(attr);
			if (valuesList.length > 0)
				returnResult.put(attr, valuesList[0]);
		}
		return returnResult;
	}

	/**
	 * Set result attributes is required.
	 * 
	 * @return
	 */
	public String getSingleAttributeResult() {
		String result = new String();
		if (resultAttributes == null || resultAttributes.length == 0)
			return result;

		Map<String, String> resultMap = getSingleAttributesResult();
		for (String attr : resultAttributes) {
			if (resultMap.containsKey(attr)) {
				return resultMap.get(attr);
			}
		}
		return result;
	}

	/**
	 * Execute a LDAP SEARCH query and return a single Distinguished Name or
	 * null none or multiples results.
	 * 
	 * @return the result
	 */
	public String getSingleDn() {
		List<String> dnList = getDnList();
		if (dnList.size() == 1) {
			return dnList.get(0);
		} else {
			return null;
		}
	}

	/**
	 * Execute a LDAP SEARCH query and return a List of Distinguished Names.
	 * 
	 * @return a list of the Distinguished Names
	 */
	public List<String> getDnList() {
		List<String> dnList = new ArrayList<String>();
		resultAttributes = new String[] { "objectClass" };
		Iterator<String> itr = find().keySet().iterator();
		while (itr.hasNext() == true) {
			dnList.add(itr.next());
		}
		resultAttributes = null;
		return dnList;
	}

	/**
	 * Execute a LDAP SEARCH query and return a List of Attributes values. Set
	 * result attributes is required.
	 * 
	 * @return a list of the Attributes values
	 */
	public List<String> getAttributeList() {
		if (resultAttributes == null || resultAttributes.length == 0)
			return new ArrayList<String>();

		List<String> resultList = new ArrayList<String>();
		Iterator<LDAPEntry> itr = find().values().iterator();
		while (itr.hasNext()) {
			LDAPEntry entry = itr.next();
			for (int i = 0; i != resultAttributes.length; i++) {
				LDAPAttribute attr = entry.getAttribute(resultAttributes[i]);
				String[] attrArray = attr.getStringValueArray();
				for (int y = 0; y != attrArray.length; y++) {
					resultList.add(attrArray[y]);
				}
			}
		}
		return resultList;
	}

	/**
	 * if isn't a search filter, convert to a valid search filter by template.
	 * 
	 * @param searchFilterTemplate
	 * @param maybeSearchFilter
	 * @return
	 */
	@SuppressWarnings("unused")
	private String getSearchFilter(String searchFilterTemplate, String maybeSearchFilter) {
		String searchFilter = "(invalidFilter=*)";
		if (maybeSearchFilter == null || !maybeSearchFilter.contains("=")) {
			if (searchFilterTemplate == null || !searchFilterTemplate.contains("=")) {
				logger.warn("Search filter template must have RFC 2254 sintax.");
			} else {
				searchFilter = searchFilterTemplate.replaceAll("%s", maybeSearchFilter);
			}
		} else {
			searchFilter = maybeSearchFilter;
		}
		return searchFilter;
	}

}
