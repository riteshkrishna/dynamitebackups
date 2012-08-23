#!/usr/bin/perl


#######################################################################
##   Andy Jones 2010                                         
##                                                                   
## This parses output from a CSV file, which requires a separate config file to map column headers to data types in conjunction with ReadCSV.pm
## The parser requires a second params file which specifies the necessary search protocols and parameters, which are used to create valid mzIdentML
## The essential columns are the spec_title, e-value/FDRScore, peptide sequence, mods, start, end, prot_acc or defline
##  The code can also read in a column for protein ambiguity groups, if the protein_grouping flag is switched on
##
#######################################################################


use strict;

use FindBin;
use lib "$FindBin::Bin";


use ReadCSV;
use GetFDRValues;
use XML::Simple;
use Data::Dumper;	#Only required for testing
use Text::CSV;
use Data::Types qw(:all);	#added to check data types used in Mods


print Dumper(\@ARGV);

 if(@ARGV != 3 && @ARGV != 4)
 {
 print "Usage: csv2mzIdentML [results].csv [params].csv [outputfile].mzid [-protein_grouping]\n";
 print "If the \"protein_grouping\" flag is included, a Protein Detection List will be output, with ProteinAmbiguityGroups defined in Column F of the CSV file\n";
 print "Consult the user docs for allowed CSV format\n";
  
 exit(1);
 }
my $results_file = $ARGV[0];
my $paramFile = $ARGV[1];
my $outfile = $ARGV[2];


my $protGrouping = 0;

if($ARGV[3] =~ m/protein_grouping/){
	$protGrouping = 1;
	print "Protein groups will be output\n";
}


# *********************   Parser parameters ****************************
our $prot_FDR_threshold = 0.05;
our $MGF_TITLE_FOR_SPECTRUM_ID = 1;  #set to 1 to print out spectrumID as the title attribute from MGF, otherwise use index=[spectrum_index_pos]
our $MAX_RANK = 25;
my $configFile = $FindBin::Bin."/csv_config_file.csv";
#********************************************************************


#*******
#ARJ Removed requirement for local database path 16/12/009
#*******
#my @REQUIRED_PARAMS = ("Local database path","Software name","Provider","Parent mass type","Fragment mass type","Enzyme","Missed cleavages","Fragment search tolerance plus","Fragment search tolerance minus","Parent search tolerance plus","Parent search tolerance minus","PSM threshold","Input file format","Database file format","Spectra data file format","Spectrum ID format");

my @REQUIRED_PARAMS = ("Software name","Provider","Parent mass type","Fragment mass type","Enzyme","Missed cleavages","Fragment search tolerance plus","Fragment search tolerance minus","Parent search tolerance plus","Parent search tolerance minus","PSM threshold","Input file format","Database file format","Spectra data file format","Spectrum ID format");
my ($tmp1,$tmp2) = readCSVParams($paramFile,\@REQUIRED_PARAMS); 
our %params = %{$tmp1};
our %searchMods = %{$tmp2};

my $config_exists = (-e $configFile);
#print("\nconfig file ".$configFile." exists?".$config_exists."\n");

my ($tmp_config) = readConfig($configFile);
our %config_datatypes = %{$tmp_config};


our %se_config;

if(!$config_datatypes{$params{'Software name'}{'cvTerm'}}){
	die "Fatal error, search engine: $params{'Software name'}{'cvTerm'} not recognised in the config file\n";
}
else{
	%se_config = %{$config_datatypes{$params{'Software name'}{'cvTerm'}}};
}
#print Dumper $config_datatypes{$params{'Software name'}{'cvTerm'}};



my($ptr1,$ptr2,$ptr3);
if(-e $results_file)
{
	($ptr1,$ptr2,$ptr3) = ParseCSV($results_file,\%se_config,$protGrouping);
}
else
{
	die "Error in csv2mzidentML: ".$results_file." does not exist";
}

my @results = @{$ptr1};

#print Dumper (@results);

our %protein_mappings = %{$ptr2};
our $numDBseqs = 0;
our @all_pags= @{$ptr3};


# ********************************                    Need to re-instate this method to get FDR values - this method was altering the results array and causing errors  08/10/2009
#my %ScoreThreshold = GetFDRValues('0','T',@results);
#*********************************



#our %unimods;

#if(!$modfile){
#	die "No mod file provided";
#}
#else{
	#%unimods = GetModsFromXML($modfile);
#}


#global variables
my %aa;
my $spectrumIdentificationList = "Spectrum_1_result";
my $spectrumIdentificationProtocol = "SearchProtocol";
my $proteindeterminationprotocol="protein_detection_protocol_1";
my $proteinDetectionList;
my $spectrumIdSoftwareRef;
my $proteinIdentificationProtocol;
my $db_identifier = "search_database";
my %prot_evidence;

#################  header #####################################################

my $header = GetFileHeader();

################################################################################


################   conceptual molecule collection  #############################

#proteins and peptides
#get the proteins

my %proteins;
if($params{'Local database path'}){
	my %temp = %{$params{'Local database path'}};
	my $database = $temp{'Value'};
	%proteins = GetProteins($database,\%protein_mappings);
}
else{
	print "No local database file provided, protein sequences will not be exported to mzid\n";
}



#Method no longer used
#my %peptides = GetPeptides(@results);

#get the MolecultCollection
my $moleculeCollection = GetMoleculeCollection(@results);

################################################################################



#################  analysis Collection       ###################################

#links protocols, databases and sectra
my $analysisCollection = GetAnalysisCollection();

################################################################################


##############   analysis protocol collection  #################################

#params associated with software
my $analaysisProtocolCollection = GetAnalysisProtocolCollection(\%params);

################################################################################


############   data collection   ###############################################
##databases
#spectra data
#spectrum identifications - all results

my $dataCollection = GetDataCollection();


#################################################################################



####################    analysis software list    ################################

#the different software
my $analysisSoftwareList = GetAnalysisSoftwareList();

my $proteinList;
if($protGrouping){
	$proteinList = GetProteinDetectionList();
}


##################################################################################

#create the file
open(XML,">$outfile") or die "unable to open the $outfile\n";

print XML $header;
print XML $analysisSoftwareList;
print XML $moleculeCollection; 
print XML $analysisCollection;
print XML $analaysisProtocolCollection;
print XML $dataCollection;
print XML $proteinList;

print XML "\t\t</AnalysisData>\n";
print XML "\t</DataCollection>\n";
print XML "</mzIdentML>\n";

close XML;

print "file $outfile\n";



