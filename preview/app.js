/**
 * College Football Head Coach — Dynasty Dashboard Overhaul JavaScript
 * Handles live sidebar navigation, news slider, live tweaker controls, team theme switching,
 * keyboard shortcuts, and audio soundtrack ticker.
 */

document.addEventListener('DOMContentLoaded', () => {

  // DOM Elements
  const themeSelect = document.getElementById('select-team-theme');
  const weekSelect = document.getElementById('select-week');
  const btnToggleTweaker = document.getElementById('btn-toggle-tweaker');
  const btnCloseDrawer = document.getElementById('btn-close-drawer');
  const sandboxDrawer = document.getElementById('sandbox-drawer');
  const btnSimWeek = document.getElementById('btn-sim-week');
  
  // Customizer Inputs
  const inputTeamOvr = document.getElementById('input-team-ovr');
  const valTeamOvr = document.getElementById('val-team-ovr');
  const displayTeamOvr = document.getElementById('display-team-ovr');

  const inputTeamOff = document.getElementById('input-team-off');
  const valTeamOff = document.getElementById('val-team-off');
  const displayTeamOff = document.getElementById('display-team-off');

  const inputTeamDef = document.getElementById('input-team-def');
  const valTeamDef = document.getElementById('val-team-def');
  const displayTeamDef = document.getElementById('display-team-def');

  const displayWeekText = document.getElementById('display-week-text');

  // Team Theme Selector
  if (themeSelect) {
    themeSelect.addEventListener('change', (e) => {
      document.body.className = `theme-default theme-${e.target.value}`;
    });
  }

  // Sidebar navigation selection
  const navItems = document.querySelectorAll('.nav-item');
  navItems.forEach((item, index) => {
    item.addEventListener('click', () => {
      navItems.forEach(i => i.classList.remove('active'));
      item.classList.add('active');
    });
  });

  // Drawer Toggle
  if (btnToggleTweaker && sandboxDrawer) {
    btnToggleTweaker.addEventListener('click', () => {
      sandboxDrawer.classList.toggle('collapsed');
    });
  }

  if (btnCloseDrawer && sandboxDrawer) {
    btnCloseDrawer.addEventListener('click', () => {
      sandboxDrawer.classList.add('collapsed');
    });
  }

  // Live Rating Customizer Listeners
  if (inputTeamOvr) {
    inputTeamOvr.addEventListener('input', (e) => {
      const val = e.target.value;
      if (valTeamOvr) valTeamOvr.textContent = val;
      if (displayTeamOvr) displayTeamOvr.textContent = val;
    });
  }

  if (inputTeamOff) {
    inputTeamOff.addEventListener('input', (e) => {
      const val = e.target.value;
      if (valTeamOff) valTeamOff.textContent = val;
      if (displayTeamOff) displayTeamOff.textContent = val;
    });
  }

  if (inputTeamDef) {
    inputTeamDef.addEventListener('input', (e) => {
      const val = e.target.value;
      if (valTeamDef) valTeamDef.textContent = val;
      if (displayTeamDef) displayTeamDef.textContent = val;
    });
  }

  // Week Selector
  if (weekSelect) {
    weekSelect.addEventListener('change', (e) => {
      const weekNum = e.target.value;
      if (displayWeekText) displayWeekText.textContent = `WEEK ${weekNum}`;
    });
  }

  // Sim Week Button
  if (btnSimWeek) {
    btnSimWeek.addEventListener('click', () => {
      let currentWeek = parseInt(weekSelect.value) || 8;
      currentWeek = currentWeek >= 15 ? 1 : currentWeek + 1;
      weekSelect.value = currentWeek.toString();
      if (displayWeekText) displayWeekText.textContent = `WEEK ${currentWeek}`;

      // Toast notification
      const toast = document.createElement('div');
      toast.className = 'sim-toast';
      toast.style.cssText = `
        position: fixed;
        bottom: 60px;
        right: 20px;
        background: #00e676;
        color: #000;
        font-weight: 900;
        padding: 10px 18px;
        border-radius: 8px;
        box-shadow: 0 4px 16px rgba(0,230,118,0.4);
        z-index: 999;
        font-family: var(--font-display);
      `;
      toast.textContent = `Advanced to Week ${currentWeek}! Simulation Complete.`;
      document.body.appendChild(toast);

      setTimeout(() => {
        toast.remove();
      }, 2500);
    });
  }

  // News Headlines Carousel
  const newsItems = [
    {
      title: "OWLS CLIMB TO #24 IN LATEST POLL",
      text: "Back-to-back road wins have the Owls ranked #24 nationally. Coach Carter credits 'buy-in and belief.'"
    },
    {
      title: "MASON HARRISON NAMED OPOW",
      text: "Junior QB threw for 287 yards and 3 TDs in Saturday's victory over Stonebridge."
    },
    {
      title: "RECRUITING PIPELINE SURGES TO 14 COMMITS",
      text: "Pine Valley State lands 4-star West Coast pass rusher, boosting class ranking to #18."
    }
  ];

  let currentNewsIdx = 0;
  const newsTitleEl = document.getElementById('news-title');
  const newsTextEl = document.getElementById('news-text');
  const newsDots = document.querySelectorAll('.news-dots-pagination .dot');

  setInterval(() => {
    currentNewsIdx = (currentNewsIdx + 1) % newsItems.length;
    if (newsTitleEl) newsTitleEl.textContent = newsItems[currentNewsIdx].title;
    if (newsTextEl) newsTextEl.textContent = newsItems[currentNewsIdx].text;
    newsDots.forEach((dot, idx) => {
      dot.classList.toggle('active', idx === currentNewsIdx);
    });
  }, 4000);

  // Keyboard navigation shortcuts
  document.addEventListener('keydown', (e) => {
    if (e.target.tagName === 'INPUT' || e.target.tagName === 'SELECT') return;

    // Space / Enter -> Advance week (Console [A] SELECT)
    if (e.code === 'Space' || e.key === 'Enter') {
      if (btnSimWeek) btnSimWeek.click();
      e.preventDefault();
    }

    // Escape -> Collapse customizer drawer / Return to Dashboard (Console [B] BACK)
    if (e.key === 'Escape') {
      if (sandboxDrawer) sandboxDrawer.classList.add('collapsed');
      navItems[0].click();
      e.preventDefault();
    }

    // F1 / ? -> Help shortcuts info (Console [Y] HELP)
    if (e.key === 'F1' || e.key === '?') {
      alert("CFHC KEYBOARD CONTROLS:\n\n• [SPACE / ENTER]: Advance Week / Select (Button A)\n• [ESC]: Back / Collapse Drawer (Button B)\n• [F1 / ?]: Help (Button Y)\n• [1 - 9]: Jump to Sidebar Tabs 1-9\n• [UP / DOWN]: Navigate Sidebar Options");
      e.preventDefault();
    }

    // Arrow keys down / up to select sidebar items
    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
      const activeIdx = Array.from(navItems).findIndex(item => item.classList.contains('active'));
      let nextIdx = e.key === 'ArrowDown' ? activeIdx + 1 : activeIdx - 1;
      if (nextIdx < 0) nextIdx = navItems.length - 1;
      if (nextIdx >= navItems.length) nextIdx = 0;
      navItems[nextIdx].click();
      e.preventDefault();
    }

    // Digit keys 1-9 jump to sidebar tabs
    if (e.key >= '1' && e.key <= '9') {
      const idx = parseInt(e.key) - 1;
      if (idx < navItems.length) {
        navItems[idx].click();
        e.preventDefault();
      }
    }
  });

});
