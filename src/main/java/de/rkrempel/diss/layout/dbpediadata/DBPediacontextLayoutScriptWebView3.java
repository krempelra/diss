/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.rkrempel.diss.layout.dbpediadata;

import de.rkrempel.diss.core.commontools.CooccuredMetaEdge;
import de.rkrempel.diss.core.commontools.GraphContainer;
import de.rkrempel.diss.core.commontools.TwoModeToOneModeNetworkConverter;
import de.rkrempel.diss.core.converters.GraphContainerToGephi;
import de.rkrempel.diss.core.report.FilterReportWriter;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.filters.api.FilterController;
import org.gephi.filters.api.Query;
import org.gephi.filters.plugin.attribute.AttributeEqualBuilder.EqualNumberFilter;
import org.gephi.graph.api.*;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.exporter.plugin.ExporterGEXF;
import org.gephi.io.exporter.spi.CharacterExporter;
import org.gephi.layout.plugin.AutoLayout;
import org.gephi.layout.plugin.circularlayout.radialaxislayout.RadialAxisLayout;
import org.gephi.layout.plugin.circularlayout.radialaxislayout.RadialAxisLayoutBuilder;
import org.gephi.layout.plugin.forceAtlas.ForceAtlasLayout;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2Builder;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.ranking.api.Ranking;
import org.gephi.ranking.api.RankingController;
import org.gephi.ranking.api.Transformer;
import org.gephi.ranking.plugin.transformer.AbstractColorTransformer;
import org.gephi.ranking.plugin.transformer.AbstractSizeTransformer;
import org.openide.util.Lookup;

import java.awt.*;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 *This will be The runable Class for the WEB Harvesting
 * Compile as Exexcutable packed jar for usage
 * See "5.3 LWMap: eine Perspektive auf Zusammenhang"
 */
public class DBPediacontextLayoutScriptWebView3 extends AbstractLayout {

       private Workspace workspace;
       private AttributeModel attributeModel;
       private GraphModel graphModel;
       private List<Edge> TempEdges;
       private int contextNodeSize =0;
       private int contextEdgeSize= 0;
       private int sourceNodeSize=0;
       private int sourceEdgeSize=0;
       private static boolean verbose = false;
       private int LayoutThreadCount=4;
       private List<CooccuredMetaEdge> metas;
       
       //THE SETTINGS

       private static Color SourceNodeMaxColor = new Color(0x001EB8); 
       private static Color SourceNodeMinColor = new Color(0x00A6FF); 
       private static Color ContextNodeMaxColor = new Color(0xB80000); 
       private static Color ContextNodeMinColor = new Color(0xFFDA6D); 
       private static Color FilterEdgeColor = new Color(0x111111);
       
