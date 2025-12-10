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

    /**
     * Called before the hosting window is closed. Applications may veto the close
     * request (for example when unsaved state exists) by returning {@code false}.
     */
    default boolean requestClose() {
        return true;
    }
}
