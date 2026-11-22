(() => {
  'use strict';

  const STORAGE_KEY = 'lazy_rehab_web_state_v1';

  // -------------------- Utility Helpers --------------------
  function todayDateKey() {
    const d = new Date();
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  function escapeHtml(str) {
    return String(str ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function pickNonRepeating(list, current) {
    if (!list || list.length === 0) return '';
    const filtered = list.filter((x) => x !== current);
    const pool = filtered.length > 0 ? filtered : list;
    return pool[Math.floor(Math.random() * pool.length)];
  }

  function uid() {
    if (window.crypto && typeof window.crypto.randomUUID === 'function') {
      return window.crypto.randomUUID();
    }
    return 't_' + Date.now().toString(36) + '_' + Math.random().toString(36).slice(2, 8);
  }

  // -------------------- Inline SVG Icons --------------------
  const ICONS = {
    spa: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C9.5 6 7 8.5 7 12a5 5 0 0 0 10 0c0-3.5-2.5-6-5-10zm0 16.5A6.5 6.5 0 0 1 5.5 12c0-1.1.3-2.2.7-3.2C3.8 10.1 2 12.6 2 15.5 2 19.1 5.1 22 9 22h6c3.9 0 7-2.9 7-6.5 0-2.9-1.8-5.4-4.2-6.7.4 1 .7 2.1.7 3.2A6.5 6.5 0 0 1 12 18.5z"/></svg>',
    hospital: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-2 11h-3v3h-4v-3H7v-4h3V7h4v3h3v4z"/></svg>',
    pharmacy: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M21 5h-2.64l1.14-3.14L17.15 1l-1.46 4H3v2l2 6-2 6v2h18v-2l-2-6 2-6V5zm-5 9h-3v3h-2v-3H8v-2h3V9h2v3h3v2z"/></svg>',
    memories: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm16-4H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-5 10.5v-5l4 2.5-4 2.5zm-4-5L7.5 10 11 12.5v-5z"/></svg>',
    stories: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M21 5c-1.11-.35-2.33-.5-3.5-.5-1.95 0-4.05.4-5.5 1.5-1.45-1.1-3.55-1.5-5.5-1.5S2.45 4.9 1 6v14.65c0 .25.25.5.5.5.1 0 .15-.05.25-.05C3.1 20.45 5.05 20 6.5 20c1.95 0 4.05.4 5.5 1.5 1.35-.85 3.8-1.5 5.5-1.5 1.65 0 3.35.3 4.75 1.05.1.05.15.05.25.05.25 0 .5-.25.5-.5V6c-.6-.45-1.25-.75-2-1zm0 13.5c-1.1-.35-2.3-.5-3.5-.5-1.7 0-4.15.65-5.5 1.5V8c1.35-.85 3.8-1.5 5.5-1.5 1.2 0 2.4.15 3.5.5v11.5z"/></svg>',
    account: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 4c1.93 0 3.5 1.57 3.5 3.5S13.93 13 12 13s-3.5-1.57-3.5-3.5S10.07 6 12 6zm0 14c-2.03 0-4.43-.82-6.14-2.88a9.95 9.95 0 0 1 12.28 0C16.43 19.18 14.03 20 12 20z"/></svg>',
    add: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>',
    check: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M9 16.17 4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>',
    checkCircle: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>',
    moreVert: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"/></svg>',
    edit: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zm17.71-10.21a1.003 1.003 0 0 0 0-1.42l-2.34-2.34a1.003 1.003 0 0 0-1.42 0l-1.83 1.83 3.75 3.75 1.84-1.82z"/></svg>',
    wand: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M7.5 5.6 10 7 8.6 4.5 10 2 7.5 3.4 5 2l1.4 2.5L5 7zm12 9.8L17 14l1.4 2.5L17 19l2.5-1.4L22 19l-1.4-2.5L22 14zM22 2l-2.5 1.4L17 2l1.4 2.5L17 7l2.5-1.4L22 7l-1.4-2.5zm-7.63 5.29a.996.996 0 0 0-1.41 0L1.29 18.96a.996.996 0 0 0 0 1.41l2.34 2.34c.39.39 1.02.39 1.41 0L16.7 11.05a.996.996 0 0 0 0-1.41l-2.33-2.35zm-1.03 5.49-2.12-2.12 2.44-2.44 2.12 2.12-2.44 2.44z"/></svg>',
    trash: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM8 9h8v10H8V9zm7.5-5-1-1h-5l-1 1H5v2h14V4z"/></svg>',
    touch: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M9 11.24V7.5a2.5 2.5 0 0 1 5 0v3.74c1.21-.81 2-2.18 2-3.74C16 5.01 13.99 3 11.5 3S7 5.01 7 7.5c0 1.56.79 2.93 2 3.74zm9.84 4.63-4.54-2.26c-.17-.07-.35-.11-.54-.11H13v-6c0-.83-.67-1.5-1.5-1.5S10 6.67 10 7.5v10.74l-3.43-.72c-.08-.01-.15-.03-.24-.03-.31 0-.59.13-.79.33l-.79.8 4.94 4.94c.27.27.65.44 1.06.44h6.79c.75 0 1.33-.55 1.44-1.28l.75-5.27c.01-.07.02-.14.02-.2 0-.62-.38-1.16-.91-1.38z"/></svg>',
    checkroom: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M21.6 18.2 13 11.75v-.91a3.496 3.496 0 0 0 2.4-4.27C14.99 5.02 13.5 4 11.88 4 9.86 4 8.5 5.45 8.5 7.25h2c0-.83.67-1.5 1.5-1.5s1.5.67 1.5 1.5c0 .84-.69 1.52-1.53 1.5-.54-.01-.97.45-.97.99v2.01l-8.6 6.45c-.9.68-.42 2.1.7 2.1h17.8c1.12 0 1.6-1.42.7-2.1z"/></svg>',
    medical: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M20 6h-4V4c0-1.1-.9-2-2-2h-4c-1.1 0-2 .9-2 2v2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zM10 4h4v2h-4V4zm6 11h-3v3h-2v-3H8v-2h3v-3h2v3h3v2z"/></svg>',
    bed: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M21 10.78V8c0-1.65-1.35-3-3-3h-4c-.77 0-1.47.3-2 .78-.53-.48-1.23-.78-2-.78H6C4.35 5 3 6.35 3 8v2.78c-.61.55-1 1.34-1 2.22v6h2v-2h16v2h2v-6c0-.88-.39-1.67-1-2.22zM14 7h4c.55 0 1 .45 1 1v2h-6V8c0-.55.45-1 1-1zM5 8c0-.55.45-1 1-1h4c.55 0 1 .45 1 1v2H5V8zm-1 7v-2c0-.55.45-1 1-1h14c.55 0 1 .45 1 1v2H4z"/></svg>',
    bedtime: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12.34 2.02C6.59 1.82 2 6.42 2 12c0 5.52 4.48 10 10 10 3.71 0 6.93-2.02 8.66-5.02-7.51-.25-12.09-8.43-8.32-14.96z"/></svg>',
    cafe: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M20 3H4v10c0 2.21 1.79 4 4 4h6c2.21 0 4-1.79 4-4v-3h2a2 2 0 0 0 2-2V5c0-1.11-.89-2-2-2zm0 5h-2V5h2v3zM4 19h16v2H4z"/></svg>',
    heart: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 21.35-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>',
    sparkle: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="m19 9 1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9zm-7.5.5L9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12l-5.5-2.5zM19 15l-1.25 2.75L15 19l2.75 1.25L19 23l1.25-2.75L23 19l-2.75-1.25L19 15z"/></svg>',
    casino: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM7.5 18c-.83 0-1.5-.67-1.5-1.5S6.67 15 7.5 15s1.5.67 1.5 1.5S8.33 18 7.5 18zm0-9C6.67 9 6 8.33 6 7.5S6.67 6 7.5 6 9 6.67 9 7.5 8.33 9 7.5 9zm4.5 4.5c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zm4.5 4.5c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zm0-9c-.83 0-1.5-.67-1.5-1.5S15.67 6 16.5 6s1.5.67 1.5 1.5S17.33 9 16.5 9z"/></svg>',
    addTask: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M22 5.18 10.59 16.6l-4.24-4.24 1.41-1.41 2.83 2.83 10-10L22 5.18zm-2.21 5.04c.13.57.21 1.17.21 1.78 0 4.42-3.58 8-8 8s-8-3.58-8-8 3.58-8 8-8c1.58 0 3.04.46 4.28 1.25l1.44-1.44A9.92 9.92 0 0 0 12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10c0-1.19-.22-2.33-.6-3.39l-1.61 1.61z"/></svg>',
    batteryAlert: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M15.67 4H14V2h-4v2H8.33C7.6 4 7 4.6 7 5.33v15.33C7 21.4 7.6 22 8.33 22h7.33c.74 0 1.34-.6 1.34-1.33V5.33C17 4.6 16.4 4 15.67 4zM13 18h-2v-2h2v2zm0-4h-2V9h2v5z"/></svg>',
    batteryLow: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M15.67 4H14V2h-4v2H8.33C7.6 4 7 4.6 7 5.33v15.33C7 21.4 7.6 22 8.33 22h7.33c.74 0 1.34-.6 1.34-1.33V5.33C17 4.6 16.4 4 15.67 4zM15 16H9V6h6v10z"/></svg>',
    batterySteady: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M15.67 4H14V2h-4v2H8.33C7.6 4 7 4.6 7 5.33v15.33C7 21.4 7.6 22 8.33 22h7.33c.74 0 1.34-.6 1.34-1.33V5.33C17 4.6 16.4 4 15.67 4zM15 11H9V6h6v5z"/></svg>',
    batteryFull: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M15.67 4H14V2h-4v2H8.33C7.6 4 7 4.6 7 5.33v15.33C7 21.4 7.6 22 8.33 22h7.33c.74 0 1.34-.6 1.34-1.33V5.33C17 4.6 16.4 4 15.67 4z"/></svg>',
    trophy: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 5h-2V3H7v2H5c-1.1 0-2 .9-2 2v1c0 2.55 1.92 4.63 4.39 4.94A5.01 5.01 0 0 0 11 15.9V19H7v2h10v-2h-4v-3.1a5.01 5.01 0 0 0 3.61-2.96C19.08 12.63 21 10.55 21 8V7c0-1.1-.9-2-2-2zM5 8V7h2v3.82C5.84 10.4 5 9.3 5 8zm14 0c0 1.3-.84 2.4-2 2.82V7h2v1z"/></svg>',
    star: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 17.27 18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/></svg>',
    lockOpen: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 17c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm6-9h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6h1.9c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm0 12H6V10h12v10z"/></svg>',
    camera: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M9 2 7.17 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2h-3.17L15 2H9zm3 15c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5z"/></svg>',
    agenda: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M20 13H4c-.55 0-1 .45-1 1v6c0 .55.45 1 1 1h16c.55 0 1-.45 1-1v-6c0-.55-.45-1-1-1zM20 3H4c-.55 0-1 .45-1 1v6c0 .55.45 1 1 1h16c.55 0 1-.45 1-1V4c0-.55-.45-1-1-1z"/></svg>',
    calendar: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 4h-1V2h-2v2H8V2H6v2H5c-1.11 0-1.99.9-1.99 2L3 20a2 2 0 0 0 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 16H5V9h14v11z"/></svg>',
    pinEdit: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M13.95 13H9V8.05l5.61-5.61A8.17 8.17 0 0 0 12 2c-4.2 0-8 3.22-8 8.2 0 3.32 2.67 7.25 8 11.8 5.33-4.55 8-8.48 8-11.8 0-1.01-.16-1.94-.45-2.8l-5.6 5.6zm-2.95-2h2.12l5.8-5.8-2.12-2.12-5.8 5.8V11zm9.3-6.64-1.41-1.41c-.2-.2-.51-.2-.71 0l-.88.88 2.12 2.12.88-.88c.2-.2.2-.51 0-.71z"/></svg>',
    close: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 6.41 17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>',
    chevLeft: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M15.41 7.41 14 6l-6 6 6 6 1.41-1.41L10.83 12z"/></svg>',
    chevRight: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M10 6 8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/></svg>',
    download: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/></svg>',
    upload: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M9 16h6v-6h4l-7-7-7 7h4zm-4 2h14v2H5z"/></svg>',
    reset: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M17.65 6.35A7.958 7.958 0 0 0 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08A5.99 5.99 0 0 1 12 18c-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/></svg>'
  };

  // -------------------- Domain Catalogs (Exact Port of RehabModels.kt) --------------------
  const TABS = [
    { id: 'room', navLabel: '康復室', topTitle: '懶癌康復室', icon: ICONS.spa },
    { id: 'prescriptions', navLabel: '療程', topTitle: '復健療程', icon: ICONS.hospital },
    { id: 'pharmacy', navLabel: '藥局', topTitle: '院內藥局', icon: ICONS.pharmacy },
    { id: 'memories', navLabel: '回憶', topTitle: '康復回憶冊', icon: ICONS.memories },
    { id: 'log', navLabel: '日記', topTitle: '日記', icon: ICONS.stories }
  ];

  const EFFORT_LEVELS = {
    micro: { id: 'micro', labelZh: '超微事', badgeHint: '約 1 分鐘' },
    gentle: { id: 'gentle', labelZh: '小康復', badgeHint: '約 5 分鐘' },
    boss: { id: 'boss', labelZh: '大魔王', badgeHint: '建議切小塊' }
  };

  const ENERGY_LEVELS = {
    flat: {
      id: 'flat',
      shortTag: '0% 躺平',
      descriptionZh: '棉被與沙發引力最強的時候，喝口水就算重大勝利',
      icon: ICONS.batteryAlert
    },
    low: {
      id: 'low',
      shortTag: '20% 微動',
      descriptionZh: '只能動動手指或伸懶腰 10 秒的輕量康復',
      icon: ICONS.batteryLow
    },
    steady: {
      id: 'steady',
      shortTag: '50% 起身',
      descriptionZh: '難得有一點點動力，趁機做一件暖呼呼的小事',
      icon: ICONS.batterySteady
    }
  };

  const DOCTOR_POSES = {
    OBSERVE: { id: 'OBSERVE', img: './assets/img_mascot_main.png', labelZh: '冷面值班' },
    PHARMACY: { id: 'PHARMACY', img: './assets/img_doctor_pharmacy.png', labelZh: '藥局發藥' },
    SIPPING: { id: 'SIPPING', img: './assets/img_doctor_sipping.png', labelZh: '偷喝珍奶' },
    APPROVE: { id: 'APPROVE', img: './assets/img_doctor_approve.png', labelZh: '面無表情給讚' }
  };
  const DOCTOR_POSE_ORDER = ['OBSERVE', 'PHARMACY', 'SIPPING', 'APPROVE'];

  const WARD_SPRITES = {
    IDLE_A: './assets/img_doc_idle_1.png',
    IDLE_B: './assets/img_doc_idle_2.png',
    WALK_A: './assets/img_doc_walk_1.png',
    WALK_B: './assets/img_doc_walk_2.png',
    TURN: './assets/img_doc_turn.png',
    INTERACT: './assets/img_doc_interact.png',
    POSE_SIP: './assets/img_doctor_sipping.png',
    POSE_APPROVE: './assets/img_doctor_approve.png'
  };

  const COAT_STYLES = [
    {
      id: 'classic_white',
      titleZh: '標準值班白袍',
      badgeZh: '預設款',
      descriptionZh: '乾淨俐落的基礎白袍，口袋裡只裝著無奈與下班願望。'
    },
    {
      id: 'star_director',
      titleZh: '星輝摸魚白袍',
      badgeZh: '金色徽章',
      descriptionZh: '領口別上金色摸魚星星章，口袋插著開不出嚴厲醫囑的星星筆。'
    },
    {
      id: 'cat_paw_coat',
      titleZh: '貓掌口袋白袍',
      badgeZh: '肉球縫線',
      descriptionZh: '口袋繡著雙倍貓掌肉墊＋領口粉紅小啾啾，專治心情低落患者。'
    },
    {
      id: 'pastel_mint_coat',
      titleZh: '薄荷休診長袍',
      badgeZh: '休診滾邊',
      descriptionZh: '縫上本院療癒薄荷綠滾邊與『休診發呆中』名牌的特製長袍。'
    }
  ];

  const STETHOSCOPE_STYLES = [
    {
      id: 'default_heart',
      titleZh: '粉心基礎聽診器',
      badgeZh: '預設款',
      descriptionZh: '能聽見心跳與肚子餓聲音的普通粉心聽診器。'
    },
    {
      id: 'gold_vip',
      titleZh: '黃金院長聽診器',
      badgeZh: '院長級',
      descriptionZh: '香檳金導管搭配閃亮星芒聽診頭，專門鑑定患者是否合格耍廢。'
    },
    {
      id: 'cat_bell',
      titleZh: '貓耳鈴鐺聽診器',
      badgeZh: '療癒系',
      descriptionZh: '淺藍耳管配上會輕輕搖晃的金色小鈴鐺與粉嫩貓耳聽診頭。'
    },
    {
      id: 'boba_scope',
      titleZh: '珍奶續命聽診器',
      badgeZh: '全糖微冰',
      descriptionZh: '焦糖奶茶色導管加上珍珠奶茶造型聽診頭，隨時偵測今日含糖量。'
    }
  ];

  const STARTER_TASKS = [
    {
      title: '喝一口水',
      effortLevel: 'micro',
      note: '今天願意打開康復室已經很棒了，先喝口水吧！'
    },
    {
      title: '伸懶腰晃動肩膀 10 秒',
      effortLevel: 'micro',
      note: '懶得站起來的話，坐著動一動也完全算數喔'
    },
    {
      title: '打開一件拖延很久的事，只看 2 分鐘',
      effortLevel: 'gentle',
      note: '覺得太重的話，點右邊「⋮」切小塊交給康復室'
    }
  ];

  const FEIFEI_QUOTES = [
    '根據本院精密儀器檢測，你目前的生命跡象非常微弱，建議立刻執行『呼吸』以維持基本運作。',
    '病情分析：你的懶癌細胞今天非常活躍，建議採取『順其自然』的保守療法。',
    '別自責，從醫學角度來看，在床上翻身也算是一種全身性的核心肌群復健。',
    '這不是拖延症，這是大腦為了防止過熱而啟動的自動休眠保護機制，屬於正常現象。',
    '臨床顯示，對著天花板發呆超過十分鐘，有助於降低對人類社會的焦慮感。',
    '今日處方箋已開立：喝一口水。警告，過量飲水可能導致必須起身去廁所的副作用。',
    '能坐著就不要站著，能躺著就不要坐著——這是本院最高指導原則，請務必遵守。',
    '看你今天精神不錯，竟然能把手機拿起來點我，這在醫學史上已經算是奇蹟了。',
    '如果覺得今天的任務太難，本院允許你把『活著』當成今日唯一指標。',
    '建議每小時眨眼數百次，確保眼角膜濕潤，這也是你今天運動量最大的地方了。',
    '病歷紀錄：患者今日成功對抗了地心引力並離開床鋪，雖然只有三分鐘，但值得寫進醫學期刊。',
    '雖然你的康復進度條幾乎沒動，但至少沒有往後倒退，本醫師對此表示高度肯定。',
    '別看其他病房的人那麼拼命，他們的肝指數可沒有你的漂亮。',
    '你這不是懶，你只是在為下一次的重大行動儲存能量，預計儲存時間為……無限期。',
    '今天有完成一項處方箋嗎？有的話，你已經打敗了昨天那個連這行字都懶得看的自己。',
    '照你這個康復速度，預計再住院三百年就能順利出院了，加油，我看好你。',
    '本院提供終身免費床位，不用急著康復，慢慢來比較不會閃到腰。',
    '情緒穩定是抗癌關鍵。遇到煩心事時，請默念三遍：『關我什麼事』，療效顯著。',
    '今天的復健指標已經達標了，剩下的時間請安心當一塊沒有夢想的馬鈴薯。',
    '作為你的主治醫師，我正式宣布：你今天的表現非常優秀，准許你繼續耍廢。'
  ];

  const SHRINK_SUGGESTIONS = [
    '只打開檔案或頁面，看 30 秒就好',
    '先完成這件事最簡單的 1% 開頭',
    '只做 2 分鐘計時，時間到就可以停手',
    '把這件事拆成「先拿出工具就好」'
  ];

  const PHARMACY_QUOTES = [
    '歡迎來到院內藥局。本院所有道具免費供應，看順眼就直接領回病房擺著吧。',
    '不用集點也不用解鎖，只要你開心，全院十件鎮院之寶現在就能全包。',
    '這個珍珠奶茶點滴架是本院暢銷第一名，醫師本人值班時也偷裝過兩袋。',
    '請放心領取，本藥局絕不開立會讓你流汗或早起的產品。',
    '拿到『居然活著』錦旗的時候，記得掛在病房牆上，那是對生命最高的敬意。',
    '溫馨提醒：精神勝利法膠囊配白開水或珍珠奶茶吞服皆可，全憑今日心情。'
  ];

  const PRESCRIPTION_QUOTES = [
    '挑一個今天不會讓你想逃跑的療程吧，越簡單越好。',
    '選 0% 躺平族完全不丟臉，本醫師自己值班時也常選這區。',
    '如果光看清單就覺得累，按下『隨緣抽一張』交給命運決定就好。',
    '不用一次排滿十項，今天只要選一件能勉強動一根手指的就夠了。',
    '注意：本院開立的復健療程皆不具任何強迫性，想放著明天做也行。'
  ];

  const MEMORY_QUOTES = [
    '翻翻這些病歷，你其實比自己以為的還要頑強地活到了現在。',
    '哪怕那天只喝了一口水、在床上翻個身，在本院紀錄上都算重大復健實績。',
    '每一張病房快照，都是你成功繼續賴在這家醫院不肯出院的鐵證。',
    '康復進度條慢到幾乎看不見也沒關係，至少生命跡象欄位每天都有寫上日期。',
    '本醫師審查完你的過往病歷了：評定為『合格馬鈴薯』，准許繼續按這節奏躺平。'
  ];

  const EQUIP_REACTIONS = [
    '這套配件摸魚手感不錯，就這樣繼續冷面值班吧。',
    '換好了。放心，用新聽診器也聽不到什麼上進心，只聽得到你想睡覺。',
    '裝備更新完畢，本醫師的專業冷漠氣場又提升了 3%。',
    '這身行頭去藥局偷喝全糖珍奶應該也不容易被拆穿。',
    '好，今天的診療穿搭就按這張處方執行。'
  ];

  const PRESCRIPTIONS = [
    { id: 'rx_water', title: '喝三口水', effortLevel: 'micro', energyLevel: 'flat', doctorNote: '不用一整杯，三口潤潤喉嚨就是康復大成功。' },
    { id: 'rx_breathe', title: '閉上眼睛深呼吸 3 次', effortLevel: 'micro', energyLevel: 'flat', doctorNote: '吸氣數四秒、吐氣數六秒，讓肩膀慢慢放下來。' },
    { id: 'rx_blink', title: '遠眺窗外或閉眼放空 20 秒', effortLevel: 'micro', energyLevel: 'flat', doctorNote: '螢幕看太久了，跟我們一起眨眨眼休息一下。' },
    { id: 'rx_roll', title: '在床上或沙發轉動腳踝 10 下', effortLevel: 'micro', energyLevel: 'flat', doctorNote: '不用爬起來！躺著也能做的無痛康復操。' },
    { id: 'rx_trash', title: '順手丟掉桌上的 1 個小垃圾', effortLevel: 'micro', energyLevel: 'low', doctorNote: '不用整理整個房間，只要丟掉一張衛生紙或空瓶就好。' },
    { id: 'rx_curtain', title: '站起來拉開窗簾讓暖陽進來', effortLevel: 'micro', energyLevel: 'low', doctorNote: '曬一點自然光，告訴身體現在是溫柔的一天。' },
    { id: 'rx_stretch', title: '雙手舉高伸懶腰 10 秒鐘', effortLevel: 'micro', energyLevel: 'low', doctorNote: '把卡住的脊椎拉開一點點，打個大哈欠更棒。' },
    { id: 'rx_face', title: '用溫水洗把臉或擦擦臉', effortLevel: 'gentle', energyLevel: 'low', doctorNote: '沒心力全套保養也沒關係，清水拍兩下就很清爽。' },
    { id: 'rx_dishes', title: '只洗 3 個碗或 1 個杯子', effortLevel: 'gentle', energyLevel: 'steady', doctorNote: '剩下的留給未來的自己，今天先洗這幾個就好。' },
    { id: 'rx_reply', title: '回覆 1 則拖延已久的訊息', effortLevel: 'gentle', energyLevel: 'steady', doctorNote: '哪怕只回「好喔！晚點聊」或一個貼圖都算數。' },
    { id: 'rx_file', title: '打開卡關的文件，只寫 1 行字', effortLevel: 'gentle', energyLevel: 'steady', doctorNote: '開頭是最難的，寫出第一行標題就立刻給自己掌聲。' },
    { id: 'rx_walk', title: '去巷口買杯喜歡喝的順便散步 5 分鐘', effortLevel: 'gentle', energyLevel: 'steady', doctorNote: '不用開計步器，單純出去吹吹微風就很好。' }
  ];

  const PHARMACY_CATEGORIES = [
    { id: 'CONSUMABLE', titleZh: '💊 基礎消耗品' },
    { id: 'BED_GEAR', titleZh: '🛏️ 病床升級裝備' },
    { id: 'TREASURE', titleZh: '🏆 鎮院之寶' }
  ];

  const PHARMACY_ITEMS = [
    { id: 'pill_victory', name: '精神勝利法膠囊', shortLabel: '精神膠囊', category: 'CONSUMABLE', img: './assets/img_prop_pill_victory.png', description: '內含 100% 純天然空氣。吞服後會產生「我今天已經很努力了」的強烈幻覺，無任何副作用。' },
    { id: 'long_straw', name: '三公尺超長吸管', shortLabel: '3m吸管', category: 'CONSUMABLE', img: './assets/img_prop_long_straw.png', description: '專為連坐起來喝水都覺得浪費生命的患者設計，從床頭一路延伸至書桌水杯，實現真正的躺平補水。' },
    { id: 'procrastination_permit', name: '合法拖延許可證', shortLabel: '拖延許可', category: 'CONSUMABLE', img: './assets/img_prop_procrastination_permit.png', description: '蓋有主治醫師鋼印的官方文件。出示此證可理直氣壯地將任何非緊急事項推遲到「明天再說」。' },
    { id: 'antigravity_pillow', name: '反重力記憶枕', shortLabel: '記憶枕', category: 'BED_GEAR', img: './assets/img_prop_antigravity_pillow.png', description: '擁有強大的磁場，一旦頭部接觸枕面，將會產生高達 10 倍的地心引力，讓你與床鋪合而為一。' },
    { id: 'antisocial_blanket', name: '防社交結界被單', shortLabel: '結界被單', category: 'BED_GEAR', img: './assets/img_prop_antisocial_blanket.png', description: '只要將此被單拉過頭頂，即可自動屏蔽外界所有的訊息通知、未接來電與人類社交需求。' },
    { id: 'boba_iv_drip', name: '生理食鹽水（其實是手搖飲）點滴架', shortLabel: '珍奶點滴', category: 'BED_GEAR', img: './assets/img_prop_boba_iv_drip.png', description: '透過靜脈注射直接將全糖珍珠奶茶輸送至體內，省去咀嚼與吞嚥的體力消耗。' },
    { id: 'auto_turn_arm', name: '自動翻身機械臂', shortLabel: '翻身機械臂', category: 'BED_GEAR', img: './assets/img_prop_auto_turn_arm.png', description: '每隔兩小時自動幫患者翻面一次，有效預防長期維持同一姿勢滑手機造成的單側手臂麻痺。' },
    { id: 'silent_alarm', name: '永遠不會響的鬧鐘', shortLabel: '靜音鬧鐘', category: 'TREASURE', img: './assets/img_prop_silent_alarm.png', description: '拆除了所有發聲零件的高科技工藝品，每天早上準時用「絕對的安靜」溫柔喚醒你的自然醒。' },
    { id: 'miracle_banner', name: '醫學奇蹟錦旗', shortLabel: '居然活著', category: 'TREASURE', img: './assets/img_prop_miracle_banner.png', description: '掛在病床正上方的金色錦旗，上面繡著四個大字：「居然活著」，是本院最高榮譽象徵。' },
    { id: 'vip_deed', name: 'VIP 終身免費床位地契', shortLabel: 'VIP地契', category: 'TREASURE', img: './assets/img_prop_vip_deed.png', description: '恭喜你放棄治療並決定在此定居！持有此地契可永久享有本院軟爛 VIP 待遇，永遠不用出院。' }
  ];

  const WARD_ANCHORS = [
    { id: 'anchor_wall_left', labelZh: '床頭上方牆', x: 0.34, y: 0.25, isWall: true },
    { id: 'anchor_wall_mid', labelZh: '病歷牆區', x: 0.52, y: 0.34, isWall: true },
    { id: 'anchor_window_sill', labelZh: '陽光窗台', x: 0.76, y: 0.40, isWall: false },
    { id: 'anchor_nightstand', labelZh: '床頭小桌', x: 0.45, y: 0.43, isWall: false },
    { id: 'anchor_cabinet_top', labelZh: '藥櫃檯面', x: 0.60, y: 0.43, isWall: false },
    { id: 'anchor_bed_pillow', labelZh: '枕頭區', x: 0.31, y: 0.46, isWall: false },
    { id: 'anchor_bed_blanket', labelZh: '棉被區', x: 0.38, y: 0.56, isWall: false },
    { id: 'anchor_bed_iv', labelZh: '點滴架位', x: 0.26, y: 0.62, isWall: false },
    { id: 'anchor_chair_side', labelZh: '陪病椅', x: 0.83, y: 0.65, isWall: false },
    { id: 'anchor_bed_front', labelZh: '床邊地板', x: 0.45, y: 0.72, isWall: false },
    { id: 'anchor_rug_center', labelZh: '圓毯區', x: 0.58, y: 0.72, isWall: false }
  ];

  const WALL_HANGING_IDS = new Set(['miracle_banner', 'vip_deed']);

  function defaultWardPosition(itemId) {
    switch (itemId) {
      case 'miracle_banner': return [0.34, 0.25];
      case 'vip_deed': return [0.52, 0.34];
      case 'procrastination_permit': return [0.60, 0.43];
      case 'boba_iv_drip': return [0.26, 0.62];
      case 'antigravity_pillow': return [0.31, 0.46];
      case 'antisocial_blanket': return [0.38, 0.56];
      case 'auto_turn_arm': return [0.45, 0.72];
      case 'long_straw': return [0.58, 0.72];
      case 'pill_victory': return [0.76, 0.40];
      case 'silent_alarm': return [0.45, 0.43];
      default: return [0.58, 0.72];
    }
  }

  function nearestWardAnchor(x, y) {
    let best = WARD_ANCHORS[0];
    let bestDist = Infinity;
    for (const a of WARD_ANCHORS) {
      const dx = a.x - x;
      const dy = a.y - y;
      const d = dx * dx + dy * dy;
      if (d < bestDist) {
        bestDist = d;
        best = a;
      }
    }
    return best;
  }

  const WARD_WANDER_LINES = [
    '巡房中…生命跡象依然微弱',
    '這張床的引力挺強的',
    '觀察患者發呆進度',
    '今天也准許繼續耍廢',
    '檢查窗外雲有沒有在動',
    '盯著點滴一滴一滴落下，挺催眠的'
  ];

  const WARD_TAP_LINES = [
    '不要一直戳主治醫師，掛號費會增加喔。',
    '幹嘛？你今天水喝了嗎，還是只是手癢想點我？',
    '警告：重複戳擊醫療人員，將開立『多躺十分鐘』處分。',
    '…你以為點我就能加速康復嗎？不會，但我心情好了一點點。',
    '別戳了，白袍起皺的話我又要假裝自己在忙了。',
    '好喔，接收到你的生存訊號了。現在可以回去繼續當馬鈴薯了。',
    '再戳的話，我就把你的點滴偷偷換成無糖綠茶喔。',
    '這不是 NPC，我是有執照的冷面醫師（雖然大多時間也在發呆）。'
  ];

  const WARD_DRAG_START_LINES = [
    '……你這是在搬運醫療器材嗎？我好歹是有執照的主治醫師。',
    '輕一點，白袍要是起皺了我又要假裝很忙去燙衣服。',
    '把我拎起來是打算做什麼？先說好，我拒絕任何需要跑步的醫囑。',
    '如果等一下打算把我塞進角落，記得留一點發呆的空間給我。',
    '本院原則禁止夾帶醫師移動……算了，看你搬得挺開心的。',
    '注意腰椎保護，單手拎起成年白髮醫師屬於高風險行為。',
    '先把我放下來再說，口袋裡的珍珠奶茶差點灑出來了。',
    '這算是一種復健重量訓練嗎？如果是，那你今天運動量超標了。'
  ];

  const WARD_DRAG_DROP_BED_LINES = [
    '這張病床我先代為試躺看看，嗯，引力極強。',
    '經本醫師實測，這張床墊確實具備讓人放棄夢想的魔力。',
    '既然你把我放到床上，那現在換我住院，你負責查房。',
    '床邊這塊區域風水不錯，難怪你每天都不太想爬起來。'
  ];

  const WARD_DRAG_DROP_CHAIR_LINES = [
    '陪病椅也滿好坐的，患者今天請自行努力。',
    '我就坐在陪病椅上看你發呆，這樣算不算一對一監護？',
    '椅子軟硬度及格，准許我在這裡摸魚三十分鐘。'
  ];

  const WARD_DRAG_DROP_FLOOR_LINES = [
    '落地了。希望這裡沒有要我站著寫病歷的工作。',
    '新位置確認完畢。我就站在這兒冷眼看著你繼續耍廢。',
    '行吧，換個角度觀察患者躺平，也算是臨床研究的一環。',
    '雙腳重新踩回地板了。剛剛差點以為你要把我掛到牆上當裝飾。'
  ];

  function wardItemInteractionLine(targetId) {
    const map = {
      default_iv: [
        '檢查病床旁點滴流速中…滴速穩定，適合繼續躺平。',
        '確認生理食鹽水存量充足，今天也請安心廢著。'
      ],
      default_bed: [
        '幫病床檢查一下棉被邊角…嗯，軟爛度非常標準。',
        '這張病床的舒適度，足以讓人忘記外面的社會。'
      ],
      default_cabinet: [
        '清點醫療櫃裡的維他命與偷懶許可證…庫存正常。',
        '整理藥櫃中…其實裡面一半都是我的備用珍奶吸管。'
      ],
      default_chair: ['巡視陪病椅…今天也很安靜，沒有人催你起身。'],
      pill_victory: ['確認精神勝利法膠囊庫存：空氣含量 100%，品質優良。'],
      long_straw: ['研究這根三公尺吸管的流體力學…躺著喝水真是一大發明。'],
      procrastination_permit: ['幫你的拖延許可證補蓋一個鋼印，有效期延到下週（或明年）。'],
      antigravity_pillow: ['測試反重力記憶枕…嗯，差點連我的手也被吸在上面。'],
      antisocial_blanket: ['輕拍防社交結界被單，偵測到零個未接來電，防護力滿分。'],
      boba_iv_drip: ['檢查珍奶點滴濃度…（偷吸一口）嗯，全糖微冰，符合醫囑。'],
      auto_turn_arm: ['校準自動翻身機械臂的角度，等一下幫你翻面曬太陽。'],
      silent_alarm: ['確認靜音鬧鐘運作正常：明天早上也絕對不會吵醒你。'],
      miracle_banner: ['仰望『居然活著』錦旗，對你的頑強生命力致上冷面敬意。'],
      vip_deed: ['核對 VIP 終身床位地契…恭喜，本院永遠不會趕你出院。']
    };
    const arr = map[targetId] || ['對這件擺設進行例行醫療評估…看起來非常適合廢物復健。'];
    return arr[Math.floor(Math.random() * arr.length)];
  }

  const MILESTONES = [
    { threshold: 1, title: '生命跡象確認', subtitle: '居然真的動了 1 次' },
    { threshold: 5, title: '懶癌細胞暫退', subtitle: '累計完成 5 件廢人小事' },
    { threshold: 10, title: '臨床康復奇蹟', subtitle: '累計完成 10 件復健指標' },
    { threshold: 25, title: '本院終身傳說', subtitle: '累計完成 25 件（醫師震驚）' }
  ];

  function stageFromTodayCount(count) {
    if (count <= 0) {
      return { id: 'FLAT', subtitleZh: '患者生命跡象穩定，本院建議先躺著呼吸就好。', icon: ICONS.bedtime, glow: false };
    }
    if (count <= 2) {
      return { id: 'SIT', subtitleZh: '竟然動了一點點，病歷上先幫你記一筆奇蹟。', icon: ICONS.cafe, glow: false };
    }
    if (count <= 4) {
      return { id: 'CHEER', subtitleZh: '雖然表情沒變，但本醫師內心對你表示高度肯定。', icon: ICONS.heart, glow: false };
    }
    return { id: 'GLOW', subtitleZh: '今天的復健指標已達標，准許你安心去當一塊馬鈴薯。', icon: ICONS.sparkle, glow: true };
  }

  // -------------------- State & localStorage Persistence --------------------
  function createDefaultData() {
    const today = todayDateKey();
    const now = Date.now();
    return {
      profile: {
        displayName: '旅人患者',
        feifeiNickname: '主治醫師',
        todayDate: today,
        todayCount: 0,
        totalCompleted: 0,
        ownedItemIds: [],
        equippedItemIds: [],
        wardPlacements: []
      },
      tasks: STARTER_TASKS.map((t, i) => ({
        id: 'starter_' + i,
        title: t.title,
        effortLevel: t.effortLevel,
        completed: false,
        completedDate: '',
        note: t.note,
        createdAt: now - i * 1000
      })),
      checkins: [],
      gear: {
        coat: 'classic_white',
        stethoscope: 'default_heart'
      }
    };
  }

  function loadPersistedData() {
    const today = todayDateKey();
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) {
        const fresh = createDefaultData();
        savePersistedData(fresh);
        return fresh;
      }
      const parsed = JSON.parse(raw);
      const def = createDefaultData();
      const profile = Object.assign({}, def.profile, parsed.profile || {});
      if (!profile.feifeiNickname || !profile.feifeiNickname.trim()) {
        profile.feifeiNickname = '主治醫師';
      }
      if (profile.todayDate !== today) {
        profile.todayDate = today;
        profile.todayCount = 0;
      }
      const tasks = Array.isArray(parsed.tasks) ? parsed.tasks : def.tasks;
      const checkins = Array.isArray(parsed.checkins) ? parsed.checkins : [];
      const gear = {
        coat: COAT_STYLES.some((c) => c.id === parsed.gear?.coat) ? parsed.gear.coat : 'classic_white',
        stethoscope: STETHOSCOPE_STYLES.some((s) => s.id === parsed.gear?.stethoscope) ? parsed.gear.stethoscope : 'default_heart'
      };
      const normalized = { profile, tasks, checkins, gear };
      savePersistedData(normalized);
      return normalized;
    } catch (_) {
      const fresh = createDefaultData();
      savePersistedData(fresh);
      return fresh;
    }
  }

  function savePersistedData(data) {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
    } catch (e) {
      console.warn('localStorage save failed', e);
    }
  }

  function resolveWardPlacements(profile) {
    if (!profile) return [];
    if (Array.isArray(profile.wardPlacements) && profile.wardPlacements.length > 0) {
      return profile.wardPlacements
        .map((p) => {
          if (typeof p === 'string') {
            const [itemId, xs, ys] = p.split(':');
            if (!itemId) return null;
            const [dx, dy] = defaultWardPosition(itemId);
            return {
              itemId,
              x: Math.min(0.92, Math.max(0.08, parseFloat(xs) || dx)),
              y: Math.min(0.86, Math.max(0.14, parseFloat(ys) || dy))
            };
          }
          if (p && p.itemId) {
            const [dx, dy] = defaultWardPosition(p.itemId);
            return {
              itemId: p.itemId,
              x: Math.min(0.92, Math.max(0.08, Number(p.x) || dx)),
              y: Math.min(0.86, Math.max(0.14, Number(p.y) || dy))
            };
          }
          return null;
        })
        .filter(Boolean);
    }
    return (profile.equippedItemIds || []).map((id) => {
      const [dx, dy] = defaultWardPosition(id);
      return { itemId: id, x: dx, y: dy };
    });
  }

  // -------------------- Global App Runtime State --------------------
  let db = loadPersistedData();
  const calNow = new Date();

  const ui = {
    selectedTab: 'room',
    selectedEnergyFilter: 'flat',
    drawnPrescription: null,
    feifeiQuote: FEIFEI_QUOTES[Math.floor(Math.random() * FEIFEI_QUOTES.length)],
    patCounter: 0,
    stagePose: 'OBSERVE',
    pharmacyPose: 'PHARMACY',
    pharmacyQuote: PHARMACY_QUOTES[0],
    pharmacyPatCounter: 0,
    prescriptionPose: 'OBSERVE',
    prescriptionQuote: PRESCRIPTION_QUOTES[0],
    prescriptionPatCounter: 0,
    memoryPose: 'APPROVE',
    memoryQuote: MEMORY_QUOTES[0],
    memoryPatCounter: 0,
    wardDoctorPose: 'OBSERVE',
    openMenuTaskId: null,
    diaryMode: 'LIST',
    showAnchors: false,
    activePlacingItemId: null,
    batteryPercent: 10,
    reflectionDraft: '',
    lastLoadedCheckinKey: null,
    calYear: calNow.getFullYear(),
    calMonth: calNow.getMonth() + 1,
    selectedCalDate: todayDateKey(),
    equipReactionQuote: null,
    modal: null // { type: 'addTask' | 'editTask' | 'shrinkTask' | 'doctorEquip' | 'account', task?: ... }
  };

  function syncDraftFromTodayCheckin() {
    const today = todayDateKey();
    const todayEntry = db.checkins.find((c) => c.dateKey === today);
    if (todayEntry) {
      ui.batteryPercent = todayEntry.batteryPercent != null
        ? todayEntry.batteryPercent
        : (todayEntry.energyLevel === 'steady' ? 75 : todayEntry.energyLevel === 'low' ? 35 : 10);
      ui.reflectionDraft = todayEntry.reflection || '';
    }
    ui.lastLoadedCheckinKey = today;
  }
  syncDraftFromTodayCheckin();

  function commitDb() {
    savePersistedData(db);
  }

  let snackbarTimer = null;
  function showToast(msg) {
    const el = document.getElementById('snackbar');
    if (!el) return;
    el.textContent = msg;
    el.classList.add('show');
    clearTimeout(snackbarTimer);
    snackbarTimer = setTimeout(() => {
      el.classList.remove('show');
    }, 2800);
  }

  // -------------------- SVG Doctor Gear Overlay (Exact Port of DoctorAvatarWithGear.kt) --------------------
  function renderStarSvg(cx, cy, outerR, fill, outlineW) {
    const innerR = outerR * 0.45;
    const pts = [];
    for (let i = 0; i < 10; i++) {
      const a = -Math.PI / 2 + i * (Math.PI / 5);
      const r = i % 2 === 0 ? outerR : innerR;
      pts.push(`${(cx + r * Math.cos(a)).toFixed(1)},${(cy + r * Math.sin(a)).toFixed(1)}`);
    }
    return `<polygon points="${pts.join(' ')}" fill="${fill}" stroke="#4B3532" stroke-width="${outlineW.toFixed(2)}" stroke-linejoin="round" />`;
  }

  function renderPawSvg(cx, cy, r, fill, outlineW) {
    const toeR = r * 0.42;
    const toes = [
      [cx - r * 0.95, cy - r * 0.85],
      [cx, cy - r * 1.15],
      [cx + r * 0.95, cy - r * 0.85]
    ];
    return toes.map(([tx, ty]) =>
      `<circle cx="${tx.toFixed(1)}" cy="${ty.toFixed(1)}" r="${toeR.toFixed(1)}" fill="${fill}" stroke="#4B3532" stroke-width="${(outlineW * 0.7).toFixed(2)}" />`
    ).join('') +
      `<circle cx="${cx.toFixed(1)}" cy="${cy.toFixed(1)}" r="${r.toFixed(1)}" fill="${fill}" stroke="#4B3532" stroke-width="${(outlineW * 0.8).toFixed(2)}" />`;
  }

  function renderDoctorAvatarHtml(imgSrc, gear, heightCss, altText, isSideProfile = false, extraClass = '') {
    const w = isSideProfile ? 256 : 285;
    const h = 512;
    const aspect = isSideProfile ? '0.5' : '0.556';
    const outlineW = w * 0.015;
    let svgParts = '';

    // White Coat Accessories
    if (gear.coat === 'star_director') {
      const pinX = isSideProfile ? w * 0.62 : w * 0.355;
      const pinY = h * 0.485;
      svgParts += renderStarSvg(pinX, pinY, w * 0.048, '#FFD54F', outlineW * 0.9);
      if (!isSideProfile) {
        const x1 = w * 0.69, y1 = h * 0.618, x2 = w * 0.72, y2 = h * 0.588;
        svgParts += `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="#7986CB" stroke-width="${(outlineW * 2.1).toFixed(2)}" stroke-linecap="round" />`;
        svgParts += `<circle cx="${x2}" cy="${y2}" r="${(w * 0.022).toFixed(1)}" fill="#FFD54F" stroke="#4B3532" stroke-width="${(outlineW * 0.65).toFixed(2)}" />`;
        svgParts += `<rect x="${w * 0.25}" y="${h * 0.578}" width="${w * 0.09}" height="${h * 0.015}" rx="4" fill="rgba(144,202,249,0.85)" />`;
        svgParts += `<rect x="${w * 0.67}" y="${h * 0.578}" width="${w * 0.09}" height="${h * 0.015}" rx="4" fill="rgba(144,202,249,0.85)" />`;
      }
    } else if (gear.coat === 'cat_paw_coat') {
      if (!isSideProfile) {
        const cx = w * 0.5, cy = h * 0.475, hw = w * 0.062, hh = w * 0.025;
        svgParts += `<path d="M ${cx} ${cy} L ${cx - hw} ${cy - hh} L ${cx - hw} ${cy + hh} Z M ${cx} ${cy} L ${cx + hw} ${cy - hh} L ${cx + hw} ${cy + hh} Z" fill="#F48FB1" stroke="#4B3532" stroke-width="${(outlineW * 0.85).toFixed(2)}" stroke-linejoin="round" />`;
        svgParts += `<circle cx="${cx}" cy="${cy}" r="${(hh * 0.55).toFixed(1)}" fill="#F48FB1" stroke="#4B3532" stroke-width="${(outlineW * 0.85).toFixed(2)}" />`;
        svgParts += renderPawSvg(w * 0.32, h * 0.65, w * 0.036, '#F8BBD0', outlineW * 0.75);
        svgParts += renderPawSvg(w * 0.68, h * 0.65, w * 0.036, '#F8BBD0', outlineW * 0.75);
      } else {
        svgParts += renderPawSvg(w * 0.59, h * 0.635, w * 0.042, '#F8BBD0', outlineW * 0.75);
      }
    } else if (gear.coat === 'pastel_mint_coat') {
      if (!isSideProfile) {
        svgParts += `<line x1="${w * 0.365}" y1="${h * 0.47}" x2="${w * 0.395}" y2="${h * 0.625}" stroke="#80CBC4" stroke-width="${(outlineW * 1.9).toFixed(2)}" stroke-linecap="round" />`;
        svgParts += `<line x1="${w * 0.635}" y1="${h * 0.47}" x2="${w * 0.605}" y2="${h * 0.625}" stroke="#80CBC4" stroke-width="${(outlineW * 1.9).toFixed(2)}" stroke-linecap="round" />`;
        const tx = w * 0.635, ty = h * 0.568, tw = w * 0.115, th = h * 0.03;
        svgParts += `<rect x="${tx}" y="${ty}" width="${tw}" height="${th}" rx="4" fill="#E0F2F1" stroke="#4B3532" stroke-width="${(outlineW * 0.7).toFixed(2)}" />`;
        svgParts += `<line x1="${tx + tw * 0.22}" y1="${ty + th * 0.5}" x2="${tx + tw * 0.78}" y2="${ty + th * 0.5}" stroke="#00897B" stroke-width="${(outlineW * 0.9).toFixed(2)}" stroke-linecap="round" />`;
      } else {
        svgParts += `<rect x="${w * 0.55}" y="${h * 0.565}" width="${w * 0.12}" height="${h * 0.025}" rx="4" fill="#80CBC4" />`;
      }
    }

    // Stethoscope Accessories
    if (gear.stethoscope !== 'default_heart') {
      const tubeColor = gear.stethoscope === 'gold_vip'
        ? '#FFCA28'
        : gear.stethoscope === 'cat_bell'
          ? '#81D4FA'
          : '#D7A86E';
      const neckD = !isSideProfile
        ? `M ${w * 0.375} ${h * 0.525} C ${w * 0.385} ${h * 0.452}, ${w * 0.425} ${h * 0.448}, ${w * 0.438} ${h * 0.478} M ${w * 0.578} ${h * 0.46} C ${w * 0.605} ${h * 0.458}, ${w * 0.625} ${h * 0.486}, ${w * 0.625} ${h * 0.52}`
        : `M ${w * 0.495} ${h * 0.46} C ${w * 0.555} ${h * 0.465}, ${w * 0.600} ${h * 0.492}, ${w * 0.608} ${h * 0.525}`;

      svgParts += `<path d="${neckD}" fill="none" stroke="#4B3532" stroke-width="${(outlineW * 3.1).toFixed(2)}" stroke-linecap="round" stroke-linejoin="round" />`;
      svgParts += `<path d="${neckD}" fill="none" stroke="${tubeColor}" stroke-width="${(outlineW * 1.75).toFixed(2)}" stroke-linecap="round" stroke-linejoin="round" />`;

      const dcx = isSideProfile ? w * 0.61 : w * 0.626;
      const dcy = h * 0.528;
      const dr = w * 0.045;

      if (gear.stethoscope === 'gold_vip') {
        svgParts += `<circle cx="${dcx}" cy="${dcy}" r="${dr}" fill="#FFD54F" stroke="#4B3532" stroke-width="${(outlineW * 0.85).toFixed(2)}" />`;
        svgParts += renderStarSvg(dcx, dcy, dr * 0.72, '#FFF8E1', outlineW * 0.55);
      } else if (gear.stethoscope === 'cat_bell') {
        const eo = dr * 0.62;
        const lEar = `M ${dcx - eo * 1.1} ${dcy - dr * 0.4} L ${dcx - eo * 1.35} ${dcy - dr * 1.35} L ${dcx - eo * 0.25} ${dcy - dr * 0.85} Z`;
        const rEar = `M ${dcx + eo * 1.1} ${dcy - dr * 0.4} L ${dcx + eo * 1.35} ${dcy - dr * 1.35} L ${dcx + eo * 0.25} ${dcy - dr * 0.85} Z`;
        svgParts += `<path d="${lEar}" fill="#FFCDD2" stroke="#4B3532" stroke-width="${(outlineW * 0.75).toFixed(2)}" />`;
        svgParts += `<path d="${rEar}" fill="#FFCDD2" stroke="#4B3532" stroke-width="${(outlineW * 0.75).toFixed(2)}" />`;
        svgParts += `<circle cx="${dcx}" cy="${dcy}" r="${dr}" fill="#FFE082" stroke="#4B3532" stroke-width="${(outlineW * 0.85).toFixed(2)}" />`;
        svgParts += `<line x1="${dcx - dr * 0.48}" y1="${dcy + dr * 0.22}" x2="${dcx + dr * 0.48}" y2="${dcy + dr * 0.22}" stroke="#4B3532" stroke-width="${(outlineW * 0.85).toFixed(2)}" stroke-linecap="round" />`;
        svgParts += `<circle cx="${dcx}" cy="${dcy + dr * 0.1}" r="${dr * 0.18}" fill="#4B3532" />`;
      } else if (gear.stethoscope === 'boba_scope') {
        const cw = dr * 1.65, ch = dr * 1.9, ct = dcy - ch * 0.45, cl = dcx - cw / 2;
        svgParts += `<line x1="${dcx + cw * 0.12}" y1="${ct + ch * 0.25}" x2="${dcx + cw * 0.24}" y2="${ct - ch * 0.42}" stroke="rgba(156,39,176,0.75)" stroke-width="${(outlineW * 1.3).toFixed(2)}" stroke-linecap="round" />`;
        svgParts += `<rect x="${cl}" y="${ct}" width="${cw}" height="${ch}" rx="3.5" fill="#E6C29F" stroke="#4B3532" stroke-width="${(outlineW * 0.8).toFixed(2)}" />`;
        const pr = dr * 0.17;
        svgParts += `<circle cx="${dcx - cw * 0.22}" cy="${ct + ch * 0.72}" r="${pr}" fill="#4B3532" />`;
        svgParts += `<circle cx="${dcx}" cy="${ct + ch * 0.76}" r="${pr}" fill="#4B3532" />`;
        svgParts += `<circle cx="${dcx + cw * 0.22}" cy="${ct + ch * 0.72}" r="${pr}" fill="#4B3532" />`;
      }
    }

    const svgOverlay = svgParts
      ? `<svg viewBox="0 0 ${w} ${h}" xmlns="http://www.w3.org/2000/svg">${svgParts}</svg>`
      : '';

    return `
      <span class="doctor-avatar ${extraClass}" style="height:${heightCss};aspect-ratio:${aspect};">
        <img src="${imgSrc}" alt="${escapeHtml(altText)}" draggable="false" />
        ${svgOverlay}
      </span>
    `;
  }

  // -------------------- Reusable Fragments --------------------
  function renderDoctorBannerHtml(opts) {
    const pose = DOCTOR_POSES[opts.pose] || DOCTOR_POSES.OBSERVE;
    return `
      <button type="button" class="doctor-banner" data-action="${opts.action}">
        <div class="banner-avatar-frame">
          ${renderDoctorAvatarHtml(pose.img, db.gear, '102px', `${opts.doctorName}目前動作：${pose.labelZh}`)}
        </div>
        <div class="banner-copy">
          <div><span class="pill pill-secondary">${escapeHtml(opts.doctorName)}</span></div>
          <div class="banner-quote">「${escapeHtml(opts.quote)}」</div>
          ${opts.subtitle ? `<div class="banner-sub">${escapeHtml(opts.subtitle)}</div>` : ''}
          <div class="banner-tap-hint">${ICONS.touch}<span>點一下與醫師互動</span></div>
        </div>
      </button>
    `;
  }

  // Static snapshot WardRoomView (used in Memories postcards & Diary entries / Calendar detail)
  function renderStaticWardRoomHtml(placements, fixedPoseName) {
    const pose = DOCTOR_POSES[fixedPoseName] || DOCTOR_POSES.OBSERVE;
    const docX = 0.59;
    const docY = 0.73;

    const propsHtml = placements.map((pl, idx) => {
      const item = PHARMACY_ITEMS.find((it) => it.id === pl.itemId);
      if (!item) return '';
      const anchor = nearestWardAnchor(pl.x, pl.y);
      const isWall = anchor.isWall && pl.y < 0.37;
      const zOrder = isWall ? (2 + idx * 0.03) : (pl.y > docY + 0.01 ? (12 + idx * 0.03) : (5 + idx * 0.03));
      const sizePct = propWidthPct(item.id);
      const showShadow = !WALL_HANGING_IDS.has(item.id);
      return `
        <div class="ward-prop-sprite static" style="left:${(pl.x * 100).toFixed(2)}%;top:${(pl.y * 100).toFixed(2)}%;z-index:${zOrder};width:${sizePct}%;">
          <div class="ward-prop-body" style="width:100%;aspect-ratio:1/1;">
            ${showShadow ? '<div class="ward-shadow"></div>' : ''}
            <img src="${item.img}" alt="${escapeHtml(item.name)}" />
          </div>
        </div>
      `;
    }).join('');

    return `
      <div class="ward-room-shell card-framed">
        <img class="ward-bg-img" src="./assets/img_ward_room_bg.png" alt="當日病房快照" />
        ${propsHtml}
        <div class="ward-doctor-visual" style="left:${docX * 100}%;top:${docY * 100}%;height:19.5%;">
          ${renderDoctorAvatarHtml(pose.img, db.gear, '100%', '當日醫師快照')}
        </div>
      </div>
    `;
  }

  function propWidthPct(itemId) {
    switch (itemId) {
      case 'boba_iv_drip': return 15;
      case 'auto_turn_arm': return 12.5;
      case 'antigravity_pillow': return 12;
      case 'antisocial_blanket': return 12;
      case 'miracle_banner': return 11.5;
      case 'vip_deed': return 11.5;
      case 'long_straw': return 11;
      case 'procrastination_permit': return 10.5;
      default: return 10;
    }
  }

  // -------------------- Live Interactive 2.5D Ward Controller --------------------
  let activeWardCleanup = null;

  function stopLiveWardController() {
    if (typeof activeWardCleanup === 'function') {
      activeWardCleanup();
      activeWardCleanup = null;
    }
  }

  function clampToWalkableFloor(rawX, rawY) {
    const x = Math.min(0.74, Math.max(0.38, rawX));
    let minY;
    if (x < 0.44) minY = 0.76;
    else if (x < 0.58) minY = Math.max(0.59, 0.76 - 0.85 * (x - 0.44));
    else if (x <= 0.65) minY = 0.59;
    else minY = 0.59 + (x - 0.65) * 0.65;

    const maxY = Math.min(0.84, 0.88 - 0.52 * Math.abs(x - 0.50));
    const safeMax = Math.max(minY + 0.01, maxY);
    const y = Math.min(safeMax, Math.max(minY, rawY));
    return [x, y];
  }

  function isBehindBed(x, y) {
    if (x < 0.19 || x > 0.60) return false;
    const frontEdgeY = x <= 0.43 ? 0.66 + 0.43 * (x - 0.22) : 0.75 - 0.73 * (x - 0.43);
    return y < frontEdgeY;
  }
  function isBehindIv(x, y) { return x >= 0.14 && x <= 0.29 && y < 0.72; }
  function isBehindCabinet(x, y) { return x >= 0.51 && x <= 0.69 && y < 0.55; }
  function isBehindChair(x, y) { return x >= 0.77 && x <= 0.95 && y < 0.71; }

  function mountLiveWardRoom(rootEl) {
    stopLiveWardController();
    if (!rootEl) return;

    const placements = resolveWardPlacements(db.profile);
    const doctorName = db.profile.feifeiNickname || '主治醫師';

    const state = {
      docX: 0.56,
      docY: 0.72,
      facingRight: false,
      spriteKey: 'IDLE_A',
      isWalking: false,
      isTurning: false,
      isFingerDown: false,
      isDraggingDoctor: false,
      playerHoldUntil: 0,
      tapReactUntil: 0,
      interactingId: null,
      speech: WARD_WANDER_LINES[0],
      tapNonce: 0,
      tick: 0,
      walkPhase: 0,
      draggingPropId: null,
      hoveredAnchorId: null
    };

    rootEl.innerHTML = `
      <div id="live-ward-stage" class="ward-room-shell">
        <img class="ward-bg-img" src="./assets/img_ward_room_bg.png" alt="療癒可愛病房全景" draggable="false" />
        <div id="ward-iv-badge" class="ward-iv-badge hidden">💧 點滴巡檢中</div>
        <div id="ward-anchors-layer"></div>
        <div id="ward-props-layer"></div>

        <!-- Visual Doctor Layer (under occlusion overlays when behind furniture) -->
        <div id="ward-doc-visual" class="ward-doctor-visual" style="left:56%;top:72%;height:26%;">
          <div class="ward-shadow" style="width:42px;bottom:0;"></div>
          <div id="ward-doc-sprite-wrap" class="ward-doctor-sprite-inner" style="height:100%;display:flex;align-items:flex-end;">
            ${renderDoctorAvatarHtml(WARD_SPRITES.IDLE_A, db.gear, '100%', `${doctorName}在病房巡房`, false)}
          </div>
        </div>

        <!-- Floating Speech Bubble -->
        <div id="ward-speech-bubble" class="ward-speech-bubble">${escapeHtml(state.speech)}</div>

        <!-- Dedicated Gesture Hitbox Layer (above furniture occlusion so dragging NEVER gets blocked or jitters!) -->
        <div id="ward-doc-hitbox" class="ward-doctor-hitbox" style="left:56%;top:72%;width:16%;height:26%;" title="點一下互動・長按拖曳移動位置"></div>

        <!-- 2.5D Furniture Occlusion Overlays -->
        <img id="occ-bed" class="ward-occlusion ward-occlusion-bed hidden" src="./assets/img_ward_room_bg.png" alt="" />
        <img id="occ-iv" class="ward-occlusion ward-occlusion-iv hidden" src="./assets/img_ward_room_bg.png" alt="" />
        <img id="occ-cabinet" class="ward-occlusion ward-occlusion-cabinet hidden" src="./assets/img_ward_room_bg.png" alt="" />
        <img id="occ-chair" class="ward-occlusion ward-occlusion-chair hidden" src="./assets/img_ward_room_bg.png" alt="" />

        <div id="ward-top-pill" class="ward-top-pill"></div>
      </div>
    `;

    const stage = document.getElementById('live-ward-stage');
    const docVisual = document.getElementById('ward-doc-visual');
    const docSpriteWrap = document.getElementById('ward-doc-sprite-wrap');
    const docHitbox = document.getElementById('ward-doc-hitbox');
    const speechEl = document.getElementById('ward-speech-bubble');
    const ivBadgeEl = document.getElementById('ward-iv-badge');
    const anchorsLayer = document.getElementById('ward-anchors-layer');
    const propsLayer = document.getElementById('ward-props-layer');
    const topPill = document.getElementById('ward-top-pill');
    const occBed = document.getElementById('occ-bed');
    const occIv = document.getElementById('occ-iv');
    const occCabinet = document.getElementById('occ-cabinet');
    const occChair = document.getElementById('occ-chair');

    function updateTopPill() {
      if (state.isDraggingDoctor) {
        topPill.textContent = '🖐️ 拖曳中：放開即可把醫師留在該處';
      } else if (ui.activePlacingItemId) {
        topPill.textContent = '📍 請點選病房內的目標錨點放置道具';
      } else if (placements.length === 0) {
        topPill.textContent = '✨ 長按拖曳醫師換位置・點道具擺進病房';
      } else {
        topPill.textContent = '✋ 拖曳道具吸錨點・長按移動醫師';
      }
    }

    function updateDoctorVisual(smoothMs = 0) {
      const transition = smoothMs > 0 ? `left ${smoothMs}ms linear, top ${smoothMs}ms linear` : 'none';
      docVisual.style.transition = transition;
      docVisual.style.left = `${(state.docX * 100).toFixed(2)}%`;
      docVisual.style.top = `${(state.docY * 100).toFixed(2)}%`;

      if (!state.isDraggingDoctor) {
        docHitbox.style.transition = transition;
        docHitbox.style.left = `${(state.docX * 100).toFixed(2)}%`;
        docHitbox.style.top = `${(state.docY * 100).toFixed(2)}%`;
      }

      const bubbleLeft = Math.min(82, Math.max(18, state.docX * 100));
      const bubbleTop = Math.max(18, state.docY * 100 - 27.5);
      speechEl.style.transition = transition;
      speechEl.style.left = `${bubbleLeft.toFixed(2)}%`;
      speechEl.style.top = `${bubbleTop.toFixed(2)}%`;
      speechEl.textContent = state.speech;
      speechEl.classList.toggle('highlight', state.isDraggingDoctor || Date.now() < state.tapReactUntil);

      const scaleX = state.facingRight ? -1 : 1;
      const liftY = state.isDraggingDoctor ? -9 : 0;
      const scaleMul = state.isDraggingDoctor ? 1.12 : 1.0;
      docSpriteWrap.style.transform = `translateY(${liftY}px) scale(${scaleX * scaleMul}, ${scaleMul})`;

      occBed.classList.toggle('hidden', !isBehindBed(state.docX, state.docY));
      occIv.classList.toggle('hidden', !isBehindIv(state.docX, state.docY));
      occCabinet.classList.toggle('hidden', !isBehindCabinet(state.docX, state.docY));
      occChair.classList.toggle('hidden', !isBehindChair(state.docX, state.docY));
      ivBadgeEl.classList.toggle('hidden', state.interactingId !== 'default_iv');

      updateTopPill();
    }

    function renderDoctorSprite() {
      const isSide = state.spriteKey === 'WALK_A' || state.spriteKey === 'WALK_B' || state.spriteKey === 'TURN';
      docSpriteWrap.innerHTML = renderDoctorAvatarHtml(
        WARD_SPRITES[state.spriteKey],
        db.gear,
        '100%',
        `${doctorName}在病房巡房`,
        isSide
      );
    }

    function renderAnchors() {
      const visible = ui.showAnchors || state.draggingPropId !== null || ui.activePlacingItemId !== null;
      if (!visible) {
        anchorsLayer.innerHTML = '';
        return;
      }
      const occupiedIds = new Set(placements.map((p) => nearestWardAnchor(p.x, p.y).id));
      anchorsLayer.innerHTML = WARD_ANCHORS.map((a) => {
        const hovered = state.hoveredAnchorId === a.id;
        const occupied = occupiedIds.has(a.id);
        return `
          <button
            type="button"
            class="ward-anchor-dot ${hovered ? 'hovered' : ''} ${occupied ? 'occupied' : ''}"
            data-anchor-id="${a.id}"
            style="left:${a.x * 100}%;top:${a.y * 100}%;"
          >
            <span class="ward-anchor-circle"><span class="ward-anchor-inner"></span></span>
            <span class="ward-anchor-label">${escapeHtml(a.labelZh)}</span>
          </button>
        `;
      }).join('');

      anchorsLayer.querySelectorAll('.ward-anchor-dot').forEach((btn) => {
        btn.addEventListener('click', () => {
          const anchorId = btn.getAttribute('data-anchor-id');
          const anchor = WARD_ANCHORS.find((a) => a.id === anchorId);
          if (!anchor) return;
          if (ui.activePlacingItemId) {
            moveWardItem(ui.activePlacingItemId, anchor.x, anchor.y);
          } else {
            const existing = placements.find((p) => nearestWardAnchor(p.x, p.y).id === anchor.id);
            if (existing) {
              ui.activePlacingItemId = existing.itemId;
              renderCurrentTab();
            } else {
              const placedIds = new Set(placements.map((p) => p.itemId));
              const nextOwned = db.profile.ownedItemIds.find((id) => !placedIds.has(id));
              if (nextOwned) {
                ui.activePlacingItemId = nextOwned;
                moveWardItem(nextOwned, anchor.x, anchor.y);
              }
            }
          }
        });
      });
    }

    function renderProps() {
      propsLayer.innerHTML = placements.map((pl, idx) => {
        const item = PHARMACY_ITEMS.find((it) => it.id === pl.itemId);
        if (!item) return '';
        const anchor = nearestWardAnchor(pl.x, pl.y);
        const isWall = anchor.isWall && pl.y < 0.37;
        const zOrder = isWall ? (2 + idx * 0.03) : (pl.y > state.docY + 0.01 ? (12 + idx * 0.03) : (5 + idx * 0.03));
        const interacting = state.interactingId === item.id;
        const showShadow = !WALL_HANGING_IDS.has(item.id);
        return `
          <div
            class="ward-prop-sprite ${interacting ? 'interacting' : ''}"
            data-prop-id="${item.id}"
            style="left:${pl.x * 100}%;top:${pl.y * 100}%;z-index:${zOrder};width:${propWidthPct(item.id)}%;"
          >
            ${interacting ? `<div class="ward-prop-tag">✨ ${escapeHtml(item.shortLabel)}</div>` : ''}
            <div class="ward-prop-body" style="width:100%;aspect-ratio:1/1;">
              ${showShadow ? '<div class="ward-shadow"></div>' : ''}
              <img src="${item.img}" alt="${escapeHtml(item.name)}" draggable="false" />
            </div>
          </div>
        `;
      }).join('');

      // Attach smooth pointer drag + snap to nearest anchor on each prop
      propsLayer.querySelectorAll('.ward-prop-sprite').forEach((propEl) => {
        const itemId = propEl.getAttribute('data-prop-id');
        propEl.addEventListener('pointerdown', (e) => {
          e.preventDefault();
          e.stopPropagation();
          propEl.setPointerCapture(e.pointerId);
          const rect = stage.getBoundingClientRect();
          state.draggingPropId = itemId;
          propEl.classList.add('dragging');
          const tag = document.createElement('div');
          tag.className = 'ward-prop-tag';
          const foundItem = PHARMACY_ITEMS.find((it) => it.id === itemId);
          tag.textContent = foundItem ? foundItem.shortLabel : '';
          if (!propEl.querySelector('.ward-prop-tag')) {
            propEl.prepend(tag);
          }

          let lastFx = (e.clientX - rect.left) / rect.width;
          let lastFy = (e.clientY - rect.top) / rect.height;

          const onMove = (ev) => {
            lastFx = Math.min(0.92, Math.max(0.08, (ev.clientX - rect.left) / rect.width));
            lastFy = Math.min(0.86, Math.max(0.14, (ev.clientY - rect.top) / rect.height));
            propEl.style.left = `${lastFx * 100}%`;
            propEl.style.top = `${lastFy * 100}%`;
            const hovered = nearestWardAnchor(lastFx, lastFy);
            if (state.hoveredAnchorId !== hovered.id) {
              state.hoveredAnchorId = hovered.id;
              renderAnchors();
            }
          };

          const finish = () => {
            propEl.removeEventListener('pointermove', onMove);
            propEl.removeEventListener('pointerup', finish);
            propEl.removeEventListener('pointercancel', finish);
            propEl.classList.remove('dragging');
            const snapped = nearestWardAnchor(lastFx, lastFy);
            state.draggingPropId = null;
            state.hoveredAnchorId = null;
            moveWardItem(itemId, snapped.x, snapped.y);
          };

          propEl.addEventListener('pointermove', onMove);
          propEl.addEventListener('pointerup', finish);
          propEl.addEventListener('pointercancel', finish);
          renderAnchors();
        });
      });
    }

    // Unified Doctor Tap + Long-Press Drag Pointer Handling
    let longPressTimer = null;
    let activePointerId = null;
    let downX = 0;
    let downY = 0;
    let stageRect = null;
    let dragStartDocX = 0.56;
    let dragStartDocY = 0.72;

    function resetPointerState() {
      clearTimeout(longPressTimer);
      longPressTimer = null;
      activePointerId = null;
      state.isFingerDown = false;
      state.isDraggingDoctor = false;
      docHitbox.classList.remove('dragging');
    }

    docHitbox.addEventListener('pointerdown', (e) => {
      if (activePointerId !== null) return;
      activePointerId = e.pointerId;
      stageRect = stage.getBoundingClientRect();
      downX = e.clientX;
      downY = e.clientY;

      // Freeze patrol immediately on pointerdown!
      const computed = window.getComputedStyle(docVisual);
      const curLeftPx = parseFloat(computed.left);
      const curTopPx = parseFloat(computed.top);
      if (Number.isFinite(curLeftPx) && Number.isFinite(curTopPx) && stageRect.width > 0 && stageRect.height > 0) {
        state.docX = Math.min(0.90, Math.max(0.12, curLeftPx / stageRect.width));
        state.docY = Math.min(0.87, Math.max(0.32, curTopPx / stageRect.height));
      }
      dragStartDocX = state.docX;
      dragStartDocY = state.docY;
      state.isFingerDown = true;
      state.isWalking = false;
      state.isTurning = false;
      updateDoctorVisual(0);

      try { docHitbox.setPointerCapture(e.pointerId); } catch (_) {}

      longPressTimer = setTimeout(() => {
        if (activePointerId === null) return;
        state.isDraggingDoctor = true;
        state.interactingId = null;
        state.playerHoldUntil = Date.now() + 9000;
        state.spriteKey = 'TURN';
        state.speech = pickNonRepeating(WARD_DRAG_START_LINES, state.speech);
        docHitbox.classList.add('dragging');
        if (navigator.vibrate) {
          try { navigator.vibrate(22); } catch (_) {}
        }
        renderDoctorSprite();
        updateDoctorVisual(0);
      }, 360);
    });

    docHitbox.addEventListener('pointermove', (e) => {
      if (e.pointerId !== activePointerId) return;
      const dxPx = e.clientX - downX;
      const dyPx = e.clientY - downY;

      if (!state.isDraggingDoctor) {
        if (Math.hypot(dxPx, dyPx) > 12) {
          // User is scrolling/swiping before long press -> abort cleanly
          resetPointerState();
        }
        return;
      }

      e.preventDefault();
      if (!stageRect || stageRect.width <= 0 || stageRect.height <= 0) return;
      const prevX = state.docX;
      state.docX = Math.min(0.90, Math.max(0.12, dragStartDocX + dxPx / stageRect.width));
      state.docY = Math.min(0.87, Math.max(0.32, dragStartDocY + dyPx / stageRect.height));
      if (Math.abs(state.docX - prevX) > 0.004) {
        state.facingRight = state.docX > prevX;
      }
      updateDoctorVisual(0);
    });

    function onDocPointerEnd(e) {
      if (e.pointerId !== activePointerId) return;
      clearTimeout(longPressTimer);
      longPressTimer = null;

      if (state.isDraggingDoctor) {
        state.isDraggingDoctor = false;
        state.isFingerDown = false;
        state.playerHoldUntil = Date.now() + 8500;
        docHitbox.classList.remove('dragging');
        state.spriteKey = 'IDLE_A';

        let dropPool = WARD_DRAG_DROP_FLOOR_LINES;
        if (state.docX >= 0.21 && state.docX <= 0.56 && state.docY >= 0.42 && state.docY <= 0.67) {
          dropPool = WARD_DRAG_DROP_BED_LINES;
        } else if (state.docX >= 0.75 && state.docY >= 0.50 && state.docY <= 0.74) {
          dropPool = WARD_DRAG_DROP_CHAIR_LINES;
        }
        state.speech = pickNonRepeating(dropPool, state.speech);
        renderDoctorSprite();
        updateDoctorVisual(0);
      } else if (state.isFingerDown) {
        // Short tap!
        state.isFingerDown = false;
        state.tapNonce += 1;
        state.tapReactUntil = Date.now() + 2600;
        state.interactingId = null;
        const reactionSprites = ['POSE_APPROVE', 'INTERACT', 'IDLE_B', 'POSE_SIP'];
        const chosen = reactionSprites[state.tapNonce % reactionSprites.length];
        state.spriteKey = chosen;
        state.speech = pickNonRepeating(WARD_TAP_LINES, state.speech);
        ui.wardDoctorPose = chosen === 'POSE_SIP' ? 'SIPPING' : chosen === 'POSE_APPROVE' ? 'APPROVE' : 'OBSERVE';
        renderDoctorSprite();
        docSpriteWrap.style.transform = `scale(${state.facingRight ? -1.14 : 1.14}, 1.14)`;
        setTimeout(() => {
          if (docSpriteWrap) updateDoctorVisual(0);
        }, 170);
        updateDoctorVisual(0);
      }
      activePointerId = null;
    }

    docHitbox.addEventListener('pointerup', onDocPointerEnd);
    docHitbox.addEventListener('pointercancel', onDocPointerEnd);

    // Stride & Blink Sub-loop
    const strideTimer = setInterval(() => {
      if (state.isDraggingDoctor) {
        if (state.spriteKey !== 'TURN') {
          state.spriteKey = 'TURN';
          renderDoctorSprite();
        }
        return;
      }
      if (state.isTurning) {
        if (state.spriteKey !== 'TURN') {
          state.spriteKey = 'TURN';
          renderDoctorSprite();
        }
        return;
      }
      if (state.isWalking) {
        const pattern = ['WALK_A', 'WALK_B'];
        state.walkPhase = (state.walkPhase + 1) % pattern.length;
        state.spriteKey = pattern[state.walkPhase];
        renderDoctorSprite();
        return;
      }
      if (!state.interactingId && Date.now() >= state.tapReactUntil) {
        state.tick += 1;
        const nextIdle = state.tick % 9 === 0 ? 'IDLE_B' : 'IDLE_A';
        if (state.spriteKey !== nextIdle) {
          state.spriteKey = nextIdle;
          renderDoctorSprite();
        }
      }
    }, 220);

    // Autonomous Patrol Loop
    const builtInSpots = [
      { x: 0.39, y: 0.77, id: 'default_iv', lookX: 0.22 },
      { x: 0.47, y: 0.75, id: 'default_bed', lookX: 0.30 },
      { x: 0.60, y: 0.60, id: 'default_cabinet', lookX: 0.59 },
      { x: 0.71, y: 0.71, id: 'default_chair', lookX: 0.83 },
      { x: 0.56, y: 0.73, id: null, lookX: 0.56 },
      { x: 0.65, y: 0.65, id: null, lookX: 0.65 }
    ];

    let stepCount = 0;
    let disposed = false;
    let patrolTimeout = null;

    async function waitMs(ms) {
      return new Promise((resolve) => {
        patrolTimeout = setTimeout(resolve, ms);
      });
    }

    async function patrolLoop() {
      while (!disposed) {
        await waitMs(2800);
        if (disposed) return;
        stepCount += 1;
        if (state.isFingerDown || state.isDraggingDoctor || Date.now() < state.tapReactUntil || Date.now() < state.playerHoldUntil) {
          continue;
        }

        state.interactingId = null;
        renderProps();

        const placedTargets = placements.map((p) => {
          const [fx, fy] = clampToWalkableFloor(p.x, p.y);
          return { x: fx, y: fy, id: p.itemId, lookX: p.x };
        });

        const pool = placedTargets.length > 0 && stepCount % 2 === 1 ? placedTargets : builtInSpots;
        const candidates = pool.filter((s) => Math.abs(s.x - state.docX) + Math.abs(s.y - state.docY) > 0.06);
        const rawChosen = candidates.length > 0
          ? candidates[Math.floor(Math.random() * candidates.length)]
          : pool[stepCount % pool.length];
        const [safeX, safeY] = clampToWalkableFloor(rawChosen.x, rawChosen.y);
        const dx = safeX - state.docX;
        const wantsRight = Math.abs(dx) > 0.015 ? dx > 0 : state.facingRight;

        if (wantsRight !== state.facingRight) {
          state.isTurning = true;
          renderDoctorSprite();
          await waitMs(240);
          if (disposed) return;
          if (!state.isFingerDown && !state.isDraggingDoctor) {
            state.facingRight = wantsRight;
          }
          state.isTurning = false;
        }

        if (state.isFingerDown || state.isDraggingDoctor || Date.now() < state.playerHoldUntil) continue;

        const dist = Math.abs(safeX - state.docX) + Math.abs(safeY - state.docY);
        const durMs = Math.min(1950, Math.max(950, Math.round(dist * 4200)));
        state.isWalking = true;
        state.docX = safeX;
        state.docY = safeY;
        updateDoctorVisual(durMs);
        await waitMs(durMs + 80);
        if (disposed) return;
        state.isWalking = false;

        if (state.isFingerDown || state.isDraggingDoctor || Date.now() < state.tapReactUntil || Date.now() < state.playerHoldUntil) continue;

        if (rawChosen.id) {
          const lookRight = rawChosen.lookX > state.docX + 0.02;
          const lookLeft = rawChosen.lookX < state.docX - 0.02;
          if ((lookRight && !state.facingRight) || (lookLeft && state.facingRight)) {
            state.facingRight = lookRight;
          }
          state.interactingId = rawChosen.id;
          state.spriteKey = (rawChosen.id === 'boba_iv_drip' || rawChosen.id === 'long_straw')
            ? 'POSE_SIP'
            : (rawChosen.id === 'miracle_banner' || rawChosen.id === 'vip_deed')
              ? 'POSE_APPROVE'
              : 'INTERACT';
          state.speech = wardItemInteractionLine(rawChosen.id);
          ui.wardDoctorPose = state.spriteKey === 'POSE_SIP' ? 'SIPPING' : state.spriteKey === 'POSE_APPROVE' ? 'APPROVE' : 'OBSERVE';
          renderDoctorSprite();
          renderProps();
          updateDoctorVisual(0);
          await waitMs(2600);
          if (disposed) return;
          state.interactingId = null;
          renderProps();
        } else {
          state.spriteKey = 'IDLE_A';
          state.speech = pickNonRepeating(WARD_WANDER_LINES, state.speech);
          ui.wardDoctorPose = 'OBSERVE';
          renderDoctorSprite();
          updateDoctorVisual(0);
        }
      }
    }

    renderAnchors();
    renderProps();
    updateDoctorVisual(0);
    patrolLoop();

    activeWardCleanup = () => {
      disposed = true;
      clearInterval(strideTimer);
      clearTimeout(patrolTimeout);
      clearTimeout(longPressTimer);
    };
  }

  // -------------------- Business Logic Actions --------------------
  function patFeifei() {
    ui.patCounter += 1;
    ui.feifeiQuote = pickNonRepeating(FEIFEI_QUOTES, ui.feifeiQuote);
    ui.stagePose = DOCTOR_POSE_ORDER[ui.patCounter % DOCTOR_POSE_ORDER.length];
    ui.equipReactionQuote = null;
  }

  function toggleTask(taskId) {
    const today = todayDateKey();
    const t = db.tasks.find((x) => x.id === taskId);
    if (!t) return;
    t.completed = !t.completed;
    t.completedDate = t.completed ? today : '';
    if (db.profile.todayDate !== today) {
      db.profile.todayDate = today;
      db.profile.todayCount = 0;
    }
    if (t.completed) {
      db.profile.todayCount = Math.min(999, (db.profile.todayCount || 0) + 1);
      db.profile.totalCompleted = (db.profile.totalCompleted || 0) + 1;
      ui.feifeiQuote = '病歷紀錄：患者居然真的完成了一件事，本醫師面無表情地表示肯定。';
      ui.stagePose = 'APPROVE';
      showToast(`完成「${t.title}」！真是醫學小奇蹟`);
    } else {
      db.profile.todayCount = Math.max(0, (db.profile.todayCount || 0) - 1);
      db.profile.totalCompleted = Math.max(0, (db.profile.totalCompleted || 0) - 1);
    }
    commitDb();
    renderCurrentTab();
  }

  function addTask(title, effortLevel, note) {
    const clean = (title || '').trim().slice(0, 100);
    if (!clean) {
      showToast('請輸入一個小小任務的名稱喔');
      return;
    }
    db.tasks.unshift({
      id: uid(),
      title: clean,
      effortLevel: effortLevel || 'micro',
      completed: false,
      completedDate: '',
      note: (note || '').trim().slice(0, 200),
      createdAt: Date.now()
    });
    commitDb();
    closeModal();
    showToast(`已加入復健療程：「${clean.slice(0, 16)}」`);
    renderCurrentTab();
  }

  function editTask(taskId, newTitle, newNote) {
    const clean = (newTitle || '').trim().slice(0, 100);
    if (!clean) return;
    const t = db.tasks.find((x) => x.id === taskId);
    if (!t) return;
    t.title = clean;
    t.note = (newNote || '').trim().slice(0, 200);
    commitDb();
    closeModal();
    showToast(`已更新「${clean.slice(0, 16)}」`);
    renderCurrentTab();
  }

  function shrinkTask(taskId, newTitle, newNote) {
    const clean = (newTitle || '').trim().slice(0, 100);
    if (!clean) return;
    const t = db.tasks.find((x) => x.id === taskId);
    if (!t) return;
    t.title = clean;
    t.effortLevel = 'micro';
    t.note = (newNote || '').trim().slice(0, 200);
    commitDb();
    closeModal();
    showToast('成功把大魔王切小塊了！這樣輕鬆很多對吧～');
    renderCurrentTab();
  }

  function deleteTask(taskId) {
    db.tasks = db.tasks.filter((x) => x.id !== taskId);
    commitDb();
    showToast('沒關係，這個先放過自己吧！');
    renderCurrentTab();
  }

  function acceptPrescription(rx) {
    db.tasks.unshift({
      id: uid(),
      title: rx.title,
      effortLevel: rx.effortLevel,
      completed: false,
      completedDate: '',
      note: rx.doctorNote,
      createdAt: Date.now()
    });
    commitDb();
    ui.selectedTab = 'room';
    showToast(`已將療程「${rx.title}」加入康復室！`);
    renderAppChrome();
    renderCurrentTab();
  }

  function redeemPharmacyItem(itemId) {
    const item = PHARMACY_ITEMS.find((it) => it.id === itemId);
    if (!item) return;
    if (db.profile.ownedItemIds.includes(item.id)) return;
    db.profile.ownedItemIds.push(item.id);
    if (!db.profile.equippedItemIds.includes(item.id)) {
      db.profile.equippedItemIds.push(item.id);
    }
    const placements = resolveWardPlacements(db.profile).filter((p) => p.itemId !== item.id);
    const [dx, dy] = defaultWardPosition(item.id);
    placements.push({ itemId: item.id, x: dx, y: dy });
    db.profile.wardPlacements = placements;

    ui.pharmacyPose = 'APPROVE';
    ui.pharmacyQuote = `「${item.name}」已免費領取，順便幫你擺進病房了。`;
    commitDb();
    showToast(`已領取「${item.name}」！同時擺進你的病房囉`);
    renderCurrentTab();
  }

  function toggleEquipItem(itemId, fromWardDrawer = false) {
    const item = PHARMACY_ITEMS.find((it) => it.id === itemId);
    if (!item) return;
    if (!db.profile.ownedItemIds.includes(itemId)) {
      showToast('需要先在院內藥局領取此道具喔！');
      return;
    }
    const placements = resolveWardPlacements(db.profile);
    const isInWard = db.profile.equippedItemIds.includes(itemId) || placements.some((p) => p.itemId === itemId);
    if (isInWard) {
      db.profile.equippedItemIds = db.profile.equippedItemIds.filter((id) => id !== itemId);
      db.profile.wardPlacements = placements.filter((p) => p.itemId !== itemId);
      if (ui.activePlacingItemId === itemId) ui.activePlacingItemId = null;
      ui.pharmacyPose = 'PHARMACY';
      showToast(fromWardDrawer ? `已將「${item.name}」收進病房收納箱` : `已從病床收回「${item.name}」`);
    } else {
      db.profile.equippedItemIds.push(itemId);
      const [dx, dy] = defaultWardPosition(itemId);
      db.profile.wardPlacements = [...placements.filter((p) => p.itemId !== itemId), { itemId, x: dx, y: dy }];
      ui.pharmacyPose = 'APPROVE';
      showToast(fromWardDrawer ? `已把「${item.name}」放進病房囉！` : `已將「${item.name}」擺上病床！`);
    }
    commitDb();
    renderCurrentTab();
  }

  function moveWardItem(itemId, x, y) {
    if (!db.profile.ownedItemIds.includes(itemId)) return;
    const placements = resolveWardPlacements(db.profile).filter((p) => p.itemId !== itemId);
    placements.push({ itemId, x, y });
    db.profile.wardPlacements = placements;
    if (!db.profile.equippedItemIds.includes(itemId)) {
      db.profile.equippedItemIds.push(itemId);
    }
    commitDb();
    renderCurrentTab();
  }

  function saveTodayCheckin() {
    const today = todayDateKey();
    const pct = Math.round(ui.batteryPercent);
    const energyLevel = pct < 25 ? 'flat' : pct < 60 ? 'low' : 'steady';
    const placements = resolveWardPlacements(db.profile);
    const cleanText = (ui.reflectionDraft || '').trim().slice(0, 300);

    const nextEntry = {
      id: today,
      dateKey: today,
      energyLevel,
      batteryPercent: pct,
      reflection: cleanText,
      wardSnapshot: placements,
      doctorPoseSnapshot: ui.wardDoctorPose,
      updatedAt: Date.now()
    };
    db.checkins = [nextEntry, ...db.checkins.filter((c) => c.dateKey !== today)]
      .sort((a, b) => b.dateKey.localeCompare(a.dateKey));
    commitDb();
    showToast('已儲存今天的日記與病房樣貌！');
    renderCurrentTab();
  }

  // -------------------- Screen Renderers --------------------
  function renderRoomTab(container) {
    const today = todayDateKey();
    const todayCount = db.profile.todayDate === today ? (db.profile.todayCount || 0) : 0;
    const totalCompleted = db.profile.totalCompleted || 0;
    const stage = stageFromTodayCount(todayCount);
    const pose = DOCTOR_POSES[ui.stagePose] || DOCTOR_POSES.OBSERVE;
    const nickname = db.profile.feifeiNickname || '主治醫師';

    const equippedItems = PHARMACY_ITEMS.filter((it) => db.profile.equippedItemIds.includes(it.id));
    const hasBanner = equippedItems.some((it) => it.id === 'miracle_banner');
    const hasBlanket = equippedItems.some((it) => it.id === 'antisocial_blanket');
    const hasPillow = equippedItems.some((it) => it.id === 'antigravity_pillow');
    const sideBadges = equippedItems.filter((it) => it.id !== 'miracle_banner');
    const leftBadges = sideBadges.filter((_, i) => i % 2 === 0).slice(0, 3);
    const rightBadges = sideBadges.filter((_, i) => i % 2 === 1).slice(0, 3);
    const overflowBadges = sideBadges.slice(6);

    const renderBadge = (it) => `
      <span class="equipped-badge">
        <img src="${it.img}" alt="${escapeHtml(it.name)}" />
        <span>${escapeHtml(it.shortLabel)}</span>
      </span>
    `;

    const sortedTasks = [...db.tasks].sort((a, b) => {
      if (a.completed !== b.completed) return a.completed ? 1 : -1;
      return (b.createdAt || 0) - (a.createdAt || 0);
    });

    const tasksHtml = sortedTasks.length === 0
      ? `
        <div class="card card-soft" style="padding:28px;text-align:center;display:flex;flex-direction:column;align-items:center;gap:10px;">
          <span style="color:var(--primary);width:36px;height:36px;display:inline-flex;">${ICONS.spa}</span>
          <div style="font-size:16px;font-weight:700;">目前沒有待辦壓力喔！</div>
          <div style="font-size:14px;color:var(--on-surface-variant);">想做點什麼小事的話，可以新增一個超微任務，或是去「復健療程」隨緣抽一張。</div>
          <button type="button" class="btn btn-outline" id="btn-empty-add" style="margin-top:4px;">新增一件超級小事</button>
        </div>
      `
      : sortedTasks.map((task) => {
        const menuOpen = ui.openMenuTaskId === task.id;
        return `
          <div class="task-row ${task.completed ? 'completed' : ''}">
            <button type="button" class="task-toggle-btn" data-toggle-task="${task.id}" aria-label="切換完成狀態 ${escapeHtml(task.title)}">
              <span class="task-circle">${task.completed ? ICONS.check : ''}</span>
            </button>
            <button type="button" class="task-main-click" data-toggle-task="${task.id}">
              <span class="task-title">${escapeHtml(task.title)}</span>
              ${task.note ? `<span class="task-note-preview">${escapeHtml(task.note)}</span>` : ''}
            </button>
            <div class="task-menu-wrap">
              <button type="button" class="icon-btn" data-menu-btn="${task.id}" aria-label="更多操作">
                ${ICONS.moreVert}
              </button>
              ${menuOpen ? `
                <div class="task-dropdown">
                  <button type="button" class="dropdown-item" data-edit-task="${task.id}">
                    <span style="color:var(--primary);display:inline-flex;">${ICONS.edit}</span>
                    <span>編輯這個項目</span>
                  </button>
                  ${!task.completed ? `
                    <button type="button" class="dropdown-item" data-shrink-task="${task.id}">
                      <span style="color:var(--primary);display:inline-flex;">${ICONS.wand}</span>
                      <span>太難了，切小塊</span>
                    </button>
                  ` : ''}
                  <button type="button" class="dropdown-item" data-delete-task="${task.id}">
                    <span style="color:var(--secondary);display:inline-flex;">${ICONS.trash}</span>
                    <span>放下這個任務</span>
                  </button>
                </div>
              ` : ''}
            </div>
          </div>
        `;
      }).join('');

    container.className = 'content-container';
    container.innerHTML = `
      <section class="card card-lg stage-card">
        <div class="stage-top-row">
          <span class="pill pill-primary">${stage.icon}<span>${escapeHtml(nickname)}</span></span>
          <button type="button" class="pill pill-secondary pill-clickable" id="btn-stage-pharmacy">
            ${ICONS.pharmacy}<span>院內藥局</span>
          </button>
        </div>

        <button type="button" class="stage-viewport" id="btn-stage-doctor" aria-label="點擊醫師更換配件與互動">
          <div class="stage-halo ${stage.glow ? 'glow' : ''}"></div>
          <div class="stage-bed ${hasBlanket ? 'blanket-equipped' : ''}">
            <div class="stage-pillow ${hasPillow ? 'pillow-equipped' : ''}"></div>
          </div>
          ${hasBanner ? `<div class="stage-miracle-banner">🚩 醫學奇蹟「居然活著」</div>` : ''}
          ${leftBadges.length ? `<div class="stage-side-badges left ${hasBanner ? 'has-banner' : ''}">${leftBadges.map(renderBadge).join('')}</div>` : ''}
          ${rightBadges.length ? `<div class="stage-side-badges right ${hasBanner ? 'has-banner' : ''}">${rightBadges.map(renderBadge).join('')}</div>` : ''}
          <div class="stage-doctor-wrap ${hasBanner ? 'has-banner' : ''}" id="stage-doctor-sprite">
            ${renderDoctorAvatarHtml(pose.img, db.gear, '174px', `${nickname}目前穿搭`)}
          </div>
          <span class="stage-hint-chip">${ICONS.checkroom}<span>點醫師換配件</span></span>
        </button>

        ${overflowBadges.length ? `<div class="overflow-badges">${overflowBadges.map(renderBadge).join('')}</div>` : ''}

        <button type="button" class="quote-box" id="btn-stage-quote">
          <span class="quote-main">「${escapeHtml(ui.feifeiQuote)}」</span>
          <span class="quote-sub">${escapeHtml(stage.subtitleZh)}（今日 ${todayCount} 件・累計 ${totalCompleted} 件）</span>
        </button>
      </section>

      <div class="section-header-row">
        <h2 class="section-title">今日復健療程</h2>
        <span class="section-hint">點圓圈完成・點 ⋮ 更多</span>
      </div>

      ${tasksHtml}
    `;

    document.getElementById('btn-stage-pharmacy')?.addEventListener('click', () => switchTab('pharmacy'));
    document.getElementById('btn-stage-doctor')?.addEventListener('click', () => {
      patFeifei();
      openModal({ type: 'doctorEquip' });
      renderCurrentTab();
    });
    document.getElementById('btn-stage-quote')?.addEventListener('click', () => {
      patFeifei();
      renderCurrentTab();
    });
    document.getElementById('btn-empty-add')?.addEventListener('click', () => openModal({ type: 'addTask' }));

    container.querySelectorAll('[data-toggle-task]').forEach((el) => {
      el.addEventListener('click', () => {
        ui.openMenuTaskId = null;
        toggleTask(el.getAttribute('data-toggle-task'));
      });
    });
    container.querySelectorAll('[data-menu-btn]').forEach((el) => {
      el.addEventListener('click', (e) => {
        e.stopPropagation();
        const id = el.getAttribute('data-menu-btn');
        ui.openMenuTaskId = ui.openMenuTaskId === id ? null : id;
        renderCurrentTab();
      });
    });
    container.querySelectorAll('[data-edit-task]').forEach((el) => {
      el.addEventListener('click', (e) => {
        e.stopPropagation();
        const task = db.tasks.find((t) => t.id === el.getAttribute('data-edit-task'));
        ui.openMenuTaskId = null;
        if (task) openModal({ type: 'editTask', task });
      });
    });
    container.querySelectorAll('[data-shrink-task]').forEach((el) => {
      el.addEventListener('click', (e) => {
        e.stopPropagation();
        const task = db.tasks.find((t) => t.id === el.getAttribute('data-shrink-task'));
        ui.openMenuTaskId = null;
        if (task) openModal({ type: 'shrinkTask', task });
      });
    });
    container.querySelectorAll('[data-delete-task]').forEach((el) => {
      el.addEventListener('click', (e) => {
        e.stopPropagation();
        ui.openMenuTaskId = null;
        deleteTask(el.getAttribute('data-delete-task'));
      });
    });
  }

  function renderPrescriptionsTab(container) {
    const nickname = db.profile.feifeiNickname || '主治醫師';
    const currentEnergy = ENERGY_LEVELS[ui.selectedEnergyFilter];
    const filtered = PRESCRIPTIONS.filter((p) => p.energyLevel === ui.selectedEnergyFilter);

    container.className = 'content-container';
    container.innerHTML = `
      ${renderDoctorBannerHtml({
        doctorName: nickname,
        pose: ui.prescriptionPose,
        quote: ui.prescriptionQuote,
        action: 'pat-rx'
      })}

      <section class="card card-soft" style="padding:20px;display:flex;flex-direction:column;gap:12px;">
        <div style="display:flex;align-items:center;gap:8px;">
          <span style="color:var(--primary);display:inline-flex;width:24px;height:24px;">${ICONS.hospital}</span>
          <h2 style="margin:0;font-size:21px;font-weight:700;">懶癌復健療程</h2>
        </div>
        <div style="font-size:14px;color:var(--on-surface-variant);">先感覺一下自己現在剩多少電量，讓康復室幫你挑一個剛剛好、不會累的復健療程。</div>
        <div class="chip-row">
          ${Object.values(ENERGY_LEVELS).map((lvl) => `
            <button type="button" class="filter-chip flex1 ${ui.selectedEnergyFilter === lvl.id ? 'selected' : ''}" data-energy="${lvl.id}">
              ${lvl.icon}<span>${lvl.shortTag}</span>
            </button>
          `).join('')}
        </div>
        <div style="font-size:13px;font-weight:700;color:var(--primary);">${escapeHtml(currentEnergy.descriptionZh)}</div>
        <button type="button" class="btn btn-primary btn-full" id="btn-draw-rx">
          ${ICONS.casino}<span>選擇困難？幫我隨緣抽一張</span>
        </button>
      </section>

      ${ui.drawnPrescription ? `
        <section class="card" style="background:var(--primary-container);border:1.5px solid var(--primary);padding:18px;display:flex;flex-direction:column;gap:10px;">
          <div style="font-size:13px;font-weight:700;color:var(--primary);">今日隨緣復健療程</div>
          <div style="font-size:20px;font-weight:700;color:var(--on-primary-container);">${escapeHtml(ui.drawnPrescription.title)}</div>
          <div style="font-size:14px;color:var(--on-primary-container);opacity:0.88;">療程叮嚀：${escapeHtml(ui.drawnPrescription.doctorNote)}</div>
          <button type="button" class="btn btn-primary btn-full" id="btn-accept-drawn">
            ${ICONS.addTask}<span>就決定是你了！加入康復室</span>
          </button>
        </section>
      ` : ''}

      ${filtered.map((rx) => {
        const eff = EFFORT_LEVELS[rx.effortLevel];
        return `
          <article class="card" style="padding:18px;display:flex;flex-direction:column;gap:8px;">
            <div>
              <span class="pill" style="background:var(--surface-container);color:var(--on-surface-variant);">${eff.labelZh}・${eff.badgeHint}</span>
            </div>
            <div style="font-size:16px;font-weight:700;">${escapeHtml(rx.title)}</div>
            <div style="font-size:14px;color:var(--on-surface-variant);">${escapeHtml(rx.doctorNote)}</div>
            <button type="button" class="btn btn-outline btn-full" data-add-rx="${rx.id}" style="margin-top:4px;">
              ${ICONS.addTask}<span>加入今日康復</span>
            </button>
          </article>
        `;
      }).join('')}
    `;

    container.querySelector('[data-action="pat-rx"]')?.addEventListener('click', () => {
      ui.prescriptionPatCounter += 1;
      ui.prescriptionPose = pickNonRepeating(DOCTOR_POSE_ORDER, ui.prescriptionPose);
      ui.prescriptionQuote = pickNonRepeating(PRESCRIPTION_QUOTES, ui.prescriptionQuote);
      renderCurrentTab();
    });
    container.querySelectorAll('[data-energy]').forEach((btn) => {
      btn.addEventListener('click', () => {
        ui.selectedEnergyFilter = btn.getAttribute('data-energy');
        ui.drawnPrescription = null;
        renderCurrentTab();
      });
    });
    document.getElementById('btn-draw-rx')?.addEventListener('click', () => {
      ui.drawnPrescription = filtered[Math.floor(Math.random() * filtered.length)] || PRESCRIPTIONS[0];
      renderCurrentTab();
    });
    document.getElementById('btn-accept-drawn')?.addEventListener('click', () => {
      if (ui.drawnPrescription) acceptPrescription(ui.drawnPrescription);
    });
    container.querySelectorAll('[data-add-rx]').forEach((btn) => {
      btn.addEventListener('click', () => {
        const rx = PRESCRIPTIONS.find((p) => p.id === btn.getAttribute('data-add-rx'));
        if (rx) acceptPrescription(rx);
      });
    });
  }

  function renderPharmacyTab(container) {
    const nickname = db.profile.feifeiNickname || '主治醫師';
    const ownedSet = new Set(db.profile.ownedItemIds || []);
    const equippedSet = new Set(db.profile.equippedItemIds || []);
    const pose = DOCTOR_POSES[ui.pharmacyPose] || DOCTOR_POSES.PHARMACY;

    container.className = 'content-container';
    container.innerHTML = `
      <section class="card" style="padding:18px;display:flex;flex-direction:column;gap:12px;">
        <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;flex-wrap:wrap;">
          <span class="pill pill-primary">${ICONS.pharmacy}<span>院內免費道具領取處</span></span>
          <span class="pill pill-secondary">${ICONS.sparkle}<span>圖鑑 ${ownedSet.size} / ${PHARMACY_ITEMS.length}</span></span>
        </div>

        <button type="button" class="quote-box" id="btn-pharmacy-doctor" style="flex-direction:row;align-items:center;gap:12px;padding:14px;">
          <div style="width:104px;height:108px;display:flex;align-items:center;justify-content:center;flex-shrink:0;">
            ${renderDoctorAvatarHtml(pose.img, db.gear, '106px', `${nickname}藥局動作`)}
          </div>
          <div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:6px;">
            <div><span class="pill pill-white" style="color:var(--primary);">${escapeHtml(nickname)}</span></div>
            <div style="font-size:14px;font-weight:600;color:var(--on-surface);">「${escapeHtml(ui.pharmacyQuote)}」</div>
            <div style="display:inline-flex;align-items:center;gap:4px;font-size:11px;color:var(--on-surface-variant);">${ICONS.touch}<span>點一下醫師切換動作</span></div>
          </div>
        </button>
      </section>

      ${PHARMACY_CATEGORIES.map((cat) => {
        const catItems = PHARMACY_ITEMS.filter((it) => it.category === cat.id);
        return `
          <h2 class="section-title" style="padding:6px 4px 0;">${cat.titleZh}</h2>
          ${catItems.map((item) => {
            const isOwned = ownedSet.has(item.id);
            const isEquipped = equippedSet.has(item.id);
            return `
              <article class="card pharmacy-item-card ${isEquipped ? 'equipped' : ''}">
                <div style="display:flex;align-items:center;gap:12px;">
                  <div class="pharmacy-thumb ${isOwned ? 'owned' : ''}">
                    <img src="${item.img}" alt="${escapeHtml(item.name)}" />
                  </div>
                  <div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:5px;">
                    <div style="font-size:16px;font-weight:700;">${escapeHtml(item.name)}</div>
                    <div>
                      ${isOwned
                        ? `<span class="pill pill-tertiary">${ICONS.checkCircle}<span>已擁有</span></span>`
                        : `<span class="pill pill-secondary">免費領取</span>`}
                    </div>
                  </div>
                </div>
                <div style="font-size:14px;color:var(--on-surface-variant);">${escapeHtml(item.description)}</div>
                ${isOwned
                  ? (isEquipped
                    ? `<button type="button" class="btn btn-primary btn-full" data-equip-item="${item.id}">${ICONS.bed}<span>已擺進病房（點此收回）</span></button>`
                    : `<button type="button" class="btn btn-outline btn-full" data-equip-item="${item.id}">${ICONS.bed}<span>擺進病房／病床旁</span></button>`)
                  : `<button type="button" class="btn btn-primary btn-full" data-redeem-item="${item.id}">領取並擺進病房</button>`}
              </article>
            `;
          }).join('')}
        `;
      }).join('')}
    `;

    document.getElementById('btn-pharmacy-doctor')?.addEventListener('click', () => {
      ui.pharmacyPatCounter += 1;
      ui.pharmacyPose = pickNonRepeating(DOCTOR_POSE_ORDER, ui.pharmacyPose);
      ui.pharmacyQuote = pickNonRepeating(PHARMACY_QUOTES, ui.pharmacyQuote);
      renderCurrentTab();
    });
    container.querySelectorAll('[data-redeem-item]').forEach((btn) => {
      btn.addEventListener('click', () => redeemPharmacyItem(btn.getAttribute('data-redeem-item')));
    });
    container.querySelectorAll('[data-equip-item]').forEach((btn) => {
      btn.addEventListener('click', () => toggleEquipItem(btn.getAttribute('data-equip-item'), false));
    });
  }

  function renderMemoriesTab(container) {
    const nickname = db.profile.feifeiNickname || '主治醫師';
    const completedTasks = db.tasks.filter((t) => t.completed);
    const totalCompleted = Math.max(db.profile.totalCompleted || 0, completedTasks.length);

    const tasksByDate = {};
    completedTasks.forEach((t) => {
      if (!t.completedDate) return;
      if (!tasksByDate[t.completedDate]) tasksByDate[t.completedDate] = [];
      tasksByDate[t.completedDate].push(t);
    });
    const checkinsByDate = {};
    db.checkins.forEach((c) => { checkinsByDate[c.dateKey] = c; });

    const allDates = Array.from(new Set([...Object.keys(tasksByDate), ...Object.keys(checkinsByDate)]))
      .sort((a, b) => b.localeCompare(a));

    container.className = 'content-container';
    container.innerHTML = `
      ${renderDoctorBannerHtml({
        doctorName: nickname,
        pose: ui.memoryPose,
        quote: ui.memoryQuote,
        subtitle: `住院觀察紀錄簿・累計完成 ${totalCompleted} 項復健・共 ${allDates.length} 天存活證明`,
        action: 'pat-memories'
      })}

      <section class="card card-soft" style="padding:18px;display:flex;flex-direction:column;gap:12px;">
        <div style="display:flex;align-items:center;gap:8px;">
          <span style="color:var(--primary);display:inline-flex;width:20px;height:20px;">${ICONS.trophy}</span>
          <h2 style="margin:0;font-size:16px;font-weight:700;">院內榮譽貼紙牆（只增不減）</h2>
        </div>
        <div class="milestone-grid">
          ${MILESTONES.map((m) => {
            const unlocked = totalCompleted >= m.threshold;
            return `
              <div class="milestone-pill ${unlocked ? 'unlocked' : ''}">
                <span style="color:${unlocked ? 'var(--secondary)' : 'rgba(125,99,95,0.45)'};display:inline-flex;">
                  ${unlocked ? ICONS.star : ICONS.lockOpen}
                </span>
                <div>
                  <div style="font-size:13px;font-weight:700;color:${unlocked ? 'var(--on-secondary-container)' : 'var(--on-surface-variant)'};">${m.title}</div>
                  <div style="font-size:11px;color:var(--on-surface-variant);">${m.subtitle}</div>
                </div>
              </div>
            `;
          }).join('')}
        </div>
      </section>

      <h2 class="section-title" style="padding:0 4px;">每日住院觀察病歷</h2>

      ${allDates.length === 0
        ? `
          <div class="card" style="padding:24px;text-align:center;display:flex;flex-direction:column;align-items:center;gap:8px;">
            <span style="color:var(--primary);width:32px;height:32px;display:inline-flex;">${ICONS.sparkle}</span>
            <div style="font-size:16px;font-weight:700;">這份病歷夾目前空白得像剛洗完的床單</div>
            <div style="font-size:14px;color:var(--on-surface-variant);">回康復室隨便勾一件喝水或發呆的小事，或去日記寫下今天，本院就會自動幫你蓋章存進這裡。</div>
          </div>
        `
        : allDates.map((dateKey) => {
          const dayTasks = tasksByDate[dateKey] || [];
          const ci = checkinsByDate[dateKey];
          const eMeta = ci ? (ENERGY_LEVELS[ci.energyLevel] || ENERGY_LEVELS.flat) : null;
          const hasSnap = ci && ((ci.wardSnapshot && ci.wardSnapshot.length > 0) || ci.doctorPoseSnapshot);
          return `
            <article class="card" style="padding:18px;display:flex;flex-direction:column;gap:10px;">
              <div style="display:flex;align-items:center;justify-content:space-between;">
                <span style="font-size:16px;font-weight:700;color:var(--primary);">${dateKey}</span>
                ${eMeta ? `<span class="pill pill-secondary">${eMeta.shortTag}</span>` : ''}
              </div>
              ${hasSnap ? `
                <div style="display:flex;flex-direction:column;gap:4px;">
                  <div style="display:inline-flex;align-items:center;gap:4px;font-size:11px;color:var(--on-surface-variant);">
                    <span style="color:var(--primary);width:14px;height:14px;display:inline-flex;">${ICONS.camera}</span>
                    <span>當日病房快照</span>
                  </div>
                  ${renderStaticWardRoomHtml(ci.wardSnapshot || [], ci.doctorPoseSnapshot || 'OBSERVE')}
                </div>
              ` : ''}
              ${ci && ci.reflection ? `
                <div class="card-soft" style="border-radius:16px;padding:10px 12px;display:flex;align-items:center;gap:8px;">
                  <span style="color:var(--secondary);width:16px;height:16px;display:inline-flex;flex-shrink:0;">${ICONS.heart}</span>
                  <span style="font-size:14px;">「${escapeHtml(ci.reflection)}」</span>
                </div>
              ` : ''}
              ${dayTasks.length > 0 ? `
                <div style="display:flex;flex-direction:column;gap:5px;">
                  ${dayTasks.map((t) => `
                    <div style="display:flex;align-items:center;gap:8px;font-size:14px;">
                      <span style="color:var(--primary);width:16px;height:16px;display:inline-flex;">${ICONS.checkCircle}</span>
                      <span>${escapeHtml(t.title)}</span>
                    </div>
                  `).join('')}
                </div>
              ` : ''}
            </article>
          `;
        }).join('')}
    `;

    container.querySelector('[data-action="pat-memories"]')?.addEventListener('click', () => {
      ui.memoryPatCounter += 1;
      ui.memoryPose = pickNonRepeating(DOCTOR_POSE_ORDER, ui.memoryPose);
      ui.memoryQuote = pickNonRepeating(MEMORY_QUOTES, ui.memoryQuote);
      renderCurrentTab();
    });
  }

  function renderLogTab(container) {
    const today = todayDateKey();
    if (ui.lastLoadedCheckinKey !== today) {
      syncDraftFromTodayCheckin();
    }
    const nickname = db.profile.feifeiNickname || '主治醫師';
    const placements = resolveWardPlacements(db.profile);
    const placedIds = new Set(placements.map((p) => p.itemId));
    const ownedItems = PHARMACY_ITEMS.filter((it) => (db.profile.ownedItemIds || []).includes(it.id));
    const completedTasks = db.tasks.filter((t) => t.completed);

    container.className = 'content-container full-bleed-log';

    const toggleHtml = `
      <div class="px-16">
        <div class="segmented-row">
          <button type="button" class="seg-btn ${ui.diaryMode === 'LIST' ? 'active' : ''}" id="btn-mode-list">
            ${ICONS.agenda}<span>列表</span>
          </button>
          <button type="button" class="seg-btn ${ui.diaryMode === 'CALENDAR' ? 'active' : ''}" id="btn-mode-cal">
            ${ICONS.calendar}<span>日曆</span>
          </button>
        </div>
      </div>
    `;

    if (ui.diaryMode === 'LIST') {
      const pct = Math.round(ui.batteryPercent);
      const energyLabel = pct < 25 ? '躺平充電中' : pct < 60 ? '微弱電力' : '暖暖有電';
      const selectedItem = PHARMACY_ITEMS.find((it) => it.id === ui.activePlacingItemId);

      const trayHtml = ownedItems.length === 0
        ? `
          <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;">
            <span style="font-size:12px;color:var(--on-surface-variant);">還沒有病房擺飾，去藥局領些可愛裝備吧！</span>
            <button type="button" class="btn btn-outline" id="btn-ward-go-pharmacy" style="height:36px;border-radius:999px;padding:0 12px;font-size:12px;">
              ${ICONS.pharmacy}<span>免費領道具</span>
            </button>
          </div>
        `
        : `
          <div style="display:flex;flex-direction:column;gap:8px;">
            <div style="font-size:12px;font-weight:600;color:var(--on-surface-variant);">
              ${selectedItem
                ? `已選「${escapeHtml(selectedItem.shortLabel)}」：點病房任一錨點移動位置，點 ✕ 收回`
                : '病房道具抽屜（點道具選取錨點擺放，點 ✕ 收回）'}
            </div>
            <div class="ward-tray-scroll">
              ${ownedItems.map((it) => {
                const isPlaced = placedIds.has(it.id);
                const isSel = ui.activePlacingItemId === it.id;
                return `
                  <div class="tray-chip ${isSel ? 'selected' : isPlaced ? 'placed' : ''}">
                    <button type="button" data-tray-item="${it.id}" style="border:none;background:transparent;display:inline-flex;align-items:center;gap:6px;padding:0;font-weight:700;font-size:12px;">
                      <img src="${it.img}" alt="" />
                      <span>${isSel ? '📍' : ''}${escapeHtml(it.shortLabel)}</span>
                    </button>
                    ${isPlaced ? `
                      <button type="button" class="tray-remove-x" data-tray-remove="${it.id}" aria-label="從病房收回 ${escapeHtml(it.name)}">
                        ${ICONS.close}
                      </button>
                    ` : ''}
                  </div>
                `;
              }).join('')}
            </div>
          </div>
        `;

      const entriesHtml = db.checkins.length === 0
        ? `
          <div class="px-16">
            <div class="card card-soft" style="padding:18px;font-size:14px;color:var(--on-surface-variant);">
              還沒有記錄，滑動上方電池或留幾行字就能存下今天囉。
            </div>
          </div>
        `
        : db.checkins.map((entry) => {
          const eMeta = ENERGY_LEVELS[entry.energyLevel] || ENERGY_LEVELS.flat;
          const dayTasks = completedTasks.filter((t) => t.completedDate === entry.dateKey);
          const hasSnap = (entry.wardSnapshot && entry.wardSnapshot.length > 0) || entry.doctorPoseSnapshot;
          return `
            <div class="px-16">
              <article class="card" style="padding:16px;display:flex;flex-direction:column;gap:10px;">
                <div style="display:flex;align-items:center;justify-content:space-between;">
                  <span style="font-size:14px;font-weight:700;color:var(--primary);">${entry.dateKey}</span>
                  <span class="pill pill-secondary">${eMeta.shortTag}</span>
                </div>
                ${hasSnap ? `
                  <div style="display:flex;flex-direction:column;gap:4px;">
                    <div style="display:inline-flex;align-items:center;gap:4px;font-size:11px;color:var(--on-surface-variant);">
                      <span style="color:var(--primary);width:14px;height:14px;display:inline-flex;">${ICONS.camera}</span>
                      <span>當日病房快照</span>
                    </div>
                    ${renderStaticWardRoomHtml(entry.wardSnapshot || [], entry.doctorPoseSnapshot || 'OBSERVE')}
                  </div>
                ` : ''}
                ${entry.reflection ? `<div style="font-size:14px;">${escapeHtml(entry.reflection)}</div>` : ''}
                ${dayTasks.length > 0 ? `
                  <div style="display:flex;flex-direction:column;gap:4px;">
                    ${dayTasks.map((t) => `
                      <div style="display:flex;align-items:center;gap:6px;font-size:14px;color:var(--on-surface-variant);">
                        <span style="color:var(--primary);width:15px;height:15px;display:inline-flex;">${ICONS.checkCircle}</span>
                        <span>${escapeHtml(t.title)}</span>
                      </div>
                    `).join('')}
                  </div>
                ` : ''}
              </article>
            </div>
          `;
        }).join('');

      container.innerHTML = `
        ${toggleHtml}

        <!-- Interactive Ward Decorator Section -->
        <section style="display:flex;flex-direction:column;gap:10px;">
          <div class="px-16" style="display:flex;align-items:center;justify-content:space-between;gap:8px;">
            <div style="min-width:0;flex:1;">
              <div style="font-size:16px;font-weight:700;">療癒病房佈置</div>
              <div style="font-size:11px;color:var(--on-surface-variant);">${escapeHtml(nickname)}巡房中・寫日記會同時記錄今天病房的長相</div>
            </div>
            <div style="display:flex;align-items:center;gap:6px;flex-shrink:0;">
              <button type="button" class="pill pill-clickable ${ui.showAnchors ? '' : 'pill-white'}" id="btn-toggle-anchors" style="${ui.showAnchors ? 'background:var(--primary);color:#fff;' : ''}">
                ${ICONS.pinEdit}<span>${ui.showAnchors ? '隱藏錨點' : '顯示錨點'}</span>
              </button>
              <span class="pill pill-primary">已擺 ${placements.length} 件</span>
            </div>
          </div>

          <div id="live-ward-mount"></div>

          <div class="px-16">
            <div class="card" style="padding:12px 14px;">${trayHtml}</div>
          </div>
        </section>

        <!-- Battery Slider + Reflection Card -->
        <div class="px-16">
          <section class="card" style="padding:20px;display:flex;flex-direction:column;gap:14px;">
            <div style="display:flex;align-items:center;justify-content:space-between;">
              <div style="display:flex;align-items:center;gap:6px;">
                <span id="battery-icon-wrap" style="color:var(--primary);width:20px;height:20px;display:inline-flex;">${ICONS.batteryFull}</span>
                <span id="battery-title-text" style="font-size:16px;font-weight:700;">今日電量・${pct}%</span>
              </div>
              <span class="pill pill-primary" id="battery-label-pill">${energyLabel}</span>
            </div>
            <div class="battery-track-box" id="battery-track-box">
              <canvas id="battery-canvas" class="battery-canvas"></canvas>
            </div>
            <div style="font-size:11px;color:var(--on-surface-variant);margin-top:-4px;">左右滑動電池調整今日電量</div>
            <textarea id="input-reflection" class="textarea-box" maxlength="300" placeholder="今天想留給自己的一句話、或只是記錄呼吸著的此刻...">${escapeHtml(ui.reflectionDraft)}</textarea>
            <button type="button" class="btn btn-primary btn-full" id="btn-save-checkin">儲存</button>
          </section>
        </div>

        <div class="px-16"><h2 class="section-title">日記列表</h2></div>
        ${entriesHtml}
      `;

      mountLiveWardRoom(document.getElementById('live-ward-mount'));
      mountBatteryCanvas();

      document.getElementById('btn-toggle-anchors')?.addEventListener('click', () => {
        ui.showAnchors = !ui.showAnchors;
        if (!ui.showAnchors) ui.activePlacingItemId = null;
        renderCurrentTab();
      });
      document.getElementById('btn-ward-go-pharmacy')?.addEventListener('click', () => switchTab('pharmacy'));
      container.querySelectorAll('[data-tray-item]').forEach((btn) => {
        btn.addEventListener('click', () => {
          const itemId = btn.getAttribute('data-tray-item');
          ui.showAnchors = true;
          if (!placedIds.has(itemId)) {
            const occupiedAnchors = new Set(placements.map((p) => nearestWardAnchor(p.x, p.y).id));
            const [dx, dy] = defaultWardPosition(itemId);
            const defAnchor = nearestWardAnchor(dx, dy);
            const target = !occupiedAnchors.has(defAnchor.id)
              ? defAnchor
              : (WARD_ANCHORS.find((a) => !occupiedAnchors.has(a.id)) || defAnchor);
            ui.activePlacingItemId = itemId;
            moveWardItem(itemId, target.x, target.y);
          } else {
            ui.activePlacingItemId = ui.activePlacingItemId === itemId ? null : itemId;
            renderCurrentTab();
          }
        });
      });
      container.querySelectorAll('[data-tray-remove]').forEach((btn) => {
        btn.addEventListener('click', (e) => {
          e.stopPropagation();
          toggleEquipItem(btn.getAttribute('data-tray-remove'), true);
        });
      });
      document.getElementById('input-reflection')?.addEventListener('input', (e) => {
        ui.reflectionDraft = e.target.value.slice(0, 300);
      });
      document.getElementById('btn-save-checkin')?.addEventListener('click', saveTodayCheckin);
    } else {
      // Calendar Mode
      const checkinSet = new Set(db.checkins.map((c) => c.dateKey));
      const taskDateSet = new Set(completedTasks.map((t) => t.completedDate).filter(Boolean));
      const daysInMonth = new Date(ui.calYear, ui.calMonth, 0).getDate();
      const firstOffset = new Date(ui.calYear, ui.calMonth - 1, 1).getDay();

      const cells = [];
      for (let i = 0; i < firstOffset; i++) cells.push('<div></div>');
      for (let d = 1; d <= daysInMonth; d++) {
        const key = `${ui.calYear}-${String(ui.calMonth).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
        const hasRec = checkinSet.has(key) || taskDateSet.has(key);
        const sel = key === ui.selectedCalDate;
        cells.push(`
          <button type="button" class="cal-cell ${hasRec ? 'has-record' : ''} ${sel ? 'selected' : ''}" data-cal-date="${key}">
            <span>${d}</span>
            ${hasRec ? '<span class="cal-dot"></span>' : ''}
          </button>
        `);
      }

      const dayEntry = db.checkins.find((c) => c.dateKey === ui.selectedCalDate);
      const dayTasks = completedTasks.filter((t) => t.completedDate === ui.selectedCalDate);
      const eMeta = dayEntry ? (ENERGY_LEVELS[dayEntry.energyLevel] || ENERGY_LEVELS.flat) : null;
      const hasSnap = dayEntry && ((dayEntry.wardSnapshot && dayEntry.wardSnapshot.length > 0) || dayEntry.doctorPoseSnapshot);

      container.innerHTML = `
        ${toggleHtml}

        <div class="px-16">
          <section class="card" style="padding:16px;display:flex;flex-direction:column;gap:12px;">
            <div class="cal-header">
              <button type="button" class="icon-btn" id="btn-prev-month" aria-label="上個月">${ICONS.chevLeft}</button>
              <span style="font-size:20px;font-weight:700;">${ui.calYear}年 ${ui.calMonth}月</span>
              <button type="button" class="icon-btn" id="btn-next-month" aria-label="下個月">${ICONS.chevRight}</button>
            </div>
            <div class="cal-weekdays">
              ${['日', '一', '二', '三', '四', '五', '六'].map((w) => `<div class="cal-weekday">${w}</div>`).join('')}
            </div>
            <div class="cal-grid">${cells.join('')}</div>
          </section>
        </div>

        <div class="px-16">
          <section class="card card-soft" style="padding:18px;display:flex;flex-direction:column;gap:10px;">
            <div style="display:flex;align-items:center;justify-content:space-between;">
              <span style="font-size:16px;font-weight:700;color:var(--primary);">${ui.selectedCalDate}</span>
              ${eMeta ? `<span class="pill pill-secondary">${eMeta.shortTag}</span>` : ''}
            </div>
            ${!dayEntry && dayTasks.length === 0
              ? '<div style="font-size:14px;color:var(--on-surface-variant);">這天沒有留下紀錄。</div>'
              : `
                ${hasSnap ? `
                  <div style="display:flex;flex-direction:column;gap:4px;">
                    <div style="display:inline-flex;align-items:center;gap:4px;font-size:11px;color:var(--on-surface-variant);">
                      <span style="color:var(--primary);width:14px;height:14px;display:inline-flex;">${ICONS.camera}</span>
                      <span>當日病房快照</span>
                    </div>
                    ${renderStaticWardRoomHtml(dayEntry.wardSnapshot || [], dayEntry.doctorPoseSnapshot || 'OBSERVE')}
                  </div>
                ` : ''}
                ${dayEntry && dayEntry.reflection ? `<div style="font-size:14px;">${escapeHtml(dayEntry.reflection)}</div>` : ''}
                ${dayTasks.map((t) => `
                  <div style="display:flex;align-items:center;gap:8px;font-size:14px;">
                    <span style="color:var(--primary);width:16px;height:16px;display:inline-flex;">${ICONS.checkCircle}</span>
                    <span>${escapeHtml(t.title)}</span>
                  </div>
                `).join('')}
              `}
          </section>
        </div>
      `;

      document.getElementById('btn-prev-month')?.addEventListener('click', () => {
        if (ui.calMonth === 1) { ui.calYear -= 1; ui.calMonth = 12; }
        else { ui.calMonth -= 1; }
        renderCurrentTab();
      });
      document.getElementById('btn-next-month')?.addEventListener('click', () => {
        if (ui.calMonth === 12) { ui.calYear += 1; ui.calMonth = 1; }
        else { ui.calMonth += 1; }
        renderCurrentTab();
      });
      container.querySelectorAll('[data-cal-date]').forEach((btn) => {
        btn.addEventListener('click', () => {
          ui.selectedCalDate = btn.getAttribute('data-cal-date');
          renderCurrentTab();
        });
      });
    }

    document.getElementById('btn-mode-list')?.addEventListener('click', () => {
      ui.diaryMode = 'LIST';
      renderCurrentTab();
    });
    document.getElementById('btn-mode-cal')?.addEventListener('click', () => {
      ui.diaryMode = 'CALENDAR';
      renderCurrentTab();
    });
  }

  function mountBatteryCanvas() {
    const trackBox = document.getElementById('battery-track-box');
    const canvas = document.getElementById('battery-canvas');
    if (!trackBox || !canvas) return;

    function draw() {
      const dpr = window.devicePixelRatio || 1;
      const w = trackBox.clientWidth || 300;
      const h = trackBox.clientHeight || 62;
      canvas.width = Math.round(w * dpr);
      canvas.height = Math.round(h * dpr);
      const ctx = canvas.getContext('2d');
      ctx.scale(dpr, dpr);
      ctx.clearRect(0, 0, w, h);

      const pct = Math.round(ui.batteryPercent);
      const fillCol = pct < 25 ? '#D99559' : pct < 60 ? 'rgba(234,135,151,0.82)' : '#EA8797';
      const capW = 10, capH = 22, gap = 4;
      const bodyW = w - capW - gap;
      const topY = 4, bodyH = h - 8, rad = 16;

      // Track background
      ctx.fillStyle = '#FFF1F4';
      ctx.beginPath();
      ctx.roundRect(0, topY, bodyW, bodyH, rad);
      ctx.fill();

      // Fill charge
      const pad = 5;
      const maxFill = Math.max(0, bodyW - pad * 2);
      const fillW = maxFill * (pct / 100);
      if (fillW > 2) {
        ctx.fillStyle = fillCol;
        ctx.beginPath();
        ctx.roundRect(pad, topY + pad, fillW, bodyH - pad * 2, Math.max(4, rad - 4));
        ctx.fill();
      }

      // Outline
      ctx.strokeStyle = 'rgba(67,46,43,0.65)';
      ctx.lineWidth = 2.5;
      ctx.beginPath();
      ctx.roundRect(0, topY, bodyW, bodyH, rad);
      ctx.stroke();

      // Positive terminal nub
      ctx.fillStyle = 'rgba(67,46,43,0.65)';
      ctx.beginPath();
      ctx.roundRect(bodyW + gap, (h - capH) / 2, capW, capH, 5);
      ctx.fill();

      const titleEl = document.getElementById('battery-title-text');
      const pillEl = document.getElementById('battery-label-pill');
      const iconWrap = document.getElementById('battery-icon-wrap');
      if (titleEl) titleEl.textContent = `今日電量・${pct}%`;
      if (pillEl) pillEl.textContent = pct < 25 ? '躺平充電中' : pct < 60 ? '微弱電力' : '暖暖有電';
      if (iconWrap) iconWrap.style.color = fillCol;
    }

    function updateFromPointer(clientX) {
      const rect = trackBox.getBoundingClientRect();
      const usable = Math.max(1, rect.width - 24);
      const ratio = Math.min(1, Math.max(0, (clientX - rect.left - 8) / usable));
      ui.batteryPercent = ratio * 100;
      draw();
    }

    trackBox.addEventListener('pointerdown', (e) => {
      trackBox.setPointerCapture(e.pointerId);
      updateFromPointer(e.clientX);
      const move = (ev) => updateFromPointer(ev.clientX);
      const up = () => {
        trackBox.removeEventListener('pointermove', move);
        trackBox.removeEventListener('pointerup', up);
        trackBox.removeEventListener('pointercancel', up);
      };
      trackBox.addEventListener('pointermove', move);
      trackBox.addEventListener('pointerup', up);
      trackBox.addEventListener('pointercancel', up);
    });

    draw();
  }

  // -------------------- Modals & Bottom Sheets --------------------
  function openModal(modal) {
    ui.modal = modal;
    renderModal();
  }

  function closeModal() {
    ui.modal = null;
    renderModal();
  }

  function renderModal() {
    const root = document.getElementById('modal-root');
    if (!ui.modal) {
      root.classList.add('hidden');
      root.classList.remove('center');
      root.innerHTML = '';
      root.setAttribute('aria-hidden', 'true');
      return;
    }

    root.classList.remove('hidden');
    root.setAttribute('aria-hidden', 'false');
    const isDialog = ui.modal.type === 'editTask' || ui.modal.type === 'shrinkTask';
    root.classList.toggle('center', isDialog);

    if (ui.modal.type === 'addTask') {
      let effort = 'micro';
      root.innerHTML = `
        <div class="sheet-panel" role="dialog" aria-modal="true">
          <div class="sheet-handle"></div>
          <h2 style="margin:0;font-size:21px;">新增一項復健療程</h2>
          <div style="font-size:14px;color:var(--on-surface-variant);">越小越好！哪怕只有「打開書本看一秒」也算康復成功喔。</div>
          <div class="field-group">
            <label class="field-label" for="add-task-title">今天想試著做哪件小事？</label>
            <input id="add-task-title" class="input-box" maxlength="100" placeholder="例如：喝一口水、把桌上的杯子拿去洗" />
          </div>
          <div class="field-group">
            <span class="field-label">康復室推薦（點一下直接帶入）</span>
            <div class="chip-row">
              ${['喝三口水', '站起來伸懶腰 10 秒', '丟掉桌上 1 個垃圾', '打開卡關文件只寫 1 行'].map((q) => `
                <button type="button" class="filter-chip" data-quick="${escapeHtml(q)}">${escapeHtml(q)}</button>
              `).join('')}
            </div>
          </div>
          <div class="field-group">
            <span class="field-label">這件事需要多少力氣？</span>
            <div class="chip-row">
              ${Object.values(EFFORT_LEVELS).map((ef) => `
                <button type="button" class="filter-chip flex1 ${ef.id === effort ? 'selected' : ''}" data-effort="${ef.id}">${ef.labelZh}</button>
              `).join('')}
            </div>
          </div>
          <div class="field-group">
            <label class="field-label" for="add-task-note">給自己的溫柔備註（選填）</label>
            <textarea id="add-task-note" class="textarea-box" style="min-height:66px;" maxlength="200" placeholder="做到一半停下來也沒關係喔"></textarea>
          </div>
          <div style="display:flex;gap:12px;">
            <button type="button" class="btn btn-outline-soft" style="flex:1;" id="btn-modal-cancel">先不用</button>
            <button type="button" class="btn btn-primary" style="flex:1.5;" id="btn-modal-confirm">加入今日復健</button>
          </div>
        </div>
      `;
      const titleInput = document.getElementById('add-task-title');
      titleInput.focus();
      root.querySelectorAll('[data-quick]').forEach((b) => {
        b.addEventListener('click', () => { titleInput.value = b.getAttribute('data-quick'); });
      });
      root.querySelectorAll('[data-effort]').forEach((b) => {
        b.addEventListener('click', () => {
          effort = b.getAttribute('data-effort');
          root.querySelectorAll('[data-effort]').forEach((x) => x.classList.toggle('selected', x === b));
        });
      });
      document.getElementById('btn-modal-cancel').addEventListener('click', closeModal);
      document.getElementById('btn-modal-confirm').addEventListener('click', () => {
        addTask(titleInput.value, effort, document.getElementById('add-task-note').value);
      });
    } else if (ui.modal.type === 'editTask') {
      const t = ui.modal.task;
      root.innerHTML = `
        <div class="dialog-panel" role="dialog" aria-modal="true">
          <div style="display:flex;align-items:center;gap:8px;color:var(--primary);">
            ${ICONS.edit}<h2 style="margin:0;font-size:19px;color:var(--on-surface);">編輯復健項目</h2>
          </div>
          <div class="field-group">
            <label class="field-label" for="edit-title">項目名稱</label>
            <input id="edit-title" class="input-box" maxlength="100" value="${escapeHtml(t.title)}" />
          </div>
          <div class="field-group">
            <label class="field-label" for="edit-note">溫柔備註（選填）</label>
            <textarea id="edit-note" class="textarea-box" style="min-height:70px;" maxlength="200">${escapeHtml(t.note)}</textarea>
          </div>
          <div style="display:flex;justify-content:flex-end;gap:8px;">
            <button type="button" class="btn btn-text" id="btn-modal-cancel">取消</button>
            <button type="button" class="btn btn-primary" id="btn-modal-confirm">儲存修改</button>
          </div>
        </div>
      `;
      document.getElementById('btn-modal-cancel').addEventListener('click', closeModal);
      document.getElementById('btn-modal-confirm').addEventListener('click', () => {
        editTask(t.id, document.getElementById('edit-title').value, document.getElementById('edit-note').value);
      });
    } else if (ui.modal.type === 'shrinkTask') {
      const t = ui.modal.task;
      const defaultShrunk = `先做 1 分鐘：${t.title}`.slice(0, 100);
      root.innerHTML = `
        <div class="dialog-panel" role="dialog" aria-modal="true">
          <div style="display:flex;align-items:center;gap:8px;color:var(--secondary);">
            ${ICONS.wand}<h2 style="margin:0;font-size:19px;color:var(--on-surface);">把大魔王切得像芝麻一樣小</h2>
          </div>
          <div style="font-size:14px;color:var(--on-surface-variant);">原本的「${escapeHtml(t.title)}」是不是讓人有點喘不過氣？我們把它縮成毫無壓力的第一步：</div>
          <div class="field-group">
            <label class="field-label" for="shrink-title">縮小後的第一步</label>
            <input id="shrink-title" class="input-box" maxlength="100" value="${escapeHtml(defaultShrunk)}" />
          </div>
          <div class="chip-row">
            ${SHRINK_SUGGESTIONS.map((s) => `
              <button type="button" class="filter-chip" style="font-size:11px;padding:6px 10px;" data-preset="${escapeHtml(s)}">${escapeHtml(s)}</button>
            `).join('')}
          </div>
          <div style="display:flex;justify-content:flex-end;gap:8px;">
            <button type="button" class="btn btn-text" id="btn-modal-cancel">取消</button>
            <button type="button" class="btn btn-primary" id="btn-modal-confirm">變成超微事</button>
          </div>
        </div>
      `;
      const input = document.getElementById('shrink-title');
      root.querySelectorAll('[data-preset]').forEach((b) => {
        b.addEventListener('click', () => {
          input.value = `${b.getAttribute('data-preset')}（${t.title}）`.slice(0, 100);
        });
      });
      document.getElementById('btn-modal-cancel').addEventListener('click', closeModal);
      document.getElementById('btn-modal-confirm').addEventListener('click', () => {
        shrinkTask(t.id, input.value, '幫你把大魔王切小塊了，完成這一步就很棒！');
      });
    } else if (ui.modal.type === 'doctorEquip') {
      const doctorName = db.profile.feifeiNickname || '主治醫師';
      const pose = DOCTOR_POSES[ui.stagePose] || DOCTOR_POSES.OBSERVE;
      const coatMeta = COAT_STYLES.find((c) => c.id === db.gear.coat) || COAT_STYLES[0];
      const scopeMeta = STETHOSCOPE_STYLES.find((s) => s.id === db.gear.stethoscope) || STETHOSCOPE_STYLES[0];
      const quote = ui.equipReactionQuote || ui.feifeiQuote;

      root.innerHTML = `
        <div class="sheet-panel" role="dialog" aria-modal="true">
          <div class="sheet-handle"></div>
          <div style="display:flex;align-items:center;justify-content:space-between;">
            <h2 style="margin:0;font-size:21px;">${escapeHtml(doctorName)}的配件與穿搭</h2>
            <span class="pill pill-primary">即時套用</span>
          </div>

          <button type="button" class="quote-box" id="btn-equip-preview" style="flex-direction:row;align-items:center;gap:14px;border:1px solid var(--outline-variant);border-radius:24px;">
            <div style="width:92px;height:146px;display:flex;align-items:center;justify-content:center;flex-shrink:0;">
              ${renderDoctorAvatarHtml(pose.img, db.gear, '142px', `${doctorName}換裝預覽`)}
            </div>
            <div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:8px;">
              <div style="font-size:14px;font-weight:700;">「${escapeHtml(quote)}」</div>
              <div style="font-size:12px;font-weight:700;color:var(--primary);">目前搭配：${escapeHtml(coatMeta.titleZh)} + ${escapeHtml(scopeMeta.titleZh)}</div>
              <div style="display:inline-flex;align-items:center;gap:4px;font-size:11px;color:var(--on-surface-variant);">${ICONS.touch}<span>點一下醫師切換動作與台詞</span></div>
            </div>
          </button>

          <div style="display:flex;flex-direction:column;gap:8px;">
            <div style="display:flex;align-items:center;gap:6px;font-size:16px;font-weight:700;">
              <span style="color:var(--primary);width:18px;height:18px;display:inline-flex;">${ICONS.checkroom}</span>
              <span>白袍配件款式</span>
            </div>
            ${COAT_STYLES.map((c) => {
              const sel = db.gear.coat === c.id;
              return `
                <button type="button" class="gear-option ${sel ? 'selected' : ''}" data-select-coat="${c.id}">
                  <div style="flex:1;min-width:0;">
                    <div style="display:flex;align-items:center;gap:6px;margin-bottom:3px;">
                      <span style="font-size:14px;font-weight:700;">${escapeHtml(c.titleZh)}</span>
                      <span class="pill pill-white" style="padding:2px 7px;font-size:11px;color:var(--primary);">${escapeHtml(c.badgeZh)}</span>
                    </div>
                    <div style="font-size:12px;color:var(--on-surface-variant);">${escapeHtml(c.descriptionZh)}</div>
                  </div>
                  <span style="width:22px;height:22px;color:var(--primary);display:inline-flex;flex-shrink:0;">${sel ? ICONS.checkCircle : ''}</span>
                </button>
              `;
            }).join('')}
          </div>

          <div style="display:flex;flex-direction:column;gap:8px;">
            <div style="display:flex;align-items:center;gap:6px;font-size:16px;font-weight:700;">
              <span style="color:var(--secondary);width:18px;height:18px;display:inline-flex;">${ICONS.medical}</span>
              <span>聽診器配件款式</span>
            </div>
            ${STETHOSCOPE_STYLES.map((s) => {
              const sel = db.gear.stethoscope === s.id;
              return `
                <button type="button" class="gear-option ${sel ? 'selected' : ''}" data-select-scope="${s.id}">
                  <div style="flex:1;min-width:0;">
                    <div style="display:flex;align-items:center;gap:6px;margin-bottom:3px;">
                      <span style="font-size:14px;font-weight:700;">${escapeHtml(s.titleZh)}</span>
                      <span class="pill pill-white" style="padding:2px 7px;font-size:11px;color:var(--primary);">${escapeHtml(s.badgeZh)}</span>
                    </div>
                    <div style="font-size:12px;color:var(--on-surface-variant);">${escapeHtml(s.descriptionZh)}</div>
                  </div>
                  <span style="width:22px;height:22px;color:var(--primary);display:inline-flex;flex-shrink:0;">${sel ? ICONS.checkCircle : ''}</span>
                </button>
              `;
            }).join('')}
          </div>

          <button type="button" class="btn btn-primary btn-full" id="btn-equip-done">穿好這套回康復室</button>
        </div>
      `;

      document.getElementById('btn-equip-preview').addEventListener('click', () => {
        patFeifei();
        renderModal();
        renderCurrentTab();
      });
      root.querySelectorAll('[data-select-coat]').forEach((btn) => {
        btn.addEventListener('click', () => {
          db.gear.coat = btn.getAttribute('data-select-coat');
          ui.equipReactionQuote = pickNonRepeating(EQUIP_REACTIONS, ui.equipReactionQuote);
          commitDb();
          renderModal();
          renderCurrentTab();
        });
      });
      root.querySelectorAll('[data-select-scope]').forEach((btn) => {
        btn.addEventListener('click', () => {
          db.gear.stethoscope = btn.getAttribute('data-select-scope');
          ui.equipReactionQuote = pickNonRepeating(EQUIP_REACTIONS, ui.equipReactionQuote);
          commitDb();
          renderModal();
          renderCurrentTab();
        });
      });
      document.getElementById('btn-equip-done').addEventListener('click', closeModal);
    } else if (ui.modal.type === 'account') {
      const currentNickname = db.profile.feifeiNickname || '主治醫師';
      root.innerHTML = `
        <div class="sheet-panel" role="dialog" aria-modal="true">
          <div class="sheet-handle"></div>
          <h2 style="margin:0;font-size:21px;">掛號病歷與陪伴設定</h2>
          <div style="font-size:14px;color:var(--on-surface-variant);">目前儲存方式：單機儲存（localStorage，本瀏覽器離線也可永久保留）</div>
          <div class="field-group">
            <label class="field-label" for="input-nickname">幫你的主治醫師取個名字</label>
            <input id="input-nickname" class="input-box" maxlength="30" value="${escapeHtml(currentNickname)}" />
          </div>
          <button type="button" class="btn btn-primary btn-full" id="btn-save-nickname">儲存陪伴暱稱</button>
          <div style="display:flex;gap:10px;">
            <button type="button" class="btn btn-secondary" style="flex:1;" id="btn-export-json">${ICONS.download}<span>匯出病歷備份</span></button>
            <button type="button" class="btn btn-outline-soft" style="flex:1;" id="btn-import-json">${ICONS.upload}<span>匯入備份</span></button>
            <input type="file" id="file-import-json" accept="application/json" class="hidden" />
          </div>
          <button type="button" class="btn btn-outline" style="color:var(--secondary);border-color:var(--secondary);" id="btn-reset-local">
            ${ICONS.reset}<span>重置單機病歷為初始狀態</span>
          </button>
        </div>
      `;

      document.getElementById('btn-save-nickname').addEventListener('click', () => {
        const clean = document.getElementById('input-nickname').value.trim().slice(0, 30);
        if (!clean) return;
        db.profile.feifeiNickname = clean;
        commitDb();
        closeModal();
        showToast(`好喔！以後我就叫「${clean}」！`);
        renderCurrentTab();
      });

      document.getElementById('btn-export-json').addEventListener('click', () => {
        const blob = new Blob([JSON.stringify(db, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `lazy-rehab-backup-${todayDateKey()}.json`;
        a.click();
        URL.revokeObjectURL(url);
        showToast('已匯出單機病歷備份檔！');
      });

      const fileInput = document.getElementById('file-import-json');
      document.getElementById('btn-import-json').addEventListener('click', () => fileInput.click());
      fileInput.addEventListener('change', (e) => {
        const file = e.target.files && e.target.files[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onload = () => {
          try {
            const parsed = JSON.parse(String(reader.result || '{}'));
            if (!parsed.profile || !Array.isArray(parsed.tasks)) {
              showToast('這份檔案好像不是懶癌康復室的備份喔');
              return;
            }
            localStorage.setItem(STORAGE_KEY, JSON.stringify(parsed));
            db = loadPersistedData();
            syncDraftFromTodayCheckin();
            closeModal();
            showToast('成功匯入病歷與病房紀錄！');
            renderCurrentTab();
          } catch (_) {
            showToast('讀取備份檔失敗，請確認格式是否為 JSON');
          }
        };
        reader.readAsText(file);
      });

      document.getElementById('btn-reset-local').addEventListener('click', () => {
        if (!window.confirm('確定要清空目前的任務、日記與道具收藏，回到剛入院時的狀態嗎？')) return;
        db = createDefaultData();
        savePersistedData(db);
        syncDraftFromTodayCheckin();
        closeModal();
        showToast('已重置為剛入院的初始狀態！');
        renderCurrentTab();
      });
    }
  }

  document.getElementById('modal-root')?.addEventListener('click', (e) => {
    if (e.target && e.target.id === 'modal-root') closeModal();
  });

  // -------------------- Shell & Navigation Rendering --------------------
  function renderAppChrome() {
    const activeTabMeta = TABS.find((t) => t.id === ui.selectedTab) || TABS[0];
    document.getElementById('top-bar-title').textContent = activeTabMeta.topTitle;
    document.getElementById('btn-account-settings').innerHTML = ICONS.account;

    const fab = document.getElementById('fab-add-task');
    fab.querySelector('.fab-icon').innerHTML = ICONS.add;
    fab.classList.toggle('hidden', ui.selectedTab !== 'room');

    const navHtml = TABS.map((t) => `
      <button type="button" class="nav-item ${t.id === ui.selectedTab ? 'active' : ''}" data-tab="${t.id}">
        <span class="nav-pill">${t.icon}</span>
        <span>${t.navLabel}</span>
      </button>
    `).join('');

    const railEl = document.getElementById('nav-rail');
    const bottomEl = document.getElementById('bottom-nav');
    railEl.innerHTML = navHtml;
    bottomEl.innerHTML = navHtml;

    document.querySelectorAll('[data-tab]').forEach((btn) => {
      btn.addEventListener('click', () => switchTab(btn.getAttribute('data-tab')));
    });
  }

  function switchTab(tabId) {
    if (ui.selectedTab === tabId) return;
    ui.selectedTab = tabId;
    ui.openMenuTaskId = null;
    stopLiveWardController();
    renderAppChrome();
    renderCurrentTab();
    const scrollEl = document.getElementById('main-scroll');
    if (scrollEl) scrollEl.scrollTop = 0;
  }

  function renderCurrentTab() {
    stopLiveWardController();
    const container = document.getElementById('tab-content');
    if (!container) return;
    switch (ui.selectedTab) {
      case 'room': renderRoomTab(container); break;
      case 'prescriptions': renderPrescriptionsTab(container); break;
      case 'pharmacy': renderPharmacyTab(container); break;
      case 'memories': renderMemoriesTab(container); break;
      case 'log': renderLogTab(container); break;
    }
  }

  // Close task dropdown when clicking outside
  document.addEventListener('click', () => {
    if (ui.openMenuTaskId !== null) {
      ui.openMenuTaskId = null;
      renderCurrentTab();
    }
  });

  document.getElementById('btn-account-settings')?.addEventListener('click', () => openModal({ type: 'account' }));
  document.getElementById('fab-add-task')?.addEventListener('click', () => openModal({ type: 'addTask' }));

  // Initial mount
  renderAppChrome();
  renderCurrentTab();
})();