    public String script(GraphContainer gc, String writeto){
        /*
         * Vorraussetzungen
         * 
         * Node Attribut  IsContext 1 (Kontextnodes)
         * Node Attribut  IsContext 0 (Sourcenode)
         * 
         * Node Attribut OUTDegreeInTotalGraph  Links des Artikels zu Anderen Artikeln
         * Node Attribut INDegreeInTotalGraph  Links Anderen Artikeln zu diesem Artikel
         * Node Attribut SourcenodeRanking Prozentsatz der Sourcenodes die Gerankt werden
         * 
         * Edge Attribut is ConnectionType 1 (zwischen Sourcenodes)(Wert 0)
         * Edge Attribut is ConnectionType 2 (zwischen Sourcenodes und Contextnodes)(Wert 1)
         * Edge Attribut is ConnectionType 3 (zwischen Contextnodes)(Wert 2)
         * 
         */
        
    	TempEdges = new ArrayList<Edge>();
        
        //Ablauf
        
        //Essentials
        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();
        
        workspace = pc.getCurrentWorkspace();
        graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
        
        attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();

        ///Load DATA

        GraphContainerToGephi.GraphtoGephi(gc, graphModel);
        GraphView mainView = graphModel.getVisibleView();

        EdgeIterator edgeIt = graphModel.getGraphVisible().getEdges().iterator();
        boolean hasct0 = false;
        boolean hasct1 = false;
        boolean hasct2 = false;
        
        for (;edgeIt.hasNext();) {
        	if(hasct0 && hasct1&&hasct2)
        		break;
			Edge e = edgeIt.next();
			Integer ct =(Integer) e.getAttributes().getValue("ConnectionType");
			if(ct == 0){
				hasct0=true;
			}
			else if(ct == 1){
				hasct1=true;
			}else if(ct == 2){
				hasct1=true;
			}
			
		}
        if(!hasct1)
        	return "";
        
        ///////////////////Detect Unconnected
        
        NodeIterator nodeIt = graphModel.getGraphVisible().getNodes().iterator();
        List<Node> toremove= new ArrayList<Node>(3);
        for (;nodeIt.hasNext();) {
        	
        	Node temp = nodeIt.next();
        	
        	if(0==graphModel.getGraphVisible().getDegree(temp)){
        		
        		toremove.add(temp);
        	}
        	
        }
        
        for (Node node : toremove) {
    		FilterReportWriter.getInstance().appendActiveEvent("Removed unconnected: "+node.getNodeData().getLabel() );

        	graphModel.getGraph().removeNode(node);
        	
		}
        writeToSystemout("Ursprung");
        writeToSystemout("EdgeCount :"+graphModel.getGraphVisible().getEdgeCount());
        writeToSystemout("NodeCount :"+graphModel.getGraphVisible().getNodeCount());
        //Filter
        
        FilterController filterController = Lookup.getDefault().lookup(FilterController.class);
        
        
        EqualNumberFilter contextFilter = new EqualNumberFilter(this.getAttColumbyString("IsContext"));
        contextFilter.init(graphModel.getGraph());
        
        AttributeColumn coltemp = attributeModel.getEdgeTable().getColumn("ConnectionType");
       
        EqualNumberFilter edgeTypeFilter = new EqualNumberFilter(coltemp);
        edgeTypeFilter.init(graphModel.getGraph());
        
        Query query= null;
        GraphView view = null;
        
        //
        //********************************Reweight All
        //
        
		//writeToSystemout("Reweight ");
        
        
        //
        //*********************************** Node size
        //
        //checkNANPos();
        

        
        
        writeToSystemout("Knoten Größe ");
        // Knoten Größe Kontext knoten
        RankingController rankingController = layoutSizeContextNodes(
				filterController, contextFilter);
        
        
        
        
        //Knoten Größe  Source Nodes
        AttributeColumn inDegreeInTotalGraphColumn = layoutSizeSourceNodes(
				filterController, contextFilter, rankingController);
        
        
        //checkNANPos();
        //
        //*************************************** Node color
        //
        writeToSystemout("Knoten Farbe ");
        layoutNodeColor(filterController, contextFilter, rankingController,
				inDegreeInTotalGraphColumn);
        
        //INIT  POSITIONING
        graphModel.setVisibleView(mainView);
        
        //
        //*************************************** Kanten Farbe 
        //
        
        //TODO Idea Make Edge Color!
        writeToSystemout("Kanten Farbe ");
        layoutEdgeColor(filterController, rankingController,mainView);
        
        //setSomePos();
        //
        //***************************************Positionierung der Ausgangsknoten
        //
        writeToSystemout("Positionierung der Ausgangsknoten ");

		layoutPositionStartNodes(mainView, hasct0,
				filterController, contextFilter);
        
        
        //
        //********************************Positionierung Kontext
        //
		
		//Kopiert die richtigen kanten gewichte Rein
		this.copytoEdgeWeight("RankContextLowSourcesHigh");
		
		ReweightandPostprocessing(mainView,filterController); 
		
		writeToSystemout("Positionierung Kontext ");
        layoutPositionContext(mainView); 
        
        
        //
        //********************************Export
        //
        graphModel.setVisibleView(mainView);
        
        AttributeColumn[] NodeAttColums = attributeModel.getNodeTable().getColumns();
        
        Set<String> nodeArrtNoDeleteFilter = new HashSet<String>();
        
        nodeArrtNoDeleteFilter.add("IsContext");
        nodeArrtNoDeleteFilter.add("Classes");
        nodeArrtNoDeleteFilter.add("Id");
        nodeArrtNoDeleteFilter.add("Weight");
        nodeArrtNoDeleteFilter.add("Label");
        nodeArrtNoDeleteFilter.add("HiddenContext");
        
        for (int i = 0; i < NodeAttColums.length; i++) {

        	//System.out.println(NodeAttColums[i].getTitle());
        	if(nodeArrtNoDeleteFilter.contains( NodeAttColums[i].getTitle()) )
        		continue;
        	attributeModel.getNodeTable().removeColumn(NodeAttColums[i]); 
		}
        
        AttributeColumn[] EdgeAttColums = attributeModel.getEdgeTable().getColumns();
        
        Set<String> edgeArrtNoDeleteFilter = new HashSet<String>();
        
        edgeArrtNoDeleteFilter.add("Id");
        edgeArrtNoDeleteFilter.add("Weight");
        edgeArrtNoDeleteFilter.add("Label");
        edgeArrtNoDeleteFilter.add("ConnectionType");

        for (int i = 0; i < EdgeAttColums.length; i++) {
        	
        	if(edgeArrtNoDeleteFilter.contains( EdgeAttColums[i].getTitle()) )
        		continue;
        	attributeModel.getEdgeTable().removeColumn(EdgeAttColums[i]); 
		}
               
        writeToSystemout("Export Start");
        
        //Export
        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        ExporterGEXF fe = new ExporterGEXF();
        //Attribute Rausnehmen (weniger übertragung)
        //fe.setExportAttributes(false);
        
        CharacterExporter characterExporter = (CharacterExporter)fe;
        StringWriter stringWriter = new StringWriter();
        ec.exportWriter(stringWriter, characterExporter);
        String result = stringWriter.toString();
                

        writeToSystemout("Export End");
        graphModel.clear();
        
        pc.closeCurrentProject();
        return result;

  
               
    }
    
