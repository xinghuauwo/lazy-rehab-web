/* 懶癌康復室 — App Coordinator, Navigation Shell, Settings & Reminder Scheduler */
(function () {
  'use strict';

  var A = window.REHAB_APP;
  var D = window.REHAB_DATA;
  if (!A || !D) return;

  var NAV_ITEMS = [
    { key: 'room', label: '康復室', title: '懶癌康復室', icon: A.ICONS.room },
    { key: 'prescriptions', label: '療程', title: '微療程開立所', icon: A.ICONS.rx },
    { key: 'pharmacy', label: '藥局', title: '康復獎勵藥局', icon: A.ICONS.pharmacy },
    { key: 'memories', label: '回憶', title: '康復紀錄簿', icon: A.ICONS.memories },
    { key: 'log', label: '日記', title: '今日電量日記', icon: A.ICONS.log }
  ];

  var lastRenderedTab = A.runtime.activeTab;

  function renderNavContainers() {
    var cur = A.runtime.activeTab;
    var railEl = document.getElementById('nav-rail');
    var bottomEl = document.getElementById('bottom-nav');

    var navHtml = NAV_ITEMS.map(function (item) {
      var activeCls = item.key === cur ? ' active' : '';
      return (
        '<button type="button" class="nav-item' + activeCls + '" data-nav-tab="' + item.key + '" aria-label="' + A.esc(item.label) + '"' + (item.key === cur ? ' aria-current="page"' : '') + '>' +
        '<span class="nav-icon-pill">' + item.icon + '</span>' +
        '<span class="nav-label">' + A.esc(item.label) + '</span>' +
        '</button>'
      );
    }).join('');

    if (railEl) railEl.innerHTML = navHtml;
    if (bottomEl) bottomEl.innerHTML = navHtml;

    [railEl, bottomEl].forEach(function (root) {
      if (!root) return;
      root.querySelectorAll('[data-nav-tab]').forEach(function (btn) {
        btn.addEventListener('click', function () {
          var nextTab = btn.getAttribute('data-nav-tab');
          if (!nextTab || nextTab === A.runtime.activeTab) return;
          A.runtime.openTaskMenuId = null;
          A.runtime.activeTab = nextTab;
          rerender();
        });
      });
    });
  }

  function formatTemplate(tpl, arg1, arg2) {
    var out = String(tpl || '');
    if (arg1 !== undefined) out = out.replace('%s', String(arg1));
    if (arg2 !== undefined) out = out.replace('%s', String(arg2));
    return out;
  }

  function parseMinutesOfDay(timeStr) {
    var m = String(timeStr || '').trim().match(/^(\d{1,2})[:：](\d{2})$/);
    if (!m) return null;
    var h = parseInt(m[1], 10);
    var min = parseInt(m[2], 10);
    if (isNaN(h) || isNaN(min) || h < 0 || h > 23 || min < 0 || min > 59) return null;
    return h * 60 + min;
  }

  function runPeriodicTick(allowRerender) {
    var st = A.state;
    var changedReset = A.runAutoTaskResets(st);
    var gainedPoints = A.accrueIdlePointsOnState(st);
    if (changedReset || gainedPoints > 0) {
      A.saveState(st);
      if (allowRerender) rerender();
    }
    checkDoctorNotifications();
  }

  function checkDoctorNotifications() {
    var st = A.state;
    var now = new Date();
    var todayKey = A.formatDateKey(now);
    var nowMin = now.getHours() * 60 + now.getMinutes();
    var nick = st.feifeiNickname || '主治醫生';

    // 1. Homework due today
    (st.tasks || []).forEach(function (t) {
      if (t.category === 'homework' && !t.completed && t.deadlineDate === todayKey) {
        var tpl = A.pickRandom(D.DOCTOR_QUOTES.notificationDeadlineQuotes);
        var body = formatTemplate(tpl, t.title);
        A.maybeNotify(nick + '的死線提醒', body, 'hw_due_' + t.id + '_' + todayKey);
      }
    });

    // 2. Calendar timed events today (-30m and AT_TIME)
    (st.calendarEvents || []).forEach(function (ev) {
      if (ev.dateKey !== todayKey || ev.allDay) return;
      var startMin = parseMinutesOfDay(ev.startTime);
      if (startMin == null) return;
      var diff = startMin - nowMin;
      if (diff >= 25 && diff <= 30) {
        var tpl30 = A.pickRandom(D.DOCTOR_QUOTES.notificationBefore30MinQuotes);
        A.maybeNotify(
          nick + '的行程預告',
          formatTemplate(tpl30, ev.title, ev.startTime),
          'ev_30m_' + ev.id + '_' + todayKey
        );
      } else if (diff <= 0 && diff >= -4) {
        var tplAt = A.pickRandom(D.DOCTOR_QUOTES.notificationAtTimeQuotes);
        A.maybeNotify(
          nick + '的時間到囉',
          formatTemplate(tplAt, ev.title, ev.startTime),
          'ev_at_' + ev.id + '_' + todayKey
        );
      }
    });

    // 3. Weekly timetable classes / shifts today (-30m and AT_TIME)
    var tt = st.timetable;
    if (tt && Array.isArray(tt.periods) && tt.cells) {
      var todayIdx = (now.getDay() + 6) % 7; // 0 = Mon ... 6 = Sun
      tt.periods.forEach(function (p) {
        var rawCell = tt.cells[p.id + '_' + todayIdx];
        var cellText = rawCell && typeof rawCell === 'object' ? String(rawCell.title || '').trim() : String(rawCell || '').trim();
        if (!cellText) return;
        var label = cellText.split(/\r?\n/)[0].trim() || cellText;
        var startMin = parseMinutesOfDay(p.startTime);
        if (startMin == null) return;
        var diff = startMin - nowMin;
        if (diff >= 25 && diff <= 30) {
          var tpl30 = A.pickRandom(D.DOCTOR_QUOTES.notificationBefore30MinQuotes);
          A.maybeNotify(
            nick + '的課表 / 班表提醒',
            formatTemplate(tpl30, label, p.startTime),
            'tt_30m_' + p.id + '_' + todayKey + '_' + label
          );
        } else if (diff <= 0 && diff >= -4) {
          var tplAt = A.pickRandom(D.DOCTOR_QUOTES.notificationAtTimeQuotes);
          A.maybeNotify(
            nick + '的時段提醒',
            formatTemplate(tplAt, label, p.startTime),
            'tt_at_' + p.id + '_' + todayKey + '_' + label
          );
        }
      });
    }
  }

  function openSettingsModal() {
    var st = A.state;
    var hasNotifApi = typeof Notification !== 'undefined';
    var perm = hasNotifApi ? Notification.permission : 'unsupported';
    var permLabel = perm === 'granted' ? '已允許瀏覽器推播'
      : perm === 'denied' ? '瀏覽器已封鎖推播（僅站內氣泡提示）'
      : perm === 'default' ? '尚未授權瀏覽器推播'
      : '此裝置不支援系統推播（仍支援站內提醒）';

    var html =
      '<div class="dialog-panel">' +
      '<div style="display:flex;align-items:center;justify-content:space-between;gap:12px">' +
      '<div class="dialog-title">掛號病歷與陪伴設定</div>' +
      '<button type="button" class="icon-btn" id="st-close-x" aria-label="關閉">' + A.ICONS.close + '</button>' +
      '</div>' +

      '<div>' +
      '<label class="field-label" for="st-nick-input">陪伴醫生稱呼</label>' +
      '<input id="st-nick-input" class="input-field" type="text" maxlength="16" value="' + A.esc(st.feifeiNickname) + '" placeholder="主治醫生" />' +
      '<div style="font-size:11.5px;color:var(--on-surface-variant);margin-top:4px">修改後會同步顯示於對話框與行程通知標題。</div>' +
      '</div>' +

      '<div style="padding:12px 14px;border-radius:16px;background:var(--surface-container-low);border:1px solid var(--outline-variant)">' +
      '<div style="display:flex;align-items:center;justify-content:space-between;gap:10px">' +
      '<div>' +
      '<div style="font-size:14px;font-weight:700">醫生行程與死線提醒</div>' +
      '<div style="font-size:11.5px;color:var(--on-surface-variant);margin-top:2px">' + A.esc(permLabel) + '</div>' +
      '</div>' +
      '<button type="button" class="' + (st.notificationsEnabled ? 'pill-btn' : 'tonal-btn') + '" id="st-notif-toggle">' +
      (st.notificationsEnabled ? '已開啟' : '開啟提醒') +
      '</button>' +
      '</div>' +
      '<div style="margin-top:10px;display:flex;gap:8px;flex-wrap:wrap">' +
      '<button type="button" class="outlined-btn" id="st-notif-test" style="min-height:36px;font-size:12.5px">' + A.ICONS.bell + ' 發送測試通知</button>' +
      '</div>' +
      '</div>' +

      '<div style="padding:12px 14px;border-radius:16px;background:var(--surface-container-low);border:1px solid var(--outline-variant);display:flex;flex-direction:column;gap:8px">' +
      '<div style="font-size:14px;font-weight:700">單機病歷備份（JSON）</div>' +
      '<div style="font-size:12px;color:var(--on-surface-variant)">所有療程、日記、課表、復健點數與病房擺設皆儲存在此瀏覽器本機；跨裝置換機可先匯出檔案。</div>' +
      '<div style="display:flex;gap:8px;flex-wrap:wrap;margin-top:2px">' +
      '<button type="button" class="tonal-btn" id="st-export-btn" style="flex:1;min-width:120px">匯出存檔</button>' +
      '<label class="outlined-btn" style="flex:1;min-width:120px;cursor:pointer">' +
      '匯入存檔<input type="file" id="st-import-file" accept="application/json,.json" style="display:none" />' +
      '</label>' +
      '</div>' +
      '</div>' +

      '<div style="display:flex;align-items:center;justify-content:space-between;gap:10px;padding-top:4px;border-top:1px solid var(--outline-variant)">' +
      '<button type="button" class="outlined-btn" id="st-reset-all" style="color:var(--error);border-color:rgba(179,38,30,0.35)">重置全部資料</button>' +
      '<button type="button" class="pill-btn" id="st-save-btn">儲存設定</button>' +
      '</div>' +
      '</div>';

    var root = A.openModal(html, { center: true });
    if (!root) return;

    root.querySelector('#st-close-x').addEventListener('click', A.closeModal);

    root.querySelector('#st-notif-toggle').addEventListener('click', function () {
      var nextVal = !A.state.notificationsEnabled;
      if (nextVal && typeof Notification !== 'undefined' && Notification.permission === 'default') {
        Notification.requestPermission().then(function () {
          A.setState(function (s) { s.notificationsEnabled = true; }, true);
          openSettingsModal();
        });
        return;
      }
      A.setState(function (s) { s.notificationsEnabled = nextVal; }, true);
      if (nextVal && typeof Notification !== 'undefined' && Notification.permission === 'denied') {
        A.showToast('瀏覽器已阻擋系統推播，將改用站內提示條提醒你。');
      }
      openSettingsModal();
    });

    root.querySelector('#st-notif-test').addEventListener('click', function () {
      var nick = (root.querySelector('#st-nick-input').value || '').trim() || '主治醫生';
      if (typeof Notification !== 'undefined' && Notification.permission === 'default') {
        Notification.requestPermission().then(function (res) {
          A.setState(function (s) { s.notificationsEnabled = true; }, true);
          if (res === 'granted') {
            new Notification(nick + '的測試掛號訊息', {
              body: '通知線路暢通！死線當天與行程前 30 分鐘我都會呼叫你。',
              icon: './assets/mascot_launcher_icon.png'
            });
          } else {
            A.showToast(nick + '：通知線路暢通（目前為站內提醒模式）。');
          }
          openSettingsModal();
        });
      } else if (typeof Notification !== 'undefined' && Notification.permission === 'granted') {
        new Notification(nick + '的測試掛號訊息', {
          body: '通知線路暢通！死線當天與行程前 30 分鐘我都會呼叫你。',
          icon: './assets/mascot_launcher_icon.png'
        });
        A.showToast('已發送測試推播通知。');
      } else {
        A.showToast(nick + '：收到！提醒功能已就緒。');
      }
    });

    root.querySelector('#st-export-btn').addEventListener('click', function () {
      try {
        var json = JSON.stringify(A.state, null, 2);
        var blob = new Blob([json], { type: 'application/json' });
        var url = URL.createObjectURL(blob);
        var a = document.createElement('a');
        a.href = url;
        a.download = 'lazy_rehab_backup_' + A.formatDateKey() + '.json';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        A.showToast('已匯出單機病歷備份檔');
      } catch (e) {
        A.showToast('匯出失敗，請重試');
      }
    });

    root.querySelector('#st-import-file').addEventListener('change', function (ev) {
      var file = ev.target.files && ev.target.files[0];
      if (!file) return;
      var reader = new FileReader();
      reader.onload = function () {
        try {
          var parsed = JSON.parse(String(reader.result || '{}'));
          if (!parsed || typeof parsed !== 'object') throw new Error('invalid');
          var normalized = A.normalizeState(parsed);
          A.runAutoTaskResets(normalized);
          A.accrueIdlePointsOnState(normalized);
          Object.keys(A.state).forEach(function (k) { delete A.state[k]; });
          Object.assign(A.state, normalized);
          A.saveState(A.state);
          A.closeModal();
          rerender();
          A.showToast('已成功匯入並還原病歷存檔！');
        } catch (err) {
          A.showToast('存檔格式無效，無法匯入');
        }
      };
      reader.readAsText(file);
    });

    root.querySelector('#st-reset-all').addEventListener('click', function () {
      if (!window.confirm('確定要清空所有療程、日記、復健點數與病房佈置，重新掛號嗎？')) return;
      var fresh = A.createDefaultState();
      Object.keys(A.state).forEach(function (k) { delete A.state[k]; });
      Object.assign(A.state, fresh);
      A.saveState(A.state);
      A.closeModal();
      rerender();
      A.showToast('已重設為全新病歷檔案');
    });

    root.querySelector('#st-save-btn').addEventListener('click', function () {
      var nextNick = (root.querySelector('#st-nick-input').value || '').trim() || '主治醫生';
      A.setState(function (s) {
        s.feifeiNickname = nextNick;
      });
      A.closeModal();
      A.showToast('已更新陪伴設定');
    });
  }

  function rerender() {
    A.runTabCleanups();

    var curTab = A.runtime.activeTab || 'room';
    var meta = NAV_ITEMS.filter(function (x) { return x.key === curTab; })[0] || NAV_ITEMS[0];

    var titleEl = document.getElementById('top-bar-title');
    if (titleEl) {
      titleEl.textContent = (curTab === 'pharmacy' && A.runtime.pharmacySubMode === 'counseling')
        ? '心理診療室'
        : meta.title;
    }

    var fab = document.getElementById('fab-add-task');
    if (fab) {
      if (curTab === 'room') fab.classList.remove('hidden');
      else fab.classList.add('hidden');
    }

    var mainScroll = document.getElementById('main-scroll');
    var container = document.getElementById('tab-content');
    if (mainScroll && container) {
      var isGym = curTab === 'prescriptions' && A.runtime.rxSubMode === 'gym';
      var isWardFull = curTab === 'log' && A.runtime.logMode === 'LIST';
      mainScroll.classList.toggle('no-pad-gym', isGym);
      container.classList.toggle('gym-layout', isGym);
      container.classList.toggle('full-bleed-log', isWardFull);
    }

    renderNavContainers();

    if (container) {
      if (curTab === 'room' && typeof A.renderRoomTab === 'function') {
        A.renderRoomTab(container);
      } else if (curTab === 'prescriptions' && typeof A.renderPrescriptionsTab === 'function') {
        A.renderPrescriptionsTab(container);
      } else if (curTab === 'pharmacy' && typeof A.renderPharmacyTab === 'function') {
        A.renderPharmacyTab(container);
      } else if (curTab === 'memories' && typeof A.renderMemoriesTab === 'function') {
        A.renderMemoriesTab(container);
      } else if (curTab === 'log' && typeof A.renderLogTab === 'function') {
        A.renderLogTab(container);
      }
    }

    if (curTab !== lastRenderedTab && mainScroll) {
      mainScroll.scrollTop = 0;
    }
    lastRenderedTab = curTab;
  }

  function init() {
    A.rerender = rerender;

    var settingsBtn = document.getElementById('btn-account-settings');
    if (settingsBtn) {
      settingsBtn.innerHTML = A.ICONS.settings;
      settingsBtn.addEventListener('click', openSettingsModal);
    }

    var fab = document.getElementById('fab-add-task');
    if (fab) {
      var fabIcon = fab.querySelector('.fab-icon');
      if (fabIcon) fabIcon.innerHTML = A.ICONS.plus;
      fab.addEventListener('click', function () {
        if (typeof A.openTaskEditorModal === 'function') {
          A.openTaskEditorModal(null, A.runtime.roomCategory);
        }
      });
    }

    // Close floating task dropdown menu on outside clicks
    document.addEventListener('click', function (ev) {
      if (!A.runtime.openTaskMenuId) return;
      var target = ev.target;
      if (target && typeof target.closest === 'function' && target.closest('.task-menu-wrap')) return;
      A.runtime.openTaskMenuId = null;
      rerender();
    });

    rerender();
    runPeriodicTick(false);

    setInterval(function () {
      runPeriodicTick(true);
    }, 30000);

    document.addEventListener('visibilitychange', function () {
      if (!document.hidden) {
        runPeriodicTick(true);
      }
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
