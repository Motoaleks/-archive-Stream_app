package com.example.aleksandr.socketstreamer.data.Abstractions;

/**
 * Created by Aleksand Smilyanskiy on 30.04.2016.
 * "The more we do, the more we can do." ©
 */

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Представляет всю необходимую информацию о стриме
 */
public class StreamData implements Parcelable{
    // параметры изображения
    private PictureData pictureData;
    // координаты
    private Coordinate coordinate;
    // id стрима
    private String id;
    // имя стрима
    private String name;

    public StreamData() {

    }

    public StreamData(PictureData pictureData) {
        this();
        setPictureData(pictureData);
    }

    public StreamData(String id, String name) {
        this();
        setId(id);
        setName(name);
    }

    public StreamData(String id, String name, PictureData pictureData) {
        this();
        setId(id);
        setName(name);
        setPictureData(pictureData);
    }

    // Сеттеры
    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    public void setPictureData(PictureData pictureData) {
        this.pictureData = pictureData;
    }

    public void setId(String id) {
        if (this.id != null)
            throw new IllegalArgumentException("Id already set.");
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Геттеры

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public PictureData getPictureData() {
        return pictureData;
    }


    // Parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getId());
        dest.writeString(getName());
    }

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<StreamData> CREATOR = new Parcelable.Creator<StreamData>() {
        public StreamData createFromParcel(Parcel in) {
            return new StreamData(in);
        }

        public StreamData[] newArray(int size) {
            return new StreamData[size];
        }
    };

    // example constructor that takes a Parcel and gives you an object populated with it's values
    private StreamData(Parcel in) {
        setId(in.readString());
        setName(in.readString());
    }
}
