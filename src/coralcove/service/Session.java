package coralcove.service;

import coralcove.model.User;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Signed-in user and a lightweight signal that screens should reload. */
public final class Session {

    private static User current;
    /** Null means every resort, which only an administrator may select. */
    private static Long resortFilter;
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();

    private Session() {
    }

    public static User get() {
        return current;
    }

    public static void set(User user) {
        current = user;
    }

    public static void clear() {
        current = null;
        resortFilter = null;
    }

    public static Long resortFilter() {
        return resortFilter;
    }

    public static void setResortFilter(Long resortId) {
        resortFilter = resortId;
        SettingsService.clearCache();
    }

    public static void onDataChanged(Runnable listener) {
        LISTENERS.add(listener);
    }

    public static void dataChanged() {
        for (Runnable listener : LISTENERS) {
            listener.run();
        }
    }
}
