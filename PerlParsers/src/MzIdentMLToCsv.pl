## The program to comvert Omssa and X!Tandem MzIdentML files to CSV format.
##
## Usage -
## * For Omssa MzIdentML file -
## >> perl MzIdentMLToCsv.pl -J Toxo_1D_Slice43_omssa.mzid  -X omssa -Y mzIdCsvOut_omssa.csv
##
## * For X!Tandem MzIdentML file -
## >> perl MzIdentMLToCsv.pl -J Toxo_1D_Slice43_tandem.mzid  -X X!Tandem -Y mzIdCsvOut_tandem.csv
##
## @author Ritesh Krishna - riteshk@liv.ac.uk
## @date Feb 9, 2010.



use FindBin;
use lib "$FindBin::Bin/..";
use lib "../XMLCommunications";

use XMLCommunications::QuerySpectrumInfoFromMzIdentML;

use Data::Dumper;
use Getopt::Std;
use strict;

our ($opt_J,$opt_X,$opt_Y);
getopts('J:X::Y:'); 

my $file = $opt_J;
my $searchEngine = $opt_X;
my $writeCsvFile = $opt_Y;

QuerySpectrumInfoFromMzIdentML::start_parse($file,$searchEngine);

my %records_spectrumIdentificationItem = QuerySpectrumInfoFromMzIdentML::getSpectrumInfo();
my %records_peptideEvidence            = QuerySpectrumInfoFromMzIdentML::getPeptideEvidence();
my %records_PeptideSeqInfo             = QuerySpectrumInfoFromMzIdentML::getPeptideSeqInfo();
my %records_PeptideMODInfo             = QuerySpectrumInfoFromMzIdentML::getPeptideMODInfo(); 


#open OUTFILE1,'>log_spectrumID_Pipeline1.txt';
#print OUTFILE1 Dumper(\%records_spectrumIdentificationItem);
#close OUTFILE1;
##
#open OUTFILE1,'>log_peptideEvidence_Pipeline1.txt';
#print OUTFILE1 Dumper(\%records_peptideEvidence);
#close OUTFILE1;
##
#open OUTFILE1,'>log_peptideSeqInfo_Pipeline1.txt';
#print OUTFILE1 Dumper(\%records_PeptideSeqInfo);
#close OUTFILE1;
#
#open OUTFILE1,'>log_peptideMOD_Pipeline1.txt';
#print OUTFILE1 Dumper(\%records_PeptideMODInfo);
#close OUTFILE1;


open WRITEFILE,">".$writeCsvFile  or die "Can't open $writeCsvFile for write : $!";

my $counter = 0;

my $header = 'Spectrum number,Filename/id,Peptide,Protein FDRScore,Mass,PAG,Accession,Start,Stop,e-value,Mods,Charge,Theo Mass'."\n";
print WRITEFILE $header;

for my $specID (keys %records_spectrumIdentificationItem){
	
	#print "\n $specID";
	
	my @spectraInfoForThisID = @{$records_spectrumIdentificationItem{$specID}};
	
	my $spectraId            = $specID;
	my $fileNameId           = $spectraInfoForThisID[5];
	my $calcMass             = $spectraInfoForThisID[0];
	my $experimentalMass     = $spectraInfoForThisID[1];
	my $chargeState          = $spectraInfoForThisID[6];
	my $evalue 				 = $spectraInfoForThisID[4];
	
	my @pepSequenceArray     = @{$records_PeptideSeqInfo{$spectraInfoForThisID[2]}};
	my $pepSeq               = $pepSequenceArray[0];
	
	my @pepEvidenceArray    = @{$records_peptideEvidence{$specID}};
	
	my @allDBRef;
	my @allStartIndex;
	my @allEndIndex;
	
	my $noOfPeptides = scalar @pepEvidenceArray;
	for(my $i=0; $i < $noOfPeptides ; $i++){
 		my $dbrefName  = $pepEvidenceArray[$i][5];
 		my $startIndex = $pepEvidenceArray[$i][1];
 		my $endIndex   = $pepEvidenceArray[$i][2];
 		
 		@allDBRef      = (@allDBRef,$dbrefName);
 		@allStartIndex = (@allStartIndex,$startIndex);
 		@allEndIndex   = (@allEndIndex,$endIndex);
 		
 	}
	 
	my @modString ;
 	if (exists $records_PeptideMODInfo{$spectraInfoForThisID[2]}){
 		my @modValues = @{$records_PeptideMODInfo{$spectraInfoForThisID[2]}};				
 		my $numberOfModReports = scalar @modValues;
 		for (my $i = 0 ; $i < $numberOfModReports ; $i++){
 			my $tempModString;
 			if($modValues[$i][2] eq 'unknown modification'){ 
 				$tempModString = $modValues[$i][3]."_".$modValues[$i][1].":".$modValues[$i][0].",";
 			}
 			else{
 				$tempModString = $modValues[$i][2]." (".$modValues[$i][1]."):".$modValues[$i][0].",";
 			}
 			
 			if($i == ($numberOfModReports - 1)){
 				chop($tempModString); ## remove the last comma
 			}
 			
 			@modString = (@modString,$tempModString);
 		}
 	}else {
 		@modString = ("\ ");
 	} 
	
	
	$counter = $counter + 1;
	
	my $noOfDBRef = scalar @allDBRef;
	for (my $i = 0 ; $i < $noOfDBRef ; $i++){
		my $outputLine = $spectraId.','.$fileNameId.','.$pepSeq.','.'P_FDR'.','.$calcMass.','.$counter.','.$allDBRef[$i].','.$allStartIndex[$i].','.$allEndIndex[$i].','.$evalue.','.'"'."@modString".'"'.','.$chargeState.','.$experimentalMass."\n";
		print WRITEFILE $outputLine;
	}
	  
	#my $outputLine = $spectraId.','.$fileNameId.','.$pepSeq.','.'P_FDR'.','.$calcMass.','.$counter.','.$allDBRef.','.$allStartIndex.','.$allEndIndex.','.$evalue.','.$modString.','.$chargeState.','.$experimentalMass."\n";
	#print WRITEFILE $outputLine;
	
	  
	  
}

close WRITEFILE;
