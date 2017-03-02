package newera.EliJ.image.processing.shaders;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import newera.EliJ.MainActivity;
import newera.EliJ.R;
import newera.EliJ.ScriptC_grayscale;
import newera.EliJ.image.Image;


/**
 * Created by romain on 09/02/17.
 */

public class GrayScale extends Shader{

    public GrayScale(MainActivity activity) {
        super(activity);
        this.drawableIconId = R.drawable.ic_grayscale_gradient_black_24dp;
        this.clickableName = R.string.shaderGrayScaleName;
    }

    @Override
    public void ApplyFilter(Image image)
    {
        if(image != null && !image.isEmpty()) {

            ScriptC_grayscale rsGrayscale = new ScriptC_grayscale(renderScript);

            for (Bitmap[] arrBitmap : image.getBitmaps())
                for (Bitmap bitmap : arrBitmap) {
                    Allocation in = Allocation.createFromBitmap(renderScript, bitmap);
                    Allocation out = Allocation.createTyped(renderScript, in.getType());

                    rsGrayscale.forEach_Grayscale(in, out);
                    out.copyTo(bitmap);
                }
        }
        refreshImage();
    }

}