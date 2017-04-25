package de.rkrempel.diss.harvesting.rdfwebaccesoirs;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import de.rkrempel.diss.core.harvester.ClassSource;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Retrieves Classes.
 */
public class DBpediaClassing implements ClassSource<URI, String> {
	protected String FusekiLink;
	protected boolean castrate =false;
	public DBpediaClassing(String FusekiLink) {
		this.FusekiLink = FusekiLink;
	}
	
	public DBpediaClassing(String FusekiLink, boolean OnlyEnd) {
		this.FusekiLink = FusekiLink;
		this.castrate= OnlyEnd;
	}
	
	public List<String> getClasses(URI ToClassify) {
		Set<String> out = new HashSet<String>();
		QueryExecution qexec = QueryExecutionFactory.sparqlService(FusekiLink, "SELECT ?type WHERE {<"+ToClassify.toString()+"> a ?type}");
		ResultSet results = qexec.execSelect();
		for(;results.hasNext();){
		      QuerySolution soln = results.nextSolution() ;
		      RDFNode x = soln.get("type") ;
		      if(x.isURIResource()){
		    	  String temp =x.asResource().getURI();
		    	  if(castrate)
		    		  temp = temp.substring(temp.lastIndexOf("/")+1 ); 
		    	  if(!temp.equals("owl#Thing"))
		    		  out.add(temp );
		      }
		}
		
		qexec.close();
		
		
		return new ArrayList<String>(out);
		
		
	};
	
	
}