    /*
    //********************Force Atlas 2 Cheat Sheet **********
     * 
     *
    Scaling: How much repulsion you want. More makes a more sparse graph.
    Gravity: Attracts nodes to the center. Prevents islands from drifting away.
    Dissuade Hubs: Distributes attraction along outbound edges. Hubs attract less and thus are pushed to the borders.
    LinLog mode: Switch ForceAtlas’ model from lin-lin to lin-log (tribute to Andreas Noack). Makes clusters more tight.
    Prevent Overlap: Use only when spatialized. Should not be used with “Approximate Repulsion”
    Tolerance (speed): How much swinging you allow. Above 1 discouraged. Lower gives less speed and more precision.
    Approximate Repulsion: Barnes Hut optimization: n² complexity to n.ln(n) ; allows larger graphs.
    Approximation: Theta of the Barnes Hut optimization.
    Edge Weight Influence: How much influence you give to the edges weight. 0 is “no influence” and 1 is “normal”.

    */

	private void ReweightandPostprocessing(GraphView mainView,FilterController filterController) {
		EqualNumberFilter edgeTypeFilter;
		
		Query query;
		GraphView view;
		AttributeColumn coltemp;
		coltemp = attributeModel.getEdgeTable().getColumn("ConnectionType");
	    
		edgeTypeFilter = new EqualNumberFilter(coltemp);
		edgeTypeFilter.init(graphModel.getGraph());
		EdgeIterable visedges;
	    //Edgeweight Type 1
	    
	    edgeTypeFilter.setMatch(new Integer(1));
	    query = filterController.createQuery(edgeTypeFilter);
	    view = filterController.filter(query);
	    graphModel.setVisibleView(view); 
	    
	    visedges = graphModel.getGraphVisible().getEdges();
	    Double SourceModelWeight=0d;
	    for (Edge edge : visedges) {
	    	Float weight = edge.getWeight();
	    	SourceModelWeight= SourceModelWeight+weight;

	    }
	    
	    
	    graphModel.setVisibleView(mainView);
	    
	    //Edgeweight Type 2
	    edgeTypeFilter.setMatch(new Integer(2));
	    query = filterController.createQuery(edgeTypeFilter);
	    view = filterController.filter(query);
	    graphModel.setVisibleView(view); 
	    
	    visedges = graphModel.getGraphVisible().getEdges();
	    Double ContextModelWeight=0d;
	    Edge[] visEdgeArray = visedges.toArray();
	    for (Edge edge : visEdgeArray) {
	    	Float weight = edge.getWeight();
	    	ContextModelWeight= ContextModelWeight+weight;
	    }
	 
	    Double ContextSource =  ContextModelWeight/SourceModelWeight; 
	    //visedges = graphModel.getGraphVisible().getEdges();
	    System.out.println("Weight Ratio"+ContextSource);
	    visedges = null;
	    graphModel.setVisibleView(mainView);
	    
	    //Edgeweight Type 2
	    edgeTypeFilter.setMatch(new Integer(2));
	    query = filterController.createQuery(edgeTypeFilter);
	    view = filterController.filter(query);
	    graphModel.setVisibleView(view); 
	    visedges = graphModel.getGraphVisible().getEdges();
	    
	    //Normalisie wheigt to 1
	    if(1<ContextSource){
	    	
		    for (Edge edge : visedges) {
		    	
		    	Float weight = edge.getWeight();
		    	edge.setWeight( weight/ContextSource.floatValue() );
		    	edge.getAttributes().setValue("weight",weight/ContextSource.floatValue() );

		    		
		    }	
	    }
	    
	    
	    graphModel.setVisibleView(mainView);
	    
		
	}

