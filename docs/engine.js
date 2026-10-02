/* QA OPS demo engine — faithful client-side simulation of SmartLocator. */
(function () {
  "use strict";
  const $ = (id) => document.getElementById(id);
  const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

  /* ---------- mascot: Bolt ---------- */
  const bolt = $("bolt");
  async function boltPress(btn, travelMs) {
    const r = btn.getBoundingClientRect();
    const b = bolt.getBoundingClientRect();
    bolt.style.position = "fixed";
    bolt.style.left = (r.left + r.width / 2 - 37) + "px";
    bolt.style.top = (r.top - 108) + "px";
    bolt.style.right = "auto"; bolt.style.bottom = "auto";
    await sleep(travelMs || 1100);
    bolt.classList.add("pressing");
    btn.classList.add("pressed");
    await sleep(280);
    bolt.classList.remove("pressing");
    btn.classList.remove("pressed");
    await sleep(250);
  }
  function boltCelebrate() {
    bolt.style.transform = "rotate(-8deg)";
    setTimeout(() => (bolt.style.transform = "rotate(8deg)"), 180);
    setTimeout(() => (bolt.style.transform = ""), 420);
  }

  /* ---------- healing playground ---------- */
  // Mirrors LoginPage.java: each element = primary + ordered fallbacks.
  const ELEMENTS = {
    username: {
      name: "username field",
      el: () => document.getElementById("user-name") || document.querySelector("[data-test='username']"),
      chain: [
        { by: "By.id", sel: "#user-name", broken: () => !document.getElementById("user-name") },
        { by: "By.cssSelector", sel: "[data-test='username']" },
        { by: "By.xpath", sel: "//input[@placeholder='Username']" },
      ],
    },
    password: {
      name: "password field",
      chain: [
        { by: "By.id", sel: "#password", broken: () => !document.getElementById("password") },
        { by: "By.cssSelector", sel: "[data-test='password']" },
      ],
    },
    loginBtn: {
      name: "login button",
      chain: [
        { by: "By.id", sel: "#login-button", broken: () => !document.getElementById("login-button") },
        { by: "By.cssSelector", sel: "[data-test='login-button']" },
        { by: "By.xpath", sel: "//input[@type='submit']", never: true },
      ],
    },
  };
  const report = { primary: 0, healed: 0, failed: 0, events: [] };
  const con = $("console"), rep = $("report");
  let broken = null, running = false;

  function log(html, cls) {
    const d = document.createElement("div");
    d.className = "step " + (cls || "");
    d.innerHTML = html;
    con.appendChild(d);
    con.scrollTop = con.scrollHeight;
  }
  function clearBox() { con.innerHTML = ""; rep.classList.remove("show"); }

  function query(loc) {
    if (loc.never) return null;
    if (loc.broken && loc.broken()) return null; // primary is stale after the UI change
    if (loc.sel.startsWith("#")) return document.getElementById(loc.sel.slice(1));
    if (loc.sel.startsWith("[data-test")) {
      const m = loc.sel.match(/data-test='([^']+)'/);
      return document.querySelector(`[data-test='${m[1]}']`);
    }
    if (loc.sel.startsWith("//input[@placeholder")) {
      const m = loc.sel.match(/placeholder='([^']+)'/);
      return document.querySelector(`input[placeholder='${m[1]}']`);
    }
    return null;
  }

  async function resolveElement(key) {
    const def = ELEMENTS[key];
    for (let i = 0; i < def.chain.length; i++) {
      const loc = def.chain[i];
      const tag = i === 0 ? "primary" : `fallback ${i}`;
      log(`<span class="t-dim">▸ trying ${tag}</span> <span class="t-cyan">${loc.by}(\`${loc.sel}\`)</span> …`);
      await sleep(420);
      const found = query(loc);
      if (found) {
        if (i === 0) { report.primary++; log(`<span class="t-ok">✓ resolved via primary locator</span>`); }
        else {
          report.healed++;
          report.events.push(`${def.name} → healed via ${loc.by}: ${loc.sel}`);
          log(`<span class="t-warn">⚠ primary failed — <b>SELF-HEALED</b> via ${loc.by}: ${loc.sel}</span>`);
        }
        return found;
      }
      log(`<span class="t-bad">✗ not found</span>`);
      await sleep(200);
    }
    report.failed++;
    return null;
  }

  async function runTest() {
    if (running) return;
    running = true;
    clearBox();
    log(`<span class="t-vio">━━━ scenario: "Successful login with a standard user" ━━━</span>`);
    await sleep(300);
    const u = await resolveElement("username");
    if (!u) return fail("username field");
    u.value = "standard_user";
    log(`<span class="t-dim">typing username…</span>`);
    await sleep(300);
    const p = await resolveElement("password");
    if (!p) return fail("password field");
    p.value = "secret_sauce";
    await sleep(300);
    const b = await resolveElement("loginBtn");
    if (!b) return fail("login button");
    log(`<span class="t-ok">✓ clicked login — inventory page loaded</span>`);
    await sleep(300);
    log(`<span class="t-vio">━━━ scenario PASSED ━━━</span>`);
    [u, p].forEach((x) => x.classList.remove("healed"));
    if (report.healed) {
      document.querySelectorAll(".bpage input").forEach((x) => { if (broken && x.placeholder.toLowerCase().includes(broken)) x.classList.add("healed"); });
    }
    showReport();
    boltCelebrate();
    running = false;
  }
  function fail(what) {
    log(`<span class="t-bad">✗✗ SCENARIO FAILED — could not locate ${what}</span>`);
    showReport();
    running = false;
  }
  function showReport() {
    const total = report.primary + report.healed + report.failed;
    rep.innerHTML =
      `<div class="t-vio">==== Self-Healing Summary ====</div>` +
      `<div><span class="t-dim">Total lookups:</span> ${total}</div>` +
      `<div><span class="t-dim">Resolved via primary locator:</span> ${report.primary}</div>` +
      `<div><span class="t-ok">Self-healed via fallback: ${report.healed}</span></div>` +
      `<div><span class="t-dim">Failed (no locator worked):</span> ${report.failed}</div>` +
      report.events.map((e) => `<div class="t-warn">  - ${e}</div>`).join("");
    rep.classList.add("show");
  }

  const BREAKS = [
    { key: "username", id: "user-name", label: "username field id → “user-name-v2”" },
    { key: "password", id: "password", label: "password field id → “password-v2”" },
    { key: "loginBtn", id: "login-button", label: "login button id → “login-button-v2”" },
  ];
  let breakIdx = 0;
  $("breakBtn").addEventListener("click", async (e) => {
    const b = BREAKS[breakIdx % BREAKS.length]; breakIdx++;
    const elm = document.getElementById(b.id);
    if (elm) elm.id = b.id + "-v2";
    broken = b.key === "username" ? "username" : b.key === "password" ? "password" : null;
    $("chaosWhat").textContent = b.label;
    $("chaosNote").style.display = "block";
    const btn = e.currentTarget;
    btn.classList.add("armed"); btn.textContent = "🔧 UI broken — run the test!";
    log(`<span class="t-warn">🔧 chaos: developer renamed ${b.label} — primary locator is now stale</span>`);
    await boltPress(btn, 700);
  });
  $("runBtn").addEventListener("click", async (e) => { await boltPress(e.currentTarget, 900); runTest(); });
  $("resetBtn").addEventListener("click", () => {
    document.getElementById("user-name-v2")?.setAttribute("id", "user-name");
    document.getElementById("password-v2")?.setAttribute("id", "password");
    document.getElementById("login-button-v2")?.setAttribute("id", "login-button");
    document.querySelectorAll(".bpage input").forEach((x) => { x.classList.remove("healed", "broken"); x.value = ""; });
    $("chaosNote").style.display = "none";
    const bb = $("breakBtn"); bb.classList.remove("armed"); bb.textContent = "🔧 Break the UI (rename an id)";
    report.primary = report.healed = report.failed = 0; report.events = [];
    broken = null; clearBox();
    log(`<span class="t-dim">// fresh page — primaries intact. Break the UI, then run.</span>`);
  });

  /* ---------- pipeline stages ---------- */
  const STAGES = [
    `<b>Execute</b> — the Cucumber suite (<code>TestRunner</code>) drives headless Chrome against SauceDemo: login, cart and checkout scenarios. Every run is real, in CI, on every push.`,
    `<b>Self-heal</b> — <code>SmartLocator</code> tries the primary locator, then an ordered fallback chain (id → data-test attribute → structural XPath). Every resolution is counted by <code>HealingReport</code> and printed at the end of the run. Try it above!`,
    `<b>Diagnose</b> — on failure the <code>@After</code> hook screenshots the page and asks the LLM for a plain-English root cause with a confidence level. Low confidence? The agent stands down instead of guessing.`,
    `<b>File bug</b> — at medium/high confidence <code>AzureDevOpsClient</code> files a Bug work item with the scenario, the AI analysis and the screenshot — after checking an open duplicate doesn't already exist.`,
  ];
  const sd = $("stagedetail");
  document.querySelectorAll(".stage").forEach((s) =>
    s.addEventListener("click", () => {
      document.querySelectorAll(".stage").forEach((x) => x.classList.remove("active"));
      s.classList.add("active");
      sd.innerHTML = STAGES[+s.dataset.s];
      sd.classList.add("show");
    })
  );

  /* ---------- RCA demo ---------- */
  const FAILS = [
    { name: "Stale checkout button",
      trace: "org.openqa.selenium.TimeoutException: Expected condition failed: waiting for url to contain \"checkout-step-one\"",
      cause: "The checkout button was clicked before the cart page finished rendering, so the click never registered.",
      fix: "Wait for clickability and confirm navigation after the click.",
      cat: "locator", pri: "P2" },
    { name: "Login error unreadable",
      trace: "java.lang.AssertionError: Expected error to contain \"locked out\" but was: \"\"",
      cause: "The test read the error container before the message text rendered — a timing race, not a product bug.",
      fix: "Wait for the error text to be non-empty before asserting.",
      cat: "timeout", pri: "P3" },
    { name: "Cart total mismatch",
      trace: "org.junit.ComparisonFailure: expected:<43.18> but was:<43.17>",
      cause: "Floating-point rounding in the test's expected total vs the displayed $43.18.",
      fix: "Compare with a tolerance (delta) instead of exact equality.",
      cat: "assertion", pri: "P2" },
  ];
  let cur = 0;
  const rc = $("rcaConsole");
  function rcaLog(html) { const d = document.createElement("div"); d.className = "step"; d.innerHTML = html; rc.appendChild(d); }
  function pickRca(i) {
    cur = i;
    document.querySelectorAll("#failpick button").forEach((b, j) => b.classList.toggle("sel", j === i));
    rc.innerHTML = "";
    $("ticket").classList.remove("show");
    rcaLog(`<span class="t-bad">✗ FAILED:</span> <span class="t-dim">${FAILS[i].name}</span>`);
    rcaLog(`<span class="t-dim">${FAILS[i].trace}</span>`);
    rcaLog(`<span class="t-dim">// press "Ask the agent" for the AI diagnosis.</span>`);
  }
  document.querySelectorAll("#failpick button").forEach((b, i) => b.addEventListener("click", () => pickRca(i)));
  $("rcaBtn").addEventListener("click", async (e) => {
    await boltPress(e.currentTarget, 800);
    const f = FAILS[cur];
    rcaLog(`<span class="t-cyan">🤖 analyzing screenshot + stack trace…</span>`);
    await sleep(900);
    const t = $("ticket");
    $("tId").textContent = "Bug #" + (4200 + cur * 7);
    $("tTitle").textContent = "[Auto-filed] " + f.name + " is failing";
    $("tPri").textContent = f.pri; $("tCat").textContent = f.cat;
    $("tBody").innerHTML = "";
    t.classList.add("show");
    const full = `<b>AI root cause:</b> ${f.cause}<br><b>Suggested fix:</b> ${f.fix}<br><span class="t-dim">Confidence: High — screenshot attached, no duplicate open.</span>`;
    const tmp = document.createElement("div"); tmp.innerHTML = full;
    const txt = tmp.textContent;
    let shown = "";
    $("tBody").innerHTML = `<span class="typing"></span>`;
    const ty = $("tBody").querySelector(".typing");
    for (const ch of txt) { shown += ch; ty.textContent = shown; await sleep(12); }
    $("tBody").innerHTML = full;
    rcaLog(`<span class="t-ok">✓ bug filed in Azure DevOps (duplicate check passed)</span>`);
    boltCelebrate();
  });
  pickRca(0);

  // greet: bolt wanders over to Run on load
  setTimeout(() => { log(`<span class="t-dim">// hi, I'm <b>Bolt</b> 🤖 — break the UI, then I'll run the test for you.</span>`); }, 600);
})();
