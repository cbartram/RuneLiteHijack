package ca.arnah.runelite;

import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Hijacks the RuneLite launcher to inject custom client code.
 */
@Slf4j
public class LauncherHijack {

    private static final long CLASSLOADER_POLL_INTERVAL_MS = 100;
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 10;
    private static final String RUNELITE_PACKAGE = "net.runelite.client.rs";
    private static final String LAUNCHER_CLASS = "net.runelite.launcher.Launcher";

    private final ExecutorService executorService;

    public LauncherHijack() {
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "LauncherHijack-Worker");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Starts the hijack process asynchronously.
     */
    public void start() {
        executorService.submit(this::hijackLauncher);
    }

    /**
     * Shuts down the executor service gracefully.
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void hijackLauncher() {
        try {
            // TODO In Hydra this is never found
            ClassLoader runeliteClassLoader = waitForRuneLiteClassLoader();
            log.info("RuneLite ClassLoader located: {}", runeliteClassLoader.getName());

            injectHijackClient(runeliteClassLoader);
        } catch (InterruptedException e) {
            log.warn("Hijack process interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Failed to hijack launcher", e);
        }
    }

    /**
     * Polls for the RuneLite ClassLoader until it's available.
     */
    private ClassLoader waitForRuneLiteClassLoader() throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            ClassLoader classLoader = (ClassLoader) UIManager.get("ClassLoader");

            if (classLoader != null && isRuneLiteClassLoader(classLoader)) {
                return classLoader;
            }

            Thread.sleep(CLASSLOADER_POLL_INTERVAL_MS);
        }
        throw new InterruptedException("Interrupted while waiting for ClassLoader");
    }

    /**
     * Checks if the given ClassLoader contains RuneLite packages.
     */
    private boolean isRuneLiteClassLoader(ClassLoader classLoader) {
        for (Package pack : classLoader.getDefinedPackages()) {
            log.info("Classloader package name: {}", pack.getName());
            if (pack.getName().equals(RUNELITE_PACKAGE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Injects the hijack client into the RuneLite ClassLoader.
     */
    private void injectHijackClient(ClassLoader classLoader) throws Exception {
        if (!(classLoader instanceof URLClassLoader)) {
            throw new IllegalStateException("ClassLoader is not a URLClassLoader");
        }

        URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
        URL hijackJarUrl = resolveHijackJarUrl();

        addUrlToClassLoader(urlClassLoader, hijackJarUrl);
        log.info("Added hijack JAR to ClassLoader: {}", hijackJarUrl);

        instantiateHijackClient(urlClassLoader);
    }

    /**
     * Resolves the URL of the hijack JAR file.
     */
    private URL resolveHijackJarUrl() throws Exception {
        URI uri = LauncherHijack.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI();

        // Handle IntelliJ classes directory
        if (uri.getPath().endsWith("classes/")) {
            uri = uri.resolve("..");
        }

        // Handle non-JAR paths
        if (!uri.getPath().endsWith(".jar")) {
            uri = uri.resolve("RuneLiteHijack.jar");
        }

        return uri.toURL();
    }

    /**
     * Adds a URL to the URLClassLoader using reflection.
     */
    private void addUrlToClassLoader(URLClassLoader classLoader, URL url) throws Exception {
        Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        addUrl.setAccessible(true);
        addUrl.invoke(classLoader, url);
    }

    /**
     * Instantiates the ClientHijack class in the RuneLite ClassLoader.
     */
    private void instantiateHijackClient(ClassLoader classLoader) throws Exception {
        Class<?> hijackClass = classLoader.loadClass(ClientHijack.class.getName());
        hijackClass.getConstructor().newInstance();
        log.info("ClientHijack class instantiated successfully");
    }

    public static void main(String[] args) {
        log.info("Starting RuneLite Hijack");

        System.setProperty("runelite.launcher.nojvm", "true");
        System.setProperty("runelite.launcher.reflect", "true");

        LauncherHijack hijack = new LauncherHijack();
        hijack.start();

        try {
            Class<?> launcherClass = Class.forName(LAUNCHER_CLASS);
            launcherClass.getMethod("main", String[].class).invoke(null, (Object) args);
            log.info("RuneLite Launcher started successfully");
        } catch (Exception e) {
            log.error("Failed to start RuneLite launcher", e);
            hijack.shutdown();
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(hijack::shutdown, "LauncherHijack-Shutdown"));
    }
}