	private void layoutEdgeColor(FilterController filterController,
			RankingController rankingController, GraphView mainView) {
	EqualNumberFilter edgeTypeFilter;
		
	Query query;
	GraphView view;
	AttributeColumn coltemp;
	coltemp = attributeModel.getEdgeTable().getColumn("ConnectionType");
    
	edgeTypeFilter = new EqualNumberFilter(coltemp);
	edgeTypeFilter.init(graphModel.getGraph());
    //COLOR Type 0
    edgeTypeFilter.setMatch(new Integer(0));
    query = filterController.createQuery(edgeTypeFilter);
    view = filterController.filter(query);
    graphModel.setVisibleView(view); 

    
    EdgeIterable visedges = graphModel.getGraphVisible().getEdges();
    // Weiß Zwischen den Ausgangsknoten
    for (Edge edge : visedges) {
    	edge.getEdgeData().setColor(0.f, 0.f, 0.f);
    }
    graphModel.setVisibleView(mainView);
    //COLOR TYPE 1
    edgeTypeFilter.setMatch(new Integer(1));
    query = filterController.createQuery(edgeTypeFilter);
    view = filterController.filter(query);
    graphModel.setVisibleView(view); 
    //COLORIZE-> Copy color Topic node
    visedges = graphModel.getGraphVisible().getEdges();
    for (Edge edge : visedges) {
    	NodeData temp = edge.getSource().getNodeData();
    	edge.getEdgeData().setColor(temp.r(), temp.g(), temp.b());
    }
    
    graphModel.setVisibleView(mainView);
    //COLOR TYPE 2
    
    edgeTypeFilter.setMatch(new Integer(2));
    query = filterController.createQuery(edgeTypeFilter);
    view = filterController.filter(query);
    graphModel.setVisibleView(view); 
    visedges = graphModel.getGraphVisible().getEdges();
    for (Edge edge : visedges) {
    	NodeData temp = edge.getSource().getNodeData();
    	NodeData temp2 = edge.getTarget().getNodeData();
    	Color temppp = new Color(0xFEF0D9);
    	float[] components = temppp.getColorComponents(new float[3]);

    	edge.getEdgeData().setColor((temp.r()+temp2.r()+components[0])/3, (temp.g()+temp2.g()+components[1])/3, (temp.b()+temp2.b()+components[2])/2);
    	
    }
    graphModel.setVisibleView(mainView);
    
    
	}

