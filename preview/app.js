/**
 * CFHC Player Profile Live Preview - Interactive App Logic
 */

// Sample Database of College Football Players
const PLAYERS = {
  qb_vance: {
    number: "#12",
    name: "MARCUS VANCE",
    stars: 5,
    archetype: "FIELD GENERAL",
    devTrait: "★ STAR DEV",
    isCaptain: true,
    pos: "QB",
    class: "SR (RS)",
    htwt: "6'3\" / 218 LBS",
    town: "DALLAS, TX",
    ethic: "ELITE",
    team: "WOLVERINES",
    theme: "theme-navy",
    ovr: 94,
    xp: 3450,
    maxXp: 5000,
    skillPts: 3,
    sprite: "sprites/pixel_qb_sprite.jpg",
    stats: {
      yds: "3,420",
      tds: "32",
      ints: "6",
      compPct: 68.4,
      rating: "164.2",
      ypa: "8.6",
      qbr: "88.5"
    },
    attributes: [
      { name: "Throw Power", val: 96 },
      { name: "Throw Accuracy", val: 92 },
      { name: "Awareness", val: 95 },
      { name: "Play Recognition", val: 91 },
      { name: "Speed", val: 84 },
      { name: "Acceleration", val: 86 },
      { name: "Stamina", val: 90 },
      { name: "Toughness", val: 88 }
    ],
    archetypes: [
      { icon: "🎯", name: "Field General (Lvl 3)", desc: "+5 Throw Accuracy under pressure" },
      { icon: "⚡", name: "Pocket Passer (Lvl 2)", desc: "+4 Deep Ball Precision in clean pocket" },
      { icon: "🔥", name: "Clutch Performer", desc: "Boosts ratings in 4th quarter & rivalry games" }
    ],
    history: [
      { year: "2026", title: "Heisman Trophy Candidate", desc: "Ranked #2 in national passer rating through week 11" },
      { year: "2025", title: "All-Big Ten First Team", desc: "Threw for 3,890 yards and 36 TDs leading team to CFP" },
      { year: "2024", title: "Breakout Sophomore Season", desc: "Earned starter role in Week 3, threw 24 TDs" }
    ]
  },

  rb_reed: {
    number: "#24",
    name: "JAXSON REED",
    stars: 4,
    archetype: "POWER BACK",
    devTrait: "⚡ IMPACT DEV",
    isCaptain: false,
    pos: "RB",
    class: "JR",
    htwt: "5'11\" / 225 LBS",
    town: "BIRMINGHAM, AL",
    ethic: "HIGH",
    team: "CRIMSON TIDE",
    theme: "theme-crimson",
    ovr: 89,
    xp: 4100,
    maxXp: 5000,
    skillPts: 2,
    sprite: "sprites/pixel_rb_sprite.jpg",
    stats: {
      yds: "1,240",
      tds: "14",
      ints: "0",
      compPct: 5.4, // avg per carry
      rating: "112.8 YPG",
      ypa: "5.4 YPC",
      qbr: "14 TD"
    },
    attributes: [
      { name: "Break Tackle", val: 94 },
      { name: "Trucking", val: 95 },
      { name: "Speed", val: 88 },
      { name: "Acceleration", val: 91 },
      { name: "Stiff Arm", val: 92 },
      { name: "Elusiveness", val: 82 },
      { name: "Ball Carrier Vision", val: 89 },
      { name: "Stamina", val: 93 }
    ],
    archetypes: [
      { icon: "🚂", name: "Power Freight (Lvl 3)", desc: "+6 Trucking vs Defensive Backs" },
      { icon: "🛡️", name: "Bruiser", desc: "Reduces fumble chance when tackled by multiple defenders" }
    ],
    history: [
      { year: "2026", title: "Doak Walker Award Semifinalist", desc: "Leads conference in yards after contact (740 YAC)" },
      { year: "2025", title: "1,000-Yard Rusher", desc: "Averaged 5.2 YPC as sophomore primary starter" }
    ]
  },

  wr_smith: {
    number: "#6",
    name: "DEVONTE SMITH",
    stars: 5,
    archetype: "DEEP THREAT",
    devTrait: "🔥 SUPERSTAR",
    isCaptain: true,
    pos: "WR",
    class: "SO",
    htwt: "6'1\" / 185 LBS",
    town: "ATLANTA, GA",
    ethic: "ELITE",
    team: "BULLDOGS",
    theme: "theme-red-black",
    ovr: 92,
    xp: 2200,
    maxXp: 5000,
    skillPts: 4,
    sprite: "sprites/pixel_qb_sprite.jpg",
    stats: {
      yds: "1,180",
      tds: "12",
      ints: "0",
      compPct: 18.2, // avg per rec
      rating: "65 REC",
      ypa: "18.2 YPR",
      qbr: "9 YAC/REC"
    },
    attributes: [
      { name: "Speed", val: 97 },
      { name: "Acceleration", val: 96 },
      { name: "Deep Route Running", val: 91 },
      { name: "Catch in Traffic", val: 88 },
      { name: "Spectacular Catch", val: 94 },
      { name: "Agility", val: 93 },
      { name: "Release", val: 90 },
      { name: "Jumping", val: 92 }
    ],
    archetypes: [
      { icon: "🚀", name: "Deep Threat (Lvl 3)", desc: "+4 Speed on press coverage releases" },
      { icon: "✨", name: "Acrobat", desc: "Unlocks spectacular diving catch animations" }
    ],
    history: [
      { year: "2026", title: "Biletnikoff Watch List", desc: "Averaging 107.2 receiving YPG" },
      { year: "2025", title: "Freshman All-American", desc: "Set school freshman record with 9 receiving TDs" }
    ]
  },

  de_miller: {
    number: "#99",
    name: "CALEB MILLER",
    stars: 5,
    archetype: "EDGE RUSHER",
    devTrait: "★ STAR DEV",
    isCaptain: true,
    pos: "DE",
    class: "SR",
    htwt: "6'4\" / 265 LBS",
    town: "COLUMBUS, OH",
    ethic: "ELITE",
    team: "LONGHORNS",
    theme: "theme-burnt-orange",
    ovr: 95,
    xp: 4800,
    maxXp: 5000,
    skillPts: 5,
    sprite: "sprites/pixel_rb_sprite.jpg",
    stats: {
      yds: "14.5 SACKS",
      tds: "22 TFL",
      ints: "4 FF",
      compPct: 48, // tackles
      rating: "14.5 SKS",
      ypa: "22 TFL",
      qbr: "3 FR"
    },
    attributes: [
      { name: "Power Move", val: 96 },
      { name: "Finesse Move", val: 94 },
      { name: "Block Shedding", val: 92 },
      { name: "Play Recognition", val: 95 },
      { name: "Speed", val: 85 },
      { name: "Acceleration", val: 89 },
      { name: "Tackle", val: 91 },
      { name: "Hit Power", val: 93 }
    ],
    archetypes: [
      { icon: "⚡", name: "Speed Rusher (Lvl 3)", desc: "Faster pass rush moves on 3rd & long" },
      { icon: "💥", name: "Unstoppable Force", desc: "Increased block shed rate in 4th quarter" }
    ],
    history: [
      { year: "2026", title: "Bednarik Award Finalist", desc: "Leads FBS with 14.5 sacks" },
      { year: "2025", title: "First-Team All-American", desc: "Recorded 11.5 sacks and 18 TFL" }
    ]
  },

  cb_carter: {
    number: "#1",
    name: "TYREE CARTER",
    stars: 4,
    archetype: "LOCKDOWN CB",
    devTrait: "⚡ IMPACT DEV",
    isCaptain: false,
    pos: "CB",
    class: "FR",
    htwt: "6'0\" / 190 LBS",
    town: "MIAMI, FL",
    ethic: "HIGH",
    team: "DUCKS",
    theme: "theme-green-yellow",
    ovr: 83,
    xp: 1800,
    maxXp: 5000,
    skillPts: 1,
    sprite: "sprites/pixel_qb_sprite.jpg",
    stats: {
      yds: "4 INT",
      tds: "1 TD",
      ints: "11 PDU",
      compPct: 42.1, // allowed completion %
      rating: "32 TKL",
      ypa: "4 INT",
      qbr: "11 PBU"
    },
    attributes: [
      { name: "Man Coverage", val: 88 },
      { name: "Zone Coverage", val: 84 },
      { name: "Press", val: 86 },
      { name: "Speed", val: 94 },
      { name: "Acceleration", val: 93 },
      { name: "Agility", val: 92 },
      { name: "Play Recognition", val: 80 },
      { name: "Catching", val: 76 }
    ],
    archetypes: [
      { icon: "🔒", name: "Island Coverage (Lvl 2)", desc: "+3 Man Coverage when isolated wide" }
    ],
    history: [
      { year: "2026", title: "True Freshman Starter", desc: "Intercepted 2 passes in college debut vs ranked opponent" }
    ]
  }
};

