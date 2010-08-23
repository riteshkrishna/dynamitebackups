package ReadCSV;
require Exporter;

use FindBin;
use lib "$FindBin::Bin/..";


our @ISA = qw(Exporter);
our $VERSION = 1.0;;
our @EXPORT = qw(ParseCSV);




use strict;
use IPC::Open2;
use strict;
use DBI;
use LWP;
use HTML::LinkExtor;
use HTML::Form;
use URI;
use Getopt::Long;
use Data::Dumper;
use MIME::Base64;
use Math::Complex;
use POSIX qw(log10);
use FindBin;
use lib "$FindBin::Bin/../";
#use lib qw(C:/Work/MultipleSearch/FDRWeb/perl_modules/);
#use Parser;
use Text::CSV;
use PeptideHit;
use Data::Dumper;


#######################################################################
##   Andy Jones 2010                                         
##                                                                   
## This parses output from a CSV file, which requires a separate config file to map column headers to data types
## The essential columns are the spec_title, e-value/FDRScore, peptide sequence, mods, start, end, prot_acc or defline
##  The code can also read in a column for protein ambiguity groups, which are handled separately
#######################################################################

my $cgi;

#a globl string to hold the mod info
my %mod_params;
my %params;
my %search;
my @mod_order;



return 1;

sub ParseCSV{

	my $csv_file = shift;
	my $tmp = shift;
	my $tmp_pags = shift;
	my %config = %{$tmp};

	
	my %mapped_columns;

	my $parse_pags = $tmp_pags;
	my %protein_details;
	my @results;

	my %peptide;
	my $rank=1;
	my $db_source;

	my $title = 0;
	my %protein_group;
	my @all_pags;	#only used if parsing Protein Ambiguity groups

	my %spec_titles;
	my $spec_counter =0;
	
	my $line_index = 0;

	my $rank = 0;		#altered this code so that the best rank is zero; easier code at this end; this needs to be accounted for elsewhere
	my $spec_num = 0;		#Altered the code so that spectrum numbers are now arbitrary, the spectrum title is used for all identification purposes (Mascot does not maintain the spectrum index)
	
	my $checked_headers = 0;

	print("opening csv file:".$csv_file."\n");

	open(RES,"<$csv_file") or die "ERROR: There is a problem opening the CSV results, $csv_file\n";
	 while(my $line = <RES>)
	 {
	    #Needs a fix since Omssa's CSV implementation is incorrect with respect to quotes within quotes
		$line = fixCSV($line);
		my $csv = Text::CSV->new();
		my $status = $csv->parse($line);         # parse a CSV string into fields
		my $bad_argument = $csv->error_input();
		#print("CSV status = ".$status.", bad arg=".$bad_argument."\n");
		my @cells = $csv->fields(); 
		
		#print("CSV line: ".$line."\n");

		if(!$checked_headers) {
			#Algorithm
			# 1. Check that all headers can be mapped to internal datatypes
			# 2. Create a hash that maps each internal data-type to a column position

			for my $data_type (keys %config){
			
				my $mapped_header = $config{$data_type};
				
				if($mapped_header && $data_type ne "cv_mappings"){
					my $found = 0;
					for(my $i =0 ; $i<@cells ; $i++){
						my $cell = $cells[$i];
						
						#DCW 160210 Check for an EXACT string match
						$cell =~ s/^\s+//;
						$cell =~ s/\s+$//;
						$mapped_header =~ s/^\s+//;
						$mapped_header =~ s/\s+$//;
						if($cell eq $mapped_header){
						#if($cell =~ /$mapped_header/){
							$found =1;
							$mapped_columns{$data_type} = $i;
							my $comp = ($cell eq $mapped_header);
							#print "Mapped $data_type to columm $i. cell= $cell, header=$mapped_header, equal? $comp\n";
						}
					}

					if($found == 0 && ($data_type eq "spec_title" || $data_type eq "evalue" || $data_type eq "mods" || $data_type eq "start" || $data_type eq "stop" )){
						die "Fatal error in CSV parsing, no header found in CSV file for $data_type file= $csv_file target= $config{$data_type}\n";
					}
					elsif($found == 0){
						print "Warning, internal data type: $data_type not detected in the CSV file\n";
					}
				}
			}		
			
			$checked_headers = 1;			
			
		}
		else
		{		
			if(!$checked_headers){
			
				die "Fatal error, incorrect headers reported in CSV file\n";
			}
			
			if(!$cells[$mapped_columns{"spec_title"}]){
				print "Warning, empty spec_title ignoring row\n";
			}
			else{
				  
				my $pep_hit =  eval { new PeptideHit(); }  or die ($@);  
				
				foreach my $cell (@cells){
					$cell =~ s/^\s+//; #remove leading spaces
					$cell =~ s/\s+$//; #remove trailing spaces
				}
		  
				#Get the data out of the mapped columns
				#my $spec_index = $cells[$mapped_columns{"spec_index"}]; 	#spec_index no longer used
				my $spec_title = $cells[$mapped_columns{"spec_title"}];
				my $seq = uc($cells[$mapped_columns{"seq"}]); #omssa makes the residues with mods lower case - change them to upper
				
				if(!$seq){
					die "Fatal error - no peptide sequence for $spec_title near line: $line_index\n";
				}
				
				my $evalue = $cells[$mapped_columns{"evalue"}];
				my $mass_obs = $cells[$mapped_columns{"mass_obs"}];
				my $gi  = $cells[$mapped_columns{"gi"}];
				my $prot_acc  = $cells[$mapped_columns{"prot_acc"}];
				my $start  = $cells[$mapped_columns{"start"}];
				my $stop  = $cells[$mapped_columns{"stop"}];				
				my $defline  = $cells[$mapped_columns{"defline"}];
				my $mods  = $cells[$mapped_columns{"mods"}];
				my $charge  = $cells[$mapped_columns{"charge"}];
				my $mass_exp  = $cells[$mapped_columns{"mass_exp"}];
				my $pvalue  = $cells[$mapped_columns{"pvalue"}];
				my $pag_num  = $cells[$mapped_columns{"pag_num"}]; 
				my $prot_score = $cells[$mapped_columns{"prot_score"}];
				
				my $delta = $mass_exp - $mass_obs;
				
								
				if(!$start){
					print "Warning, no start value given, setting to zero\n";
					$start =0;
				}
				if(!$stop){
					print "Warning, no stop value given, setting to zero\n";
					$stop =0;
				}	
				$evalue =~ s/ //g;				
				if(!$evalue){
					print "Error - no e-value or other equivalent score needed for ordering provided for Peptide: $seq ($spec_title), setting to 9999\n";
					$evalue = 9999;
				}	
				
				#Omssa has a bug in that it doesn't always correctly parse out the accessions	
				
				#if ($prot_acc =~ /^-?\d/){	#if $cells[6] holds a number, Omssa has failed to parse accessions - this code was causing a bug for some non-numeric accessions, fixed by ARJ July 1st 2010
				if ($prot_acc =~ /^[+-]?\d+$/){
				
					my $prev_prot_acc = $prot_acc;
					my @split_prot = split/ /,$defline;
					$prot_acc = $split_prot[0];
					
					if(!$prot_acc){	#Fall back position to avoid fatal errors if possible
						$prot_acc = $prev_prot_acc;
					}
				}
				else{
					$prot_acc = $cells[$mapped_columns{"prot_acc"}];
				}
				
				if(!$prot_acc){
					die "Fatal error: no protein accession for peptide $seq near line: $line_index\n";
				}

				
				$pep_hit->specTitle($spec_title);
				$pep_hit->pvalue($pvalue);				
				$pep_hit->evalue($evalue);
				$pep_hit->sequence($seq);
				$pep_hit->mods($mods);
				$pep_hit->start($start);
				$pep_hit->end($stop);
				$pep_hit->theoMass($mass_exp);
				$pep_hit->charge($charge);
				$pep_hit->delta($delta);
				$pep_hit->precMassToCharge($mass_obs);
				
				$pep_hit->pepHitID($line_index);
				$line_index++;
				
				if($prot_score){
					$pep_hit->protScore($prot_score);
				}
				
				#If spectrum title has been seen before, the index is the a
				if($spec_titles{$spec_title}){
					$spec_num = $spec_titles{$spec_title};
				}
				else{
					$spec_counter++;	#new spectrum therefore increment spectrum counter
					$spec_num = $spec_counter;
					$spec_titles{$spec_title} = $spec_num;
				}
				
				$pep_hit->queryIndex($spec_num);
				
					   
				#and now group the results in proteins		  
				#Add the query,rank, start and end position to the protein mapping	  	 
			  
				my @ranked_hits;	  	  
				my $rank = 0;  
				

				
				if($results[$spec_num]){
				
				
					#@ranked_hits = @{$results[$spec_num]};
					#print "Sending $results[$spec_num]\n";
					my $new_res;

					($rank,$new_res,$pep_hit) = insertRankPos($results[$spec_num], $pep_hit);  #This re-orders the hits for this spectrum and returns the new rank and the peptide hit - in case this only a new peptide2protein mapping (returns previous peptide hit)
					$results[$spec_num] = $new_res;					

				}
				else{
					$results[$spec_num][0] = $pep_hit;
				}
				  
				  
				if($rank == -1){
					die "Error, incorrect rank calculated for $pep_hit\n";
				}
				
			
				push(@{$protein_group{$prot_acc}},$spec_num . "," . $rank . "," . $start . "," . $stop . "," . $prot_score.",".$pep_hit->pepHitID()); 
				my $prot_map = $prot_acc ."£".$start."£".$stop."£".$pep_hit->pepHitID();
				
				if($pep_hit->getProteinMaps()){
					my @prev_prot = $pep_hit->getProteinMaps();
				

					my $temp_prot_string =  join ",", @prev_prot; 					
					
					#if($temp_prot_string eq $prot_map){
					if(index($temp_prot_string,$prot_map) != -1){
						print "Error, duplicate protein maps found: $temp_prot_string and $prot_map\n";
					}
					else{
						$pep_hit->addProteinMap($prot_map);	
					}
				}
				else{
					$pep_hit->addProteinMap($prot_map);	
				}
				
				#If this is a new peptide hit, 
				if($parse_pags){		
					$pep_hit->pag($pag_num);
										
					my %PAG_prot_list;
					if($all_pags[$pag_num]){
						%PAG_prot_list = %{$all_pags[$pag_num]};
					}
					$PAG_prot_list{$prot_acc} = 1;
					$all_pags[$pag_num] = \%PAG_prot_list;
					
				}				
			}
		}
	}
	
	close FILE;
	
	#print "All results*************\n";
	#print Dumper @results;
	#print "*******************";
	
	#print "All Prot groups*************\n";
	#print Dumper %protein_group;
	#print "*******************";
	
	#print "All PAGs*************\n";
	#print Dumper @all_pags;
	#print "*******************";

	return(\@results,\%protein_group,\@all_pags);

}

