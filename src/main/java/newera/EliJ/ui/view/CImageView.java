package newera.EliJ.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.RenderScript;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import newera.EliJ.R;
import newera.EliJ.ScriptC_fuse_bitmap;
import newera.EliJ.image.Image;
import newera.EliJ.image.processing.EItems;
import newera.EliJ.image.processing.shaders.*;
import newera.EliJ.ui.system.PictureFileManager;
import newera.EliJ.ui.system.SystemActionHandler;
import newera.EliJ.ui.view.inputs.InputManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Emile Barjou-Suire on 09/02/2017.
 */

public class CImageView extends View {
    private final static float MOVE_SAFEZONE = 0.5f;
    private EItems currentInputItem;

    private enum TouchMethod {DRAG, ZOOM, TOOL;}
    private Image image;

    private Point contentCoords;

    private CCanvas cCanvas;

    private Bitmap drawingCache;

    private float contentScale;

    private TouchHandler touchHandler;
    private InputManager inputManager;

    private Paint imagePaint;

    public InputManager getManager() {
        return inputManager;
    }
    public CImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setDrawingCacheEnabled(true);
        image = null;
        this.contentCoords = new Point(0, 0);
        this.touchHandler = new TouchHandler();
        this.contentScale = 1f;

        this.inputManager = new InputManager(this);

        this.imagePaint = new Paint();
        imagePaint.setAntiAlias(false);
        imagePaint.setFilterBitmap(false);

