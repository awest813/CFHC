package desktop;

import simulation.FileSystemResourceProvider;
import simulation.PlatformResourceProvider;

import java.io.IOException;
import java.io.InputStream;

/**
 * Test-classpath shadow of {@link DesktopResourceProvider} that loads from the
 * repo filesystem via {@link FileSystemResourceProvider}. Prefer that type in
 * shared (non-desktop) tests so Android unit tests need no {@code desktop} package.
 */
public class DesktopResourceProvider implements PlatformResourceProvider {
    private final FileSystemResourceProvider delegate;

    public DesktopResourceProvider(String projectRoot) {
        this.delegate = new FileSystemResourceProvider(projectRoot);
    }

    @Override
    public String getString(String key) {
        return delegate.getString(key);
    }

    @Override
    public String getString(String key, Object... args) {
        return delegate.getString(key, args);
    }

    @Override
    public InputStream openAsset(String path) throws IOException {
        return delegate.openAsset(path);
    }
}
