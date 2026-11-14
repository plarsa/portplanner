# Rental objects: a formatted description stored as markdown

## Intent and why

A rental object's description is a single-line `<input>`, so the harbour office can only write one
short sentence about the trailer or the day-sailing boat. They need room for a heading followed by
text, written without knowing markdown. The description is shown to members in the member portal,
which is where the text actually matters.

## Goals

- The description is edited in a large box with buttons for heading, bold, italic and bullet list,
  applied to the selected text.
- What is stored in the database is markdown.
- The description renders formatted both on the admin object card and in the member portal.
- The uploaded image sits at the top of the card, above the text, in both apps.

## Non-goals

- Images inside the description text. The object's one uploaded image stays separate, uploaded as
  it is today.
- Links, tables, quotes, code blocks or raw HTML in the description.
- Markdown anywhere else. Fault reports and every other free-text field are untouched.

## Anti-goals

- An editor whose box shows formatted text while typing (WYSIWYG). That was approach B and costs
  5-10 npm packages for the single editor in the project.
- Any sanitizing dependency. The renderer escapes the text before it emits tags, so no untrusted
  HTML can reach the page.

## Constraints

- Vue 3 + Vite. No new dependencies in either SPA, as in the account-passwords change.
- Code, comments, log and error messages in English; interface text in Swedish.
- No backend change and no migration: `rental_objects.description` is already a CLOB and the DTOs
  pass the text through untouched.

## Approaches considered

- **A - textarea with formatting buttons and a preview (chosen).** The box is a `<textarea>`, a
  narrow button row above it wraps or prefixes the selection with markdown, and the rendered result
  is shown under the box. A small renderer, written here, handles the display in both apps. No new
  dependencies, and the box itself shows markdown while typing.
- **B - a real WYSIWYG editor** (tiptap with markdown serialization). The text looks formatted while
  typing, but it brings 5-10 packages into the admin app and the member portal still needs a
  renderer. Rejected as too much surface for one field.
- **C - a plain large textarea with `marked` and `dompurify`.** Least code, but the description has
  to be written in markdown by hand, which is what the office should not have to do.

## The how, as decided

- **The markdown subset** is heading (`#`, `##`), bold, italic, bullet list and blank-line
  paragraphs. Nothing else is recognised; anything unrecognised renders as plain text.
- **The renderer** is a function in a module of its own, used with `v-html` on an already escaped
  string. It is copied into both apps, the way `api/rentals.js` and `api/axios.js` already are:
  the two SPAs share no code and this change does not introduce a way for them to.
- **The editor** is one component in the admin app, used by the rental object form. The toolbar acts
  on the textarea's selection, so pressing *Rubrik* on a selected line prefixes it with `## `.
- **The member portal card** changes from a row to a column so the image sits on top. The admin card
  is already a column and keeps its layout.
- **Long descriptions** are rendered in full on both cards. Descriptions are short in practice and
  clamping them would hide the heading the office just wrote.

## Left to the builder

- Whether the preview sits inside the editor component or in the form next to it.
- The exact Swedish labels on the toolbar buttons.
- The heading levels the renderer maps to, and their styling on the two cards.
- Whether the editor component is reused for the fault report description (it should not be, but
  the component must not prevent it).
