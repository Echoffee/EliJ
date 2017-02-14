package newera.myapplication.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import newera.myapplication.R;
import newera.myapplication.image.Image;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Emile Barjou-Suire on 09/02/2017.
 */

public class CImageView extends View {
    private enum TouchMethod {DRAG, ZOOM, TOOL};
    private Image image;
    private Point contentCoords;
    private float contentScale;
    private TouchHandler touchHandler;

    public CImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        image = null;
        this.contentCoords = new Point(0, 0);
        this.touchHandler = new TouchHandler();
    }

    public void setImage(Image image)
    {
        this.image = image;
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas){
        if (image == null || image.isEmpty()){
            canvas.drawColor(getResources().getColor(R.color.colorPrimaryDark));
        } else {
            canvas.drawBitmap(image.getBitmap(), contentCoords.x, contentCoords.y, null);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        contentScale = touchHandler.onTouch(event, TouchMethod.DRAG, contentCoords, contentScale);
        invalidate();
        return true;
    }

    private class TouchHandler{
        private int initialX, initialY, initialScale;
        private int initialContentX, initialContentY;
        private float initialDist;
        private boolean method;
        private int mActivePointerId, pointerIndex;
        private List<Point> touchList;


        public TouchHandler(){
            this.touchList = new ArrayList<Point>();
            this.touchList = new ArrayList<Point>();
        }

        public float onTouch(MotionEvent event, TouchMethod method, Point coord, float scale){
            touchList.clear();
            for(int i = 0; i < event.getPointerCount(); ++i){
                mActivePointerId = event.getPointerId(i);
                pointerIndex = event.findPointerIndex(mActivePointerId);
                touchList.add( new Point((int)event.getX(pointerIndex), (int)event.getY(pointerIndex)) );
            }
            switch(method) {
                case DRAG: {
                    switch(event.getAction()) {
                        case MotionEvent.ACTION_DOWN: {
                            initialX = touchList.get(0).x;
                            initialY = touchList.get(0).y;
                            initialContentX = coord.x;
                            initialContentY = coord.y;
                        } break;
                        case MotionEvent.ACTION_MOVE: {
                            coord.x = initialContentX + (touchList.get(0).x - initialX);
                            coord.y = initialContentY + (touchList.get(0).y - initialY);

;                        } break;
                    }
                } break;
                case ZOOM: {
                } break;
                case TOOL: {
                } break;
            }
            return scale;
        }

    }

    private class Point{
        public int x, y;

        public Point(){
            this.x = 0;
            this.y = 0;
        }
        public Point(int x, int y){
            this.x = x;
            this.y = y;
        }
    }
}
