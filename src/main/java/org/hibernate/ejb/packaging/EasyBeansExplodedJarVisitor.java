/**
 * EasyBeans
 * Copyright (C) 2012 Bull S.A.S.
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to strip WEB-INF/classes in entry names when scanning war file.
 * @author Florent Benoit
 */
public class EasyBeansExplodedJarVisitor extends AbstractJarVisitor {

    private static final String WAR_SUFFIX = ".war";

    private static final String CLASS_SUFFIX = ".class";

    private static final String WEB_PREFIX = "WEB-INF/classes";

    private static final int WEb_PREFIX_AFTER_INDEX = WEB_PREFIX.length();
    
    
    private final Logger log = LoggerFactory.getLogger( ExplodedJarVisitor.class );
    
    private String entry;
    
    /**
     * URL
     */
    private String path = null;
    
    public EasyBeansExplodedJarVisitor(URL url, Filter[] filters, String entry) {
        super( url, filters );
        this.entry = entry;
        this.path = url.getPath();
    }

    public EasyBeansExplodedJarVisitor(String fileName, Filter[] filters) {
        super(fileName, filters);
        this.path = fileName;
    }
    
    
    private boolean isWar() {
        if (path.endsWith(WAR_SUFFIX) || path.endsWith(WAR_SUFFIX.concat("/"))) {
            return true;
        }
        return false;
    }

    private String treat(final String name) {
        // treat WEB-INF/classes case
        if (name.endsWith(CLASS_SUFFIX)) {
            if (name.startsWith(WEB_PREFIX)) {
                return name.substring(WEb_PREFIX_AFTER_INDEX + 1);
            }
        }
        return name;
    }
    
    protected void doProcessElements() throws IOException {
        File jarFile;
        try {
            String filePart = jarUrl.getFile();
            if ( filePart != null && filePart.indexOf( ' ' ) != -1 ) {
                //unescaped (from the container), keep as is
                jarFile = new File( jarUrl.getFile() );
            }
            else {
                jarFile = new File( jarUrl.toURI().getSchemeSpecificPart() );
            }
        }
        catch (URISyntaxException e) {
            log.warn( "Malformed url: " + jarUrl, e );
            return;
        }
        
        if ( !jarFile.exists() ) {
            log.warn( "Exploded jar does not exists (ignored): {}", jarUrl );
            return;
        }
        if ( !jarFile.isDirectory() ) {
            log.warn( "Exploded jar file not a directory (ignored): {}", jarUrl );
            return;
        }
        File rootFile;
        if (entry != null && entry.length() > 0 && ! "/".equals( entry ) ) {
            rootFile = new File(jarFile, entry);
        }
        else {
            rootFile = jarFile;
        }
        if ( rootFile.isDirectory() ) {
            getClassNamesInTree( rootFile, null );
        }
        else {
            //assume zipped file
            processZippedRoot(rootFile);
        }
    }

    //FIXME shameful copy of FileZippedJarVisitor.doProcess()
    //TODO long term fix is to introduce a process interface (closure like) to addElements and then share the code
    private void processZippedRoot(File rootFile) throws IOException {
        JarFile jarFile = new JarFile(rootFile);
        Enumeration<? extends ZipEntry> entries = jarFile.entries();
        while ( entries.hasMoreElements() ) {
            ZipEntry zipEntry = entries.nextElement();
            String name = zipEntry.getName();
            if ( !zipEntry.isDirectory() ) {
                //build relative name
                if ( name.startsWith( "/" ) ) name = name.substring( 1 );
                
                if (isWar()) {
                    // Check if entry name needs treatment
                    name = treat(name);
                }
                
                
                addElement(
                        name,
                        new BufferedInputStream( jarFile.getInputStream( zipEntry ) ),
                        new BufferedInputStream( jarFile.getInputStream( zipEntry ) )
                );
            }
        }
    }

    private void getClassNamesInTree(File jarFile, String header) throws IOException {
        File[] files = jarFile.listFiles();
        header = header == null ? "" : header + "/";
        for ( File localFile : files ) {
            if ( !localFile.isDirectory() ) {
                String entryName = localFile.getName();
                String updatedEntryName = header + entryName;
                if (isWar()) {
                    updatedEntryName = treat(updatedEntryName);
                }
                
                addElement(
                        updatedEntryName,
                        new BufferedInputStream( new FileInputStream( localFile ) ),
                        new BufferedInputStream( new FileInputStream( localFile ) )
                );

            }
            else {
                getClassNamesInTree( localFile, header + localFile.getName() );
            }
        }
    }

}