sub fixCSV{
	my $line = shift;
	my $pos = index($line,",\"");
	my $pos2;
	my $old_pos = 0;
	
	#Need a regular expression to replace " with "" unless it is preceeded or followed by a comma	
	$line =~ s/,\"/,TEMPQUOTE/g;
	$line =~ s/\",/TEMPQUOTE,/g;
	$line =~ s/\"/\"\"/g;
	$line =~ s/TEMPQUOTE/\"/g;		# replace " with ""
	
	#$line =~ s/\n//g;	#remove any end of line chars - DCW commented out, causes problem with unix 
	#print("ReadCSV line=".$line."*\n");
	return $line;
	
}

#Method to insert a hit in the correct rank position for a given spectrum, based on e-value
sub insertRankPos{
	my $results = shift;
	
	#print "ranked res:  $temp1\n";
	my $pep_hit = shift;
	
	
	
	my @ranked_res = @{$results};	
	#print "Received: $temp1\n";
	#my $temp2 = \@ranked_res;
	#print "Now received: $temp2\n";
	
	my $rank = -1;	
		
	my $test_eval = $pep_hit->evalue();
	#better design to start ranks from zero and account for this elsewhere
	
	my $found = 0;
	
	for(my $i = 0; $i < @ranked_res ; $i++){
	
		my $hit = $ranked_res[$i];

		
		my $newE = $hit->evalue();		
		#print "stored hit e: $newE, new hit E: $test_eval\n";
	
		if($test_eval < $hit->evalue()){
			# Need to insert evalue in here in the array and shift all others along
			#splice ARRAY,OFFSET,LENGTH,LIST
			splice (@ranked_res, $i, 0, $pep_hit);
			
			#print "Splice: new evalue: $test_eval, old evalue: ".$hit->evalue() . " at position $i \n";
			#print "@ranked_res\n";
			$rank = $i;
			$i = @ranked_res;
			$found = 1;
			
		}
		elsif($test_eval == $hit->evalue()){
			
			$rank = $i;
			$i = @ranked_res;
			
			#New protein mapping, just return rank
			if($hit->mods() eq $pep_hit->mods() && $hit->sequence() eq $pep_hit->sequence()){
				$found = 1;
				$pep_hit = $hit;	#return the old peptide hit, protein maps will be added to this one
				my $temp_seq =  $hit->sequence();
			}
			else{
				splice (@ranked_res, $i, 0, $pep_hit);
			
				#print "Splice: new evalue: $test_eval, old evalue: ".$hit->evalue() . " at position $i \n";
				#print "@ranked_res\n";
				$rank = $i;
				$i = @ranked_res;
				$found = 1;
			}
			
			#print "New prot map\n";
		}			
	}	
	
	if(!$found){
		$rank = @ranked_res;
		push(@ranked_res,$pep_hit);
		#print "Adding to end of array for $test_eval\n";
	}

	#print "*******  After insertRankPos ************ \n";
	#print Dumper @ranked_res;
	#print "\n*******  End after insertRankPos ************ \n";
	
		
	return ($rank,\@ranked_res,$pep_hit);
}




