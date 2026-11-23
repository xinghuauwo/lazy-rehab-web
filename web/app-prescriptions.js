/* 懶癌康復室 — Tab 2 (療程目錄 + 復健室放置 & 四款小遊戲) */
(function () {
  'use strict';

  var A = window.REHAB_APP;
  var D = A.D;
  var ICONS = A.ICONS;

  var MINI_GAME_TYPES = [
    { id: 'RPS', titleZh: '猜拳對決' },
    { id: 'HIGH_CARD', titleZh: '撲克牌比大小' },
    { id: 'MASSAGE', titleZh: '肩頸安撫' },
    { id: 'FIND_OBJECT', titleZh: '限時尋物' }
  ];

  var FIND_PROPS = [
    { id: 'boba_straw', labelZh: '珍奶吸管', img: './assets/img_find_boba_straw.png' },
    { id: 'stethoscope', labelZh: '備用聽診器', img: './assets/img_find_stethoscope.png' },
    { id: 'lazy_stamp', labelZh: '拖延許可印章', img: './assets/img_find_lazy_stamp.png' },
    { id: 'mint_tin', labelZh: '薄荷糖罐', img: './assets/img_find_mint_tin.png' },
    { id: 'cat_hairclip', labelZh: '貓爪髮夾', img: './assets/img_find_cat_hairclip.png' },
    { id: 'sleep_mask', labelZh: '熱敷眼罩', img: './assets/img_find_sleep_mask.png' }
  ];

  var RPS_HANDS = [
    { id: 'scissors', labelZh: '剪刀', img: './assets/img_rps_scissors.png' },
    { id: 'rock', labelZh: '石頭', img: './assets/img_rps_rock.png' },
    { id: 'paper', labelZh: '布', img: './assets/img_rps_paper.png' }
  ];

  var gameBag = [];
  var lastGamePicked = null;

  function pickNextRandomGame() {
    if (!gameBag.length) {
      var ids = MINI_GAME_TYPES.map(function (g) { return g.id; });
      for (var i = ids.length - 1; i > 0; i--) {
        var j = Math.floor(Math.random() * (i + 1));
        var tmp = ids[i]; ids[i] = ids[j]; ids[j] = tmp;
      }
      if (lastGamePicked && ids.length > 1 && ids[0] === lastGamePicked) {
        var swap = 1 + Math.floor(Math.random() * (ids.length - 1));
        ids[0] = ids[swap];
        ids[swap] = lastGamePicked;
      }
      gameBag = ids;
    }
    lastGamePicked = gameBag.shift();
    return lastGamePicked;
  }

  function cardRankLabel(rank) {
    if (rank === 14 || rank === 1) return 'A';
    if (rank === 11) return 'J';
    if (rank === 12) return 'Q';
    if (rank === 13) return 'K';
    return String(rank);
  }

  function addPrescriptionToToday(rx) {
    var exists = A.state.tasks.some(function (t) {
      return t.title === rx.title && t.category === rx.category && !t.completed;
    });
    if (exists) {
      A.showToast('這張小處方已經在你的清單裡了！');
      return;
    }
    A.setState(function (s) {
      var minOrder = s.tasks.reduce(function (m, x) { return Math.min(m, x.sortOrder || 0); }, 0);
      s.tasks.unshift({
        id: A.uid('task'),
        title: rx.title,
        note: rx.doctorNote || '',
        effortLevel: rx.effortLevel || 'micro',
        category: rx.category || 'daily',
        completed: false,
        completedDate: '',
        completedAtMs: 0,
        sortOrder: minOrder - 1,
        timeSlotsEnabled: false,
        morningEnabled: true,
        noonEnabled: true,
        eveningEnabled: true,
        morningDone: false,
        noonDone: false,
        eveningDone: false,
        resetHour: 4,
        deadlineDate: ''
      });
    }, true);
    A.showToast('已加入「' + rx.title + '」到康復室');
  }

  function renderSegmentedModeHtml(subMode) {
    return (
      '<div class="segmented-row">' +
        '<button type="button" class="seg-btn' + (subMode === 'catalog' ? ' active' : '') + '" data-rx-mode="catalog">' + ICONS.rx + '<span>處方目錄</span></button>' +
        '<button type="button" class="seg-btn' + (subMode === 'gym' ? ' active' : '') + '" data-rx-mode="gym">' + ICONS.spark + '<span>復健室</span></button>' +
      '</div>'
    );
  }

  /* ===================== CATALOG MODE ===================== */

  function renderCatalogMode(container) {
    var r = A.runtime;
    var st = A.state;
    var activeEnergy = D.ENERGY_LEVELS.filter(function (e) { return e.wireValue === r.rxEnergyFilter; })[0] || D.ENERGY_LEVELS[0];
    var filtered = D.PRESCRIPTIONS.filter(function (p) {
      return p.energyLevel === r.rxEnergyFilter && r.rxSelectedCategories.indexOf(p.category) >= 0;
    });
    if (!filtered.length) {
      filtered = D.PRESCRIPTIONS.filter(function (p) { return p.energyLevel === r.rxEnergyFilter; });
    }
    var picked = r.rxPickedCard;
    if (!picked || filtered.every(function (p) { return p.id !== picked.id; })) {
      picked = filtered[0] || D.PRESCRIPTIONS[0];
      r.rxPickedCard = picked;
    }

    var catDefs = [
      { wire: 'daily', label: '日常' },
      { wire: 'game', label: '遊戲' },
      { wire: 'homework', label: '作業' }
    ];

    var energyChipsHtml = D.ENERGY_LEVELS.map(function (e) {
      return '<button type="button" class="filter-chip' + (e.wireValue === r.rxEnergyFilter ? ' selected' : '') + '" data-rx-energy="' + e.wireValue + '">' + A.esc(e.shortTag) + '</button>';
    }).join('');

    var catChipsHtml = catDefs.map(function (c) {
      var sel = r.rxSelectedCategories.indexOf(c.wire) >= 0;
      return '<button type="button" class="filter-chip flex1' + (sel ? ' selected' : '') + '" data-rx-cat="' + c.wire + '">' + A.esc(c.label) + '</button>';
    }).join('');

    var pickedEffort = D.EFFORT_LEVELS.filter(function (e) { return e.wireValue === picked.effortLevel; })[0] || D.EFFORT_LEVELS[0];

    var listHtml = filtered.map(function (p) {
      var ef = D.EFFORT_LEVELS.filter(function (e) { return e.wireValue === p.effortLevel; })[0] || D.EFFORT_LEVELS[0];
      var catLbl = p.category === 'game' ? '遊戲' : p.category === 'homework' ? '作業' : '日常';
      return '<div class="card" style="padding:14px;display:flex;align-items:center;gap:12px">' +
        '<div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:4px">' +
          '<div style="display:flex;align-items:center;gap:6px;flex-wrap:wrap">' +
            '<span class="pill pill-primary" style="font-size:10.5px;padding:2px 8px">' + catLbl + '</span>' +
            '<span class="pill pill-secondary" style="font-size:10.5px;padding:2px 8px">' + A.esc(ef.labelZh) + '</span>' +
          '</div>' +
          '<div style="font-weight:700;font-size:15px">' + A.esc(p.title) + '</div>' +
          '<div style="font-size:12px;color:var(--on-surface-variant)">' + A.esc(p.doctorNote) + '</div>' +
        '</div>' +
        '<button type="button" class="btn btn-outline" style="height:38px;padding:0 12px;font-size:12.5px;flex-shrink:0" data-add-rx="' + A.esc(p.id) + '">排進今天</button>' +
      '</div>';
    }).join('');

    container.innerHTML =
      renderSegmentedModeHtml('catalog') +
      '<button type="button" class="doctor-banner" id="btn-rx-pat">' +
        '<div class="banner-avatar-frame">' +
          A.renderDoctorAvatarHtml(A.resolvePoseImage(r.rxPose), st.doctorGear, { height: '104px', alt: st.feifeiNickname }) +
        '</div>' +
        '<div class="banner-copy">' +
          '<div style="display:flex;align-items:center;gap:6px"><span class="pill pill-primary" style="padding:2px 8px;font-size:11px">' + A.esc(st.feifeiNickname) + '・低電量處方箋</span></div>' +
          '<div class="banner-quote">「' + A.esc(r.rxQuote) + '」</div>' +
          '<div class="banner-sub">' + A.esc(activeEnergy.descriptionZh) + '</div>' +
          '<span class="banner-tap-hint">' + ICONS.chat + ' 點一下醫師換一句醫囑</span>' +
        '</div>' +
      '</button>' +
      '<div class="card" style="padding:14px;display:flex;flex-direction:column;gap:10px">' +
        '<div class="field-label">目前剩餘電量篩選</div>' +
        '<div class="chip-row">' + energyChipsHtml + '</div>' +
        '<div class="field-label" style="margin-top:4px">療程類型（可複選）</div>' +
        '<div class="chip-row">' + catChipsHtml + '</div>' +
      '</div>' +
      '<div class="card card-soft" style="padding:16px;display:flex;flex-direction:column;gap:10px;border:1.5px solid var(--outline)">' +
        '<div style="display:flex;align-items:center;justify-content:space-between">' +
          '<span class="pill pill-primary">' + ICONS.dice + ' 今日隨緣處方籤</span>' +
          '<button type="button" class="btn btn-text" id="btn-rx-reroll" style="height:34px;font-size:13px">' + ICONS.dice + ' 換一張</button>' +
        '</div>' +
        '<div style="font-size:17px;font-weight:700">' + A.esc(picked.title) + '</div>' +
        '<div style="font-size:13px;color:var(--on-surface-variant)">' + A.esc(picked.doctorNote) + '（' + A.esc(pickedEffort.labelZh) + '）</div>' +
        '<button type="button" class="btn btn-primary btn-full" data-add-rx="' + A.esc(picked.id) + '">' + ICONS.plus + ' 排進我的康復室</button>' +
      '</div>' +
      '<div class="section-header-row"><h2 class="section-title">可選療程庫</h2><span class="section-hint">共 ' + filtered.length + ' 張</span></div>' +
      '<div style="display:flex;flex-direction:column;gap:10px">' + listHtml + '</div>';

    container.querySelectorAll('[data-rx-mode]').forEach(function (btn) {
      btn.onclick = function () {
        A.runtime.rxSubMode = btn.getAttribute('data-rx-mode');
        A.rerender();
      };
    });

    container.querySelector('#btn-rx-pat').onclick = function () {
      var poses = ['CALM', 'APPROVE', 'SIPPING', 'PHARMACY'];
      r.rxPose = poses[(poses.indexOf(r.rxPose) + 1) % poses.length];
      r.rxQuote = A.pickRandom(D.DOCTOR_QUOTES.prescriptionDoctorQuotes, r.rxQuote);
      r.rxPatCount += 1;
      A.rerender();
    };

    container.querySelectorAll('[data-rx-energy]').forEach(function (btn) {
      btn.onclick = function () {
        r.rxEnergyFilter = btn.getAttribute('data-rx-energy');
        r.rxPickedCard = null;
        A.rerender();
      };
    });

    container.querySelectorAll('[data-rx-cat]').forEach(function (btn) {
      btn.onclick = function () {
        var wire = btn.getAttribute('data-rx-cat');
        var idx = r.rxSelectedCategories.indexOf(wire);
        if (idx >= 0) {
          if (r.rxSelectedCategories.length <= 1) {
            A.showToast('至少選擇一個分類');
            return;
          }
          r.rxSelectedCategories.splice(idx, 1);
        } else {
          r.rxSelectedCategories.push(wire);
        }
        r.rxPickedCard = null;
        A.rerender();
      };
    });

    container.querySelector('#btn-rx-reroll').onclick = function () {
      r.rxPickedCard = A.pickRandom(filtered, picked);
      A.rerender();
    };

    container.querySelectorAll('[data-add-rx]').forEach(function (btn) {
      btn.onclick = function () {
        var id = btn.getAttribute('data-add-rx');
        var rx = D.PRESCRIPTIONS.filter(function (p) { return p.id === id; })[0];
        if (rx) addPrescriptionToToday(rx);
      };
    });
  }

  /* ===================== GYM DIALOGS & MINI-GAMES ===================== */

  function openGymIntroDialog() {
    var html =
      '<div class="dialog-panel">' +
        '<div style="font-size:19px;font-weight:700">' + A.esc(D.DOCTOR_QUOTES.gymFirstVisitTitle) + '</div>' +
        '<div style="font-size:14px;color:var(--on-surface-variant);line-height:1.6;white-space:pre-line">' + A.esc(D.DOCTOR_QUOTES.gymFirstVisitBody) + '</div>' +
        '<button type="button" class="btn btn-primary btn-full" id="gym-intro-ok">我知道了，開始復健</button>' +
      '</div>';
    var root = A.openModal(html, {
      center: true,
      onClose: function () { A.setState(function (s) { s.hasSeenRehabGymIntro = true; }, false); }
    });
    root.querySelector('#gym-intro-ok').onclick = function () {
      A.closeModal();
      A.setState(function (s) { s.hasSeenRehabGymIntro = true; }, true);
    };
  }

  function openSpecialChannelDialog(onLaunchMiniGame) {
    var statusText = '';
    function renderChannel() {
      var st = A.state;
      var unlocked = !!st.specialChannelUnlocked;
      var bodyHtml = '';
      if (!unlocked) {
        bodyHtml =
          '<div class="field-group">' +
            '<div class="field-label">輸入特別代碼</div>' +
            '<input type="number" inputmode="numeric" class="input-box" id="sc-code-input" placeholder="輸入代碼即可解鎖（可隨時上鎖關閉）" />' +
          '</div>';
      } else {
        var gameBtns = MINI_GAME_TYPES.map(function (g) {
          return '<button type="button" class="btn btn-outline btn-full" style="justify-content:flex-start" data-sc-launch="' + g.id + '">' + ICONS.spark + ' <span>' + A.esc(g.titleZh) + '</span></button>';
        }).join('');
        bodyHtml =
          '<div class="field-group">' +
            '<div class="field-label">復健點數直接調整</div>' +
            '<input type="number" inputmode="numeric" class="input-box" id="sc-points-input" value="' + st.rehabPoints + '"/>' +
            '<div style="display:flex;gap:8px;margin-top:4px">' +
              '<button type="button" class="btn btn-outline" style="flex:1" id="sc-plus-500">+500 點</button>' +
              '<button type="button" class="btn btn-primary" style="flex:1" id="sc-apply-pts">套用數值</button>' +
            '</div>' +
          '</div>' +
          '<div class="field-group">' +
            '<div class="field-label">指定開啟復健室小遊戲</div>' +
            '<div style="display:flex;flex-direction:column;gap:6px">' + gameBtns + '</div>' +
          '</div>' +
          '<button type="button" class="btn btn-outline-soft btn-full" id="sc-lock-btn">' + ICONS.lock + ' <span>關閉／上鎖特別通道</span></button>';
      }

      var html =
        '<div class="dialog-panel">' +
          '<div style="display:flex;align-items:center;justify-content:space-between">' +
            '<div style="font-size:19px;font-weight:700">特別通道</div>' +
            '<button type="button" class="icon-btn" id="sc-close">' + ICONS.close + '</button>' +
          '</div>' +
          '<div style="font-size:13px;color:var(--on-surface-variant)">目前復健點數：<strong style="color:var(--primary)">' + st.rehabPoints + ' 點</strong></div>' +
          bodyHtml +
          (statusText ? '<div style="font-size:12.5px;color:var(--primary);font-weight:700">' + A.esc(statusText) + '</div>' : '') +
        '</div>';

      var root = A.openModal(html, { center: true });
      root.querySelector('#sc-close').onclick = A.closeModal;

      var codeEl = root.querySelector('#sc-code-input');
      if (codeEl) {
        codeEl.oninput = function () {
          if (codeEl.value.trim() === '9487') {
            A.setState(function (s) { s.specialChannelUnlocked = true; }, true);
            statusText = '已開啟特別通道，關閉前都不需重複輸入代碼。';
            renderChannel();
          }
        };
      }

      var p500 = root.querySelector('#sc-plus-500');
      if (p500) {
        p500.onclick = function () {
          var next = Math.min(9999999, (A.state.rehabPoints || 0) + 500);
          A.setState(function (s) { s.rehabPoints = next; }, true);
          statusText = '已加發 +500 復健點（目前 ' + next + ' 點）。';
          renderChannel();
          A.rerender();
        };
      }

      var applyBtn = root.querySelector('#sc-apply-pts');
      if (applyBtn) {
        applyBtn.onclick = function () {
          var val = Math.max(0, Math.min(9999999, parseInt(root.querySelector('#sc-points-input').value, 10) || 0));
          A.setState(function (s) { s.rehabPoints = val; }, true);
          statusText = '已將復健點數設為 ' + val + ' 點。';
          renderChannel();
          A.rerender();
        };
      }

      root.querySelectorAll('[data-sc-launch]').forEach(function (btn) {
        btn.onclick = function () {
          var gid = btn.getAttribute('data-sc-launch');
          A.closeModal();
          onLaunchMiniGame(gid);
        };
      });

      var lockBtn = root.querySelector('#sc-lock-btn');
      if (lockBtn) {
        lockBtn.onclick = function () {
          A.setState(function (s) { s.specialChannelUnlocked = false; }, true);
          statusText = '已關閉特別通道，下次開啟需重新輸入代碼。';
          renderChannel();
        };
      }
    }
    renderChannel();
  }

  function launchMiniGame(gameType, onGameClosed) {
    var docName = A.state.feifeiNickname;
    var settled = false;
    var finished = false;
    var outcome = null; // WIN | LOSE | TIE | COMPLETED

    // Game-specific state
    var targetProp = A.pickRandom(FIND_PROPS);
    var targetX = Math.round(16 + Math.random() * 68);
    var targetY = Math.round(28 + Math.random() * 50);
    var secondsLeft = 15;
    var findTimer = null;

    var playerRps = null;
    var docRps = null;

    var allRanks = [2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14];
    for (var ri = allRanks.length - 1; ri > 0; ri--) {
      var rj = Math.floor(Math.random() * (ri + 1));
      var rtmp = allRanks[ri]; allRanks[ri] = allRanks[rj]; allRanks[rj] = rtmp;
    }
    var leftRank = allRanks[0];
    var rightRank = allRanks[1];
    var cardsRevealed = false;
    var pickedLeft = null;

    var rubMs = 0;
    var lastRubAt = 0;
    var isRubbing = false;
    var touchX = 0;
    var touchY = 0;
    var showHeart = false;

    var dialogue = gameType === 'RPS' ? D.DOCTOR_QUOTES.gymRpsStartQuote
      : gameType === 'HIGH_CARD' ? D.DOCTOR_QUOTES.gymHighCardStartQuote
      : gameType === 'MASSAGE' ? D.DOCTOR_QUOTES.gymMassageStartQuote
      : D.DOCTOR_QUOTES.gymFindObjectStartQuote(targetProp.labelZh);

    function awardPoints() {
      if (settled) return;
      settled = true;
      A.setState(function (s) {
        s.rehabPoints = Math.min(9999999, (s.rehabPoints || 0) + 60);
      }, true);
    }

    function cleanupGame() {
      if (findTimer) { clearInterval(findTimer); findTimer = null; }
      A.closeFullscreen();
      if (typeof onGameClosed === 'function') onGameClosed();
    }

    function getBannerText() {
      if (gameType === 'RPS') {
        if (outcome === 'WIN') return '勝利！復健點數 +60 點已入帳！';
        if (outcome === 'TIE') return '平手！本回合不計輸贏（+0 點）';
        if (outcome === 'LOSE') return '落敗！一局定勝負（+0 點）';
        return '來猜拳吧！一局定勝負（勝 +60 復健點，平手不計）';
      }
      if (gameType === 'HIGH_CARD') {
        if (cardsRevealed) {
          var pR = pickedLeft ? leftRank : rightRank;
          var dR = pickedLeft ? rightRank : leftRank;
          return outcome === 'WIN'
            ? '你的「' + cardRankLabel(pR) + '」大於「' + cardRankLabel(dR) + '」！獲得 +60 點！'
            : '你的「' + cardRankLabel(pR) + '」小於「' + cardRankLabel(dR) + '」（+0 點）';
        }
        return '撲克比大小！任選左右一張翻牌（勝 +60 復健點）';
      }
      if (gameType === 'FIND_OBJECT') {
        if (outcome === 'WIN') return '找到了「' + targetProp.labelZh + '」！復健點數 +60 點！';
        if (outcome === 'LOSE') return '時間到！沒能找到「' + targetProp.labelZh + '」（+0 點）';
        return '限時尋物：在房間找出「' + targetProp.labelZh + '」（剩 ' + secondsLeft + 's）';
      }
      return finished ? '安撫完成！復健點數 +60 點已入帳！' : '用手指滑動安撫他！（+60 復健點）';
    }

    function getDocPoseSrc() {
      if (gameType === 'MASSAGE' && finished) return './assets/img_rehab_doc_look_away.png';
      if (gameType === 'MASSAGE' && isRubbing) return './assets/img_rehab_doc_eyes_closed.png';
      if (outcome === 'WIN') return './assets/img_rehab_doc_thumbs_up.png';
      if (outcome === 'TIE') return './assets/img_rehab_doc_think.png';
      if (outcome === 'LOSE') return './assets/img_rehab_doc_arms_crossed.png';
      return './assets/img_rehab_doc_full.png';
    }

    function renderGame() {
      var bannerText = getBannerText();
      var docImgHtml = gameType === 'FIND_OBJECT' ? '' : '<img class="minigame-doc-full" id="mg-doc-pose" src="' + getDocPoseSrc() + '" alt="' + A.esc(docName) + '" />';

      var showDialogue = gameType !== 'RPS' || finished;
      var dialogueHtml = showDialogue
        ? '<div class="minigame-dialogue-card' + (gameType === 'FIND_OBJECT' ? ' centered' : '') + '">' +
            '<div class="minigame-speaker-tag">' + A.esc(docName) + '</div>' +
            '<div class="minigame-dialogue-body" id="mg-dialogue">' + A.esc(dialogue) + '</div>' +
          '</div>'
        : '';

      var midHtml = '';
      if (gameType === 'RPS' && finished && docRps) {
        var rpsObj = RPS_HANDS.filter(function (h) { return h.id === docRps; })[0];
        var cls = outcome === 'WIN' ? 'win' : outcome === 'TIE' ? 'tie' : 'lose';
        var ribbonTxt = outcome === 'WIN' ? 'YOU WIN !' : outcome === 'TIE' ? 'DRAW !' : 'YOU LOSE...';
        midHtml =
          '<div class="rps-result-wrap">' +
            '<div class="rps-result-ribbon ' + cls + '">' + ribbonTxt + '</div>' +
            '<div class="rps-doctor-card"><img src="' + rpsObj.img + '" alt="' + A.esc(rpsObj.labelZh) + '"/></div>' +
          '</div>';
      } else if (gameType === 'HIGH_CARD') {
        var leftBadge = !cardsRevealed ? '選左邊這張' : (pickedLeft ? '你的手牌' : docName + '的牌');
        var rightBadge = !cardsRevealed ? '選右邊這張' : (!pickedLeft ? '你的手牌' : docName + '的牌');
        midHtml =
          '<div class="poker-table">' +
            '<div class="poker-slot">' +
              '<span class="pill" style="background:' + (!cardsRevealed || pickedLeft ? '#FF6D8A' : 'rgba(74,49,52,0.85)') + ';color:#fff">' + A.esc(leftBadge) + '</span>' +
              '<button type="button" class="poker-card-btn" data-pick-card="left"' + (cardsRevealed ? ' disabled' : '') + '>' +
                '<img src="' + (cardsRevealed ? './assets/img_poker_cat_front.png' : './assets/img_poker_cat_back.png') + '" alt="撲克牌"/>' +
                (cardsRevealed ? '<span class="poker-rank-tl">' + cardRankLabel(leftRank) + '</span><span class="poker-rank-br">' + cardRankLabel(leftRank) + '</span>' : '') +
              '</button>' +
            '</div>' +
            '<div class="poker-slot">' +
              '<span class="pill" style="background:' + (!cardsRevealed || !pickedLeft ? '#FF6D8A' : 'rgba(74,49,52,0.85)') + ';color:#fff">' + A.esc(rightBadge) + '</span>' +
              '<button type="button" class="poker-card-btn" data-pick-card="right"' + (cardsRevealed ? ' disabled' : '') + '>' +
                '<img src="' + (cardsRevealed ? './assets/img_poker_cat_front.png' : './assets/img_poker_cat_back.png') + '" alt="撲克牌"/>' +
                (cardsRevealed ? '<span class="poker-rank-tl">' + cardRankLabel(rightRank) + '</span><span class="poker-rank-br">' + cardRankLabel(rightRank) + '</span>' : '') +
              '</button>' +
            '</div>' +
          '</div>';
      } else if (gameType === 'FIND_OBJECT') {
        midHtml =
          '<button type="button" class="find-prop-btn" id="mg-find-target" style="left:' + targetX + '%;top:' + targetY + '%">' +
            '<img src="' + targetProp.img + '" alt="' + A.esc(targetProp.labelZh) + '"/>' +
          '</button>';
      }

      var dockContent = '';
      if (gameType === 'RPS') {
        dockContent += '<div class="rps-strip">' + RPS_HANDS.map(function (h) {
          var isCh = playerRps === h.id;
          var cls = 'rps-choice-btn' + (isCh ? ' chosen' : (finished ? ' dimmed' : ''));
          return '<button type="button" class="' + cls + '" data-rps="' + h.id + '"' + (finished ? ' disabled' : '') + '><img src="' + h.img + '" alt="' + A.esc(h.labelZh) + '"/></button>';
        }).join('') + '</div>';
      } else if (gameType === 'MASSAGE') {
        var pct = Math.min(100, Math.round((rubMs / 5000) * 100));
        if (!finished) {
          dockContent += '<div style="width:min(560px,calc(100vw - 32px));display:flex;justify-content:flex-end">' +
            '<span class="pill" style="background:#FF6D8A;color:#fff;border:2px solid #fff">' + ICONS.heart + ' 在他身上滑動</span>' +
          '</div>';
        }
        dockContent +=
          '<div class="massage-progress-wrap">' +
            '<div class="massage-bar-track"><div class="massage-bar-fill" id="mg-rub-fill" style="width:' + pct + '%"></div></div>' +
            '<span id="mg-rub-pct" style="font-weight:700;font-size:14px;color:#3E292E">' + pct + '%</span>' +
          '</div>';
      }

      if (finished) {
        var finCls = (outcome === 'WIN' || outcome === 'COMPLETED') ? 'win' : outcome === 'TIE' ? 'tie' : 'lose';
        var finTxt = (outcome === 'WIN' || outcome === 'COMPLETED') ? '已收下 60 復健點數・點擊任意空白處返回復健室'
          : outcome === 'TIE' ? '本局平手・點擊任意空白處返回復健室' : '點擊任意空白處返回復健室';
        dockContent += '<div class="minigame-finish-pill ' + finCls + '">' + finTxt + '</div>';
      }

      var heartHtml = (gameType === 'MASSAGE' && showHeart && !finished)
        ? '<div class="massage-heart-cursor" id="mg-heart" style="left:' + touchX + 'px;top:' + touchY + 'px">' + ICONS.heart + '</div>'
        : '<div class="massage-heart-cursor hidden" id="mg-heart">' + ICONS.heart + '</div>';

      var html =
        '<div class="minigame-stage" id="mg-stage">' +
          '<img class="minigame-bg" src="./assets/img_rehab_gym_bg.jpg" alt=""/>' +
          '<div class="minigame-scrim"></div>' +
          docImgHtml +
          '<div class="minigame-top-row">' +
            '<div class="minigame-top-banner">' +
              '<span class="minigame-banner-badge">!</span>' +
              '<span class="minigame-banner-text" id="mg-banner">' + A.esc(bannerText) + '</span>' +
            '</div>' +
            '<button type="button" class="minigame-close-btn" id="mg-close" aria-label="離開小遊戲">' + ICONS.close + '</button>' +
          '</div>' +
          dialogueHtml +
          midHtml +
          heartHtml +
          '<div class="minigame-bottom-dock">' + dockContent + '</div>' +
        '</div>';

      var fsRoot = A.openFullscreen(html);
      fsRoot.querySelector('#mg-close').onclick = function (ev) {
        ev.stopPropagation();
        cleanupGame();
      };

      var stageEl = fsRoot.querySelector('#mg-stage');
      if (finished) {
        stageEl.onclick = function () { cleanupGame(); };
      }

      if (gameType === 'RPS' && !finished) {
        fsRoot.querySelectorAll('[data-rps]').forEach(function (btn) {
          btn.onclick = function (ev) {
            ev.stopPropagation();
            playerRps = btn.getAttribute('data-rps');
            docRps = RPS_HANDS[Math.floor(Math.random() * 3)].id;
            finished = true;
            var win = (playerRps === 'rock' && docRps === 'scissors') ||
              (playerRps === 'scissors' && docRps === 'paper') ||
              (playerRps === 'paper' && docRps === 'rock');
            if (win) {
              outcome = 'WIN';
              awardPoints();
              dialogue = A.pickRandom(D.DOCTOR_QUOTES.rpsWinQuotes);
            } else if (playerRps === docRps) {
              outcome = 'TIE';
              dialogue = A.pickRandom(D.DOCTOR_QUOTES.rpsTieQuotes);
            } else {
              outcome = 'LOSE';
              dialogue = A.pickRandom(D.DOCTOR_QUOTES.rpsLoseQuotes);
            }
            renderGame();
          };
        });
      } else if (gameType === 'HIGH_CARD' && !cardsRevealed) {
        fsRoot.querySelectorAll('[data-pick-card]').forEach(function (btn) {
          btn.onclick = function (ev) {
            ev.stopPropagation();
            pickedLeft = btn.getAttribute('data-pick-card') === 'left';
            var pR = pickedLeft ? leftRank : rightRank;
            var dR = pickedLeft ? rightRank : leftRank;
            cardsRevealed = true;
            finished = true;
            if (pR > dR) {
              outcome = 'WIN';
              awardPoints();
              dialogue = A.pickRandom(D.DOCTOR_QUOTES.highCardWinQuotes);
            } else {
              outcome = 'LOSE';
              dialogue = A.pickRandom(D.DOCTOR_QUOTES.highCardLoseQuotes);
            }
            renderGame();
          };
        });
      } else if (gameType === 'FIND_OBJECT' && !finished) {
        fsRoot.querySelector('#mg-find-target').onclick = function (ev) {
          ev.stopPropagation();
          if (findTimer) { clearInterval(findTimer); findTimer = null; }
          finished = true;
          outcome = 'WIN';
          awardPoints();
          dialogue = A.pickRandom(D.DOCTOR_QUOTES.findObjWinQuotes);
          renderGame();
        };
      } else if (gameType === 'MASSAGE' && !finished) {
        stageEl.onpointerdown = function (ev) {
          isRubbing = true;
          lastRubAt = Date.now();
          touchX = ev.clientX;
          touchY = ev.clientY;
          showHeart = true;
          var poseImg = fsRoot.querySelector('#mg-doc-pose');
          if (poseImg) poseImg.src = getDocPoseSrc();
          var heartEl = fsRoot.querySelector('#mg-heart');
          if (heartEl) {
            heartEl.classList.remove('hidden');
            heartEl.style.left = touchX + 'px';
            heartEl.style.top = touchY + 'px';
          }
        };
        stageEl.onpointermove = function (ev) {
          if (!isRubbing || finished) return;
          var now = Date.now();
          var dt = lastRubAt ? Math.min(70, Math.max(10, now - lastRubAt)) : 16;
          lastRubAt = now;
          rubMs = Math.min(5000, rubMs + dt);
          var ratio = rubMs / 5000;
          var pct = Math.round(ratio * 100);
          touchX = ev.clientX;
          touchY = ev.clientY;

          var heartEl = fsRoot.querySelector('#mg-heart');
          if (heartEl) {
            heartEl.style.left = touchX + 'px';
            heartEl.style.top = touchY + 'px';
          }
          var fillEl = fsRoot.querySelector('#mg-rub-fill');
          var pctEl = fsRoot.querySelector('#mg-rub-pct');
          if (fillEl) fillEl.style.width = pct + '%';
          if (pctEl) pctEl.textContent = pct + '%';

          if (ratio >= 0.35 && ratio <= 0.42) {
            dialogue = D.DOCTOR_QUOTES.gymMassageMidEarlyQuote;
            var dEl = fsRoot.querySelector('#mg-dialogue');
            if (dEl) dEl.textContent = dialogue;
          } else if (ratio >= 0.7 && ratio <= 0.78) {
            dialogue = D.DOCTOR_QUOTES.gymMassageMidLateQuote;
            var dEl2 = fsRoot.querySelector('#mg-dialogue');
            if (dEl2) dEl2.textContent = dialogue;
          }

          if (rubMs >= 5000) {
            isRubbing = false;
            showHeart = false;
            finished = true;
            outcome = 'COMPLETED';
            awardPoints();
            dialogue = A.pickRandom(D.DOCTOR_QUOTES.massageCompleteQuotes);
            renderGame();
          }
        };
        function stopRub() {
          if (finished) return;
          isRubbing = false;
          showHeart = false;
          var poseImg = fsRoot.querySelector('#mg-doc-pose');
          if (poseImg) poseImg.src = getDocPoseSrc();
          var heartEl = fsRoot.querySelector('#mg-heart');
          if (heartEl) heartEl.classList.add('hidden');
        }
        stageEl.onpointerup = stopRub;
        stageEl.onpointercancel = stopRub;
      }
    }

    if (gameType === 'FIND_OBJECT') {
      findTimer = setInterval(function () {
        if (finished) return;
        secondsLeft -= 1;
        var bEl = document.getElementById('mg-banner');
        if (secondsLeft > 0) {
          if (bEl) bEl.textContent = getBannerText();
        } else {
          clearInterval(findTimer);
          findTimer = null;
          finished = true;
          outcome = 'LOSE';
          dialogue = A.pickRandom(D.DOCTOR_QUOTES.findObjLoseQuotes);
          renderGame();
        }
      }, 1000);
    }

    renderGame();
  }

  /* ===================== GYM MODE ===================== */

  function renderGymMode(container) {
    A.accrueIdlePointsOnState(A.state);
    A.saveState(A.state);

    var docX = 0.52;
    var docY = 0.76;
    var facingRight = false;
    var isWalking = false;
    var isTurning = false;
    var walkFrameIdx = 0;
    var speechLine = D.DOCTOR_QUOTES.gymWalkLines[0];
    var exclamationActive = false;
    var miniGameOpen = false;
    var walkSprites = [
      './assets/img_doc_walk_1.png',
      './assets/img_doc_walk_mid.png',
      './assets/img_doc_walk_opp.png',
      './assets/img_doc_walk_pass.png'
    ];
    var floorSpots = [[0.24, 0.78], [0.42, 0.71], [0.58, 0.80], [0.74, 0.73], [0.48, 0.66], [0.66, 0.69]];
    var currentSpriteSrc = './assets/img_doc_idle_1.png';
    var isSide = false;

    container.innerHTML =
      '<div class="gym-shell">' +
        '<div class="gym-top-toggle-wrap">' + renderSegmentedModeHtml('gym') + '</div>' +
        '<div class="gym-stage" id="gym-stage">' +
          '<img class="gym-bg-img" src="./assets/img_rehab_gym_bg.jpg" alt="復健室背景"/>' +
          '<div class="gym-hud-row">' +
            '<div class="gym-points-pill">' +
              '<span id="gym-pts-label">復健點數 ' + A.state.rehabPoints + '</span>' +
              '<span class="gym-rate-badge">' + ICONS.spark + ' +60/hr</span>' +
            '</div>' +
            '<div style="display:flex;gap:8px">' +
              '<button type="button" class="gym-hud-btn info" id="gym-info-btn" aria-label="復健室玩法說明">' + ICONS.info + '</button>' +
              '<button type="button" class="gym-hud-btn menu" id="gym-menu-btn" aria-label="特別通道選單">' + ICONS.menu + '</button>' +
            '</div>' +
          '</div>' +
          '<div class="gym-doc-shadow" id="gym-doc-shadow" style="left:52%;top:76%"></div>' +
          '<div class="gym-speech-bubble" id="gym-bubble" style="left:52%;top:55%">' + A.esc(speechLine) + '</div>' +
          '<button type="button" class="gym-exclamation-btn hidden" id="gym-excl-btn" style="left:52%;top:55%" aria-label="復健室驚嘆號事件">!</button>' +
          '<div class="gym-doc-wrap" id="gym-doc-wrap" style="left:52%;top:76%">' +
            '<div class="gym-doc-inner" id="gym-doc-inner">' +
              A.renderDoctorAvatarHtml(currentSpriteSrc, A.state.doctorGear, { height: '108px', alt: A.state.feifeiNickname }) +
            '</div>' +
          '</div>' +
        '</div>' +
      '</div>';

    var wrapEl = container.querySelector('#gym-doc-wrap');
    var innerEl = container.querySelector('#gym-doc-inner');
    var shadowEl = container.querySelector('#gym-doc-shadow');
    var bubbleEl = container.querySelector('#gym-bubble');
    var exclEl = container.querySelector('#gym-excl-btn');
    var ptsEl = container.querySelector('#gym-pts-label');

    function updateDocDOM() {
      if (!wrapEl) return;
      var xPct = (docX * 100).toFixed(2) + '%';
      var yPct = (docY * 100).toFixed(2) + '%';
      wrapEl.style.left = xPct;
      wrapEl.style.top = yPct;
      shadowEl.style.left = xPct;
      shadowEl.style.top = yPct;

      var clampedBubbleX = A.clamp(docX * 100, 18, 82).toFixed(1) + '%';
      var bubbleTop = Math.max(22, docY * 100 - 21).toFixed(1) + '%';
      bubbleEl.style.left = clampedBubbleX;
      bubbleEl.style.top = bubbleTop;
      exclEl.style.left = xPct;
      exclEl.style.top = bubbleTop;

      var img = innerEl.querySelector('img');
      if (img && img.getAttribute('src') !== currentSpriteSrc) {
        innerEl.innerHTML = A.renderDoctorAvatarHtml(currentSpriteSrc, A.state.doctorGear, {
          height: '108px',
          isSideProfile: isSide,
          alt: A.state.feifeiNickname
        });
      }
      var bobY = (isWalking && walkFrameIdx % 2 === 1) ? -3 : 0;
      innerEl.style.transform = 'translateY(' + bobY + 'px) scaleX(' + (facingRight ? -1 : 1) + ')';
    }

    function openNextMiniGame(specificId) {
      miniGameOpen = true;
      var chosen = specificId || pickNextRandomGame();
      if (!specificId) {
        exclamationActive = false;
        exclEl.classList.add('hidden');
        bubbleEl.classList.remove('hidden');
      }
      launchMiniGame(chosen, function () {
        miniGameOpen = false;
        if (ptsEl) ptsEl.textContent = '復健點數 ' + A.state.rehabPoints;
      });
    }

    container.querySelectorAll('[data-rx-mode]').forEach(function (btn) {
      btn.onclick = function () {
        A.runtime.rxSubMode = btn.getAttribute('data-rx-mode');
        A.rerender();
      };
    });

    container.querySelector('#gym-info-btn').onclick = openGymIntroDialog;
    container.querySelector('#gym-menu-btn').onclick = function () {
      openSpecialChannelDialog(function (gid) { openNextMiniGame(gid); });
    };

    exclEl.onclick = function () { openNextMiniGame(null); };
    wrapEl.onclick = function () {
      if (exclamationActive) {
        openNextMiniGame(null);
      } else {
        speechLine = A.pickRandom(D.DOCTOR_QUOTES.all, speechLine);
        bubbleEl.textContent = speechLine;
        innerEl.classList.add('bounce');
        setTimeout(function () { if (innerEl) innerEl.classList.remove('bounce'); }, 220);
      }
    };

    if (!A.state.hasSeenRehabGymIntro) {
      openGymIntroDialog();
    }

    // Idle points tick (30s)
    var idleTick = setInterval(function () {
      var gained = A.accrueIdlePointsOnState(A.state);
      if (gained > 0) {
        A.saveState(A.state);
        if (ptsEl) ptsEl.textContent = '復健點數 ' + A.state.rehabPoints;
      }
    }, 30000);

    // Exclamation spawn timer (18~42s)
    var exclTimer = null;
    function scheduleExcl() {
      exclTimer = setTimeout(function () {
        if (!miniGameOpen && !exclamationActive) {
          exclamationActive = true;
          exclEl.classList.remove('hidden');
          bubbleEl.classList.add('hidden');
        }
        scheduleExcl();
      }, 18000 + Math.random() * 24000);
    }
    scheduleExcl();

    // Sprite frame loop (135ms)
    var idleTickCount = 0;
    var spriteInterval = setInterval(function () {
      if (miniGameOpen) return;
      if (isTurning) {
        currentSpriteSrc = './assets/img_doc_turn.png';
        isSide = true;
      } else if (isWalking) {
        walkFrameIdx = (walkFrameIdx + 1) % walkSprites.length;
        currentSpriteSrc = walkSprites[walkFrameIdx];
        isSide = true;
      } else {
        idleTickCount += 1;
        currentSpriteSrc = (idleTickCount % 18 === 0) ? './assets/img_doc_idle_2.png' : './assets/img_doc_idle_1.png';
        isSide = false;
      }
      updateDocDOM();
    }, 135);

    // Smooth walk loop
    var rafId = null;
    var stepTimer = null;
    var patrolStep = 0;

    function startNextWalk() {
      stepTimer = setTimeout(function () {
        if (miniGameOpen) { startNextWalk(); return; }
        patrolStep += 1;
        var cands = floorSpots.filter(function (pt) { return Math.abs(pt[0] - docX) + Math.abs(pt[1] - docY) > 0.08; });
        var target = A.pickRandom(cands.length ? cands : floorSpots);
        var tx = target[0], ty = target[1];
        var dx = tx - docX;
        var wantsRight = Math.abs(dx) > 0.015 ? dx > 0 : facingRight;

        function beginMove() {
          var sx = docX, sy = docY;
          var dist = Math.abs(tx - sx) + Math.abs(ty - sy);
          var dur = A.clamp(dist * 4500, 1000, 2100);
          var startT = performance.now();
          isWalking = true;

          function stepRaf(now) {
            var p = Math.min(1, (now - startT) / dur);
            docX = sx + (tx - sx) * p;
            docY = sy + (ty - sy) * p;
            updateDocDOM();
            if (p < 1) {
              rafId = requestAnimationFrame(stepRaf);
            } else {
              isWalking = false;
              if (patrolStep % 3 === 0) {
                speechLine = A.pickRandom(D.DOCTOR_QUOTES.gymWalkLines, speechLine);
                bubbleEl.textContent = speechLine;
              }
              updateDocDOM();
              startNextWalk();
            }
          }
          rafId = requestAnimationFrame(stepRaf);
        }

        if (wantsRight !== facingRight) {
          isTurning = true;
          updateDocDOM();
          setTimeout(function () {
            facingRight = wantsRight;
            isTurning = false;
            beginMove();
          }, 220);
        } else {
          beginMove();
        }
      }, 2400);
    }
    startNextWalk();

    A.registerCleanup(function () {
      clearInterval(idleTick);
      clearInterval(spriteInterval);
      if (exclTimer) clearTimeout(exclTimer);
      if (stepTimer) clearTimeout(stepTimer);
      if (rafId) cancelAnimationFrame(rafId);
    });
  }

  A.renderPrescriptionsTab = function (container) {
    if (A.runtime.rxSubMode === 'gym') {
      renderGymMode(container);
    } else {
      renderCatalogMode(container);
    }
  };
})();
