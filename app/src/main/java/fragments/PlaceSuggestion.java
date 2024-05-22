package fragments;

public class PlaceSuggestion {
    private String name;
    private String city;
    private String address; // Add address field
    private double latitude;
    private double longitude;
    private String placeId;

    // Default constructor (no-argument constructor) required by Firebase
    public PlaceSuggestion() {
        // Default constructor required by Firebase
    }

    public PlaceSuggestion(String name, String city, String address, double latitude, double longitude, String placeId) {
        this.name = name;
        this.city = city;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.placeId = placeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }
}