	private void layoutPositionStartNodes(GraphView mainView,
			boolean hasct0, FilterController filterController,
			EqualNumberFilter contextFilter) {
		AttributeColumn coltemp;
		EqualNumberFilter edgeTypeFilter;
		Query query;
		GraphView view;

        //Remove Edges
        
        if(hasct0){
        	attributeModel.getEdgeTable().getColumn("ConnectionType");
        
        
        	coltemp = attributeModel.getEdgeTable().getColumn("ConnectionType");
        	edgeTypeFilter = new EqualNumberFilter(coltemp);
        	edgeTypeFilter.init(graphModel.getGraph());
        
        	edgeTypeFilter.setMatch(new Integer(0));
        	query = filterController.createQuery(edgeTypeFilter);
        
        	view = filterController.filter(query);
        
        	graphModel.setVisibleView(view);    //Set the filter result as the visible view
        	writeToSystemout("Filtered Edges Only Typ 0");
        	writeToSystemout("EdgeCount :"+graphModel.getGraphVisible().getEdgeCount());
        	writeToSystemout("NodeCount :"+graphModel.getGraphVisible().getNodeCount());
        
        
        	RemoveAndSaveVisibleEdges();
        }
        //Get Create Coocnet

                graphModel.setVisibleView(mainView);
        
        
        coltemp = attributeModel.getEdgeTable().getColumn("ConnectionType");
        
        edgeTypeFilter = new EqualNumberFilter(coltemp);
        edgeTypeFilter.init(graphModel.getGraph());
        
        edgeTypeFilter.setMatch(new Integer(1));
        query = filterController.createQuery(edgeTypeFilter);
        view = filterController.filter(query);
        graphModel.setVisibleView(view); 
        writeToSystemout("Filtered Edges Only Type 1");
        writeToSystemout("EdgeCount :"+graphModel.getGraphVisible().getEdgeCount());
        writeToSystemout("NodeCount :"+graphModel.getGraphVisible().getNodeCount());
        List<Long[]> CoccEdges = new ArrayList<Long[]>();
        
        
        EdgeIterable visedges = graphModel.getGraphVisible().getEdges();
        for (Edge object : visedges) {
        	Long[] Coocedge = new Long[2]; 
        	
        	if(object.getSource().getAttributes().getValue("IsContext").equals(1) ){
        		//object.getSource().getId();
        		Coocedge[1] =new Long(object.getSource().getId());
        		Coocedge[0] =new Long(object.getTarget().getId());
        	}else{
        		Coocedge[0] =new Long(object.getSource().getId());
        		Coocedge[1] =new Long(object.getTarget().getId());
        	}
        	CoccEdges.add(Coocedge);
        	
        }
        
        TwoModeToOneModeNetworkConverter twotoONE = new TwoModeToOneModeNetworkConverter();
        
        List<Object[]> coocedges = twotoONE.makeCTEdgesAllWeights(CoccEdges);
        
        //Maximal Gewicht Finden + Meta Edges Addieren
        //Find maximun Wheight + addup meta edges
        Float MaxSourceCoocEdgeWeight =new Float(0);
        // TODO to settings
        float MaximumEdgeWeight = 40.0f;
        float MinimumEdgeWeight = 5.0f;
        
        for (Object[] objects : coocedges) {
			
        	Float tempWeight = (Float) objects[2]; 
        	
        	
        	if(tempWeight > MaxSourceCoocEdgeWeight)
        		MaxSourceCoocEdgeWeight  =tempWeight;
        
		}

        
        //Invert the Weight Take Weight as it is!
        
        for (Object[] objects : coocedges) {
			
        	
        	Float tempWeight = (Float) objects[2]; 
        	Float Factor = (tempWeight.floatValue()/  MaxSourceCoocEdgeWeight.floatValue());
        	Float InverseFactor = (1.f-Factor);
        	
        	//Wir Willen Eine glocke Die perfekte Distanz ist bei 0,5 daher =,5 ist das neue 1
        	// 
        	Float WeightFactor;
        	if(Factor > 0.5f )
        		WeightFactor = (0.5f-(Factor - 0.5f))*2;
        	else
        		WeightFactor = (0.5f-(0.5f - Factor))*2;
        	/*
        	System.out.println("Factor: "+Factor);
        	System.out.println("InverseFactor: "+InverseFactor);
        	System.out.println("WeightFactor: "+WeightFactor);
        	*/
        	objects[2] = new Float( WeightFactor *MaximumEdgeWeight);
        	
        	if(MinimumEdgeWeight > (Float)objects[2]){
        		System.out.println("Minimum Edge weight had to be declared"+objects[2]);
        		objects[2] = MinimumEdgeWeight;
        	}
        }
        
        
        
        List<Object[]> filterMetas = FilterReportWriter.getInstance().getMetaEdgesAsGraphContainerEdges();
        //Summing Up some More if Somethin is Filtered
        List<Object[]> positioningGroup = new ArrayList<Object[]>();
        positioningGroup.addAll(coocedges);
        
        for (Object[] filterEdge:  filterMetas){
        	boolean matched =false;
            for (Object[] coocurenceEdge : coocedges) {
            	//Forward Backward Matching
            	if((coocurenceEdge[1].equals(filterEdge[1])&& coocurenceEdge[0].equals(filterEdge[0]))|| (coocurenceEdge[0].equals(filterEdge[1])&& coocurenceEdge[1].equals(filterEdge[0])))
            		//Simple Weight
            		coocurenceEdge[2] = ((Float)coocurenceEdge[2] +((Integer)filterEdge[2]*2));
        			//Uniquity
            		coocurenceEdge[3] = ((Float)coocurenceEdge[3] +((Integer)filterEdge[2]));
            		//Avarage Uniquity adjustment
            		coocurenceEdge[4] = (((Float)coocurenceEdge[4]*(Float)coocurenceEdge[2] +((Integer)filterEdge[2]))/((Float)coocurenceEdge[2]+(Integer)filterEdge[2]));
            		matched =true;
            }
            if(matched == false){
            	//creating a new Edge Especially for Filetered Results with the Uniquity Values
            	Object[] temp = new Object[5];
            	temp[0] = filterEdge[0];
            	temp[1] = filterEdge[1];
            	temp[2] = new Float(((Integer)filterEdge[2]));
            	temp[3] = temp[2];
            	temp[4] = new Float(1.0);
            	positioningGroup.add(temp);
            	
            }
        }

        
        List<Edge> newEdges = GraphContainerToGephi.AttachEdgesWeightetToGephi(coocedges, graphModel);
        //Here The Repulsion of the Source Nodes is set This is a Weight of

        contextFilter.setMatch(0);
        query = filterController.createQuery(contextFilter);
        view = filterController.filter(query);
        graphModel.setVisibleView(view);
        
        writeToSystemout("Context Filter 0");
        writeToSystemout("EdgeCount :"+graphModel.getGraphVisible().getEdgeCount());
        writeToSystemout("NodeCount :"+graphModel.getGraphVisible().getNodeCount());
        
        AutoLayout autoLayout = new AutoLayout(4, TimeUnit.SECONDS);
        AutoLayout.DynamicProperty adjustBySizeProperty = AutoLayout.createDynamicProperty("forceAtlas.adjustSizes.name", Boolean.TRUE, 0.1f);
        AutoLayout.DynamicProperty repulsion = AutoLayout.createDynamicProperty("forceAtlas.repulsionStrength.name", this.RepulsionFactorSourceNodes(),0.f );
        autoLayout.setGraphModel(graphModel.getVisibleView().getGraphModel());
        ForceAtlasLayout secondLayout = new ForceAtlasLayout(null);
        RadialAxisLayout radlayout = new RadialAxisLayout(new RadialAxisLayoutBuilder(), 10, false);

        autoLayout.addLayout(radlayout, .1f );
        autoLayout.addLayout(secondLayout, .9f,new AutoLayout.DynamicProperty[]{repulsion,adjustBySizeProperty} );
        autoLayout.execute();
        // set Sourcenodes to fixed position
        this.lockVisible();
        
        graphModel.setVisibleView(mainView);
        
        for (int i = 0; i < newEdges.size(); i++) {
        	if(graphModel.getGraph().contains(newEdges.get(i)))
        		try{
        			graphModel.getGraph().removeEdge(newEdges.get(i));
        		}catch (Exception e) {
        			writeToSystemout(graphModel.getGraph().contains(newEdges.get(i))+""+newEdges.get(i).toString() );
        			writeToSystemout("Critical Temporal Addup Edge Could not be Removed");
				}
		}
        
        for (int i = 0 ; i< TempEdges.size();i++){
        	try{
        		boolean tempo = graphModel.getGraph().addEdge(TempEdges.get(i));
        		if(!tempo)
        			writeToSystemout("Edge could not be Re-added");
        	}catch (Exception e) {
    			writeToSystemout("Critical Old Edge Could no be Found must be Lost forever!");
        	}
        }
        
        
	}

    
	private void layoutPositionContext(GraphView mainView) {
		//AutoLayout autoLayout;
		ForceAtlas2 secondLayout;
		//Filter Aufheben

        writeToSystemout("ALL");
        writeToSystemout("EdgeCount :"+graphModel.getGraphVisible().getEdgeCount());
        writeToSystemout("NodeCount :"+graphModel.getGraphVisible().getNodeCount());
        // Kantengewicht einstellen 

        secondLayout = new ForceAtlas2Builder().buildLayout();
        secondLayout.setGraphModel(graphModel.getVisibleView().getGraphModel());
        secondLayout.setEdgeWeightInfluence(1.2d);
        secondLayout.setScalingRatio(1.d);
       	secondLayout.setThreadsCount(LayoutThreadCount);

       	//Run Algorithm
      	secondLayout.initAlgo();
        for (int i = 0; i < 3500 && secondLayout.canAlgo(); i++) {
        	secondLayout.goAlgo();
        }

        secondLayout.setAdjustSizes(true);
        
        for (int i = 0; i < 1000 && secondLayout.canAlgo(); i++) {
        	secondLayout.goAlgo();
        }
        
        secondLayout.endAlgo();
      	return;
	}

