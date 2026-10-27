// GH-174: report=keys for harness.html. Plays steps (keys and clicks) against the game in an iframe and
// reports, after each, what focus, screen readers and the page's inert see, and what the live region
// says (GH-166). Query options: README.md.

/** One line per step, the first for the screen as `mode` left it. */
async function runKeys(win, steps, label) {
    const lines = [`${label} | start - -> ${snapshot(win)}`];
    for (const step of steps) {
        // Emptied, so the step's line shows only what that step says.
        const region = win.document.getElementById("announcer");
        if (region) region.textContent = "";
        const how = play(win, step);
        await redrawn(win);
        lines.push(`${label} | ${step} ${how} -> ${snapshot(win)}`);
    }
    return lines;
}

/** Plays [step]; `page` when the page took the key (preventDefault), `native` when the harness did
 *  what the browser would have, since a synthetic key gets no default action, else `-`. */
function play(win, step) {
    const doc = win.document;
    if (step.startsWith("click:")) {
        clickText(doc, step.slice(6)); // harness.html's
        return "-";
    }
    if (step.startsWith("focus:")) {
        doc.querySelector(`[data-focus="${step.slice(6)}"]`)?.focus();
        return "-";
    }
    const shift = step.startsWith("S-");
    // GH-164: R- is a held key's auto-repeat.
    const repeat = step.startsWith("R-");
    const name = shift || repeat ? step.slice(2) : step;
    const key = { Space: " " }[name] || name;
    const target = doc.activeElement || doc.body;
    const event = new win.KeyboardEvent("keydown", { key, shiftKey: shift, repeat, bubbles: true, cancelable: true });
    if (!target.dispatchEvent(event)) return "page";
    if (key === "Tab") {
        tab(doc, target, shift);
        return "native";
    }
    if ((key === "Enter" || key === " ") && target.tagName === "BUTTON") {
        target.click();
        return "native";
    }
    return "-";
}

/** Moves focus as Tab would. The page sets no positive tabindex, so the order is the DOM's; past
 *  either end focus leaves the page, as it goes to the browser's own controls. `visibilityProperty`
 *  skips the solution screen's arrows, hidden with `visibility` but still laid out. */
function tab(doc, from, back) {
    const order = [...doc.querySelectorAll("button, input, [href], [tabindex]")].filter(el =>
        !el.disabled && el.getAttribute("tabindex") !== "-1" && !el.closest("[inert]")
        && el.checkVisibility({ visibilityProperty: true }));
    const next = order[order.indexOf(from) + (back ? -1 : 1)] || (order.includes(from) ? null : order[back ? order.length - 1 : 0]);
    if (next) next.focus();
    else doc.activeElement?.blur();
}

/** Two frames, so the page has rebuilt #app, then a moment for the flow the key resumed and for
 *  DomAnnouncer's 100 ms refill of the live region. */
async function redrawn(win) {
    for (let i = 0; i < 2; i++) await new Promise(r => win.requestAnimationFrame(r));
    await new Promise(r => setTimeout(r, 150));
}

function snapshot(win) {
    const doc = win.document;
    const el = doc.activeElement === doc.body ? null : doc.activeElement;
    const dialog = doc.querySelector('[role="dialog"]');
    const title = dialog && doc.getElementById(dialog.getAttribute("aria-labelledby"));
    const said = flat(doc.getElementById("announcer")?.textContent);
    const inert = [...doc.querySelectorAll("[inert]")].map(e => e.tagName + [...e.classList].map(c => `.${c}`).join(""));
    return `focus ${el ? describe(doc, el) : "-"} | dialog ${title ? `"${flat(title.textContent)}"` : "-"} `
        + `| ring ${el ? win.getComputedStyle(el).outlineStyle : "-"} `
        + `| keyboard ${doc.querySelector(".app.keyboard") ? "yes" : "no"} | inert ${inert.join(" ") || "-"} `
        + `| said ${said ? `"${said}"` : "-"}` + (new URLSearchParams(location.search).get("grid") ? ` | ${grid(doc)}` : "");
}

/** GH-167: the board as a screen reader's table - its name, rows and cells, how many copies of a
 *  wrapping tile it would read, how many cells say `in place`, and the first row's cells - and
 *  the Read board button: `enabled`, `disabled` or `-`. */
function grid(doc) {
    const button = doc.querySelector('[data-focus="read-board"]');
    const read = `read-board ${button ? (button.disabled ? "disabled" : "enabled") : "-"}`;
    const table = doc.querySelector('[role="table"]');
    if (!table) return `grid - | ${read}`;
    const rows = table.querySelectorAll('[role="row"]');
    const cells = table.querySelectorAll('[role="cell"]');
    const unread = [...table.querySelectorAll(".tile")].filter(t => !t.matches('[role="cell"], [aria-hidden="true"]'));
    // What a reader hears from a cell: its text, less what is aria-hidden.
    const heard = c => flat([...c.childNodes].filter(n => !n.getAttribute?.("aria-hidden")).map(n => n.textContent).join(""));
    const home = [...cells].filter(c => heard(c).endsWith("in place")).length;
    const first = [...(rows[0]?.querySelectorAll('[role="cell"]') || [])].map(heard).join(" / ");
    return `grid table "${table.getAttribute("aria-label")}" rows ${rows.length} cells ${cells.length} `
        + `unread-copies ${unread.length} in-place ${home} row1 "${first}" | ${read}`;
}

/** Its data-focus key (or tag and text), role, accessible name and checked state. */
function describe(doc, el) {
    const role = el.getAttribute("role") || { BUTTON: "button", INPUT: "textbox", A: "link" }[el.tagName] || "-";
    const labelledBy = el.getAttribute("aria-labelledby");
    const name = el.getAttribute("aria-label") || (labelledBy && doc.getElementById(labelledBy)?.textContent) || el.textContent;
    const key = el.getAttribute("data-focus") || `${el.tagName} "${flat(el.textContent).slice(0, 20)}"`;
    return `${key} role ${role} name "${flat(name)}"${el.getAttribute("aria-checked") === "true" ? " checked" : ""}`;
}

const flat = text => (text || "").replace(/\s+/g, " ").trim();
