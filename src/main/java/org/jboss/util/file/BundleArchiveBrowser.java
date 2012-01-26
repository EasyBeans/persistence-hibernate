/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.util.file;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;

import org.osgi.framework.BundleContext;
import org.ow2.easybeans.util.osgi.BCMapper;

/**
 * Comment
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision: 1456 $
 *
 */
public class BundleArchiveBrowser implements Iterator {

    Enumeration entries = null;;

    URL next = null;

    ArchiveBrowser.Filter filter = null;

    public BundleArchiveBrowser(URL url, ArchiveBrowser.Filter filter) {

        BCMapper mapper = BCMapper.getInstance();
        BundleContext bc = mapper.get(url);

        entries = bc.getBundle().findEntries("", "*", true);
        this.filter = filter;

        setNext();
    }

    public boolean hasNext() {
        return next != null;
    }

    private void setNext() {
        next = null;
        while (entries.hasMoreElements() && next == null) {
            next = (URL) entries.nextElement();

            if (next != null && !filter.accept(next.getFile())) {
            	next = null;
            }
        }
    }

    public Object next() {
        URL entry = next;
        setNext();

        try {
            return entry.openStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void remove() {
        throw new RuntimeException("Illegal operation on ArchiveBrowser");
    }
}