let currentKey = "qb_vance";
let currentPlayer = PLAYERS.qb_vance;

document.addEventListener("DOMContentLoaded", () => {
  initTabs();
  initPlayerSelect();
  initSandboxControls();
  initKeyShortcuts();
  renderPlayerProfile();
});

// Initialize Nav Tabs
function initTabs() {
  const tabs = document.querySelectorAll(".nav-tab");
  tabs.forEach(tab => {
    tab.addEventListener("click", () => {
      tabs.forEach(t => t.classList.remove("active"));
      tab.classList.add("active");

      const targetPaneId = "tab-" + tab.dataset.tab;
      document.querySelectorAll(".tab-pane").forEach(pane => pane.classList.remove("active"));
      const targetPane = document.getElementById(targetPaneId);
      if (targetPane) targetPane.classList.add("active");
    });
  });
}

// Player Switcher
function initPlayerSelect() {
  const select = document.getElementById("player-select");
  select.addEventListener("change", (e) => {
    currentKey = e.target.value;
    currentPlayer = PLAYERS[currentKey];
    renderPlayerProfile();
  });

  const themeSelect = document.getElementById("team-theme");
  themeSelect.addEventListener("change", (e) => {
    document.body.className = e.target.value;
  });

  document.getElementById("btn-randomize").addEventListener("click", randomizePlayerStats);
}

