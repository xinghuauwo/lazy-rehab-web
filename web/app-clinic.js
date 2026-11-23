/* 懶癌康復室 — Tab 3 (藥局道具 & 乙女心理診療室) + Tab 4 (回憶病歷 & 特別CG) */
(function () {
  'use strict';

  var A = window.REHAB_APP;
  var D = A.D;
  var ICONS = A.ICONS;

  /* ===================== TAB 3: PHARMACY & OTOME CLINIC ===================== */

  function redeemItem(item) {
    var pos = D.DEFAULT_WARD_POSITIONS[item.id] || { xFraction: 0.5, yFraction: 0.72 };
    A.setState(function (s) {
      if (s.ownedItemIds.indexOf(item.id) < 0) s.ownedItemIds.push(item.id);
      if (s.equippedItemIds.indexOf(item.id) < 0) s.equippedItemIds.push(item.id);
      if (!s.wardPlacements.some(function (p) { return p.itemId === item.id; })) {
        s.wardPlacements.push({ itemId: item.id, xFraction: pos.xFraction, yFraction: pos.yFraction });
      }
    }, true);
    A.runtime.pharmacyPose = 'APPROVE';
    A.runtime.pharmacyQuote = item.redeemQuote || A.pickRandom(D.DOCTOR_QUOTES.pharmacyQuotes);
    A.showToast('已領取並擺進病房「' + item.name + '」');
    A.rerender();
  }

  function toggleEquipItem(item) {
    var currentlyEquipped = A.state.equippedItemIds.indexOf(item.id) >= 0;
    var pos = D.DEFAULT_WARD_POSITIONS[item.id] || { xFraction: 0.5, yFraction: 0.72 };
    A.setState(function (s) {
      if (currentlyEquipped) {
        s.equippedItemIds = s.equippedItemIds.filter(function (id) { return id !== item.id; });
        s.wardPlacements = s.wardPlacements.filter(function (p) { return p.itemId !== item.id; });
      } else {
        s.equippedItemIds.push(item.id);
        if (!s.wardPlacements.some(function (p) { return p.itemId === item.id; })) {
          s.wardPlacements.push({ itemId: item.id, xFraction: pos.xFraction, yFraction: pos.yFraction });
        }
      }
    }, true);
    A.runtime.pharmacyPose = currentlyEquipped ? 'CALM' : 'APPROVE';
    A.runtime.pharmacyQuote = currentlyEquipped
      ? '先替你收起來，「' + item.name + '」隨時想擺再跟我說。'
      : '已經把「' + item.name + '」擺好在康復室了。';
    A.rerender();
  }

  function renderPharmacySubToggle(mode) {
    return (
      '<div class="segmented-row">' +
        '<button type="button" class="seg-btn' + (mode === 'supply' ? ' active' : '') + '" data-ph-mode="supply">' + ICONS.pharmacy + '<span>院內免費道具領取處</span></button>' +
        '<button type="button" class="seg-btn' + (mode === 'counseling' ? ' active' : '') + '" data-ph-mode="counseling">' + ICONS.heart + '<span>心理診療室</span></button>' +
      '</div>'
    );
  }

  function renderSupplyView(container) {
    var st = A.state;
    var r = A.runtime;
    var catSections = D.PHARMACY_CATEGORIES.map(function (cat) {
      var items = D.PHARMACY_ITEMS.filter(function (it) { return it.category === cat.id; });
      var cards = items.map(function (it) {
        var owned = st.ownedItemIds.indexOf(it.id) >= 0;
        var equipped = st.equippedItemIds.indexOf(it.id) >= 0;
        var btnHtml = !owned
          ? '<button type="button" class="btn btn-primary btn-full" data-redeem="' + A.esc(it.id) + '">領取並擺進病房</button>'
          : equipped
            ? '<button type="button" class="btn btn-primary btn-full" data-equip="' + A.esc(it.id) + '">已擺進病房（點此收回）</button>'
            : '<button type="button" class="btn btn-outline btn-full" data-equip="' + A.esc(it.id) + '">擺進病房／病床旁</button>';

        return '<div class="card pharmacy-item-card' + (equipped ? ' equipped' : '') + '">' +
          '<div style="display:flex;align-items:center;gap:12px">' +
            '<div class="pharmacy-thumb' + (owned ? ' owned' : '') + '"><img src="' + A.esc(it.imgSrc) + '" alt="' + A.esc(it.name) + '"/></div>' +
            '<div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:4px">' +
              '<div style="font-size:16px;font-weight:700">' + A.esc(it.name) + '</div>' +
              '<div>' +
                (owned
                  ? '<span class="pill pill-tertiary">' + ICONS.check + ' 已擁有</span>'
                  : '<span class="pill pill-secondary">免費領取</span>') +
              '</div>' +
            '</div>' +
          '</div>' +
          '<div style="font-size:13.5px;color:var(--on-surface-variant)">' + A.esc(it.description) + '</div>' +
          btnHtml +
        '</div>';
      }).join('');
      return '<h2 class="section-title" style="padding:4px 4px 0">' + A.esc(cat.titleZh) + '</h2>' + cards;
    }).join('');

    container.innerHTML =
      renderPharmacySubToggle('supply') +
      '<button type="button" class="doctor-banner" id="btn-ph-pat">' +
        '<div class="banner-avatar-frame">' +
          A.renderDoctorAvatarHtml(A.resolvePoseImage(r.pharmacyPose), st.doctorGear, { height: '104px', alt: st.feifeiNickname }) +
        '</div>' +
        '<div class="banner-copy">' +
          '<div style="display:flex;align-items:center;justify-content:space-between;gap:6px;flex-wrap:wrap">' +
            '<span class="pill pill-primary" style="font-size:11px;padding:2px 8px">' + A.esc(st.feifeiNickname) + '</span>' +
            '<span class="pill pill-secondary" style="font-size:11px;padding:2px 8px">' + ICONS.spark + ' 圖鑑 ' + st.ownedItemIds.length + ' / ' + D.PHARMACY_ITEMS.length + '</span>' +
          '</div>' +
          '<div class="banner-quote">「' + A.esc(r.pharmacyQuote) + '」</div>' +
          '<span class="banner-tap-hint">' + ICONS.chat + ' 點一下醫師切換動作</span>' +
        '</div>' +
      '</button>' +
      catSections;

    container.querySelectorAll('[data-ph-mode]').forEach(function (b) {
      b.onclick = function () { r.pharmacySubMode = b.getAttribute('data-ph-mode'); A.rerender(); };
    });
    container.querySelector('#btn-ph-pat').onclick = function () {
      var poses = ['PHARMACY', 'CALM', 'APPROVE', 'SIPPING'];
      r.pharmacyPose = poses[(poses.indexOf(r.pharmacyPose) + 1) % poses.length];
      r.pharmacyQuote = A.pickRandom(D.DOCTOR_QUOTES.pharmacyQuotes, r.pharmacyQuote);
      r.pharmacyPatCount += 1;
      A.rerender();
    };
    container.querySelectorAll('[data-redeem]').forEach(function (b) {
      b.onclick = function () {
        var it = D.PHARMACY_ITEMS.filter(function (x) { return x.id === b.getAttribute('data-redeem'); })[0];
        if (it) redeemItem(it);
      };
    });
    container.querySelectorAll('[data-equip]').forEach(function (b) {
      b.onclick = function () {
        var it = D.PHARMACY_ITEMS.filter(function (x) { return x.id === b.getAttribute('data-equip'); })[0];
        if (it) toggleEquipItem(it);
      };
    });
  }

  /* ===================== OTOME COUNSELING VIEW ===================== */

  function renderCounselingView(container) {
    var st = A.state;
    var r = A.runtime;
    var O = D.OTOME_CLINIC;
    var todayKey = A.formatDateKey();
    var completedToday = st.tasks.filter(function (t) {
      return t.completed && (!t.completedDate || t.completedDate === todayKey);
    }).length;
    var totalTasks = st.tasks.length;
    var pct = totalTasks > 0 ? Math.round((completedToday / totalTasks) * 100) : 0;
    var hour = new Date().getHours();
    var period = O.getTimePeriod(hour);
    var isLateNight = period.id === 'LATE_NIGHT';
    var initPair = O.greetingFor(period.id, completedToday, totalTasks);

    function getExprObj(id) {
      var key = String(id || 'COOL').toUpperCase();
      return O.expressions[key] || O.expressions.COOL;
    }

    var currentExprId = initPair.exp;
    var currentDialogue = initPair.text;
    var currentSubtitle = getExprObj(currentExprId).moodTagZh;
    var isCloseup = false;
    var showHotspots = true;

    function getThemeObj() {
      return O.themes.filter(function (th) { return th.id === st.otomeThemeId; })[0] || O.themes[0];
    }

    function recordVisit() {
      A.setState(function (s) {
        s.otomeClinicProgress.clinicVisits = (s.otomeClinicProgress.clinicVisits || 0) + 1;
      }, true);
      var cntEl = container.querySelector('#otome-visit-pill');
      if (cntEl) cntEl.textContent = '療程 ' + completedToday + '/' + totalTasks + ' (' + pct + '%)・' + st.otomeClinicProgress.clinicVisits + '診';
    }

    function setDialogue(exprId, line, subOverride, countVisit) {
      currentExprId = String(exprId || 'COOL').toUpperCase();
      currentDialogue = line;
      currentSubtitle = subOverride || getExprObj(currentExprId).moodTagZh;
      if (countVisit !== false) recordVisit();
      drawStage();
    }

    function cycleTap() {
      var pool = O.progressTapLines(completedToday, totalTasks, period.id);
      var filtered = pool.filter(function (p) { return p.text !== currentDialogue; });
      var picked = A.pickRandom(filtered.length ? filtered : pool);
      setDialogue(picked.exp, picked.text, null, true);
    }

    function openSelfReportSheet() {
      var choicesHtml = O.selfReportChoices.map(function (c) {
        return '<button type="button" class="gear-option" data-sr-id="' + A.esc(c.id) + '">' +
          '<span style="color:var(--primary)">' + ICONS.pharmacy + '</span>' +
          '<span style="font-size:15px;font-weight:600">' + A.esc(c.patientPrompt) + '</span>' +
        '</button>';
      }).join('');
      var html =
        '<div class="sheet-panel">' +
          '<div class="sheet-handle"></div>' +
          '<div style="display:flex;align-items:center;justify-content:space-between">' +
            '<div style="font-size:18px;font-weight:700">病情自述・今日症狀回報</div>' +
            '<button type="button" class="icon-btn" id="sr-close">' + ICONS.close + '</button>' +
          '</div>' +
          '<div style="font-size:13px;color:var(--on-surface-variant)">挑一句最符合你現在狀態的話告訴醫師，他會幫你開立最不費力的官方解釋。</div>' +
          '<div style="display:flex;flex-direction:column;gap:8px">' + choicesHtml + '</div>' +
        '</div>';
      var root = A.openModal(html);
      root.querySelector('#sr-close').onclick = A.closeModal;
      root.querySelectorAll('[data-sr-id]').forEach(function (btn) {
        btn.onclick = function () {
          var cid = btn.getAttribute('data-sr-id');
          A.closeModal();
          var pool = O.selfReportRepliesByChoiceId[cid] || [];
          var picked = A.pickRandom(pool.filter(function (x) { return x.text !== currentDialogue; }));
          if (picked) {
            setDialogue(picked.exp, picked.text, null, true);
          }
        };
      });
    }

    function getTodayHeartRx() {
      var p = st.otomeClinicProgress;
      var card = null;
      if (p.drawnHeartRxDate === todayKey && p.drawnHeartRxId) {
        card = O.heartPrescriptions.filter(function (h) { return h.id === p.drawnHeartRxId; })[0];
      }
      if (!card) {
        card = A.pickRandom(O.heartPrescriptions);
        A.setState(function (s) {
          s.otomeClinicProgress.drawnHeartRxId = card.id;
          s.otomeClinicProgress.drawnHeartRxDate = todayKey;
          s.otomeClinicProgress.clinicVisits = (s.otomeClinicProgress.clinicVisits || 0) + 1;
        }, true);
      }
      return card;
    }

    function openHeartRxDialog() {
      var card = getTodayHeartRx();
      function renderRxDlg() {
        var html =
          '<div class="dialog-panel" style="background:#FFF8FA;border:2px solid var(--primary)">' +
            '<div style="display:flex;align-items:center;justify-content:space-between">' +
              '<span class="pill ' + (card.isSecretAffection ? 'pill-primary' : 'pill-secondary') + '">' + ICONS.check + ' ' + A.esc(card.stampLabel) + '</span>' +
              '<button type="button" class="icon-btn" id="hrx-close">' + ICONS.close + '</button>' +
            '</div>' +
            '<div style="font-size:18px;font-weight:700">' + A.esc(card.title) + '</div>' +
            '<div style="padding:14px;border-radius:16px;background:#fff;border:1px solid var(--outline-variant);font-size:14px;color:var(--on-surface-variant);line-height:1.55">' + A.esc(card.body) + '</div>' +
            '<div style="display:flex;gap:8px">' +
              '<button type="button" class="btn btn-outline" style="flex:1" id="hrx-reroll">重抽一張</button>' +
              '<button type="button" class="btn btn-primary" style="flex:1" id="hrx-keep">收進病歷夾</button>' +
            '</div>' +
          '</div>';
        var root = A.openModal(html, { center: true });
        root.querySelector('#hrx-close').onclick = A.closeModal;
        root.querySelector('#hrx-reroll').onclick = function () {
          card = A.pickRandom(O.heartPrescriptions, card);
          A.setState(function (s) {
            s.otomeClinicProgress.drawnHeartRxId = card.id;
            s.otomeClinicProgress.drawnHeartRxDate = todayKey;
          }, true);
          renderRxDlg();
        };
        root.querySelector('#hrx-keep').onclick = function () {
          A.closeModal();
          setDialogue(card.isSecretAffection ? 'GENTLE' : 'COOL', D.DOCTOR_QUOTES.otomeHeartRxStampedQuote, null, false);
        };
      }
      renderRxDlg();
    }

    function openSecretTreeholeDialog() {
      var html =
        '<div class="dialog-panel">' +
          '<div style="display:flex;align-items:center;justify-content:space-between">' +
            '<div style="font-size:17px;font-weight:700">秘密樹洞・悄悄寫在病歷背面</div>' +
            '<button type="button" class="icon-btn" id="th-close">' + ICONS.close + '</button>' +
          '</div>' +
          '<div style="font-size:13px;color:var(--on-surface-variant)">把委屈、焦慮或說不出口的疲倦寫在紙上。可以親手碎掉，也可以交給他收進白袍口袋。</div>' +
          '<textarea class="textarea-box" id="th-input" maxlength="240" placeholder="今天其實有一點點累，或者有什麼煩惱…"></textarea>' +
          '<div style="display:flex;gap:8px">' +
            '<button type="button" class="btn btn-outline" style="flex:1" id="th-shred">碎紙機銷毀</button>' +
            '<button type="button" class="btn btn-primary" style="flex:1" id="th-give" disabled>交給醫師</button>' +
          '</div>' +
        '</div>';
      var root = A.openModal(html, { center: true });
      var inp = root.querySelector('#th-input');
      var giveBtn = root.querySelector('#th-give');
      inp.oninput = function () { giveBtn.disabled = !inp.value.trim(); };
      root.querySelector('#th-close').onclick = A.closeModal;
      root.querySelector('#th-shred').onclick = function () {
        A.closeModal();
        setDialogue('SIGH', A.pickRandom(O.secretShredReplies), '碎紙機啟動', true);
      };
      giveBtn.onclick = function () {
        A.setState(function (s) {
          s.otomeClinicProgress.secretLettersGiven = (s.otomeClinicProgress.secretLettersGiven || 0) + 1;
        }, true);
        A.closeModal();
        setDialogue('MUG', A.pickRandom(O.secretPocketReplies), '收進白袍口袋檔案', false);
      };
    }

    function openObservationNotesSheet() {
      var pr = st.otomeClinicProgress;
      var notesHtml = O.observationNotes.map(function (n) {
        var unlocked = (pr.clinicVisits || 0) >= n.requiredVisits && (st.totalCompleted || 0) >= n.requiredCompleted;
        return '<div class="card" style="padding:14px;background:' + (unlocked ? 'var(--surface-low)' : 'rgba(253,228,233,0.4)') + ';border-color:' + (unlocked ? 'var(--primary)' : 'var(--outline-variant)') + '">' +
          '<div style="display:flex;align-items:center;gap:6px;font-weight:700;font-size:14px;margin-bottom:4px">' +
            '<span style="color:' + (unlocked ? 'var(--primary)' : 'var(--on-surface-variant)') + '">' + (unlocked ? ICONS.unlock : ICONS.lock) + '</span>' +
            '<span>' + A.esc(n.title) + '</span>' +
          '</div>' +
          '<div style="font-size:13.5px;color:var(--on-surface-variant);line-height:1.5">' +
            A.esc(unlocked ? n.diaryText : '解鎖條件：診間互動達 ' + n.requiredVisits + ' 次且累計完成 ' + n.requiredCompleted + ' 項療程') +
          '</div>' +
        '</div>';
      }).join('');

      var html =
        '<div class="sheet-panel">' +
          '<div class="sheet-handle"></div>' +
          '<div style="display:flex;align-items:center;justify-content:space-between">' +
            '<div style="font-size:18px;font-weight:700">' + A.esc(st.feifeiNickname) + '的秘密觀察筆記</div>' +
            '<button type="button" class="icon-btn" id="on-close">' + ICONS.close + '</button>' +
          '</div>' +
          '<div style="padding:10px 14px;border-radius:14px;background:var(--primary-container);color:var(--on-primary-container);font-size:13px;font-weight:700">' +
            '累計診間互動 ' + (pr.clinicVisits || 0) + ' 次・累計完成療程 ' + st.totalCompleted + ' 件・口袋來信 ' + (pr.secretLettersGiven || 0) + ' 封' +
          '</div>' +
          '<div style="display:flex;flex-direction:column;gap:10px">' + notesHtml + '</div>' +
        '</div>';
      var root = A.openModal(html);
      root.querySelector('#on-close').onclick = A.closeModal;
    }

    function openThemePickerSheet() {
      var listHtml = O.themes.map(function (th) {
        var sel = th.id === st.otomeThemeId;
        return '<button type="button" class="gear-option' + (sel ? ' selected' : '') + '" data-ot-theme="' + A.esc(th.id) + '">' +
          '<img src="' + A.esc(th.imgSrc) + '" alt="' + A.esc(th.titleZh) + '" style="width:60px;height:74px;border-radius:12px;object-fit:cover;flex-shrink:0"/>' +
          '<div style="flex:1;min-width:0">' +
            '<div style="font-size:15px;font-weight:700">' + A.esc(th.titleZh) + '</div>' +
            '<div style="font-size:12px;color:var(--on-surface-variant);margin-top:2px">' + A.esc(th.subtitleZh) + '</div>' +
          '</div>' +
          (sel ? '<span style="color:' + th.accentHex + '">' + ICONS.check + '</span>' : '') +
        '</button>';
      }).join('');
      var html =
        '<div class="sheet-panel">' +
          '<div class="sheet-handle"></div>' +
          '<div style="display:flex;align-items:center;justify-content:space-between">' +
            '<div style="font-size:18px;font-weight:700">心理診療室・空間佈置主題</div>' +
            '<button type="button" class="icon-btn" id="otp-close">' + ICONS.close + '</button>' +
          '</div>' +
          '<div style="font-size:13px;color:var(--on-surface-variant)">挑一間今天最讓你想放空的診間氛圍，切換後會自動儲存。</div>' +
          '<div style="display:flex;flex-direction:column;gap:10px">' + listHtml + '</div>' +
        '</div>';
      var root = A.openModal(html);
      root.querySelector('#otp-close').onclick = A.closeModal;
      root.querySelectorAll('[data-ot-theme]').forEach(function (btn) {
        btn.onclick = function () {
          var id = btn.getAttribute('data-ot-theme');
          A.setState(function (s) { s.otomeThemeId = id; }, true);
          var th = getThemeObj();
          A.closeModal();
          setDialogue(th.id === 'warm_study' ? 'MUG' : 'GENTLE', th.switchDoctorReply, '空間切換：' + th.titleZh, false);
        };
      });
    }

    function drawStage() {
      var th = getThemeObj();
      var expr = getExprObj(currentExprId);
      var overlayBg = isLateNight ? 'rgba(31,22,36,0.46)' : th.ambientOverlayRgba;
      var zoomStyle = isCloseup
        ? 'transform: scale(1.62) translateY(3%);'
        : 'transform: scale(1.22) translateY(0%);';

      var spotsHtml = '';
      if (isCloseup) {
        spotsHtml = O.touchSpots.map(function (sp) {
          if (showHotspots) {
            return '<button type="button" class="otome-hotspot-chip" data-ot-spot="' + sp.id + '" style="' + sp.style + 'transform:translate(-50%,-50%)">' +
              '<span class="otome-hotspot-dot"></span><span>' + A.esc(sp.labelZh) + '</span>' +
            '</button>';
          }
          return '<button type="button" class="otome-hotspot-hidden" data-ot-spot="' + sp.id + '" style="' + sp.style + '" aria-label="' + A.esc(sp.labelZh) + '"></button>';
        }).join('');
      }

      container.innerHTML =
        renderPharmacySubToggle('counseling') +
        '<div class="otome-status-row">' +
          '<span class="pill" style="background:' + (isLateNight ? '#4F3E4D' : 'var(--secondary-container)') + ';color:' + (isLateNight ? '#FFF4F6' : 'var(--on-secondary-container)') + ';border-color:var(--outline-variant)">' + A.esc(period.labelZh) + '</span>' +
          '<span class="pill pill-primary" id="otome-visit-pill" style="flex:1;justify-content:center">療程 ' + completedToday + '/' + totalTasks + ' (' + pct + '%)・' + (st.otomeClinicProgress.clinicVisits || 0) + '診</span>' +
          '<button type="button" class="pill pill-clickable" id="btn-ot-theme" style="background:' + th.accentHex + '22;border-color:' + th.accentHex + ';color:var(--on-surface)">' + ICONS.palette + ' ' + A.esc(th.titleZh) + '</button>' +
        '</div>' +
        '<div class="otome-stage">' +
          '<img class="otome-bg-img" src="' + A.esc(th.imgSrc) + '" alt="' + A.esc(th.titleZh) + '"/>' +
          '<div class="otome-ambient-overlay" style="background:' + overlayBg + '"></div>' +
          '<div class="otome-portrait-area" id="otome-portrait-area">' +
            '<img class="otome-portrait-img" src="' + A.esc(expr.imgSrc) + '" alt="' + A.esc(st.feifeiNickname) + '" style="' + zoomStyle + 'animation:otomeBreathe 2.4s ease-in-out infinite alternate;"/>' +
            spotsHtml +
          '</div>' +
          (isCloseup
            ? '<button type="button" class="otome-hotspot-toggle" id="btn-ot-hotspot-toggle" style="background:' + (showHotspots ? 'var(--primary)' : 'rgba(255,248,250,0.95)') + ';color:' + (showHotspots ? '#fff' : 'var(--on-surface)') + ';border:1px solid var(--outline)">' + ICONS.eye + ' ' + (showHotspots ? '隱藏感應區' : '顯示感應區') + '</button>'
            : '') +
          '<div class="otome-bookmarks">' +
            '<button type="button" class="otome-bookmark-btn" id="bm-self">' + ICONS.chat + '<span>病情\n自述</span></button>' +
            '<button type="button" class="otome-bookmark-btn' + (isCloseup ? ' active' : '') + '" id="bm-zoom">' + (isCloseup ? ICONS.zoomOut : ICONS.zoomIn) + '<span>' + (isCloseup ? '恢復\n距離' : '專屬\n問診') + '</span></button>' +
            '<button type="button" class="otome-bookmark-btn" id="bm-rx">' + ICONS.rx + '<span>本日\n醫囑</span></button>' +
            '<button type="button" class="otome-bookmark-btn" id="bm-tree">' + ICONS.mail + '<span>秘密\n樹洞</span></button>' +
            '<button type="button" class="otome-bookmark-btn" id="bm-notes">' + ICONS.memories + '<span>觀察\n筆記</span></button>' +
          '</div>' +
          '<div class="otome-dialogue-wrap">' +
            '<div class="otome-nameplate" style="background:' + th.accentHex + '">' + A.esc(st.feifeiNickname) + '</div>' +
            '<button type="button" class="otome-dialogue-box" id="btn-ot-dialogue">' +
              '<div style="font-size:15px;font-weight:600;color:#4A3134;line-height:1.48">' + A.esc(currentDialogue) + '</div>' +
              '<div style="display:flex;justify-content:space-between;align-items:center;font-size:11.5px">' +
                '<span style="color:var(--primary);font-weight:700">' + A.esc(currentSubtitle) + '</span>' +
                '<span style="color:#9C7A7E">▾ 點擊繼續對話</span>' +
              '</div>' +
            '</button>' +
          '</div>' +
        '</div>';

      container.querySelectorAll('[data-ph-mode]').forEach(function (b) {
        b.onclick = function () { r.pharmacySubMode = b.getAttribute('data-ph-mode'); A.rerender(); };
      });
      container.querySelector('#btn-ot-theme').onclick = openThemePickerSheet;
      container.querySelector('#btn-ot-dialogue').onclick = cycleTap;
      container.querySelector('#otome-portrait-area').onclick = function (ev) {
        if (ev.target.closest('[data-ot-spot]')) return;
        if (!isCloseup || !showHotspots) cycleTap();
      };
      var htBtn = container.querySelector('#btn-ot-hotspot-toggle');
      if (htBtn) {
        htBtn.onclick = function () {
          showHotspots = !showHotspots;
          drawStage();
        };
      }
      container.querySelectorAll('[data-ot-spot]').forEach(function (btn) {
        btn.onclick = function (ev) {
          ev.stopPropagation();
          var sid = btn.getAttribute('data-ot-spot');
          var sp = O.touchSpots.filter(function (s) { return s.id === sid; })[0];
          if (!sp) return;
          var replies = sp.reactionReplies || [];
          var picked = A.pickRandom(replies.filter(function (x) { return x.text !== currentDialogue; }));
          var sub = sid === 'forehead' ? '腦袋散熱評估中' : sid === 'chest_heartbeat' ? '胸口心跳聽診中' : sid === 'pocket_snacks' ? '物資盤點中' : '懶癌脈象檢測中';
          if (picked) setDialogue(picked.exp, picked.text, sub, true);
        };
      });
      container.querySelector('#bm-self').onclick = openSelfReportSheet;
      container.querySelector('#bm-zoom').onclick = function () {
        isCloseup = !isCloseup;
        if (isCloseup) {
          setDialogue('COOL', '近距離專屬檢查模式啟動。先說好，心跳太快不能算我頭上喔。', '近距離檢查模式', false);
        } else {
          setDialogue(isLateNight ? 'NIGHT' : 'COOL', '恢復正常診間距離了。隨時想休息或再檢查都沒問題。', null, false);
        }
      };
      container.querySelector('#bm-rx').onclick = openHeartRxDialog;
      container.querySelector('#bm-tree').onclick = openSecretTreeholeDialog;
      container.querySelector('#bm-notes').onclick = openObservationNotesSheet;
    }

    drawStage();
  }

  A.renderPharmacyTab = function (container) {
    if (A.runtime.pharmacySubMode === 'counseling') {
      renderCounselingView(container);
    } else {
      renderSupplyView(container);
    }
  };

  /* ===================== TAB 4: MEMORIES & SPECIAL CG GALLERY ===================== */

  function openCgViewer(cg) {
    function openPreviewModal() {
      var html =
        '<div class="dialog-panel" style="padding:18px">' +
          '<div style="display:flex;align-items:center;justify-content:space-between;gap:8px">' +
            '<div style="flex:1;min-width:0">' +
              '<div style="font-size:18px;font-weight:700">' + A.esc(cg.title) + '</div>' +
              '<div style="font-size:12px;color:var(--primary);font-weight:700">' + A.esc(cg.subtitle) + '</div>' +
            '</div>' +
            '<div style="display:flex;gap:4px">' +
              '<button type="button" class="icon-btn" id="cg-expand" style="color:var(--primary)" aria-label="全螢幕放大查看">' + ICONS.zoomIn + '</button>' +
              '<button type="button" class="icon-btn" id="cg-pclose" aria-label="關閉預覽">' + ICONS.close + '</button>' +
            '</div>' +
          '</div>' +
          '<button type="button" class="cg-thumb-box" id="cg-img-expand" style="border-radius:16px">' +
            '<img src="' + A.esc(cg.imgSrc) + '" alt="' + A.esc(cg.title) + '"/>' +
            '<span class="pill" style="position:absolute;right:8px;bottom:8px;background:rgba(0,0,0,0.58);color:#fff">' + ICONS.zoomIn + ' 全螢幕大圖</span>' +
          '</button>' +
          '<div style="font-size:14px;color:var(--on-surface-variant);line-height:1.55">' + A.esc(cg.description) + '</div>' +
        '</div>';
      var root = A.openModal(html, { center: true });
      root.querySelector('#cg-pclose').onclick = A.closeModal;
      root.querySelector('#cg-expand').onclick = openFullscreenCg;
      root.querySelector('#cg-img-expand').onclick = openFullscreenCg;
    }

    function openFullscreenCg() {
      A.closeModal();
      var scale = 1;
      var panX = 0;
      var panY = 0;
      var dragging = false;
      var lastX = 0;
      var lastY = 0;

      var html =
        '<div class="cg-fullscreen-viewer" id="cg-fs-wrap">' +
          '<img class="cg-fullscreen-img" id="cg-fs-img" src="' + A.esc(cg.imgSrc) + '" alt="' + A.esc(cg.title) + '"/>' +
          '<div class="cg-viewer-hud-top">' +
            '<div style="padding:7px 14px;border-radius:999px;background:rgba(0,0,0,0.62);border:1px solid rgba(255,255,255,0.28);color:#fff">' +
              '<div style="font-size:14px;font-weight:700">' + A.esc(cg.title) + '</div>' +
              '<div style="font-size:11px;color:#FFB5C5">' + A.esc(cg.subtitle) + '</div>' +
            '</div>' +
            '<button type="button" class="minigame-close-btn" id="cg-fs-close" aria-label="關閉大圖">' + ICONS.close + '</button>' +
          '</div>' +
          '<div class="cg-viewer-hud-bottom">' +
            '<div class="cg-zoom-controls">' +
              '<button type="button" class="icon-btn" id="cg-zout" style="color:#fff" aria-label="縮小">' + ICONS.zoomOut + '</button>' +
              '<span id="cg-zpct" style="font-size:13px;font-weight:700;min-width:44px;text-align:center">100%</span>' +
              '<button type="button" class="icon-btn" id="cg-zin" style="color:#fff" aria-label="放大">' + ICONS.zoomIn + '</button>' +
            '</div>' +
            '<div style="width:100%;max-width:560px;padding:10px 14px;border-radius:18px;background:rgba(0,0,0,0.68);border:1px solid rgba(255,255,255,0.24);color:#fff;font-size:13px">' +
              '<div>' + A.esc(cg.description) + '</div>' +
              '<div style="font-size:11px;color:#FFC1CE;margin-top:4px">支援縮放按鈕或拖曳移動細節</div>' +
            '</div>' +
          '</div>' +
        '</div>';

      var fsRoot = A.openFullscreen(html);
      var imgEl = fsRoot.querySelector('#cg-fs-img');
      var pctEl = fsRoot.querySelector('#cg-zpct');

      function applyTransform() {
        if (scale <= 1.02) { panX = 0; panY = 0; }
        imgEl.style.transform = 'translate(' + panX + 'px,' + panY + 'px) scale(' + scale + ')';
        pctEl.textContent = Math.round(scale * 100) + '%';
      }

      fsRoot.querySelector('#cg-fs-close').onclick = A.closeFullscreen;
      fsRoot.querySelector('#cg-zin').onclick = function () { scale = Math.min(4, scale + 0.6); applyTransform(); };
      fsRoot.querySelector('#cg-zout').onclick = function () { scale = Math.max(1, scale - 0.6); applyTransform(); };

      var wrapEl = fsRoot.querySelector('#cg-fs-wrap');
      wrapEl.onpointerdown = function (ev) {
        if (scale <= 1.02) return;
        dragging = true;
        lastX = ev.clientX;
        lastY = ev.clientY;
      };
      wrapEl.onpointermove = function (ev) {
        if (!dragging) return;
        panX += ev.clientX - lastX;
        panY += ev.clientY - lastY;
        lastX = ev.clientX;
        lastY = ev.clientY;
        applyTransform();
      };
      wrapEl.onpointerup = function () { dragging = false; };
      wrapEl.ondblclick = function () {
        scale = scale > 1.05 ? 1 : 2.2;
        applyTransform();
      };
    }

    openPreviewModal();
  }

  function renderMemoriesTab(container) {
    var st = A.state;
    var r = A.runtime;
    var checkinsByDate = {};
    st.checkins.forEach(function (c) { checkinsByDate[c.dateKey] = c; });

    var tasksByDate = {};
    st.tasks.filter(function (t) { return t.completed && t.completedDate; }).forEach(function (t) {
      if (!tasksByDate[t.completedDate]) tasksByDate[t.completedDate] = [];
      tasksByDate[t.completedDate].push(t.title);
    });

    var dateSet = {};
    Object.keys(checkinsByDate).forEach(function (d) { dateSet[d] = true; });
    Object.keys(tasksByDate).forEach(function (d) { dateSet[d] = true; });
    var allDates = Object.keys(dateSet).sort().reverse();

    var dayGroups = allDates.map(function (d) {
      var chk = checkinsByDate[d] || null;
      var combined = (chk ? chk.completedTaskTitles : []).concat(tasksByDate[d] || []);
      var seen = {};
      var uniq = [];
      combined.forEach(function (title) {
        var tr = String(title || '').trim();
        if (tr && !seen[tr]) { seen[tr] = true; uniq.push(tr); }
      });
      return { dateKey: d, checkin: chk, completedTitles: uniq };
    });

    var totalCompleted = Math.max(st.totalCompleted || 0, dayGroups.reduce(function (sum, g) { return sum + g.completedTitles.length; }, 0));

    var milestonesHtml = D.MILESTONES.map(function (m) {
      var unlocked = totalCompleted >= m.count;
      return '<div class="milestone-pill' + (unlocked ? ' unlocked' : '') + '">' +
        '<span style="color:' + (unlocked ? 'var(--secondary)' : 'rgba(125,99,95,0.5)') + '">' + (unlocked ? ICONS.spark : ICONS.lock) + '</span>' +
        '<div>' +
          '<div style="font-size:13px;font-weight:700">' + A.esc(m.title) + '</div>' +
          '<div style="font-size:11px;color:var(--on-surface-variant)">' + A.esc(m.subtitle) + '</div>' +
        '</div>' +
      '</div>';
    }).join('');

    var cgsHtml = D.SPECIAL_CGS.map(function (cg) {
      var unlocked = st.unlockedCgIds.indexOf(cg.id) >= 0;
      var canAfford = (st.rehabPoints || 0) >= cg.costPoints;
      return '<div class="cg-card">' +
        '<button type="button" class="cg-thumb-box" ' + (unlocked ? 'data-preview-cg="' + A.esc(cg.id) + '"' : 'disabled') + '>' +
          '<img src="' + A.esc(cg.imgSrc) + '" alt="' + A.esc(cg.title) + '" style="opacity:' + (unlocked ? '1' : '0.22') + '"/>' +
          (!unlocked ? '<div class="cg-thumb-locked">' + ICONS.lock + '<span>需要 ' + cg.costPoints + ' 復健點解鎖</span></div>' : '') +
        '</button>' +
        '<div style="padding:12px;display:flex;flex-direction:column;gap:6px">' +
          '<div style="font-size:15px;font-weight:700">' + A.esc(cg.title) + '</div>' +
          '<div style="font-size:12px;color:' + (unlocked ? 'var(--primary)' : 'var(--on-surface-variant)') + '">' + (unlocked ? A.esc(cg.subtitle) : '需花費 ' + cg.costPoints + ' 復健點解鎖') + '</div>' +
          (unlocked
            ? '<button type="button" class="btn btn-outline btn-full" style="height:36px;font-size:12.5px" data-preview-cg="' + A.esc(cg.id) + '">已收藏・點擊放大預覽</button>'
            : '<button type="button" class="btn btn-primary btn-full" style="height:36px;font-size:12.5px" data-unlock-cg="' + A.esc(cg.id) + '"' + (canAfford ? '' : ' disabled') + '>' + (canAfford ? '花 ' + cg.costPoints + ' 點解鎖' : '還差 ' + (cg.costPoints - (st.rehabPoints || 0)) + ' 點') + '</button>') +
        '</div>' +
      '</div>';
    }).join('');

    var postcardsHtml = dayGroups.length
      ? dayGroups.map(function (day) {
        var chk = day.checkin;
        var energyTag = '';
        if (chk) {
          var lvl = A.percentToEnergyLevel(chk.energyPercent);
          var shortStatus = lvl.shortTag.indexOf(' ') >= 0 ? lvl.shortTag.split(' ')[1] : lvl.shortTag;
          energyTag = '<span class="pill pill-secondary">電量 ' + chk.energyPercent + '%・' + A.esc(shortStatus) + '</span>';
        }
        var tasksHtml = day.completedTitles.length
          ? '<div style="display:flex;flex-direction:column;gap:5px">' + day.completedTitles.map(function (t) {
              return '<div style="display:flex;align-items:center;gap:6px;font-size:14px"><span style="color:var(--primary)">' + ICONS.check + '</span><span>' + A.esc(t) + '</span></div>';
            }).join('') + '</div>'
          : '';
        var wardMini = (chk && typeof A.renderWardSnapshotHtml === 'function' && (chk.wardPlacements && chk.wardPlacements.length || chk.wardDoctorSnapshot))
          ? '<div style="border-radius:16px;overflow:hidden;border:1px solid var(--outline-variant)">' + A.renderWardSnapshotHtml(chk.wardPlacements, chk.wardDoctorSnapshot, st.doctorGear) + '</div>'
          : '';

        return '<div class="card" style="padding:18px;display:flex;flex-direction:column;gap:10px">' +
          '<div style="display:flex;align-items:center;justify-content:space-between;gap:8px;flex-wrap:wrap">' +
            '<span style="font-size:16px;font-weight:700;color:var(--primary)">' + A.esc(day.dateKey) + '</span>' +
            energyTag +
          '</div>' +
          (chk && chk.reflection ? '<div class="card-soft" style="padding:10px 12px;border-radius:14px;display:flex;align-items:flex-start;gap:8px"><span style="color:var(--secondary)">' + ICONS.heart + '</span><span style="font-size:14px">「' + A.esc(chk.reflection) + '」</span></div>' : '') +
          wardMini +
          tasksHtml +
        '</div>';
      }).join('')
      : '<div class="card" style="padding:24px;text-align:center;display:flex;flex-direction:column;align-items:center;gap:8px">' +
          '<span style="color:var(--primary)">' + ICONS.spark + '</span>' +
          '<div style="font-weight:700;font-size:15px">這份病歷夾目前空白得像剛洗完的床單</div>' +
          '<div style="font-size:13px;color:var(--on-surface-variant)">回康復室隨便勾一件小事，或去日記寫下今天，本院就會自動幫你蓋章存進這裡。</div>' +
        '</div>';

    container.innerHTML =
      '<button type="button" class="doctor-banner" id="btn-mem-pat">' +
        '<div class="banner-avatar-frame">' +
          A.renderDoctorAvatarHtml(A.resolvePoseImage(r.memoryPose), st.doctorGear, { height: '104px', alt: st.feifeiNickname }) +
        '</div>' +
        '<div class="banner-copy">' +
          '<span class="pill pill-primary" style="width:fit-content;font-size:11px;padding:2px 8px">' + A.esc(st.feifeiNickname) + '</span>' +
          '<div class="banner-quote">「' + A.esc(r.memoryQuote) + '」</div>' +
          '<div class="banner-sub">住院觀察紀錄簿・累計完成 ' + totalCompleted + ' 項復健・共 ' + dayGroups.length + ' 天存活證明</div>' +
          '<span class="banner-tap-hint">' + ICONS.chat + ' 點一下醫師看今日評語</span>' +
        '</div>' +
      '</button>' +
      '<div class="card card-soft" style="padding:18px;display:flex;flex-direction:column;gap:12px">' +
        '<div style="display:flex;align-items:center;gap:8px;font-weight:700;font-size:16px"><span style="color:var(--primary)">' + ICONS.trophy + '</span><span>院內榮譽貼紙牆（只增不減）</span></div>' +
        '<div class="milestone-grid">' + milestonesHtml + '</div>' +
      '</div>' +
      '<div class="card" style="padding:18px;display:flex;flex-direction:column;gap:12px">' +
        '<div style="display:flex;align-items:center;justify-content:space-between;gap:8px">' +
          '<span style="font-weight:700;font-size:16px">特別 CG 紀念圖鑑</span>' +
          '<span class="pill pill-primary">' + (st.rehabPoints || 0) + ' 復健點</span>' +
        '</div>' +
        '<div style="font-size:12.5px;color:var(--on-surface-variant)">可在「療程 → 復健室」放置領取每小時 60 點數，或陪醫生玩小遊戲累積點數解鎖限定合影。</div>' +
        '<div class="cg-grid">' + cgsHtml + '</div>' +
      '</div>' +
      '<h2 class="section-title" style="padding:4px 4px 0">每日住院觀察病歷</h2>' +
      postcardsHtml;

    container.querySelector('#btn-mem-pat').onclick = function () {
      var poses = ['CALM', 'APPROVE', 'SIPPING', 'PHARMACY'];
      r.memoryPose = poses[(poses.indexOf(r.memoryPose) + 1) % poses.length];
      r.memoryQuote = A.pickRandom(D.DOCTOR_QUOTES.memoryDoctorQuotes, r.memoryQuote);
      r.memoryPatCount += 1;
      A.rerender();
    };
    container.querySelectorAll('[data-unlock-cg]').forEach(function (btn) {
      btn.onclick = function () {
        var cid = btn.getAttribute('data-unlock-cg');
        var cg = D.SPECIAL_CGS.filter(function (c) { return c.id === cid; })[0];
        if (!cg || (A.state.rehabPoints || 0) < cg.costPoints) return;
        A.setState(function (s) {
          s.rehabPoints -= cg.costPoints;
          if (s.unlockedCgIds.indexOf(cid) < 0) s.unlockedCgIds.push(cid);
        }, true);
        A.showToast('已解鎖「' + cg.title + '」');
        A.rerender();
      };
    });
    container.querySelectorAll('[data-preview-cg]').forEach(function (btn) {
      btn.onclick = function () {
        var cid = btn.getAttribute('data-preview-cg');
        var cg = D.SPECIAL_CGS.filter(function (c) { return c.id === cid; })[0];
        if (cg) openCgViewer(cg);
      };
    });
  }

  A.renderMemoriesTab = renderMemoriesTab;
})();