sub GetProteins
{
	my $db = shift;
	my $tmp = shift;
	my %prot_res = %{$tmp};


	my %prot;

	#now for all of these proteins get the full sequence
	#go through the database file
	my $seq;
	my $acc;
	my $count = 0;
	
	open(DB,"<$db") or print "unable to open the database file, protein sequences will not be exported $db\n";
	 while(my $line = <DB>){ 
	  if($line =~ m/^\>/){
		$count ++;
	  
	   	if($seq){
	   		$seq =~ s/\n//g;
	   		$seq =~ s/\s+//g;
			$seq =~ s/\*//g;
			
			
	    	if($prot_res{$acc}){
	    		$prot{$acc} = $seq;
	    	}
	   		$seq = "";
	   		$acc = "";
	   	}
	   	my @split = split/ /,$line;
	  	$split[0] =~ s/^\>//;
	  	$acc = $split[0];
	  	$acc =~ s/\n//;
	  	#bug fix for weird prefixes on protein names
	  	$acc =~ s/SS\d+\_//;
	  	$acc =~ s/\_\_SR//;
	  	$seq = "";
	  }
	  else{
	  	$seq .= $line;
	  }
	 } # end of while

	#and the last sequence
	   if($seq){
		$seq =~ s/\n//g;
	   	$seq =~ s/\s+//g;
		$seq =~ s/\*//g;
	    if($prot_res{$acc}){
	    	$prot{$acc} = $seq;
	    }
	    $seq = "";
	    $acc = "";
	}
	close dB;
	$numDBseqs = $count;
	return %prot;
}

####################################################################################################################

#not used to delete...
sub GetPeptides
{
my @res = @_;
my %peptides;

 for(my $s=0 ; $s<scalar(@res) ; $s++)
 {
  for(my $r=0 ; $r<=$MAX_RANK ; $r++)
  {
  
	my $pep_hit = $res[$s][$r];	
	
	
   #if($res[$s][$r]{'sequence'} && $res[$s][$r]{'sequence'} ne "NULL")
   if($pep_hit->sequence() && $pep_hit->sequence() ne "NULL")
   {
    if($pep_hit->mods()){
	    #the sequence that is stored should be sequence_mod1_mod2 etc.

	    my $mod_string;		
	    $pep_hit->mods() =~ s/\"//g;
		
		print "temp1: ". $pep_hit->mods() . "\n";
		
	    my @split1 = split/\,/,$pep_hit->mods();
	    
		for(my $i=0 ; $i<scalar(@split1) ; $i++){	
		
			my @split2 = split/\:/,$split1[$i];		
			#my $res = substr($split2[0],length($split2[0])-1); 
			
			my $unimod_def;
			my $tempMod = $searchMods{$split2[0]};			
			
			if($tempMod){
				my %mod = %{$tempMod};
				$unimod_def = "$mod{'Unimod ID'}:$mod{'Unimod name'}:$mod{'Mass Delta'}";
				my $loc = $split2[1];
				
				my $res = substr($pep_hit->sequence(),$loc-1, 1);
				$loc =~ tr/ //d;
				
				$mod_string .= "*$unimod_def" .":" . $res . ":". $loc;
				
				print "Mod string: $mod_string\n";
			}
			else{
				print "The modification: $split2[0], missing from the input params file\n";
			}			
	    }
		
	    $mod_string =~ s/^\*//;	#remove leading *
		
		$pep_hit->mods($mod_string);
		
		my $seq_identifier;

		if($mod_string){
			$seq_identifier	= $pep_hit->sequence() . "*" . $mod_string;
		}
		else{
			$seq_identifier	= $pep_hit->sequence();
		}
	    $peptides{$seq_identifier}[0] = $mod_string;
	    #my $PE_ref = "PE_" . $s . "_" . $r . "_" . $results[$s][$r]{'protein'};
		
		my $temp_rank = $r + 1;					#Need to increment rank, start counting from 1 rather than zero for array
		my $PE_ref = "PE_" . $s . "_" . $temp_rank . "_" . $pep_hit->getProteinMap();
		$peptides{$seq_identifier}[1] = $PE_ref;
	}
    else
    {
    $peptides{$pep_hit->sequence()}[0] = 'X';
    #my $PE_ref = "PE_" . $s . "_" . $r . "_" . $results[$s][$r]{'protein'};
	my $temp_rank = $r + 1;					#Need to increment rank, start counting from 1 rather than zero for array
	my $PE_ref = "PE_" . $s . "_" . $temp_rank. "_" . $pep_hit->getProteinMap();
    $peptides{$pep_hit->sequence()}[1] = $PE_ref;
    }
   }
  }
 }

	return %peptides;
}



