package br.gov.frameworkdemoiselle.fuselage.view.list;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import br.gov.frameworkdemoiselle.enumeration.contrib.Comparison;
import br.gov.frameworkdemoiselle.enumeration.contrib.Logic;
import br.gov.frameworkdemoiselle.fuselage.business.ResourceBC;
import br.gov.frameworkdemoiselle.fuselage.domain.SecurityResource;
import br.gov.frameworkdemoiselle.message.SeverityType;
import br.gov.frameworkdemoiselle.query.contrib.QueryConfig;
import br.gov.frameworkdemoiselle.stereotype.ViewController;
import br.gov.frameworkdemoiselle.template.contrib.AbstractListPageBean;
import br.gov.frameworkdemoiselle.transaction.Transactional;
import br.gov.frameworkdemoiselle.util.contrib.Faces;
import br.gov.frameworkdemoiselle.util.contrib.Strings;

@ViewController
public class ResourceListMB extends AbstractListPageBean<SecurityResource, Long> {
	private static final long serialVersionUID = 1L;

	@Inject
	private ResourceBC bc;

	@Override
	protected List<SecurityResource> handleResultList(QueryConfig<SecurityResource> queryConfig) {
		try {
			if (Strings.isNotBlank(getResultFilter())) {
				queryConfig.getFilter().put("name", getResultFilter());
				queryConfig.getFilter().put("value", getResultFilter());
				queryConfig.getFilter().put("description", getResultFilter());
				queryConfig.setFilterComparison(Comparison.CONTAINS);
				queryConfig.setFilterLogic(Logic.OR);
				queryConfig.setFilterCaseInsensitive(true);
			}
			if (!queryConfig.hasSorting())
				queryConfig.setSorting("name");
			return bc.findAll();
		} catch (RuntimeException e) {
			Faces.validationFailed();
			Faces.addI18nMessage("fuselage.generic.business.error", SeverityType.ERROR);
		}
		return new ArrayList<SecurityResource>();
	}

	@Transactional
	public String deleteSelection() {
		try {
			bc.delete(getSelectedList());
			initSelection();
		} catch (RuntimeException e) {
			Faces.validationFailed();
			Faces.addI18nMessage("fuselage.generic.business.error", SeverityType.ERROR);
		}
		return null;
	}

}
