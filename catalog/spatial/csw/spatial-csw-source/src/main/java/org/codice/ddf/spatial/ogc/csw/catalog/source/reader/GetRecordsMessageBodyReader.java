/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.spatial.ogc.csw.catalog.source.reader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.MessageBodyReader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.tika.metadata.HttpHeaders;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.GetRecordsResponseConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.DataHolder;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.xml.XppDriver;
import com.thoughtworks.xstream.io.xml.XppReader;

import ddf.catalog.data.Metacard;
import ddf.catalog.resource.impl.ResourceImpl;

/**
 * Custom JAX-RS MessageBodyReader for parsing a CSW GetRecords response, extracting the search
 * results and CSW records.
 */
public class GetRecordsMessageBodyReader implements MessageBodyReader<CswRecordCollection> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetRecordsMessageBodyReader.class);

    private XStream xstream;

    private DataHolder argumentHolder;

    public GetRecordsMessageBodyReader(Converter provider, CswSourceConfiguration configuration) {
        xstream = new XStream(new XppDriver());
        xstream.setClassLoader(this.getClass()
                .getClassLoader());
        GetRecordsResponseConverter converter = new GetRecordsResponseConverter(provider);
        xstream.registerConverter(converter);
        xstream.alias(CswConstants.GET_RECORDS_RESPONSE, CswRecordCollection.class);
        xstream.alias(CswConstants.CSW_NAMESPACE_PREFIX + CswConstants.NAMESPACE_DELIMITER
                + CswConstants.GET_RECORDS_RESPONSE, CswRecordCollection.class);
        buildArguments(configuration);
    }

    private void buildArguments(CswSourceConfiguration configuration) {
        argumentHolder = xstream.newDataHolder();
        argumentHolder.put(CswConstants.OUTPUT_SCHEMA_PARAMETER, configuration.getOutputSchema());
        argumentHolder.put(CswConstants.CSW_MAPPING, configuration.getMetacardCswMappings());
        argumentHolder.put(CswConstants.AXIS_ORDER_PROPERTY, configuration.getCswAxisOrder());
        argumentHolder.put(Metacard.RESOURCE_URI, configuration.getResourceUriMapping());
        argumentHolder.put(Metacard.THUMBNAIL, configuration.getThumbnailMapping());
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        return CswRecordCollection.class.isAssignableFrom(type);
    }

    @Override
    public CswRecordCollection readFrom(Class<CswRecordCollection> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream inStream)
            throws IOException, WebApplicationException {

        CswRecordCollection cswRecords = null;

        // If the following http header exists and its value is true, the input stream will contain
        // raw product data
        List<String> productRetrievalHeader =
                httpHeaders.get(CswConstants.PRODUCT_RETRIEVAL_HTTP_HEADER);
        if (productRetrievalHeader != null) {
            if (productRetrievalHeader.get(0)
                    .equals("true")) {
                inStream = new ByteArrayInputStream(IOUtils.toByteArray(inStream));
                String fileName = handleContentDispositionHeader(httpHeaders.
                        get(HttpHeaders.CONTENT_DISPOSITION)
                        .get(0));
                cswRecords = new CswRecordCollection();
                cswRecords.setResource(new ResourceImpl(inStream, mediaType.toString(), fileName));
                return cswRecords;
            }
        }
        // Save original input stream for any exception message that might need to be
        // created
        String originalInputStream = IOUtils.toString(inStream, "UTF-8");
        LOGGER.debug("Converting to CswRecordCollection: \n {}", originalInputStream);

        // Re-create the input stream (since it has already been read for potential
        // exception message creation)
        inStream = new ByteArrayInputStream(originalInputStream.getBytes("UTF-8"));

        try {
            HierarchicalStreamReader reader = new XppReader(new InputStreamReader(inStream,
                    StandardCharsets.UTF_8),
                    XmlPullParserFactory.newInstance()
                            .newPullParser());
            cswRecords = (CswRecordCollection) xstream.unmarshal(reader, null, argumentHolder);
        } catch (XmlPullParserException e) {
            LOGGER.error("Unable to create XmlPullParser, and cannot parse CSW Response.", e);
        } catch (XStreamException e) {
            // If an ExceptionReport is sent from the remote CSW site it will be sent with an
            // JAX-RS "OK" status, hence the ErrorResponse exception mapper will not fire.
            // Instead the ExceptionReport will come here and be treated like a GetRecords
            // response, resulting in an XStreamException since ExceptionReport cannot be
            // unmarshalled. So this catch clause is responsible for catching that XStream
            // exception and creating a JAX-RS response containing the original stream
            // (with the ExceptionReport) and rethrowing it as a WebApplicatioNException,
            // which CXF will wrap as a ClientException that the CswSource catches, converts
            // to a CswException, and logs.
            ByteArrayInputStream bis = new ByteArrayInputStream(originalInputStream.getBytes(
                    StandardCharsets.UTF_8));
            ResponseBuilder responseBuilder = Response.ok(bis);
            responseBuilder.type("text/xml");
            Response response = responseBuilder.build();
            throw new WebApplicationException(e, response);
        } finally {
            IOUtils.closeQuietly(inStream);
        }
        return cswRecords;
    }

    /* Check Connection headers for filename */
    private String handleContentDispositionHeader(String contentDispositionHeader) {
        if (StringUtils.isNotBlank(contentDispositionHeader)) {
            ContentDisposition contentDisposition =
                    new ContentDisposition(contentDispositionHeader);
            String filename = contentDisposition.getParameter("filename");
            if (StringUtils.isNotBlank(filename)) {
                LOGGER.debug("Found content disposition header, changing resource name to {}",
                        filename);
                return filename;
            }
        }
        return "";
    }

}