sub GetMoleculeCollection{
	my $xml;
	my @res = @_;
	
	$xml = "\t<SequenceCollection>\n";

	#now for all the proteins

	
	if(%proteins != ()){
		 foreach my $acc (keys %proteins){
			 my $length = length($proteins{$acc});
			 $xml .= "\t\t<DBSequence id=\"$acc\" length=\"$length\" SearchDatabase_ref=\"$db_identifier\" accession=\"$acc\">\n";
			 $xml .= "\t\t\t<seq>$proteins{$acc}</seq>\n";
			 $xml .= "\t\t</DBSequence>\n";
		 }
	}
	else{
			foreach my $acc (keys %protein_mappings){
				$xml .= "\t\t<DBSequence id=\"$acc\" SearchDatabase_ref=\"$db_identifier\" accession=\"$acc\"/>\n";
		 }
	}

	#and the peptides

	#print Dumper (@res); 
	
	for(my $s=0 ; $s<scalar(@res) ; $s++){
	
		if($res[$s]){
			
		
			for(my $r=0 ; $r<=$MAX_RANK ; $r++){
			
				my $pep_hit = $res[$s][$r];
				
			
				if(defined($pep_hit)){
					
					my $temp_rank = $r+1;		#ranks start from 1 rather than zero
					my $peptide_id = "Peptide_" . $s . "_" . $temp_rank;
					my $seq = $pep_hit->sequence();
					$xml .= "\t\t<Peptide id=\"$peptide_id\">\n";
					$xml .= "\t\t\t<peptideSequence>$seq</peptideSequence>\n";
					
					my $temp_mods = $pep_hit->mods();
					if($temp_mods){
					    #the sequence that is stored should be sequence_mod1_mod2 etc.

					    my $mod_string;
					    $temp_mods =~ s/\"//g;
						
											
					    my @split1 = split/\,/,$temp_mods;
					
						my $massType = $params{'Parent mass type'}{'cvTerm'};
						my $massAtt;
							
						if($massType eq "parent mass type mono"){
							$massAtt = "monoisotopicMassDelta";
						}
						elsif($massType eq "parent mass type average"){
							$massAtt = "avgMassDelta";
						}
						else{
							print "Error: no parent mass type specified, assuming monoisotopic\n";
							$massAtt = "monoisotopicMassDelta";
						}
						
						for(my $i=0 ; $i<scalar(@split1) ; $i++) {
						
							my @split2 = split/\:/,$split1[$i];
							#my $res = substr($split2[0],length($split2[0])-1); 
							my $unimod_def;
							
							my $nospace = $split2[0];
							#$nospace =~ s/\s+//g;
							#$nospace =~ s/^\s//;
							#$nospace =~ s/\s$//;
							my $tmp_mod = $searchMods{$nospace};
							#my $tmp_mod = $searchMods{$split2[0]};
							
							if($tmp_mod){

								my %mod = %{$tmp_mod};													
								$unimod_def = "$mod{'Unimod ID'}:$mod{'Unimod name'}:$mod{'Mass Delta'}";
								my $loc = $split2[1];
								
								my $res = substr($pep_hit->sequence(),$loc-1, 1);
								$loc =~ tr/ //d;
								
								#$mod_string .= "*$unimod_def" .":" . $res . ":". $loc;						
								
							   $xml .= "\t\t\t<Modification location=\"$loc\" residues=\"$res\" $massAtt=\"$mod{'Mass Delta'}\">\n";
							   $xml .= "\t\t\t\t\<cvParam accession=\"UNIMOD:$mod{'Unimod ID'}\" name=\"$mod{'Unimod name'}\" cvRef=\"UNIMOD\"/>\n";
							   $xml .= "\t\t\t</Modification>\n";
							}
							else{
								print "Modification: $split2[0], missing from the input params file, reported as 'unknown modification'\n";
								my $loc = $split2[1];							
								my @tmp2 = split(/_/,$split2[0]);							
								my $res = $tmp2[1];
								my $mass = $tmp2[0];						
								
								if(!$res){	#attempt to find residue if unspecified (locations start counting from 1)
									$res = substr($seq,$loc-1,1);
								}
								
								if(!is_float($mass)){
									print "Error extracting unknown modification mass from CSV file, setting to -999\n";
									$mass = -999;
								}
							
								
								
								$xml .= "\t\t\t<Modification location=\"$loc\" residues=\"$res\" $massAtt=\"$mass\">\n";
								$xml .= "\t\t\t\t\<cvParam accession=\"MS:1001460\" name=\"unknown modification\" cvRef=\"PSI-MS\" value=\"\"/>\n";
								$xml .= "\t\t\t</Modification>\n";
								
							}	
							
						}
					}
					$xml .= "\t\t</Peptide>\n";
					
				}
			}
		}
	}
	
	$xml .= "\t</SequenceCollection>\n";
	
	return $xml;
}			




sub GetPeptideMass
{
	my $seq = shift;
	my $mass = 0;

	#peptide masses
	#get the aa masses

	$aa{'A'} = "71.03711";
	$aa{'C'} = "103.00919";
	$aa{'D'} = "115.02694";
	$aa{'E'} = "129.04259";
	$aa{'F'} = "147.06841";
	$aa{'G'} = "57.02146";
	$aa{'H'} = "137.05891";
	$aa{'I'} = "113.08406";
	$aa{'K'} = "128.09496";
	$aa{'J'} = "113.08406";
	$aa{'L'} = "113.08406";
	$aa{'M'} = "131.04049";
	$aa{'N'} = "114.04293";
	$aa{'P'} = "97.05276";
	$aa{'Q'} = "128.05858";
	$aa{'R'} = "156.10111";
	$aa{'S'} = "87.03203";
	$aa{'T'} = "131.04768";
	$aa{'V'} = "99.06841";
	$aa{'W'} = "186.07931";
	$aa{'Y'} = "163.06333";
	$aa{'nterm'} = "0.00";
	$aa{'cterm'} = "0.00";

	my @split = split//,$seq;
	 for(my $i=0 ; $i<scalar(@split) ; $i++)
	 {
	 $mass += $aa{$split[$i]};
	 }
	return $mass;
}

sub GetFileHeader
{
my $xml;

$xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
$xml .= "<mzIdentML id=\"\" xsi:schemaLocation=\"http://psidev.info/psi/pi/mzIdentML/1.0  ../../../../schema/mzIdentML1.0.0.xsd\"\n";
$xml .= "\t\t\t\txmlns=\"http://psidev.info/psi/pi/mzIdentML/1.0\"\n";

# Junk code.....$response never used...ideally it should go in $xml<creationDate>
#my $response;
#open(PIPE,"date --rfc-3339=seconds |");
#$response = <PIPE>;
#close PIPE;
#$response =~ s/\n$//g;
#$response =~ s/\s+/T/;


$xml .= "\t\t\t\txmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" creationDate=\"2009-07-08T10:29:47\" version =\"1.0.0\">\n";


return $xml;

}

