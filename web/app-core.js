/* 懶癌康復室 — Core State, Migration, Utilities, Icons & Doctor SVG Renderer */
(function () {
  'use strict';

  var D = window.REHAB_DATA;
  var STORAGE_KEY_V2 = 'lazy_rehab_web_state_v2';
  var STORAGE_KEY_V1 = 'lazy_rehab_web_state_v1';

  var POSE_MAP = {
    CALM: './assets/img_mascot_main.png',
    APPROVE: './assets/img_doctor_approve.png',
    SIPPING: './assets/img_doctor_sipping.png',
    PHARMACY: './assets/img_doctor_pharmacy.png',
    SLEEPING: './assets/img_mascot_main.png'
  };

  function resolvePoseImage(pose) {
    return POSE_MAP[pose] || POSE_MAP.CALM;
  }

  function uid(prefix) {
    return (prefix || 'id') + '_' + Date.now().toString(36) + '_' + Math.random().toString(36).slice(2, 7);
  }

  function esc(str) {
    return String(str == null ? '' : str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function clamp(v, min, max) {
    return Math.max(min, Math.min(max, v));
  }

  function pad2(n) {
    return (n < 10 ? '0' : '') + n;
  }

  function pickRandom(arr, exclude) {
    if (!arr || !arr.length) return '';
    if (arr.length === 1) return arr[0];
    var pool = exclude ? arr.filter(function (x) { return x !== exclude; }) : arr;
    if (!pool.length) pool = arr;
    return pool[Math.floor(Math.random() * pool.length)];
  }

  function formatDateKey(date) {
    var d = date || new Date();
    return d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate());
  }

  function parseDateKey(key) {
    var parts = String(key || '').split('-');
    var y = parseInt(parts[0], 10);
    var m = parseInt(parts[1], 10);
    var day = parseInt(parts[2], 10);
    if (!y || !m || !day) {
      var now = new Date();
      return { year: now.getFullYear(), month: now.getMonth() + 1, day: now.getDate() };
    }
    return { year: y, month: m, day: day };
  }

  function shiftDateKey(key, deltaDays) {
    var p = parseDateKey(key);
    var d = new Date(p.year, p.month - 1, p.day + (deltaDays || 0));
    return formatDateKey(d);
  }

  function daysBetweenDateKeys(fromKey, toKey) {
    var a = parseDateKey(fromKey);
    var b = parseDateKey(toKey);
    var t1 = new Date(a.year, a.month - 1, a.day).getTime();
    var t2 = new Date(b.year, b.month - 1, b.day).getTime();
    return Math.round((t2 - t1) / 86400000);
  }

  function effectiveCompletionDateKey(category, resetHour, now) {
    var d = now || new Date();
    var rh = Number(resetHour) || 0;
    if (category === 'game' && rh > 0 && d.getHours() < rh) {
      return shiftDateKey(formatDateKey(d), -1);
    }
    return formatDateKey(d);
  }

  function lastResetBoundaryMillis(resetHour, now) {
    var d = now || new Date();
    var rh = clamp(Number(resetHour) || 0, 0, 23);
    var todayReset = new Date(d.getFullYear(), d.getMonth(), d.getDate(), rh, 0, 0, 0).getTime();
    if (d.getTime() >= todayReset) return todayReset;
    return new Date(d.getFullYear(), d.getMonth(), d.getDate() - 1, rh, 0, 0, 0).getTime();
  }

  function shouldResetTask(task, now) {
    if (!task || !task.completed) return false;
    var d = now || new Date();
    var cat = task.category || 'daily';
    if (cat === 'homework') return false;
    if (cat === 'game') {
      var rh = clamp(Number(task.resetHour) || 0, 0, 23);
      if (task.completedAtMs && Number(task.completedAtMs) > 0) {
        return Number(task.completedAtMs) < lastResetBoundaryMillis(rh, d);
      }
      var expectedKey = effectiveCompletionDateKey('game', rh, d);
      return !!task.completedDate && task.completedDate !== expectedKey;
    }
    var todayKey = formatDateKey(d);
    return !!task.completedDate && task.completedDate !== todayKey;
  }

  function isEarly8AmStartTime(startTime) {
    return /^\s*0?8[:：]/.test(String(startTime || ''));
  }

  function addMinutesToTime(timeStr, addMins) {
    var m = String(timeStr || '').trim().match(/^(\d{1,2})[:：](\d{2})$/);
    if (!m) return '09:00';
    var h = clamp(parseInt(m[1], 10) || 0, 0, 23);
    var min = clamp(parseInt(m[2], 10) || 0, 0, 59);
    var total = (h * 60 + min + (addMins || 0)) % 1440;
    if (total < 0) total += 1440;
    return pad2(Math.floor(total / 60)) + ':' + pad2(total % 60);
  }

  function percentToEnergyLevel(pct) {
    var val = clamp(Math.round(Number(pct) || 0), 0, 100);
    var levels = D.ENERGY_LEVELS;
    for (var i = 0; i < levels.length; i++) {
      if (val <= levels[i].percentValue) return levels[i];
    }
    return levels[levels.length - 1];
  }

  function energyWireToPercent(wire) {
    var found = D.ENERGY_LEVELS.filter(function (e) { return e.wireValue === wire; })[0];
    return found ? found.percentValue : 50;
  }

  function createDefaultState() {
    var nowMs = Date.now();
    return {
      schemaVersion: 2,
      feifeiNickname: '主治醫生',
      totalCompleted: 0,
      ownedItemIds: [],
      equippedItemIds: [],
      wardPlacements: [],
      wardDoctorSnapshot: null,
      doctorGear: {
        coatId: 'classic_white',
        stethoscopeId: 'default_heart',
        headwearId: 'none'
      },
      tasks: D.STARTER_TASKS.map(function (t, idx) {
        return {
          id: t.id,
          title: t.title,
          note: t.note || '',
          effortLevel: t.effortLevel || 'micro',
          category: t.category || 'daily',
          completed: false,
          completedDate: '',
          completedAtMs: 0,
          sortOrder: idx,
          timeSlotsEnabled: false,
          morningEnabled: true,
          noonEnabled: true,
          eveningEnabled: true,
          morningDone: false,
          noonDone: false,
          eveningDone: false,
          resetHour: t.resetHour != null ? t.resetHour : 4,
          deadlineDate: ''
        };
      }),
      customCategories: [],
      hiddenCategories: [],
      checkins: [],
      calendarEvents: [],
      timetable: D.createDefaultTimetableConfig(),
      rehabPoints: 0,
      lastIdleAccrualMs: nowMs,
      unlockedCgIds: [],
      hasSeenRehabGymIntro: false,
      specialChannelUnlocked: false,
      otomeThemeId: 'treehouse',
      otomeClinicProgress: {
        clinicVisits: 0,
        secretLettersGiven: 0,
        drawnHeartRxId: '',
        drawnHeartRxDate: ''
      },
      notificationsEnabled: false,
      sentNotifKeys: []
    };
  }

  function normalizeTask(t, idx) {
    var slotOn = !!t.timeSlotsEnabled;
    var mOn = t.morningEnabled !== undefined ? !!t.morningEnabled : true;
    var nOn = t.noonEnabled !== undefined ? !!t.noonEnabled : true;
    var eOn = t.eveningEnabled !== undefined ? !!t.eveningEnabled : true;
    if (!mOn && !nOn && !eOn) mOn = true;
    return {
      id: String(t.id || uid('task')),
      title: String(t.title || '超微小復健事'),
      note: String(t.note || ''),
      effortLevel: ['micro', 'gentle', 'boss'].indexOf(t.effortLevel) >= 0 ? t.effortLevel : 'micro',
      category: String(t.category || 'daily'),
      completed: !!t.completed,
      completedDate: String(t.completedDate || ''),
      completedAtMs: Number(t.completedAtMs) || 0,
      sortOrder: Number.isFinite(Number(t.sortOrder)) ? Number(t.sortOrder) : idx,
      timeSlotsEnabled: slotOn,
      morningEnabled: mOn,
      noonEnabled: nOn,
      eveningEnabled: eOn,
      morningDone: !!t.morningDone,
      noonDone: !!t.noonDone,
      eveningDone: !!t.eveningDone,
      resetHour: clamp(Number(t.resetHour != null ? t.resetHour : 4), 0, 23),
      deadlineDate: String(t.deadlineDate || '')
    };
  }

  function normalizeState(raw) {
    var base = createDefaultState();
    if (!raw || typeof raw !== 'object') return base;

    var nick = String(raw.feifeiNickname || '').trim();
    if (!nick || (nick.indexOf('主治') === 0 && nick.charAt(nick.length - 1) === '師')) {
      nick = '主治醫生';
    }
    base.feifeiNickname = nick;
    base.totalCompleted = Math.max(0, Number(raw.totalCompleted) || 0);
    base.ownedItemIds = Array.isArray(raw.ownedItemIds) ? raw.ownedItemIds.filter(Boolean) : [];
    base.equippedItemIds = Array.isArray(raw.equippedItemIds) ? raw.equippedItemIds.filter(Boolean) : [];

    var rawPlacements = Array.isArray(raw.wardPlacements) ? raw.wardPlacements : [];
    if (!rawPlacements.length && base.equippedItemIds.length) {
      rawPlacements = base.equippedItemIds.map(function (id) {
        var pos = D.DEFAULT_WARD_POSITIONS[id] || { xFraction: 0.5, yFraction: 0.72 };
        return { itemId: id, xFraction: pos.xFraction, yFraction: pos.yFraction };
      });
    }
    base.wardPlacements = rawPlacements.map(function (p) {
      return {
        itemId: String(p.itemId || ''),
        xFraction: clamp(Number(p.xFraction) || 0.5, 0.08, 0.92),
        yFraction: clamp(Number(p.yFraction) || 0.72, 0.3, 0.94)
      };
    }).filter(function (p) { return p.itemId; });

    if (raw.wardDoctorSnapshot && typeof raw.wardDoctorSnapshot === 'object') {
      base.wardDoctorSnapshot = {
        xFraction: clamp(Number(raw.wardDoctorSnapshot.xFraction) || 0.52, 0.1, 0.9),
        yFraction: clamp(Number(raw.wardDoctorSnapshot.yFraction) || 0.78, 0.35, 0.94),
        facingRight: !!raw.wardDoctorSnapshot.facingRight,
        pose: String(raw.wardDoctorSnapshot.pose || 'CALM'),
        spriteName: String(raw.wardDoctorSnapshot.spriteName || '')
      };
    }

    if (raw.doctorGear && typeof raw.doctorGear === 'object') {
      base.doctorGear = {
        coatId: String(raw.doctorGear.coatId || 'classic_white'),
        stethoscopeId: String(raw.doctorGear.stethoscopeId || 'default_heart'),
        headwearId: String(raw.doctorGear.headwearId || 'none')
      };
    }

    if (Array.isArray(raw.tasks)) {
      base.tasks = raw.tasks.map(normalizeTask);
    }

    if (Array.isArray(raw.customCategories)) {
      base.customCategories = raw.customCategories
        .filter(function (c) { return c && c.wireValue && c.labelZh; })
        .map(function (c) {
          return { wireValue: String(c.wireValue), labelZh: String(c.labelZh), isBuiltIn: false };
        });
    }

    if (Array.isArray(raw.hiddenCategories)) {
      base.hiddenCategories = raw.hiddenCategories.map(String);
    }

    if (Array.isArray(raw.checkins)) {
      base.checkins = raw.checkins.map(function (c) {
        var lvl = String(c.energyLevel || 'e50');
        var pct = c.energyPercent != null ? clamp(Number(c.energyPercent), 0, 100) : energyWireToPercent(lvl);
        return {
          id: String(c.id || c.dateKey || uid('chk')),
          dateKey: String(c.dateKey || formatDateKey()),
          energyLevel: lvl,
          energyPercent: pct,
          reflection: String(c.reflection || ''),
          completedTaskTitles: Array.isArray(c.completedTaskTitles) ? c.completedTaskTitles.map(String) : [],
          wardPlacements: Array.isArray(c.wardPlacements) ? c.wardPlacements : [],
          wardDoctorSnapshot: c.wardDoctorSnapshot || null
        };
      });
    }

    if (Array.isArray(raw.calendarEvents)) {
      base.calendarEvents = raw.calendarEvents.map(function (ev) {
        return {
          id: String(ev.id || uid('ev')),
          title: String(ev.title || ''),
          note: String(ev.note || ''),
          dateKey: String(ev.dateKey || formatDateKey()),
          allDay: !!ev.allDay,
          startTime: String(ev.startTime || '09:00'),
          endTime: String(ev.endTime || '10:00')
        };
      }).filter(function (ev) { return ev.title; });
    }

    if (raw.timetable && typeof raw.timetable === 'object') {
      var defT = D.createDefaultTimetableConfig();
      base.timetable = {
        tableTitle: String(raw.timetable.tableTitle || defT.tableTitle),
        cornerLabel: String(raw.timetable.cornerLabel || defT.cornerLabel),
        showWeekends: raw.timetable.showWeekends !== undefined ? !!raw.timetable.showWeekends : true,
        periods: Array.isArray(raw.timetable.periods) && raw.timetable.periods.length ? raw.timetable.periods : defT.periods,
        cells: raw.timetable.cells && typeof raw.timetable.cells === 'object' && !Array.isArray(raw.timetable.cells) ? raw.timetable.cells : {}
      };
    }

    base.rehabPoints = Math.max(0, Number(raw.rehabPoints) || 0);
    base.lastIdleAccrualMs = Number(raw.lastIdleAccrualMs) || Date.now();
    base.unlockedCgIds = Array.isArray(raw.unlockedCgIds) ? raw.unlockedCgIds.map(String) : [];
    base.hasSeenRehabGymIntro = !!raw.hasSeenRehabGymIntro;
    base.specialChannelUnlocked = !!raw.specialChannelUnlocked;
    base.otomeThemeId = String(raw.otomeThemeId || 'treehouse');

    if (raw.otomeClinicProgress && typeof raw.otomeClinicProgress === 'object') {
      base.otomeClinicProgress = {
        clinicVisits: Number(raw.otomeClinicProgress.clinicVisits) || 0,
        secretLettersGiven: Number(raw.otomeClinicProgress.secretLettersGiven) || 0,
        drawnHeartRxId: String(raw.otomeClinicProgress.drawnHeartRxId || ''),
        drawnHeartRxDate: String(raw.otomeClinicProgress.drawnHeartRxDate || '')
      };
    }

    base.notificationsEnabled = !!raw.notificationsEnabled;
    base.sentNotifKeys = Array.isArray(raw.sentNotifKeys) ? raw.sentNotifKeys.slice(-80) : [];
    return base;
  }

  function runAutoTaskResets(st) {
    var now = new Date();
    var changed = false;
    st.tasks = st.tasks.map(function (t) {
      if (!shouldResetTask(t, now)) return t;
      changed = true;
      return Object.assign({}, t, {
        completed: false,
        completedDate: '',
        completedAtMs: 0,
        morningDone: false,
        noonDone: false,
        eveningDone: false
      });
    });
    return changed;
  }

  function accrueIdlePointsOnState(st) {
    var nowMs = Date.now();
    var lastMs = Number(st.lastIdleAccrualMs) || nowMs;
    var elapsedMs = Math.max(0, nowMs - lastMs);
    var hours = Math.floor(elapsedMs / 3600000);
    if (hours > 0) {
      st.rehabPoints = Math.min(9999999, (st.rehabPoints || 0) + hours * 60);
      st.lastIdleAccrualMs = lastMs + hours * 3600000;
      return hours * 60;
    }
    return 0;
  }

  function loadState() {
    var raw = null;
    try {
      var str = localStorage.getItem(STORAGE_KEY_V2);
      if (str) {
        raw = JSON.parse(str);
      } else {
        var v1Str = localStorage.getItem(STORAGE_KEY_V1);
        if (v1Str) raw = JSON.parse(v1Str);
      }
    } catch (e) {
      raw = null;
    }
    var st = normalizeState(raw);
    runAutoTaskResets(st);
    accrueIdlePointsOnState(st);
    saveState(st);
    return st;
  }

  function saveState(st) {
    try {
      localStorage.setItem(STORAGE_KEY_V2, JSON.stringify(st));
    } catch (e) {
      /* ignore quota errors */
    }
  }

  /* Clean Inline SVG Icons */
  var ICONS = {
    room: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 3L3 10.5V20a1 1 0 001 1h5v-6h6v6h5a1 1 0 001-1v-9.5L12 3z"/></svg>',
    rx: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M4.22 11.29l7.07-7.07a4.5 4.5 0 016.36 6.36l-7.07 7.07a4.5 4.5 0 01-6.36-6.36zm4.95 4.95a2.5 2.5 0 003.54 0l3-3-3.54-3.54-3 3a2.5 2.5 0 000 3.54z"/></svg>',
    pharmacy: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 6h-2.18A3 3 0 0014 4h-4a3 3 0 00-2.82 2H5a2 2 0 00-2 2v11a2 2 0 002 2h14a2 2 0 002-2V8a2 2 0 00-2-2zm-9 0h4v1h-4V6zm5 9h-2v2h-2v-2H9v-2h2v-2h2v2h2v2z"/></svg>',
    memories: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M21 5c-1.11-.35-2.33-.5-3.5-.5-1.95 0-4.05.4-5.5 1.5-1.45-1.1-3.55-1.5-5.5-1.5S2.45 4.9 1 6v14.65c0 .25.25.5.5.5.1 0 .15-.05.25-.05C3.1 20.45 5.05 20 6.5 20c1.95 0 4.05.4 5.5 1.5 1.35-.85 3.8-1.5 5.5-1.5 1.65 0 3.35.3 4.75 1.05.1.05.15.05.25.05.25 0 .5-.25.5-.5V6c-.6-.45-1.25-.75-2-1zm0 13.5c-1.1-.35-2.3-.5-3.5-.5-1.7 0-4.15.65-5.5 1.5V8c1.35-.85 3.8-1.5 5.5-1.5 1.2 0 2.4.15 3.5.5v11.5z"/></svg>',
    log: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 3h-1V1h-2v2H8V1H6v2H5a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2V5a2 2 0 00-2-2zm0 16H5V9h14v10zM7 11h5v5H7z"/></svg>',
    settings: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M19.14 12.94c.04-.3.06-.61.06-.94s-.02-.64-.07-.94l2.03-1.58a.5.5 0 00.12-.61l-1.92-3.32a.5.5 0 00-.59-.22l-2.39.96a7.1 7.1 0 00-1.62-.94l-.36-2.54A.49.49 0 0014 2h-4a.5.5 0 00-.49.42l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96a.49.49 0 00-.59.22L2.63 8.48a.49.49 0 00.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58a.49.49 0 00-.12.61l1.92 3.32a.5.5 0 00.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.42.49.42h4c.25 0 .44-.18.49-.42l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32a.5.5 0 00-.12-.61l-2.01-1.58zM12 15.6A3.6 3.6 0 1112 8.4a3.6 3.6 0 010 7.2z"/></svg>',
    plus: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>',
    close: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>',
    more: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 8a2 2 0 100-4 2 2 0 000 4zm0 2a2 2 0 100 4 2 2 0 000-4zm0 6a2 2 0 100 4 2 2 0 000-4z"/></svg>',
    check: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>',
    edit: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zm17.71-10.21a1 1 0 000-1.41l-2.34-2.34a1 1 0 00-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg>',
    trash: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M6 19a2 2 0 002 2h8a2 2 0 002-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/></svg>',
    drag: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M11 18a2 2 0 11-4 0 2 2 0 014 0zm0-6a2 2 0 11-4 0 2 2 0 014 0zm0-6a2 2 0 11-4 0 2 2 0 014 0zm6 12a2 2 0 11-4 0 2 2 0 014 0zm0-6a2 2 0 11-4 0 2 2 0 014 0zm0-6a2 2 0 11-4 0 2 2 0 014 0z"/></svg>',
    chevLeft: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"/></svg>',
    chevRight: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/></svg>',
    spark: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 9l1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9zm-7.5.5L9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12l-5.5-2.5z"/></svg>',
    shirt: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M16 4l4 2-2 5h-2v9H8v-9H6L4 6l4-2c.6 1.2 1.9 2 4 2s3.4-.8 4-2z"/></svg>',
    heart: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>',
    clock: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2a10 10 0 1010 10A10 10 0 0012 2zm1 11h-5v-2h3V6h2v7z"/></svg>',
    bell: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 22a2 2 0 002-2h-4a2 2 0 002 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4a1.5 1.5 0 00-3 0v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"/></svg>',
    info: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2a10 10 0 1010 10A10 10 0 0012 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/></svg>',
    menu: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z"/></svg>',
    lock: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M18 8h-1V6a5 5 0 00-10 0v2H6a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V10a2 2 0 00-2-2zm-6 9a2 2 0 110-4 2 2 0 010 4zm3.1-9H8.9V6a3.1 3.1 0 016.2 0v2z"/></svg>',
    unlock: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 17a2 2 0 100-4 2 2 0 000 4zm6-9h-1V6a5 5 0 00-9.9-1h2a3 3 0 015.9 1v2H6a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V10a2 2 0 00-2-2z"/></svg>',
    zoomIn: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M15.5 14h-.79l-.28-.27A6.47 6.47 0 0016 9.5 6.5 6.5 0 109.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14zm.5-7H9v2H7v1h2v2h1v-2h2V9h-2V7z"/></svg>',
    zoomOut: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M15.5 14h-.79l-.28-.27A6.47 6.47 0 0016 9.5 6.5 6.5 0 109.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14zM7 9h5v1H7z"/></svg>',
    palette: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 3a9 9 0 000 18c.83 0 1.5-.67 1.5-1.5 0-.39-.15-.74-.39-1.01-.23-.26-.38-.61-.38-.99 0-.83.67-1.5 1.5-1.5H16a5 5 0 005-5c0-4.42-4.03-8-9-8zm-5.5 9a1.5 1.5 0 110-3 1.5 1.5 0 010 3zm3-4a1.5 1.5 0 110-3 1.5 1.5 0 010 3zm5 0a1.5 1.5 0 110-3 1.5 1.5 0 010 3zm3 4a1.5 1.5 0 110-3 1.5 1.5 0 010 3z"/></svg>',
    chat: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M20 2H4a2 2 0 00-2 2v18l4-4h14a2 2 0 002-2V4a2 2 0 00-2-2z"/></svg>',
    mail: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M20 4H4a2 2 0 00-2 2v12a2 2 0 002 2h16a2 2 0 002-2V6a2 2 0 00-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>',
    eye: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17a5 5 0 110-10 5 5 0 010 10zm0-8a3 3 0 100 6 3 3 0 000-6z"/></svg>',
    dice: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 3H5a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2V5a2 2 0 00-2-2zM7.5 18a1.5 1.5 0 110-3 1.5 1.5 0 010 3zm0-9a1.5 1.5 0 110-3 1.5 1.5 0 010 3zm4.5 4.5a1.5 1.5 0 110-3 1.5 1.5 0 010 3zm4.5 4.5a1.5 1.5 0 110-3 1.5 1.5 0 010 3zm0-9a1.5 1.5 0 110-3 1.5 1.5 0 010 3z"/></svg>',
    trophy: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 5h-2V3H7v2H5a2 2 0 00-2 2v1c0 2.55 1.92 4.63 4.39 4.94A5 5 0 0011 15.9V19H7v2h10v-2h-4v-3.1a5 5 0 003.61-2.96C19.08 12.63 21 10.55 21 8V7a2 2 0 00-2-2zM5 8V7h2v3.82C5.84 10.4 5 9.3 5 8zm14 0c0 1.3-.84 2.4-2 2.82V7h2v1z"/></svg>',
    pin: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2a7 7 0 00-7 7c0 5.25 7 13 7 13s7-7.75 7-13a7 7 0 00-7-7zm0 9.5a2.5 2.5 0 110-5 2.5 2.5 0 010 5z"/></svg>',
    move: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M10 9h4V6h3l-5-5-5 5h3v3zm-1 1H6V7l-5 5 5 5v-3h3v-4zm14 2l-5-5v3h-3v4h3v3l5-5zm-9 3h-4v3H7l5 5 5-5h-3v-3z"/></svg>'
  };

  /* Normalized SVG Avatar Gear Overlay matching DoctorAvatarWithGear.kt (viewBox 100x180) */
  function svgStar(cx, cy, r, fill, strokeW) {
    var pts = [];
    for (var i = 0; i < 10; i++) {
      var a = -Math.PI / 2 + i * (Math.PI / 5);
      var rad = i % 2 === 0 ? r : r * 0.45;
      pts.push((cx + rad * Math.cos(a)).toFixed(2) + ',' + (cy + rad * Math.sin(a)).toFixed(2));
    }
    return '<polygon points="' + pts.join(' ') + '" fill="' + fill + '" stroke="#4B3532" stroke-width="' + strokeW.toFixed(2) + '" stroke-linejoin="round"/>';
  }

  function svgPaw(cx, cy, padR, fill, strokeW) {
    var toeR = padR * 0.42;
    var toes = [
      [cx - padR * 0.95, cy - padR * 0.85],
      [cx, cy - padR * 1.15],
      [cx + padR * 0.95, cy - padR * 0.85]
    ];
    var out = '';
    for (var i = 0; i < toes.length; i++) {
      out += '<circle cx="' + toes[i][0].toFixed(2) + '" cy="' + toes[i][1].toFixed(2) + '" r="' + toeR.toFixed(2) + '" fill="' + fill + '" stroke="#4B3532" stroke-width="' + (strokeW * 0.7).toFixed(2) + '"/>';
    }
    out += '<circle cx="' + cx.toFixed(2) + '" cy="' + cy.toFixed(2) + '" r="' + padR.toFixed(2) + '" fill="' + fill + '" stroke="#4B3532" stroke-width="' + (strokeW * 0.8).toFixed(2) + '"/>';
    return out;
  }

  function renderDoctorAvatarHtml(imgSrc, gear, opts) {
    var o = opts || {};
    var g = gear || {};
    var coat = g.coatId || 'classic_white';
    var scope = g.stethoscopeId || 'default_heart';
    var head = g.headwearId || 'none';
    var isSide = !!o.isSideProfile;
    var hideGear = !!o.hideGear;
    var heightStyle = o.height ? 'height:' + o.height + ';' : '';
    var widthStyle = o.width ? 'width:' + o.width + ';' : '';
    var aspect = o.aspect || (isSide ? 0.5 : 0.556);
    var extraStyle = o.style || '';
    var alt = esc(o.alt || '主治醫生');
    var cls = 'doctor-avatar' + (o.className ? ' ' + o.className : '');

    var svgLayers = '';
    if (!hideGear && (coat !== 'classic_white' || scope !== 'default_heart' || head !== 'none')) {
      var W = 100;
      var H = 180;
      var ow = 1.5;

      // 1. Coat
      if (coat === 'star_director') {
        var pinX = isSide ? 62 : 35.5;
        var pinY = 0.485 * H;
        svgLayers += svgStar(pinX, pinY, 4.8, '#FFD54F', ow * 0.9);
        if (!isSide) {
          svgLayers += '<line x1="69" y1="' + (0.618 * H) + '" x2="72" y2="' + (0.588 * H) + '" stroke="#7986CB" stroke-width="' + (ow * 2.1) + '" stroke-linecap="round"/>';
          svgLayers += '<circle cx="72" cy="' + (0.588 * H) + '" r="2.2" fill="#FFD54F" stroke="#4B3532" stroke-width="' + (ow * 0.65) + '"/>';
          svgLayers += '<rect x="25" y="' + (0.578 * H) + '" width="9" height="' + (0.015 * H) + '" rx="2" fill="rgba(144,202,249,0.85)"/>';
          svgLayers += '<rect x="67" y="' + (0.578 * H) + '" width="9" height="' + (0.015 * H) + '" rx="2" fill="rgba(144,202,249,0.85)"/>';
        }
      } else if (coat === 'cat_paw_coat') {
        if (!isSide) {
          var bx = 50, by = 0.475 * H, hw = 6.2, hh = 4.5;
          svgLayers += '<path d="M' + bx + ',' + by + ' L' + (bx - hw) + ',' + (by - hh) + ' L' + (bx - hw) + ',' + (by + hh) + ' Z M' + bx + ',' + by + ' L' + (bx + hw) + ',' + (by - hh) + ' L' + (bx + hw) + ',' + (by + hh) + ' Z" fill="#F48FB1" stroke="#4B3532" stroke-width="' + (ow * 0.85) + '" stroke-linejoin="round"/>';
          svgLayers += '<circle cx="' + bx + '" cy="' + by + '" r="' + (hh * 0.55) + '" fill="#F48FB1" stroke="#4B3532" stroke-width="' + (ow * 0.85) + '"/>';
          svgLayers += svgPaw(32, 0.65 * H, 3.6, '#F8BBD0', ow * 0.75);
          svgLayers += svgPaw(68, 0.65 * H, 3.6, '#F8BBD0', ow * 0.75);
        } else {
          svgLayers += svgPaw(59, 0.635 * H, 4.2, '#F8BBD0', ow * 0.75);
        }
      } else if (coat === 'pastel_mint_coat') {
        if (!isSide) {
          svgLayers += '<line x1="36.5" y1="' + (0.47 * H) + '" x2="39.5" y2="' + (0.625 * H) + '" stroke="#80CBC4" stroke-width="' + (ow * 1.9) + '" stroke-linecap="round"/>';
          svgLayers += '<line x1="63.5" y1="' + (0.47 * H) + '" x2="60.5" y2="' + (0.625 * H) + '" stroke="#80CBC4" stroke-width="' + (ow * 1.9) + '" stroke-linecap="round"/>';
          var tx = 63.5, ty = 0.568 * H, tw = 11.5, th = 0.03 * H;
          svgLayers += '<rect x="' + tx + '" y="' + ty + '" width="' + tw + '" height="' + th + '" rx="2" fill="#E0F2F1" stroke="#4B3532" stroke-width="' + (ow * 0.7) + '"/>';
          svgLayers += '<line x1="' + (tx + tw * 0.22) + '" y1="' + (ty + th * 0.5) + '" x2="' + (tx + tw * 0.78) + '" y2="' + (ty + th * 0.5) + '" stroke="#00897B" stroke-width="' + (ow * 0.9) + '" stroke-linecap="round"/>';
        } else {
          svgLayers += '<rect x="55" y="' + (0.565 * H) + '" width="12" height="' + (0.025 * H) + '" rx="2" fill="#80CBC4"/>';
        }
      }

      // 2. Stethoscope
      if (scope !== 'default_heart') {
        var tubeColor = scope === 'gold_vip' ? '#FFCA28' : scope === 'cat_bell' ? '#81D4FA' : '#D7A86E';
        var neckD = !isSide
          ? 'M 37.5 ' + (0.525 * H) + ' C 38.5 ' + (0.452 * H) + ' 42.5 ' + (0.448 * H) + ' 43.8 ' + (0.478 * H) + ' M 57.8 ' + (0.46 * H) + ' C 60.5 ' + (0.458 * H) + ' 62.5 ' + (0.486 * H) + ' 62.5 ' + (0.52 * H)
          : 'M 49.5 ' + (0.46 * H) + ' C 55.5 ' + (0.465 * H) + ' 60.0 ' + (0.492 * H) + ' 60.8 ' + (0.525 * H);
        svgLayers += '<path d="' + neckD + '" fill="none" stroke="#4B3532" stroke-width="' + (ow * 3.1) + '" stroke-linecap="round" stroke-linejoin="round"/>';
        svgLayers += '<path d="' + neckD + '" fill="none" stroke="' + tubeColor + '" stroke-width="' + (ow * 1.75) + '" stroke-linecap="round" stroke-linejoin="round"/>';

        var dcx = isSide ? 61.0 : 62.6;
        var dcy = 0.528 * H;
        var dr = 4.5;
        if (scope === 'gold_vip') {
          svgLayers += '<circle cx="' + dcx + '" cy="' + dcy + '" r="' + dr + '" fill="#FFD54F" stroke="#4B3532" stroke-width="' + (ow * 0.85) + '"/>';
          svgLayers += svgStar(dcx, dcy, dr * 0.72, '#FFF8E1', ow * 0.55);
        } else if (scope === 'cat_bell') {
          var eo = dr * 0.62;
          svgLayers += '<polygon points="' + (dcx - eo * 1.1) + ',' + (dcy - dr * 0.4) + ' ' + (dcx - eo * 1.35) + ',' + (dcy - dr * 1.35) + ' ' + (dcx - eo * 0.25) + ',' + (dcy - dr * 0.85) + '" fill="#FFCDD2" stroke="#4B3532" stroke-width="' + (ow * 0.75) + '"/>';
          svgLayers += '<polygon points="' + (dcx + eo * 1.1) + ',' + (dcy - dr * 0.4) + ' ' + (dcx + eo * 1.35) + ',' + (dcy - dr * 1.35) + ' ' + (dcx + eo * 0.25) + ',' + (dcy - dr * 0.85) + '" fill="#FFCDD2" stroke="#4B3532" stroke-width="' + (ow * 0.75) + '"/>';
          svgLayers += '<circle cx="' + dcx + '" cy="' + dcy + '" r="' + dr + '" fill="#FFE082" stroke="#4B3532" stroke-width="' + (ow * 0.85) + '"/>';
          svgLayers += '<line x1="' + (dcx - dr * 0.48) + '" y1="' + (dcy + dr * 0.22) + '" x2="' + (dcx + dr * 0.48) + '" y2="' + (dcy + dr * 0.22) + '" stroke="#4B3532" stroke-width="' + (ow * 0.85) + '" stroke-linecap="round"/>';
          svgLayers += '<circle cx="' + dcx + '" cy="' + (dcy + dr * 0.1) + '" r="' + (dr * 0.18) + '" fill="#4B3532"/>';
        } else if (scope === 'boba_scope') {
          var cw = dr * 1.65, ch = dr * 1.9, ct = dcy - ch * 0.45, cl = dcx - cw / 2;
          svgLayers += '<line x1="' + (dcx + cw * 0.12) + '" y1="' + (ct + ch * 0.25) + '" x2="' + (dcx + cw * 0.24) + '" y2="' + (ct - ch * 0.42) + '" stroke="rgba(156,39,176,0.75)" stroke-width="' + (ow * 1.3) + '" stroke-linecap="round"/>';
          svgLayers += '<rect x="' + cl + '" y="' + ct + '" width="' + cw + '" height="' + ch + '" rx="2" fill="#E6C29F" stroke="#4B3532" stroke-width="' + (ow * 0.8) + '"/>';
          var pr = dr * 0.17;
          svgLayers += '<circle cx="' + (dcx - cw * 0.22) + '" cy="' + (ct + ch * 0.72) + '" r="' + pr + '" fill="#4B3532"/>';
          svgLayers += '<circle cx="' + dcx + '" cy="' + (ct + ch * 0.76) + '" r="' + pr + '" fill="#4B3532"/>';
          svgLayers += '<circle cx="' + (dcx + cw * 0.22) + '" cy="' + (ct + ch * 0.72) + '" r="' + pr + '" fill="#4B3532"/>';
        }
      }

      // 3. Headwear
      if (head === 'cat_ears') {
        var ew = 17, eh = 0.088 * H;
        var ears = !isSide ? [[34, 0.115 * H, -0.22], [66, 0.115 * H, 0.22]] : [[44, 0.112 * H, -0.15], [61, 0.12 * H, 0.18]];
        for (var ei = 0; ei < ears.length; ei++) {
          var ex = ears[ei][0], ey = ears[ei][1], tilt = ears[ei][2];
          var tipX = ex + ew * tilt, tipY = ey - eh;
          svgLayers += '<polygon points="' + (ex - ew * 0.55) + ',' + ey + ' ' + tipX + ',' + tipY + ' ' + (ex + ew * 0.55) + ',' + ey + '" fill="#F6F5FA" stroke="#4B3532" stroke-width="' + (ow * 1.05) + '" stroke-linejoin="round"/>';
          svgLayers += '<polygon points="' + (ex - ew * 0.28) + ',' + (ey - eh * 0.08) + ' ' + tipX + ',' + (ey - eh * 0.68) + ' ' + (ex + ew * 0.28) + ',' + (ey - eh * 0.08) + '" fill="#F8BBD0"/>';
        }
      } else if (head === 'head_mirror') {
        var bandY = 0.138 * H;
        var bandD = !isSide
          ? 'M 23 ' + (bandY + 4.5) + ' Q 50 ' + (bandY - 3.6) + ' 77 ' + (bandY + 4.5)
          : 'M 26 ' + (bandY + 3.6) + ' Q 52 ' + (bandY - 2.7) + ' 74 ' + (bandY + 4.0);
        svgLayers += '<path d="' + bandD + '" fill="none" stroke="#4B3532" stroke-width="' + (ow * 2.5) + '" stroke-linecap="round"/>';
        svgLayers += '<path d="' + bandD + '" fill="none" stroke="#DCE4EC" stroke-width="' + (ow * 1.4) + '" stroke-linecap="round"/>';
        var mx = isSide ? 62 : 48, my = isSide ? 0.128 * H : 0.125 * H, mr = 9.2;
        svgLayers += '<circle cx="' + mx + '" cy="' + my + '" r="' + mr + '" fill="#E3EEF8" stroke="#4B3532" stroke-width="' + (ow * 0.95) + '"/>';
        svgLayers += '<circle cx="' + mx + '" cy="' + my + '" r="' + (mr * 0.74) + '" fill="rgba(176,196,222,0.45)"/>';
        svgLayers += '<line x1="' + (mx - mr * 0.48) + '" y1="' + (my - mr * 0.35) + '" x2="' + (mx - mr * 0.12) + '" y2="' + (my - mr * 0.55) + '" stroke="#FFFFFF" stroke-width="' + (ow * 1.1) + '" stroke-linecap="round"/>';
        svgLayers += '<circle cx="' + mx + '" cy="' + my + '" r="' + (mr * 0.19) + '" fill="#4B3532"/>';
      } else if (head === 'flower_crown') {
        var cy = 0.105 * H;
        svgLayers += '<path d="M 26 ' + (cy + 5.76) + ' Q 50 ' + (cy - 3.6) + ' 74 ' + (cy + 5.76) + '" fill="none" stroke="#81C784" stroke-width="' + (ow * 2.0) + '" stroke-linecap="round"/>';
        var fls = !isSide
          ? [[30, cy + 3.6, '#FFCDD2', 4.4], [40, cy + 0.72, '#FFF9C4', 4.8], [50, cy - 0.72, '#FFFFFF', 5.2], [60, cy + 0.9, '#BBDEFB', 4.6], [70, cy + 3.6, '#FFCDD2', 4.4]]
          : [[36, cy + 2.7, '#FFCDD2', 4.6], [51, cy + 0.36, '#FFFFFF', 5.2], [66, cy + 2.88, '#BBDEFB', 4.6]];
        for (var fi = 0; fi < fls.length; fi++) {
          var fx = fls[fi][0], fy = fls[fi][1], fc = fls[fi][2], fr = fls[fi][3];
          for (var p = 0; p < 5; p++) {
            var pa = p * (2 * Math.PI / 5);
            var px = fx + fr * 0.55 * Math.cos(pa);
            var py = fy + fr * 0.55 * Math.sin(pa);
            svgLayers += '<circle cx="' + px.toFixed(1) + '" cy="' + py.toFixed(1) + '" r="' + (fr * 0.46).toFixed(1) + '" fill="' + fc + '" stroke="rgba(75,53,50,0.6)" stroke-width="' + (ow * 0.4) + '"/>';
          }
          svgLayers += '<circle cx="' + fx + '" cy="' + fy + '" r="' + (fr * 0.34).toFixed(1) + '" fill="#FFD54F"/>';
        }
      } else if (head === 'paw_clip') {
        var ccx = isSide ? 58.5 : 67.5, ccy = 0.165 * H;
        svgLayers += '<rect x="' + (ccx - 6.5) + '" y="' + (ccy - 0.9) + '" width="13" height="2.52" rx="1.2" fill="#E0E0E0" stroke="#4B3532" stroke-width="' + (ow * 0.65) + '"/>';
        svgLayers += svgPaw(ccx + 1.5, ccy - 0.72, 4.2, '#FFABBE', ow * 0.85);
      }
    }

    var svgTag = svgLayers
      ? '<svg viewBox="0 0 100 180" preserveAspectRatio="none" aria-hidden="true">' + svgLayers + '</svg>'
      : '';

    return (
      '<div class="' + cls + '" style="' + heightStyle + widthStyle + 'aspect-ratio:' + aspect + ';' + extraStyle + '">' +
      '<img src="' + esc(imgSrc) + '" alt="' + alt + '" draggable="false" />' +
      svgTag +
      '</div>'
    );
  }

  /* App Singleton Store */
  var state = loadState();
  var runtime = {
    activeTab: 'room', // room | prescriptions | pharmacy | memories | log
    // ROOM tab
    roomCategory: 'daily',
    stagePose: 'CALM',
    stageQuote: D.DOCTOR_QUOTES.firstVisitWelcomeQuote,
    stageSub: '',
    stagePatCount: 0,
    openTaskMenuId: null,
    // PRESCRIPTIONS tab
    rxSubMode: 'catalog', // catalog | gym
    rxEnergyFilter: 'e10',
    rxSelectedCategories: ['daily', 'game', 'homework'],
    rxPickedCard: null,
    rxPose: 'CALM',
    rxQuote: pickRandom(D.DOCTOR_QUOTES.prescriptionDoctorQuotes),
    rxPatCount: 0,
    // PHARMACY tab
    pharmacySubMode: 'supply', // supply | counseling
    pharmacyPose: 'PHARMACY',
    pharmacyQuote: pickRandom(D.DOCTOR_QUOTES.pharmacyQuotes),
    pharmacyPatCount: 0,
    // MEMORIES tab
    memoryPose: 'CALM',
    memoryQuote: pickRandom(D.DOCTOR_QUOTES.memoryDoctorQuotes),
    memoryPatCount: 0,
    // LOG tab
    logMode: 'LIST', // LIST | CALENDAR
    wardPose: state.wardDoctorSnapshot ? (state.wardDoctorSnapshot.pose || 'CALM') : 'CALM',
    wardSpeech: null,
    wardSpeechNonce: 0,
    calYear: new Date().getFullYear(),
    calMonth: new Date().getMonth() + 1,
    calSelectedDate: formatDateKey(),
    // Cleanups
    tabCleanupFns: []
  };

  var toastTimer = null;
  function showToast(msg) {
    var el = document.getElementById('snackbar');
    if (!el || !msg) return;
    el.textContent = msg;
    el.classList.add('show');
    if (toastTimer) clearTimeout(toastTimer);
    toastTimer = setTimeout(function () {
      el.classList.remove('show');
    }, 2800);
  }

  function openModal(html, opts) {
    var o = opts || {};
    var root = document.getElementById('modal-root');
    if (!root) return null;
    root.classList.remove('hidden', 'center');
    if (o.center) root.classList.add('center');
    root.setAttribute('aria-hidden', 'false');
    root.innerHTML = html;
    root.onclick = function (ev) {
      if (ev.target === root && o.dismissable !== false) {
        closeModal();
        if (typeof o.onClose === 'function') o.onClose();
      }
    };
    return root;
  }

  function closeModal() {
    var root = document.getElementById('modal-root');
    if (!root) return;
    root.classList.add('hidden');
    root.setAttribute('aria-hidden', 'true');
    root.innerHTML = '';
    root.onclick = null;
  }

  function openFullscreen(html) {
    var root = document.getElementById('fullscreen-root');
    if (!root) return null;
    root.classList.remove('hidden');
    root.setAttribute('aria-hidden', 'false');
    root.innerHTML = html;
    return root;
  }

  function closeFullscreen() {
    var root = document.getElementById('fullscreen-root');
    if (!root) return;
    root.classList.add('hidden');
    root.setAttribute('aria-hidden', 'true');
    root.innerHTML = '';
  }

  function setState(mutator, skipRender) {
    mutator(state);
    saveState(state);
    if (!skipRender && typeof window.REHAB_APP.rerender === 'function') {
      window.REHAB_APP.rerender();
    }
  }

  function registerCleanup(fn) {
    if (typeof fn === 'function') runtime.tabCleanupFns.push(fn);
  }

  function runTabCleanups() {
    while (runtime.tabCleanupFns.length) {
      var fn = runtime.tabCleanupFns.pop();
      try { fn(); } catch (e) { /* ignore */ }
    }
  }

  function maybeNotify(title, body, uniqueKey) {
    if (uniqueKey) {
      if (state.sentNotifKeys.indexOf(uniqueKey) >= 0) return false;
      state.sentNotifKeys.push(uniqueKey);
      if (state.sentNotifKeys.length > 100) {
        state.sentNotifKeys = state.sentNotifKeys.slice(-80);
      }
      saveState(state);
    }
    if (state.notificationsEnabled && typeof Notification !== 'undefined' && Notification.permission === 'granted') {
      try {
        new Notification(title, {
          body: body,
          icon: './assets/mascot_launcher_icon.png'
        });
        return true;
      } catch (e) { /* fallback toast */ }
    }
    showToast(title + '：' + body);
    return true;
  }

  window.REHAB_APP = {
    D: D,
    ICONS: ICONS,
    state: state,
    runtime: runtime,
    uid: uid,
    esc: esc,
    clamp: clamp,
    pad2: pad2,
    pickRandom: pickRandom,
    formatDateKey: formatDateKey,
    parseDateKey: parseDateKey,
    shiftDateKey: shiftDateKey,
    daysBetweenDateKeys: daysBetweenDateKeys,
    effectiveCompletionDateKey: effectiveCompletionDateKey,
    shouldResetTask: shouldResetTask,
    runAutoTaskResets: runAutoTaskResets,
    accrueIdlePointsOnState: accrueIdlePointsOnState,
    isEarly8AmStartTime: isEarly8AmStartTime,
    addMinutesToTime: addMinutesToTime,
    percentToEnergyLevel: percentToEnergyLevel,
    energyWireToPercent: energyWireToPercent,
    resolvePoseImage: resolvePoseImage,
    renderDoctorAvatarHtml: renderDoctorAvatarHtml,
    createDefaultState: createDefaultState,
    normalizeState: normalizeState,
    saveState: saveState,
    setState: setState,
    showToast: showToast,
    openModal: openModal,
    closeModal: closeModal,
    openFullscreen: openFullscreen,
    closeFullscreen: closeFullscreen,
    registerCleanup: registerCleanup,
    runTabCleanups: runTabCleanups,
    maybeNotify: maybeNotify,
    rerender: null
  };
})();