	private RankingController layoutSizeContextNodes(
			FilterController filterController, EqualNumberFilter contextFilter) {
		Query query;
		GraphView view;
		contextFilter.setMatch(1);
        query = filterController.createQuery(contextFilter);
        view = filterController.filter(query);
        graphModel.setVisibleView(view);
        writeToSystemout("Context Filter 1");
        contextNodeSize = graphModel.getGraphVisible().getNodeCount();
        contextEdgeSize= graphModel.getGraphVisible().getEdgeCount();
        writeToSystemout("EdgeCount :"+graphModel.getGraphVisible().getEdgeCount());
        writeToSystemout("NodeCount :"+graphModel.getGraphVisible().getNodeCount());
        
        //Rank size by Context to Worldwide
        RankingController rankingController = Lookup.getDefault().lookup(RankingController.class);
        
        AttributeColumn contextToWorldwideColumn = attributeModel.getNodeTable().getColumn("ContextToWorldwideRanking");
        Ranking<?> contextToWorldwideRanking = rankingController.getModel().getRanking(Ranking.NODE_ELEMENT, contextToWorldwideColumn.getId());
        String temp =  Transformer.RENDERABLE_SIZE;
        AbstractSizeTransformer<?> sizeTransformer = (AbstractSizeTransformer) rankingController.getModel().getTransformer(Ranking.NODE_ELEMENT, temp);
        sizeTransformer.setMinSize(3);
        sizeTransformer.setMaxSize(20);
        rankingController.transform(contextToWorldwideRanking,sizeTransformer);
		return rankingController;
	}

