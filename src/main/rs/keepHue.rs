#pragma version(1)
#pragma rs java_package_name(newera.EliJ)

#include "utility.rsh"

float newHue;
float epsilon;

static int3 keepHue(float3 pixel, float hue){
    float3 hsl = RgbToHsl(pixel);
    float3 new = HslToRgb(hsl);
    hsl.x = restreinHue(hsl.x) / 360.0f;

    newHue = restreinHue(newHue * 360.0f) / 360.0f;

    float infBound = newHue - epsilon;
    float supBound = newHue + epsilon;

    if(infBound < 0){
        infBound = 1.0f + fmod(infBound, 1.0f);
    }

    supBound =  fmod(newHue + epsilon, 1.0f);

    if(epsilon >= 0.5f){
        infBound = 0;
        supBound = 1.0f;
    }

    //case 1
    if(infBound > supBound){
        if((hsl.x > supBound && hsl.x < infBound)){
            int r, g, b;
            r = (int)(new.x);
            g = (int)(new.y);
            b = (int)(new.z);
            int3 pixelGS = convertGrayScale(r, g, b);
            return pixelGS;
        } else {
            int3 newPixel = {(int)(new.x), (int)(new.y), (int)(new.z)};
            return newPixel;
        }
    } else {

        //case 2
        //if the pixel's hue is not in the range [newHue-epsilon; newHue+epsilon], we convert it as gray
        if(!( hsl.x >= infBound && hsl.x <= supBound) ){
            int r, g, b;
            r = (int)(new.x);
            g = (int)(new.y);
            b = (int)(new.z);
            int3 pixelGS = convertGrayScale(r, g, b);
            return pixelGS;
        }

        int3 newPixel = {(int)(new.x), (int)(new.y), (int)(new.z)};
        return newPixel;
    }
}

//MAINS
uchar4 __attribute__((kernel)) KeepSpecificHue(uchar4 in, uint32_t x, uint32_t y) {
    float3 rgb = {in.r, in.g, in.b};
    int3 new = keepHue(rgb, newHue);

    uchar4 out;

    out.a = in.a;
    out.r = (uchar)(new.x);
    out.g = (uchar)(new.y);
    out.b = (uchar)(new.z);

    return out;
}