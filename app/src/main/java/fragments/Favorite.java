package fragments;

public class Favorite {
    private String name;
    private String address;
    private String latLng;
    private String key;

    // Default constructor required for calls to DataSnapshot.getValue(Favorite.class)
    public Favorite() {
    }

    public Favorite(String name, String address, String latLng) {
        this.name = name;
        this.address = address;
        this.latLng = latLng;
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

    // Getter and setter for the key field
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
