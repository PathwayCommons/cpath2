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
import cpath.fetcher.metadata.Metadata;
import cpath.fetcher.metadata.ProviderMetadataService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;

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

    private static Log log = LogFactory.getLog(ProviderMetadataServiceImpl.class);

    /**
     * (non-Javadoc)
     * @see cpath.fetcher.metadata.ProviderMetadataService#getProviderMetadata(java.lang.String)
     */
    @Override
    public Collection<Metadata> getProviderMetadata(final String url) throws IOException {

        // check args
        if (url == null) {
            throw new IllegalArgumentException("url must not be null");
        }

        // setup httpclient and execute method
        HttpClient httpClient = new HttpClient();
        GetMethod method = new GetMethod(url);
        httpClient.executeMethod(method);

        // parse the output
        return readFromMetadataService(method.getResponseBodyAsStream());
    }

    /**
     * Method which parses metadata.
     *
     * @param in InputStream
     * @return Collection<Metadata>
     * @throws IOException
     */
    private Collection<Metadata> readFromMetadataService(InputStream in) throws IOException {

        HashSet<Metadata> toReturn = new HashSet<Metadata>();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            while (reader.ready()) {
                String line = reader.readLine();
                // TODO, parse the page
                System.out.println(line);
            }
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
