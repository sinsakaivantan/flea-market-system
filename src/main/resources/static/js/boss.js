class Boss {
    constructor(bossImage) {
        this.active = false; 
        this.spawned = false; 
        this.retreating = false;
        this.rageModeTriggered = false; 

        this.width = 100; 
        this.height = 100;
        this.x = 400; 
        this.y = -100;
        this.image = bossImage;
        
        this.state = "IDLE"; 
        
        // 時間管理用
        this.stateStartTime = 0; 
        this.currentTime = 0;
        
        this.nextGimmickTime = 0;
        this.hasHitCurrentState = false;
        this.storedAttr = {}; 

        this.p1_bottom = "RED"; 
        this.p1_right = "GREEN"; 
        this.p2_right = "ORANGE";
        this.beam1_top = "LIGHT";
        this.beam2_top = "LIGHT";
    }

    update(player, elapsedTime, spawnTime) {
        this.currentTime = elapsedTime;

        // ボス出現管理
        if (!this.spawned && elapsedTime >= spawnTime) {
            this.spawned = true; 
            this.active = true;
            this.x = 400; 
            this.y = 100; 
            
            this.stateStartTime = elapsedTime;
            this.nextGimmickTime = 2.0; 
        }

        if (this.retreating) {
            this.y -= 3;
            if (this.y < -200) { 
                this.active = false; 
                this.rageModeTriggered = true; 
            } 
            return;
        }

        if (!this.active) return;
        
        if (checkCollision(this, this.width/2, player, player.width/2)) {
            if (Date.now() - player.lastDmgTime > 500) { 
                player.takeDamage(10); 
                player.lastDmgTime = Date.now(); 
            }
        }

        this.handleGimmick(player);
    }

    handleGimmick(player) {
        const dt = this.currentTime - this.stateStartTime;

        if (this.state === "IDLE") {
            if (dt >= this.nextGimmickTime) {
                this.startGimmickSetup(player);
            }
        }
        
        // --- Phase 1 ---
        else if (this.state === "P1_WARN") {
            if (dt >= 5.0) { // 予兆5秒
                this.changeState("P1_FIRE1");
                this.storeBeam(this.beam1_top, this.p1_bottom);
                this.storeBeam(this.beam1_top, this.p1_right);
            }
        }
        else if (this.state === "P1_FIRE1") {
            this.fireBeam_Vertical(this.beam1_top, player);
            if (dt >= 0.2) { // 攻撃0.2秒
                this.changeState("P1_FIRE2");
            }
        }
        else if (this.state === "P1_FIRE2") {
            this.fireBeam_HorizontalFromLeft(this.p1_bottom, player);
            if (dt >= 0.2) { // 攻撃0.2秒
                this.changeState("WAIT_2");
            }
        }
        else if (this.state === "WAIT_2") {
            if (dt >= 10.0) { // 待機10秒
                this.changeState("P2_WARN");
            }
        }

        // --- Phase 2 ---
        else if (this.state === "P2_WARN") {
            if (dt >= 5.0) {
                this.changeState("P2_FIRE1");
                this.storeBeam(this.beam2_top, this.p1_bottom); 
            }
        }
        else if (this.state === "P2_FIRE1") {
            this.fireBeam_Vertical(this.beam2_top, player);
            if (dt >= 0.2) { 
                this.changeState("P2_FIRE2");
                this.storeBeamHorizontal(this.p1_right, this.p2_right);
            }
        }
        else if (this.state === "P2_FIRE2") {
            this.fireBeam_HorizontalFromLeft(this.p1_right, player);
            if (dt >= 0.2) { 
                this.changeState("WAIT_3");
            }
        }
        else if (this.state === "WAIT_3") {
            if (dt >= 10.0) { 
                this.changeState("P3_WARN");
            }
        }

        // --- Phase 3 ---
        else if (this.state === "P3_WARN") {
            if (dt >= 5.0) {
                this.changeState("P3_FIRE1");
            }
        }
        else if (this.state === "P3_FIRE1") {
            this.fireBeam_VerticalFromTop(this.p1_bottom, player);
            if (dt >= 0.2) {
                this.changeState("P3_FIRE2");
            }
        }
        else if (this.state === "P3_FIRE2") {
            this.fireBeam_HorizontalFromLeft(this.p2_right, player);
            if (dt >= 0.2) { 
                this.finishGimmick(player);
            }
        }
    }

    changeState(newState) {
        this.state = newState;
        this.stateStartTime = this.currentTime;
        this.hasHitCurrentState = false;
    }

    startGimmickSetup(player) {
        this.changeState("P1_WARN");
        this.storedAttr = {};
        
        player.attribute = Math.random() < 0.5 ? "LIGHT" : "DARK";
        
        this.beam1_top = Math.random() < 0.5 ? "LIGHT" : "DARK";
        this.p1_bottom = Math.random() < 0.5 ? "RED" : "BLUE"; 
        this.p1_right = Math.random() < 0.5 ? "GREEN" : "YELLOW";
        
        this.beam2_top = Math.random() < 0.5 ? "LIGHT" : "DARK"; 
        this.p2_right = Math.random() < 0.5 ? "ORANGE" : "PINK";
    }

    getOppositeColor(c) {
        if(c==="RED") return "BLUE"; if(c==="BLUE") return "RED";
        if(c==="GREEN") return "YELLOW"; if(c==="YELLOW") return "GREEN";
        if(c==="ORANGE") return "PINK"; if(c==="PINK") return "ORANGE";
        return c;
    }

    storeBeam(beamAttrTopLeft, portalColorLeft) {
        const portalColorRight = this.getOppositeColor(portalColorLeft);
        const beamAttrLeft = beamAttrTopLeft;
        const beamAttrRight = (beamAttrTopLeft === "LIGHT" ? "DARK" : "LIGHT");
        this.storedAttr[portalColorLeft] = beamAttrLeft;
        this.storedAttr[portalColorRight] = beamAttrRight;
    }

    storeBeamHorizontal(sourcePortalColorLeft, targetPortalColorRight) {
        const sourcePortalColorBottom = this.getOppositeColor(sourcePortalColorLeft); 
        const targetPortalColorBottom = this.getOppositeColor(targetPortalColorRight); 
        const beamTop = this.storedAttr[sourcePortalColorLeft];
        this.storedAttr[targetPortalColorRight] = beamTop;
        const beamBottom = this.storedAttr[sourcePortalColorBottom];
        this.storedAttr[targetPortalColorBottom] = beamBottom;
    }

    hitCheck(beamAttr, player) {
        if (this.hasHitCurrentState) return;
        if (player.attribute === beamAttr) {
            player.takeDamage(50, true); 
        }
        player.attribute = beamAttr; 
        this.hasHitCurrentState = true;
    }

    fireBeam_Vertical(beamAttrLeft, player) {
        const isLeft = player.x < 800 / 2;
        const beamAttr = isLeft ? beamAttrLeft : (beamAttrLeft === "LIGHT" ? "DARK" : "LIGHT");
        this.hitCheck(beamAttr, player);
    }

    fireBeam_VerticalFromTop(portalColorLeft, player) {
        const portalColorRight = this.getOppositeColor(portalColorLeft);
        const beamAttrLeft = this.storedAttr[portalColorLeft];
        const beamAttrRight = this.storedAttr[portalColorRight];
        const isLeft = player.x < 800 / 2;
        this.hitCheck(isLeft ? beamAttrLeft : beamAttrRight, player);
    }

    fireBeam_HorizontalFromLeft(portalColorTop, player) {
        const portalColorBottom = this.getOppositeColor(portalColorTop);
        const beamAttrTop = this.storedAttr[portalColorTop];
        const beamAttrBottom = this.storedAttr[portalColorBottom];
        const isTop = player.y < 800 / 2;
        this.hitCheck(isTop ? beamAttrTop : beamAttrBottom, player);
    }

    finishGimmick(player) {
        this.changeState("IDLE");
        player.attribute = "NONE"; 
        this.retreating = true; 
    }

    draw(ctx, elapsedTime) {
        // ★修正: ボス出現前(25秒~30秒)のみWarning表示
        // それ以外のギミック攻撃時のWarningは削除しました
        if (!this.spawned && elapsedTime >= 25 && elapsedTime < 30) {
            ctx.save();
            // 0.25秒間隔で点滅
            if (Math.floor(elapsedTime * 4) % 2 === 0) {
                ctx.fillStyle = "rgba(255, 100, 0, 0.5)"; 
                ctx.beginPath(); 
                ctx.arc(400, 100, 80, 0, Math.PI*2); 
                ctx.fill();
            }
            ctx.fillStyle = "#ffaa00"; 
            ctx.font = "30px Arial"; 
            ctx.textAlign = "center"; 
            ctx.fillText("WARNING", 400, 100); 
            ctx.restore();
        }

        if (!this.active) return;
        
        ctx.save(); 
        ctx.translate(this.x, this.y);
        drawSafeImage(ctx, this.image, -this.width/2, -this.height/2, this.width, this.height, '#ff4444');
        ctx.restore();

        const w = 800; const h = 800;
        const hw = w/2, hh = h/2;

        if (this.state.startsWith("P1")) {
            this.drawPortalPair(ctx, hw/2, h-40, this.p1_bottom, "H");
            this.drawPortalPair(ctx, 40, hh/2, this.p1_bottom, "V");
            this.drawPortalPair(ctx, w-40, hh/2, this.p1_right, "V");

            if (this.state === "P1_WARN") {
                this.drawRect(ctx, 0, 0, hw, 50, this.beam1_top);
                this.drawRect(ctx, hw, 0, hw, 50, this.beam1_top==="LIGHT"?"DARK":"LIGHT");
            } else if (this.state === "P1_FIRE1") {
                this.drawBeam(ctx, 0, 0, hw, h, this.beam1_top);
                this.drawBeam(ctx, hw, 0, hw, h, this.beam1_top==="LIGHT"?"DARK":"LIGHT");
            } else if (this.state === "P1_FIRE2") {
                const beamLeft = this.storedAttr[this.p1_bottom];
                const beamRight = this.storedAttr[this.getOppositeColor(this.p1_bottom)];
                this.drawBeam(ctx, 0, 0, w, hh, beamLeft);
                this.drawBeam(ctx, 0, hh, w, hh, beamRight);
            }
        }
        else if (this.state.startsWith("P2")) {
            this.drawPortalPair(ctx, hw/2, h-40, this.p1_bottom, "H");
            this.drawPortalPair(ctx, 40, hh/2, this.p1_right, "V");
            this.drawPortalPair(ctx, w-40, hh/2, this.p2_right, "V");

            if (this.state === "P2_WARN") {
                this.drawRect(ctx, 0, 0, hw, 50, this.beam2_top);
                this.drawRect(ctx, hw, 0, hw, 50, this.beam2_top==="LIGHT"?"DARK":"LIGHT");
            } else if (this.state === "P2_FIRE1") {
                this.drawBeam(ctx, 0, 0, hw, h, this.beam2_top);
                this.drawBeam(ctx, hw, 0, hw, h, this.beam2_top==="LIGHT"?"DARK":"LIGHT");
            } else if (this.state === "P2_FIRE2") {
                const beamLeft = this.storedAttr[this.p1_right];
                const beamRight = this.storedAttr[this.getOppositeColor(this.p1_right)];
                this.drawBeam(ctx, 0, 0, w, hh, beamLeft);
                this.drawBeam(ctx, 0, hh, w, hh, beamRight);
            }
        }
        else if (this.state.startsWith("P3")) {
            this.drawPortalPair(ctx, hw/2, 40, this.p1_bottom, "H");
            this.drawPortalPair(ctx, 40, hh/2, this.p2_right, "V");

            if (this.state === "P3_FIRE1") {
                this.drawBeamFromPortalTop(ctx, this.p1_bottom, hw, h);
            } else if (this.state === "P3_FIRE2") {
                this.drawBeamFromPortalLeft(ctx, this.p2_right, w, hh);
            }
        }
    }

    drawBeamFromPortalTop(ctx, c1, hw, h) {
        const b1 = this.storedAttr[c1];
        const b2 = this.storedAttr[this.getOppositeColor(c1)];
        this.drawBeam(ctx, 0, 0, hw, h, b1);
        this.drawBeam(ctx, hw, 0, hw, h, b2);
    }
    drawBeamFromPortalLeft(ctx, c1, w, hh) {
        const b1 = this.storedAttr[c1];
        const b2 = this.storedAttr[this.getOppositeColor(c1)];
        this.drawBeam(ctx, 0, 0, w, hh, b1);
        this.drawBeam(ctx, 0, hh, w, hh, b2);
    }

    drawPortalPair(ctx, x, y, c1, orient) {
        const c2 = this.getOppositeColor(c1);
        if (orient === "H") {
            this.drawPortal(ctx, x, y, c1);
            this.drawPortal(ctx, x*3, y, c2); 
        } else { 
            this.drawPortal(ctx, x, y, c1);
            this.drawPortal(ctx, x, y*3, c2);
        }
    }

    drawRect(ctx, x,y,w,h,c) {
        ctx.fillStyle = c==="LIGHT"?"rgba(255,255,200,0.3)":"rgba(100,0,100,0.3)"; ctx.fillRect(x,y,w,h);
        ctx.strokeStyle = c==="LIGHT"?"#ffc":"#a0a"; ctx.strokeRect(x,y,w,h);
    }
    drawBeam(ctx, x,y,w,h,c) {
        ctx.fillStyle = c==="LIGHT"?"rgba(255,255,255,0.8)":"rgba(50,0,50,0.8)"; ctx.fillRect(x,y,w,h);
    }
    drawPortal(ctx, x,y,c) {
        ctx.save(); ctx.translate(x,y); ctx.beginPath(); ctx.arc(0,0,30,0,Math.PI*2);
        let col = "#fff";
        if(c==="RED") col="red"; else if(c==="BLUE") col="blue";
        else if(c==="GREEN") col="#0f0"; else if(c==="YELLOW") col="#ff0";
        else if(c==="ORANGE") col="#f90"; else if(c==="PINK") col="#f0f";
        ctx.fillStyle = col; ctx.globalAlpha=0.5; ctx.fill(); 
        ctx.strokeStyle = "#fff"; ctx.globalAlpha=1.0; ctx.lineWidth=3; ctx.stroke();
        ctx.restore();
    }
}