// Sandbox Drawer Controls
function initSandboxControls() {
  const drawer = document.getElementById("sandbox-drawer");
  const toggleBtn = document.getElementById("btn-toggle-panel");
  const closeBtn = document.getElementById("btn-close-drawer");

  toggleBtn.addEventListener("click", () => drawer.classList.toggle("collapsed"));
  closeBtn.addEventListener("click", () => drawer.classList.add("collapsed"));

  // Sliders
  const inputOvr = document.getElementById("input-ovr");
  inputOvr.addEventListener("input", (e) => {
    currentPlayer.ovr = parseInt(e.target.value);
    document.getElementById("val-ovr").textContent = currentPlayer.ovr;
    updateGauge("ovr-circle-meter", currentPlayer.ovr, 99);
    document.getElementById("display-ovr").textContent = currentPlayer.ovr;
  });

  const inputStars = document.getElementById("input-stars");
  inputStars.addEventListener("input", (e) => {
    currentPlayer.stars = parseInt(e.target.value);
    document.getElementById("val-stars").textContent = currentPlayer.stars + " Stars";
    renderStarRating(currentPlayer.stars);
  });

  const inputStat1 = document.getElementById("input-stat1");
  inputStat1.addEventListener("input", (e) => {
    if (currentPlayer.attributes.length > 0) {
      currentPlayer.attributes[0].val = parseInt(e.target.value);
      document.getElementById("val-stat1").textContent = e.target.value;
      renderAttributes();
    }
  });

  const inputStat2 = document.getElementById("input-stat2");
  inputStat2.addEventListener("input", (e) => {
    if (currentPlayer.attributes.length > 4) {
      currentPlayer.attributes[4].val = parseInt(e.target.value);
      document.getElementById("val-stat2").textContent = e.target.value;
      renderAttributes();
    }
  });

  const inputXp = document.getElementById("input-xp");
  inputXp.addEventListener("input", (e) => {
    currentPlayer.xp = parseInt(e.target.value);
    document.getElementById("val-xp").textContent = currentPlayer.xp;
    const pct = Math.round((currentPlayer.xp / currentPlayer.maxXp) * 100);
    document.getElementById("xp-bar-fill").style.width = pct + "%";
    document.getElementById("xp-current-display").textContent = `${currentPlayer.xp.toLocaleString()} / ${currentPlayer.maxXp.toLocaleString()} XP`;
  });

  const inputPose = document.getElementById("input-pose");
  inputPose.addEventListener("change", (e) => {
    const val = e.target.value;
    const imgEl = document.getElementById("sprite-image");
    const canvasEl = document.getElementById("sprite-canvas");

    if (val === "qb") {
      imgEl.src = "sprites/pixel_qb_sprite.jpg";
      imgEl.classList.remove("hidden");
      canvasEl.classList.add("hidden");
      document.getElementById("pose-tag").textContent = "PIXEL ART SPRITE • QB STANCE";
    } else if (val === "rb") {
      imgEl.src = "sprites/pixel_rb_sprite.jpg";
      imgEl.classList.remove("hidden");
      canvasEl.classList.add("hidden");
      document.getElementById("pose-tag").textContent = "PIXEL ART SPRITE • RB SPRINT";
    } else {
      imgEl.classList.add("hidden");
      canvasEl.classList.remove("hidden");
      drawCanvasPixelSprite(canvasEl, currentPlayer.theme);
      document.getElementById("pose-tag").textContent = "PROCEDURAL CANVAS PIXEL ART";
    }
  });
}

