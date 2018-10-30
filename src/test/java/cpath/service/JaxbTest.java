package cpath.service;

import static org.junit.Assert.*;

import java.io.StringWriter;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import cpath.service.api.Status;
import org.junit.Test;

import cpath.service.jaxb.SearchResponse;
import cpath.service.jaxb.TraverseEntry;
import cpath.service.jaxb.TraverseResponse;

public class JaxbTest {

	@Test
	public final void testGetAllStatusCodes() {
		List<String> list = Status.getAllStatusCodes();
		assertEquals(4, list.size());
	}

	@Test
	public final void testMarshalServiceResponse() throws Exception {
		JAXBContext jaxbContext = JAXBContext.newInstance(
			SearchResponse.class, TraverseResponse.class, TraverseEntry.class);

		Marshaller ma = jaxbContext.createMarshaller();
		ma.setProperty("jaxb.formatted.output", true);

		SearchResponse sr = new SearchResponse();
		sr.setPageNo(0);
		StringWriter writer = new StringWriter();
		ma.marshal(sr, writer);
		String out = writer.toString();
		assertTrue(out.length()>0);
		assertTrue(out.contains("searchResponse"));
		
		TraverseResponse tr = new TraverseResponse();
		tr.setPropertyPath("test/path");
		writer = new StringWriter();
		ma.marshal(tr, writer);
		out = writer.toString();
		assertTrue(out.length()>0);
		assertTrue(out.contains("traverseResponse"));
	}
}
