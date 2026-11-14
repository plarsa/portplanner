package se.portplanner.service;

/**
 * A reason a person may not be deleted. Modules contribute implementations, and PersonService runs
 * every one before it deletes; Spring injects an empty list when none exist, so base keeps no
 * reference to any module (DEP-4).
 */
public interface PersonDeletionCheck {

    /** Throws when this check refuses the deletion. */
    void check(Long personId);
}
