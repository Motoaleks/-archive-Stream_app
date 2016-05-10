package data.Abstractions;

/**
 * Created by Aleksand Smilyanskiy on 30.04.2016.
 * "The more we do, the more we can do." ©
 */
public class Coordinate {
    // Широта и долгота
    double latitude, longitude;

    Coordinate() {
    }

    public Coordinate(double latitude, double longitude) {
        this();
        setCoordinates(latitude, longitude);
    }

    public Coordinate(double[] data){
        this();
        setCoordinates(data[0],data[1]);
    }


    // Сеттеры
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setCoordinates(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != this.getClass()){
            return false;
        }
        return this.longitude == ((Coordinate) obj).longitude && this.latitude == ((Coordinate) obj).latitude;
    }

    @Override
    public String toString() {
        return "Latitude: " + latitude + "\tLongitude: " + longitude;
    }
}
