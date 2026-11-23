/* 懶癌康復室 — Tab 1 (康復室: Stage, Categories, Tasks, Drag Reorder, Timetable, Equip Sheet) */
(function () {
  'use strict';

  var A = window.REHAB_APP;
  var D = A.D;
  var ICONS = A.ICONS;

  function allCategories() {
    return D.BUILTIN_CATEGORIES.concat(A.state.customCategories || []);
  }

  function visibleCategories() {
    var hidden = A.state.hiddenCategories || [];
    var vis = allCategories().filter(function (c) { return hidden.indexOf(c.wireValue) < 0; });
    return vis.length ? vis : [D.BUILTIN_CATEGORIES[0]];
  }

  function getCategoryLabel(wire) {
    var found = allCategories().filter(function (c) { return c.wireValue === wire; })[0];
    return found ? found.labelZh : '日常';
  }

  function ensureActiveCategory() {
    var vis = visibleCategories();
    if (!vis.some(function (c) { return c.wireValue === A.runtime.roomCategory; })) {
      A.runtime.roomCategory = vis[0].wireValue;
    }
    return vis;
  }

  function formatDeadlineChip(deadlineDate, todayKey) {
    if (!deadlineDate) return null;
    var diff = A.daysBetweenDateKeys(todayKey, deadlineDate);
    var parts = A.parseDateKey(deadlineDate);
    var shortDate = parts.month + '/' + parts.day;
    if (diff < 0) return { text: shortDate + ' 已逾期 ' + Math.abs(diff) + ' 天', urgent: true };
    if (diff === 0) return { text: shortDate + ' 今天截止！', urgent: true };
    if (diff === 1) return { text: shortDate + ' 明天截止', urgent: true };
    return { text: shortDate + ' 截止（剩 ' + diff + ' 天）', urgent: diff <= 3 };
  }

  function isHomeworkUrgent(task, todayKey) {
    if (task.completed || !task.deadlineDate) return false;
    return A.daysBetweenDateKeys(todayKey, task.deadlineDate) <= 1;
  }

  function handleStagePat() {
    var poses = ['CALM', 'APPROVE', 'SIPPING', 'PHARMACY'];
    var idx = poses.indexOf(A.runtime.stagePose);
    A.runtime.stagePose = poses[(idx + 1) % poses.length];
    A.runtime.stageQuote = A.pickRandom(D.DOCTOR_QUOTES.all, A.runtime.stageQuote);
    A.runtime.stageSub = '';
    A.runtime.stagePatCount += 1;
    A.rerender();
    var wrap = document.querySelector('.stage-doctor-wrap');
    if (wrap) {
      wrap.classList.add('squish');
      setTimeout(function () { wrap.classList.remove('squish'); }, 220);
    }
  }

  function toggleTaskComplete(taskId) {
    var st = A.state;
    var target = st.tasks.filter(function (t) { return t.id === taskId; })[0];
    if (!target) return;

    var now = new Date();
    var nextCompleted = !target.completed;
    var dateKey = nextCompleted ? A.effectiveCompletionDateKey(target.category, target.resetHour, now) : '';
    var nowMs = nextCompleted ? Date.now() : 0;

    A.setState(function (s) {
      s.tasks = s.tasks.map(function (t) {
        if (t.id !== taskId) return t;
        return Object.assign({}, t, {
          completed: nextCompleted,
          completedDate: dateKey,
          completedAtMs: nowMs,
          morningDone: t.timeSlotsEnabled ? nextCompleted : false,
          noonDone: t.timeSlotsEnabled ? nextCompleted : false,
          eveningDone: t.timeSlotsEnabled ? nextCompleted : false
        });
      });
      if (nextCompleted) s.totalCompleted += 1;
    }, true);

    if (nextCompleted) {
      var pool = D.DOCTOR_QUOTES.taskCompleteQuotesByEffort[target.effortLevel] || D.DOCTOR_QUOTES.taskCompleteQuotesByEffort.micro;
      A.runtime.stagePose = 'APPROVE';
      A.runtime.stageQuote = A.pickRandom(pool, A.runtime.stageQuote);
      A.runtime.stageSub = '已記錄為本院第 ' + A.state.totalCompleted + ' 項康復實績';
      A.showToast('已完成「' + target.title + '」');
    } else {
      A.runtime.stagePose = 'CALM';
      A.runtime.stageQuote = A.pickRandom(D.DOCTOR_QUOTES.taskUncheckQuotes, A.runtime.stageQuote);
      A.runtime.stageSub = '';
    }
    A.rerender();
  }

  function toggleTaskSlot(taskId, slotKey) {
    var target = A.state.tasks.filter(function (t) { return t.id === taskId; })[0];
    if (!target) return;

    var nextM = slotKey === 'm' ? !target.morningDone : target.morningDone;
    var nextN = slotKey === 'n' ? !target.noonDone : target.noonDone;
    var nextE = slotKey === 'e' ? !target.eveningDone : target.eveningDone;

    var mReady = !target.morningEnabled || nextM;
    var nReady = !target.noonEnabled || nextN;
    var eReady = !target.eveningEnabled || nextE;
    var allDone = mReady && nReady && eReady;
    var wasCompleted = !!target.completed;
    var now = new Date();

    A.setState(function (s) {
      s.tasks = s.tasks.map(function (t) {
        if (t.id !== taskId) return t;
        return Object.assign({}, t, {
          morningDone: nextM,
          noonDone: nextN,
          eveningDone: nextE,
          completed: allDone,
          completedDate: allDone ? (t.completedDate || A.effectiveCompletionDateKey(t.category, t.resetHour, now)) : '',
          completedAtMs: allDone ? (t.completedAtMs || Date.now()) : 0
        });
      });
      if (!wasCompleted && allDone) s.totalCompleted += 1;
    }, true);

    if (!wasCompleted && allDone) {
      A.runtime.stagePose = 'APPROVE';
      A.runtime.stageQuote = A.pickRandom(D.DOCTOR_QUOTES.taskCompleteQuotesByEffort[target.effortLevel] || D.DOCTOR_QUOTES.all);
      A.runtime.stageSub = '早中晚時段全部打卡完成！';
      A.showToast('「' + target.title + '」今日時段全數完成');
    }
    A.rerender();
  }

  function reorderCategoryTasks(categoryWire, orderedIds) {
    A.setState(function (s) {
      var orderMap = {};
      orderedIds.forEach(function (id, idx) { orderMap[id] = idx; });
      var otherMax = 0;
      s.tasks = s.tasks.map(function (t) {
        if (t.category === categoryWire && orderMap[t.id] != null) {
          return Object.assign({}, t, { sortOrder: orderMap[t.id] });
        }
        return t;
      });
      // Keep stable sort by sortOrder
      s.tasks.sort(function (a, b) {
        if (a.category === categoryWire && b.category === categoryWire) {
          return a.sortOrder - b.sortOrder;
        }
        return (a.sortOrder || otherMax) - (b.sortOrder || otherMax);
      });
    }, false);
  }

  /* ===================== MODALS ===================== */

  function openDoctorEquipSheet() {
    function renderSheet() {
      var gear = A.state.doctorGear;
      var coatList = D.DOCTOR_COATS.map(function (c) {
        var sel = gear.coatId === c.id;
        return '<button type="button" class="gear-option' + (sel ? ' selected' : '') + '" data-coat="' + A.esc(c.id) + '">' +
          '<div style="flex:1;min-width:0">' +
            '<div style="display:flex;align-items:center;gap:6px;font-weight:700;font-size:14px">' +
              '<span>' + A.esc(c.titleZh) + '</span>' +
              '<span class="pill pill-secondary" style="font-size:10px;padding:2px 8px">' + A.esc(c.badgeZh) + '</span>' +
            '</div>' +
            '<div style="font-size:12px;color:var(--on-surface-variant);margin-top:2px">' + A.esc(c.descriptionZh) + '</div>' +
          '</div>' +
          (sel ? '<span style="color:var(--primary)">' + ICONS.check + '</span>' : '') +
        '</button>';
      }).join('');

      var scopeList = D.DOCTOR_STETHOSCOPES.map(function (s) {
        var sel = gear.stethoscopeId === s.id;
        return '<button type="button" class="gear-option' + (sel ? ' selected' : '') + '" data-scope="' + A.esc(s.id) + '">' +
          '<div style="flex:1;min-width:0">' +
            '<div style="display:flex;align-items:center;gap:6px;font-weight:700;font-size:14px">' +
              '<span>' + A.esc(s.titleZh) + '</span>' +
              '<span class="pill pill-primary" style="font-size:10px;padding:2px 8px">' + A.esc(s.badgeZh) + '</span>' +
            '</div>' +
            '<div style="font-size:12px;color:var(--on-surface-variant);margin-top:2px">' + A.esc(s.descriptionZh) + '</div>' +
          '</div>' +
          (sel ? '<span style="color:var(--primary)">' + ICONS.check + '</span>' : '') +
        '</button>';
      }).join('');

      var headList = D.DOCTOR_HEADWEARS.map(function (h) {
        var sel = gear.headwearId === h.id;
        return '<button type="button" class="gear-option' + (sel ? ' selected' : '') + '" data-head="' + A.esc(h.id) + '">' +
          '<div style="flex:1;min-width:0">' +
            '<div style="display:flex;align-items:center;gap:6px;font-weight:700;font-size:14px">' +
              '<span>' + A.esc(h.titleZh) + '</span>' +
              '<span class="pill pill-tertiary" style="font-size:10px;padding:2px 8px">' + A.esc(h.badgeZh) + '</span>' +
            '</div>' +
            '<div style="font-size:12px;color:var(--on-surface-variant);margin-top:2px">' + A.esc(h.descriptionZh) + '</div>' +
          '</div>' +
          (sel ? '<span style="color:var(--primary)">' + ICONS.check + '</span>' : '') +
        '</button>';
      }).join('');

      var html =
        '<div class="sheet-panel">' +
          '<div class="sheet-handle"></div>' +
          '<div style="display:flex;align-items:center;justify-content:space-between">' +
            '<div>' +
              '<div style="font-size:18px;font-weight:700">幫' + A.esc(A.state.feifeiNickname) + '換裝</div>' +
              '<div style="font-size:12px;color:var(--on-surface-variant)">切換白袍、聽診器與可愛頭飾，復健室與病房會同步換裝</div>' +
            '</div>' +
            '<button type="button" class="icon-btn" id="equip-close-btn" aria-label="關閉">' + ICONS.close + '</button>' +
          '</div>' +
          '<div style="display:flex;align-items:center;gap:14px;padding:12px;border-radius:20px;background:var(--surface-low)">' +
            '<div style="width:92px;height:142px;display:flex;align-items:center;justify-content:center;background:#FFFDF4;border-radius:16px;border:1px solid var(--outline-variant)">' +
              A.renderDoctorAvatarHtml(A.resolvePoseImage('CALM'), gear, { height: '132px', alt: A.state.feifeiNickname }) +
            '</div>' +
            '<div style="flex:1;font-size:13px;font-weight:600;color:var(--on-surface);line-height:1.45" id="equip-reaction-text">「' + A.esc(A.pickRandom(D.DOCTOR_QUOTES.doctorEquipReactions)) + '」</div>' +
          '</div>' +
          '<div class="field-group"><div class="field-label">醫師白袍</div>' + coatList + '</div>' +
          '<div class="field-group"><div class="field-label">聽診器款式</div>' + scopeList + '</div>' +
          '<div class="field-group"><div class="field-label">頭飾配件</div>' + headList + '</div>' +
          '<button type="button" class="btn btn-primary btn-full" id="equip-done-btn">完成換裝</button>' +
        '</div>';

      var root = A.openModal(html);
      root.querySelector('#equip-close-btn').onclick = A.closeModal;
      root.querySelector('#equip-done-btn').onclick = function () {
        A.closeModal();
        A.rerender();
      };
      root.querySelectorAll('[data-coat]').forEach(function (btn) {
        btn.onclick = function () {
          var id = btn.getAttribute('data-coat');
          A.setState(function (s) { s.doctorGear.coatId = id; }, true);
          renderSheet();
        };
      });
      root.querySelectorAll('[data-scope]').forEach(function (btn) {
        btn.onclick = function () {
          var id = btn.getAttribute('data-scope');
          A.setState(function (s) { s.doctorGear.stethoscopeId = id; }, true);
          renderSheet();
        };
      });
      root.querySelectorAll('[data-head]').forEach(function (btn) {
        btn.onclick = function () {
          var id = btn.getAttribute('data-head');
          A.setState(function (s) { s.doctorGear.headwearId = id; }, true);
          renderSheet();
        };
      });
    }
    renderSheet();
  }

  function openCategoryManagerModal() {
    function renderMgr() {
      var hidden = A.state.hiddenCategories || [];
      var builtins = D.BUILTIN_CATEGORIES.map(function (c) {
        var shown = hidden.indexOf(c.wireValue) < 0;
        return '<div style="display:flex;align-items:center;justify-content:space-between;padding:10px 12px;border-radius:14px;background:var(--surface-low)">' +
          '<span style="font-weight:700;font-size:14px">' + A.esc(c.labelZh) + '</span>' +
          '<button type="button" class="toggle-switch' + (shown ? ' on' : '') + '" data-toggle-cat="' + A.esc(c.wireValue) + '" aria-label="切換顯示"></button>' +
        '</div>';
      }).join('');

      var customs = (A.state.customCategories || []).map(function (c) {
        return '<div style="display:flex;align-items:center;justify-content:space-between;padding:10px 12px;border-radius:14px;background:var(--surface-lowest);border:1px solid var(--outline-variant)">' +
          '<span style="font-weight:700;font-size:14px">' + A.esc(c.labelZh) + '</span>' +
          '<button type="button" class="icon-btn" data-del-cat="' + A.esc(c.wireValue) + '" aria-label="刪除分類" style="color:var(--primary)">' + ICONS.trash + '</button>' +
        '</div>';
      }).join('');

      var html =
        '<div class="dialog-panel">' +
          '<div style="display:flex;align-items:center;justify-content:space-between">' +
            '<div style="font-size:18px;font-weight:700">管理療程分類</div>' +
            '<button type="button" class="icon-btn" id="catmgr-close">' + ICONS.close + '</button>' +
          '</div>' +
          '<div class="field-group"><div class="field-label">顯示預設分類（至少保留一項）</div>' + builtins + '</div>' +
          '<div class="field-group"><div class="field-label">自訂分類</div>' +
            (customs || '<div style="font-size:12px;color:var(--on-surface-variant)">尚未建立自訂分類</div>') +
            '<div style="display:flex;gap:8px;margin-top:4px">' +
              '<input type="text" class="input-box" id="new-cat-name" maxlength="10" placeholder="例如：閱讀、拉伸、繪圖" />' +
              '<button type="button" class="btn btn-primary" id="add-cat-btn" style="flex-shrink:0">新增</button>' +
            '</div>' +
          '</div>' +
        '</div>';

      var root = A.openModal(html, { center: true });
      root.querySelector('#catmgr-close').onclick = A.closeModal;
      root.querySelectorAll('[data-toggle-cat]').forEach(function (btn) {
        btn.onclick = function () {
          var wire = btn.getAttribute('data-toggle-cat');
          var vis = visibleCategories();
          var isCurrentlyShown = A.state.hiddenCategories.indexOf(wire) < 0;
          if (isCurrentlyShown && vis.length <= 1) {
            A.showToast('至少需要保留一個顯示中的分類喔');
            return;
          }
          A.setState(function (s) {
            if (isCurrentlyShown) {
              if (s.hiddenCategories.indexOf(wire) < 0) s.hiddenCategories.push(wire);
            } else {
              s.hiddenCategories = s.hiddenCategories.filter(function (x) { return x !== wire; });
            }
          }, false);
          renderMgr();
        };
      });
      root.querySelectorAll('[data-del-cat]').forEach(function (btn) {
        btn.onclick = function () {
          var wire = btn.getAttribute('data-del-cat');
          A.setState(function (s) {
            s.customCategories = s.customCategories.filter(function (c) { return c.wireValue !== wire; });
            s.tasks = s.tasks.filter(function (t) { return t.category !== wire; });
          }, false);
          renderMgr();
        };
      });
      root.querySelector('#add-cat-btn').onclick = function () {
        var val = root.querySelector('#new-cat-name').value.trim();
        if (!val) return;
        var wire = 'custom_' + Date.now().toString(36);
        A.setState(function (s) {
          s.customCategories.push({ wireValue: wire, labelZh: val, isBuiltIn: false });
        }, false);
        A.runtime.roomCategory = wire;
        renderMgr();
      };
    }
    renderMgr();
  }

  function openGoogleDeadlinePicker(currentDateKey, onPicked) {
    var todayKey = A.formatDateKey();
    var p = A.parseDateKey(currentDateKey || todayKey);
    var viewYear = p.year;
    var viewMonth = p.month;
    var selected = currentDateKey || '';

    function renderCal() {
      var firstDow = new Date(viewYear, viewMonth - 1, 1).getDay();
      var daysInMonth = new Date(viewYear, viewMonth, 0).getDate();
      var weekdays = ['日', '一', '二', '三', '四', '五', '六'];
      var cellsHtml = '';
      for (var b = 0; b < firstDow; b++) {
        cellsHtml += '<div></div>';
      }
      for (var d = 1; d <= daysInMonth; d++) {
        var dk = viewYear + '-' + A.pad2(viewMonth) + '-' + A.pad2(d);
        var isSel = selected === dk;
        var isToday = dk === todayKey;
        cellsHtml += '<button type="button" class="cal-cell' + (isSel ? ' selected' : isToday ? ' has-record' : '') + '" data-pick-dk="' + dk + '">' +
          '<span>' + d + '</span>' +
          (isToday ? '<span class="cal-dot"></span>' : '') +
        '</button>';
      }

      var fridayDelta = (5 - new Date().getDay() + 7) % 7;
      var thisFriday = A.shiftDateKey(todayKey, fridayDelta);
      var nextMonDelta = (8 - new Date().getDay()) % 7 || 7;
      var nextMonday = A.shiftDateKey(todayKey, nextMonDelta);

      var html =
        '<div class="dialog-panel">' +
          '<div style="display:flex;align-items:center;justify-content:space-between">' +
            '<div style="font-size:18px;font-weight:700">選擇截止日期</div>' +
            '<button type="button" class="icon-btn" id="dp-close">' + ICONS.close + '</button>' +
          '</div>' +
          '<div class="chip-row">' +
            '<button type="button" class="filter-chip" data-quick-dk="' + todayKey + '">今天</button>' +
            '<button type="button" class="filter-chip" data-quick-dk="' + A.shiftDateKey(todayKey, 1) + '">明天</button>' +
            '<button type="button" class="filter-chip" data-quick-dk="' + thisFriday + '">本週五</button>' +
            '<button type="button" class="filter-chip" data-quick-dk="' + nextMonday + '">下週一</button>' +
          '</div>' +
          '<div class="cal-header">' +
            '<button type="button" class="icon-btn" id="dp-prev">' + ICONS.chevLeft + '</button>' +
            '<span style="font-weight:700;font-size:16px">' + viewYear + ' 年 ' + viewMonth + ' 月</span>' +
            '<button type="button" class="icon-btn" id="dp-next">' + ICONS.chevRight + '</button>' +
          '</div>' +
          '<div class="cal-weekdays">' + weekdays.map(function (w) { return '<div class="cal-weekday">' + w + '</div>'; }).join('') + '</div>' +
          '<div class="cal-grid">' + cellsHtml + '</div>' +
          '<div style="display:flex;gap:8px;justify-content:flex-end;margin-top:4px">' +
            '<button type="button" class="btn btn-text" id="dp-clear">清除死線</button>' +
            '<button type="button" class="btn btn-primary" id="dp-confirm">確定</button>' +
          '</div>' +
        '</div>';

      var root = A.openModal(html, { center: true });
      root.querySelector('#dp-close').onclick = function () { onPicked(currentDateKey); };
      root.querySelector('#dp-clear').onclick = function () { onPicked(''); };
      root.querySelector('#dp-confirm').onclick = function () { onPicked(selected); };
      root.querySelector('#dp-prev').onclick = function () {
        if (viewMonth === 1) { viewYear -= 1; viewMonth = 12; } else viewMonth -= 1;
        renderCal();
      };
      root.querySelector('#dp-next').onclick = function () {
        if (viewMonth === 12) { viewYear += 1; viewMonth = 1; } else viewMonth += 1;
        renderCal();
      };
      root.querySelectorAll('[data-quick-dk]').forEach(function (btn) {
        btn.onclick = function () { onPicked(btn.getAttribute('data-quick-dk')); };
      });
      root.querySelectorAll('[data-pick-dk]').forEach(function (btn) {
        btn.onclick = function () {
          selected = btn.getAttribute('data-pick-dk');
          renderCal();
        };
      });
    }
    renderCal();
  }

  function openTaskEditorModal(existingTask, defaultCat) {
    var isEdit = !!existingTask;
    var draft = {
      title: existingTask ? existingTask.title : '',
      note: existingTask ? existingTask.note : '',
      category: existingTask ? existingTask.category : (defaultCat || A.runtime.roomCategory || 'daily'),
      effortLevel: existingTask ? existingTask.effortLevel : 'micro',
      resetHour: existingTask ? existingTask.resetHour : 4,
      deadlineDate: existingTask ? existingTask.deadlineDate : ''
    };

    function renderEditor() {
      var cats = allCategories();
      var quickChips = !isEdit
        ? '<div class="field-group"><div class="field-label">一點都不費力的靈感推薦</div><div class="chip-row">' +
          D.roomQuickRecommendations(draft.category, getCategoryLabel(draft.category)).map(function (q) {
            return '<button type="button" class="filter-chip" data-quick-title="' + A.esc(q) + '">' + A.esc(q) + '</button>';
          }).join('') +
          '</div></div>'
        : '';

      var catSelect = '<div class="chip-row">' + cats.map(function (c) {
        return '<button type="button" class="filter-chip' + (draft.category === c.wireValue ? ' selected' : '') + '" data-draft-cat="' + A.esc(c.wireValue) + '">' + A.esc(c.labelZh) + '</button>';
      }).join('') + '</div>';

      var effortRow = '<div class="chip-row">' + D.EFFORT_LEVELS.map(function (ef) {
        return '<button type="button" class="filter-chip flex1' + (draft.effortLevel === ef.wireValue ? ' selected' : '') + '" data-draft-effort="' + ef.wireValue + '">' + A.esc(ef.labelZh) + '</button>';
      }).join('') + '</div>';

      var gameExtra = draft.category === 'game'
        ? '<div class="field-group"><div class="field-label">每日重置時間</div>' +
            '<select class="select-box" id="draft-reset-hour">' +
              Array.from({ length: 24 }, function (_, h) {
                return '<option value="' + h + '"' + (Number(draft.resetHour) === h ? ' selected' : '') + '>每天 ' + A.pad2(h) + ':00 自動重置</option>';
              }).join('') +
            '</select>' +
          '</div>'
        : '';

      var homeworkExtra = draft.category === 'homework'
        ? '<div class="field-group"><div class="field-label">死線日期</div>' +
            '<button type="button" class="btn btn-outline-soft btn-full" id="draft-deadline-btn" style="justify-content:space-between">' +
              '<span>' + (draft.deadlineDate ? '截止日：' + A.esc(draft.deadlineDate) : '點擊從月曆選擇死線（可留空）') + '</span>' +
              '<span>' + ICONS.log + '</span>' +
            '</button>' +
          '</div>'
        : '';

      var html =
        '<div class="sheet-panel">' +
          '<div class="sheet-handle"></div>' +
          '<div style="display:flex;align-items:center;justify-content:space-between">' +
            '<div style="font-size:18px;font-weight:700">' + (isEdit ? '編輯這個項目' : '新增一件復健小事') + '</div>' +
            '<button type="button" class="icon-btn" id="te-close">' + ICONS.close + '</button>' +
          '</div>' +
          quickChips +
          '<div class="field-group"><div class="field-label">項目名稱</div><input type="text" class="input-box" id="draft-title" maxlength="60" value="' + A.esc(draft.title) + '" placeholder="越簡單越容易完成！" /></div>' +
          '<div class="field-group"><div class="field-label">給自己的微備註（可留空）</div><input type="text" class="input-box" id="draft-note" maxlength="100" value="' + A.esc(draft.note) + '" placeholder="例如：只開檔看兩分鐘就好" /></div>' +
          '<div class="field-group"><div class="field-label">分類</div>' + catSelect + '</div>' +
          '<div class="field-group"><div class="field-label">心理負擔（難度）</div>' + effortRow + '</div>' +
          gameExtra +
          homeworkExtra +
          '<div style="display:flex;gap:10px;margin-top:4px">' +
            '<button type="button" class="btn btn-outline-soft" style="flex:1" id="te-cancel">取消</button>' +
            '<button type="button" class="btn btn-primary" style="flex:1.4" id="te-save">' + (isEdit ? '儲存修改' : '放進康復清單') + '</button>' +
          '</div>' +
        '</div>';

      var root = A.openModal(html);
      var titleEl = root.querySelector('#draft-title');
      var noteEl = root.querySelector('#draft-note');
      titleEl.oninput = function () { draft.title = titleEl.value; };
      noteEl.oninput = function () { draft.note = noteEl.value; };

      root.querySelector('#te-close').onclick = A.closeModal;
      root.querySelector('#te-cancel').onclick = A.closeModal;

      root.querySelectorAll('[data-quick-title]').forEach(function (btn) {
        btn.onclick = function () {
          draft.title = btn.getAttribute('data-quick-title');
          titleEl.value = draft.title;
        };
      });
      root.querySelectorAll('[data-draft-cat]').forEach(function (btn) {
        btn.onclick = function () {
          draft.category = btn.getAttribute('data-draft-cat');
          renderEditor();
        };
      });
      root.querySelectorAll('[data-draft-effort]').forEach(function (btn) {
        btn.onclick = function () {
          draft.effortLevel = btn.getAttribute('data-draft-effort');
          renderEditor();
        };
      });

      var rhEl = root.querySelector('#draft-reset-hour');
      if (rhEl) rhEl.onchange = function () { draft.resetHour = parseInt(rhEl.value, 10) || 0; };

      var dlBtn = root.querySelector('#draft-deadline-btn');
      if (dlBtn) {
        dlBtn.onclick = function () {
          openGoogleDeadlinePicker(draft.deadlineDate, function (picked) {
            draft.deadlineDate = picked || '';
            renderEditor();
          });
        };
      }

      root.querySelector('#te-save').onclick = function () {
        var finalTitle = (titleEl.value || '').trim();
        if (!finalTitle) {
          A.showToast('先寫一點點名稱再儲存吧');
          return;
        }
        A.setState(function (s) {
          if (isEdit) {
            s.tasks = s.tasks.map(function (t) {
              if (t.id !== existingTask.id) return t;
              return Object.assign({}, t, {
                title: finalTitle,
                note: (noteEl.value || '').trim(),
                category: draft.category,
                effortLevel: draft.effortLevel,
                resetHour: Number(draft.resetHour) || 4,
                deadlineDate: draft.category === 'homework' ? draft.deadlineDate : ''
              });
            });
          } else {
            var maxOrder = s.tasks.reduce(function (m, x) { return Math.max(m, x.sortOrder || 0); }, -1);
            s.tasks.unshift({
              id: A.uid('task'),
              title: finalTitle,
              note: (noteEl.value || '').trim(),
              effortLevel: draft.effortLevel,
              category: draft.category,
              completed: false,
              completedDate: '',
              completedAtMs: 0,
              sortOrder: maxOrder + 1,
              timeSlotsEnabled: false,
              morningEnabled: true,
              noonEnabled: true,
              eveningEnabled: true,
              morningDone: false,
              noonDone: false,
              eveningDone: false,
              resetHour: Number(draft.resetHour) || 4,
              deadlineDate: draft.category === 'homework' ? draft.deadlineDate : ''
            });
            A.runtime.roomCategory = draft.category;
          }
        }, true);
        A.closeModal();
        A.showToast(isEdit ? '已更新療程項目' : '已加入清單');
        A.rerender();
      };
    }
    renderEditor();
  }

  function openSplitSlotsModal(task) {
    var enabled = !!task.timeSlotsEnabled;
    var mOn = !!task.morningEnabled;
    var nOn = !!task.noonEnabled;
    var eOn = !!task.eveningEnabled;
    if (!mOn && !nOn && !eOn) mOn = true;

    function renderSplit() {
      var html =
        '<div class="dialog-panel">' +
          '<div style="display:flex;align-items:center;justify-content:space-between">' +
            '<div style="font-size:18px;font-weight:700">切分早中晚時段</div>' +
            '<button type="button" class="icon-btn" id="sp-close">' + ICONS.close + '</button>' +
          '</div>' +
          '<div style="font-size:13px;color:var(--on-surface-variant)">自訂「' + A.esc(task.title) + '」要拆成哪些時段打卡（可任選早／中／晚組合）</div>' +
          '<div class="chip-row">' +
            '<button type="button" class="filter-chip" id="sp-preset-all">早＋中＋晚</button>' +
            '<button type="button" class="filter-chip" id="sp-preset-me">早＋晚</button>' +
            '<button type="button" class="filter-chip" id="sp-preset-mn">早＋中</button>' +
            '<button type="button" class="filter-chip" id="sp-preset-ne">中＋晚</button>' +
          '</div>' +
          '<div style="display:flex;flex-direction:column;gap:8px">' +
            '<div style="display:flex;align-items:center;justify-content:space-between;padding:10px 12px;border-radius:14px;background:var(--surface-low)">' +
              '<span style="font-weight:700">早上（早）</span><button type="button" class="toggle-switch' + (enabled && mOn ? ' on' : '') + '" id="sp-tog-m"></button>' +
            '</div>' +
            '<div style="display:flex;align-items:center;justify-content:space-between;padding:10px 12px;border-radius:14px;background:var(--surface-low)">' +
              '<span style="font-weight:700">中午／下午（中）</span><button type="button" class="toggle-switch' + (enabled && nOn ? ' on' : '') + '" id="sp-tog-n"></button>' +
            '</div>' +
            '<div style="display:flex;align-items:center;justify-content:space-between;padding:10px 12px;border-radius:14px;background:var(--surface-low)">' +
              '<span style="font-weight:700">晚上（晚）</span><button type="button" class="toggle-switch' + (enabled && eOn ? ' on' : '') + '" id="sp-tog-e"></button>' +
            '</div>' +
          '</div>' +
          '<div style="display:flex;gap:8px;margin-top:4px">' +
            '<button type="button" class="btn btn-outline-soft" style="flex:1" id="sp-off">不切分時段</button>' +
            '<button type="button" class="btn btn-primary" style="flex:1" id="sp-save">套用時段</button>' +
          '</div>' +
        '</div>';

      var root = A.openModal(html, { center: true });
      root.querySelector('#sp-close').onclick = A.closeModal;
      function toggleSlotFlag(which) {
        if (!enabled) {
          enabled = true;
          mOn = which === 'm';
          nOn = which === 'n';
          eOn = which === 'e';
        } else {
          if (which === 'm') mOn = !mOn;
          if (which === 'n') nOn = !nOn;
          if (which === 'e') eOn = !eOn;
          if (!mOn && !nOn && !eOn) enabled = false;
        }
        renderSplit();
      }
      root.querySelector('#sp-tog-m').onclick = function () { toggleSlotFlag('m'); };
      root.querySelector('#sp-tog-n').onclick = function () { toggleSlotFlag('n'); };
      root.querySelector('#sp-tog-e').onclick = function () { toggleSlotFlag('e'); };
      root.querySelector('#sp-preset-all').onclick = function () { enabled = true; mOn = true; nOn = true; eOn = true; renderSplit(); };
      root.querySelector('#sp-preset-me').onclick = function () { enabled = true; mOn = true; nOn = false; eOn = true; renderSplit(); };
      root.querySelector('#sp-preset-mn').onclick = function () { enabled = true; mOn = true; nOn = true; eOn = false; renderSplit(); };
      root.querySelector('#sp-preset-ne').onclick = function () { enabled = true; mOn = false; nOn = true; eOn = true; renderSplit(); };

      root.querySelector('#sp-off').onclick = function () {
        A.setState(function (s) {
          s.tasks = s.tasks.map(function (t) {
            if (t.id !== task.id) return t;
            return Object.assign({}, t, { timeSlotsEnabled: false, morningDone: false, noonDone: false, eveningDone: false });
          });
        }, true);
        A.closeModal();
        A.rerender();
      };
      root.querySelector('#sp-save').onclick = function () {
        if (!enabled || (!mOn && !nOn && !eOn)) {
          root.querySelector('#sp-off').click();
          return;
        }
        A.setState(function (s) {
          s.tasks = s.tasks.map(function (t) {
            if (t.id !== task.id) return t;
            var md = mOn ? t.morningDone : false;
            var nd = nOn ? t.noonDone : false;
            var ed = eOn ? t.eveningDone : false;
            var allOk = (!mOn || md) && (!nOn || nd) && (!eOn || ed) && t.completed;
            return Object.assign({}, t, {
              timeSlotsEnabled: true,
              morningEnabled: mOn,
              noonEnabled: nOn,
              eveningEnabled: eOn,
              morningDone: md,
              noonDone: nd,
              eveningDone: ed,
              completed: allOk
            });
          });
        }, true);
        A.closeModal();
        A.rerender();
      };
    }
    renderSplit();
  }

  /* ===================== TIMETABLE MODALS ===================== */

  function openTimetableMetaModal() {
    var tt = A.state.timetable;
    var showWk = tt.showWeekends !== undefined ? !!tt.showWeekends : true;
    function renderMeta() {
      var html =
        '<div class="dialog-panel">' +
          '<div style="display:flex;align-items:center;justify-content:space-between">' +
            '<div style="font-size:18px;font-weight:700">編輯課表／班表標題</div>' +
            '<button type="button" class="icon-btn" id="ttm-close">' + ICONS.close + '</button>' +
          '</div>' +
          '<div class="field-group"><div class="field-label">表格名稱</div><input type="text" class="input-box" id="ttm-title" value="' + A.esc(tt.tableTitle) + '" maxlength="24"/></div>' +
          '<div class="field-group"><div class="field-label">左上角欄名</div><input type="text" class="input-box" id="ttm-corner" value="' + A.esc(tt.cornerLabel) + '" maxlength="12"/></div>' +
          '<div style="display:flex;align-items:center;justify-content:space-between;padding:10px 12px;border-radius:14px;background:var(--surface-low)">' +
            '<div><div style="font-weight:700;font-size:14px">顯示週六與週日</div><div style="font-size:12px;color:var(--on-surface-variant)">切換七日全覽或僅平日週一～週五</div></div>' +
            '<button type="button" class="toggle-switch' + (showWk ? ' on' : '') + '" id="ttm-weekends"></button>' +
          '</div>' +
          '<button type="button" class="btn btn-primary btn-full" id="ttm-save">儲存表格設定</button>' +
        '</div>';
      var root = A.openModal(html, { center: true });
      root.querySelector('#ttm-close').onclick = A.closeModal;
      root.querySelector('#ttm-weekends').onclick = function () {
        showWk = !showWk;
        renderMeta();
      };
      root.querySelector('#ttm-save').onclick = function () {
        var tVal = root.querySelector('#ttm-title').value.trim() || '我的每週課表／班表';
        var cVal = root.querySelector('#ttm-corner').value.trim() || '節次';
        A.setState(function (s) {
          s.timetable.tableTitle = tVal;
          s.timetable.cornerLabel = cVal;
          s.timetable.showWeekends = showWk;
        }, true);
        A.closeModal();
        A.rerender();
      };
    }
    renderMeta();
  }

  function openTimetablePeriodsModal() {
    var periods = A.state.timetable.periods.map(function (p) { return Object.assign({}, p); });
    function renderPeriods() {
      var rowsHtml = periods.map(function (p, i) {
        return '<div style="padding:10px;border-radius:14px;background:var(--surface-low);display:flex;flex-direction:column;gap:6px">' +
          '<div style="display:flex;align-items:center;gap:6px">' +
            '<input type="text" class="input-box" style="padding:8px 10px;font-size:13px" data-p-label="' + i + '" value="' + A.esc(p.label) + '" placeholder="節次名"/>' +
            (periods.length > 1 ? '<button type="button" class="icon-btn" data-p-del="' + i + '" style="color:var(--primary)">' + ICONS.trash + '</button>' : '') +
          '</div>' +
          '<div style="display:flex;align-items:center;gap:6px">' +
            '<input type="time" class="input-box" style="padding:7px 10px;font-size:13px" data-p-start="' + i + '" value="' + A.esc(p.startTime || '08:10') + '"/>' +
            '<span>—</span>' +
            '<input type="time" class="input-box" style="padding:7px 10px;font-size:13px" data-p-end="' + i + '" value="' + A.esc(p.endTime || '09:00') + '"/>' +
          '</div>' +
        '</div>';
      }).join('');

      var html =
        '<div class="sheet-panel">' +
          '<div class="sheet-handle"></div>' +
          '<div style="display:flex;align-items:center;justify-content:space-between">' +
            '<div style="font-size:18px;font-weight:700">管理節次與時段</div>' +
            '<button type="button" class="icon-btn" id="ttp-close">' + ICONS.close + '</button>' +
          '</div>' +
          '<div style="font-size:12px;color:var(--on-surface-variant)">修改開始時間會自動帶入 +50 分鐘作為結束時間，仍可再手動微調。</div>' +
          '<div style="display:flex;flex-direction:column;gap:8px">' + rowsHtml + '</div>' +
          '<button type="button" class="btn btn-outline btn-full" id="ttp-add">' + ICONS.plus + ' 新增一節時段</button>' +
          '<button type="button" class="btn btn-primary btn-full" id="ttp-save">儲存所有節次</button>' +
        '</div>';

      var root = A.openModal(html);
      root.querySelector('#ttp-close').onclick = A.closeModal;
      root.querySelectorAll('[data-p-label]').forEach(function (el) {
        el.oninput = function () { periods[Number(el.getAttribute('data-p-label'))].label = el.value; };
      });
      root.querySelectorAll('[data-p-start]').forEach(function (el) {
        el.onchange = function () {
          var idx = Number(el.getAttribute('data-p-start'));
          periods[idx].startTime = el.value;
          periods[idx].endTime = A.addMinutesToTime(el.value, 50);
          var endInput = root.querySelector('[data-p-end="' + idx + '"]');
          if (endInput) endInput.value = periods[idx].endTime;
        };
      });
      root.querySelectorAll('[data-p-end]').forEach(function (el) {
        el.onchange = function () { periods[Number(el.getAttribute('data-p-end'))].endTime = el.value; };
      });
      root.querySelectorAll('[data-p-del]').forEach(function (btn) {
        btn.onclick = function () {
          periods.splice(Number(btn.getAttribute('data-p-del')), 1);
          renderPeriods();
        };
      });
      root.querySelector('#ttp-add').onclick = function () {
        var last = periods[periods.length - 1];
        var nextStart = last ? A.addMinutesToTime(last.endTime || '09:00', 10) : '08:10';
        var nextEnd = A.addMinutesToTime(nextStart, 50);
        periods.push({
          id: 'p_' + Date.now().toString(36) + '_' + periods.length,
          label: String(periods.length + 1),
          startTime: nextStart,
          endTime: nextEnd
        });
        renderPeriods();
      };
      root.querySelector('#ttp-save').onclick = function () {
        A.setState(function (s) {
          s.timetable.periods = periods.map(function (p, i) {
            return {
              id: p.id || ('p' + (i + 1)),
              label: (p.label || String(i + 1)).trim(),
              startTime: p.startTime || '08:10',
              endTime: p.endTime || '09:00'
            };
          });
        }, true);
        A.closeModal();
        A.rerender();
      };
    }
    renderPeriods();
  }

  function openTimetableCellModal(period, dayIndex) {
    var key = period.id + '_' + dayIndex;
    var existing = A.state.timetable.cells[key] || { title: '', note: '', colorIndex: 1 };
    var dayNames = ['週一', '週二', '週三', '週四', '週五', '週六', '週日'];
    var colorIndex = existing.colorIndex != null ? existing.colorIndex : 1;

    function renderCellDlg() {
      var swatches = D.TIMETABLE_CELL_COLORS.map(function (c) {
        var sel = colorIndex === c.index;
        return '<button type="button" class="swatch-dot' + (sel ? ' selected' : '') + '" data-swatch="' + c.index + '" style="background:' + c.bgHex + '" title="' + A.esc(c.label) + '">' +
          (sel ? '<span style="color:' + c.fgHex + ';font-size:14px;font-weight:900">✓</span>' : '') +
        '</button>';
      }).join('');

      var html =
        '<div class="dialog-panel">' +
          '<div style="display:flex;align-items:center;justify-content:space-between">' +
            '<div>' +
              '<div style="font-size:18px;font-weight:700">' + dayNames[dayIndex] + '・第 ' + A.esc(period.label.replace(/\n/g, ' ')) + ' 節</div>' +
              '<div style="font-size:12px;color:var(--on-surface-variant)">' + A.esc(period.startTime) + ' - ' + A.esc(period.endTime) + '</div>' +
            '</div>' +
            '<button type="button" class="icon-btn" id="ttc-close">' + ICONS.close + '</button>' +
          '</div>' +
          '<div class="field-group"><div class="field-label">課程／班表內容</div><input type="text" class="input-box" id="ttc-title" maxlength="40" value="' + A.esc(existing.title) + '" placeholder="例如：3D建模、早班、讀書時段"/></div>' +
          '<div class="field-group"><div class="field-label">教室或備註</div><input type="text" class="input-box" id="ttc-note" maxlength="40" value="' + A.esc(existing.note) + '" placeholder="例如：C405（每週）"/></div>' +
          '<div class="field-group"><div class="field-label">底色分類</div><div class="swatch-row">' + swatches + '</div></div>' +
          '<div style="display:flex;gap:8px;margin-top:4px">' +
            '<button type="button" class="btn btn-outline-soft" style="flex:1" id="ttc-clear">清空這格</button>' +
            '<button type="button" class="btn btn-primary" style="flex:1.4" id="ttc-save">儲存內容</button>' +
          '</div>' +
        '</div>';

      var root = A.openModal(html, { center: true });
      root.querySelector('#ttc-close').onclick = A.closeModal;
      root.querySelectorAll('[data-swatch]').forEach(function (btn) {
        btn.onclick = function () {
          existing.title = root.querySelector('#ttc-title').value;
          existing.note = root.querySelector('#ttc-note').value;
          colorIndex = Number(btn.getAttribute('data-swatch'));
          renderCellDlg();
        };
      });
      root.querySelector('#ttc-clear').onclick = function () {
        A.setState(function (s) { delete s.timetable.cells[key]; }, true);
        A.closeModal();
        A.rerender();
      };
      root.querySelector('#ttc-save').onclick = function () {
        var title = root.querySelector('#ttc-title').value.trim();
        var note = root.querySelector('#ttc-note').value.trim();
        A.setState(function (s) {
          if (!title && !note) {
            delete s.timetable.cells[key];
          } else {
            s.timetable.cells[key] = { title: title, note: note, colorIndex: colorIndex };
          }
        }, true);
        A.closeModal();
        if (title && A.isEarly8AmStartTime(period.startTime)) {
          A.runtime.stagePose = 'SIPPING';
          A.runtime.stageQuote = D.DOCTOR_QUOTES.early8AmCourseQuote;
          A.runtime.stageSub = A.state.feifeiNickname + '看了一眼早上 8 點的課表後默默喝了一口水';
          A.showToast(D.DOCTOR_QUOTES.early8AmCourseQuote);
        }
        A.rerender();
      };
    }
    renderCellDlg();
  }

  /* ===================== RENDERERS ===================== */

  function renderStageCardHtml() {
    var st = A.state;
    var r = A.runtime;
    var equippedItems = D.PHARMACY_ITEMS.filter(function (i) { return st.equippedItemIds.indexOf(i.id) >= 0; });
    var hasBlanket = st.equippedItemIds.indexOf('blanket_cat') >= 0;
    var hasPillow = st.equippedItemIds.indexOf('pillow_soft') >= 0;
    var hasBanner = st.equippedItemIds.indexOf('banner_miracle') >= 0;
    var leftItems = equippedItems.slice(0, 2);
    var rightItems = equippedItems.slice(2, 4);
    var overflowItems = equippedItems.slice(4);

    function badgeHtml(item) {
      return '<div class="equipped-badge"><img src="' + A.esc(item.imgSrc) + '" alt=""/><span>' + A.esc(item.name.slice(0, 4)) + '</span></div>';
    }

    return (
      '<section class="card card-lg stage-card">' +
        '<div class="stage-top-row">' +
          '<div style="display:flex;align-items:center;gap:6px;flex-wrap:wrap">' +
            '<span class="pill pill-primary">' + ICONS.spark + ' 康復主頁</span>' +
            '<span class="pill pill-secondary">累計 ' + st.totalCompleted + ' 項</span>' +
          '</div>' +
          '<button type="button" class="pill pill-white pill-clickable" id="btn-open-equip">' + ICONS.shirt + ' 換裝</button>' +
        '</div>' +
        '<button type="button" class="stage-viewport" id="btn-stage-pat" aria-label="點擊互動">' +
          '<div class="stage-halo' + (r.stagePose === 'APPROVE' ? ' glow' : '') + '"></div>' +
          '<div class="stage-bed' + (hasBlanket ? ' blanket-equipped' : '') + '"><div class="stage-pillow' + (hasPillow ? ' pillow-equipped' : '') + '"></div></div>' +
          (hasBanner ? '<div class="stage-miracle-banner">🎉 康復奇蹟錦旗飄揚中</div>' : '') +
          '<div class="stage-side-badges left' + (hasBanner ? ' has-banner' : '') + '">' + leftItems.map(badgeHtml).join('') + '</div>' +
          '<div class="stage-side-badges right' + (hasBanner ? ' has-banner' : '') + '">' + rightItems.map(badgeHtml).join('') + '</div>' +
          '<div class="stage-doctor-wrap' + (hasBanner ? ' has-banner' : '') + '">' +
            A.renderDoctorAvatarHtml(A.resolvePoseImage(r.stagePose), st.doctorGear, { height: '148px', alt: st.feifeiNickname }) +
          '</div>' +
          '<span class="stage-hint-chip">' + ICONS.chat + ' 點他換一句</span>' +
        '</button>' +
        (overflowItems.length ? '<div class="overflow-badges">' + overflowItems.map(badgeHtml).join('') + '</div>' : '') +
        '<button type="button" class="quote-box" id="btn-stage-quote">' +
          '<div class="quote-main">「' + A.esc(r.stageQuote) + '」</div>' +
          (r.stageSub ? '<div class="quote-sub">' + A.esc(r.stageSub) + '</div>' : '<div class="quote-sub">' + A.esc(st.feifeiNickname) + '・值班觀察中（點這裡也能換台詞）</div>') +
        '</button>' +
      '</section>'
    );
  }

  function renderTimetableHtml() {
    var tt = A.state.timetable;
    var showWk = tt.showWeekends !== undefined ? !!tt.showWeekends : true;
    var days = showWk ? ['一', '二', '三', '四', '五', '六', '日'] : ['一', '二', '三', '四', '五'];
    var jsDow = new Date().getDay();
    var todayIdx = jsDow === 0 ? 6 : jsDow - 1;

    var headCells = '<th class="tt-corner-col" id="btn-tt-meta-corner">' + A.esc(tt.cornerLabel || '節次') + '</th>' +
      days.map(function (d, idx) {
        return '<th class="tt-day-col' + (idx === todayIdx ? ' today' : '') + '">' + d + '</th>';
      }).join('');

    var bodyRows = tt.periods.map(function (p) {
      var pCell = '<td class="tt-period-cell" data-tt-period="' + A.esc(p.id) + '">' +
        '<div class="tt-period-label">' + A.esc(p.label) + '</div>' +
        '<div class="tt-period-time">' + A.esc(p.startTime) + '-' + A.esc(p.endTime) + '</div>' +
      '</td>';
      var dayTds = days.map(function (_, dIdx) {
        var c = tt.cells[p.id + '_' + dIdx];
        var bgStyle = '';
        if (c && (c.title || c.note)) {
          var pal = D.TIMETABLE_CELL_COLORS[c.colorIndex != null ? c.colorIndex : 1] || D.TIMETABLE_CELL_COLORS[1];
          bgStyle = 'background:' + pal.bgHex + ';color:' + pal.fgHex + ';';
        }
        return '<td class="tt-slot-cell' + (dIdx === todayIdx ? ' today-col' : '') + '" style="' + bgStyle + '" data-tt-pid="' + A.esc(p.id) + '" data-tt-day="' + dIdx + '">' +
          (c && c.title ? '<div class="tt-cell-title">' + A.esc(c.title) + '</div>' : '') +
          (c && c.note ? '<div class="tt-cell-note">' + A.esc(c.note) + '</div>' : '') +
        '</td>';
      }).join('');
      return '<tr>' + pCell + dayTds + '</tr>';
    }).join('');

    return (
      '<section class="timetable-card">' +
        '<button type="button" class="timetable-banner" id="btn-tt-meta">' +
          '<span>' + A.esc(tt.tableTitle || '我的每週課表／班表') + '</span>' +
          '<span class="timetable-banner-edit">' + ICONS.edit + '</span>' +
        '</button>' +
        '<div class="timetable-scroll">' +
          '<table class="timetable-grid"><thead><tr>' + headCells + '</tr></thead><tbody>' + bodyRows + '</tbody></table>' +
        '</div>' +
        '<div class="timetable-footer">' +
          '<span>點任意格子編輯課程，點左側節次可新增或調整時間</span>' +
          '<button type="button" class="btn btn-text" style="height:30px;font-size:12px" id="btn-tt-periods">調整節次</button>' +
        '</div>' +
      '</section>'
    );
  }

  function renderTaskRowHtml(t, todayKey) {
    var effort = D.EFFORT_LEVELS.filter(function (e) { return e.wireValue === t.effortLevel; })[0] || D.EFFORT_LEVELS[0];
    var badges = '<span class="task-meta-chip">' + A.esc(effort.labelZh) + '</span>';
    if (t.category === 'game') {
      badges += '<span class="task-meta-chip">' + ICONS.clock + ' ' + A.pad2(t.resetHour || 0) + ':00 重置</span>';
    }
    if (t.category === 'homework' && t.deadlineDate) {
      var dl = formatDeadlineChip(t.deadlineDate, todayKey);
      if (dl) badges += '<span class="task-meta-chip' + (dl.urgent ? ' urgent' : '') + '">' + A.esc(dl.text) + '</span>';
    }

    var splitHtml = '';
    if (t.timeSlotsEnabled) {
      var slots = [];
      if (t.morningEnabled) slots.push({ k: 'm', label: '早', done: t.morningDone });
      if (t.noonEnabled) slots.push({ k: 'n', label: '中', done: t.noonDone });
      if (t.eveningEnabled) slots.push({ k: 'e', label: '晚', done: t.eveningDone });
      if (slots.length) {
        splitHtml = '<div class="task-split-row">' + slots.map(function (sl) {
          return '<button type="button" class="split-slot-chip' + (sl.done ? ' done' : '') + '" data-slot-task="' + A.esc(t.id) + '" data-slot-key="' + sl.k + '">' +
            (sl.done ? ICONS.check : '') + '<span>' + sl.label + '</span>' +
          '</button>';
        }).join('') + '</div>';
      }
    }

    var menuOpen = A.runtime.openTaskMenuId === t.id;
    var dropdown = menuOpen
      ? '<div class="task-dropdown">' +
          '<button type="button" class="dropdown-item" data-menu-action="edit" data-task-id="' + A.esc(t.id) + '">' + ICONS.edit + ' 編輯這個項目</button>' +
          '<button type="button" class="dropdown-item" data-menu-action="split" data-task-id="' + A.esc(t.id) + '">' + ICONS.clock + ' 切分早中晚時段</button>' +
          (t.effortLevel !== 'micro' ? '<button type="button" class="dropdown-item" data-menu-action="micro" data-task-id="' + A.esc(t.id) + '">' + ICONS.spark + ' 切成超微事</button>' : '') +
          '<button type="button" class="dropdown-item" data-menu-action="delete" data-task-id="' + A.esc(t.id) + '" style="color:var(--primary)">' + ICONS.trash + ' 放下這項</button>' +
        '</div>'
      : '';

    return (
      '<div class="task-card-wrap" data-task-wrap="' + A.esc(t.id) + '">' +
        '<div class="task-row' + (t.completed ? ' completed' : '') + '">' +
          '<button type="button" class="task-drag-handle" data-drag-handle="' + A.esc(t.id) + '" aria-label="拖曳調整順序">' + ICONS.drag + '</button>' +
          '<button type="button" class="task-toggle-btn" data-toggle-task="' + A.esc(t.id) + '" aria-label="完成或取消">' +
            '<span class="task-circle">' + (t.completed ? ICONS.check : '') + '</span>' +
          '</button>' +
          '<button type="button" class="task-main-click" data-edit-task="' + A.esc(t.id) + '">' +
            '<div class="task-title">' + A.esc(t.title) + '</div>' +
            (t.note ? '<div class="task-note-preview">' + A.esc(t.note) + '</div>' : '') +
            '<div class="task-meta-badges">' + badges + '</div>' +
          '</button>' +
          '<div class="task-menu-wrap">' +
            '<button type="button" class="icon-btn" data-open-menu="' + A.esc(t.id) + '" aria-label="更多操作">' + ICONS.more + '</button>' +
            dropdown +
          '</div>' +
        '</div>' +
        splitHtml +
      '</div>'
    );
  }

  function renderRoomTab(container) {
    var visCats = ensureActiveCategory();
    var activeCat = A.runtime.roomCategory;
    var todayKey = A.formatDateKey();

    var catTasks = A.state.tasks
      .filter(function (t) { return t.category === activeCat; })
      .sort(function (a, b) { return (a.sortOrder || 0) - (b.sortOrder || 0); });

    var catPillsHtml =
      '<div class="category-header-bar">' +
        '<div class="category-pills-scroll">' +
          visCats.map(function (c) {
            return '<button type="button" class="cat-pill' + (c.wireValue === activeCat ? ' active' : '') + '" data-select-cat="' + A.esc(c.wireValue) + '">' + A.esc(c.labelZh) + '</button>';
          }).join('') +
        '</div>' +
        '<button type="button" class="icon-btn" id="btn-cat-manager" aria-label="管理分類">' + ICONS.more + '</button>' +
      '</div>';

    var urgentHw = activeCat === 'homework' ? catTasks.filter(function (t) { return isHomeworkUrgent(t, todayKey); }) : [];
    var urgentHtml = urgentHw.length
      ? '<div class="deadline-alert-card">' +
          '<div class="deadline-alert-icon">!</div>' +
          '<div style="flex:1">' +
            '<div style="font-weight:700;font-size:14px;color:#5D222E">迫在眉睫的作業（' + urgentHw.length + ' 項）</div>' +
            '<div style="font-size:12.5px;color:#7D424E;margin-top:2px">' + A.esc(D.DOCTOR_QUOTES.homeworkDeadlineUrgentQuote) + '</div>' +
          '</div>' +
        '</div>'
      : '';

    var tasksBlockHtml = '';
    if (catTasks.length) {
      tasksBlockHtml = '<div id="task-list-container" style="display:flex;flex-direction:column;gap:10px">' +
        catTasks.map(function (t) { return renderTaskRowHtml(t, todayKey); }).join('') +
      '</div>';
    } else {
      tasksBlockHtml =
        '<div class="card card-soft" style="padding:20px;text-align:center;display:flex;flex-direction:column;align-items:center;gap:8px">' +
          '<div style="font-weight:700;font-size:15px">還沒有待辦壓力</div>' +
          '<div style="font-size:13px;color:var(--on-surface-variant)">今天只想躺在棉被裡也是完全合理的選擇，有餘力再放件超微事吧。</div>' +
          '<button type="button" class="btn btn-primary" id="btn-empty-add-task" style="margin-top:4px">' + ICONS.plus + ' 新增一件超級小事</button>' +
        '</div>';
    }

    var sectionTitleHtml =
      '<div class="section-header-row">' +
        '<h2 class="section-title">' + A.esc(getCategoryLabel(activeCat)) + '復健清單</h2>' +
        '<span class="section-hint">拖曳左側可排序</span>' +
      '</div>';

    // Course tab shows Timetable ABOVE the task/empty block
    var mainBodyHtml = activeCat === 'course'
      ? renderTimetableHtml() + sectionTitleHtml + tasksBlockHtml
      : urgentHtml + sectionTitleHtml + tasksBlockHtml;

    container.innerHTML = renderStageCardHtml() + catPillsHtml + mainBodyHtml;

    // Bind events
    container.querySelector('#btn-stage-pat').onclick = handleStagePat;
    container.querySelector('#btn-stage-quote').onclick = handleStagePat;
    container.querySelector('#btn-open-equip').onclick = openDoctorEquipSheet;
    container.querySelector('#btn-cat-manager').onclick = openCategoryManagerModal;

    container.querySelectorAll('[data-select-cat]').forEach(function (btn) {
      btn.onclick = function () {
        A.runtime.roomCategory = btn.getAttribute('data-select-cat');
        A.runtime.openTaskMenuId = null;
        A.rerender();
      };
    });

    var emptyAdd = container.querySelector('#btn-empty-add-task');
    if (emptyAdd) emptyAdd.onclick = function () { openTaskEditorModal(null, activeCat); };

    if (activeCat === 'course') {
      container.querySelector('#btn-tt-meta').onclick = openTimetableMetaModal;
      container.querySelector('#btn-tt-meta-corner').onclick = openTimetableMetaModal;
      container.querySelector('#btn-tt-periods').onclick = openTimetablePeriodsModal;
      container.querySelectorAll('[data-tt-period]').forEach(function (td) {
        td.onclick = openTimetablePeriodsModal;
      });
      container.querySelectorAll('[data-tt-pid]').forEach(function (td) {
        td.onclick = function () {
          var pid = td.getAttribute('data-tt-pid');
          var day = Number(td.getAttribute('data-tt-day'));
          var period = A.state.timetable.periods.filter(function (x) { return x.id === pid; })[0];
          if (period) openTimetableCellModal(period, day);
        };
      });
    }

    container.querySelectorAll('[data-toggle-task]').forEach(function (btn) {
      btn.onclick = function () { toggleTaskComplete(btn.getAttribute('data-toggle-task')); };
    });

    container.querySelectorAll('[data-edit-task]').forEach(function (btn) {
      btn.onclick = function () {
        var t = A.state.tasks.filter(function (x) { return x.id === btn.getAttribute('data-edit-task'); })[0];
        if (t) openTaskEditorModal(t);
      };
    });

    container.querySelectorAll('[data-slot-task]').forEach(function (btn) {
      btn.onclick = function () {
        toggleTaskSlot(btn.getAttribute('data-slot-task'), btn.getAttribute('data-slot-key'));
      };
    });

    container.querySelectorAll('[data-open-menu]').forEach(function (btn) {
      btn.onclick = function (ev) {
        ev.stopPropagation();
        var id = btn.getAttribute('data-open-menu');
        A.runtime.openTaskMenuId = A.runtime.openTaskMenuId === id ? null : id;
        A.rerender();
      };
    });

    container.querySelectorAll('[data-menu-action]').forEach(function (btn) {
      btn.onclick = function (ev) {
        ev.stopPropagation();
        var action = btn.getAttribute('data-menu-action');
        var id = btn.getAttribute('data-task-id');
        var t = A.state.tasks.filter(function (x) { return x.id === id; })[0];
        A.runtime.openTaskMenuId = null;
        if (!t) return;
        if (action === 'edit') {
          openTaskEditorModal(t);
        } else if (action === 'split') {
          openSplitSlotsModal(t);
        } else if (action === 'micro') {
          A.setState(function (s) {
            s.tasks = s.tasks.map(function (x) { return x.id === id ? Object.assign({}, x, { effortLevel: 'micro' }) : x; });
          }, true);
          A.runtime.stageQuote = A.pickRandom(D.DOCTOR_QUOTES.shrinkSuggestions);
          A.rerender();
        } else if (action === 'delete') {
          A.setState(function (s) {
            s.tasks = s.tasks.filter(function (x) { return x.id !== id; });
          }, true);
          A.runtime.stagePose = 'CALM';
          A.runtime.stageQuote = A.pickRandom(D.DOCTOR_QUOTES.taskDeleteQuotes);
          A.rerender();
        }
      };
    });

    // Pointer Drag Reorder
    var listEl = container.querySelector('#task-list-container');
    if (listEl) {
      listEl.querySelectorAll('[data-drag-handle]').forEach(function (handle) {
        handle.onpointerdown = function (ev) {
          ev.preventDefault();
          var dragId = handle.getAttribute('data-drag-handle');
          var wrap = listEl.querySelector('[data-task-wrap="' + dragId + '"]');
          if (!wrap) return;
          var rowEl = wrap.querySelector('.task-row');
          if (rowEl) rowEl.classList.add('dragging-row');
          try { handle.setPointerCapture(ev.pointerId); } catch (e) { /* ignore */ }

          function onMove(mv) {
            var children = Array.prototype.slice.call(listEl.children);
            for (var i = 0; i < children.length; i++) {
              var ch = children[i];
              if (ch === wrap) continue;
              var rect = ch.getBoundingClientRect();
              var mid = rect.top + rect.height / 2;
              if (mv.clientY < mid && ch.previousSibling !== wrap) {
                listEl.insertBefore(wrap, ch);
                break;
              } else if (mv.clientY > rect.bottom && ch.nextSibling !== wrap && i === children.length - 1) {
                listEl.appendChild(wrap);
                break;
              } else if (mv.clientY > mid && mv.clientY < rect.bottom) {
                listEl.insertBefore(wrap, ch.nextSibling);
                break;
              }
            }
          }

          function onUp() {
            if (rowEl) rowEl.classList.remove('dragging-row');
            window.removeEventListener('pointermove', onMove);
            window.removeEventListener('pointerup', onUp);
            window.removeEventListener('pointercancel', onUp);
            var ordered = Array.prototype.slice.call(listEl.children).map(function (el) {
              return el.getAttribute('data-task-wrap');
            });
            reorderCategoryTasks(activeCat, ordered);
          }

          window.addEventListener('pointermove', onMove);
          window.addEventListener('pointerup', onUp);
          window.addEventListener('pointercancel', onUp);
        };
      });
    }
  }

  A.renderRoomTab = renderRoomTab;
  A.openTaskEditorModal = openTaskEditorModal;
  A.openDoctorEquipSheet = openDoctorEquipSheet;
})();
