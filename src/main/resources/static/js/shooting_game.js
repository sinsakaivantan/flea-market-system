document.addEventListener('DOMContentLoaded', function () {

    // --- 設定エリア ---
    const PLAYER_SRC = '/assets/gemini-color.png';
    const BOSS_SRC = '/assets/boss.png'; 
    const BGM_SRC = '/assets/bgm.mp3';

    const ENEMY_SOURCES = [
        '/assets/claude-color.png',
        '/assets/new-ChatGPT-icon-white-png-large-size.png',
        '/assets/deepseek-color.png',
        '/assets/copilot-color.png',
        '/assets/grok-dark.webp'
    ];

    const canvas = document.getElementById('gameCanvas');
    if (!canvas) {
        console.error("Canvas element not found");
        return;
    }
    const ctx = canvas.getContext('2d');
    canvas.width = 800; canvas.height = 800;

    // UI要素の取得
    const messageScreen = document.getElementById('message-screen');
    const rankingBoard = document.getElementById('ranking-board');
    const rankingList = document.getElementById('ranking-list');
    const bestScoreEl = document.getElementById('best-score');
    const ultStatusEl = document.getElementById('ult-status');
    const hpFill = document.getElementById('hp-fill');
    const hpText = document.getElementById('hp-text');
    const loadoutInfo = document.getElementById('loadout-info');
    const timerDisplay = document.getElementById('timer-display');
    const resultTitle = document.getElementById('result-title');
    const scoreEl = document.getElementById('score');

    // CSRF対策
    const csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
    const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
    const csrfToken = csrfTokenMeta ? csrfTokenMeta.content : "";
    const csrfHeader = csrfHeaderMeta ? csrfHeaderMeta.content : "";

    // ゲーム状態変数
    let score = 0;
    let gameOver = false;
    let gameStarted = false;
    let myBestScore = 0;
    let killCountForUlt = 0;
    let startTime = 0;
    let elapsedTime = 0;
    let isRageMode = false;

    // ★追加: 時間管理用
    let lastFrameTime = 0;

    // タブ切り替え対策
    let totalPausedTime = 0;
    let tempPauseStart = 0;

    const BOSS_SPAWN_TIME = 30; 
    const GAME_LIMIT_TIME = 180; 

    // プレイヤー初期設定
    let playerLoadout = {
        fireRate: 5.0, bulletCount: 1, bulletSize: 1.0, firePattern: "SPREAD",
        bonusHp: 0, damageReduction: 0.0,
        rotationSpeed: 0.05, 
        speedForward: 3.0, 
        speedBackward: 2.0,
        ultType: "NONE", ultChargeReq: 10, ultPower: 0.0
    };

    const keys = { ArrowUp:false, ArrowDown:false, ArrowLeft:false, ArrowRight:false, w:false, s:false, a:false, d:false, ' ':false, Enter:false };

    // --- 音声管理 ---
    const bgm = new Audio(BGM_SRC);
    bgm.loop = true; 
    bgm.volume = 0.5; 

    function playBgm() {
        bgm.play().catch(error => {
            console.log("Audio play blocked until user interaction:", error);
        });
    }

    function stopBgm() {
        bgm.pause();
        bgm.currentTime = 0;
    }

    // --- Visibility API ---
    document.addEventListener("visibilitychange", () => {
        if (!gameStarted || gameOver) return;
        if (document.hidden) {
            tempPauseStart = Date.now();
            bgm.pause(); 
        } else {
            if (tempPauseStart > 0) {
                const pauseDuration = Date.now() - tempPauseStart;
                totalPausedTime += pauseDuration;
                tempPauseStart = 0;
                // ★追加: 復帰時に前フレーム時間をリセット
                lastFrameTime = Date.now();
            }
            if(!gameOver) bgm.play().catch(e=>{}); 
        }
    });

    // --- API通信: データ取得 ---
    async function initGameData() {
        try {
            const bestRes = await fetch('/api/shooting/my-best?t=' + Date.now());
            if (bestRes.ok) {
                myBestScore = await bestRes.json();
                if(bestScoreEl) bestScoreEl.innerText = myBestScore;
            }
            
            const loadoutRes = await fetch('/api/shooting/loadout?t=' + Date.now());
            if (loadoutRes.ok) {
                const data = await loadoutRes.json();
                if (data) playerLoadout = data;
            }
            
            if (!playerLoadout.ultType) playerLoadout.ultType = "NONE";



            updateLoadoutUI();
        } catch (e) { 
            console.error("Failed to load game data", e); 
        } finally { 
            loadImages();
        }
    }

    function updateLoadoutUI() {
        if(loadoutInfo) {
            loadoutInfo.innerHTML = `Skill: ${playerLoadout.ultType} | HP+${playerLoadout.bonusHp} | Cut: ${(playerLoadout.damageReduction*100).toFixed(0)}%<br>Spd: F${playerLoadout.speedForward}/B${playerLoadout.speedBackward} | Wpn: ${playerLoadout.bulletCount}way`;
        }
    }

    async function sendScore(finalScore) {
        try {
            await fetch('/api/shooting/score', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', [csrfHeader]: csrfToken },
                body: JSON.stringify({ score: finalScore })
            });
            showRanking();
        } catch (e) {}
    }

    async function showRanking() {
        try {
            const res = await fetch('/api/shooting/ranking');
            if (res.ok) {
                const data = await res.json();
                if (!gameOver) return; 
                rankingList.innerHTML = '';
                if (data.length === 0) rankingList.innerHTML = '<p>No Records</p>';
                data.forEach((record, index) => {
                    const div = document.createElement('div');
                    div.className = 'rank-item';
                    div.innerHTML = `<span>#${index + 1} ${escapeHtml(record.username)}</span><span>${record.score}</span>`;
                    rankingList.appendChild(div);
                });
                rankingBoard.style.display = 'block';
            }
        } catch (e) {}
    }

    const playerImage = new Image();
    const bossImage = new Image();
    const enemyImages = [];
    let imagesLoadedCount = 0;
    const totalImagesToLoad = 2 + ENEMY_SOURCES.length; 

    function loadImages() {
        const timeout = setTimeout(() => {
            if (!gameStarted) {
                if(messageScreen) messageScreen.innerHTML = '';
                startGame();
            }
        }, 3000);

        const checkStart = () => {
            imagesLoadedCount++;
            if (imagesLoadedCount >= totalImagesToLoad) {
                clearTimeout(timeout);
                if(messageScreen) messageScreen.innerHTML = '';
                startGame();
            }
        };

        playerImage.src = PLAYER_SRC;
        playerImage.onload = checkStart;
        playerImage.onerror = checkStart; 

        bossImage.src = BOSS_SRC;
        bossImage.onload = checkStart;
        bossImage.onerror = checkStart;

        ENEMY_SOURCES.forEach(src => {
            const img = new Image();
            img.src = src;
            img.onload = () => { enemyImages.push(img); checkStart(); };
            img.onerror = checkStart; 
        });
    }

    // --- Player Class ---
    class Player {
        constructor() {
            this.width = 50; 
            this.height = 50;
            this.x = 400; 
            this.y = 400; 
            this.angle = -Math.PI / 2; 
            this.image = playerImage;
            
            this.speedFwd = playerLoadout.speedForward;
            this.speedBwd = playerLoadout.speedBackward;
            this.rotSpeed = playerLoadout.rotationSpeed;
            this.fireInterval = 1000 / playerLoadout.fireRate;
            
            this.maxHp = 100 + playerLoadout.bonusHp;
            this.hp = this.maxHp;
            this.dmgCut = playerLoadout.damageReduction;

            this.lastShotTime = 0; 
            this.lastDmgTime = 0; 
            this.invincibleUntil = 0;
            this.attribute = "NONE"; 
        }
        
        takeDamage(amount, bypassInvincible = false) {
            if (!bypassInvincible && Date.now() < this.invincibleUntil) return;
            
            const actualDmg = Math.max(1, Math.floor(amount * (1.0 - this.dmgCut)));
            this.hp -= actualDmg;
            updateHpUI(this.hp, this.maxHp);
            
            this.invincibleUntil = Date.now() + 500;
            
            ctx.save(); ctx.globalCompositeOperation = 'lighter';
            ctx.fillStyle = 'rgba(255,0,0,0.5)';
            ctx.beginPath(); ctx.arc(this.x, this.y, 40, 0, Math.PI*2); ctx.fill(); ctx.restore();

            if (this.hp <= 0) handleGameOver(false);
        }

        draw() { 
            ctx.save(); ctx.translate(this.x, this.y);
            if (this.attribute === "LIGHT") {
                ctx.beginPath(); ctx.arc(0, 0, 40, 0, Math.PI*2);
                ctx.fillStyle = "rgba(255, 255, 255, 0.3)"; ctx.fill();
                ctx.strokeStyle = "#fff"; ctx.lineWidth = 2; ctx.stroke();
            } else if (this.attribute === "DARK") {
                ctx.beginPath(); ctx.arc(0, 0, 40, 0, Math.PI*2);
                ctx.fillStyle = "rgba(100, 0, 200, 0.3)"; ctx.fill();
                ctx.strokeStyle = "#a0f"; ctx.lineWidth = 2; ctx.stroke();
            }
            ctx.rotate(this.angle + Math.PI / 2);
            if (Date.now() < this.invincibleUntil && Math.floor(Date.now() / 50) % 2 === 0) {
                ctx.globalAlpha = 0.5;
            }
            drawSafeImage(ctx, this.image, -this.width/2, -this.height/2, this.width, this.height, '#00ffcc');
            ctx.restore();
        }
        
        // ★修正: timeScaleを受け取って移動・回転に適用
        update(timeScale) {
            if (keys.ArrowLeft || keys.a) this.angle -= this.rotSpeed * timeScale;
            if (keys.ArrowRight || keys.d) this.angle += this.rotSpeed * timeScale;
            if (keys.ArrowUp || keys.w) {
                this.x += Math.cos(this.angle) * this.speedFwd * timeScale;
                this.y += Math.sin(this.angle) * this.speedFwd * timeScale;
            }
            if (keys.ArrowDown || keys.s) {
                this.x -= Math.cos(this.angle) * this.speedBwd * timeScale;
                this.y -= Math.sin(this.angle) * this.speedBwd * timeScale;
            }
            this.x = Math.max(this.width/2, Math.min(canvas.width - this.width/2, this.x));
            this.y = Math.max(this.height/2, Math.min(canvas.height - this.height/2, this.y));

            if (Date.now() - this.lastShotTime > this.fireInterval) {
                this.fire();
                this.lastShotTime = Date.now();
            }
        }

        fire() {
            const count = playerLoadout.bulletCount;
            const size = playerLoadout.bulletSize;
            const pattern = playerLoadout.firePattern || "SPREAD";

            if (pattern === "FRONT_BACK") {
                for (let i = 0; i < count; i++) {
                    const offset = (i % 2 === 0) ? 0 : Math.PI;
                    bullets.push(new Bullet(this.x, this.y, this.angle + offset, size));
                }
            } else {
                const spreadAngle = 0.2; 
                for (let i = 0; i < count; i++) {
                    let offset = 0;
                    if (count > 1) {
                        offset = -spreadAngle/2 + (spreadAngle / (count-1)) * i;
                    }
                    bullets.push(new Bullet(this.x, this.y, this.angle + offset, size));
                }
            }
        }
    }

    class Enemy {
        constructor(x, y, speed, img) {
            this.x = x; 
            this.y = y; 
            this.width = 45; 
            this.height = 45;
            this.image = img; 
            this.speed = speed;
            this.angle = 0; 
            this.shootInterval = Math.random() * 150 + 100; 
            this.timer = 0;
            this.lastAttackTime = 0;
        }
        draw() { 
            ctx.save(); 
            ctx.translate(this.x, this.y); 
            ctx.rotate(this.angle + Math.PI / 2); 
            drawSafeImage(ctx, this.image, -22, -22, 45, 45, '#ff0000');
            ctx.restore(); 
        }
        // ★修正: timeScaleを受け取って移動・発射タイマーに適用
        update(timeScale) { 
            this.angle = Math.atan2(player.y - this.y, player.x - this.x);
            
            let currentSpeed = this.speed;
            if (isRageMode) currentSpeed *= 2; 

            this.x += Math.cos(this.angle) * currentSpeed * timeScale;
            this.y += Math.sin(this.angle) * currentSpeed * timeScale;
            
            this.timer += timeScale; // 修正
            if (this.timer > this.shootInterval) {
                enemyBullets.push(new EnemyBullet(this.x, this.y, this.angle));
                this.timer = 0;
            }
        }
    }

    class Bullet {
        constructor(x, y, angle, sizeMult) {
            this.x = x; 
            this.y = y; 
            this.radius = 4 * sizeMult;
            this.color = '#00ffcc'; 
            this.speed = 12;
            this.angle = angle;
            this.x += Math.cos(angle) * 30;
            this.y += Math.sin(angle) * 30;
        }
        draw() {
            ctx.beginPath(); 
            ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2);
            ctx.fillStyle = this.color; 
            ctx.fill();
            ctx.shadowBlur = 10; ctx.shadowColor = this.color; ctx.fill(); ctx.shadowBlur = 0;
        }
        // ★修正: timeScale適用
        update(timeScale) { 
            this.x += Math.cos(this.angle) * this.speed * timeScale;
            this.y += Math.sin(this.angle) * this.speed * timeScale;
        }
    }

    class EnemyBullet {
        constructor(x, y, angle) {
            this.x = x; 
            this.y = y; 
            this.radius = 5; 
            this.color = '#ff4444'; 
            this.speed = 5;
            this.angle = angle;
        }
        draw() {
            ctx.beginPath(); 
            ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2);
            ctx.fillStyle = this.color; 
            ctx.fill();
            ctx.shadowBlur = 5; ctx.shadowColor = this.color; ctx.fill(); ctx.shadowBlur = 0;
        }
        // ★修正: timeScale適用
        update(timeScale) { 
            this.x += Math.cos(this.angle) * this.speed * timeScale;
            this.y += Math.sin(this.angle) * this.speed * timeScale;
        }
    }

    class Particle {
        constructor(x, y, color) {
            this.x = x; 
            this.y = y; 
            this.radius = Math.random() * 3;
            this.color = color;
            this.velocity = { x: (Math.random() - 0.5) * 8, y: (Math.random() - 0.5) * 8 };
            this.alpha = 1;
        }
        draw() {
            ctx.save(); 
            ctx.globalAlpha = this.alpha; 
            ctx.beginPath();
            ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2);
            ctx.fillStyle = this.color; 
            ctx.fill(); 
            ctx.restore();
        }
        // ★修正: timeScale適用
        update(timeScale) { 
            this.x += this.velocity.x * timeScale; 
            this.y += this.velocity.y * timeScale; 
            this.alpha -= 0.04 * timeScale; 
        }
    }

    // --- MAIN LOOP ---
    let player, boss, bullets=[], enemies=[], enemyBullets=[], particles=[];

    function init() {
        if(rankingBoard) rankingBoard.style.display = 'none';
        if(scoreEl) scoreEl.innerText = "0"; 
        
        player = new Player(); 
        if (typeof Boss !== 'undefined') {
            boss = new Boss(bossImage);
        } else {
            console.error("Boss class missing.");
        }

        bullets=[]; enemies=[]; enemyBullets=[]; particles=[];
        score=0; gameOver=false; killCountForUlt=0; startTime=Date.now();
        isRageMode = false; 
        totalPausedTime = 0; tempPauseStart = 0;
        
        // ★追加: 時間初期化
        lastFrameTime = Date.now();
        
        updateUltUI(); updateHpUI(player.hp, player.maxHp);
    }

    function updateHpUI(c,m) { 
        if(hpFill) hpFill.style.width = Math.max(0,(c/m)*100)+'%'; 
        if(hpText) hpText.innerText = `${c}/${m}`; 
    }
    function updateUltUI() {
        if(!ultStatusEl) return;
        if(playerLoadout.ultType==="NONE"){ultStatusEl.innerText="ULT: NONE";return;}
        if(killCountForUlt>=playerLoadout.ultChargeReq){ultStatusEl.innerText=`ULT READY`;ultStatusEl.className="ult-ready";}
        else{ultStatusEl.innerText=`ULT: ${killCountForUlt}/${playerLoadout.ultChargeReq}`;ultStatusEl.className="";}
    }

    function activateUltimate(){
        if(!playerLoadout || !playerLoadout.ultType || playerLoadout.ultType === "NONE") return;
        if(killCountForUlt < playerLoadout.ultChargeReq) return;
        
        const t=playerLoadout.ultType, p=playerLoadout.ultPower;
        ctx.fillStyle='rgba(255,255,255,0.8)';ctx.fillRect(0,0,canvas.width,canvas.height);
        
        if(t==="WIPE"){enemies.forEach(e=>createExplosion(e.x, e.y, '#ff0055'));enemies=[];enemyBullets=[];}
        else if(t==="INVINCIBLE")player.invincibleUntil=Date.now()+(p*1000);
        else if(t==="HEAL")player.hp=Math.min(player.maxHp,player.hp+Math.floor(p));
        
        killCountForUlt=0;
        updateUltUI();
        updateHpUI(player.hp,player.maxHp);
        if(scoreEl) scoreEl.innerText = score;
    }

    document.addEventListener('keydown',e=>{
        keys[e.key]=true;
        
        if (bgm.paused && gameStarted && !gameOver) {
            playBgm();
        }
		
        if (e.key === 'q' || e.key === 'Q') {
            if (gameStarted && !gameOver) {
                if (confirm("装備画面に戻りますか？\n（現在のスコアは破棄されます）")) {
                    window.location.href = '/my-page/loadout';
                }
            } else {
                window.location.href = '/my-page/loadout';
            }
        }
				
        if(e.key===' '&&gameStarted&&!gameOver)activateUltimate();
        if(e.key==='Enter'&&gameOver){
            init();
            playBgm(); 
            animate();
            spawnEnemy();
        }
    });
    document.addEventListener('keyup',e=>keys[e.key]=false);

    function spawnEnemy(){
      if(gameOver||!gameStarted)return;
      let ex,ey;
      if(Math.random()<0.5){ex=Math.random()*800;ey=Math.random()<0.5?-50:850;}else{ex=Math.random()<0.5?-50:850;ey=Math.random()*800;}
      
      let randomImg = null;
      if (enemyImages.length > 0) randomImg = enemyImages[Math.floor(Math.random() * enemyImages.length)];
      
      enemies.push(new Enemy(ex,ey,Math.random()*1.5+1, randomImg));
      
      let delay = score > 30 ? 600 : score > 10 ? 800 : 1000;
      if(isRageMode) delay /= 2;
      
      setTimeout(spawnEnemy, delay);
    }

    function createExplosion(x, y, color = '#ffaa00') {
        for(let i = 0; i < 15; i++) particles.push(new Particle(x, y, color));
    }

    function checkCollision(o1,r1,o2,r2){return Math.sqrt((o1.x-o2.x)**2+(o1.y-o2.y)**2)<r1+r2;}

    function animate() {
      if(gameOver)return;
      requestAnimationFrame(animate);

      // ★追加: 経過時間(dt)から倍率(timeScale)を計算
      const now = Date.now();
      let dt = (now - lastFrameTime) / 1000;
      if (dt > 0.1) dt = 0.1; // 極端な飛び防止
      lastFrameTime = now;
      
      // 60FPS(約0.016s)なら1.0、30FPSなら2.0
      const timeScale = dt * 60;

      ctx.fillStyle='rgba(0,0,0,0.5)';ctx.fillRect(0,0,800,800);
      
      elapsedTime = (Date.now() - startTime - totalPausedTime) / 1000;
      if(timerDisplay) timerDisplay.innerText=`${String(Math.floor((180-elapsedTime)/60)).padStart(2,'0')}:${String(Math.floor((180-elapsedTime)%60)).padStart(2,'0')}`;
      
      if(elapsedTime>=180)handleGameOver(true);

      if(boss) {
          boss.update(player, elapsedTime, BOSS_SPAWN_TIME);
          boss.draw(ctx, elapsedTime);
          if(boss.rageModeTriggered) isRageMode = true;
      }

      // ★修正: timeScale を渡す
      player.update(timeScale); 
      player.draw();

      particles.forEach((p,i)=>{if(p.a<=0)particles.splice(i,1);else{p.update(timeScale);p.draw();}});
      
      bullets.forEach((b,i)=>{
        b.update(timeScale); b.draw();
        if(b.x<0||b.x>800||b.y<0||b.y>800){bullets.splice(i,1);return;}
        enemies.forEach((e,j)=>{
          if(checkCollision(e,e.width/2, b,b.radius)){ 
            createExplosion(e.x, e.y);
            enemies.splice(j,1);bullets.splice(i,1);
            
            let points = 1;
            if(isRageMode) points = 5; 
            score += points;
            if(scoreEl) scoreEl.innerText = score; 
            if (score > myBestScore) {
                myBestScore = score;
                if(bestScoreEl) bestScoreEl.innerText = myBestScore;
            }
            
            if(playerLoadout.ultType !== "NONE" && killCountForUlt < playerLoadout.ultChargeReq){
                killCountForUlt++;
                updateUltUI();
            }
          }
        });
        if(boss && boss.active&&checkCollision(boss,boss.width/2, b,b.radius)){createExplosion(b.x,b.y,'#aaa');bullets.splice(i,1);}
      });
      
      enemyBullets.forEach((b,i)=>{
          b.update(timeScale); b.draw();
          if(checkCollision(player,15, b,5)){
              let dmg = 1;
              if(isRageMode) dmg = 5; 
              player.takeDamage(dmg);
              enemyBullets.splice(i,1);
          }
      });
      
      enemies.forEach(e=>{
          e.update(timeScale); e.draw();
          if(checkCollision(player,20, e,20)) {
              if(Date.now() - e.lastAttackTime > 500) {
                  let dmg = isRageMode ? 10 : 2; 
                  player.takeDamage(dmg, true);
                  e.lastAttackTime = Date.now();
              }
          }
      });
    }

    function handleGameOver(win){
        if (!gameOver) {
            gameOver=true;
            stopBgm();

            ctx.fillStyle='rgba(0,0,0,0.8)';ctx.fillRect(0,0,800,800);
            if(resultTitle) {
                resultTitle.innerText=win?"CLEAR":"GAME OVER";
                resultTitle.style.color=win?"#0fc":"#f44";
            }
            if(rankingBoard) rankingBoard.style.display='block';
            sendScore(score);
        }
    }
    function startGame(){
        gameStarted=true;
        init();
        playBgm(); 
        spawnEnemy();
        animate();
    }

    initGameData();

});

/**
 * 画像が存在すれば描画し、なければ指定色の矩形を描画する関数
 */
function drawSafeImage(ctx, img, x, y, w, h, fallbackColor) {
    if (img && img.complete && img.naturalWidth !== 0) {
        ctx.drawImage(img, x, y, w, h);
    } else {
        ctx.fillStyle = fallbackColor;
        ctx.fillRect(x, y, w, h);
    }
}

/**
 * 円同士の当たり判定を行う関数
 */
function checkCollision(obj1, r1, obj2, r2) {
    const dx = obj1.x - obj2.x;
    const dy = obj1.y - obj2.y;
    return Math.sqrt(dx * dx + dy * dy) < r1 + r2;
}

/**
 * HTMLエスケープ処理
 */
function escapeHtml(str) {
    if(!str) return 'No Name';
    return str.replace(/[&<>"']/g, m => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }[m]));
}