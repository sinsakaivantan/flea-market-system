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