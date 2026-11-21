package org.telegram.ui.Components.blur3.drawable;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.blur3.LiquidGlassEffect;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSource;

import xyz.nextalone.nagram.NaConfig;

@RequiresApi(api = Build.VERSION_CODES.Q)
public class BlurredBackgroundDrawableRenderNode extends BlurredBackgroundDrawable {
    private final BlurredBackgroundSource source;
    private final Outline outline = new Outline();
    private final Rect outlineRect = new Rect();

    private final RenderNode renderNode;
    private final RenderNode renderNodeFill;

    private final Paint paintShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintStrokeTop = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintStrokeBottom = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintGlassSheen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintGlassRim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path glassPath = new Path();
    private final float[] glassRadii = new float[8];

    private boolean renderNodeInvalidated;

    public BlurredBackgroundDrawableRenderNode(BlurredBackgroundSource source) {
        this.renderNode = new RenderNode("BlurredNode");
        this.renderNodeFill = new RenderNode("BlurredFill");
        this.renderNode.setClipToOutline(true);
        this.renderNode.setClipToBounds(true);

        this.source = source;

        this.paintShadow.setColor(0);
        this.paintStrokeTop.setStyle(Paint.Style.STROKE);
        this.paintStrokeBottom.setStyle(Paint.Style.STROKE);
        this.paintGlassRim.setStyle(Paint.Style.STROKE);
    }

    @Override
    public BlurredBackgroundDrawable setClipToOutline(boolean clipToOutline) {
        renderNode.setClipToOutline(clipToOutline);
        return super.setClipToOutline(clipToOutline);
    }

    private LiquidGlassEffect liquidGlassEffect;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void setLiquidGlassEffectAllowed() {
        liquidGlassEffect = new LiquidGlassEffect(renderNodeFill);
    }


    @Override
    public BlurredBackgroundSource getSource() {
        return source;
    }

    @Override
    protected void onBoundPropsChanged() {
        super.onBoundPropsChanged();

        paintStrokeTop.setStrokeWidth(boundProps.strokeWidthTop);
        paintStrokeBottom.setStrokeWidth(boundProps.strokeWidthBottom);

        outlineRect.set(0, 0,
            boundProps.boundsWithPadding.width(),
            boundProps.boundsWithPadding.height()
        );
        getOutline(outline, outlineRect, boundProps.radii);
        outline.setAlpha(1);

        if (!boundProps.boundsWithPadding.isEmpty()) {
            renderNodeFill.setPosition(0, 0, boundProps.boundsWithPadding.width(), boundProps.boundsWithPadding.height());
            renderNode.setPosition(0, 0, boundProps.boundsWithPadding.width(), boundProps.boundsWithPadding.height());
            renderNode.setOutline(outline);

            renderNodeInvalidated = true;
        }
    }

    @Override
    protected void onSourceOffsetChange(float sourceOffsetX, float sourceOffsetY) {
        super.onSourceOffsetChange(sourceOffsetX, sourceOffsetY);
        renderNodeInvalidated = true;
    }

    @Override
    public boolean hasDisplayList() {
        return renderNode.hasDisplayList();
    }

    /**
     * VibeGram: iOS-like glass needs more than edge refraction to read as
     * glass. A soft specular sheen from the top-left corner plus a bright rim
     * give the surface its "pane of glass" look in both themes.
     */
    private void drawGlassHighlights(Canvas c) {
        final int w = boundProps.boundsWithPadding.width();
        final int h = boundProps.boundsWithPadding.height();
        if (w <= 0 || h <= 0) {
            return;
        }
        if (NaConfig.INSTANCE.getDisableGlareEffects().Bool()) {
            return;
        }
        final float maxRadius = Math.min(w, h) / 2f;
        for (int i = 0; i < 8; i++) {
            glassRadii[i] = Math.min(boundProps.shaderRadii[i], maxRadius);
        }
        glassPath.rewind();
        glassPath.addRoundRect(0, 0, w, h, glassRadii, Path.Direction.CW);

        paintGlassSheen.setShader(new LinearGradient(
            0, 0, w * 0.25f, h,
            new int[]{0x42FFFFFF, 0x1AFFFFFF, 0x00FFFFFF},
            new float[]{0f, 0.4f, 1f},
            Shader.TileMode.CLAMP
        ));
        c.drawPath(glassPath, paintGlassSheen);

        paintGlassRim.setStrokeWidth(dp(1f));
        paintGlassRim.setShader(new LinearGradient(
            0, 0, w, h,
            new int[]{0x7AFFFFFF, 0x1FFFFFFF},
            null,
            Shader.TileMode.CLAMP
        ));
        c.drawPath(glassPath, paintGlassRim);
    }

