package io.github.spring.middleware.mongo.search;

public class GeoSearch {

    private Coordinates coordinates;
    private Double minDistance;
    private Double maxDistance;

    public Double getMinDistance() {

        return minDistance;
    }

    public void setMinDistance(Double minDistance) {

        this.minDistance = minDistance;
    }

    public Double getMaxDistance() {

        return maxDistance;
    }

    public void setMaxDistance(Double maxDistance) {

        this.maxDistance = maxDistance;
    }

    public Coordinates getCoordinates() {

        return coordinates;
    }

    public void setCoordinates(Coordinates coordinates) {

        this.coordinates = coordinates;
    }
}
