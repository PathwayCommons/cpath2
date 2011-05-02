/**
 * 
 */
package cpath.webservice;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.*;

/**
 * A bean for the help web service response.
 * 
 * @author rodche
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
public class Help {

	@NotNull
	private String id;
	private String title;
	private String info;
	private String example;
	private List<Help> members;
	
	
	public Help() {
		members = new ArrayList<Help>();
	}
	
	public Help(String id) {
		this();
		this.id = id;
	}
	
	
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}


	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}


	/**
	 * @return the info
	 */
	public String getInfo() {
		return info;
	}
	/**
	 * @param info the info to set
	 */
	public void setInfo(String info) {
		this.info = info;
	}
	/**
	 * @return the example
	 */
	public String getExample() {
		return example;
	}
	/**
	 * @param example the example to set
	 */
	public void setExample(String example) {
		this.example = example;
	}
	/**
	 * @return the members
	 */
	public Help[] getMembers() {
		return members.toArray(new Help[]{});
	}
	
	public void addMember(Help arg) {
		members.add(arg);
	}
	
	/**
	 * @param members the members to set
	 */
	public void setMembers(Help[] members) {
		this.members.clear();
		for(Help h : members) {
			this.members.add(h);
		}
	}

}
