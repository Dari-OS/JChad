package net.jchad.shared.files;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Can be used to run a separate {@link Thread} checking for file system changes in a specific directory.
 */
public class PathWatcher extends Thread {
    /**
     * Path which will be watched.
     */
    private final Path path;

    /**
     * Code that gets called when an event was registered.
     * Also returns the {@link WatchEvent} that got registered.
     */
    private final Consumer<WatchEvent<?>> callback;

    /**
     * Code that gets called when an error occured.
     * Also returns the {@link Exception} that occured.
     */
    private final Consumer<Exception> errorCallback;

    /**
     * Defines if callback code should be executed when an event was registered.
     */
    private boolean running = true;

    /**
     * Set true to stop the PathWatcher.
     */
    private boolean exit = false;

    /**
     * Stores a timestamp of when this PathWatcher was started
     */
    private long startTimestamp;

    /**
     * Stores recently created files with a timestamp. This is used,
     * so recently created files aren't detected as modified until after a specified time.
     * This is needed, because when a new file is created with content, this still gets
     * detected as a modified file, because it gets created first, then modified.
     * This is technically correct behaviour, but not desired in this use case.
     */
    private final Map<Path, Long> recentlyCreatedFiles = new HashMap<>();

    /**
     * @param path     {@link Path} which will be watched
     * @param callback Code that gets called when an event was registered.
     *                 Also returns the {@link WatchEvent} that got registered.
     * @param errorCallback The method which will be called when the PathWatcher
     *                      runs into an error.
     */
    public PathWatcher(Path path, Consumer<WatchEvent<?>> callback, Consumer<Exception> errorCallback) {
        this.path = path;
        this.callback = callback;
        this.errorCallback = errorCallback;
    }

    /**
     * Returns <code>true</code> if server is running, <code>false</code> if not.
     *
     * @return <code>true</code> if server is running, <code>false</code> if not
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Pause the watching process.
     */
    public void pauseWatching() {
        running = false;
    }

    /**
     * Continue the watching process.
     */
    public void continueWatching() {
        running = true;
    }

    /**
     * Stops the PathWatcher thread gracefully.
     */
    public void stopWatcher() {
        exit = true;
        interrupt();
    }

    /**
     * Run the actual loop checking for file system modifications and creations in the specified path.
     */
    @Override
    public void run() {
        startTimestamp = System.currentTimeMillis();

        while (!exit) {
            try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
                path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);

                while (true) {
                    if (!isRunning()) continue;

                    final WatchKey key = watcher.take();

                    /*
                     * Let thread sleep for 100ms to prevent the same file edit from being recognized twice.
                     * This happens, because a file gets edited once for changing metadata and a second time
                     * for actually updating the file content.
                     */
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignore) {
                    }

                    for (final WatchEvent<?> event : key.pollEvents()) {
                        Path modifiedConfig = path.resolve((Path) event.context());

                        if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                            // Add newly created file to map with timestamp
                            recentlyCreatedFiles.put(modifiedConfig, System.currentTimeMillis());
                            callback.accept(event);
                        } else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                            if (!recentlyCreatedFiles.containsKey(modifiedConfig) ||
                                    System.currentTimeMillis() - recentlyCreatedFiles.get(modifiedConfig) > TimeUnit.MILLISECONDS.toMillis(100)) {
                                recentlyCreatedFiles.remove(modifiedConfig);
                                callback.accept(event);
                            }
                        } else {
                            callback.accept(event);
                        }
                    }

                    key.reset();
                }
            } catch (IOException | InterruptedException e) {
                errorCallback.accept(e);
            }
        }
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }
}