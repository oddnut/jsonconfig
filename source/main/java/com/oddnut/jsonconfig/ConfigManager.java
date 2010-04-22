/*
	ConfigManager.java

	Author: David Fogel
	Copyright 2010 David Fogel
	All rights reserved.
*/

package com.oddnut.jsonconfig;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConfigManager
 * 
 * Comment here.
 */
public class ConfigManager implements BundleActivator {
	// *** Class Members ***
	private static final String JSON_CONFIG_DIR_PROPERTY = "jsonconfig.dir";
	private static final String CONFIGURATION_AREA_PROPERTY = "osgi.configuration.area";
	private static final String CONFIGURATION_AREA_DEFAULT_PROPERTY = "osgi.configuration.area.default";
	
	private static ObjectMapper MAPPER = new ObjectMapper();
	
	private static Logger log = LoggerFactory.getLogger(ConfigManager.class);

	// *** Instance Members ***
	private File configDir;
	private Map<String, Config> configs;

	// *** Constructors ***
	public ConfigManager() {
		
		configs = new HashMap<String, Config>();
	}

	// *** BundleActivator Methods ***
	public void start(BundleContext bc) throws Exception {
		
		String configPath = bc.getProperty(JSON_CONFIG_DIR_PROPERTY);
		if (configPath == null)
			bc.getProperty(CONFIGURATION_AREA_PROPERTY);
		if (configPath == null)
			configPath = bc.getProperty(CONFIGURATION_AREA_DEFAULT_PROPERTY);
		
		configDir = new File(configPath);
		
		log.info("starting json config manager, config directory = {}", configDir.getAbsolutePath());
		
		if ( !configDir.exists()) {
			
			log.warn("no config directory exists at {}, so no configuration will be loaded", configDir.getAbsolutePath());
			
			return;
		}
		
		String[] filenames = configDir.list();
		Arrays.sort(filenames);
		
		for (String name : filenames) {
			
			if ( !isConfigFile(name))
				continue;
			
			Config c = new Config();
			
			c.file = new File(configDir, name);
			
			c.json = MAPPER.readValue(c.file, new TypeReference<Map<String, Object>>() {});
			
			Properties props = new Properties();
			props.put(Constants.SERVICE_PID, name);
			
			c.reg = bc.registerService(Map.class.getCanonicalName(), c.json, props);
		}
	}

	public void stop(BundleContext bc) throws Exception {
		log.info("stopping json config manager");
		
		for (Config c : configs.values()) {
			try {
				c.reg.unregister();
			}
			catch (IllegalStateException ise) {
				log.warn("couldn't unregister config service for config file {}", c.file);
			}
		}
	}

	// *** Public Methods ***

	// *** Protected Methods ***

	// *** Package Methods ***

	// *** Private Methods ***
	private boolean isConfigFile(String name) {
		
		return name.endsWith(".json");
	}

	// *** Private Classes ***
	private static class Config {
		File file;
		Map<String, Object> json;
		ServiceRegistration reg;
	}
}
