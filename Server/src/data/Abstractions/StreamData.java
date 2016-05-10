package data.Abstractions;

/**
 * Created by Aleksand Smilyanskiy on 30.04.2016.
 * "The more we do, the more we can do." ©
 */

/**
 * Представляет всю необходимую информацию о стриме
 */
public class StreamData {
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
}
