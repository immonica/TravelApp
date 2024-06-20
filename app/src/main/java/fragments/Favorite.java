package fragments;

public class Favorite {
    private String name;
    private String address;
    private String latLng;
    private String city;
    private String placeType;
    private String key;
    private String placeId;

    public Favorite() {
    }

    public Favorite(String name, String address, String latLng, String city, String placeType) {
        this.name = name;
        this.address = address;
        this.latLng = latLng;
        this.city = city;
        this.placeType = placeType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getLatLng() {
        return latLng;
    }

    public void setLatLng(String latLng) {
        this.latLng = latLng;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPlaceType() {
        return placeType;
    }

    public void setPlaceType(String placeType) {
        this.placeType = placeType;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }
}
