/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.product.navigation.control.menu.util;

import com.liferay.osgi.service.tracker.collections.map.PropertyServiceReferenceComparator;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMap;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMapFactory;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.PredicateFilter;
import com.liferay.product.navigation.control.menu.ProductNavigationControlMenuCategory;
import com.liferay.product.navigation.control.menu.ProductNavigationControlMenuEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Julio Camarero
 */
@Component(
	immediate = true,
	service = ProductNavigationControlMenuCategoryRegistry.class
)
public class ProductNavigationControlMenuCategoryRegistry {

	public List<ProductNavigationControlMenuCategory> getControlMenuCategories(
		String productControlMenuCategoryKey) {

		List<ProductNavigationControlMenuCategory> productControlMenuCategories =
			_productControlMenuCategoryServiceTrackerMap.getService(
				productControlMenuCategoryKey);

		if (productControlMenuCategories == null) {
			return Collections.emptyList();
		}

		return new ArrayList<>(productControlMenuCategories);
	}

	public List<ProductNavigationControlMenuCategory> getControlMenuCategories(
		String productControlMenuCategoryKey, final HttpServletRequest request) {

		List<ProductNavigationControlMenuCategory> productControlMenuCategories =
			getControlMenuCategories(productControlMenuCategoryKey);

		if (productControlMenuCategories.isEmpty()) {
			return productControlMenuCategories;
		}

		return ListUtil.filter(
			productControlMenuCategories,
			new PredicateFilter<ProductNavigationControlMenuCategory>() {

				@Override
				public boolean filter(
					ProductNavigationControlMenuCategory productControlMenuCategory) {

					try {
						if (!productControlMenuCategory.hasAccessPermission(request)) {
							return false;
						}

						List<ProductNavigationControlMenuEntry>
							controlMenuEntries =
								_controlMenuEntryRegistry.getControlMenuEntries(
									productControlMenuCategory, request);

						if (controlMenuEntries.isEmpty()) {
							return false;
						}

						return true;
					}
					catch (PortalException pe) {
						_log.error(pe, pe);
					}

					return false;
				}

			});
	}

	@Activate
	protected void activate(final BundleContext bundleContext) {
		_productControlMenuCategoryServiceTrackerMap =
			ServiceTrackerMapFactory.openMultiValueMap(
				bundleContext, ProductNavigationControlMenuCategory.class,
				"(product.navigation.control.menu.category.key=*)",
				new ProductNavigationControlMenuCategoryServiceReferenceMapper(),
				Collections.reverseOrder(
					new PropertyServiceReferenceComparator("service.ranking")));
	}

	@Deactivate
	protected void deactivate() {
		_productControlMenuCategoryServiceTrackerMap.close();
	}

	@Reference(unbind = "-")
	protected void setControlMenuEntryRegistry(
		ProductNavigationControlMenuEntryRegistry controlMenuEntryRegistry) {

		_controlMenuEntryRegistry = controlMenuEntryRegistry;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		ProductNavigationControlMenuCategoryRegistry.class);

	private ServiceTrackerMap
		<String, List<ProductNavigationControlMenuCategory>>
			_productControlMenuCategoryServiceTrackerMap;
	private ProductNavigationControlMenuEntryRegistry _controlMenuEntryRegistry;

}