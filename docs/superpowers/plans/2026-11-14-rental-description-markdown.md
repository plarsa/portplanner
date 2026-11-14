# Rental object description in markdown — implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The rental object description is written in a large box with formatting buttons, stored as markdown, and shown formatted in the admin app and the member portal, with the object's image on top.

**Architecture:** A pure `renderMarkdown(text)` function turns the stored text into escaped HTML for a fixed subset, used with `v-html` where the description is shown. One editor component in the admin app wraps a `<textarea>` with a toolbar that rewrites the textarea's selection and a preview rendered by the same function. The function is copied into both SPAs, as the `api/` modules already are.

**Tech Stack:** Vue 3 (script setup) + Vite in both SPAs. No new dependencies, no backend change, no migration.

**Spec:** `docs/superpowers/specs/2026-11-14-rental-description-markdown-design.md`

## Global Constraints

- No new dependencies in either SPA, runtime or dev.
- Code, comments, log and error messages in English; interface text in Swedish.
- `rental_objects.description` is already a CLOB and the DTOs pass the text through: no backend file is touched.
- The two SPAs share no build, so `renderMarkdown` is copied, not imported across apps.
- The markdown subset is exactly: `#` and `##` headings, `**bold**`, `*italic*`, `-` bullet list, blank-line paragraphs. Anything else renders as plain text.

## Review Focus

- A description containing `<script>` or any other HTML must render as visible text, never as markup: the text comes from the database and reaches the page through `v-html`.
- An empty or null description must render as nothing, not as the string `undefined` — both cards call the renderer on a value the API may omit.
- An unterminated marker (`**bold` with no closing `**`) must render as plain text, not swallow the rest of the description.
- A `*` inside `**bold**` must not be re-read as italic, so bold text does not come out mangled.
- The toolbar with nothing selected must still produce usable markdown at the cursor, since that is how someone starts a heading on an empty line.

---

### Task 1: The markdown renderer

**Files:**
- Create: `portplanner-frontend/src/lib/markdown.js`
- Verify with: a throwaway node script over a `.mjs` copy (the SPAs have no test runner and the plan adds none)

**Interfaces:**
- Produces: `renderMarkdown(text: string | null | undefined) => string`, HTML with `<h4>`, `<h5>`, `<strong>`, `<em>`, `<ul>/<li>` and `<p>` only. `#` maps to `<h4>` and `##` to `<h5>`, since the cards already use `<h3>` for the object name.

- [ ] **Step 1: Write the checks**

```js
assert.equal(renderMarkdown(''), '')
assert.equal(renderMarkdown(null), '')
assert.equal(renderMarkdown('# Regler'), '<h4>Regler</h4>')
assert.equal(renderMarkdown('## Regler'), '<h5>Regler</h5>')
assert.equal(renderMarkdown('Hej\n\nDu'), '<p>Hej</p><p>Du</p>')
assert.equal(renderMarkdown('- ett\n- två'), '<ul><li>ett</li><li>två</li></ul>')
assert.equal(renderMarkdown('en **fet** text'), '<p>en <strong>fet</strong> text</p>')
assert.equal(renderMarkdown('en *kursiv* text'), '<p>en <em>kursiv</em> text</p>')
assert.equal(renderMarkdown('<script>x</script>'), '<p>&lt;script&gt;x&lt;/script&gt;</p>')
assert.equal(renderMarkdown('en **halv fet'), '<p>en **halv fet</p>')
assert.equal(renderMarkdown('**a*b**'), '<p><strong>a*b</strong></p>')
```

- [ ] **Step 2: Run the checks and watch them fail** (module does not exist)

- [ ] **Step 3: Implement `renderMarkdown` in `portplanner-frontend/src/lib/markdown.js`**

Escape `& < > "` first, then apply inline markers, then build block elements line by line. Bold is replaced before italic so a `*` inside `**…**` is already consumed.

- [ ] **Step 4: Run the checks and watch them pass**

- [ ] **Step 5: Commit**

### Task 2: The editor component and the admin form

**Files:**
- Create: `portplanner-frontend/src/components/MarkdownEditor.vue`
- Modify: `portplanner-frontend/src/views/RentalObjectsView.vue` (the description field in the object form, and the description on the object card)

**Interfaces:**
- Consumes: `renderMarkdown` from Task 1.
- Produces: `<MarkdownEditor v-model="…" placeholder="…" rows="8" />`, emitting `update:modelValue` with the markdown text.

- [ ] **Step 1: Build the component**

A `<textarea>` with the toolbar buttons *Rubrik*, *Fet*, *Kursiv*, *Punktlista* above it and a preview below rendered with `renderMarkdown`. *Fet* and *Kursiv* wrap the selection in `**`/`*`; *Rubrik* and *Punktlista* prefix every line the selection touches with `## ` / `- `. With nothing selected, the wrapping buttons insert the markers with the word `text` between them and leave it selected, and the prefixing buttons prefix the line the cursor is on. After every change the textarea is refocused and the selection restored, so the buttons can be used in sequence.

- [ ] **Step 2: Use it in the object form in place of the one-line `<input>`**

- [ ] **Step 3: Render the description on the object card with `v-html`, keeping "Ingen beskrivning" when it is empty**

- [ ] **Step 4: Style the rendered elements on the card** (headings a little larger than the body text, lists indented, no margin collapse against the card edges)

- [ ] **Step 5: Run `npm run build` in `portplanner-frontend`**

Expected: build succeeds.

- [ ] **Step 6: Commit**

### Task 3: The member portal

**Files:**
- Create: `portplanner-member/src/lib/markdown.js` (copy of Task 1's file)
- Modify: `portplanner-member/src/views/RentalsView.vue` (description rendering, and the card layout)

- [ ] **Step 1: Copy the renderer into the member app**

- [ ] **Step 2: Render the description with `v-html`**

- [ ] **Step 3: Lay the card out as a column so the image sits on top**

`.obj-card` becomes `flex-direction: column`, the thumbnail spans the full width at the card's top with a fixed height, and `.obj-body` gets even padding on all sides.

- [ ] **Step 4: Style the rendered elements, as on the admin card**

- [ ] **Step 5: Run `npm run build` in `portplanner-member`**

Expected: build succeeds.

- [ ] **Step 6: Commit**

### Task 4: Release notes

**Files:**
- Modify: `docs/release-notes.md`

- [ ] **Step 1: Add one bullet under Unreleased for the formatted description**
- [ ] **Step 2: Commit**
