/**
 * 
 */
package org.dataportal;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.dataportal.controllers.JPADownloadController;
import org.dataportal.controllers.JPASearchController;
import org.dataportal.csw.Catalog;
import org.dataportal.csw.Filter;
import org.dataportal.csw.GetRecordById;
import org.dataportal.csw.GetRecords;
import org.dataportal.csw.Operator;
import org.dataportal.csw.Property;
import org.dataportal.csw.SortBy;
import org.dataportal.model.Download;
import org.dataportal.model.DownloadItem;
import org.dataportal.model.Search;
import org.dataportal.utils.BBox;
import org.dataportal.utils.DataPortalException;
import org.dataportal.utils.Utils;

/**
 * Controller manage client petitions to server and manage responses from server
 * too. Use Xpath to extract information from XML request and responses and XSLT
 * to transform this to client
 * 
 * @author Micho Garcia
 * 
 */
public class QueryController extends DataPortalController {

	private static Logger logger = Logger.getLogger(QueryController.class);

	private static final String OR = "Or"; //$NON-NLS-1$
	private static final String AND = "And"; //$NON-NLS-1$
	private static final int FIRST = 0;

	/**
	 * Constructor. Assign URL catalog server
	 * 
	 * @throws MalformedURLException
	 */
	public QueryController(String lang) throws MalformedURLException {
		super();
		Messages.setLang(lang);
	}

	/**
	 * Receive the params from the client request and communicates these to
	 * CSWCatalog
	 * 
	 * @param parametros
	 * @return
	 * @throws Exception
	 */
	public String ask2gn(Map<String, String[]> parametros) throws Exception {

		InputStream isCswResponse = null;
		String response = ""; //$NON-NLS-1$

		// id
		if (parametros.get("id") != null) { //$NON-NLS-1$
			String ddi = parametros.get("id")[FIRST]; //$NON-NLS-1$
			response = getItemsDDI(ddi);
		} else {
			String aCSWQuery = createCSWQuery(parametros);

			isCswResponse = catalogo.sendCatalogRequest(aCSWQuery);
			response = transform(isCswResponse);
			isCswResponse.close();

			logger.debug("RESPONSE2CLIENT: " + response); //$NON-NLS-1$
		}

		return response;
	}

	private String getItemsDDI(String ddi) throws Exception {

		Download download = new Download(ddi);
		downloadJPAController = new JPADownloadController();
		ArrayList<DownloadItem> items = downloadJPAController
				.getDownloadItems(download);
		if (items.size() == 0) {
			dtException = new DataPortalException(Messages.getString("querycontroller.ddi_not_found")); //$NON-NLS-1$
			dtException.setCode(DDINOTFOUND);
			throw dtException;
		}
		ArrayList<String> isItems = new ArrayList<String>();
		for (DownloadItem item : items) {
			isItems.add(item.getItemId());
		}
		GetRecordById getRecordById = new GetRecordById(GetRecordById.BRIEF);
		getRecordById.setOutputSchema("csw:IsoRecord"); //$NON-NLS-1$
		String cswQuery = getRecordById.createQuery(isItems);
		InputStream catalogResponse = catalogo.sendCatalogRequest(cswQuery);

		String strCatalogResponse = transform(catalogResponse);

		logger.debug("GetRecordsById RESPONSE: " + strCatalogResponse); //$NON-NLS-1$

		return strCatalogResponse;
	}

	/**
	 * Transform the response from CSW Catalog into client response
	 * 
	 * @param isCswResponse
	 * @return String
	 * @throws TransformerException
	 * @throws IOException
	 * @throws DataPortalException
	 */
	private String transform(InputStream isCswResponse)
			throws TransformerException, IOException {

		StringWriter writer2Client = new StringWriter();
		InputStream isXslt = Catalog.class
				.getResourceAsStream("/response2client.xsl"); //$NON-NLS-1$

		Source responseSource = new StreamSource(isCswResponse);
		Source xsltSource = new StreamSource(isXslt);

		TransformerFactory transFact = TransformerFactory.newInstance();
		Templates template = transFact.newTemplates(xsltSource);
		Transformer transformer = template.newTransformer();

		transformer.transform(responseSource, new StreamResult(writer2Client));

		writer2Client.flush();
		writer2Client.close();

		isXslt.close();

		return writer2Client.toString();
	}

