/**
 * EasyBeans
 * Copyright (C) 2008-2012 Bull S.A.S.
 * Contact: easybeans@ow2.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * --------------------------------------------------------------------------
 * $Id: BundleVisitor.java 5628 2010-10-12 15:45:41Z benoitf $
 * --------------------------------------------------------------------------
 */

package org.hibernate.ejb.packaging;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;

import org.osgi.framework.BundleContext;
import org.ow2.easybeans.util.osgi.BCMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to analyze data from an OSGi bundle.
 * @author Florent Benoit
 */
public class BundleVisitor extends AbstractJarVisitor {

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger( BundleVisitor.class );

    /**
     * Entry to use for this visitor.
     */
    private String entry;

    /**
     * Constructor by giving URL, array of filters and the expected entry to analyze.
     * @param url the given url.
     * @param filters the array of filters
     * @param entry the entry to analyze
     */
    public BundleVisitor(URL url, Filter[] filters, String entry) {
        super(url, filters);
        this.entry = entry;
    }

    /**
     * Implementation of the super abstract method that will analyze the bundle.
     * @throws IOException
     */
    protected void doProcessElements() throws IOException {

        // Get a bundle context from our URL
        BCMapper mapper = BCMapper.getInstance();
        BundleContext bc = mapper.get(jarUrl);

        // Get the entries of this bundle
        //TODO: Find a given entry that was provided in the constructor
        Enumeration<URL> entries = (Enumeration<URL>) bc.getBundle().findEntries("", "*", true);

        // Now add each element
        while (entries.hasMoreElements()) {
            URL url = entries.nextElement();

            // Get InputStream from this url
            URLConnection urlConnection = url.openConnection();

            // Disable cache
            urlConnection.setDefaultUseCaches(false);

            // Get Stream
            InputStream is = null;

            try {
                is = urlConnection.getInputStream();
            byte[] entryBytes = JarVisitorFactory.getBytesFromInputStream(is);

                                String subname = url.getPath();
                                if ( subname.startsWith( "/" ) ) subname = subname.substring( 1 );
                                addElement(
                                        subname,
                                        new ByteArrayInputStream(entryBytes),
                                        new ByteArrayInputStream(entryBytes)
                                );
            } finally {
                if (is != null) {
                    is.close();
                }
            }

        }


    }
}
