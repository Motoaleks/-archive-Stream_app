package data.Abstractions;

/**
 * Created by Aleksand Smilyanskiy on 30.04.2016.
 * "The more we do, the more we can do." ©
 */
public class PictureData {
    // ширина кадра, ширина и высота картинки
    private int frameLength, width, height;

    public PictureData() {
    }

    public PictureData(int frameLength, int width, int height) {
        this();
        setPictureFormat(frameLength,width,height);
    }

    public boolean checkCorrect(){
        return frameLength > 0 && width > 0 && height > 0;
    }


    // Сеттеры
    public void setPictureFormat(int length, int width, int height){
        setFrameLength(length);
        setWidth(width);
        setHeight(height);
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setFrameLength(int frameLength) {
        this.frameLength = frameLength;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    // Геттеры

    public int getFrameLength() {
        return frameLength;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
