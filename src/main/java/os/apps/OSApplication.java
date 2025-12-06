package os.apps;

import javafx.scene.Parent;

/**
 * Base contract for every internal OS application.
 */
public interface OSApplication {
    String getName();

    Parent createContent();

    default void onStart() {}

    default void onStop() {}
}
