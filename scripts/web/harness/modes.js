// harness.html's modes (split out of it, GH-267): what each does once the board is up, and the address a mode opens the page on.

/** The fragment the page opens with. mode=shared-labels is a visit to a share link (GH-267) to a 4x4 board that
 *  carries Pairs, which the default Numbers differ from, so the page asks about them. */
const modeFragment = mode => mode === "shared-labels" ? "#b=TBEQERBJAA&l=pairs" : "";

/** Gets the page to the screen [mode] names: the solution screen, a dialog, or neither. */
async function setUpMode(win, mode) {
    if (mode === "solution") {
        // Mordie reveals its solution at a check every 5 shifts, so keep shifting until Give Up lights up.
        for (let i = 0; i < 80 && win.document.querySelector(".give-up button")?.disabled !== false; i++) {
            const arrows = [...win.document.querySelectorAll(".arrow")];
            arrows[i % 2 ? 0 : arrows.length - 1]?.click();
            await wait(400);
        }
        clickText(win.document, "Give Up");
        await wait(300);
        clickText(win.document, "Yes");
        await wait(500);
    }
    const line = win.document.querySelector(".mordie .line");
    if (line && mode !== "offline") line.innerHTML = `<span class="name">Mordie:</span> ${LONG}`;
    if (mode === "dialog") {
        clickText(win.document, "Labels");
        await wait(300);
    }
    if (mode === "share") {
        clickText(win.document, "Share");
        await wait(300);
    }
}