	private AttributeColumn layoutSizeSourceNodes(
			FilterController filterController, EqualNumberFilter contextFilter,
			RankingController rankingController) {
		Query query;
		GraphView view;
		String temp;
		AbstractSizeTransformer<?> sizeTransformer;
		contextFilter.setMatch(0);
        query = filterController.createQuery(contextFilter);
        view = filterController.filter(query);
        graphModel.setVisibleView(view);
        writeToSystemout("Context Filter 0");
        sourceNodeSize = graphModel.getGraphVisible().getNodeCount();
        sourceEdgeSize= graphModel.getGraphVisible().getEdgeCount();
        writeToSystemout("EdgeCount :"+graphModel.getGraphVisible().getEdgeCount());
        writeToSystemout("NodeCount :"+graphModel.getGraphVisible().getNodeCount());
        
        AttributeColumn inDegreeInTotalGraphColumn = attributeModel.getNodeTable().getColumn("INDegreeInTotalGraph");
        Ranking inDegreeInTotalGraphRanking = rankingController.getModel().getRanking(Ranking.NODE_ELEMENT, inDegreeInTotalGraphColumn.getId());
        temp =  Transformer.RENDERABLE_SIZE;
        sizeTransformer = (AbstractSizeTransformer) rankingController.getModel().getTransformer(Ranking.NODE_ELEMENT, temp);
        sizeTransformer.setMinSize(25);
        sizeTransformer.setMaxSize(40);
        rankingController.transform(inDegreeInTotalGraphRanking,sizeTransformer);
		return inDegreeInTotalGraphColumn;
	}


	private void layoutNodeColor(FilterController filterController,
			EqualNumberFilter contextFilter,
			RankingController rankingController,
			AttributeColumn inDegreeInTotalGraphColumn) {
		Query query;
		GraphView view;
		contextFilter.setMatch(0);
        query = filterController.createQuery(contextFilter);
        view = filterController.filter(query);
        graphModel.setVisibleView(view);
        writeToSystemout("Context Filter 0");
        writeToSystemout("EdgeCount :"+graphModel.getGraphVisible().getEdgeCount());
        writeToSystemout("NodeCount :"+graphModel.getGraphVisible().getNodeCount());
        Float sourcenodequantity = new Float (graphModel.getGraphVisible().getNodeCount());
        
        Ranking InDegreeColorRanking = rankingController.getModel().getRanking(Ranking.NODE_ELEMENT, inDegreeInTotalGraphColumn.getId());
        AbstractColorTransformer colorTransformer = (AbstractColorTransformer) rankingController.getModel().getTransformer(Ranking.NODE_ELEMENT, Transformer.RENDERABLE_COLOR);
         
        colorTransformer.setColors(new Color[]{SourceNodeMinColor, SourceNodeMaxColor});
        
        rankingController.transform(InDegreeColorRanking,colorTransformer);
        

        
        contextFilter.setMatch(1);
        query = filterController.createQuery(contextFilter);
        view = filterController.filter(query);
        graphModel.setVisibleView(view);
        writeToSystemout("Context Filter 1");
        writeToSystemout("EdgeCount :"+graphModel.getGraphVisible().getEdgeCount());
        writeToSystemout("NodeCount :"+graphModel.getGraphVisible().getNodeCount());
        /////////DUMMY NODES The Dummy Nodes Define the Range of the Source node Rankin. They Prevent the Visualsiation from becomming tooo Red.
        
        //The Dummy Node 1 Should Mark the Upper Bound of Scale for The Sourcenode Ranking
        Node DummyNode1 = graphModel.factory().newNode("Dummynode1");

        //The Dummy Node 2 Should Mark the Lower Bound of Scale for The Sourcenode Ranking
        graphModel.getGraph().addNode(DummyNode1);

        DummyNode1.getAttributes().setValue("SourcenodeRanking",1f );
        DummyNode1.getAttributes().setValue("IsContext",1 );
        
        AttributeColumn SourcenodeRankingColumn = attributeModel.getNodeTable().getColumn("SourcenodeRanking");
        Ranking SourceNodeRanking = rankingController.getModel().getRanking(Ranking.NODE_ELEMENT, SourcenodeRankingColumn.getId());
        colorTransformer = (AbstractColorTransformer) rankingController.getModel().getTransformer(Ranking.NODE_ELEMENT, Transformer.RENDERABLE_COLOR);

        //SET THE UPPER Bound for The Color of the Context Nodes
        colorTransformer.setColors(new Color[]{ContextNodeMinColor, ContextNodeMaxColor});
        rankingController.transform(SourceNodeRanking,colorTransformer);

        //Remove Dummy Nodes
        graphModel.getGraph().removeNode(DummyNode1);

        
	}
    
