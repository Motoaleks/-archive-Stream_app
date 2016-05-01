package data.Abstractions;

/**
 * Created by Aleksand Smilyanskiy on 30.04.2016.
 * "The more we do, the more we can do." ©
 */
public class Coordinate {
    // Широта и долгота
    int latitude, longitude;

    Coordinate() {
    }

    Coordinate(int latitude, int longitude) {
        this();
        setCoordinates(latitude, longitude);
    }


    // Сеттеры
    public void setLatitude(int latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(int longitude) {
        this.longitude = longitude;
    }

    public void setCoordinates(int latitude, int longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public int getLatitude() {
        return latitude;
    }

    public int getLongitude() {
        return longitude;
    }

    @Override
    public String toString() {
        return "Latitude: " + latitude + "\tLongitude: " + longitude;
    }
}
