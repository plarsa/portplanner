# Release notes

## Unreleased

- Landing page on the base URL with links to the member portal and the admin app.
- The member portal moved from `/` to `/member/`.
- The build version is logged at startup and shown in the footer on every page.
- Error messages, exceptions, log output, API documentation and comments are now in English.
- Portplanner ASCII art banner on startup.
- Fixed the SPA URLs with a trailing slash (`/admin/`, `/member/`) returning an error.
- A 404 is no longer reported as 403.
- The service now starts without a configured superadmin, using a generated password that is logged once.
- Fixed the SPA stylesheets and scripts being served as HTML, which made the browser reject them.
- Rentals: members book the club's trailer and day-sailing boats in a calendar and report faults on return.
- Uploaded files are stored through a pluggable file store, with the local filesystem as the first implementation.
