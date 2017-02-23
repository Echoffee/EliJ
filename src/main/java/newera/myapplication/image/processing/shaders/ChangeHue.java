package newera.myapplication.image.processing.shaders;

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;

import newera.myapplication.MainActivity;
import newera.myapplication.R;
import newera.myapplication.ScriptC_hue;
import newera.myapplication.ScriptC_lightness;
import newera.myapplication.image.Image;
import newera.myapplication.ui.view.CImageView;

/**
 * Created by Romain on 19/02/2017.
 */

public class ChangeHue extends Shader {
    private Bitmap icone = null;

    @Override
    public void ApplyFilter(Image image)
    {
        if(image != null && !image.isEmpty()) {

            ScriptC_hue rsChangeHue = new ScriptC_hue(renderScript);
            rsChangeHue.set_factor(((int) params.get("value") * (1f/360f)));
            for (Bitmap[] b1 : image.getBitmaps())
                for (Bitmap b : b1) {
                    Allocation in = Allocation.createFromBitmap(renderScript, b);
                    Allocation out = Allocation.createTyped(renderScript, in.getType());
                    rsChangeHue.forEach_ChangeHue(in, out);
                    out.copyTo(b);
                }
        }
        //refreshImage();
    }

    public ChangeHue(MainActivity activity) {
        super(activity);
    }

    public ChangeHue(Context context)
    {
        super(context);
    }

    public String getName(){
        return activity.getResources().getString(R.string.shaderChangeHueName);
    }

    @Override
    public int getNameId() {
        return 0;
    }

    @Override
    public Bitmap getIcone() {
        return icone;
    }

}
