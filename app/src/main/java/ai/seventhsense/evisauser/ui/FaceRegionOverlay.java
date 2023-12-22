package ai.seventhsense.evisauser.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * Shows a transparent overlay with an oval hole in the middle
 */
public class FaceRegionOverlay extends View {
    private Paint mSemiBlackPaint;
    private final Path mPath = new Path();

    // The width and height of the oval
    int ovalWidth;
    int ovalHeight;

    public FaceRegionOverlay(Context context) {
        super(context);
        initPaints();
    }

    public FaceRegionOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    public FaceRegionOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaints();
    }

    private void initPaints() {
        Paint mTransparentPaint = new Paint();
        mTransparentPaint.setColor(Color.TRANSPARENT);
        mTransparentPaint.setStrokeWidth(10);

        mSemiBlackPaint = new Paint();
        mSemiBlackPaint.setColor(Color.TRANSPARENT);
        mSemiBlackPaint.setStrokeWidth(10);

        Paint mBlackPaint = new Paint();
        mBlackPaint.setColor(Color.BLACK);
    }

    /**
     * Get the long side of the oval
     * @param targetLongSide assuming that the frame is resized to this long side
     * @return the long side of the oval
     */
    public int getOvalLongSide(float targetLongSide) {
        // This is the actual long side of the view
        int viewLongSide = Math.max(getWidth(), getHeight());
        float scale = 1;
        // If the actual long side i    s larger than the target long side, we need to scale down
        if(viewLongSide > targetLongSide) {
            // The scale is the ratio between the target long side and the actual long side
            scale = targetLongSide / viewLongSide;
        }
        // Return the long side of the oval, after scaling
        return (int) (Math.max((float) ovalWidth, (float) ovalHeight) * scale);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(Color.parseColor("#44000000"));

        mPath.reset();

        int width = getWidth();
        int height = getHeight();

        int centerX = (int) (width / 2.0);
        int centerY = (int) (height / 2.0);

        // We want to leave a margin from the screen edge to the closest
        // part of the oval
        float margin = 0.1f;

        float left;
        float right;
        float top;
        float bottom;

        // The ratio of the width to the height of the oval
        float ratio = 0.85f;

        if(width  > height) {
            // We want a smaller margin for landscape
            margin = 0.05f;
            // leave a margin at the top and bottom
            float factor = height * margin;
            top = factor;
            bottom = height - factor;
            // Left and right should be more than top and bottom
            // So we use a factor of 0.85
            left = centerX - (bottom - top) / 2f * ratio;
            right = centerX + (bottom - top) / 2f * ratio;
        } else {
            // leave a margin at the left and right
            float factor = width * margin;
            left = factor;
            right = width - factor;
            // Top and bottom should be more than left and right
            // So we use a factor of 0.85
            top = centerY - (right - left) / 2f / ratio;
            bottom = centerY + (right - left) / 2f / ratio;
        }

        // left, top, right, bottom
        mPath.addOval(left, top, right, bottom, Path.Direction.CW);

        ovalWidth = (int) (right - left);
        ovalHeight = (int) (bottom - top);

        mPath.setFillType(Path.FillType.INVERSE_EVEN_ODD);

        canvas.drawPath(mPath, mSemiBlackPaint);
        canvas.clipPath(mPath);
        canvas.drawColor(Color.parseColor("#55000000"));
    }
}



