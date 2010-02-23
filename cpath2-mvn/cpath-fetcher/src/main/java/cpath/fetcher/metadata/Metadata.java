// $Id$
//------------------------------------------------------------------------------
/** Copyright (c) 2010 Memorial Sloan-Kettering Cancer Center.
 **
 ** This library is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** Memorial Sloan-Kettering Cancer Center
 ** has no obligations to provide maintenance, support,
 ** updates, enhancements or modifications.  In no event shall
 ** Memorial Sloan-Kettering Cancer Center
 ** be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** Memorial Sloan-Kettering Cancer Center
 ** has been advised of the possibility of such damage.  See
 ** the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this library; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 **/
package cpath.fetcher.metadata;

/**
 * Data Provider Metadata.
 */
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
                    final String releaseDate, final String urlToPathwayData, byte[] icon) {

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
