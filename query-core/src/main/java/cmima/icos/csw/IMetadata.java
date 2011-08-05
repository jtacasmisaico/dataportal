/**
 * 
 */
package cmima.icos.csw;

import javax.xml.namespace.NamespaceContext;

import cmima.icos.utils.BBox;

/**
 * @author Micho Garcia
 *
 */
public interface IMetadata {
	
	NamespaceContext ctx = null;
	
	/**
	 * @return BBox 
	 */
	public BBox getExtent();
	
	/**
	 * @return array time coverage [star, end] (YYYY-MM-DDTHH:MM:SSZ)
	 */
	public String[] getTemporalExtent();
	
	/**
	 * @return array variables
	 */
	public String[] getVariables();
	
	/**
	 * @return metadata title
	 */
	public String getTitle();
	
	/**
	 * @return metadata summary
	 */
	public String getAbstract();
	
	/**
	 * @return metadata schema
	 */
	public String getSchema();

}