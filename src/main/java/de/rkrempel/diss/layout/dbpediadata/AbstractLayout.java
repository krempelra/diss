package de.rkrempel.diss.layout.dbpediadata;

import de.rkrempel.diss.core.commontools.GraphContainer;

public abstract class AbstractLayout {
	
	
	public abstract String script(GraphContainer gc, String writeto);
	public String getIdentifier(){
		
		return this.getClass().getSimpleName();
		
	}
}