	/**
	 * Extract the params from a Map and create a Query in CSW 2.0.2 standard
	 * using GetRecords class
	 * 
	 * @param parametros
	 * @return String
	 * @throws XMLStreamException 
	 */
	private String createCSWQuery(Map<String, String[]> parametros) throws Exception {

		try {

			ArrayList<String> filterRules = new ArrayList<String>();
			GetRecords getrecords = new GetRecords();
			getrecords.setResulType("results"); //$NON-NLS-1$
			getrecords.setOutputSchema("csw:IsoRecord"); //$NON-NLS-1$
			getrecords.setTypeNames("gmd:MD_Metadata"); //$NON-NLS-1$
			getrecords.setElementSetName(GetRecords.FULL);
			
			Search search = new Search();
			if (getUser() != null)
				search.setUserBean(user);
			
			// bboxes
			Operator orBBox = new Operator(OR);

			String stringBBoxes = parametros.get("bboxes")[FIRST]; //$NON-NLS-1$
			ArrayList<BBox> bboxes = Utils.extractToBBoxes(stringBBoxes);
			if (bboxes != null) {
				if (bboxes.size() > 1) {
					ArrayList<String> rulesBBox = new ArrayList<String>();
					for (BBox bbox : bboxes) {
						rulesBBox.add(bbox.toOGCBBox());
					}
					orBBox.setRules(rulesBBox);
					filterRules.add(orBBox.getExpresion());
				} else {
					filterRules.add(bboxes.get(FIRST).toOGCBBox());
				}
				search.setBboxes(stringBBoxes);
			}

			// temporal range
			Property fromDate = null, toDate = null;

	        String start_date = parametros.get("start_date")[FIRST]; //$NON-NLS-1$
			if (start_date != "") { //$NON-NLS-1$
			    fromDate = new Property("PropertyIsGreaterThanOrEqualTo"); //$NON-NLS-1$
			    fromDate.setPropertyName("TempExtent_end"); //$NON-NLS-1$
			    // From the beginning of the day ('t' and 'z' must be lowercase)
			    fromDate.setLiteral(start_date+"t00:00:00.00z"); //$NON-NLS-1$
			}

            String end_date = parametros.get("end_date")[FIRST]; //$NON-NLS-1$
			if (end_date != "") { //$NON-NLS-1$
				toDate = new Property("PropertyIsLessThanOrEqualTo"); //$NON-NLS-1$
				toDate.setPropertyName("TempExtent_begin"); //$NON-NLS-1$
				 // To the end of the day ('t' and 'z' must be lowercase)
				toDate.setLiteral(end_date+"t23:59:59.99z"); //$NON-NLS-1$
			}
			
			if (fromDate != null && toDate != null) {
                ArrayList<String> dates = new ArrayList<String>(2);
                dates.add(fromDate.getExpresion());
                dates.add(toDate.getExpresion());
                Operator withinDates = new Operator("And"); //$NON-NLS-1$
                withinDates.setRules(dates);
                filterRules.add(withinDates.getExpresion());
                search.setStartDate(Utils.convertToDate(start_date));
                search.setEndDate(Utils.convertToDate(end_date));
			} else if (fromDate != null) {
			    filterRules.add(fromDate.getExpresion());
			    search.setStartDate(Utils.convertToDate(start_date));
			} else if (toDate != null) {
			    filterRules.add(toDate.getExpresion());
			    search.setEndDate(Utils.convertToDate(end_date));
			}
			
			// variables
			String variables = parametros.get("variables")[FIRST]; //$NON-NLS-1$
			if (variables != "") { //$NON-NLS-1$
				String queryVariables[] = variables.split(","); //$NON-NLS-1$

				if (queryVariables.length > 1) {
					Operator orVariables = new Operator("Or"); //$NON-NLS-1$
					ArrayList<String> arrayVariables = new ArrayList<String>();
					for (String aVariable : queryVariables) {
						Property propVariable = new Property("PropertyIsLike"); //$NON-NLS-1$
						propVariable.setPropertyName("ContentInfo"); //$NON-NLS-1$
						propVariable.setLiteral(aVariable);
						arrayVariables.add(propVariable.getExpresion());
					}
					orVariables.setRules(arrayVariables);
					filterRules.add(orVariables.getExpresion());
				} else {
					Property propVariable = new Property("PropertyIsLike"); //$NON-NLS-1$
					propVariable.setPropertyName("ContentInfo"); //$NON-NLS-1$
					propVariable.setLiteral(queryVariables[FIRST]);
					filterRules.add(propVariable.getExpresion());
				}
				search.setVariables(variables);
			}

			// free text
			String freeText = parametros.get("text")[FIRST]; //$NON-NLS-1$
			if (freeText != "") { //$NON-NLS-1$
				Property propFreeText = new Property("PropertyIsLike"); //$NON-NLS-1$
				propFreeText.setPropertyName("AnyText"); //$NON-NLS-1$
				propFreeText.setLiteral(freeText);
				filterRules.add(propFreeText.getExpresion());
				search.setText(freeText);
			}

			// Default pagination & ordering values
			String startPosition = "1"; //$NON-NLS-1$
			String maxRecords = "25"; //$NON-NLS-1$
			String sort = "title"; //$NON-NLS-1$
			String dir = "asc"; //$NON-NLS-1$

			startPosition = String.valueOf(Integer.valueOf(parametros
					.get("start")[FIRST]) + 1); //$NON-NLS-1$
			getrecords.setStartPosition(startPosition);

			maxRecords = parametros.get("limit")[FIRST]; //$NON-NLS-1$
			getrecords.setMaxRecords(maxRecords);

			sort = parametros.get("sort")[FIRST]; //$NON-NLS-1$
			dir = parametros.get("dir")[FIRST]; //$NON-NLS-1$

			Map<String, String> sortPropertyDict = new HashMap<String, String>();
			sortPropertyDict.put("id", "Identifier"); //$NON-NLS-1$ //$NON-NLS-2$
			sortPropertyDict.put("title", "Title"); //$NON-NLS-1$ //$NON-NLS-2$
			sortPropertyDict.put("start_time", "TempExtent_begin"); //$NON-NLS-1$ //$NON-NLS-2$
			sortPropertyDict.put("end_time", "TempExtent_end"); //$NON-NLS-1$ //$NON-NLS-2$

			SortBy sortby = new SortBy();
			sortby.setPropertyName(sortPropertyDict.get(sort));
			sortby.setOrder(dir);

			getrecords.setSortby(sortby);
			
			Filter filtro = new Filter();
			
			if (filterRules.size() > 1) {
				Operator and = new Operator(AND);
				and.setRules(filterRules);
				ArrayList<String> andRules = new ArrayList<String>(1);
				andRules.add(and.getExpresion());
				filtro.setRules(andRules);
			} else {
				filtro.setRules(filterRules);
			}			

			getrecords.setFilter(filtro);

			logger.debug(getrecords.getExpresion());
			
			searchJPAController = new JPASearchController();
			search.setTimestamp(Utils.extractDateSystemTimeStamp());
			searchJPAController.insert(search);

			return getrecords.getExpresion();

		} catch (XMLStreamException e) {
			logger.error(e.getMessage());
			throw e;
		}
	}
}