        this.cCanvas = new CCanvas(this);
    }
    /**
     * Set the picture to be displayed on the view.
     * @param image Image object to be displayed.
     */
    public void setImage(Image image)
    {
        if(image != null && !image.isEmpty()) {
            this.image = image;
            contentCoords.x = getWidth() / 2;
            contentCoords.y = getHeight() / 2;
            invalidate();
        }
    }
    /**
     * Reset Image's content.
     */
    void reinitialize(){
        if(this.image != null && !this.image.isEmpty()) {
            this.image.reinitializeBitmap();
            invalidate();
        }
    }

    @Override
    public void onDraw(Canvas canvas){


        canvas.drawColor(getResources().getColor(R.color.colorPrimaryDark));
        if (image != null && !image.isEmpty()){
            image.draw(canvas, imagePaint, contentCoords.x, contentCoords.y, contentScale);

            drawingCache = this.getDrawingCache(true);

            if (cCanvas.isInitialized())
            {
                if(cCanvas.getMethod() == EMethod.DRAW)
                    cCanvas.applyCanvasToImage(1f, canvas, contentCoords.x, contentCoords.y, contentScale);

                if (cCanvas.getMethod() == EMethod.SELECTION)
                    cCanvas.applyCanvasToImage(0.6f, canvas, contentCoords.x, contentCoords.y, contentScale);
            }

            inputManager.draw(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (!inputManager.handleTouch(event))
        {
            if (event.getPointerCount() <= 1) {
                contentScale = touchHandler.onTouch(event, TouchMethod.DRAG, contentCoords, contentScale);
                contentCoords.x = (int) (Math.min(contentCoords.x, getWidth() * MOVE_SAFEZONE + (int) (image.getWidth() * (contentScale/2))));      // need the scale factor
                contentCoords.y = (int) (Math.min(contentCoords.y, getHeight() * MOVE_SAFEZONE + (int) (image.getHeight() * (contentScale/2))));   // somewhere here

            }else{
                contentScale = touchHandler.onTouch(event, TouchMethod.ZOOM, contentCoords, contentScale);
            }
        }
        invalidate();
        return true;
    }

    public Point getContentCoords() {
        return contentCoords;
    }

    public float getContentScale() {
        return contentScale;
    }

    /**
     * Called by InputManager to edit the Image object with the given parameters when user presses Apply.
     * @param shader Shader to apply
     * @param params Shader's parameters
     */
    public void onApplyFilter(Shader shader, Map<String, Object> params)
    {
        shader.setParameters(params);
        if (cCanvas.getMethod() == EMethod.SELECTION)
        {
            Image dup = new Image();
            dup.setDim(image.getW(), image.getH(), image.getFw(), image.getFh());
            for (int i = 0; i < image.getW(); i++)
                for (int j = 0; j < image.getH(); j++)
                    dup.addBitmap(image.getBitmap(i, j).copy(Bitmap.Config.ARGB_8888, true), i, j);

            shader.ApplyFilter(dup);
            RenderScript rs = RenderScript.create(getContext());
            ScriptC_fuse_bitmap fb = new ScriptC_fuse_bitmap(rs);
            for (int i = 0; i < image.getW(); i++)
                for (int j = 0; j < image.getH(); j++)
                {
                    CanvasTool ct = cCanvas.getCanvasTool(i, j);
                    if (ct.active)
                    {
                        Allocation in = Allocation.createFromBitmap(rs, ct.getBitmap());
                        Allocation cut = Allocation.createFromBitmap(rs, dup.getBitmap(i, j));
                        Allocation src = Allocation.createFromBitmap(rs, image.getBitmap(i, j));
                        Allocation out = Allocation.createTyped(rs, src.getType());
                        fb.set_cut(cut);
                        fb.set_src(src);
                        fb.forEach_Cut(in, out);
                        out.copyTo(image.getBitmap(i, j));
                    }
                }

                cCanvas.reset();
        }
        else
        shader.ApplyFilter(image);
    }

    /**
     * Called by InputManager when box is closed by user.
     */
    public void onCancelFilter()
    {

    }

    /**
     * Same usage as onApplyFilter but mainly for system calls. Might be fused with onApplyFilter in the future.
     * @param params Call's parameters
     */
    public void onApplySystem(Map<String, Object> params)
    {
        switch (currentInputItem){
            case S_QUALITY_SAVE:
                try {
                    Bitmap fullBitmap = image.getBitmap();
                    PictureFileManager.SaveBitmap(fullBitmap, (int)params.get("value"));
                    fullBitmap.recycle();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
        }
    }

    //TODO

    /**
     * @return the Image's reference
     */
    public Image getImage()
    {
        return this.image;
    }

    /**
     * Setter for more abstract approach. Might be useless in the future.
     * @param item Item to deal with somewhere else.
     */
    public void setCurrentAction(EItems item) { this.currentInputItem = item; }

    @Override
    public Parcelable onSaveInstanceState()
    {
        Bundle bundle = new Bundle();
        bundle.putParcelable("superState", super.onSaveInstanceState());
        if (image != null)
        {
            bundle.putBoolean("loadedImage", true);
            bundle.putInt("contentCoords.x", contentCoords.x);
            bundle.putInt("contentCoords.y", contentCoords.y);
            bundle.putFloat("contentScale", contentScale);
        }

        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state)
    {
        cCanvas.reset();
        if (state instanceof Bundle) // implicit null check
        {
            Bundle bundle = (Bundle) state;
            if (bundle.getBoolean("loadedImage", false))
            {
                contentCoords = new Point();
                contentCoords.x = bundle.getInt("contentCoords.x");
                contentCoords.y = bundle.getInt("contentCoords.y");
                contentScale = bundle.getFloat("contentScale");
                SystemActionHandler.removeStartupView();
            }

            state = bundle.getParcelable("superState");
        }

        super.onRestoreInstanceState(state);
    }

    private class TouchHandler{

        private int initialX, initialY;

        private int initialContentX, initialContentY;

        private float initialDist, initialScale;
        private int mActivePointerId, pointerIndex;
        private List<Point> touchList;
        TouchHandler(){
            this.touchList = new ArrayList<>();
            this.touchList = new ArrayList<>();
        }
        float onTouch(MotionEvent event, TouchMethod method, Point coord, float scale){
            touchList.clear();
            for(int i = 0; i < event.getPointerCount(); ++i){
                mActivePointerId = event.getPointerId(i);
                pointerIndex = event.findPointerIndex(mActivePointerId);
                touchList.add( new Point((int)event.getX(pointerIndex), (int)event.getY(pointerIndex)) );
            }

            switch(method) {
                case DRAG: {
                    initialDist = -1f;
                    initialScale = scale;
                    switch(event.getAction()) {
                        case MotionEvent.ACTION_DOWN: {
                            initialX = touchList.get(0).x;
                            initialY = touchList.get(0).y;
                            initialContentX = coord.x;
                            initialContentY = coord.y;
                        } break;

                        case MotionEvent.ACTION_MOVE: {
                            coord.x = (int) Math.max(0 - image.getWidth() * contentScale + getWidth() * MOVE_SAFEZONE  + (int) (image.getWidth() * (contentScale/2)), initialContentX + (touchList.get(0).x - initialX)); // need a scale factor somewhere here
                            coord.y = (int) Math.max(0 - image.getHeight() * contentScale + getHeight() * MOVE_SAFEZONE + (int) (image.getHeight() * (contentScale/2)), initialContentY + (touchList.get(0).y - initialY));

                        } break;
                    }
                } break;

                case ZOOM: {
                    if (initialDist < 0)
                    {
                        initialDist = touchList.get(0).distanceFromPoint(touchList.get(1));
                    }else{
                        float currentDist = touchList.get(0).distanceFromPoint(touchList.get(1));
                        scale = currentDist / initialDist * initialScale;

                    }

                } break;

                case TOOL: {
                } break;
            }

            return scale;
        }
    }


    class Point{

        int x, y;

        Point(){
            this.x = 0;
            this.y = 0;
        }

        Point(int x, int y){
            this.x = x;
            this.y = y;
        }
        float distanceFromPoint(Point b) {
            return (float) Math.sqrt((double)((this.x - b.x)*(this.x - b.x) + (this.y - b.y)*(this.y - b.y)));
        }

    }

    public void setcCanvas(CCanvas cCanvas) {
        this.cCanvas = cCanvas;
    }


    public CCanvas getcCanvas() {
        return cCanvas;
    }




}
