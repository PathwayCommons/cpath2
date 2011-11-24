package cpath.service;

import static org.junit.Assert.*;

import java.io.StringWriter;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.junit.Test;

import cpath.service.Status;
import cpath.service.jaxb.ErrorResponse;
import cpath.service.jaxb.Help;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;
import cpath.service.jaxb.ServiceResponse;
import cpath.service.jaxb.TraverseEntry;
import cpath.service.jaxb.TraverseResponse;

public class CPath2ServiceBeansTest {

	@Test
	public final void testGetAllStatusCodes() {
		List<String> list = Status.getAllStatusCodes();
		assertEquals(5, list.size());
	}

	@Test
	public final void testMarshal() {
		ErrorResponse e = Status.BAD_COMMAND.errorResponse(null);
		e.setErrorDetails("JUnit test!");
		String out = Status.marshal(e);
		assertTrue(out.length()>0);
		assertTrue(out.toLowerCase().contains("<error_msg>"));
//		System.out.println(out);
	}

	@Test
	public final void testMarshalServiceResponse() throws Exception {
		JAXBContext jaxbContext = JAXBContext.newInstance(Help.class, 
			ServiceResponse.class, ErrorResponse.class, 
			SearchHit.class, SearchResponse.class, 
			TraverseResponse.class, TraverseEntry.class);
		
		StringWriter writer = new StringWriter();
		Marshaller ma = jaxbContext.createMarshaller();
		ma.setProperty("jaxb.formatted.output", true);
		
		ServiceResponse s = Status.INTERNAL_ERROR.errorResponse("junit test!");
		ma.marshal(s, writer);
		String out = writer.toString();
		assertTrue(out.length()>0);
//		System.out.println(out);
		assertTrue(out.contains("<errorResponse"));
		
		SearchResponse sr = new SearchResponse();
		sr.setPageNo(0);
		writer = new StringWriter();
		ma.marshal(sr, writer);
		out = writer.toString();
		assertTrue(out.length()>0);
//		System.out.println(out);
		assertTrue(out.contains("<searchResponse"));
		
		TraverseResponse tr = new TraverseResponse();
		tr.setPropertyPath("test/path");
		writer = new StringWriter();
		ma.marshal(tr, writer);
		out = writer.toString();
		assertTrue(out.length()>0);
//		System.out.println(out);
		assertTrue(out.contains("<traverseResponse"));
	}
}
