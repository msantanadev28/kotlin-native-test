const { Canvas, Image } = require('skia-canvas');
const fs = require('fs');
const path = require('path');

async function main() {
    // 1. Create a canvas with a high-resolution background to clearly show the blur and saturation
    const width = 800;
    const height = 600;
    const canvas = new Canvas(width, height);
    const ctx = canvas.getContext('2d');

    // Draw a colorful background (gradients and circles)
    // Dark background
    ctx.fillStyle = '#0a0b10';
    ctx.fillRect(0, 0, width, height);

    // Colorful glowing circles
    const colors = [
        { x: 200, y: 150, r: 180, color: 'rgba(255, 0, 128, 0.8)' },
        { x: 600, y: 200, r: 220, color: 'rgba(0, 128, 255, 0.8)' },
        { x: 400, y: 450, r: 200, color: 'rgba(0, 255, 128, 0.7)' },
        { x: 300, y: 300, r: 120, color: 'rgba(255, 255, 0, 0.8)' }
    ];

    for (const circle of colors) {
        const grad = ctx.createRadialGradient(circle.x, circle.y, 0, circle.x, circle.y, circle.r);
        grad.addColorStop(0, circle.color);
        grad.addColorStop(1, 'rgba(0, 0, 0, 0)');
        ctx.fillStyle = grad;
        ctx.beginPath();
        ctx.arc(circle.x, circle.y, circle.r, 0, Math.PI * 2);
        ctx.fill();
    }

    // Add some diagonal lines or patterns for high frequency detail
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.05)';
    ctx.lineWidth = 2;
    for (let i = -height; i < width; i += 40) {
        ctx.beginPath();
        ctx.moveTo(i, 0);
        ctx.lineTo(i + height, height);
        ctx.stroke();
    }

    // 2. BACKDROP FILTER EFFECT IMPLEMENTATION
    // We define the region of our panel
    const panelX = 150;
    const panelY = 100;
    const panelWidth = 500;
    const panelHeight = 400;
    const borderRadius = 24;

    // A. Capture current canvas state onto an offscreen canvas
    const offscreen = new Canvas(width, height);
    const offscreenCtx = offscreen.getContext('2d');
    offscreenCtx.drawImage(canvas, 0, 0);

    // B. Apply backdrop-filter: blur(90px) saturate(90%)
    ctx.save();
    
    // Create the clipping mask for the panel
    ctx.beginPath();
    ctx.roundRect(panelX, panelY, panelWidth, panelHeight, borderRadius);
    ctx.clip();

    // Apply the CSS-equivalent backdrop filter using CanvasRenderingContext2D.filter
    // 'blur(90px)' blurs, and 'saturate(90%)' (or saturate(0.9)) adjusts saturation
    ctx.filter = 'blur(90px) saturate(90%)';

    // Draw the captured background back onto the canvas inside the clipped region
    ctx.drawImage(offscreen, 0, 0);

    ctx.restore(); // Restore filter and clip

    // C. Apply the background color and border:
    // >> background-color: rgba(26, 27, 32, 0.82);
    // >> border: 1px solid rgba(255, 255, 255, 0.15);
    ctx.save();

    // Draw backdrop tint/overlay
    ctx.fillStyle = 'rgba(26, 27, 32, 0.82)';
    ctx.beginPath();
    ctx.roundRect(panelX, panelY, panelWidth, panelHeight, borderRadius);
    ctx.fill();

    // Draw border
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.15)';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.roundRect(panelX, panelY, panelWidth, panelHeight, borderRadius);
    ctx.stroke();

    ctx.restore();

    // Draw some text inside the panel to show it is a card
    ctx.fillStyle = '#ffffff';
    ctx.font = 'bold 24px system-ui, sans-serif';
    ctx.fillText('Glassmorphism in Skia Canvas', panelX + 40, panelY + 60);

    ctx.fillStyle = 'rgba(255, 255, 255, 0.7)';
    ctx.font = '16px system-ui, sans-serif';
    ctx.fillText('backdrop-filter: blur(90px) saturate(90%)', panelX + 40, panelY + 100);
    ctx.fillText('background-color: rgba(26, 27, 32, 0.82)', panelX + 40, panelY + 130);
    ctx.fillText('border: 1px solid rgba(255, 255, 255, 0.15)', panelX + 40, panelY + 160);

    // Save output to file
    const outputBuffer = await canvas.toBuffer('png');
    const outputPath = path.join(__dirname, 'output.png');
    fs.writeFileSync(outputPath, outputBuffer);
    console.log(`Saved output to ${outputPath}`);
}

main().catch(console.error);
