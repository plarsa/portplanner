package se.portplanner.module;

/** A role's access level in a module. The order matters: NONE &lt; READ &lt; WRITE. */
public enum AccessLevel {
    NONE, READ, WRITE;

    public boolean atLeast(AccessLevel required) {
        return compareTo(required) >= 0;
    }

    public static AccessLevel max(AccessLevel a, AccessLevel b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    public String json() {
        return name().toLowerCase();
    }
}