    @Override
    public void updateDisplayList() {
        final float offsetX = sourceOffsetX;
        final float offsetY = sourceOffsetY;

        Canvas c;

        final float sL = boundProps.boundsWithPadding.left + offsetX;
        final float sT = boundProps.boundsWithPadding.top + offsetY;
        final float sR = boundProps.boundsWithPadding.right + offsetX;
        final float sB = boundProps.boundsWithPadding.bottom + offsetY;

        c = renderNodeFill.beginRecording();
        c.save();
        c.translate(-sL, -sT);
        if (liquidGlassEffect != null && Build.VERSION.SDK_INT >= 33) {
            final int thickness = Math.max(Math.min(
                boundProps.liquidThickness <= 0 ? dp(18) : boundProps.liquidThickness,
                Math.min(boundProps.boundsWithPadding.width(), boundProps.boundsWithPadding.height()) / 5), 1);

            // VibeGram: an opaque theme colour would completely hide the
            // refraction, so the glass tint is always kept translucent.
            int glassForeground = backgroundColor;
            final int glassAlpha = Color.alpha(glassForeground);
            if (glassAlpha > 0) {
                final int targetAlpha = Math.min(255, Math.round(glassAlpha * 0.62f));
                glassForeground = (glassForeground & 0x00FFFFFF) | (targetAlpha << 24);
            }

            liquidGlassEffect.update(
                0, 0, boundProps.boundsWithPadding.width(), boundProps.boundsWithPadding.height(),
                boundProps.shaderRadii[0], boundProps.shaderRadii[2], boundProps.shaderRadii[4], boundProps.shaderRadii[6],
                thickness,
                boundProps.liquidIntensity,
                boundProps.liquidIndex,
                glassForeground
            );
        }
        source.draw(c, sL, sT, sR, sB);
        c.save();
        renderNodeFill.endRecording();


        c = renderNode.beginRecording();
        if (liquidGlassEffect != null) {
            // Always draw the refracted content, even under an opaque theme
            // colour, and put the specular highlights on top of it.
            c.drawRenderNode(renderNodeFill);
            drawGlassHighlights(c);
        } else if (Color.alpha(backgroundColor) == 255) {
            c.drawColor(backgroundColor);
        } else {
            c.drawRenderNode(renderNodeFill);
            if (Color.alpha(backgroundColor) != 0) {
                c.drawColor(backgroundColor);
            }
        }
        if (strokeColorTop != 0) {
            drawStroke(c, 0, 0, boundProps.boundsWithPadding.width(),
                    boundProps.boundsWithPadding.height(), boundProps.radii,
                    boundProps.strokeWidthTop, true, paintStrokeTop);
        }
        if (strokeColorBottom != 0) {
            drawStroke(c, 0, 0, boundProps.boundsWithPadding.width(),
                    boundProps.boundsWithPadding.height(), boundProps.radii,
                    boundProps.strokeWidthBottom, false, paintStrokeBottom);
        }
        renderNode.endRecording();
    }

    @Override
    public void updateColors() {
        super.updateColors();

        paintShadow.setShadowLayer(shadowLayerRadius, shadowLayerDx, shadowLayerDy, shadowColor);
        paintStrokeTop.setColor(strokeColorTop);
        paintStrokeBottom.setColor(strokeColorBottom);

        renderNodeInvalidated = true;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (boundProps.boundsWithPadding.isEmpty()) {
            return;
        }

        if (!canvas.isHardwareAccelerated()) {
            drawSource(canvas, source);
            return;
        }

        if (!renderNode.hasDisplayList()) {
            source.dispatchOnDrawablesRelativePositionChange();
            updateDisplayList();
        } else if (renderNodeInvalidated) {
            updateDisplayList();
        }
        renderNodeInvalidated = false;

        int color = Theme.multAlpha(shadowColor, renderNode.getAlpha() * shadowAlpha);
        if (Color.alpha(color) != 0) {
            paintShadow.setShadowLayer(shadowLayerRadius, shadowLayerDx, shadowLayerDy, color);
            boundProps.drawShadows(canvas, paintShadow, inAppKeyboardOptimization);
        }

        canvas.save();
        canvas.translate(boundProps.boundsWithPadding.left, boundProps.boundsWithPadding.top);
        canvas.drawRenderNode(renderNode);
        canvas.restore();
    }

    public void invalidateDisplayList() {
        renderNodeInvalidated = true;
    }

    @Override
    public void setAlpha(int alpha) {
        final int oldAlpha = getAlpha();

        super.setAlpha(alpha);
        renderNode.setAlpha(alpha / 255f);
        renderNodeInvalidated = true;

        if (oldAlpha == 0 && alpha > 0) {
            source.dispatchOnDrawablesRelativePositionChange();
        }
    }

    @Override
    protected void onSourceRelativePositionChanged(RectF position) {
        super.onSourceRelativePositionChanged(position);
        source.dispatchOnDrawablesRelativePositionChange();
    }
}
