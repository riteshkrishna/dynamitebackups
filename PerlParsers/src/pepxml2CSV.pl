#!/usr/bin/perl

use strict;
use XML::Simple qw(:strict);
use Data::Dumper;

if(@ARGV != 3){
	print "\nUsage: pepxml2CSV.pl inputpepxml.xml outputfile.csv score-types\n";
	print "score-types = comma separated list of the named scores in the file e.g \"pepxml2CSV.pl expect,ionscore\" or \"pepxml2CSV.pl xcorr,deltacn,spscore\"\n";
	#print "If spaces are required in the comma separated list, use \$ symbol e.g. e\$value ";
	exit(1);
}

my $input_result = $ARGV[0];
my $outfile = $ARGV[1];
my $tmp = $ARGV[2];

open(CSV,">$outfile") or die "unable to open the $outfile\n";

our @score_types = split(/\,/,$tmp);

my $score_header;

foreach my $s (@score_types){
	$score_header .= $s.",";
}
$score_header =~ s/\,*$//; #remove trailing comma

our $header_line = "Spectrum number,Filename/id,Peptide,".$score_header.",Mass,Accession,Start,Stop,Mods,Charge,Theo Mass\n";


ReadPepXML($input_result);


sub ReadPepXML(){

	my $xml_file = shift;
	
	my @pep_results;
	
	# create object
	my $xml = new XML::Simple(ForceArray => [ 'msms_run_summary','search_hit','search_result','search_score' ,'spectrum_query','alternative_protein','mod_aminoacid_mass'] , KeyAttr => 'name');
	# read XML file
	

	my $data = $xml->XMLin($xml_file);

	my %all_data = %{$data};
	#print Dumper %all_data;
	
	my @all_runs = @{$all_data{'msms_run_summary'}};
	
	foreach my $tmp_run (@all_runs){

		my %all_records = %{$tmp_run};		
		my @all_spectra = @{$all_records{'spectrum_query'}}; 
		
		my @csvResults;

		#print "*************all_spectra start ****************\n";
		#print Dumper(@all_spectra);
		#print "*************all_spectra end ****************\n";
		#exit;
		
		for(my $i=0 ; $i<@all_spectra ; $i++){
		
			my %spec_hit = %{$all_spectra[$i]};	
			my ($spec_name,$specID,$obs_mass,$charge);
			#my (@proteins,$specName,$specID,$pepSeq,$eval,$obs_mass,$gi,$acc,$start,$end,$defline,$mods,$charge,$theoMass);			
			
			#print "************* spec_hit start ****************\n";
			#print Dumper %spec_hit;
			#print "************* spec_hit end ****************\n";

			my @search_results;
			
			my $spec_index = $spec_hit{'index'};
			$spec_name = $spec_hit{'spectrum'};
			$obs_mass = $spec_hit{'precursor_neutral_mass'};
			$charge = $spec_hit{'assumed_charge'};
			
			
			if($spec_hit{'search_result'}){
				@search_results = @{$spec_hit{'search_result'}};		
			
				foreach my $tmp_res (@search_results){
							
					my %search_res = %{$tmp_res};
					
					#print "************* Res start ****************\n";
					#print Dumper %search_res;
					#print "************* Res end ****************\n";
					
					my @pep_hits;
					if($search_res{'search_hit'}){
						@pep_hits = @{$search_res{'search_hit'}};
					}
					
					foreach my $temp_hit (@pep_hits){
						
						my (@proteins,$pepSeq,$eval,$pval,$gi,$acc,$start,$end,$defline,$mods,$theoMass);	
						
						my %pep_hit = %{$temp_hit};
						$pepSeq = $pep_hit{'peptide'};
						my $tmp_prot = $pep_hit{'protein'};
						$theoMass = $pep_hit{'calc_neutral_pep_mass'};
						$start = 0;	#appears to be no mapping to start and end peptide position in pepXML
						$end = 0;
						push(@proteins,$tmp_prot);					
						
						my @alt_prots;
						if($pep_hit{'alternative_protein'}){
							@alt_prots = $pep_hit{'alternative_protein'};
							join (@proteins, @alt_prots);
						}			
							
						#print "prots: @proteins\n";
						
						# ************ Start handle scores ****************************
						#'search_score' => {
						#			'identityscore' => {  'value' => '40' },
						#			'homologyscore' => { 'value' => '15'},
						#			'ionscore' => { 'value' => '3.21'},
						#			'expect' => {'value' => '2.7e+02' },
						
						my %scores;
						my $pep_scores;
						
						if($pep_hit{'search_score'}){
							%scores = %{$pep_hit{'search_score'}};
							
							foreach my $score_type (@score_types){
								
								if($scores{$score_type}){								
									my $value = $scores{$score_type}{'value'};
									$pep_scores .= $value.",";
								}
							}
							$pep_scores =~ s/\,*$//; #remove trailing comma
						}
						# ************ End handle scores ****************************					
						
						# ************ Start handle mods ****************************
						#'modification_info' => {
						#	'mod_aminoacid_mass' => [
						#							{  'mass' => '147.192600',
						#							  'position' => '4'	},
						#							{
						#							  'mass' => '147.192600',
						#							  'position' => '6'
						#							},
						#							{
						#							  'mass' => '147.192600',
						#							  'position' => '11'
						#							}
						#						  ],
						#	'modified_peptide' => 'SEMM[147]AM[147]HKRFM[147]'   },					
						
						my %tmp_mods;
						
						my $found_mods;
						
						if($pep_hit{'modification_info'}){							
							%tmp_mods = %{$pep_hit{'modification_info'}};
							
							my @mods;
							if(%tmp_mods){
								@mods = @{$tmp_mods{'mod_aminoacid_mass'}};								
								foreach my $tmp_mod (@mods){					
									my %mod = %{$tmp_mod};							
									my $mass = $mod{'mass'};
									my $pos = $mod{'position'};
									#print "Mod: $mass => $pos\n";
									my $res = substr($pepSeq,$pos-1,1);
									$found_mods =  $mass."_".$res.":$pos".",";
								}
								$found_mods =~ s/\,*$//; #remove trailing comma
							}
							
							#print "Mods: $found_mods\n";
							
						}
						# ************ Endhandle mods ****************************
						
						#Spectrum number	 Filename/id	 Peptide	 E-value	 Mass	 gi	 Accession	 Start	 Stop	 Defline	 Mods	 Charge	 Theo Mass	 P-value	 NIST score

						foreach my $protein (@proteins){
							if(!$spec_name){
								$spec_name = "index=$spec_index";
							}
							my $pep_result = $spec_index.",".$spec_name.",".$pepSeq.",".$pep_scores.",".$obs_mass.",".$protein.",".$start.",".$end.",".$found_mods.",".$charge.",".$theoMass;
							push (@pep_results,$pep_result);
						}
					}				
				}
			}
			else{
				#print "No search result for spec: $spec_index\n";
			}
		}
	}
	
	printCSV(\@pep_results);
}