sub GetAnalysisProtocolCollection
{
my $params = shift;
my $xml;

#fill in Omssa
$proteinIdentificationProtocol = "protein_detect_protocol";
$spectrumIdSoftwareRef = "ID_software";

$xml = "\t\t<AnalysisProtocolCollection>\n";
$xml .= "\t\t\t<SpectrumIdentificationProtocol id=\"$spectrumIdentificationProtocol\" AnalysisSoftware_ref=\"$spectrumIdSoftwareRef\">\n";
$xml .= "\t\t\t\t<SearchType>\n";
$xml .= "\t\t\t\t\t<cvParam accession=\"MS:1001083\" name=\"ms-ms search\" cvRef=\"PSI-MS\"/>\n";
$xml .= "\t\t\t\t</SearchType>\n";  

#additional
$xml .= "\t\t\t\t<AdditionalSearchParams>\n";



$xml .= "\t\t\t\t\t<cvParam accession=\"$params{'Parent mass type'}{'Accession'}\" name=\"$params{'Parent mass type'}{'cvTerm'}\"    cvRef=\"PSI-MS\"/>\n";
$xml .= "\t\t\t\t\t<cvParam accession=\"$params{'Fragment mass type'}{'Accession'}\" name=\"$params{'Fragment mass type'}{'cvTerm'}\"    cvRef=\"PSI-MS\"/>\n";


#$xml .= "\t\t\t\t\t<userParam name=\"maxEvalueReturned\" value=\"10\" />\n";
$xml .= "\t\t\t\t</AdditionalSearchParams>\n";

#and the mods
$xml .= GetModificationParams();

#enzyme details
$xml .= "\t\t\t\t<Enzymes independent=\"0\">\n";
$xml .= "\t\t\t\t\t<Enzyme id=\"ENZ_1\" CTermGain=\"OH\" NTermGain=\"H\" missedCleavages=\"$params{'Missed cleavages'}{'Value'}\" semiSpecific=\"0\">\n";
$xml .= "\t\t\t\t\t<EnzymeName>\n";
$xml .= "\t\t\t\t\t<cvParam accession=\"$params{'Enzyme'}{'Accession'}\" name=\"$params{'Enzyme'}{'cvTerm'}\" cvRef=\"PSI-MS\" />\n";
$xml .= "\t\t\t\t\t</EnzymeName>\n";
$xml .= "\t\t\t\t\t</Enzyme>\n";
$xml .= "\t\t\t\t</Enzymes>\n";

#now the mass table
$xml .= "\t\t\t\t<MassTable id=\"0\" msLevel=\"2\">\n";
 foreach my $residue (keys %aa)
 {
  if($residue !~ m/term/)
  {
  $xml .= "\t\t\t\t\t<Residue Code=\"$residue\" Mass=\"$aa{$residue}\"/>\n";
  }
 } 
$xml .= "\t\t\t\t</MassTable>\n";

#fragment tolerances
$xml .= "\t\t\t\t<FragmentTolerance>\n";
$xml .= "\t\t\t\t\t<cvParam accession=\"MS:1001412\" name=\"search tolerance plus value\" value=\"$params{'Fragment search tolerance plus'}{'Value'}\" cvRef=\"PSI-MS\" unitAccession=\"UO:0000221\" unitName=\"dalton\" unitCvRef=\"UO\" />\n";
$xml .= "\t\t\t\t\t<cvParam accession=\"MS:1001413\" name=\"search tolerance minus value\" value=\"$params{'Fragment search tolerance minus'}{'Value'}\" cvRef=\"PSI-MS\" unitAccession=\"UO:0000221\" unitName=\"dalton\" unitCvRef=\"UO\" />\n";
$xml .= "\t\t\t\t</FragmentTolerance>\n";

$xml .= "\t\t\t\t<ParentTolerance>\n";
$xml .= "\t\t\t\t\t<cvParam accession=\"MS:1001412\" name=\"search tolerance plus value\" value=\"$params{'Parent search tolerance plus'}{'Value'}\" cvRef=\"PSI-MS\" unitAccession=\"UO:0000221\" unitName=\"dalton\" unitCvRef=\"UO\" />\n";
$xml .= "\t\t\t\t\t<cvParam accession=\"MS:1001413\" name=\"search tolerance minus value\" value=\"$params{'Parent search tolerance minus'}{'Value'}\" cvRef=\"PSI-MS\" unitAccession=\"UO:0000221\" unitName=\"dalton\" unitCvRef=\"UO\" />\n";
$xml .= "\t\t\t\t</ParentTolerance>\n";


#add the threshold in
$xml .= "\t\t\t\t<Threshold>\n";


if(lc($params{'PSM threshold'}{'Value'}) ne "n/a" && lc($params{'PSM threshold'}{'Value'}) ne "null" && $params{'PSM threshold'}{'Value'}){
	$xml .= "\t\t\t\t\t<cvParam accession=\"$params{'PSM threshold'}{'Accession'}\" name=\"$params{'PSM threshold'}{'cvTerm'}\" value=\"$params{'PSM threshold'}{'Value'}\" cvRef=\"PSI-MS\" />\n";
}
else{
	$xml .= "\t\t\t\t\t<cvParam accession=\"$params{'PSM threshold'}{'Accession'}\" name=\"$params{'PSM threshold'}{'cvTerm'}\" cvRef=\"PSI-MS\" />\n";
}
$xml .= "\t\t\t\t</Threshold>\n";


$xml .= "\t\t\t</SpectrumIdentificationProtocol>\n";

#and the protein detection protocol

if($protGrouping){
	$xml .= "\t\t\t<ProteinDetectionProtocol id=\"$proteindeterminationprotocol\" AnalysisSoftware_ref=\"$spectrumIdSoftwareRef\">\n";
	$xml .= "\t\t\t\t<Threshold>\n";
	$xml .= "\t\t\t\t\t<cvParam accession=\"MS:1001447\" name=\"prot:FDR threshold\" value=\"$prot_FDR_threshold\" cvRef=\"PSI-MS\"/>\n";
	$xml .= "\t\t\t\t</Threshold>\n";
	$xml .= "\t\t\t</ProteinDetectionProtocol>\n";
}

$xml .= "\t\t</AnalysisProtocolCollection>\n";

return $xml;
}


sub GetAnalysisSoftwareList
{

	my $xml;

	$xml = "\t<cvList>\n";
	$xml .= "\t\t<cv id=\"PSI-MS\" fullName=\"PSI-MS\" URI=\"http://psidev.cvs.sourceforge.net/viewvc/*checkout*/psidev/psi/psi-ms/mzML/controlledVocabulary/psi-ms.obo\" version=\"2.25.0\"/>\n";
	$xml .= "\t\t<cv id=\"UNIMOD\" fullName=\"UNIMOD\" URI=\"http://www.unimod.org/obo/unimod.obo\" />\n";
	$xml .= "\t\t<cv id=\"UO\" fullName=\"UNIT-ONTOLOGY\" URI=\"http://obo.cvs.sourceforge.net/*checkout*/obo/obo/ontology/phenotype/unit.obo\"></cv>\n";
	$xml .= "\t</cvList>\n"; 

	$xml .= "\t<AnalysisSoftwareList>\n";
	$xml .= "\t\t<AnalysisSoftware id=\"$spectrumIdSoftwareRef\" name=\"$params{'Software name'}{'cvTerm'}\" version=\"$params{'Software version'}{'Value'}\" >\n";
	$xml .= "\t\t\t<SoftwareName>\n";
	
	if($params{'Software name'}{'Accession'} ne "N/A"){
		$xml .= "\t\t\t\t<cvParam accession=\"$params{'Software name'}{'Accession'}\" name=\"$params{'Software name'}{'cvTerm'}\" cvRef=\"PSI-MS\" />\n";
	}
	else{
		$xml .= "\t\t\t\t<userParam name=\"$params{'Software name'}{'cvTerm'}\" />\n";
	
	}
	
	$xml .= "\t\t\t</SoftwareName>\n";
	$xml .= "\t\t</AnalysisSoftware>\n";
	$xml .= "\t</AnalysisSoftwareList>\n";


	$xml .= "\t<Provider id=\"PROVIDER\">\n";
	$xml .= "\t\t<ContactRole Contact_ref=\"PERSON_DOC_OWNER\">\n";
	$xml .= "\t\t\t<role>\n";
	$xml .= "\t\t\t\t<cvParam accession=\"MS:1001271\" name=\"researcher\" cvRef=\"PSI-MS\"/>\n";
	$xml .= "\t\t\t</role>\n";
	$xml .= "\t\t</ContactRole>\n";
	$xml .= "\t</Provider>\n";
	$xml .= "\t<AuditCollection>\n";

	my $provider = $params{'Provider'}{'Value'};
	my @tmp = split(/ /,$provider);

	$xml .= "\t\t<Person id=\"PERSON_DOC_OWNER\" firstName=\"$tmp[0]\" lastName=\"$tmp[1]\" email=\"someone\@someuniversity.com\">\n";
	$xml .= "\t\t\t<affiliations Organization_ref=\"ORG_DOC_OWNER\"/>\n";
	$xml .= "\t\t</Person>\n";
	$xml .= "\t\t<Organization id=\"ORG_DOC_OWNER\" address=\"Some address\" name=\"Some place\" />\n";

	$xml .= "\t</AuditCollection>\n";

	return $xml;

}


