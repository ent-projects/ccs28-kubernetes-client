/**
 * Copyright (2020, ) Institute of Software, Chinese Academy of Sciences
 */
package io.github.kubesys.watchers;

import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import io.github.kubesys.KubernetesClient;
import io.github.kubesys.KubernetesConfig;
import io.github.kubesys.KubernetesConstants;
import io.github.kubesys.KubernetesException;
import io.github.kubesys.KubernetesWatcher;
import io.github.kubesys.utils.URLUtils;

/**
 * @author wuheng@otcaix.iscas.ac.cn
 * 
 * @version 1.7.0
 * @since 2020.3.1
 * 
 **/
public class AutoDiscoverCustomizedResourcesWacther extends KubernetesWatcher {

	/**
	 * m_logger
	 */
	public static final Logger m_logger = Logger.getLogger(AutoDiscoverCustomizedResourcesWacther.class.getName());
	/**
	 * kind
	 */
	public static final String TARGET_KIND = "CustomResourceDefinition";
	
	/**
	 * default
	 */
	public static final String TARGET_NAMESPACE = "default";
	
	/**
	 * client
	 */
	protected final KubernetesClient client;
	
	/**
	 * config
	 */
	protected final KubernetesConfig config;

	public AutoDiscoverCustomizedResourcesWacther(KubernetesClient client) {
		this.client = client;
		this.config = client.getConfig();
	}


	@Override
	public void doAdded(JsonNode node) {
		JsonNode spec = node.get(KubernetesConstants.KUBE_SPEC);
		JsonNode names = spec.get(KubernetesConstants.KUBE_SPEC_NAMES);
		
		String kind = names.get(KubernetesConstants.KUBE_SPEC_NAMES_KIND).asText();
		if (config.getName(kind) != null) {
			return;
		}
		
		String name = names.get(KubernetesConstants.KUBE_SPEC_NAMES_PLURAL).asText();
		boolean namespaced = spec.get(KubernetesConstants.KUBE_SPEC_SCOPE).asText()
				.equals(KubernetesConstants.VALUE_NAMESPACED);
		String group = spec.get(KubernetesConstants.KUBE_SPEC_GROUP).asText();
		String version = spec.get(KubernetesConstants.KUBE_SPEC_VERSIONS)
							.iterator().next().get(KubernetesConstants
									.KUBE_SPEC_VERSIONS_NAME).asText();
		String url = URLUtils.join(client.getUrl(), KubernetesConstants
							.VALUE_APIS, group, version);
		
		config.addName(kind, name);
		config.addNamespaced(kind, namespaced);
		config.addGroup(kind, group);
		config.addVersion(kind, version);
		config.addApiPrefix(kind, url);
		m_logger.info("register " + kind + ": <" + group + "," 
											+ version + ","
											+ namespaced + ","
											+ url + ">");
	}


	@Override
	public void doDeleted(JsonNode node) {
		String kind = node.get(KubernetesConstants.KUBE_SPEC)
						.get(KubernetesConstants.KUBE_SPEC_NAMES)
						.get(KubernetesConstants.KUBE_SPEC_NAMES_KIND).asText();
		config.removeNameBy(kind);
		config.removeGroupBy(kind);
		config.removeVersionBy(kind);
		config.removeNamespacedBy(kind);
		config.removeApiPrefixBy(kind);
		
		m_logger.info("unregister " + kind);
	}

	@Override
	public void doModified(JsonNode node) {
		// Do nothing 

	}

	@Override
	public void doOnClose(KubernetesException execption) {
		this.client.watchResources(TARGET_KIND, TARGET_NAMESPACE, new AutoDiscoverCustomizedResourcesWacther(client));
	}
}
