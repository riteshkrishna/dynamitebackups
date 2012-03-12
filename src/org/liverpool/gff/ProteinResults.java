package org.liverpool.gff;

/**
 * Create this object from the result files obtained after running the pipeline. We can create such
 * objects to feed into the GFF routines to retrieve the relevant information for mapping.
 * @author riteshk
 *
 */
public class ProteinResults {
	
	String accession;
	boolean decoyOrNot;
	long start;
	long end;

	public ProteinResults(String accession,boolean decoyOrNot,long start,long end) {
		this.accession = accession;
		this.decoyOrNot = decoyOrNot;
		this.start = start;
		this.end = end;
	}
	
	public String getAccession(){
		return this.accession;
	}
	
	public boolean getDecoyOrNot(){
		return this.decoyOrNot;
	}
	
	public long getstart(){
		return this.start;
	}
	
	public long getEnd(){
		return this.end;
	}
}