sub getProteinDetails{
	
	my $tmp = shift;
	my %protein = %{$tmp};
	
	my @results;
	
	#$res{'Accession'} = $protein{'label'};
	my $prot_acc = $protein{'label'};
	
	if($protein{'peptide'}){
		
		my @peptides = @{$protein{'peptide'}};			
		if(@peptides > 1){
			die "Fatal error, unexpected array of peptide elements reported\n";
		}
		
		my %peptide = %{$peptides[0]};		
		
		#my %peptide = %{$protein{'peptide'}};
		
		if(%peptide){
			my @domains = @{$peptide{'domain'}};		
			
			foreach my $domain (@domains){
				my %domain = %{$domain};						
				my %res = %{getOneResult(\%domain)};
				$res{'Accession'} = $protein{'label'};					
				push(@results,\%res);
			}			
		}
	}
	else{
		print "no peptides for %protein\n";
		print Dumper(%protein);
		exit;
	}

	return \@results;
}

sub getOneResult{

	my $tmp = shift;
	my %domain = %{$tmp};
	my %res;
	$res{'Peptide'} = $domain{'seq'};
	$res{'E-value'} = $domain{'expect'};
	$res{'Start'} = $domain{'start'};
	$res{'Stop'}= $domain{'end'};			
	$res{'Theo Mass'}	= $domain{'mh'};
	$res{'Tandem Hyperscore'} = $domain{'hyperscore'};						

	my $mod_element = $domain{'aa'};
	
	if($mod_element){
		$res{'Mods'} = getModString($mod_element,$domain{'start'});
	}
	return \%res;
}


sub getModString{

	my $mod_element = shift;
	my $start = shift;
	
	my $mod_string;
	
	#Don't know whether $mod_element is an array or a hash	
	if(ref($mod_element) eq 'ARRAY'){
		$mod_string = "\"";
		foreach my $tmp_mod (@{$mod_element}){
			my %mod = %{$tmp_mod};
			
			my $pos = $mod{'at'} - $start + 1; 
			$mod_string .= ",".$mod{'modified'} . "_" . $mod{'type'} . ":". $pos;
		}
		$mod_string =~ s/^\"\,/\"/;	#remove leading ,
		$mod_string .= "\"";
	}
	else{
		my %mod = %{$mod_element};
		my $pos = $mod{'at'} - $start +1 ; 		
		$mod_string = $mod{'modified'} . "_" . $mod{'type'} . ":". $pos;
	}
		
	return $mod_string;
}

sub printCSV{

	my $tmp = shift;
	my @results = @{$tmp};
	
	print CSV $header_line;
	foreach my $line (@results){
	
		#my $line;
		#for(my $j = 0; $j < @columns; $j++){
		#	$line .= "," . $res{$columns[$j]};
		#}
		#$line =~ s/^\,//;	#remove leading ,
		print CSV "$line\n";
					
	}
}


