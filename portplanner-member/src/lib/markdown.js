/**
 * Renders the small markdown subset a rental object description is written in: headings
 * (`#`, `##`), bold, italic, bullet lists and blank-line paragraphs. Anything else comes out
 * as plain text.
 *
 * The text is escaped before a single tag is emitted, so a description stored in the database
 * can never bring HTML of its own to the page. That is what makes the result safe to pass to
 * `v-html`, and why no sanitizing library is needed.
 */

const escapeHtml = (text) => text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')

/** Bold is replaced before italic, so a `*` inside `**...**` is already consumed. */
const inline = (text) => escapeHtml(text)
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/(^|[^*])\*([^*]+?)\*/g, '$1<em>$2</em>')

export function renderMarkdown(text) {
  if (!text) return ''
  const html = []
  let items = null

  const closeList = () => {
    if (items) {
      html.push(`<ul>${items.join('')}</ul>`)
      items = null
    }
  }

  for (const raw of text.replace(/\r\n/g, '\n').split('\n')) {
    const line = raw.trim()
    if (!line) {
      closeList()
      continue
    }
    const heading = /^(#{1,2})\s+(.*)$/.exec(line)
    const item = /^[-*]\s+(.*)$/.exec(line)
    if (heading) {
      // The cards use <h3> for the object's name, so a description heading starts below it.
      const level = heading[1].length + 3
      closeList()
      html.push(`<h${level}>${inline(heading[2])}</h${level}>`)
    } else if (item) {
      items ??= []
      items.push(`<li>${inline(item[1])}</li>`)
    } else {
      closeList()
      html.push(`<p>${inline(line)}</p>`)
    }
  }
  closeList()
  return html.join('')
}
