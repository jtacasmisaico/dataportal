/**
 * 
 */
package test.dataportal.csw;

import java.util.ArrayList;

import javax.xml.stream.XMLStreamException;

import org.dataportal.csw.Operator;
import org.dataportal.csw.DataPortalNS;
import org.dataportal.csw.Filter;
import org.dataportal.csw.Property;
import org.dataportal.csw.SortBy;

import junit.framework.TestCase;

/**
 * @author Micho Garcia
 *
 */
public class FilterTest extends TestCase {

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
	}
	
	/**
	 * Test method for {@link org.dataportal.csw.Filter#getExpresion()}.
	 */
	public void testGetExpresion() {
		
		// TODO funcionality only launch the Filter
		
		Filter filtro = new Filter();
		
		// creating rules
		Property mayorque = new Property("PropertyIsLike");	
		Property menorque = new Property("PropertyMinorThan");
		
		try {
			ArrayList<String> rules = new ArrayList<String>();

			mayorque.setLiteral("un valor");
			mayorque.setPropertyName("una propiedad");	
			rules.add(mayorque.getExpresion());
			menorque.setLiteral("valor menor");
			menorque.setPropertyName("propiedad menor");
			rules.add(menorque.getExpresion());
			Operator and = new Operator("And");
			and.setRules(rules);
			
			SortBy sortby = new SortBy();
			sortby.setPropertyName("title");
			sortby.setOrder(SortBy.ASC);
			sortby.getExpresion();
			
			ArrayList<String> filterRules = new ArrayList<String>();
			filterRules.add(and.getExpresion());
			
			filtro.setRules(filterRules);
			filtro.getExpresion();
			
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
		//fail("Not yet implemented");
	}

}
