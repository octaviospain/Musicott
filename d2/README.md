# Diagrams

This directory holds the [d2](https://d2lang.com) source files for Musicott's
documentation diagrams. The `.d2` files here are the single source of truth;
the rendered SVGs live in the wiki repository and are referenced from wiki pages.

## Layout

| Source (`d2/`) | Rendered SVG (`../Musicott.wiki/images/`) | Used in |
|---|---|---|
| `architecture-overview.d2` | `architecture-overview.svg` | Home, Architecture |
| `event-flow.d2` | `event-flow.svg` | Architecture |
| `startup-lifecycle.d2` | `startup-lifecycle.svg` | Architecture, Install |
| `itunes-import-wizard.d2` | `itunes-import-wizard.svg` | Import |

## Rendering

Render a single diagram:

```bash
d2 d2/architecture-overview.d2 ../Musicott.wiki/images/architecture-overview.svg
```

Render all diagrams:

```bash
for f in d2/*.d2; do
  d2 "$f" "../Musicott.wiki/images/$(basename "${f%.d2}").svg"
done
```

Wiki pages reference the output as `![Description](images/name.svg)`.

## Adding a new diagram

1. Create `d2/<name>.d2`.
2. Render it to `../Musicott.wiki/images/<name>.svg` (command above).
3. Reference it from the relevant wiki page and add a row to the table above.