    private void copytoEdgeWeight(String string) {
        for (Edge e : graphModel.getUndirectedGraph().getEdges()) {
        	Object temp = e.getEdgeData().getAttributes().getValue(string);
        	if(temp != null){
        		e.setWeight((Float) temp );
        	}
        }
		
	}

	private void lockVisible(){
    	
        NodeIterable visnodes = graphModel.getGraphVisible().getNodes();
        
        for (Node object : visnodes) {
            object.getNodeData().setFixed(true);
            writeToSystemout("fixed");
        }

    	
    }
	private void RemoveAndSaveVisibleEdges(){
    	
        EdgeIterable visedges = graphModel.getGraphVisible().getEdges();
        Edge[] tempArray = visedges.toArray();
        metas = new ArrayList<CooccuredMetaEdge>(tempArray.length);
        
        for (int i = 0; i < tempArray.length; i++) {
        	TempEdges.add(tempArray[i]);
        	graphModel.getGraph().removeEdge(tempArray[i]);
        	CooccuredMetaEdge meta = new CooccuredMetaEdge(tempArray[i].getSource().getNodeData().getId(),tempArray[i].getTarget().getNodeData().getId(),new Integer(10),null, false );
        	metas.add(meta);
		}
        
    }
    
	
	
    
    private AttributeColumn getAttColumbyString(String label){
       
       AttributeColumn temp = attributeModel.getNodeTable().getColumn(label);
       
       if(temp != null)
           return temp;
       else 
           return attributeModel.getEdgeTable().getColumn(label);
    }
    
    private void checkNANPos(){
    	
    	NodeIterable it = graphModel.getGraph().getNodes();
    	
    	for (Node node : it) {
			if(Float.isNaN(node.getNodeData().x()) || Float.isNaN(node.getNodeData().y()) ){
				writeToSystemout("NAN ERROR");
				break;
				
			}
			//else
			//	writeToSystemout(node.getNodeData().x());
		}
    	
    	
    	
    }
    
    private void setSomePos(){
    	
    	NodeIterable it = graphModel.getGraph().getNodes();
    	
    	float i = 0.f ;
    	
    	for (Node node : it) {
    		node.getNodeData().setX(i);
    		node.getNodeData().setY(i);
    		i = i+0.32f;
 	
    	}
    	
    	
    	
    }
    
    //----------------------------------------WEIGHTS!!!---------------------------------------
    
    
    //This Weight Calculates an is The Repulsion Factor For the Source Node Positioning
    private double RepulsionFactorSourceNodes(){
    	double RepulsionFactor=10000.0d *(contextNodeSize*5);
    	
    	return RepulsionFactor;
    }
    
    private void writeToSystemout(String in){
    	if(verbose)
    		System.out.println(in);
    }
    
    
    //Delete All Non Nececary Attributes
    private void Clean(){
    	 NodeIterable nodeit = graphModel.getGraph().getNodes();
    	
    	 for (Node node : nodeit) {
			
    		 
    		 
		}
    	
    }
    
    
    
}
