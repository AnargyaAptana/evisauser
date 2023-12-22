package ai.seventhsense.evisauser.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * Shows a transparent overlay with a square hole in the middle
 * <p>
 * Also shows a line in the middle of the view
 */
public class QrScannerView extends View {
    private Paint mBackgroundPaint;

    private final Path mPath = new Path();

    private float cornerOffset = 5;

    // We start the line a little before the actual corner pixels
    private final Path mStrokePath = new Path();

    private Paint mStrokePaint;

    private Paint mCornerPaint;
    private ValueAnimator animator;
    private boolean isStrokeVisible = true;
    private Rect roi;

    // A method to calculate a region of interest (ROI) rectangle
    public Rect getRect(int frameWidth, int frameHeight) { // return rectangle of qr scanner view
        // This is the actual long side of the view
        int viewLongSide = Math.max(getWidth(), getHeight());
        int frameLongSide = Math.max(frameWidth, frameHeight);
        float scale = 1;
        // If the actual long side is larger than the target long side, we need to scale down
        if(viewLongSide > frameLongSide) {
            // The scale is the ratio between the target long side and the actual long side
            scale = (float) frameLongSide / (float) viewLongSide;
        }

        float sideOfSquare = (roi.right - roi.left) * scale;

        // Calculate the center of the view
        int centerX = frameWidth / 2;
        int centerY = frameHeight / 2;

        // Calculate the top left corner of the rectangle
        int left = (int) (centerX - sideOfSquare / 2);
        int top = (int) (centerY - sideOfSquare / 2);

        // Calculate the bottom right corner of the rectangle
        int right = (int) (centerX + sideOfSquare / 2);
        int bottom = (int) (centerY + sideOfSquare / 2);

        return new Rect(left, top, right, bottom);
    }


    public QrScannerView(Context context) {
        super(context);
        initPaints();
    }

    public QrScannerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }


    private void initPaints() {

        //draw qr scanner view
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(Color.TRANSPARENT);
        mBackgroundPaint.setStrokeWidth(10);

        //draw center line of qr scanner view
        mStrokePaint = new Paint();
        mStrokePaint.setColor(Color.GREEN); // Stroke color
        mStrokePaint.setStyle(Paint.Style.STROKE); // Style: Stroke
        mStrokePaint.setStrokeWidth(3); // Stroke width

        //draw corner line
        mCornerPaint = new Paint();
        mCornerPaint.setColor(Color.GREEN); // Stroke color
        mCornerPaint.setStyle(Paint.Style.STROKE); // Style: Stroke
        mCornerPaint.setStrokeWidth(12); // Stroke width


        //show and hide the centre line at every 500 milli seconds

        animator = ValueAnimator.ofFloat(0, 1); // Toggle between 0 and 1
        animator.setDuration(500); // Animation duration in milliseconds
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            if (value == 0) {
                isStrokeVisible = false;
            } else {
                isStrokeVisible = true;
            }
            invalidate();
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                animator.start(); // Restart the animation when it ends
            }
        });
        animator.start();

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);


        mPath.reset();

        mStrokePath.reset();


        //get device with and height
        int width = getWidth();
        int height = getHeight();

        //find center point of width and height
        int centerX = (int) (width / 2.0);
        int centerY = (int) (height / 2.0);


        // Leave 10% of the view as a margin (on the smaller side)
        float marginWidth = 0.1f;

        // If the width is larger than the height, we want to leave a larger margin
        if(width > height) {
            marginWidth = 0.2f;
        }

        float left ;
        float top ;
        float right ;
        float bottom ;

        // We want to margin on the smaller side of the view
        float smallSide = Math.min(width, height);
        float factor = smallSide * marginWidth;

        // The margin needs to be on the smaller side
        if(width < height) {

            left = factor;
            right = width - factor;

            // width of square
            float widthOfSquare = right - left;

            // Top will be center y - widthOfSquare / 2
            top = centerY - widthOfSquare / 2;

            // Bottom will be center y + widthOfSquare / 2
            bottom = centerY + widthOfSquare / 2;
        }
        else
        {
            top = factor;
            bottom = height - factor;

            // width of square
            float widthOfSquare = bottom - top;

            // Left will be center x - factor / 2
            left = centerX - widthOfSquare / 2;
            right = centerX + widthOfSquare / 2;
        }

        if (isStrokeVisible) {  //show and hide center line at every 500 milli seconds
            canvas.drawLine(left+cornerOffset,
                    (top+bottom)/2,
                    right-cornerOffset,
                    (top+bottom)/2, mStrokePaint);
       }

        //draw qr scanner view
        mPath.addRect(left, top, right, bottom,  Path.Direction.CW);
        mPath.setFillType(Path.FillType.INVERSE_EVEN_ODD);

        canvas.drawPath(mPath, mBackgroundPaint);
        canvas.clipPath(mPath);
        canvas.drawColor(Color.parseColor("#55000000"));

        // The corner line width
        float cornerLineWidth = getWidth() * 0.1f;


        //draw top left corner line
        canvas.drawLine(left, top, left+cornerLineWidth, top, mCornerPaint);
        canvas.drawLine(left, top-cornerOffset, left, top+cornerLineWidth, mCornerPaint);

        //draw top right corner line
        canvas.drawLine(right, top, right-cornerLineWidth, top, mCornerPaint);
        canvas.drawLine(right, top-cornerOffset, right, top+cornerLineWidth, mCornerPaint);

        //draw bottom left corner line
        canvas.drawLine(left, bottom, left+cornerLineWidth, bottom, mCornerPaint);
        canvas.drawLine(left, bottom+cornerOffset, left, bottom-cornerLineWidth, mCornerPaint);

        //draw bottom right corner line
        canvas.drawLine(right, bottom, right-cornerLineWidth, bottom, mCornerPaint); //cornerOffset is used to cover the line
        canvas.drawLine(right, bottom+cornerOffset, right, bottom-cornerLineWidth, mCornerPaint);


        roi = new Rect((int)left, (int)top, (int)right, (int)bottom); //  create qr scanner rectangle
    }
}
