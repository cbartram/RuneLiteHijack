package ca.arnah.runelite;

import ca.arnah.runelite.plugin.ArnahPluginManager;
import ca.arnah.runelite.plugin.config.ArnahPluginListPanel;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.ui.SplashScreen;

import javax.inject.Inject;

/**
 * @author Arnah
 * @since Nov 07, 2020
 */
@Slf4j
public class HijackedClient {
	
	private final ArnahPluginManager arnahPluginManager;
	
	private final ArnahPluginListPanel pluginListPanel;
	private final EventBus eventBus;
	private final ConfigManager configManager;
	private final RuneLiteHijackConfig runeLiteHijackConfig;
	
	@Inject
	public HijackedClient(ArnahPluginManager arnahPluginManager, ArnahPluginListPanel pluginListPanel, EventBus eventBus, ConfigManager configManager) {
		this.arnahPluginManager = arnahPluginManager;
		this.pluginListPanel = pluginListPanel;
		this.eventBus = eventBus;
		this.configManager = configManager;
		this.runeLiteHijackConfig = configManager.getConfig(RuneLiteHijackConfig.class);
	}
	
	public void start() {
		eventBus.register(this);
		log.info("Starting Hijacked client...");
		new Thread(()->{
			while(SplashScreen.isOpen()) {
				try{
					Thread.sleep(100);
				}catch(Exception ex){
					log.error(ex.getMessage(), ex);
				}
			}
			log.info("RuneLite splash screen completed, initializing plugin");
			try{
				pluginListPanel.init();
				startup();
				arnahPluginManager.loadExternalPlugins();
			} catch(Exception ex) {
				log.error(ex.getMessage(), ex);
			}
		}).start();
	}
	
	private void startup() {
		pluginListPanel.addFakePlugin("RuneLiteHijack", "RuneLiteHijack settings", new String[]{"pluginhub"}, runeLiteHijackConfig, configManager.getConfigDescriptor(runeLiteHijackConfig));
		refreshConfig(false);
	}
	
	
	@Subscribe
	public void onConfigChanged(ConfigChanged e) {
		if(e.getGroup().equals(RuneLiteHijackConfig.GROUP_NAME)) {
			refreshConfig(true);
		}
	}
	
	private void refreshConfig(boolean update) {
		System.setProperty(RuneLiteHijackProperties.PLUGINHUB_BASE, runeLiteHijackConfig.urls());
		if(update) {
			arnahPluginManager.update();
		}
	}
}