// Render Profile Data to UI
function renderPlayerProfile() {
  document.body.className = currentPlayer.theme;
  document.getElementById("team-theme").value = currentPlayer.theme;

  document.getElementById("player-num").textContent = currentPlayer.number;
  document.getElementById("player-name").textContent = currentPlayer.name;
  document.getElementById("bc-player-name").textContent = currentPlayer.number + " " + currentPlayer.name;
  document.getElementById("player-archetype").textContent = currentPlayer.archetype;
  document.getElementById("player-devtrait").textContent = currentPlayer.devTrait;

  document.getElementById("bio-pos").textContent = currentPlayer.pos;
  document.getElementById("bio-class").textContent = currentPlayer.class;
  document.getElementById("bio-htwt").textContent = currentPlayer.htwt;
  document.getElementById("bio-town").textContent = currentPlayer.town;
  document.getElementById("bio-ethic").textContent = currentPlayer.ethic;

  document.getElementById("team-tag-display").textContent = currentPlayer.team;
  document.getElementById("display-ovr").textContent = currentPlayer.ovr;

  // Sliders sync
  document.getElementById("input-ovr").value = currentPlayer.ovr;
  document.getElementById("val-ovr").textContent = currentPlayer.ovr;
  document.getElementById("input-stars").value = currentPlayer.stars;
  document.getElementById("val-stars").textContent = currentPlayer.stars + " Stars";
  document.getElementById("input-xp").value = currentPlayer.xp;
  document.getElementById("val-xp").textContent = currentPlayer.xp;

  renderStarRating(currentPlayer.stars);
  updateGauge("ovr-circle-meter", currentPlayer.ovr, 99);

  // Stats
  document.getElementById("stat-yds").textContent = currentPlayer.stats.yds;
  document.getElementById("stat-tds").textContent = currentPlayer.stats.tds;
  document.getElementById("stat-ints").textContent = currentPlayer.stats.ints;
  document.getElementById("val-comp").textContent = currentPlayer.stats.compPct + "%";
  document.getElementById("val-rating").textContent = currentPlayer.stats.rating;
  document.getElementById("val-ypa").textContent = currentPlayer.stats.ypa;
  document.getElementById("val-qbr").textContent = currentPlayer.stats.qbr;
  updateGauge("comp-circle-meter", currentPlayer.stats.compPct, 100);

  // XP
  const xpPct = Math.round((currentPlayer.xp / currentPlayer.maxXp) * 100);
  document.getElementById("xp-bar-fill").style.width = xpPct + "%";
  document.getElementById("xp-current-display").textContent = `${currentPlayer.xp.toLocaleString()} / ${currentPlayer.maxXp.toLocaleString()} XP`;
  document.getElementById("skill-pts-badge").textContent = `${currentPlayer.skillPts} SKILL POINTS AVAILABLE`;

  // Sprite
  document.getElementById("sprite-image").src = currentPlayer.sprite;

  renderAttributes();
  renderArchetypes();
  renderFullRatings();
  renderCareerStats();
  renderHistory();
}

function renderStarRating(count) {
  const container = document.getElementById("star-rating-box");
  container.innerHTML = "";
  for (let i = 0; i < 5; i++) {
    const span = document.createElement("span");
    span.className = i < count ? "star fill" : "star empty";
    span.textContent = "★";
    container.appendChild(span);
  }
}

