package de.rkrempel.diss.harvesting.rdfwebaccesoirs;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import de.rkrempel.diss.core.harvester.DegreeCache;
import de.rkrempel.diss.core.harvester.LinkSource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * This class is proxy for The Retrival and also Filters lists and Concepts.... Because they tend tobe incomplete.
 */
public class DBPediapagelinksNOListsANDConcepts_Abstract implements LinkSource<URI> {
	String FusekiLink;
	private DegreeCache<URI> dc;

	public DBPediapagelinksNOListsANDConcepts_Abstract(String FusekiLink) {
		this.FusekiLink = FusekiLink;
		dc = new DegreeCache<URI>();
}	
	


	public List<URI> GetLinksforResourceNoListAndCategories(URI uRI, int passiveActive) throws URISyntaxException{
		List<URI> out = new ArrayList<URI>();
		int ressize = 0;
		Set<URI> tempout = new HashSet<URI>();
		if(passiveActive == 1 || passiveActive == 3 ){
			
			QueryExecution qexec = QueryExecutionFactory.sparqlService(FusekiLink, "SELECT DISTINCT ?other WHERE {<"+uRI.toString()+"> <http://dbpedia.org/ontology/wikiPageWikiLink> ?other . FILTER (!regex(str(?other), \"/Category:|CAT:|/List_of|/Lists_of\", \"i\"))}");
			ResultSet results = qexec.execSelect();
			for(;results.hasNext();){
			      QuerySolution soln = results.nextSolution() ;
			      RDFNode x = soln.get("other") ;
				  tempout.add(new URI(x.toString()));
				  ressize++;
			}
			
			dc.setDegree(uRI,ressize,1);
			qexec.close();



		} 
		if(passiveActive == 2 ||  passiveActive == 3 ){
			QueryExecution qexec = QueryExecutionFactory.sparqlService(FusekiLink, "SELECT DISTINCT ?other WHERE {  ?other <http://dbpedia.org/ontology/wikiPageWikiLink> <"+uRI.toString()+">. FILTER (!regex(str(?other), \"/Category:|CAT:|/List_of|/Lists_of\", \"i\"))} ");
			ResultSet results = qexec.execSelect();
			ressize =0;
			for(;results.hasNext();){
			      QuerySolution soln = results.nextSolution() ;
			      RDFNode x = soln.get("other") ;
				tempout.add(new URI(x.toString()));
				ressize++;
			}
			dc.setDegree(uRI,ressize,2);
			
			qexec.close();
		}
		out.addAll(tempout);
		if(passiveActive ==3 )
			dc.setDegree(uRI,out.size(),passiveActive);
		
		
		return out;
		
	}
	public Map<URI,Number> GetLinksforResourceNoListAndCategories_Weighted(URI uRI, int passiveActive) throws URISyntaxException{
		Map<URI,Number> out = new HashMap<URI,Number>();
		int ressize = 0;
		List<URI> tempout1 = new LinkedList<URI>();
		List<URI> tempout2 = new LinkedList<URI>();
		List<URI> tempout3 = new LinkedList<URI>();
		
		if(passiveActive == 1 || passiveActive == 3 ){
			
			QueryExecution qexec = QueryExecutionFactory.sparqlService(FusekiLink, "SELECT DISTINCT ?other WHERE {<"+uRI.toString()+"> <http://dbpedia.org/ontology/wikiPageWikiLink> ?other . FILTER (!regex(str(?other), \"/Category:|CAT:|/List_of|/Lists_of\", \"i\"))}");
			ResultSet results = qexec.execSelect();
			for(;results.hasNext();){
			      QuerySolution soln = results.nextSolution() ;
			      RDFNode x = soln.get("other") ;
				  tempout1.add(new URI(x.toString()));
				  ressize++;
			}
			
			dc.setDegree(uRI,ressize,1);
			qexec.close();



		} 
		if(passiveActive == 2 ||  passiveActive == 3 ){
			QueryExecution qexec = QueryExecutionFactory.sparqlService(FusekiLink, "SELECT DISTINCT ?other WHERE {  ?other <http://dbpedia.org/ontology/wikiPageWikiLink> <"+uRI.toString()+">. FILTER (!regex(str(?other), \"/Category:|CAT:|/List_of|/Lists_of\", \"i\"))} ");
			ResultSet results = qexec.execSelect();
			ressize =0;
			for(;results.hasNext();){
			      QuerySolution soln = results.nextSolution() ;
			      RDFNode x = soln.get("other") ;
			      URI temporaryURI=new URI(x.toString());
			      if(tempout1.contains(temporaryURI))
			    	  tempout3.add(temporaryURI);
			      else
			    	  tempout2.add(temporaryURI);
			    
				ressize++;
			}
			dc.setDegree(uRI,ressize,2);
			
			qexec.close();
		}
		for (int i = 0; i < tempout1.size(); i++) {
			out.put(tempout1.get(i),1);
			
		}
		for (int i = 0; i < tempout2.size(); i++) {
			out.put(tempout2.get(i),2);
			
		}
		for (int i = 0; i < tempout3.size(); i++) {
			out.put(tempout3.get(i),3);
		}
		
		
		if(passiveActive ==3 )
			dc.setDegree(uRI,out.size(),passiveActive);
		
		
		return out;
		
	}
	
	public void byebye(){
	}
	
	private boolean IsListOrCategory(String tocheck){
		
		if(tocheck.contains("/Category:") || tocheck.contains("CAT:") || tocheck.contains("List_of")|| tocheck.contains("/Lists_of"))
			return true;

		return false;
	}


	@Override
	public List<URI> GetLinksforResource(URI in, int passiveActive) {

		try {
			return GetLinksforResourceNoListAndCategories(in,passiveActive);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}


	@Override
	public int inDegree(URI getDegreefor) {
		Integer out = dc.getInDegree(getDegreefor);
		if(out==null)
			GetLinksforResource(getDegreefor, 1);
		return dc.getInDegree(getDegreefor);
		
	}


	@Override
	public int outDegree(URI getDegreefor) {
		Integer out = dc.getOutDegree(getDegreefor);
		if(out==null)
			GetLinksforResource(getDegreefor, 2);
		return dc.getOutDegree(getDegreefor);
	}
	@Override
	public int allDegree(URI getDegreefor) {
		Integer out = dc.getAllDegree(getDegreefor);
		if(out==null)
			GetLinksforResource(getDegreefor, 3);
		return dc.getAllDegree(getDegreefor);
	}

	@Override
	public void quit() {
		this.byebye();
		
	}
	@Override
	public boolean containsLinksFor(URI TheThingThatWeDontKnowifItHasLinks) {
		List<URI> temp = this.GetLinksforResource(TheThingThatWeDontKnowifItHasLinks, 3);
		
		if(temp.size()>0)
			return true;
		else
			return false;
	}



	@Override
	public Map<URI, Number> GetLinksforResourceWeighted(URI in,
			int passiveActive) {
		try {
			return GetLinksforResourceNoListAndCategories_Weighted(in,passiveActive);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
}
