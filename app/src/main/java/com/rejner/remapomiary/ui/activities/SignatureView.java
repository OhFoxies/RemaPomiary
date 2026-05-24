package com.rejner.remapomiary.ui.activities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class SignatureView extends View {
    private Paint paint;
    private Path path;
    private Bitmap bitmap;
    private Canvas canvas;

    public SignatureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(8f);
        path = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE); // Tło podpisu
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        canvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // BLOKADA: Informujemy rodzica (np. ScrollView), żeby nie przejmował dotyku
                getParent().requestDisallowInterceptTouchEvent(true);
                path.moveTo(x, y);
                return true;

            case MotionEvent.ACTION_MOVE:
                getParent().requestDisallowInterceptTouchEvent(true);
                path.lineTo(x, y);
                break;

            case MotionEvent.ACTION_UP:
                // Odblokowanie dotyku po podniesieniu palca
                getParent().requestDisallowInterceptTouchEvent(false);
                canvas.drawPath(path, paint);
                path.reset();
                break;

            default:
                return false;
        }
        invalidate();
        return true;
    }

    public Bitmap getSignatureBitmap() {
        return bitmap;
    }

    public void clear() {
        path.reset();
        if (canvas != null) {
            canvas.drawColor(Color.WHITE);
        }
        invalidate();
    }
}