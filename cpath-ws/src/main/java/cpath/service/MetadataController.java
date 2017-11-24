package cpath.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import javax.imageio.ImageIO;

import cpath.config.CPathSettings;
import cpath.jpa.Metadata;
import cpath.service.args.binding.MetadataTypeEditor;

import org.biopax.paxtools.normalizer.MiriamLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

/**
 * 
 * @author rodche
 */
@RestController
//@CrossOrigin //enabled, allowed *, get/post/head by default for a spring-boot app
public class MetadataController extends BasicController {
    
	private static final Logger log = LoggerFactory.getLogger(MetadataController.class);   

	@InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Metadata.METADATA_TYPE.class, new MetadataTypeEditor());
	}
    
    
    /*
     * Makes current cpath2 instance properties 
     * available to the JSP views.
     * @return
     */
    @ModelAttribute("cpath")
    public CPathSettings instance() {
    	return CPathSettings.getInstance();
    }


    @RequestMapping("/metadata/logo/{identifier}")
    public byte[] queryForLogo(@PathVariable String identifier)
    		throws IOException
    {	
    	Metadata ds = service.metadata().findByIdentifier(identifier);
    	byte[] bytes = null;
    	
    	if(ds != null) {
			BufferedImage bufferedImage = ImageIO
				.read(CPathUtils.LOADER.getResource(ds.getIconUrl()).getInputStream());
			
			//resize (originals are around 125X60)
			bufferedImage = scaleImage(bufferedImage, 100, 50, null);
						
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			ImageIO.write(bufferedImage, "gif", byteArrayOutputStream);
			bytes = byteArrayOutputStream.toByteArray();
		}
        
        return bytes;
    }

    
    // to return a xml or json data http response
    @RequestMapping("/metadata/datasources")
    public List<Metadata> queryForDatasources() {
		log.debug("Getting pathway datasources info.");
    	//pathway/interaction data sources
		List<Metadata> ds = new ArrayList<Metadata>();
		//warehouse data sources
		List<Metadata> wh = new ArrayList<Metadata>();
		
		for(Metadata m : service.metadata().findAll()) {
			//set dynamic extra fields
			if(m.isNotPathwayData()) {
				wh.add(m);
			} else {
				ds.add(m);
			}
		}
		
		//add warehouse data sources to the end of the list
		ds.addAll(wh);
		
    	return ds;
    }
    
    @RequestMapping("/metadata/datasources/{identifier}")
    public Metadata datasource(@PathVariable String identifier) {
		Metadata m = service.metadata().findByIdentifier(identifier);
		if(m==null)
			return null;
    	return m;
    }


    @RequestMapping("/miriam/uri/{db}/{id}")
    public String identifierOrgUri(@PathVariable String db, @PathVariable String id) {
        try {
			return MiriamLink.getIdentifiersOrgURI(db, id);
		}
		catch (IllegalArgumentException e) {
        	//not found
		}
		return null;
    }

    @RequestMapping("/miriam/url/{db}/{id}")
    public String[] miriamUrl(@PathVariable String db, @PathVariable String id) {
    	try {
			return MiriamLink.getLocations(db, id);
		}
		catch (IllegalArgumentException e) {
    		//not found
		}
		return null;
    }

}