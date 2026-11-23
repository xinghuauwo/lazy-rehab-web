/* 懶癌康復室 — Tab 5 (2.5D 療癒病房互動、電量滑桿、日記保存、月曆行程) */
(function () {
  'use strict';

  var A = window.REHAB_APP;
  var D = A.D;
  var ICONS = A.ICONS;

  /* Exact WardRoomView occlusion & anchor-drop geometry from WardRoomView.kt / RehabModels.kt */
  function isDoctorBehindBed(x, y) {
    return x >= 0.21 && x <= 0.56 && y >= 0.42 && y <= 0.65;
  }

  function isDoctorBehindIv(x, y) {
    return x >= 0.16 && x <= 0.27 && y >= 0.45 && y <= 0.72;
  }

  function isDoctorBehindChair(x, y) {
    return x >= 0.77 && x <= 0.93 && y >= 0.56 && y <= 0.72;
  }

  function nearestWardAnchor(x, y) {
    var best = D.WARD_ANCHORS[0];
    var bestDist = Infinity;
    for (var i = 0; i < D.WARD_ANCHORS.length; i++) {
      var a = D.WARD_ANCHORS[i];
      var d = Math.hypot(a.xFraction - x, a.yFraction - y);
      if (d < bestDist) { bestDist = d; best = a; }
    }
    return best;
  }

  function resolveWardDropAnchor(x, y) {
    if (x >= 0.77 && x <= 0.93 && y >= 0.51 && y <= 0.75) {
      return D.WARD_ANCHORS.filter(function (a) { return a.id === 'anchor_chair_side'; })[0];
    }
    if (x >= 0.25 && x <= 0.38 && y >= 0.40 && y <= 0.52) {
      return D.WARD_ANCHORS.filter(function (a) { return a.id === 'anchor_bed_pillow'; })[0];
    }
    if (x >= 0.27 && x <= 0.55 && y >= 0.50 && y <= 0.67) {
      return D.WARD_ANCHORS.filter(function (a) { return a.id === 'anchor_bed_blanket'; })[0];
    }
    var near = nearestWardAnchor(x, y);
    var dist = Math.hypot(near.xFraction - x, near.yFraction - y);
    return dist <= 0.12 ? near : null;
  }

  function itemMetaById(id) {
    return D.PHARMACY_ITEMS.filter(function (x) { return x.id === id; })[0];
  }

  function propVisualSizePx(itemId, stageW) {
    var scale = (stageW || 380) / 380;
    var base = itemId === 'rug_cloud' ? 88
      : (itemId === 'plush_bunny' || itemId === 'sofa_cloud' || itemId === 'tea_table') ? 60
      : (itemId === 'lamp_star' || itemId === 'iv_milk_tea' || itemId === 'sleep_plant') ? 54
      : 48;
    return Math.round(base * scale);
  }

  /* ===================== READ-ONLY SNAPSHOT VIEW ===================== */

  function renderWardSnapshotHtml(placements, snapshot, gear) {
    var list = placements || [];
    var doc = snapshot || { xFraction: 0.52, yFraction: 0.78, facingRight: false, spriteName: './assets/img_doc_idle_1.png' };
    var spriteSrc = doc.spriteName && doc.spriteName.indexOf('./') === 0
      ? doc.spriteName
      : (doc.spriteName === 'lying' ? './assets/img_ward_doc_lying.png'
        : doc.spriteName === 'sitting' ? './assets/img_doc_sitting.png'
        : doc.spriteName === 'interact' ? './assets/img_doc_interact.png'
        : './assets/img_doc_idle_1.png');
    var isLying = spriteSrc.indexOf('lying') >= 0;

    var propsHtml = list.map(function (p) {
      var meta = itemMetaById(p.itemId);
      if (!meta) return '';
      var z = Math.round(p.yFraction * 10);
      var wPct = p.itemId === 'rug_cloud' ? 22 : 13;
      return '<div class="ward-prop-sprite static" style="left:' + (p.xFraction * 100).toFixed(1) + '%;top:' + (p.yFraction * 100).toFixed(1) + '%;z-index:' + z + ';width:' + wPct + '%">' +
        '<div class="ward-prop-body" style="width:100%"><img src="' + A.esc(meta.imgSrc) + '" alt="' + A.esc(meta.name) + '"/></div>' +
      '</div>';
    }).join('');

    var docHtml =
      '<div class="ward-doctor-visual" style="left:' + (doc.xFraction * 100).toFixed(1) + '%;top:' + (doc.yFraction * 100).toFixed(1) + '%;height:' + (isLying ? '12.5%' : '22%') + ';transform:translate(-50%,-100%) scaleX(' + (doc.facingRight ? -1 : 1) + ')">' +
        A.renderDoctorAvatarHtml(spriteSrc, gear || A.state.doctorGear, {
          height: '100%',
          aspect: isLying ? 1.65 : 0.556,
          hideGear: isLying,
          alt: A.state.feifeiNickname
        }) +
      '</div>';

    return (
      '<div class="ward-room-shell">' +
        '<img class="ward-bg-img" src="./assets/img_ward_room_bg.png" alt="病房快照"/>' +
        propsHtml +
        docHtml +
        (isDoctorBehindBed(doc.xFraction, doc.yFraction) ? '<img class="ward-occlusion ward-occlusion-bed" src="./assets/img_ward_room_bg.png" alt=""/>' : '') +
        (isDoctorBehindIv(doc.xFraction, doc.yFraction) ? '<img class="ward-occlusion ward-occlusion-iv" src="./assets/img_ward_room_bg.png" alt=""/>' : '') +
        (isDoctorBehindChair(doc.xFraction, doc.yFraction) ? '<img class="ward-occlusion ward-occlusion-chair" src="./assets/img_ward_room_bg.png" alt=""/>' : '') +
      '</div>'
    );
  }

  /* ===================== INTERACTIVE 2.5D WARD ===================== */

  function mountInteractiveWardRoom(hostEl, uiOpts, callbacks) {
    var st = A.state;
    var placements = st.wardPlacements || [];
    var snap = st.wardDoctorSnapshot || { xFraction: 0.52, yFraction: 0.78, facingRight: false, pose: 'CALM', spriteName: './assets/img_doc_idle_1.png' };

    var docX = A.clamp(snap.xFraction || 0.52, 0.14, 0.9);
    var docY = A.clamp(snap.yFraction || 0.78, 0.42, 0.93);
    var facingRight = !!snap.facingRight;
    var isDraggingDoc = false;
    var isHoldingAnchorPose = snap.spriteName === './assets/img_doc_sitting.png' || snap.spriteName === './assets/img_ward_doc_lying.png';
    var isWalking = false;
    var isTurning = false;
    var walkFrameIdx = 0;
    var currentSpriteSrc = snap.spriteName || './assets/img_doc_idle_1.png';
    var isLying = currentSpriteSrc.indexOf('lying') >= 0;
    var isSide = false;
    var speech = A.runtime.wardSpeech || '';
    var highlightedBubble = false;
    var hoveredAnchorId = null;

    var walkSprites = [
      './assets/img_doc_walk_1.png',
      './assets/img_doc_walk_mid.png',
      './assets/img_doc_walk_opp.png',
      './assets/img_doc_walk_pass.png'
    ];
    var floorSpots = [
      [0.45, 0.77], [0.58, 0.80], [0.68, 0.74], [0.74, 0.65], [0.62, 0.60], [0.50, 0.69]
    ];

    function persistSnapshot() {
      A.setState(function (s) {
        s.wardDoctorSnapshot = {
          xFraction: docX,
          yFraction: docY,
          facingRight: facingRight,
          pose: A.runtime.wardPose || 'CALM',
          spriteName: currentSpriteSrc
        };
      }, true);
    }

    function setSpeech(msg, highlight) {
      speech = msg;
      highlightedBubble = !!highlight;
      A.runtime.wardSpeech = msg;
      var bEl = hostEl.querySelector('#ward-speech');
      if (bEl) {
        if (msg) {
          bEl.textContent = msg;
          bEl.classList.remove('hidden');
          bEl.classList.toggle('highlight', !!highlight);
        } else {
          bEl.classList.add('hidden');
        }
      }
    }

    function renderAnchorsHtml() {
      if (!uiOpts.showAnchors) return '';
      return D.WARD_ANCHORS.map(function (a) {
        var occupied = placements.some(function (p) { return nearestWardAnchor(p.xFraction, p.yFraction).id === a.id; });
        var hov = hoveredAnchorId === a.id;
        return '<button type="button" class="ward-anchor-dot' + (occupied ? ' occupied' : '') + (hov ? ' hovered' : '') + '" data-ward-anchor="' + A.esc(a.id) + '" style="left:' + (a.xFraction * 100) + '%;top:' + (a.yFraction * 100) + '%">' +
          '<span class="ward-anchor-circle"><span class="ward-anchor-inner"></span></span>' +
          '<span class="ward-anchor-label">' + A.esc(a.labelZh) + '</span>' +
        '</button>';
      }).join('');
    }

    function renderPropsHtml(stageW) {
      return placements.map(function (p) {
        var meta = itemMetaById(p.itemId);
        if (!meta) return '';
        var px = propVisualSizePx(p.itemId, stageW);
        var z = Math.round(p.yFraction * 10);
        return '<div class="ward-prop-sprite' + (uiOpts.isEditingProps ? '' : ' static') + '" data-ward-prop="' + A.esc(p.itemId) + '" style="left:' + (p.xFraction * 100).toFixed(2) + '%;top:' + (p.yFraction * 100).toFixed(2) + '%;z-index:' + z + '">' +
          (uiOpts.isEditingProps ? '<span class="ward-prop-tag">拖曳移動</span>' : '') +
          '<div class="ward-prop-body" style="width:' + px + 'px;height:' + px + 'px"><img src="' + A.esc(meta.imgSrc) + '" alt="' + A.esc(meta.name) + '"/></div>' +
        '</div>';
      }).join('');
    }

    function updateDoctorVisualOnly() {
      var visEl = hostEl.querySelector('#ward-doc-visual');
      var hitEl = hostEl.querySelector('#ward-doc-hitbox');
      var bEl = hostEl.querySelector('#ward-speech');
      if (!visEl || !hitEl) return;

      var xPct = (docX * 100).toFixed(2) + '%';
      var yPct = (docY * 100).toFixed(2) + '%';
      visEl.style.left = xPct;
      visEl.style.top = yPct;
      visEl.style.height = isLying ? '12.5%' : '22%';
      hitEl.style.left = xPct;
      hitEl.style.top = yPct;

      var inner = visEl.querySelector('#ward-doc-inner');
      var img = inner && inner.querySelector('img');
      var needsRedraw = !img || img.getAttribute('src') !== currentSpriteSrc;
      if (needsRedraw && inner) {
        inner.innerHTML = A.renderDoctorAvatarHtml(currentSpriteSrc, st.doctorGear, {
          height: '100%',
          aspect: isLying ? 1.65 : (isSide ? 0.5 : 0.556),
          isSideProfile: isSide,
          hideGear: isLying,
          alt: st.feifeiNickname
        });
      }
      if (inner) {
        var bob = (isWalking && walkFrameIdx % 2 === 1) ? -2 : 0;
        inner.style.transform = 'translateY(' + bob + 'px) scaleX(' + (facingRight ? -1 : 1) + ')';
      }

      if (bEl) {
        bEl.style.left = A.clamp(docX * 100, 19, 81).toFixed(1) + '%';
        bEl.style.top = Math.max(16, docY * 100 - (isLying ? 15 : 23)).toFixed(1) + '%';
      }

      // Occlusion depth layers
      var bedOcc = hostEl.querySelector('#ward-occ-bed');
      var ivOcc = hostEl.querySelector('#ward-occ-iv');
      var chairOcc = hostEl.querySelector('#ward-occ-chair');
      if (bedOcc) bedOcc.classList.toggle('hidden', isDraggingDoc || !isDoctorBehindBed(docX, docY));
      if (ivOcc) ivOcc.classList.toggle('hidden', isDraggingDoc || !isDoctorBehindIv(docX, docY));
      if (chairOcc) chairOcc.classList.toggle('hidden', isDraggingDoc || !isDoctorBehindChair(docX, docY));
    }

    function buildShell() {
      var stageW = hostEl.clientWidth || 380;
      var hasIv = placements.some(function (p) { return p.itemId === 'iv_milk_tea'; });
      var xPct = (docX * 100).toFixed(2) + '%';
      var yPct = (docY * 100).toFixed(2) + '%';

      hostEl.innerHTML =
        '<div class="ward-room-shell card-framed" id="ward-shell">' +
          '<img class="ward-bg-img" src="./assets/img_ward_room_bg.png" alt="病房空間"/>' +
          '<div class="ward-top-pill">' + A.esc(uiOpts.isEditingProps ? '拖曳道具擺放或點擊錨點' : '可點醫生或拖曳他到椅子、床邊休息') + '</div>' +
          (hasIv ? '<div class="ward-iv-badge">全糖奶茶點滴中</div>' : '') +
          '<div id="ward-anchors-layer">' + renderAnchorsHtml() + '</div>' +
          '<div id="ward-props-layer">' + renderPropsHtml(stageW) + '</div>' +
          '<div class="ward-doctor-visual" id="ward-doc-visual" style="left:' + xPct + ';top:' + yPct + ';height:' + (isLying ? '12.5%' : '22%') + '">' +
            '<div class="ward-doctor-sprite-inner" id="ward-doc-inner" style="height:100%">' +
              A.renderDoctorAvatarHtml(currentSpriteSrc, st.doctorGear, {
                height: '100%',
                aspect: isLying ? 1.65 : (isSide ? 0.5 : 0.556),
                isSideProfile: isSide,
                hideGear: isLying,
                alt: st.feifeiNickname
              }) +
            '</div>' +
          '</div>' +
          '<div class="ward-doctor-hitbox" id="ward-doc-hitbox" style="left:' + xPct + ';top:' + yPct + ';width:15%;height:24%"></div>' +
          '<img class="ward-occlusion ward-occlusion-bed' + (isDoctorBehindBed(docX, docY) ? '' : ' hidden') + '" id="ward-occ-bed" src="./assets/img_ward_room_bg.png" alt=""/>' +
          '<img class="ward-occlusion ward-occlusion-iv' + (isDoctorBehindIv(docX, docY) ? '' : ' hidden') + '" id="ward-occ-iv" src="./assets/img_ward_room_bg.png" alt=""/>' +
          '<img class="ward-occlusion ward-occlusion-chair' + (isDoctorBehindChair(docX, docY) ? '' : ' hidden') + '" id="ward-occ-chair" src="./assets/img_ward_room_bg.png" alt=""/>' +
          '<div class="ward-speech-bubble' + (speech ? '' : ' hidden') + (highlightedBubble ? ' highlight' : '') + '" id="ward-speech">' + A.esc(speech) + '</div>' +
        '</div>';

      updateDoctorVisualOnly();

      // Anchor click placement
      hostEl.querySelectorAll('[data-ward-anchor]').forEach(function (btn) {
        btn.onclick = function () {
          var aid = btn.getAttribute('data-ward-anchor');
          var anchor = D.WARD_ANCHORS.filter(function (a) { return a.id === aid; })[0];
          if (anchor && typeof callbacks.onSelectAnchor === 'function') {
            callbacks.onSelectAnchor(anchor);
          }
        };
      });

      // Prop interaction or drag
      hostEl.querySelectorAll('[data-ward-prop]').forEach(function (el) {
        var pid = el.getAttribute('data-ward-prop');
        if (!uiOpts.isEditingProps) {
          el.onclick = function () {
            el.classList.add('interacting');
            setTimeout(function () { el.classList.remove('interacting'); }, 350);
            setSpeech(D.DOCTOR_QUOTES.wardItemInteractionLine(pid), true);
          };
          return;
        }
        el.onpointerdown = function (ev) {
          ev.preventDefault();
          var shell = hostEl.querySelector('#ward-shell');
          if (!shell) return;
          var rect = shell.getBoundingClientRect();
          el.classList.add('dragging');
          var fx = A.clamp((ev.clientX - rect.left) / rect.width, 0.08, 0.92);
          var fy = A.clamp((ev.clientY - rect.top) / rect.height, 0.30, 0.94);

          function onMove(mv) {
            fx = A.clamp((mv.clientX - rect.left) / rect.width, 0.08, 0.92);
            fy = A.clamp((mv.clientY - rect.top) / rect.height, 0.30, 0.94);
            el.style.left = (fx * 100).toFixed(2) + '%';
            el.style.top = (fy * 100).toFixed(2) + '%';
          }
          function onUp() {
            el.classList.remove('dragging');
            window.removeEventListener('pointermove', onMove);
            window.removeEventListener('pointerup', onUp);
            window.removeEventListener('pointercancel', onUp);
            var near = nearestWardAnchor(fx, fy);
            var snapDist = Math.hypot(near.xFraction - fx, near.yFraction - fy);
            var finalX = snapDist < 0.09 ? near.xFraction : fx;
            var finalY = snapDist < 0.09 ? near.yFraction : fy;
            if (typeof callbacks.onMoveItem === 'function') {
              callbacks.onMoveItem(pid, finalX, finalY);
            }
          }
          window.addEventListener('pointermove', onMove);
          window.addEventListener('pointerup', onUp);
          window.addEventListener('pointercancel', onUp);
        };
      });

      // Doctor tap & drag-drop
      var hitEl = hostEl.querySelector('#ward-doc-hitbox');
      hitEl.onpointerdown = function (ev) {
        ev.preventDefault();
        var shell = hostEl.querySelector('#ward-shell');
        if (!shell) return;
        var rect = shell.getBoundingClientRect();
        var startX = ev.clientX;
        var startY = ev.clientY;
        var moved = false;

        function onMove(mv) {
          var distPx = Math.hypot(mv.clientX - startX, mv.clientY - startY);
          if (!moved && distPx > 8) {
            moved = true;
            isDraggingDoc = true;
            isWalking = false;
            isTurning = false;
            isHoldingAnchorPose = false;
            isLying = false;
            isSide = false;
            currentSpriteSrc = './assets/img_doc_picked_up.png';
            hitEl.classList.add('dragging');
            setSpeech(A.pickRandom(D.DOCTOR_QUOTES.wardDoctorDragStartLines), true);
          }
          if (moved) {
            docX = A.clamp((mv.clientX - rect.left) / rect.width, 0.12, 0.90);
            docY = A.clamp((mv.clientY - rect.top) / rect.height, 0.36, 0.94);
            var dropCand = resolveWardDropAnchor(docX, docY);
            var nextHov = dropCand ? dropCand.id : null;
            if (nextHov !== hoveredAnchorId) {
              hoveredAnchorId = nextHov;
              var aLayer = hostEl.querySelector('#ward-anchors-layer');
              if (aLayer && uiOpts.showAnchors) aLayer.innerHTML = renderAnchorsHtml();
            }
            updateDoctorVisualOnly();
          }
        }

        function onUp() {
          window.removeEventListener('pointermove', onMove);
          window.removeEventListener('pointerup', onUp);
          window.removeEventListener('pointercancel', onUp);
          hitEl.classList.remove('dragging');
          hoveredAnchorId = null;
          var aLayer = hostEl.querySelector('#ward-anchors-layer');
          if (aLayer && uiOpts.showAnchors) aLayer.innerHTML = renderAnchorsHtml();

          if (!moved) {
            isDraggingDoc = false;
            isHoldingAnchorPose = false;
            isLying = false;
            isSide = false;
            currentSpriteSrc = './assets/img_doc_interact.png';
            setSpeech(A.pickRandom(D.DOCTOR_QUOTES.wardDoctorTapLines), true);
            updateDoctorVisualOnly();
            setTimeout(function () {
              if (!isDraggingDoc && !isWalking) {
                currentSpriteSrc = './assets/img_doc_idle_1.png';
                updateDoctorVisualOnly();
              }
            }, 900);
            return;
          }

          isDraggingDoc = false;
          var anchor = resolveWardDropAnchor(docX, docY);
          if (anchor && anchor.id === 'anchor_chair_side') {
            docX = 0.845; docY = 0.675; facingRight = false; isLying = false; isSide = false;
            currentSpriteSrc = './assets/img_doc_sitting.png';
            isHoldingAnchorPose = true;
            setSpeech(D.DOCTOR_QUOTES.wardDoctorDropLinesByAnchor[anchor.id], true);
          } else if (anchor && anchor.id === 'anchor_bed_pillow') {
            docX = 0.345; docY = 0.545; facingRight = false; isLying = true; isSide = false;
            currentSpriteSrc = './assets/img_ward_doc_lying.png';
            isHoldingAnchorPose = true;
            setSpeech(D.DOCTOR_QUOTES.wardDoctorDropLinesByAnchor[anchor.id], true);
          } else if (anchor && anchor.id === 'anchor_bed_blanket') {
            docX = anchor.xFraction; docY = anchor.yFraction; isLying = false; isSide = false;
            currentSpriteSrc = './assets/img_doc_interact.png';
            isHoldingAnchorPose = true;
            setSpeech(D.DOCTOR_QUOTES.wardDoctorDropLinesByAnchor[anchor.id], true);
          } else if (anchor) {
            docX = anchor.xFraction; docY = anchor.yFraction; isLying = false; isSide = false;
            currentSpriteSrc = './assets/img_doc_interact.png';
            isHoldingAnchorPose = true;
            setSpeech(D.DOCTOR_QUOTES.wardDoctorDropLinesByAnchor[anchor.id] || A.pickRandom(D.DOCTOR_QUOTES.wardDoctorRandomDropLines), true);
          } else {
            isLying = false; isSide = false;
            currentSpriteSrc = './assets/img_doc_idle_1.png';
            setSpeech(A.pickRandom(D.DOCTOR_QUOTES.wardDoctorRandomDropLines), false);
          }
          persistSnapshot();
          updateDoctorVisualOnly();

          if (isHoldingAnchorPose) {
            setTimeout(function () {
              isHoldingAnchorPose = false;
              isLying = false;
              if (!isDraggingDoc) {
                currentSpriteSrc = './assets/img_doc_idle_1.png';
                updateDoctorVisualOnly();
              }
            }, 6500);
          }
        }

        window.addEventListener('pointermove', onMove);
        window.addEventListener('pointerup', onUp);
        window.addEventListener('pointercancel', onUp);
      };
    }

    buildShell();

    // Patrol & sprite frame loop
    var idleTick = 0;
    var spriteTimer = setInterval(function () {
      if (isDraggingDoc || isHoldingAnchorPose) return;
      if (isTurning) {
        currentSpriteSrc = './assets/img_doc_turn.png';
        isSide = true;
      } else if (isWalking) {
        walkFrameIdx = (walkFrameIdx + 1) % walkSprites.length;
        currentSpriteSrc = walkSprites[walkFrameIdx];
        isSide = true;
      } else {
        idleTick += 1;
        currentSpriteSrc = (idleTick % 18 === 0) ? './assets/img_doc_idle_2.png' : './assets/img_doc_idle_1.png';
        isSide = false;
      }
      updateDoctorVisualOnly();
    }, 135);

    var walkTimer = null;
    var rafId = null;
    var stepNo = 0;

    function nextPatrol() {
      walkTimer = setTimeout(function () {
        if (isDraggingDoc || isHoldingAnchorPose || uiOpts.isEditingProps) {
          nextPatrol();
          return;
        }
        stepNo += 1;
        var spot = A.pickRandom(floorSpots);
        var tx = spot[0], ty = spot[1];
        var dx = tx - docX;
        var wantsRight = Math.abs(dx) > 0.015 ? dx > 0 : facingRight;

        function moveStep() {
          var sx = docX, sy = docY;
          var dist = Math.abs(tx - sx) + Math.abs(ty - sy);
          var dur = A.clamp(dist * 4200, 950, 2000);
          var t0 = performance.now();
          isWalking = true;
          function tick(now) {
            if (isDraggingDoc) { isWalking = false; return; }
            var p = Math.min(1, (now - t0) / dur);
            docX = sx + (tx - sx) * p;
            docY = sy + (ty - sy) * p;
            updateDoctorVisualOnly();
            if (p < 1) {
              rafId = requestAnimationFrame(tick);
            } else {
              isWalking = false;
              persistSnapshot();
              if (stepNo % 2 === 0) {
                setSpeech(A.pickRandom(D.DOCTOR_QUOTES.wardDoctorWanderLines), false);
              }
              updateDoctorVisualOnly();
              nextPatrol();
            }
          }
          rafId = requestAnimationFrame(tick);
        }

        if (wantsRight !== facingRight) {
          isTurning = true;
          updateDoctorVisualOnly();
          setTimeout(function () {
            facingRight = wantsRight;
            isTurning = false;
            moveStep();
          }, 220);
        } else {
          moveStep();
        }
      }, 3200);
    }
    nextPatrol();

    A.registerCleanup(function () {
      clearInterval(spriteTimer);
      if (walkTimer) clearTimeout(walkTimer);
      if (rafId) cancelAnimationFrame(rafId);
    });
  }

  /* ===================== SLIDABLE BATTERY BAR (CANVAS) ===================== */

  function mountBatteryCanvas(canvasEl, initialPercent, onChange) {
    var pct = A.clamp(Math.round(Number(initialPercent) || 50), 0, 100);

    function draw() {
      var dpr = window.devicePixelRatio || 1;
      var w = canvasEl.clientWidth || 300;
      var h = canvasEl.clientHeight || 62;
      canvasEl.width = w * dpr;
      canvasEl.height = h * dpr;
      var ctx = canvasEl.getContext('2d');
      ctx.scale(dpr, dpr);
      ctx.clearRect(0, 0, w, h);

      var capW = 10, capH = 22, gap = 4, padY = 4;
      var bodyW = w - capW - gap;
      var bodyH = h - padY * 2;
      var r = 16;

      // Track
      ctx.fillStyle = '#FFF1F4';
      ctx.beginPath();
      ctx.roundRect(0, padY, bodyW, bodyH, r);
      ctx.fill();

      // Fill color by level
      var fillColor = pct <= 20 ? '#D99559' : pct <= 50 ? 'rgba(234,135,151,0.8)' : '#EA8797';
      var inner = 5;
      var maxFill = Math.max(0, bodyW - inner * 2);
      var fillW = maxFill * (pct / 100);
      if (fillW > 2) {
        ctx.fillStyle = fillColor;
        ctx.beginPath();
        ctx.roundRect(inner, padY + inner, fillW, bodyH - inner * 2, r - 4);
        ctx.fill();
      }

      // Outline
      ctx.strokeStyle = 'rgba(67,46,43,0.65)';
      ctx.lineWidth = 2.5;
      ctx.beginPath();
      ctx.roundRect(0, padY, bodyW, bodyH, r);
      ctx.stroke();

      // Positive terminal nub
      ctx.fillStyle = 'rgba(67,46,43,0.65)';
      ctx.beginPath();
      ctx.roundRect(bodyW + gap, (h - capH) / 2, capW, capH, 5);
      ctx.fill();
    }

    function updateFromClientX(clientX) {
      var rect = canvasEl.getBoundingClientRect();
      var usable = Math.max(1, rect.width - 24);
      var ratio = A.clamp((clientX - rect.left - 8) / usable, 0, 1);
      pct = Math.round(ratio * 100);
      draw();
      onChange(pct);
    }

    var dragging = false;
    canvasEl.onpointerdown = function (ev) {
      ev.preventDefault();
      dragging = true;
      try { canvasEl.setPointerCapture(ev.pointerId); } catch (e) { /* ignore */ }
      updateFromClientX(ev.clientX);
    };
    canvasEl.onpointermove = function (ev) {
      if (dragging) updateFromClientX(ev.clientX);
    };
    canvasEl.onpointerup = function () { dragging = false; };
    canvasEl.onpointercancel = function () { dragging = false; };

    setTimeout(draw, 0);
  }

  /* ===================== CALENDAR EVENT MODAL ===================== */

  function openCalendarEventModal(dateKey, existingEv) {
    var isEdit = !!existingEv;
    var draft = {
      title: existingEv ? existingEv.title : '',
      note: existingEv ? existingEv.note : '',
      dateKey: existingEv ? existingEv.dateKey : dateKey,
      allDay: existingEv ? !!existingEv.allDay : false,
      startTime: existingEv ? existingEv.startTime : '09:00',
      endTime: existingEv ? existingEv.endTime : '10:00'
    };

    function renderEvDlg() {
      var timeInputs = !draft.allDay
        ? '<div style="display:flex;align-items:center;gap:8px">' +
            '<div style="flex:1"><div class="field-label">開始時間</div><input type="time" class="input-box" id="ev-start" value="' + A.esc(draft.startTime) + '"/></div>' +
            '<div style="flex:1"><div class="field-label">結束時間</div><input type="time" class="input-box" id="ev-end" value="' + A.esc(draft.endTime) + '"/></div>' +
          '</div>'
        : '';

      var html =
        '<div class="dialog-panel">' +
          '<div style="display:flex;align-items:center;justify-content:space-between">' +
            '<div style="font-size:18px;font-weight:700">' + (isEdit ? '編輯行事曆行程' : '新增行事曆行程') + '</div>' +
            '<button type="button" class="icon-btn" id="ev-close">' + ICONS.close + '</button>' +
          '</div>' +
          '<div style="font-size:12.5px;color:var(--primary);font-weight:700">日期：' + A.esc(draft.dateKey) + '</div>' +
          '<div class="field-group"><div class="field-label">活動／行程名稱</div><input type="text" class="input-box" id="ev-title" maxlength="50" value="' + A.esc(draft.title) + '" placeholder="例如：交報告、看醫生、讀書會"/></div>' +
          '<div style="display:flex;align-items:center;justify-content:space-between;padding:10px 12px;border-radius:14px;background:var(--surface-low)">' +
            '<span style="font-weight:700;font-size:14px">全天活動</span>' +
            '<button type="button" class="toggle-switch' + (draft.allDay ? ' on' : '') + '" id="ev-allday"></button>' +
          '</div>' +
          timeInputs +
          '<div class="field-group"><div class="field-label">備註（可留空）</div><input type="text" class="input-box" id="ev-note" maxlength="100" value="' + A.esc(draft.note) + '" placeholder="地點或小提醒"/></div>' +
          '<button type="button" class="btn btn-primary btn-full" id="ev-save">' + (isEdit ? '儲存變更' : '加入日曆') + '</button>' +
        '</div>';

      var root = A.openModal(html, { center: true });
      root.querySelector('#ev-close').onclick = A.closeModal;
      root.querySelector('#ev-allday').onclick = function () {
        draft.title = root.querySelector('#ev-title').value;
        draft.note = root.querySelector('#ev-note').value;
        draft.allDay = !draft.allDay;
        renderEvDlg();
      };
      var sEl = root.querySelector('#ev-start');
      var eEl = root.querySelector('#ev-end');
      if (sEl) {
        sEl.onchange = function () {
          draft.startTime = sEl.value;
          draft.endTime = A.addMinutesToTime(sEl.value, 60);
          if (eEl) eEl.value = draft.endTime;
        };
      }
      if (eEl) {
        eEl.onchange = function () { draft.endTime = eEl.value; };
      }
      root.querySelector('#ev-save').onclick = function () {
        var title = root.querySelector('#ev-title').value.trim();
        if (!title) {
          A.showToast('先填寫行程名稱再儲存吧');
          return;
        }
        var note = root.querySelector('#ev-note').value.trim();
        A.setState(function (s) {
          if (isEdit) {
            s.calendarEvents = s.calendarEvents.map(function (ev) {
              return ev.id === existingEv.id ? Object.assign({}, ev, draft, { title: title, note: note }) : ev;
            });
          } else {
            s.calendarEvents.push(Object.assign({}, draft, { id: A.uid('ev'), title: title, note: note }));
          }
        }, true);
        A.closeModal();
        A.showToast(isEdit ? '已更新行程' : '已加入日曆');
        A.rerender();
      };
    }
    renderEvDlg();
  }

  /* ===================== LOG SCREEN RENDER ===================== */

  var wardUiOpts = { showAnchors: false, isEditingProps: false, activePlacingItemId: null };

  function renderLogTab(container) {
    var st = A.state;
    var r = A.runtime;
    var todayKey = A.formatDateKey();
    var todayCheckin = st.checkins.filter(function (c) { return c.dateKey === todayKey; })[0] || null;
    var draftPct = todayCheckin ? todayCheckin.energyPercent : 50;
    var draftText = todayCheckin ? todayCheckin.reflection : '';

    var toggleHtml =
      '<div class="px-16">' +
        '<div class="segmented-row">' +
          '<button type="button" class="seg-btn' + (r.logMode === 'LIST' ? ' active' : '') + '" data-log-mode="LIST">' + ICONS.rx + '<span>列表</span></button>' +
          '<button type="button" class="seg-btn' + (r.logMode === 'CALENDAR' ? ' active' : '') + '" data-log-mode="CALENDAR">' + ICONS.log + '<span>日曆</span></button>' +
        '</div>' +
      '</div>';

    if (r.logMode === 'LIST') {
      var placedIds = st.wardPlacements.map(function (p) { return p.itemId; });
      var ownedItems = D.PHARMACY_ITEMS.filter(function (it) { return st.ownedItemIds.indexOf(it.id) >= 0; });

      var trayHtml = !ownedItems.length
        ? '<div style="display:flex;align-items:center;justify-content:space-between;gap:8px">' +
            '<span style="font-size:13px;color:var(--on-surface-variant)">還沒有病房擺飾，去藥局領些可愛裝備吧！</span>' +
            '<button type="button" class="btn btn-outline" id="btn-log-go-pharmacy" style="height:36px;font-size:12px">逛逛藥局</button>' +
          '</div>'
        : '<div class="ward-tray-scroll">' + ownedItems.map(function (it) {
            var isPlaced = placedIds.indexOf(it.id) >= 0;
            var isSel = wardUiOpts.activePlacingItemId === it.id;
            return '<button type="button" class="tray-chip' + (isPlaced ? ' placed' : '') + (isSel ? ' selected' : '') + '" data-tray-item="' + A.esc(it.id) + '">' +
              '<img src="' + A.esc(it.imgSrc) + '" alt=""/>' +
              '<span>' + A.esc(it.name) + '</span>' +
              (isPlaced ? '<span>✓</span>' : '') +
            '</button>';
          }).join('') + '</div>';

      var sortedCheckins = st.checkins.slice().sort(function (a, b) {
        return a.dateKey < b.dateKey ? 1 : -1;
      });
      var diaryCardsHtml = sortedCheckins.length
        ? sortedCheckins.map(function (entry) {
            var lvl = A.percentToEnergyLevel(entry.energyPercent);
            var shortSt = lvl.shortTag.indexOf(' ') >= 0 ? lvl.shortTag.split(' ')[1] : lvl.shortTag;
            var dayCompletedTasks = st.tasks.filter(function (t) { return t.completed && t.completedDate === entry.dateKey; }).map(function (t) { return t.title; });
            var seen = {};
            var allTitles = (entry.completedTaskTitles || []).concat(dayCompletedTasks).filter(function (x) {
              var tr = String(x || '').trim();
              if (!tr || seen[tr]) return false;
              seen[tr] = true;
              return true;
            });
            return '<div class="card" style="padding:18px;display:flex;flex-direction:column;gap:10px">' +
              '<div style="display:flex;align-items:center;justify-content:space-between;gap:8px">' +
                '<span style="font-size:16px;font-weight:700;color:var(--primary)">' + A.esc(entry.dateKey) + '</span>' +
                '<span class="pill pill-secondary">電量 ' + entry.energyPercent + '%・' + A.esc(shortSt) + '</span>' +
              '</div>' +
              (entry.reflection ? '<div class="card-soft" style="padding:10px 12px;border-radius:14px;font-size:14px">「' + A.esc(entry.reflection) + '」</div>' : '') +
              ((entry.wardPlacements && entry.wardPlacements.length) || entry.wardDoctorSnapshot
                ? '<div style="border-radius:16px;overflow:hidden;border:1px solid var(--outline-variant)">' + renderWardSnapshotHtml(entry.wardPlacements, entry.wardDoctorSnapshot, st.doctorGear) + '</div>'
                : '') +
              (allTitles.length ? '<div style="display:flex;flex-direction:column;gap:4px">' + allTitles.map(function (tt) {
                return '<div style="display:flex;align-items:center;gap:6px;font-size:13.5px"><span style="color:var(--primary)">' + ICONS.check + '</span><span>' + A.esc(tt) + '</span></div>';
              }).join('') + '</div>' : '') +
            '</div>';
          }).join('')
        : '<div class="card card-soft" style="padding:18px;font-size:13.5px;color:var(--on-surface-variant)">還沒有記錄，滑動上方電池或留幾行字就能存下今天囉。</div>';

      var initLvl = A.percentToEnergyLevel(draftPct);
      var initStatus = initLvl.shortTag.indexOf(' ') >= 0 ? initLvl.shortTag.split(' ')[1] : initLvl.shortTag;

      container.innerHTML =
        toggleHtml +
        '<div class="px-16" style="display:flex;flex-direction:column;gap:8px">' +
          '<div style="display:flex;align-items:center;justify-content:space-between;gap:8px">' +
            '<div>' +
              '<div style="font-weight:700;font-size:16px">療癒病房佈置</div>' +
              '<div style="font-size:12px;color:var(--on-surface-variant)">' + A.esc(st.feifeiNickname) + '巡房中・寫日記會同時記錄今天病房的長相</div>' +
            '</div>' +
            '<span class="pill pill-primary">已擺 ' + st.wardPlacements.length + ' 件</span>' +
          '</div>' +
          '<div class="chip-row">' +
            '<button type="button" class="filter-chip' + (wardUiOpts.showAnchors ? ' selected' : '') + '" id="btn-ward-anchors">' + ICONS.pin + ' ' + (wardUiOpts.showAnchors ? '隱藏錨點' : '顯示錨點') + '</button>' +
            '<button type="button" class="filter-chip' + (wardUiOpts.isEditingProps ? ' selected' : '') + '" id="btn-ward-editprops">' + ICONS.move + ' ' + (wardUiOpts.isEditingProps ? '完成編輯道具' : '編輯道具') + '</button>' +
          '</div>' +
        '</div>' +
        '<div id="ward-interactive-host"></div>' +
        '<div class="px-16">' +
          '<div class="card" style="padding:12px 14px">' + trayHtml + '</div>' +
        '</div>' +
        '<div class="px-16">' +
          '<div class="card" style="padding:20px;display:flex;flex-direction:column;gap:14px">' +
            '<div style="display:flex;align-items:center;justify-content:space-between">' +
              '<span style="font-weight:700;font-size:16px" id="bat-label-pct">今日電量・' + draftPct + '%</span>' +
              '<span class="pill pill-primary" id="bat-label-status">' + A.esc(initStatus) + '</span>' +
            '</div>' +
            '<div class="battery-track-box"><canvas class="battery-canvas" id="battery-canvas"></canvas></div>' +
            '<div style="font-size:12px;color:var(--on-surface-variant)">左右滑動電池調整今日電量</div>' +
            '<textarea class="textarea-box" id="diary-reflection" maxlength="300" placeholder="今天感覺如何？留幾句小筆記也沒問題...">' + A.esc(draftText) + '</textarea>' +
            '<button type="button" class="btn btn-primary btn-full" id="btn-save-diary">儲存</button>' +
          '</div>' +
        '</div>' +
        '<div class="px-16" style="display:flex;flex-direction:column;gap:12px">' +
          '<h2 class="section-title">日記列表</h2>' +
          diaryCardsHtml +
        '</div>';

      container.querySelectorAll('[data-log-mode]').forEach(function (b) {
        b.onclick = function () { r.logMode = b.getAttribute('data-log-mode'); A.rerender(); };
      });
      container.querySelector('#btn-ward-anchors').onclick = function () {
        wardUiOpts.showAnchors = !wardUiOpts.showAnchors;
        if (!wardUiOpts.showAnchors) wardUiOpts.activePlacingItemId = null;
        A.rerender();
      };
      container.querySelector('#btn-ward-editprops').onclick = function () {
        wardUiOpts.isEditingProps = !wardUiOpts.isEditingProps;
        A.rerender();
      };

      var goPharm = container.querySelector('#btn-log-go-pharmacy');
      if (goPharm) {
        goPharm.onclick = function () {
          r.activeTab = 'pharmacy';
          r.pharmacySubMode = 'supply';
          A.rerender();
        };
      }

      container.querySelectorAll('[data-tray-item]').forEach(function (btn) {
        btn.onclick = function () {
          var id = btn.getAttribute('data-tray-item');
          var isPlaced = st.wardPlacements.some(function (p) { return p.itemId === id; });
          if (isPlaced) {
            A.setState(function (s) {
              s.equippedItemIds = s.equippedItemIds.filter(function (x) { return x !== id; });
              s.wardPlacements = s.wardPlacements.filter(function (p) { return p.itemId !== id; });
            }, true);
            wardUiOpts.activePlacingItemId = null;
          } else {
            var pos = D.DEFAULT_WARD_POSITIONS[id] || { xFraction: 0.5, yFraction: 0.72 };
            A.setState(function (s) {
              if (s.equippedItemIds.indexOf(id) < 0) s.equippedItemIds.push(id);
              s.wardPlacements.push({ itemId: id, xFraction: pos.xFraction, yFraction: pos.yFraction });
            }, true);
            wardUiOpts.activePlacingItemId = id;
          }
          A.rerender();
        };
      });

      var wardHost = container.querySelector('#ward-interactive-host');
      mountInteractiveWardRoom(wardHost, wardUiOpts, {
        onMoveItem: function (itemId, fx, fy) {
          A.setState(function (s) {
            s.wardPlacements = s.wardPlacements.map(function (p) {
              return p.itemId === itemId ? { itemId: itemId, xFraction: fx, yFraction: fy } : p;
            });
          }, true);
          A.rerender();
        },
        onSelectAnchor: function (anchor) {
          var tid = wardUiOpts.activePlacingItemId || (ownedItems.filter(function (i) { return placedIds.indexOf(i.id) < 0; })[0] || {}).id;
          if (!tid) return;
          A.setState(function (s) {
            if (s.equippedItemIds.indexOf(tid) < 0) s.equippedItemIds.push(tid);
            var exists = s.wardPlacements.some(function (p) { return p.itemId === tid; });
            if (exists) {
              s.wardPlacements = s.wardPlacements.map(function (p) {
                return p.itemId === tid ? { itemId: tid, xFraction: anchor.xFraction, yFraction: anchor.yFraction } : p;
              });
            } else {
              s.wardPlacements.push({ itemId: tid, xFraction: anchor.xFraction, yFraction: anchor.yFraction });
            }
          }, true);
          wardUiOpts.activePlacingItemId = tid;
          A.rerender();
        }
      });

      var batCanvas = container.querySelector('#battery-canvas');
      mountBatteryCanvas(batCanvas, draftPct, function (newPct) {
        draftPct = newPct;
        var lvl = A.percentToEnergyLevel(newPct);
        var stTxt = lvl.shortTag.indexOf(' ') >= 0 ? lvl.shortTag.split(' ')[1] : lvl.shortTag;
        var lblP = container.querySelector('#bat-label-pct');
        var lblS = container.querySelector('#bat-label-status');
        if (lblP) lblP.textContent = '今日電量・' + newPct + '%';
        if (lblS) lblS.textContent = stTxt;
      });

      container.querySelector('#btn-save-diary').onclick = function () {
        var txt = container.querySelector('#diary-reflection').value.trim();
        var lvl = A.percentToEnergyLevel(draftPct);
        var existing = st.checkins.filter(function (c) { return c.dateKey === todayKey; })[0];
        var existingTitles = existing && Array.isArray(existing.completedTaskTitles) ? existing.completedTaskTitles : [];
        var liveTitles = st.tasks.filter(function (t) { return t.completed && t.completedDate === todayKey; }).map(function (t) { return t.title; });
        var seen = {};
        var mergedTitles = existingTitles.concat(liveTitles).filter(function (x) {
          var tr = String(x || '').trim();
          if (!tr || seen[tr]) return false;
          seen[tr] = true;
          return true;
        });

        A.setState(function (s) {
          var rest = s.checkins.filter(function (c) { return c.dateKey !== todayKey; });
          rest.unshift({
            id: todayKey,
            dateKey: todayKey,
            energyLevel: lvl.wireValue,
            energyPercent: draftPct,
            reflection: txt,
            completedTaskTitles: mergedTitles,
            wardPlacements: s.wardPlacements.slice(),
            wardDoctorSnapshot: s.wardDoctorSnapshot ? Object.assign({}, s.wardDoctorSnapshot) : null
          });
          s.checkins = rest;
        }, true);
        A.showToast('已儲存今日電量 ' + draftPct + '% 與病房日記！');
        A.rerender();
      };
    } else {
      // CALENDAR MODE
      var y = r.calYear;
      var m = r.calMonth;
      var selDk = r.calSelectedDate;
      var firstDow = new Date(y, m - 1, 1).getDay();
      var daysInMonth = new Date(y, m, 0).getDate();
      var weekdays = ['日', '一', '二', '三', '四', '五', '六'];

      var calCells = '';
      for (var b = 0; b < firstDow; b++) calCells += '<div></div>';
      for (var d = 1; d <= daysInMonth; d++) {
        var dk = y + '-' + A.pad2(m) + '-' + A.pad2(d);
        var hasRec = st.checkins.some(function (c) { return c.dateKey === dk; }) || st.tasks.some(function (t) { return t.completed && t.completedDate === dk; });
        var hasEv = st.calendarEvents.some(function (ev) { return ev.dateKey === dk; });
        var isSel = selDk === dk;
        calCells +=
          '<button type="button" class="cal-cell' + (isSel ? ' selected' : hasRec ? ' has-record' : '') + '" data-cal-day="' + dk + '">' +
            '<span>' + d + '</span>' +
            '<span class="cal-dots-row">' +
              (hasRec ? '<span class="cal-dot"></span>' : '') +
              (hasEv ? '<span class="cal-dot-event"></span>' : '') +
            '</span>' +
          '</button>';
      }

      var selEntry = st.checkins.filter(function (c) { return c.dateKey === selDk; })[0] || null;
      var selEvents = st.calendarEvents.filter(function (ev) { return ev.dateKey === selDk; });
      var selDoneTasks = st.tasks.filter(function (t) { return t.completed && t.completedDate === selDk; }).map(function (t) { return t.title; });
      var seenCal = {};
      var mergedCalTitles = (selEntry ? selEntry.completedTaskTitles || [] : []).concat(selDoneTasks).filter(function (x) {
        var tr = String(x || '').trim();
        if (!tr || seenCal[tr]) return false;
        seenCal[tr] = true;
        return true;
      });

      var evListHtml = selEvents.length
        ? selEvents.map(function (ev) {
            return '<div style="display:flex;align-items:center;justify-content:space-between;gap:8px;padding:10px 12px;border-radius:14px;background:var(--surface-low)">' +
              '<div style="flex:1;min-width:0">' +
                '<div style="font-weight:700;font-size:14px">' + A.esc(ev.title) + '</div>' +
                '<div style="font-size:12px;color:var(--on-surface-variant)">' + (ev.allDay ? '全天行程' : A.esc(ev.startTime) + ' - ' + A.esc(ev.endTime)) + (ev.note ? '・' + A.esc(ev.note) : '') + '</div>' +
              '</div>' +
              '<button type="button" class="icon-btn" data-edit-ev="' + A.esc(ev.id) + '" aria-label="編輯行程">' + ICONS.edit + '</button>' +
              '<button type="button" class="icon-btn" data-del-ev="' + A.esc(ev.id) + '" aria-label="刪除行程" style="color:var(--primary)">' + ICONS.trash + '</button>' +
            '</div>';
          }).join('')
        : '<div style="font-size:13px;color:var(--on-surface-variant)">這天還沒有安排行事曆行程</div>';

      var selLvl = selEntry ? A.percentToEnergyLevel(selEntry.energyPercent) : null;
      var selStatus = selLvl ? (selLvl.shortTag.indexOf(' ') >= 0 ? selLvl.shortTag.split(' ')[1] : selLvl.shortTag) : '';

      container.innerHTML =
        toggleHtml +
        '<div class="px-16" style="display:flex;flex-direction:column;gap:14px">' +
          '<div class="card" style="padding:16px;display:flex;flex-direction:column;gap:10px">' +
            '<div class="cal-header">' +
              '<button type="button" class="icon-btn" id="cal-prev">' + ICONS.chevLeft + '</button>' +
              '<span style="font-weight:700;font-size:17px">' + y + ' 年 ' + m + ' 月</span>' +
              '<button type="button" class="icon-btn" id="cal-next">' + ICONS.chevRight + '</button>' +
            '</div>' +
            '<div class="cal-weekdays">' + weekdays.map(function (w) { return '<div class="cal-weekday">' + w + '</div>'; }).join('') + '</div>' +
            '<div class="cal-grid">' + calCells + '</div>' +
          '</div>' +
          '<div class="card" style="padding:18px;display:flex;flex-direction:column;gap:12px">' +
            '<div style="display:flex;align-items:center;justify-content:space-between;gap:8px">' +
              '<span style="font-size:16px;font-weight:700;color:var(--primary)">' + A.esc(selDk) + '</span>' +
              '<button type="button" class="btn btn-primary" id="btn-add-cal-ev" style="height:36px;font-size:12.5px;padding:0 12px">' + ICONS.plus + ' 新增行程</button>' +
            '</div>' +
            '<div style="display:flex;flex-direction:column;gap:8px">' + evListHtml + '</div>' +
            (selEntry
              ? '<div style="border-top:1px solid var(--outline-variant);padding-top:12px;display:flex;flex-direction:column;gap:8px">' +
                  '<div style="display:flex;align-items:center;justify-content:space-between"><span style="font-weight:700;font-size:14px">當天電量與日記</span><span class="pill pill-secondary">電量 ' + selEntry.energyPercent + '%・' + A.esc(selStatus) + '</span></div>' +
                  (selEntry.reflection ? '<div class="card-soft" style="padding:10px 12px;border-radius:14px;font-size:13.5px">「' + A.esc(selEntry.reflection) + '」</div>' : '') +
                  ((selEntry.wardPlacements && selEntry.wardPlacements.length) || selEntry.wardDoctorSnapshot
                    ? '<div style="border-radius:16px;overflow:hidden;border:1px solid var(--outline-variant)">' + renderWardSnapshotHtml(selEntry.wardPlacements, selEntry.wardDoctorSnapshot, st.doctorGear) + '</div>'
                    : '') +
                '</div>'
              : '') +
            (mergedCalTitles.length
              ? '<div style="border-top:1px solid var(--outline-variant);padding-top:10px;display:flex;flex-direction:column;gap:5px">' +
                  '<div style="font-weight:700;font-size:13px;color:var(--on-surface-variant)">當天完成療程</div>' +
                  mergedCalTitles.map(function (tt) {
                    return '<div style="display:flex;align-items:center;gap:6px;font-size:13.5px"><span style="color:var(--primary)">' + ICONS.check + '</span><span>' + A.esc(tt) + '</span></div>';
                  }).join('') +
                '</div>'
              : '') +
          '</div>' +
        '</div>';

      container.querySelectorAll('[data-log-mode]').forEach(function (b) {
        b.onclick = function () { r.logMode = b.getAttribute('data-log-mode'); A.rerender(); };
      });
      container.querySelector('#cal-prev').onclick = function () {
        if (r.calMonth === 1) { r.calYear -= 1; r.calMonth = 12; } else r.calMonth -= 1;
        A.rerender();
      };
      container.querySelector('#cal-next').onclick = function () {
        if (r.calMonth === 12) { r.calYear += 1; r.calMonth = 1; } else r.calMonth += 1;
        A.rerender();
      };
      container.querySelectorAll('[data-cal-day]').forEach(function (b) {
        b.onclick = function () { r.calSelectedDate = b.getAttribute('data-cal-day'); A.rerender(); };
      });
      container.querySelector('#btn-add-cal-ev').onclick = function () {
        openCalendarEventModal(selDk, null);
      };
      container.querySelectorAll('[data-edit-ev]').forEach(function (b) {
        b.onclick = function () {
          var ev = st.calendarEvents.filter(function (x) { return x.id === b.getAttribute('data-edit-ev'); })[0];
          if (ev) openCalendarEventModal(selDk, ev);
        };
      });
      container.querySelectorAll('[data-del-ev]').forEach(function (b) {
        b.onclick = function () {
          var id = b.getAttribute('data-del-ev');
          A.setState(function (s) {
            s.calendarEvents = s.calendarEvents.filter(function (x) { return x.id !== id; });
          }, true);
          A.rerender();
        };
      });
    }
  }

  A.renderWardSnapshotHtml = renderWardSnapshotHtml;
  A.renderLogTab = renderLogTab;
})();
