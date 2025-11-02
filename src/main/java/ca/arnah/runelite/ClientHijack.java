package ca.arnah.runelite;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

/**
 * @author Arnah
 * @since Nov 07, 2020
 */
@Slf4j
public class ClientHijack {
	
	public ClientHijack() {
		log.info("Searching for RuneLite injector");
		new Thread(() -> {
			while(RuneLite.getInjector() == null) {
				try {
					Thread.sleep(100);
				} catch(Exception ex) {
					log.error(ex.getMessage(), ex);
				}
			}
			log.info("RuneLite Injector located, starting HijackedClient.class thread");
			RuneLite.getInjector().getInstance(HijackedClient.class).start();
		}).start();
	}
}