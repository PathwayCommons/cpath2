package cpath.warehouse.beans;

// imports
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Entity;

/**
 * Data Provider Metadata.
 */
@Entity
@Table(name="METADATA")
public final class Metadata {

    // some members
    private final String cv;
    private final String name;
    private final String version;
    private final String releaseDate;
    private final String urlToPathwayData;
    private final byte[] icon;


    /**
     * Create a Metadata obj with the specified properties;
     *
     * @param cv String (string used in web service calls)
     * @param name String
     * @param version String
     * @param releaseDate String
     * @param urlToPathwayData String
     * @param icon byte[]
     */
    public Metadata(final String cv, final String name, final String version,
                    final String releaseDate, final String urlToPathwayData, final byte[] icon) {

        if (cv == null) {
            throw new IllegalArgumentException("cv must not be null");
        }
        this.cv = cv;

        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        this.name = name;

        if (version == null) {
            throw new IllegalArgumentException("version must not be null");
        }
        this.version = version;

        if (releaseDate == null) {
            throw new IllegalArgumentException("release data must not be null");
        }
        this.releaseDate = releaseDate;

        if (urlToPathwayData == null) {
            throw new IllegalArgumentException("URL to pathway data must not be null");
        }
        this.urlToPathwayData = urlToPathwayData;

        if (icon == null) {
            throw new IllegalArgumentException("icon must not be null");
        }
        this.icon = icon;
    }

    /**
     * Return the data provider CV.
     *
     * @return String
     */
    @Id
    public String getCV() {
        return cv;
    }

    /**
     * Return the data provider name.
     *
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the version of the current release.
     *
     * @return String
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the release data of the current release.
     *
     * @return String
     */
    public String getReleaseDate() {
        return releaseDate;
    }

    /**
     * Returns the url to the provider pathway data.
     *
     * @return String
     */
    public String getURLToPathwayData() {
        return urlToPathwayData;
    }

    /**
     * Returns the icon data.
     *
     * @return byte[]
     */
    public byte[] getIcon() {
        return icon;
    }

    @Override
    public String toString() {
        return name;
    }
}