sub GetDataCollection
{

my $xml;


my $pval_warning = 0; #Warning only needs to printed once
my $eval_warning = 0;

$xml .= "\t<DataCollection>\n";

my $tmp = $results_file;

my $db_loc = $params{'Local database path'}{'Value'};
my @tmp = split(/\//, $db_loc);
my $last = scalar(@tmp) - 1;
my $db_name = $tmp[$last];


###Inputs
$xml .= "\t\t<Inputs>\n";
$xml .= "\t\t\t<SourceFile location=\"$tmp\" id=\"SF_1\">\n";
$xml .= "\t\t\t\t<fileFormat>\n";
$xml .= "\t\t\t\t<cvParam accession=\"$params{'Input file format'}{'Accession'}\" name=\"$params{'Input file format'}{'cvTerm'}\" cvRef=\"PSI-MS\"/>\n"; 
$xml .= "\t\t\t\t</fileFormat>\n";
$xml .= "\t\t\t</SourceFile>\n";
$xml .= "\t\t\t<SearchDatabase location=\"$db_loc\" id=\"$db_identifier\" numDatabaseSequences=\"$numDBseqs\"  version=\"1.0\" >\n";
$xml .= "\t\t\t\t<fileFormat>\n";
$xml .= "\t\t\t\t<cvParam accession=\"MS:1001348\" name=\"FASTA format\" cvRef=\"PSI-MS\"/>\n";
$xml .= "\t\t\t\t</fileFormat>\n";
$xml .= "\t\t\t\t<DatabaseName>\n";
$xml .= "\t\t\t\t\t<userParam name=\"$db_name\" />\n";
$xml .= "\t\t\t\t</DatabaseName>\n";

if($params{'Decoy database composition'}){
	$xml .= "\t\t\t\t<cvParam accession=\"$params{'Decoy database composition'}{'Accession'}\" name=\"$params{'Decoy database composition'}{'cvTerm'}\" cvRef=\"PSI-MS\"/>\n";
}
if($params{'Decoy database regex'}){
	$xml .= "\t\t\t\t<cvParam accession=\"$params{'Decoy database regex'}{'Accession'}\" name=\"$params{'Decoy database regex'}{'cvTerm'}\" value=\"$params{'Decoy database regex'}{'Value'}\" cvRef=\"PSI-MS\"/>\n";
}
if($params{'Decoy database type'}){
	$xml .= "\t\t\t\t<cvParam accession=\"$params{'Decoy database type'}{'Accession'}\" name=\"$params{'Decoy database type'}{'cvTerm'}\" cvRef=\"PSI-MS\"/>\n";
}


$xml .= "\t\t\t</SearchDatabase>\n";
$xml .= "\t\t\t<SpectraData location=\"/localdirectory/*\" id=\"SD_1\" >\n";
$xml .= "\t\t\t\t<fileFormat>\n";
$xml .= "\t\t\t\t\t<cvParam accession=\"MS:1001062\" name=\"Mascot MGF file\" cvRef=\"PSI-MS\" />\n";
$xml .= "\t\t\t\t</fileFormat>\n";
$xml .= "\t\t\t\t<spectrumIDFormat>\n";
$xml .= "\t\t\t\t\t<cvParam accession=\"MS:1000774\" name=\"multiple peak list nativeID format\" cvRef=\"PSI-MS\" />\n";
$xml .= "\t\t\t\t</spectrumIDFormat>\n";
$xml .= "\t\t\t</SpectraData>\n";
$xml .= "\t\t</Inputs>\n";


### And the analysis data

##peptide hits first
$xml .= "\t\t<AnalysisData>\n";
$spectrumIdentificationList = "Spectrum_1_result";
$xml .= "\t\t\t<SpectrumIdentificationList id=\"$spectrumIdentificationList\" numSequencesSearched=\"$numDBseqs\">\n";

my $counter = 0;
my $specTitle;
 
#and now all the results
 #for all the spectra
 for(my $s=0 ; $s<scalar(@results) ; $s++)
 {
	my $top_rank_hit = $results[$s][0];
	
   if($top_rank_hit){
		my $i = "result_ref_" . $s;	   
		
	   
		if($MGF_TITLE_FOR_SPECTRUM_ID){
	   
			
			#$specID = $results[$s][1]{'title'};
			$specTitle = $top_rank_hit->specTitle();
			$specTitle =~ s/\s+$//; #remove trailing spaces
		}
		else{
			$specTitle = "index=".$s;
		}
	   
	   
	   $xml .= "\t\t\t\t<SpectrumIdentificationResult id=\"$i\" spectrumID=\"$specTitle\" SpectraData_ref=\"SD_1\">\n";
   } 
  #for all the ranks
  for(my $r=0 ; $r<=$MAX_RANK ; $r++)
  {
	my $pep_hit = $results[$s][$r];

   if($top_rank_hit && $pep_hit)
   {

   #peptide evidence must link to below (protein detection)
   $counter++;
   #my $PE_ref = "PE_" . $s . "_" . $r . "_" . $results[$s][$r]{'protein'};
   #push(@{$prot_evidence{$results[$s][$r]{'protein'}}},$PE_ref);

   my $temp_rank = $r+1;		#ranks start from 1 rather than zero
   my $id = "result_ref_" . $s . "_" . $temp_rank;
   my $item_id = "item_ref_" . $s . "_" . $temp_rank;
   #get the peptide ref 
   my $ref = GetPeptideRef($s,$r);

   my $length = length($pep_hit->sequence());
   my $end = $pep_hit->start() + $length - 1;

    #does it pass the threshold?
    my $pass=0;
	
	my $eval = $pep_hit->evalue();
	my $pval = $pep_hit->pvalue();
	my %se_cv_maps = %{$se_config{'cv_mappings'}};
	
	if(lc($params{'PSM threshold'}{'Value'}) ne "n/a" && lc($params{'PSM threshold'}{'Value'}) ne "null" && $params{'PSM threshold'}{'Value'}){
	
		my @cells = @{$se_cv_maps{'evalue'}};	
		
		if($cells[1] =~ m/e-value|evalue/i){
			if($params{'PSM threshold'}{'cvTerm'} =~ m/e-value/i){
				if($eval < $params{'PSM threshold'}{'Value'}){
					$pass = 1;
				}
				else{
					$pass = 0;
				}				
			}
			else{
				print "Warning: e-value threshold set, but e-values have not been detected, all peptides will pass threshold";
				$pass = 1;
			}
		}
		
		if($params{'PSM threshold'}{'cvTerm'} =~ m/no threshold/i){
			$pass = 1;
		}
		
		if($params{'PSM threshold'}{'cvTerm'} =~ m/FDR/i){
			if($cells[1] =~ m/fdr/i){
				if($eval < $params{'PSM threshold'}{'Value'}){
					$pass = 1;
				}
				else{
					$pass = 0;
				}
			}
			else{
				print "Warning: FDR threshold set, but FDR values have not been detected, all peptides will pass threshold";
				$pass = 1;
			}
		}	
		
	}
	
	my $temp_rank = $r+1;	#start counting from 1 rather than zero
	my $peptide_ref = "Peptide_" . $s . "_" . $temp_rank;

	my $rank = $r+1;
   $xml .= "\t\t\t\t\t<SpectrumIdentificationItem id=\"$item_id\" calculatedMassToCharge=\"".$pep_hit->precMassToCharge."\"  chargeState=\"".$pep_hit->charge()."\" experimentalMassToCharge=\"".$pep_hit->theoMass()."\" Peptide_ref=\"$peptide_ref\" passThreshold=\"$pass\" rank=\"$rank\">\n";
   
	my @proteins = $pep_hit->getProteinMaps();	
	
	#my $count= 1;
	foreach my $prot (@proteins){
	
		#$prot£$start£$end
		my @tmp = split(/£/,$prot);
		my $prot_acc =$tmp[0];
		
		my $decoy = " isDecoy=\"false\"";
   
		my $decoyString = $params{'Decoy database regex'}{'Value'};
		if($decoyString){	
		    if($prot_acc =~ m/^$decoyString/)
		    {
				$decoy = " isDecoy=\"true\"";
		    }
		}
		my $temp_rank = $r+1;	#start counting ranks from 1 rather than zero
		#my $pe_ref = "PE_".$s."_".$temp_rank."_".$prot_acc."_".$count;
		my $specID = $pep_hit->queryIndex();
		my $pe_ref = "PE_".$specID."_".$tmp[1]."_".$tmp[2]."_".$prot_acc."_".$tmp[3];
		$xml .= "\t\t\t\t\t\t<PeptideEvidence id=\"$pe_ref\" start=\"$tmp[1]\" end=\"$tmp[2]\" DBSequence_Ref=\"$prot_acc\" $decoy \/>\n";
		#$count++;
		
	}
      	
	########## Print primary score (e-value or FDRScore or equivalent) ###############
	if($se_cv_maps{'evalue'}){
		my @cells = @{$se_cv_maps{'evalue'}};
		if($cells[2] ne "N/A"){
			$xml .= "\t\t\t\t\t\t<cvParam accession=\"$cells[2]\" name=\"$cells[1]\" cvRef=\"PSI-MS\"  value=\"$eval\" />\n";
		}
		else{
			$xml .= "\t\t\t\t\t\t<userParam name=\"$cells[1]\" value=\"$eval\" />\n";
		}
	}
	else{
		if(!$eval_warning){
			print "Warning, unable to detect a mapping to the e-value/FDRscore, no equivalent score will be output\n";
			$eval_warning = 1;
		}
	}
	
	########## Print second score (p-value, hyperscore or equivalent) ###############
	if($se_cv_maps{'pvalue'}){
		my @cells = @{$se_cv_maps{'pvalue'}};
		if($cells[2] ne "N/A"){
			$xml .= "\t\t\t\t\t\t<cvParam accession=\"$cells[2]\" name=\"$cells[1]\" cvRef=\"PSI-MS\"  value=\"$pval\" />\n";
		}
		else{
			$xml .= "\t\t\t\t\t\t<userParam name=\"$cells[1]\" value=\"$pval\" />\n";
		}
	}
	else{
		if(!$pval_warning){	
			print "Warning, unable to detect a mapping to the second score (p-value / hyperscore), no equivalent score will be output\n";
			$pval_warning = 1;
		}
	}
   
   
   #if($params{'Software name'}{'cvTerm'} eq "OMSSA"){
	#$xml .= "\t\t\t\t\t\t<cvParam accession=\"MS:1001328\" name=\"OMSSA:evalue\" cvRef=\"PSI-MS\"  value=\"$eval\" />\n";
	#$xml .= "\t\t\t\t\t\t<cvParam accession=\"MS:1001329\" name=\"OMSSA:pvalue\" cvRef=\"PSI-MS\"  value=\"$pval\" />\n";
   #}
   #elsif($params{'Software name'}{'cvTerm'} eq "xtandem"){
	#$xml .= "\t\t\t\t\t\t<cvParam accession=\"MS:1001330\" name=\"xtandem:expect\" cvRef=\"PSI-MS\"  value=\"$eval\" />\n";
	#$xml .= "\t\t\t\t\t\t<cvParam accession=\"MS:1001331\" name=\"xtandem:hyperscore\" cvRef=\"PSI-MS\"  value=\"$pval\" />\n";
  #}
  #else{
#		print "WARNING, unable to detect search engine used: [$params{'Software name'}{'cvTerm'}], will not output any search scores\n";
  #}
   
   
   $xml .= "\t\t\t\t\t</SpectrumIdentificationItem>\n";
   my $spectra_ref = "spectra_" . $s;
   } 
  }
 
   if($top_rank_hit)
   {
   $xml .= "\t\t\t\t</SpectrumIdentificationResult>\n";
   }  
 } 

$xml .= "\t\t\t</SpectrumIdentificationList>\n";

return $xml;

}


sub GetPeptideRef
{
	my $s = shift;
	my $r = shift;
	my $ref;

	my $pep_hit = $results[$s][$r];
    if($pep_hit->mods())
    {
		$ref = $pep_hit->sequence() . "*" . $pep_hit->mods();
    }
    else
    {
		$ref = $pep_hit->sequence();
    }
	
	return $ref;
}

sub GetModificationParams{
	my $xml;

	if(%searchMods){
		$xml .= "\t\t\t\t<ModificationParams>\n";
		foreach my $key (keys %searchMods){
			my %mod = %{$searchMods{$key}};
	
			if($mod{'Fixed'} && $mod{'Mass Delta'} && $mod{'Unimod ID'} && $mod{'Unimod name'}){
				$xml .= "\t\t\t\t\t<SearchModification fixedMod=\"$mod{'Fixed'}\">\n";
				$xml .= "\t\t\t\t\t\t<ModParam massDelta=\"$mod{'Mass Delta'}\" residues=\"$mod{'Residue'}\">\n";
				$xml .= "\t\t\t\t\t\t\t<cvParam accession=\"UNIMOD:$mod{'Unimod ID'}\" name=\"$mod{'Unimod name'}\" cvRef=\"UNIMOD\" />\n"; 
				$xml .= "\t\t\t\t\t\t</ModParam>\n";
				$xml .= "\t\t\t\t\t</SearchModification>\n";
			}
			else{
				print "WARNING: Incomplete definition for searched modification: $key\n Required: Mods results name	Unimod name	Unimod ID	Residue	Fixed	Mass Delta\n";
			}	
		}
		$xml .= "\t\t\t\t</ModificationParams>\n";
	}
	else{
		print "No search mods detected in the input parameters\n";
	}
	return $xml;

}


sub GetAnalysisCollection
{
	my $xml;

	$xml = "\t<AnalysisCollection>\n";
	$xml .= "\t\t<SpectrumIdentification id=\"SI_1\" SpectrumIdentificationProtocol_ref=\"$spectrumIdentificationProtocol\" SpectrumIdentificationList_ref=\"$spectrumIdentificationList\" activityDate=\"2008-02-27T08:22:12\">\n";
	$xml .= "\t\t\t<InputSpectra SpectraData_ref=\"SD_1\"/>\n";
	$xml .= "\t\t\t<SearchDatabase SearchDatabase_ref=\"$db_identifier\"/>\n";
	$xml .= "\t\t</SpectrumIdentification>\n";

	if($protGrouping){
		$proteinDetectionList = "ProteinList_1";
		$xml .= "\t\t<ProteinDetection id=\"$proteinDetectionList\" ProteinDetectionProtocol_ref=\"$proteindeterminationprotocol\" ProteinDetectionList_ref=\"$proteinDetectionList\" activityDate=\"2008-02-29T10:29:47\">\n";
		$xml .= "\t\t\t<InputSpectrumIdentifications SpectrumIdentificationList_ref=\"$spectrumIdentificationList\"/>\n";
		$xml .= "\t\t</ProteinDetection>\n";

		
	}
	$xml .= "\t</AnalysisCollection>\n";

	return $xml;

}

sub GetProteinDetectionList_deprecated{

	my $xml;

	$xml .= "\t\t\t<ProteinDetectionList id=\"$proteinDetectionList\">\n";

	 my $count = 0;
	 #now for all the proteins
	 foreach my $p (keys %prot_evidence)
	 {
	 $count++;
	 my $identifier = "Ambiguity_grp_" . $count;
	 $xml .= "\t\t\t\t<ProteinAmbiguityGroup id=\"$identifier\">\n";
	 $identifier = "detection_id_" . $p;
	 $xml .= "\t\t\t\t\t<ProteinDetectionHypothesis id=\"$identifier\" DBSequence_ref=\"$p\" passThreshold=\"0\">\n";

	my $pep_counter = 0;
	my %unique_peps;
	my $coverage = 0;
	  #for each of the peptides in this protein
	  for(my $i=0 ; $i<scalar(@{$prot_evidence{$p}}) ; $i++)
	  {
	   if(!$unique_peps{$prot_evidence{$p}[$i]})
	   {
	   my $tmp = GetSeqFromId($prot_evidence{$p}[$i]);
	   my $prot_len = length($proteins{$p});
	   my $pep_len = length($tmp);
	   $tmp = int($pep_len/$prot_len*100);
	   $coverage += $tmp;
	   $unique_peps{$prot_evidence{$p}[$i]} = 1;
	   $pep_counter++;
	   } 
	 
	  $xml .= "\t\t\t\t\t\t<PeptideHypothesis PeptideEvidence_Ref=\"$prot_evidence{$p}[$i]\" \/>\n";
	   if($i == (scalar(@{$prot_evidence{$p}})-1))
	   {
	   $xml .= "\t\t\t\t\t\t<cvParam accession=\"MS:1001093\" name=\"sequence coverage\" cvRef=\"PSI-MS\" value=\"$coverage\" />\n";
	   }
	  }


	 $xml .= "\t\t\t\t\t<cvParam accession=\"MS:1001097\" name=\"distinct peptide sequences\" cvRef=\"PSI-MS\" value=\"$pep_counter\" />\n"; 
	 $xml .= "\t\t\t\t\t</ProteinDetectionHypothesis>\n";
	 $xml .= "\t\t\t\t</ProteinAmbiguityGroup>\n";
	 }
	$xml .= "\t\t\t</ProteinDetectionList>\n";

	return $xml;

}

#sub GetSeqFromId
#{
#my $id = shift;

# foreach my $modseq (keys %peptides)
 #{
 # if($peptides{$modseq}[1] eq $id)
 # {
 #return $modseq;
 # } 
 #}
#return 0;

#}

sub GetModsFromXML(){

	my $xml_file = shift;
	my %mods;
	
	# create object
	my $xml = new XML::Simple;

	# read XML file
	my $data = $xml->XMLin($xml_file);

	# print output
	#print Dumper($data);
	
	my %all_data = %{$data};
	my @all_records = @{$all_data{'MSModSpec'}};
	
	for(my $i=0 ; $i<@all_records ; $i++){
		my %record = %{$all_records[$i]};
		
		my $modname = $record{'MSModSpec_name'};
		my $unimod_id = $record{'MSModSpec_unimod'};
		my $unimod_name = $record{'MSModSpec_psi-ms'};
		$unimod_name =~ s/:/£/g;			#fix to replace : with £
		my $mono_mass = $record{'MSModSpec_monomass'};
		my $ave_mass = $record{'MSModSpec_averagemass'};
		
		$mods{$modname} = $unimod_id .":".$unimod_name.":".$mono_mass.":".$ave_mass;
	}
	
	
	return %mods;
}

sub readCSVParams(){
	my $param_file = shift;
	my $tmp = shift;
	my @required_params = @{$tmp};
	
	my (%params, %mods);
	
	if(!$param_file){
		die "Fatal error, no parameter file given\n";
	}

	open(PARAM,"<$param_file") or die "Fatal error: There is a problem opening the param file, $param_file\n";
	
	my $foundMods = 0;
	
	while(my $line = <PARAM>){
		my $csv = Text::CSV->new();
		my $status = $csv->parse($line);         # parse a CSV string into fields
		my @cells = $csv->fields(); 

		#if($line =~ /Mods results name/){
		if($line =~ /results name/){
			$foundMods = 1;	#do nothing with this line except stop searching for params
		}				
		elsif(!$foundMods){
			
			my %param;
			
			if($cells[0]){
				$param{"Param name"} = $cells[0];
				$param{"cvTerm"} = $cells[1];
				$param{"Accession"} = $cells[2];
				$param{"Value"} = $cells[3];
				
				$params{$cells[0]} = \%param;
			}
		}
		else{
			my %mod;		#Mods results name	Unimod name	Unimod ID	Residue	Fixed	Mass Delta
			if($cells[0]){
				$mod{"Mods results name"} = $cells[0];
				$mod{"Unimod name"} = $cells[1];
				$mod{"Unimod ID"} = $cells[2];
				$mod{"Residue"} = $cells[3];
				$mod{"Fixed"} = lc($cells[4]);
				$mod{"Mass Delta"} = $cells[5];
				$mods{$cells[0]} = \%mod;
			}
		}		
	}
	
	foreach my $req_param (@required_params){
		
		
		if($params{$req_param}){
			
			my %param = %{$params{$req_param}};
			
			if(!($param{"Param name"} && $param{"cvTerm"} && $param{"Accession"}&&$param{"Value"})){
				print "FATAL: Error in param file (".$param_file.")for required parameter: $req_param\n";
				print "Each line must contain exactly \"Param,cvTerm,Accession,Value\"\n Use Null or N/A for empty values\n";	
				die;
			}
		}	
		else{
			print "FATAL: Error in param file for required parameter: $req_param\n";
			print "Each line must contain exactly \"Param,cvTerm,Accession,Value\n Use Null or N/A for empty values";	
			die;
		}
	}
	
	
	return (\%params,\%mods);
	
}


sub readConfig{
	my $config_file = shift;
	
	my (%search_engine_maps,%cv_maps);
	
	if(!$config_file){
		die "Fatal error, no config file given\n";
	}

	open(PARAM,"<$config_file") or die "Fatal error: There is a problem opening the config file, $config_file\n";
	
	my $foundMods = 0;
	
	my %se_pos;
	
	my $parsing_data_types = 1;
		
	while(my $line = <PARAM>){
		my $csv = Text::CSV->new();
		my $status = $csv->parse($line);         # parse a CSV string into fields
		my @cells = $csv->fields(); 
		
		if($line =~ /Internal data type/){
			for (my $i =1 ; $i <@cells; $i++){
				my %temp_hash;
				$search_engine_maps{$cells[$i]} =\%temp_hash;
				$se_pos{$i} = $cells[$i];
				#print "Detected search engine maps for $cells[$i]\n";				
			}			
		}		
		elsif(!%search_engine_maps){
			die "Fatal error, no search engine maps detected in the config file\n";			
		}
		elsif($parsing_data_types){
			
			if($line =~/CV mappings/){
				$parsing_data_types = 0;
				
			}
			else{
				my $data_type = $cells[0];
				
				if($data_type){
					for (my $i =1 ; $i <@cells; $i++){				
						my %temp_se = %{$search_engine_maps{$se_pos{$i}}};	
						$temp_se{$data_type} = $cells[$i];			
						
						#print "Added mapping from $data_type to $cells[$i] for $se_pos{$i}\n";
						$search_engine_maps{$se_pos{$i}} = \%temp_se;
					}
				}
			}
						
		}

		if(!$parsing_data_types){
			if($line =~ /Data type/ || $line =~/CV mappings/){	}	#do nothing header line
			else{
				
				if($cells[0]){
					my $engine = $cells[3];
					my %map;
					if($search_engine_maps{$engine}){
						%map = %{$search_engine_maps{$engine}};
					}
					
					if(!%map){
						die "Fatal error, search engine name ($engine) in CV maps of config file not recognised, first cell: $cells[0]\n"
					}
					
					my %se_cv_map;
					if($map{"cv_mappings"}){
						%se_cv_map = %{$map{"cv_mappings"}};
					}
					$se_cv_map{$cells[0]} = \@cells;
					$map{"cv_mappings"} = \%se_cv_map;
					$search_engine_maps{$engine} = \%map;
					#print "adding mapping from $cells[0] to @cells\n";
				}			
			}
		}
		
	}
		
	#print Dumper %search_engine_maps;
	
	return (\%search_engine_maps);
	
}



sub GetProteinDetectionList(){


	my $xmlStringToWrite; ## Append all the stuff in this string to finally dump it in the file
 	$xmlStringToWrite .= "\t\t\t<ProteinDetectionList id=\"$proteinDetectionList\">\n";
 	
 	my $flag = 1;
 	my $prevLineGroupId;
 	my $prevLineProtAcc;
 	my $pagCounter = 1;
	
	my $this_prot_score;
	
	
	
	for(my $i=0; $i < @all_pags ; $i++ ){
		
		
		if($all_pags[$i]){
			#print "Processing PAG: $i, $all_pags[$i]\n";
			$xmlStringToWrite .= "\t\t\t\t<ProteinAmbiguityGroup id= \"PAG_".$i."\" > \n"; 
			
			my %pag_prot_list = %{$all_pags[$i]};
			for my $prot_acc (keys %pag_prot_list){
				
				$xmlStringToWrite.= "\t\t\t\t\t<ProteinDetectionHypothesis id= \"".$i."_$prot_acc\" DBSequence_ref= \"$prot_acc\"  passThreshold=\"true\">\n";
				
				my @pep_hits = @{$protein_mappings{$prot_acc}};
				
				my $prot_score;
				
				foreach my $pep_hit (@pep_hits){
					my @tmp = split(/,/,$pep_hit);
					my $pepID = "PE_".$tmp[0]."_".$tmp[2]."_".$tmp[3]."_".$prot_acc."_".$tmp[5];
					$xmlStringToWrite.= "\t\t\t\t\t\t<PeptideHypothesis PeptideEvidence_Ref= \"$pepID\"  />\n";
					
					if($tmp[4]){
						$prot_score = $tmp[4];
					}
				}
				$xmlStringToWrite .= "\t\t\t\t\t\t<userParam name = \"Product FDRscore\" value = \"$prot_score\" />\n";
				$xmlStringToWrite .= "\t\t\t\t\t</ProteinDetectionHypothesis>\n"; 
	
			}
			$xmlStringToWrite .= "\t\t\t\t</ProteinAmbiguityGroup>\n";			
		
		}	
	}
 	$xmlStringToWrite .= "\t\t\t</ProteinDetectionList>\n"; ## close the PDL	

	return$xmlStringToWrite;
}