function renderAttributes() {
  const container = document.getElementById("attributes-list");
  container.innerHTML = "";

  currentPlayer.attributes.forEach(attr => {
    const row = document.createElement("div");
    row.className = "attr-row";

    let tierClass = "val-low";
    if (attr.val >= 90) tierClass = "val-elite";
    else if (attr.val >= 80) tierClass = "val-high";
    else if (attr.val >= 70) tierClass = "val-med";

    row.innerHTML = `
      <span class="attr-name">${attr.name}</span>
      <div class="attr-bar-bg">
        <div class="attr-bar-fill ${tierClass}" style="width: ${attr.val}%;"></div>
      </div>
      <span class="attr-val ${tierClass}">${attr.val}</span>
    `;
    container.appendChild(row);
  });
}

function renderArchetypes() {
  const container = document.getElementById("archetypes-container");
  container.innerHTML = "";

  currentPlayer.archetypes.forEach(arch => {
    const item = document.createElement("div");
    item.className = "archetype-item";
    item.innerHTML = `
      <span class="arch-icon">${arch.icon}</span>
      <div class="arch-details">
        <span class="arch-name">${arch.name}</span>
        <span class="arch-desc">${arch.desc}</span>
      </div>
    `;
    container.appendChild(item);
  });
}

function renderFullRatings() {
  const container = document.getElementById("full-ratings-container");
  if (!container) return;
  container.innerHTML = "";

  // Full set of 16 ratings
  const fullSet = [
    ...currentPlayer.attributes,
    { name: "Clutch Rating", val: currentPlayer.ovr >= 90 ? 94 : 82 },
    { name: "Injury Resistance", val: 88 },
    { name: "Leadership", val: 92 },
    { name: "Composure", val: 90 },
    { name: "Concentration", val: 87 },
    { name: "BC Vision", val: 85 },
    { name: "Juke Move", val: 78 },
    { name: "Spin Move", val: 80 }
  ];

  fullSet.forEach(attr => {
    const box = document.createElement("div");
    box.className = "stat-box";

    let color = "#ef4444";
    if (attr.val >= 90) color = "#fbbf24";
    else if (attr.val >= 80) color = "#10b981";
    else if (attr.val >= 70) color = "#3b82f6";

    box.innerHTML = `
      <span class="stat-val" style="color: ${color}">${attr.val}</span>
      <span class="stat-lbl">${attr.name.toUpperCase()}</span>
    `;
    container.appendChild(box);
  });
}

function renderCareerStats() {
  const tbody = document.getElementById("career-stats-rows");
  if (!tbody) return;
  tbody.innerHTML = `
    <tr>
      <td>2026</td>
      <td>${currentPlayer.team}</td>
      <td>11</td>
      <td>224</td>
      <td>328</td>
      <td>${currentPlayer.stats.compPct}%</td>
      <td>${currentPlayer.stats.yds}</td>
      <td>${currentPlayer.stats.tds}</td>
      <td>${currentPlayer.stats.ints}</td>
      <td>${currentPlayer.stats.rating}</td>
    </tr>
    <tr>
      <td>2025</td>
      <td>${currentPlayer.team}</td>
      <td>14</td>
      <td>268</td>
      <td>402</td>
      <td>66.7%</td>
      <td>3,890</td>
      <td>36</td>
      <td>9</td>
      <td>158.4</td>
    </tr>
    <tr>
      <td>2024</td>
      <td>${currentPlayer.team}</td>
      <td>9</td>
      <td>142</td>
      <td>218</td>
      <td>65.1%</td>
      <td>1,940</td>
      <td>24</td>
      <td>5</td>
      <td>151.2</td>
    </tr>
  `;
}

function renderHistory() {
  const container = document.getElementById("history-timeline-box");
  if (!container) return;
  container.innerHTML = "";

  currentPlayer.history.forEach(item => {
    const div = document.createElement("div");
    div.className = "timeline-item";
    div.innerHTML = `
      <div class="t-year">${item.year}</div>
      <div class="t-title">${item.title}</div>
      <div class="t-desc">${item.desc}</div>
    `;
    container.appendChild(div);
  });
}

function updateGauge(elementId, currentVal, maxVal) {
  const circle = document.getElementById(elementId);
  if (!circle) return;
  const radius = circle.r.baseVal.value;
  const circumference = 2 * Math.PI * radius;
  const pct = Math.min(currentVal / maxVal, 1.0);
  const offset = circumference - (pct * circumference);
  circle.style.strokeDasharray = `${circumference} ${circumference}`;
  circle.style.strokeDashoffset = offset;
}

