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
package cpath.fetcher.metadata.internal;

// imports
import cpath.warehouse.beans.Metadata;
import cpath.fetcher.metadata.ProviderMetadataService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.BufferedReader;

import java.util.HashSet;
import java.util.Collection;


/**
 * Provider Metadata service.  Retrieves provider metadata.
 */
@Service
public final class ProviderMetadataServiceImpl implements ProviderMetadataService {

    // some bits for metadata reading
    private static final int METADATA_IDENTIFIER_INDEX = 0;
    private static final int METADATA_NAME_INDEX = 1;
    private static final int METADATA_VERSION_INDEX = 2;
    private static final int METADATA_RELEASE_DATE_INDEX = 3;
    private static final int METADATA_DATA_URL_INDEX = 4;
    private static final int METADATA_ICON_URL_INDEX = 5;
    private static final int METADATA_IS_PSI_INDEX = 6;
    private static final int NUMBER_METADATA_ITEMS = 7;

    private static Log log = LogFactory.getLog(ProviderMetadataServiceImpl.class);

    /**
     * (non-Javadoc)
     * @see cpath.fetcher.metadata.ProviderMetadataService#getProviderMetadata(java.lang.String)
     */
    @Override
    public Collection<Metadata> getProviderMetadata(final String url) throws IOException {

        Collection<Metadata> toReturn = new HashSet<Metadata>();

        // check args
        if (url == null) {
            throw new IllegalArgumentException("url must not be null");
        }

        // setup httpclient and method
        HttpClient httpClient = new HttpClient();
        GetMethod method = new GetMethod(url);
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, 
                                        new DefaultHttpMethodRetryHandler());
        try {

            // execute
            int statusCode = httpClient.executeMethod(method);

            // get the output
            if (statusCode == 200) {
                toReturn = readFromMetadataService(method.getResponseBodyAsStream());
            }
        }
        finally {
            method.releaseConnection();
        }

        // outta here
        return toReturn;
    }

    /**
     * Method which parses metadata.
     *
     * @param inputStream InputStream
     * @return Collection<Metadata>
     * @throws IOException
     */
    private Collection<Metadata> readFromMetadataService(final InputStream inputStream) throws IOException {

        HashSet<Metadata> toReturn = new HashSet<Metadata>();

        BufferedReader reader = null;
        try {

            // we'd like to read lines at a time
            reader = new BufferedReader(new InputStreamReader(inputStream));

            // are we ready to read?
            while (reader.ready()) {

                // grab a line
                String line = reader.readLine();
                log.info("readFromMetadataService(), line: " + line);

                // for now assume line is delimited by '<br>'
                // TODO: update when data moved to wiki page
                String[] tokens = line.split("<br>");

                if (tokens.length == NUMBER_METADATA_ITEMS) {

					// convert version string to float
					Float version = null;
					try {
						version = new Float(tokens[METADATA_VERSION_INDEX]);
					}
					catch (NumberFormatException e) {
						log.info("number format exception caught for provider: " + tokens[METADATA_IDENTIFIER_INDEX] + " skipping");
						continue;
					}

                    // grab icon data
                    byte[] iconData = getIconData(tokens[METADATA_ICON_URL_INDEX]);

                    if (iconData != null) {

                        // create a metadata bean
                        Metadata metadata = new Metadata(tokens[METADATA_IDENTIFIER_INDEX], tokens[METADATA_NAME_INDEX],
                                                         version, tokens[METADATA_RELEASE_DATE_INDEX],
                                                         tokens[METADATA_DATA_URL_INDEX], iconData,
                                                         new Boolean(tokens[METADATA_IS_PSI_INDEX]));
                        log.info(metadata.getIdentifier());
                        log.info(metadata.getName());
                        log.info(metadata.getVersion());
                        log.info(metadata.getReleaseDate());
                        log.info(metadata.getURLToPathwayData());
                        log.info(tokens[METADATA_ICON_URL_INDEX]);
                        log.info(metadata.isPSI());

                        // add metadata object toc collection we return
                        toReturn.add(metadata);
                    }
                }
            }
        }
        catch (java.io.UnsupportedEncodingException e) {
        }
        catch (IOException e) {
            throw e;
        }
        finally {
            closeQuietly(reader);
        }

        // outta here
        return toReturn;
    }

    /**
     * Given url, fetches byte data for icon
     */
    private byte[] getIconData(final String url) throws IOException {

        byte[] toReturn = null;

        // setup httpclient and method
        HttpClient httpClient = new HttpClient();
        GetMethod method = new GetMethod(url);
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, 
                                        new DefaultHttpMethodRetryHandler());

        try {

            // execute
            int statusCode = httpClient.executeMethod(method);

            // get the output
            if (statusCode == 200) {
                toReturn =  method.getResponseBody();
            }
        }
        finally {
            method.releaseConnection();
        }

        // outta here
        return toReturn;
    }

   /**
    * Close the specified reader quietly.
    *
    * @param reader BufferedReader
    */
    private static void closeQuietly(final BufferedReader reader) {
    
        try {
            reader.close();
        }
        catch (Exception e) {
            // ignore
        }
    }
}
