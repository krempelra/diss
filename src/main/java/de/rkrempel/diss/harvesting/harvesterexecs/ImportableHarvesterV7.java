package de.rkrempel.diss.harvesting.harvesterexecs;

import de.rkrempel.diss.core.commontools.GraphContainer;
import de.rkrempel.diss.core.commontools.URIModifier;
import de.rkrempel.diss.core.harvester.ClassSource;
import de.rkrempel.diss.core.harvester.LinkSource;
import de.rkrempel.diss.core.harvester.MultipointContextHarvesterforWeb_Parallel_classes;
import de.rkrempel.diss.core.harvester.ToHumanReadableStringConverter;
import de.rkrempel.diss.core.report.FilterReportWriter;
import de.rkrempel.diss.harvesting.rdfwebaccesoirs.DBPediapagelinksNOListsANDConcepts_Abstract;
import de.rkrempel.diss.harvesting.rdfwebaccesoirs.DBpediaClassing;
import de.rkrempel.diss.layout.dbpediadata.DBPediacontextLayoutScriptWebView3;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This Should be The Seventh! Implementation of the harvester as a tool you can run from Command line For the use as Command line Tool
 * This Tool uses the DBpedia Links Provided by a Fuseki Store
 * See "5.3 LWMap: eine Perspektive auf Zusammenhang"
 */
public class ImportableHarvesterV7{
	static String name ="Wiki Importable Harvester V1.0.0";
	/**
	 * @param args index 0 Input File(List URIs) , index 1 The Web Location of SPARQL ENDPoint, index 2 OutputFile , Prefix for TDB
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		Set<URI> DBPeadiaSourceArticles = new HashSet<URI>();
		List<String> DBPediaLinks = new ArrayList<String>();
		String Prefix= new String("http://dbpedia.org/resource/");

		//Get Data From File!
		if(args !=null && args.length >0 &&  args[0].length()>4 && args[1].length()>4 && args[2].length()>4  ){
			
			if(args[3].length()>4)
				Prefix=args[3];
			FileReader freader = new FileReader(args[0]);
			BufferedReader in
			   = new BufferedReader(freader);
			String strLine;
			boolean first =true;
			boolean addprefix = false;
			while ((strLine = in.readLine()) != null)   {
					
					//Check for Prefix
					if(first){
						if(!strLine.contains(Prefix))
							addprefix = true;
						first = false;
					}
					//addTo set
					if(addprefix)
						DBPediaLinks.add(Prefix+strLine);
					else
						DBPediaLinks.add(strLine);
				  }
			
			in.close();
		}else{ 
			System.out.println("args:\n" +
					"index 0 Input File (List URIs) , \n" +
					"index 1 The Web Location of SPARQL ENDPoint, Links \n" +
					"index 2 OutputFile , \n" +
					"index 3 Prefix for TDB \n");
			
			return;}
			
		String Filename = args[0].subSequence(args[0].lastIndexOf("/")+1,args[0].lastIndexOf(".")).toString();
		String Path = args[2].subSequence(0,args[2].lastIndexOf("/")+1).toString();
		
		if(FilterReportWriter.getInstance().setActiveReportFile(Path+"report_"+Filename))
			System.out.println("Did not work the Report Writer");
		FilterReportWriter.getInstance().appendReportEvent("File Hash: "+Filename);
		FilterReportWriter.getInstance().appendTechEvent("ExeccutableID: "+name);
		FilterReportWriter.getInstance().appendActiveEvent("ExeccutableID: "+name);
		FilterReportWriter.getInstance().appendActiveEvent("File Hash: "+Filename);
		/*
		 *------------------ Passive OR Active Links--------------------
		 */
		for (String string : DBPediaLinks) {

			try {
				
				DBPeadiaSourceArticles.add(new URI(string));
			} catch (Exception e) {
				e.printStackTrace();
			}

			
		}
		//Writing report to Public
		String publicPath = args[2].subSequence(0,args[2].lastIndexOf("/")+1).toString();
		
		LinkSource<URI> linkSource= new DBPediapagelinksNOListsANDConcepts_Abstract(args[1]);
		//TODO Well this is quick and Dirty hack... ... ... This is a hard coded link...  was in a Hurry i suspect..
		ClassSource<URI,String> classifier = new DBpediaClassing("http://localhost:3030/classes/query",true);
		ToHumanReadableStringConverter<URI> uRIModifier= new URIModifier(Prefix);
	
		
		MultipointContextHarvesterforWeb_Parallel_classes<URI> mCH = new MultipointContextHarvesterforWeb_Parallel_classes<URI>(DBPeadiaSourceArticles,linkSource,3,classifier);
		
		mCH.setToHumanReadableStringConverter(uRIModifier);
		
		long start_time = System.nanoTime();
		
		mCH.Harvest();
		long end_time = System.nanoTime();
		double difference = (end_time - start_time)/1e6;
		FilterReportWriter.getInstance().appendTechEvent("Harvesting Time :"+difference);
		
		linkSource.quit();

		start_time = System.nanoTime();
		DBPediacontextLayoutScriptWebView3 dblay = new DBPediacontextLayoutScriptWebView3();
		GraphContainer temp = mCH.getGraphContainer();
		FilterReportWriter.getInstance().appendTechEvent("Edges:"+temp.getEdges().size());
		FilterReportWriter.getInstance().appendTechEvent("nodes:"+temp.getNodes().size());
		
		FilterReportWriter.getInstance().appendActiveEvent("Edges:"+temp.getEdges().size());
		FilterReportWriter.getInstance().appendActiveEvent("nodes:"+temp.getNodes().size());
		
		String result = dblay.script(temp,args[2]);
		int startDesc = result.indexOf("</description>");
		StringBuffer out = new StringBuffer(result.substring(0, startDesc));
		out.append("License: CC BY-SA 3.0 http://creativecommons.org/licenses/by-sa/3.0/ \n ");
		out.append("Created by script "+name+" created by Rasmus Krempel \n");
		out.append("Depends on Data of DBpedia, see http://dbpedia.org/About \n");
		out.append(FilterReportWriter.getInstance().getActiveReport());
		out.append(result.substring(startDesc));
		FileWriter file = null;
        try {
        	file =new FileWriter(args[2]+".gexf");
        	BufferedWriter buff = new BufferedWriter(file);
        	buff.write(out.toString());
        	buff.close();
            out = null;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
		
		end_time = System.nanoTime();
		difference = (end_time - start_time)/1e6;
		FilterReportWriter.getInstance().appendTechEvent("Layout and Output Time :"+difference);
		//TODO writeReport to Doku
		
		return;
		
		
	}


}