function randomizePlayerStats() {
  const keys = Object.keys(PLAYERS);
  const randomKey = keys[Math.floor(Math.random() * keys.length)];
  currentKey = randomKey;
  currentPlayer = PLAYERS[randomKey];
  document.getElementById("player-select").value = currentKey;
  renderPlayerProfile();
}

function initKeyShortcuts() {
  document.addEventListener("keydown", (e) => {
    if (e.target.tagName === "INPUT" || e.target.tagName === "SELECT") return;

    if (e.key >= "1" && e.key <= "4") {
      const tabNames = ["overview", "ratings", "stats", "history"];
      const targetName = tabNames[parseInt(e.key) - 1];
      const targetBtn = document.querySelector(`.nav-tab[data-tab="${targetName}"]`);
      if (targetBtn) targetBtn.click();
    } else if (e.key === "r" || e.key === "R") {
      randomizePlayerStats();
    } else if (e.key === "t" || e.key === "T") {
      const themes = ["theme-navy", "theme-crimson", "theme-burnt-orange", "theme-green-yellow", "theme-red-black", "theme-purple-gold"];
      const idx = (themes.indexOf(currentPlayer.theme) + 1) % themes.length;
      currentPlayer.theme = themes[idx];
      document.body.className = currentPlayer.theme;
      document.getElementById("team-theme").value = currentPlayer.theme;
    } else if (e.key === "Escape") {
      document.getElementById("sandbox-drawer").classList.add("collapsed");
    }
  });
}

// Procedural Pixel Art Generator on HTML5 Canvas
function drawCanvasPixelSprite(canvas, themeClass) {
  const ctx = canvas.getContext("2d");
  ctx.imageSmoothingEnabled = false;
  ctx.clearRect(0, 0, canvas.width, canvas.height);

  // Pixel Grid Settings (16x24 block size scaled up)
  const cols = 16;
  const rows = 24;
  const pw = canvas.width / cols;
  const ph = canvas.height / rows;

  let primaryColor = "#1e3a8a";
  let accentColor = "#f59e0b";
  if (themeClass.includes("crimson")) { primaryColor = "#881337"; accentColor = "#f43f5e"; }
  if (themeClass.includes("burnt")) { primaryColor = "#7c2d12"; accentColor = "#ea580c"; }
  if (themeClass.includes("green")) { primaryColor = "#064e3b"; accentColor = "#84cc16"; }
  if (themeClass.includes("red")) { primaryColor = "#7f1d1d"; accentColor = "#dc2626"; }

  // Draw Football Player Helmet
  ctx.fillStyle = accentColor;
  ctx.fillRect(5 * pw, 2 * ph, 6 * pw, 4 * ph);
  ctx.fillStyle = "#111"; // visor
  ctx.fillRect(7 * pw, 4 * ph, 4 * pw, 1.5 * ph);

  // Jersey / Torso
  ctx.fillStyle = primaryColor;
  ctx.fillRect(4 * pw, 6 * ph, 8 * pw, 8 * ph);
  ctx.fillStyle = "#ffffff"; // jersey number 12
  ctx.fillRect(7 * pw, 8 * ph, 1 * pw, 4 * ph);
  ctx.fillRect(9 * pw, 8 * ph, 1 * pw, 4 * ph);

  // Arms holding football
  ctx.fillStyle = "#d97706"; // skin tone
  ctx.fillRect(2 * pw, 7 * ph, 2 * pw, 6 * ph);
  ctx.fillRect(12 * pw, 7 * ph, 2 * pw, 6 * ph);
  ctx.fillStyle = "#78350f"; // football
  ctx.fillRect(10 * pw, 9 * ph, 3 * pw, 2 * ph);

  // Pants & Legs
  ctx.fillStyle = "#ffffff";
  ctx.fillRect(5 * pw, 14 * ph, 3 * pw, 7 * ph);
  ctx.fillRect(8 * pw, 14 * ph, 3 * pw, 7 * ph);

  // Cleats
  ctx.fillStyle = "#000000";
  ctx.fillRect(4 * pw, 21 * ph, 4 * pw, 2 * ph);
  ctx.fillRect(8 * pw, 21 * ph, 4 * pw, 2 * ph);
}
