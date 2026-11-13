package com.host1syan.claudecn.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

public class StatusDot extends View {
    public static final int ATTENTION = 2;
    public static final int NONE = 0;
    public static final int RUNNING = 1;
    private final Paint paint;
    private final Palette palette;
    private int state;

    public int getState() {
        return this.state;
    }

    public StatusDot(Context context, Palette palette) {
        super(context);
        this.paint = new Paint(1);
        this.state = 0;
        this.palette = palette;
    }

    public void setState(int i) {
        if (i == this.state) {
            return;
        }
        this.state = i;
        invalidate();
    }

    @Override
    protected void onMeasure(int i, int i2) {
        setMeasuredDimension(Ui.dp(getContext(), 16.0f), Ui.dp(getContext(), 18.0f));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (this.state == 0) {
            return;
        }
        float width = getWidth() / 2.0f;
        float height = getHeight() / 2.0f;
        if (this.state == 1) {
            int i = this.palette.success;
            this.paint.setStyle(Paint.Style.FILL);
            this.paint.setColor((i & 16777215) | 855638016);
            canvas.drawCircle(width, height, Ui.dp(getContext(), 5.0f), this.paint);
            this.paint.setColor(this.palette.success);
            canvas.drawCircle(width, height, Ui.dp(getContext(), 3.0f), this.paint);
            return;
        }
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setColor(this.palette.warning);
        canvas.drawCircle(width, height, Ui.dp(getContext(), 6.5f), this.paint);
        this.paint.setColor(-1);
        float dp = Ui.dp(getContext(), 1.7f) / 2.0f;
        canvas.drawRoundRect(new RectF(width - dp, height - Ui.dp(getContext(), 3.2f), width + dp, Ui.dp(getContext(), 0.6f) + height), dp, dp, this.paint);
        canvas.drawCircle(width, height + Ui.dp(getContext(), 2.6f), Ui.dp(getContext(), 0.95f), this.paint);
    }
}
