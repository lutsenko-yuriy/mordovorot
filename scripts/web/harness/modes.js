// harness.html's modes (split out of it, GH-267): what each does once the board is up, and the address a mode opens the page on.

/** The fragment the page opens with. mode=shared-labels is a visit to a share link (GH-267) to a 4x4 board that
 *  carries Pairs, which the default Numbers differ from, so the page asks about them. mode=shared-progress (GH-272)
 *  is one that carries the sender's progress, `n12`: no prompt, Mordie's opening line says it. */
const modeFragment = mode => ({ "shared-labels": "#b=TBEQERBJAA&l=pairs", "shared-progress": "#b=TBEQERBJAA&p=n12" })[mode] ?? "";

/** Gets the page to the screen [mode] names: the solution screen, a dialog, or neither. */
async function setUpMode(win, mode, step) {
    if (mode === "tutorial") {
        // GH-288: plays the shifts the steps before [step] ask: row 1 left, column 2 up, row 3 right twice, column 4 down.
        const asked = ["arrow-left-0", "arrow-up-1", "arrow-right-2", "arrow-right-2", "arrow-down-3"];
        const shifts = [0, 1, 2, 4, 5];
        for (const key of asked.slice(0, shifts[step - 1])) {
            win.document.querySelector(`[data-focus="${key}"]`)?.click();
            await wait(300);
        }
    }
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
        // One move first (GH-272): at 0 moves the progress toggle is dimmed.
        win.document.querySelector(".arrow")?.click();
        await wait(300);
        clickText(win.document, "Share");
        await wait(300);
    }
}
